/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vss;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTransactionRollbackException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.hsqldb.jdbc.JDBCDriver;
import clustering.Cluster;
import clustering.ClusterMember;
import clustering.ClusterSet;
import clustering.Datapoint;
import clustering.Dataset;
import clustering.Score;
import vortex.main.VoronoiCell;
import annotations.Annotation;
import clustering.AngularDistance;
import clustering.DistanceMatrix;
import clustering.DistanceMeasure;
import java.util.HashSet;
import vortex.clustering.Kernel;
import vortex.util.ConnectionManager;
import util.MatrixOp;
import util.logger;
import util.util;

/**
 *
 * @author Nikolay
 */
public final class HSQLDBStorageEngine2 implements StorageEngine {

    private static final long serialVersionUID = 1L;
    private static final String db_version = "2.0.0";
    private Connection con;

    public HSQLDBStorageEngine2(ConnectionManager.DatabaseHost h) throws SQLException, IOException {

        try {
            DriverManager.registerDriver(new JDBCDriver());
        } catch (SQLException e) {
            logger.print("ERROR: failed to register HSQLDB JDBC driver.");
            logger.showException(e);
            con = null;
            return;
        }

        assert (h.instanceName.equals("local file"));
        String connectionString = "jdbc:hsqldb:file:" + h.address.replaceAll("\\\\", "/") + ";shutdown=true";//;hsqldb.large_data=true";

        try {
            con = DriverManager.getConnection(connectionString, h.username, h.password);
        } catch (Exception e) {
            logger.print("Connection string: " + connectionString);
            e.printStackTrace();
        }
        if (getSchemaVersion() == null) {
            InputStream is = getClass().getClassLoader().getResourceAsStream("vss/HSQLDB_init_script2.sql");
            if (is == null) {
                throw new IllegalStateException("Cannot read HSQLDB_init_script2.sql");
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String in;
            while ((in = br.readLine()) != null) {
                sb.append(in);
            }
            //logger.print(sb);
            String[] sqlLines = sb.toString().split(";");
            for (String string : sqlLines) {
                try {
                    con.prepareStatement(string).execute();
                } catch (SQLException e) {
                    e = new SQLException("Error parsing the following query: " + string + "\n" + e.getMessage());
                    throw e;
                }
            }
            try (ResultSet conf = con.prepareStatement("Select param, value from vss.config", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery()) {
                conf.moveToInsertRow();
                conf.updateString(1, "Database Version");
                conf.updateObject(2, db_version);
                conf.insertRow();
                conf.close();
            }
            this.shutdown();
            con = DriverManager.getConnection(connectionString, h.username, h.password);
        }
        con.createStatement().execute("SET FILES WRITE DELAY 100 MILLIS");
        logger.print("HSQLDB storage engine init done.");
        logger.print("Connected to: " + connectionString + ", usr: sa " + h.username + "password " + h.password.length() + " characters long");
        /*
         PreparedStatement st = con.prepareStatement("select cluster_size, caption, comment, id from vss.clusters ");

         try (ResultSet rs = st.executeQuery()) {
         while (rs.next()) {
         logger.print(rs.getInt(1), rs.getString(2), rs.getString(3),  rs.getInt(4));
         }
         } catch (SQLException e) {
         logger.print(e);
         }*/
    }

    @Override
    public String toString() {
        return "HSQLDBStorageEngine";
    }

    @Override
    public String[] getAvailableDatasetNames() throws SQLException {
        ResultSet rs = con.createStatement().executeQuery("Select distinct dataset_name from vss.datasets");
        List<String> list = new ArrayList<>();
        while (rs.next()) {
            list.add(rs.getString(1));
            //logger.print(rs.getString(1));
        }
        return list.toArray(new String[list.size()]);
    }

    @Override
    public void clearPrecomputedData(int dsID) throws SQLException {

    }

    public String getSchemaVersion() throws SQLException {
        String query = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE lower(SCHEMA_NAME) = 'vss'";
        ResultSet conf = con.prepareStatement(query).executeQuery();
        if (conf.next()) {
            conf = con.prepareStatement("Select param, value from vss.config where param = 'Database Version'").executeQuery();
            if (conf.next()) {
                String s = (String) conf.getObject(2);
                conf.close();
                return s;
            } else {
                conf.close();
                return null;
            }
        } else {
            conf.close();
            return null;
        }
    }

    @Override
    public int getNextClusterSetBatchID(int datasetID) throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT max(batch_id) FROM vss.cluster_sets WHERE dataset_id = ?");
        ps.setInt(1, datasetID);
        ResultSet conf = ps.executeQuery();
        conf.next();
        int ret = conf.getInt(1);
        conf.close();
        return ret;
    }

    @Override
    public int[] getClusterSetIDs(int datasetID) throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT id FROM vss.cluster_sets WHERE dataset_id = ?");
        ps.setInt(1, datasetID);
        ArrayList<Integer> al;
        try (ResultSet conf = ps.executeQuery()) {
            al = new ArrayList<>();
            while (conf.next()) {
                al.add(conf.getInt(1));
            }
            conf.close();
        }
        Collections.sort(al);
        return util.toArray(al);
    }

    @Override
    public Cluster loadCluster(final int clusterID, final ClusterSet cs) throws SQLException {
        PreparedStatement st = con.prepareStatement("select cluster_size, mode, caption, comment, color_code, scores from vss.clusters where id = ?");
        st.setInt(1, clusterID);
        Cluster ret;
        try (ResultSet rs = st.executeQuery()) {
            rs.next();
            double[] modeVector = (double[]) rs.getObject(2);
            final double[] baseModeVector = Arrays.copyOf(modeVector, cs.getDataset().getDimension());

            final double[] sideModeVector = (modeVector.length == baseModeVector.length) ? null : Arrays.copyOfRange(modeVector, baseModeVector.length, modeVector.length);

            ArrayList<Score.ScoringMethod> scores = new ArrayList<>();//(Arrays.asList((Score.ScoringMethod[]) rs.getObject(6)));
            ret = new Cluster(clusterID, cs, rs.getInt(1), rs.getString(3), rs.getString(4), (Color) rs.getObject(5), baseModeVector, sideModeVector, scores) {
                @Override
                protected int[] getMemberDPIDs() {
                    if (memberDPIDs == null) {
                        try {
                            PreparedStatement st = con.prepareStatement("select cluster_member_indices from vss.clusters where id = ?");
                            st.setInt(1, clusterID);
                            try (ResultSet rs = st.executeQuery()) {
                                rs.next();
                                memberDPIDs = (int[]) rs.getObject(1);
                            }
                            rs.close();
                            st = null;
                            Arrays.sort(memberDPIDs);
                            // logger.print("des call#" + (numOfCalls++));
                        } catch (SQLException e) {
                            logger.showException(e);
                        }
                    }
                    //  logger.print("mem call#" + (numOfCalls2++));
                    return memberDPIDs;
                }

                @Override
                public void updateValue(int col, Object value) {
                    super.updateValue(col, value); //To change body of generated methods, choose Tools | Templates.
                    try {
                        saveCluster(this, false);
                    } catch (Exception e) {
                        logger.showException(e);
                    }
                }

                @Override
                protected ClusterMember[] deserializeItems() throws SQLException {
                    if (items == null) {

                        int[] memberIDs = getMemberDPIDs();

                        items = new ClusterMember[memberIDs.length];
                        for (int i = 0; i < items.length; i++) {
                            items[i] = new ClusterMember(getClusterSet().getDataset().getDPByID(memberIDs[i]), this, null /*scores*/, ""/*comment*/);
                        }

                    }
                    return items;
                }
            };
            ret.setClusterSet(cs);
        }
        return ret;
    }

    @Override
    public void saveCluster(Cluster c, boolean deep) throws SQLException {
        if (c.getClusterSet() == null) {
            throw new IllegalArgumentException("This cluster has no cluster set assigned");
        }
        if (c.getClusterSet().getID() == 0) {
            throw new IllegalArgumentException("The cluster set of this cluster has not been assigned an ID yet");
        }

        PreparedStatement st;
        boolean upd = false;
        if (c.getID() == 0) {
            ResultSet rs2 = con.createStatement().executeQuery("select max(id)+1 from vss.clusters");
            rs2.next();
            c.setID(rs2.getInt(1));
            st = con.prepareStatement("insert into vss.clusters (id, cs_id, cluster_size, mode, caption, comment, color_code, scores) values (?,?,?,?,?,?,?,?)");
        } else {
            upd = true;
            st = con.prepareStatement("update vss.clusters set id = ?, cs_id =?, cluster_size =?, mode =?, caption =?, comment = ?, color_code = ?, scores = ? where id = ?");
        }

        st.setInt(1, c.getID());
        st.setInt(2, c.getClusterSet().getID());
        st.setInt(3, c.size());
        st.setObject(4, MatrixOp.concat(c.getMode().getVector(), c.getMode().getSideVector()));
        st.setString(5, c.getCaption());
        st.setString(6, c.getComment());
        st.setObject(7, c.getColorCode());
        st.setObject(8, c.getComputedScores().toArray(new Score.ScoringMethod[c.getComputedScores().size()]));
        if (upd) {
            st.setInt(9, c.getID());
        }
        st.executeUpdate();
        if (deep) {
            st = con.prepareStatement("update vss.clusters set cluster_member_indices = ? where id = ?");
            ClusterMember[] cm = c.getClusterMembers();
            int[] dpid = new int[cm.length];
            for (int i = 0; i < dpid.length; i++) {
                dpid[i] = cm[i].getDatapoint().getID();
            }
            st.setObject(1, dpid);
            st.setInt(2, c.getID());
            st.executeUpdate();
        }
    }

    @Override
    public ClusterMember[] loadClusterMembers(Cluster c) throws SQLException {
        //  Thread.dumpStack();

        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public synchronized void saveClusterMembers(Cluster cluster, ClusterMember[] cm) throws SQLException {
        con.createStatement().execute("delete from vss.cluster_members where cluster_id = " + cluster.getID());
        int id;
        try (ResultSet rs = con.createStatement().executeQuery("select max(id)+1 FROM vss.cluster_members")) {
            rs.next();
            id = Math.max(rs.getInt(1), 1);
        }
        PreparedStatement st = con.prepareStatement("insert into vss.cluster_members (id, cluster_id, datapoint_id, scores, comment) values (?,?,?,?,?)", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);

        Score.ScoringMethod[] compScores = new Score.ScoringMethod[cluster.getComputedScores().size()];
        for (int i = 0; i < compScores.length; i++) {
            compScores[i] = cluster.getComputedScores().get(i);
        }

        for (ClusterMember c : cm) {
            int i = 1;
            st.setInt(i++, id++);
            st.setInt(i++, cluster.getID());
            st.setInt(i++, c.getDatapoint().getID());

            double[] scoreValues = new double[compScores.length];
            for (int j = 0; j < scoreValues.length; j++) {
                scoreValues[j] = c.getScores().get(compScores[j]).score;
            }

            st.setObject(i++, scoreValues);
            st.setString(i++, c.getComment());
            st.addBatch();
        }
        try {
            st.executeBatch();
        } catch (SQLException e) {
            logger.showException(e);
        }
    }

    @Override
    public Cluster[] loadClustersForClusterSet(ClusterSet cs) throws SQLException {
        PreparedStatement st = con.prepareStatement("select distinct id from vss.clusters where cs_id = ?");
        st.setInt(1, cs.getID());
        List<Cluster> list;
        try (ResultSet rs = st.executeQuery()) {
            list = new LinkedList<>();
            while (rs.next()) {
                list.add(loadCluster(rs.getInt(1), cs));
            }

            Collections.sort(list, new Comparator<Cluster>() {
                @Override
                public int compare(Cluster o1, Cluster o2) {
                    return o2.size() - o1.size();
                }
            });

            return list.toArray(new Cluster[list.size()]);
        } catch (SQLTransactionRollbackException e) {
            logger.showException(e);
            return new Cluster[0];
        }

    }

    @Override
    public ClusterSet loadClusterSet(int clusterSetID, Dataset nd) throws SQLException {

        try {
            con.prepareStatement("select DISTANCE_MEASURE from VSS.CLUSTER_SETS").executeQuery();

        } catch (SQLException e) {
            con.prepareStatement("ALTER TABLE VSS.CLUSTER_SETS ADD COLUMN DISTANCE_MEASURE OTHER").execute();
        }

        PreparedStatement st = con.prepareStatement("select COLOR_CODE, CLUSTERING_METHOD, ALGORITHMPARAMETERSTRING, MAINPARAMETERVALUE, NUM_CLUSTERS, BATCH_ID, DATE_CREATED, COMMENT, DISTANCE_MEASURE from VSS.CLUSTER_SETS where ID = ?");
        st.setInt(1, clusterSetID);
        ResultSet rs = st.executeQuery();
        rs.next();
        DistanceMeasure dm = (rs.getObject("DISTANCE_MEASURE") == null) ? new AngularDistance() : (DistanceMeasure) rs.getObject("DISTANCE_MEASURE");
        ClusterSet cs = new ClusterSet(clusterSetID, nd, (Color) rs.getObject("COLOR_CODE"), rs.getString("CLUSTERING_METHOD"), rs.getString("ALGORITHMPARAMETERSTRING"), rs.getDouble("MAINPARAMETERVALUE"), rs.getInt("NUM_CLUSTERS"), rs.getInt("BATCH_ID"), rs.getDate("DATE_CREATED"), rs.getString("COMMENT"), dm) {

            @Override
            public void delete() throws SQLException {
                super.delete(); //To change body of generated methods, choose Tools | Templates.
                deleteClusterSet(this.getID());
            }

            @Override
            public Cluster[] getClusters() {
                if (clusters == null) {
                    try {
                        clusters = loadClustersForClusterSet(this);
                    } catch (SQLException e) {
                        logger.showException(e);
                    }
                }
                return clusters;
            }
        };
        rs.close();
        return cs;
    }

    private HashMap<Integer, String> getFileNamesForDataset(int datasetID) throws SQLException {
        PreparedStatement st = con.prepareStatement("Select id, filename from vss.sourcefiles where dataset_id = ? order by id");
        HashMap<Integer, String> hm = new HashMap<>();
        st.setInt(1, datasetID);
        ResultSet rs = st.executeQuery();
        while (rs.next()) {
            hm.put(rs.getInt(1), rs.getString(2));
        }
        return hm;
    }

    @Override
    public Datapoint[] loadDatapoints(int datasetID) throws SQLException {
        // logger.print("Loading datapoints for ds " + datasetID);
        // Thread.dumpStack();
        ResultSet rs = con.createStatement().executeQuery("Select min(id) from vss.datapoints where dataset_id = " + datasetID);

        if (!rs.next()) {
            return null;
        }

        int minid = rs.getInt(1);
        rs.close();

        HashMap<Integer, String> hmFileNames = getFileNamesForDataset(datasetID);

        PreparedStatement st = con.prepareStatement("Select id, dataset_id, profile_name, vector, side_vector, file_id, id_within_file from vss.datapoints where dataset_id = ? order by ID");
        st.setInt(1, datasetID);
        ArrayList<Datapoint> dp = new ArrayList<>();
        rs = st.executeQuery();

        while (rs.next()) {
            String profileName = rs.getString(3);
            int file_id = rs.getInt(6);
            final String fileName = hmFileNames.get(file_id);
            final int idWithinFile = rs.getInt(7);

            Datapoint d = (profileName != null) ? new Datapoint(profileName, (double[]) rs.getObject(4), (double[]) rs.getObject(5), rs.getInt(1) - minid, hmFileNames.get(rs.getInt(6)), rs.getInt(7))
                    : new Datapoint(null, (double[]) rs.getObject(4), (double[]) rs.getObject(5), rs.getInt(1) - minid, hmFileNames.get(rs.getInt(6)), rs.getInt(7)) {
                @Override
                public String getFullName() {
                    return fileName + String.format(" Event %07d", idWithinFile);
                }
            };

            for (double[] vec : new double[][]{d.getVector(), (d.getSideVector() == null) ? new double[0] : d.getSideVector()}) {
                for (int i = 0; i < vec.length; i++) {
                    if (Double.isNaN(vec[i])) {
                        vec[i] = 0;
                    }
                }
            }

            dp.add(d);
        }

        Collections.sort(dp, new Comparator<Datapoint>() {
            @Override
            public int compare(Datapoint o1, Datapoint o2) {
                return o1.getID() - o2.getID();
            }
        });
        rs.close();
        return dp.toArray(new Datapoint[dp.size()]);
    }

    @Override
    public Dataset[] loadDatasets() throws SQLException {
        PreparedStatement st = con.prepareStatement("Select id from vss.datasets order by id");
        ResultSet rs = st.executeQuery();
        ArrayList<Dataset> ds = new ArrayList<>();
        while (rs.next()) {
            ds.add(loadDataset(rs.getInt(1)));
        }
        rs.close();
        return ds.toArray(new Dataset[ds.size()]);
    }

    @Override
    public Dataset[] loadDatasets(int[] datasetIDs) throws SQLException {
        ArrayList<Dataset> ds = new ArrayList<>();
        for (int id : datasetIDs) {
            ds.add(loadDataset(id));
        }
        return ds.toArray(new Dataset[ds.size()]);
    }

    private Date updateLastAccessDate(int datasetID) throws SQLException {
        Date date = new Date(Calendar.getInstance().getTimeInMillis());
        PreparedStatement st = con.prepareStatement("Update vss.datasets set last_access = ? where id = ?");
        st.setDate(1, date);
        st.setInt(2, datasetID);
        st.execute();
        return date;
    }

    boolean trim = false;

    @Override
    public Dataset loadDataset(int datasetID) throws SQLException {

        try {
            con.prepareStatement("select SCALING_VECTOR from VSS.DATASETS").executeQuery();

        } catch (SQLException e) {
            con.prepareStatement("ALTER TABLE VSS.DATASETS ADD COLUMN  SCALING_VECTOR OTHER").execute();
        }

        PreparedStatement st = con.prepareStatement("Select dataset_name, date_created, last_access, num_points, feature_list, side_var_list, scaling_vector from vss.datasets where id = ?");
        st.setInt(1, datasetID);
        Dataset ds;
        try (ResultSet rs = st.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            int k = 1;
            ds = new Dataset(datasetID, rs.getString(k++), rs.getDate(k++), rs.getDate(k++), rs.getInt(k++), (String[]) rs.getObject(k++), (String[]) rs.getObject(k++)) {
                private static final long serialVersionUID = 1L;

                @Override
                public Annotation[] getAnnotations() {

                    if (annotations == null) {
                        annotations = new ArrayList<>();
                        try {
                            annotations.addAll(Arrays.asList(ConnectionManager.getStorageEngine().loadAnnotations(this)));
                        } catch (SQLException e) {
                            logger.showException(e);
                        }
                        if (true || annotations.isEmpty()) {
                            HashMap<String, List<Integer>> hmTerms = new HashMap<>();
                            for (Datapoint dp : getDatapoints()) {
                                String term = dp.getFilename();
                                if (term.length() < dp.getFullName().length()) {
                                    if (hmTerms.get(term) == null) {
                                        hmTerms.put(term, new LinkedList<Integer>());
                                    }
                                    hmTerms.get(term).add(dp.getID());
                                }
                            }
                            Annotation ann = new Annotation(this, "By file: " + name);
                            for (String term : hmTerms.keySet()) {
                                ann.addTerm(hmTerms.get(term), term);
                            }

                            annotations.add(ann);
                        }
                    }
                    return super.getAnnotations(); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public synchronized void removeAnnotation(Annotation ann) {
                    super.removeAnnotation(ann); //To change body of generated methods, choose Tools | Templates.
                    try {
                        ConnectionManager.getStorageEngine().deleteAnnotation(ann);
                    } catch (SQLException e) {
                        logger.showException(e);
                    }
                }

                @Override
                public synchronized void addAnnotation(Annotation ann) {
                    super.addAnnotation(ann); //To change body of generated methods, choose Tools | Templates.
                    try {
                        saveAnnotation(ann);
                    } catch (SQLException e) {
                        logger.showException(e);
                    }
                }

                @Override
                public String[] getFeatureNames() {
                    if (fn == null) {
                        String[] fNames = super.getFeatureNames();
                        String[] ret = new String[fNames.length];
                        for (int i = 0; i < ret.length; i++) {
                            ret[i] = (trim) ? fNames[i].replaceFirst("\\(.+\\)", "") : fNames[i];
                        }
                        fn = ret;
                    }
                    return fn;
                }

                @Override
                public String[] getFeatureNamesCombined() {
                    if (ffn == null) {
                        String[] fNames = super.getFeatureNamesCombined();
                        String[] ret = new String[fNames.length];
                        for (int i = 0; i < ret.length; i++) {
                            ret[i] = (trim) ? fNames[i].replaceFirst("\\(.+\\)", "") : fNames[i];
                        }
                        ffn = ret;
                    }
                    return ffn;
                }

                String[] svn = null;
                String[] fn = null;
                String[] ffn = null;

                @Override
                public String[] getSideVarNames() {
                    if (svn == null) {
                        String[] fNames = super.getSideVarNames();
                        if (fNames == null) {
                            svn = new String[0];
                        } else {
                            String[] ret = new String[fNames.length];
                            for (int i = 0; i < ret.length; i++) {
                                ret[i] = (trim) ? fNames[i].replaceFirst("\\(.+\\)", "") : fNames[i];
                            }
                            svn = ret;
                        }
                    }
                    return svn;
                }

                @Override
                public Datapoint[] getDatapoints() {
                    if (this.datapoints == null) {
                        try {
                            this.datapoints = HSQLDBStorageEngine2.this.loadDatapoints(getID());
                            dateAccessed = updateLastAccessDate(getID());
                        } catch (Exception e) {
                            logger.showException(e);
                        }
                    }
                    return super.getDatapoints();
                }
            };
            updateLastAccessDate(datasetID);
        }
        return ds;
    }

    @Override
    public Dataset loadDataset(String datasetName) throws SQLException {
        PreparedStatement st = con.prepareStatement("Select id from vss.datasets where dataset_name = ?");
        st.setString(1, datasetName);
        ResultSet rs = st.executeQuery();
        if (!rs.next()) {
            return null;
        } else {
            return loadDataset(rs.getInt(1));
        }
    }

    @Override
    public synchronized void saveClusterSet(ClusterSet cs, boolean deep) throws SQLException {

        try {
            con.prepareStatement("select DISTANCE_MEASURE from VSS.CLUSTER_SETS").executeQuery();

        } catch (SQLException e) {
            con.prepareStatement("ALTER TABLE VSS.CLUSTER_SETS ADD COLUMN DISTANCE_MEASURE OTHER").execute();
        }
        PreparedStatement st;

        int k = 1;
        if (cs.getID() == 0) {
            ResultSet rs = con.createStatement().executeQuery("select max(id)+1 from VSS.CLUSTER_SETS");
            rs.next();
            cs.setID(Math.max(1, rs.getInt(1)));
            st = con.prepareStatement("Insert into VSS.CLUSTER_SETS values (?, ?, ? ,? ,?, ?, ?, ?, ?, ?, ?)");
            st.setInt(k++, cs.getID());
        } else {
            st = con.prepareStatement("update VSS.CLUSTER_SETS set BATCH_ID=?,  DATASET_ID=?, DATE_CREATED=?, CLUSTERING_METHOD=?, NUM_CLUSTERS=?, COMMENT=?, COLORCODE=?, ALGORITHMPARAMETERSTRING=?, MAINPARAMETERVALUE=?, DISTANCE_MEASURE=? from VSS.CLUSTER_SETS where ID = " + cs.getID());
        }
        st.setInt(k++, cs.getBatchID());
        st.setInt(k++, cs.getDataset().getID());
        st.setDate(k++, cs.getDateCreated());
        st.setString(k++, cs.getClusteringAlgorithm());
        st.setInt(k++, cs.getNumberOfClusters());
        st.setString(k++, cs.getComment());
        st.setObject(k++, cs.getColorCode());
        st.setString(k++, cs.getClusteringParameterString());
        st.setDouble(k++, cs.getMainClusteringParameterValue());
        st.setObject(k++, cs.getDistanceMeasure());
        st.executeUpdate();

        if (deep) {
            for (Cluster c : cs.getClusters()) {
                saveCluster(c, deep);
            }
        }
        //return new ClusterSet(clusterSetID, nd, this, (Color)rs.getObject("COLORCODE"), rs.getString("CLUSTERING_METHOD"), rs.getString("ALGORITHMPARAMETERSTRING"),  rs.getDouble("MAINPARAMETERVALUE"), rs.getInt("NUM_CLUSTERS"), rs.getInt("BATCH_ID"), rs.getDate("DATE_CREATED"), rs.getString("COMMENT"));
    }

    @Override
    public void saveDatapoints(int datasetID, Datapoint[] dp) throws SQLException {

        HashSet<String> hs = new HashSet<>();

        for (Datapoint d : dp) {
            hs.add(d.getFilename());
        }

        PreparedStatement ps = con.prepareStatement("Insert into vss.sourcefiles values (next value for vss.seq_sourcefile_id, ? ,?)");

        for (String h : hs) {
            ps.setInt(1, datasetID);
            ps.setString(2, h);
            ps.executeUpdate();

        }

        ResultSet rsfiles = con.createStatement().executeQuery("select id, filename from vss.sourcefiles where dataset_id = " + datasetID);

        HashMap<String, Integer> hmFileIDs = new HashMap<>();

        while (rsfiles.next()) {
            hmFileIDs.put(rsfiles.getString(2), rsfiles.getInt(1));
        }

        Arrays.sort(dp, new Comparator<Datapoint>() {
            @Override
            public int compare(Datapoint o1, Datapoint o2) {
                return o1.getID() - o2.getID();
            }
        });
        int nid = 0;
        logger.print("saving datapoints:");

        int maxDPID = 0;
        ResultSet rs = con.createStatement().executeQuery("Select max(id) from vss.datapoints");
        rs.next();
        maxDPID = rs.getInt(1) + 1;
        ResultSet rs2 = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE).executeQuery("Select id, dataset_id, profile_name,vector, side_vector, file_id, id_within_file from vss.datapoints where id=-1");

        for (Datapoint d : dp) {
            rs2.moveToInsertRow();
            int k = 1;
            rs2.updateInt(k++, maxDPID++);
            rs2.updateInt(k++, datasetID);
            rs2.updateString(k++, d.getNameWithoutFileName());
            rs2.updateObject(k++, d.getVector());
            rs2.updateObject(k++, d.getSideVector());
            rs2.updateInt(k++, hmFileIDs.get(d.getFilename()));
            rs2.updateInt(k++, d.getIdWithinFile());
            rs2.insertRow();

            //ps.execute();
            d.setID(nid++);
            if (nid % 100 == 0) {
                logger.print("saved: " + nid);
            }
        }

        /*      
         for (NDatapoint d : dp) {
         int k = 1;
         ps.setInt(k++, datasetID);
         ps.setString(k++, d.getName());
         ps.setString(k++, "");
         ps.setObject(k++, d.getVector());
         ps.setObject(k++, d.getSideVector());
         ps.addBatch();
         //ps.execute();
         d.setID(nid++);
         if(nid%1000==0){
         logger.print("added: " + nid);
               
         }
         }
         logger.print("executing batch: ");
         ps.executeBatch();
         */
    }

    @Override
    public int saveDataset(Dataset ds, boolean deep) throws SQLException {
        con.setAutoCommit(false);

        /*
        id int,
dataset_name varchar(1024) unique,
date_created date,
last_access date,
num_points int,
feature_list other,
side_var_list other null,
         */
        PreparedStatement ps = con.prepareStatement("Insert into vss.datasets (id, dataset_name,date_created,last_access,num_points,feature_list,side_var_list) values(next value for vss.seq_dataset_id, ?, ?, ?, ?, ?,?)");
        int k = 1;
        ps.setString(k++, ds.getName());
        ps.setDate(k++, ds.getDateCreated());
        ps.setDate(k++, ds.getDateAccessed());
        ps.setInt(k++, ds.size());
        ps.setObject(k++, ds.getFeatureNames());
        ps.setObject(k++, ds.getSideVarNames());
        ps.executeUpdate();
        ResultSet rs = con.prepareStatement("select max(id) from vss.datasets").executeQuery();
        rs.next();
        int id = rs.getInt(1);
        rs.close();
        ds.setID(id);

        saveDatapoints(id, ds.getDatapoints());
        con.commit();
        return id;
        /*id int,
         dataset_name varchar(1024),
         date_created date,
         last_access date,
         num_points int,
         num_features int,
         feature_list other,
         primary key (id)*/
    }

    @Override
    public void saveDensities(int datasetID, double[] densities, Kernel k) throws SQLException {
        /*
         CREATE TABLE vss.densities(
         dataset_id int,
         density_array other, 
         kernel varchar(1024),
         foreign key (dataset_id) references vss.datasets(id) on delete cascade
         )
         CREATE INDEX vss.ix_densities_kernel ON vss.densities (kernel) 
         */
        PreparedStatement pst = con.prepareStatement("Insert into vss.densities values (?,?,?)");
        pst.setInt(1, datasetID);
        pst.setObject(2, densities);
        pst.setString(3, k.toString() + " " + k.getDistanceMeasure().toString());
        pst.executeUpdate();
    }

    @Override
    public double[] loadDensities(Dataset ds, Kernel k) throws SQLException {
        /*
         CREATE TABLE vss.densities(
         dataset_id int,
         density_array other, 
         kernel varchar(1024),
         foreign key (dataset_id) references vss.datasets(id) on delete cascade
         )
         CREATE INDEX vss.ix_densities_kernel ON vss.densities (kernel) 
         */
        PreparedStatement pst = con.prepareStatement("Select density_array from vss.densities where dataset_id = ? and kernel = ?");
        pst.setInt(1, ds.getID());
        pst.setString(2, k.toString() + " " + k.getDistanceMeasure().toString());
        ResultSet rs = pst.executeQuery();
        double[] res = rs.next() ? (double[]) rs.getObject(1) : null;
        rs.close();
        return res;
    }

    @Override
    public void deleteAnnotation(Annotation ann) throws SQLException {
        con.createStatement().execute("delete from vss.annotations where id = " + ann.getID());
    }

    @Override
    public void deleteDataset(Dataset ds) throws SQLException {
        clearPrecomputedData(ds.getID());
        con.createStatement().execute("delete from vss.cluster_sets where dataset_id = " + ds.getID());
        con.createStatement().execute("delete from vss.datapoints where dataset_id = " + ds.getID());
        con.createStatement().execute("delete from vss.datasets where id = " + ds.getID());
        logger.print("Dataset deleted");
    }

    @Override
    public void deleteClusterSet(int csid) throws SQLException {
        con.createStatement().execute("delete from vss.cluster_sets where id = " + csid);
    }

    @Override
    public void deleteDensities(int datasetID, Kernel k) throws SQLException {
        PreparedStatement pst = con.prepareStatement("Delete from vss.densities where dataset_id = ? and kernel = ?");
        pst.setInt(1, datasetID);
        pst.setString(2, k.toString());
        pst.execute();
    }

    @Override
    public void deleteSortedDatapoints(int datasetID, DistanceMeasure dm) throws SQLException {
        PreparedStatement pst = con.prepareStatement("Delete from vss.densities where dataset_id = ? and kernel = ?");
        pst.setInt(1, datasetID);
        pst.setString(2, dm.toString());
        pst.execute();
    }

    @Override
    public void deleteVoronoiCells(int datasetID, DistanceMeasure dm) throws SQLException {
        if (dm != null) {
            PreparedStatement pst = con.prepareStatement("delete from vss.voronoi_cells where dataset_id = ? and distance_measure = ?");
            pst.setInt(1, datasetID);
            pst.setString(2, dm.toString());
            pst.execute();
        } else {
            con.prepareStatement("delete from vss.voronoi_cells where dataset_id = " + datasetID).execute();
        }
    }

    @Override
    public VoronoiCell[] loadVoronoiCells(Dataset ds, DistanceMeasure dm) throws SQLException {
        PreparedStatement pst = con.prepareStatement("Select dp_id, point_count, volume, neighbor_ids from vss.voronoi_cells where dataset_id = ? and distance_measure = ? order by dp_id");
        pst.setInt(1, ds.getID());
        pst.setString(2, dm.toString());
        ResultSet rs = pst.executeQuery();
        VoronoiCell[] res = new VoronoiCell[ds.size()];
        while (rs.next()) {
            int dpid = rs.getInt(1);
            res[dpid] = new VoronoiCell(ds.getDPByID(dpid), rs.getDouble(2), rs.getDouble(3));
            int[] neighborIDs = (int[]) rs.getObject(4);
            for (int i : neighborIDs) {
                res[dpid].neighbors.add(ds.getDPByID(i));
            }
        }
        rs.close();
        return res;
    }

    @Override
    public void saveVoronoiCell(VoronoiCell cell, int datasetID, DistanceMeasure dm) throws SQLException {
        PreparedStatement pst = con.prepareStatement("Select dataset_id, distance_measure, dp_id, point_count, volume, neighbor_ids from vss.voronoi_cells where dataset_id = ? and dp_id = ? and  distance_measure = ?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
        pst.setInt(1, datasetID);
        pst.setInt(2, cell.dp.getID());
        String dms = dm.toString();
        pst.setString(3, dms);
        synchronized (con) {
            try (ResultSet rs = pst.executeQuery()) {
                boolean update = rs.next();

                if (update) {
                } else {
                    rs.moveToInsertRow();
                }
                int i = 1;
                rs.updateInt(i++, datasetID);
                rs.updateString(i++, dms);
                rs.updateInt(i++, cell.dp.getID());
                rs.updateDouble(i++, cell.point_count);
                rs.updateDouble(i++, cell.volume);
                int[] idx = new int[cell.neighbors.size()];
                for (int j = 0; j < idx.length; j++) {
                    idx[j] = cell.neighbors.get(j).getID();
                }
                rs.updateObject(i++, idx);

                if (update) {
                    rs.updateRow();
                } else {
                    rs.insertRow();
                }
                rs.close();
            }
        }
    }

    @Override
    public void saveDistanceMatrix(DistanceMatrix dm) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void shutdown() {
        try {
            con.createStatement().execute("COMMIT;");
            con.createStatement().execute("SHUTDOWN;");
            con.close();

        } catch (Exception e) {
            logger.showException(e);
        }
    }

    @Override
    public DistanceMatrix loadDistanceMatrix(Dataset ds) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void deleteDistanceMatrix(Dataset ds) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void saveSortedDatapoints(int datasetID, Datapoint[] datapoints, DistanceMeasure dm) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Annotation[] loadAnnotations(Dataset ds) throws SQLException {
        PreparedStatement pst = con.prepareStatement("select id from vss.annotations where dataset_id = ?");
        pst.setInt(1, ds.getID());
        List<Integer> lstID;
        try (ResultSet rs = pst.executeQuery()) {
            lstID = new LinkedList<>();
            while (rs.next()) {
                lstID.add(rs.getInt(1));
            }
            rs.close();
        }
        Annotation[] ann = new Annotation[lstID.size()];

        for (int i = 0; i < ann.length; i++) {
            int annID = lstID.get(i);
            ann[i] = loadAnnotation(annID, ds);
        }

        return ann;
    }

    @Override
    public Annotation loadAnnotation(int annotationID, Dataset ds) throws SQLException {

        PreparedStatement pst = con.prepareStatement("select ann_name from vss.annotations where id = ?");
        pst.setInt(1, annotationID);
        ResultSet rs = pst.executeQuery();
        if (!rs.next()) {
            return null;
        }
        String name = rs.getString(1);
        rs.close();

        pst = con.prepareStatement("select dp_id, terms from vss.annotation_members where annotation_id = ?");
        pst.setInt(1, annotationID);
        rs = pst.executeQuery();

        Annotation ann = new Annotation(ds, name);
        ann.setID(annotationID);
        HashMap<String, List<Integer>> hmProfileIDsForTerm = new HashMap<>();

        while (rs.next()) {
            if (rs.getObject(2) != null) {
                String[] terms = (String[]) rs.getObject(2);
                int pid = rs.getInt(1);
                if (terms != null) {
                    for (String t : terms) {
                        if (hmProfileIDsForTerm.get(t) == null) {
                            hmProfileIDsForTerm.put(t, new LinkedList<Integer>());
                        }
                        hmProfileIDsForTerm.get(t).add(pid);
                    }
                }
            }
        }

        for (String t : hmProfileIDsForTerm.keySet()) {
            ann.addTerm(hmProfileIDsForTerm.get(t), t);
        }

        return ann;
    }

    @Override
    public Datapoint[] loadSortedDatapoints(Dataset ds, DistanceMeasure dm) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Datapoint[] loadSortedDatapoints(Dataset ds, DistanceMeasure dm, int limit) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void saveAnnotation(Annotation ann) throws SQLException {
        PreparedStatement pst = con.prepareStatement("Insert into vss.annotations values(?, next value for vss.annotations_id, ?)");
        pst.setInt(1, ann.getBaseDataset().getID());
        pst.setString(2, ann.getAnnotationName());
        pst.executeUpdate();
        ResultSet rs = con.createStatement().executeQuery("Select max(id) from vss.annotations");
        rs.next();
        int id = rs.getInt(1);
        rs.close();
        ann.setID(id);
        //pst = con.prepareStatement("Insert into vss.annotation_members values(" + id + ", ?, ?)");
        PreparedStatement pst2 = con.prepareStatement("insert into vss.annotation_members (annotation_id, dp_id, terms) values (?,?,?)", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        //rs2.next();
        for (Datapoint dp : ann.getBaseDataset().getDatapoints()) {
            pst2.setInt(1, id);
            pst2.setInt(2, dp.getID());
            pst2.setObject(3, ann.getTermsForDpID(dp.getID()));
            pst2.executeUpdate();
        }

    }
}
