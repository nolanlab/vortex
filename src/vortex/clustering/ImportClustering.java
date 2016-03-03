/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustering;

import clustering.ClusteringCore;
import clustering.DistanceMeasure;
import java.util.ArrayList;
import java.util.Map.Entry;
import javax.swing.JPanel;
import clustering.Cluster;
import clustering.ClusterSet;
import clustering.Datapoint;
import java.sql.SQLException;
import main.Dataset;
import util.MatrixOp;
import util.logger;
import vortex.util.ConnectionManager;


/**
 *
 * @author Nikolay
 */
public class ImportClustering extends ClusteringCore {

    ImportClusteringParamPanel pan;

    @Override
    public ClusterSet[] doBatchClustering(Dataset ds, String comment) {
        Entry<String, ArrayList<String[]>>[] lists = pan.getProfileIDLists();
        ClusterSet[] css = new ClusterSet[lists.length];
        int batchID = 0;
        try{
            ConnectionManager.getStorageEngine().getNextClusterSetBatchID(ds.getID());
        }catch(SQLException e){
            logger.showException(e);
        }
        for (int i = 0; i < css.length; i++) {

            Entry<String, ArrayList<String[]>> list = lists[i];

            ArrayList<String[]> pidLists = list.getValue();
            Cluster[] clusters = new Cluster[pidLists.size()];
            for (int k = 0; k < pidLists.size(); k++) {
                String[] pids = pidLists.get(k);
                ArrayList<Datapoint> alDP = new ArrayList<Datapoint>();

                double[][] vec = new double[pids.length][];
                double[][] sidevec = new double[pids.length][];
                for (int j = 0; j < pids.length; j++) {
                    Datapoint dp = ds.getDPbyName(pids[j]);
                    if (dp == null) {
                    try{
                        dp = ds.getDatapointByID(Integer.parseInt(pids[j]));
                    }catch(NumberFormatException|ArrayIndexOutOfBoundsException e){
                        
                    }
                    }
                    if (dp == null) {
                        logger.print("ProfileID not found: " + pids[j]);
                    } else {
                        alDP.add(dp);
                        vec[j] = dp.getVector();
                        sidevec[j] = dp.getSideVector();
                    }
                }
                double[] center = MatrixOp.toUnityLen(dm.getPrototype(vec));
                if (alDP.size() > 0) {
                    clusters[k] = new Cluster(alDP.toArray(new Datapoint[alDP.size()]), center, MatrixOp.toUnityLen(dm.getPrototype(sidevec)), "", dm);
//logger.print(clusters[k], clusters[k].getSize());
                }
            }
            double val = 0;
            try {
                val = Double.parseDouble(list.getKey());
            } catch (NumberFormatException e) {
            }
            css[i] = new ClusterSet(batchID, ds, clusters, dm, "Imported " + pan.getClusteringMethodName(), list.getKey(), val, comment);
        }
        return css;
    }

    public ImportClustering(DistanceMeasure dm) {
        super(dm);
        if (!dm.supportsPrototyping()) {
            throw new IllegalArgumentException("This clustering method requires distance measures that support prototyping");
        }
        this.pan = new ImportClusteringParamPanel();
    }

    @Override
    public boolean isAlgorithmPublic() {
        return true;
    }

    @Override
    public String getAlgorithmDescription() {
        return "Import clustering results from an external source";
    }

    @Override
    public JPanel getParamControlPanel() {
        return pan;
    }

    @Override
    public String getAlgorithmName() {
        return "Import Clustering";
    }
}
