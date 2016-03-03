/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustering;

import clustering.DistanceMeasure;
import clustering.ClusterSet;
import clustering.Cluster;
import vortex.main.Bessel;
import main.Dataset;
import vortex.main.VoronoiCell;
import clustering.Datapoint;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.AbstractMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import samusik.glasscmp.GlassComboBox;
import vortex.gridengine.GridEngine;
import vortex.tasks.ClusteringGraphReusableObject;
import vortex.tasks.FindShotestPathToRootTask;
import util.DefaultEntry;
import util.MatrixOp;
import util.ProfileAverager;
import util.SparseDoubleMatrix;
import util.logger;

/**
 * @author Nikolay
 */
public class NGLiDE_ClusteringCore extends KernelClusteringCore {

    /**
     *
     * @return - Algorithm name
     */
    @Override
    public String getAlgorithmName() {
        return "NGLiDE";
    }
    private GlassComboBox cmb = new GlassComboBox();

    @Override
    public ClusterSet[] doBatchClustering(Dataset ds, String comment) {
        ClusterSet[] cs = super.doBatchClustering(ds, comment);
        // Prefetch.getInstance().clearVoronoiCellCache();
        return cs; //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public JPanel getParamControlPanel() {
        cmb.setModel(new DefaultComboBoxModel(new FilteringMode[]{FilteringMode.INCLUSIVE_NNN_FILTERING, FilteringMode.NO_FILTERING, FilteringMode.EXCLUSIVE_NNN_FILTERING}));
        JPanel paramPanel = new JPanel();
        paramPanel.setLayout(new GridBagLayout());
        paramPanel.add(super.getParamControlPanel(), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        JLabel lbl = new JLabel("Gabriel graph filtering (controls the maximum degree of fragmentation):");
        lbl.setForeground(new Color(0, 51, 102));
        paramPanel.add(lbl, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));

        paramPanel.add(cmb, new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
        cmb.setMinimumSize(new Dimension(160, 27));
        cmb.setMaximumSize(new Dimension(500, 27));
        cmb.setPreferredSize(new Dimension(160, 27));
        paramPanel.setOpaque(false);
        return paramPanel;
    }

    private FilteringMode getFilteringMode() {
        return (FilteringMode) cmb.getItemAt(cmb.getSelectedIndex());
    }

    enum FilteringMode {

        NO_FILTERING, INCLUSIVE_NNN_FILTERING, EXCLUSIVE_NNN_FILTERING;

        @Override
        public String toString() {
            switch (this) {
                case NO_FILTERING:
                    return "No filtering (least stringent)";

                case INCLUSIVE_NNN_FILTERING:
                    return "Inclusive nearest neighbor filtering (medium stringent)";

                case EXCLUSIVE_NNN_FILTERING:
                    return "Exclusive nearest neighbor filtering (most stringent)";
                default:
                    return "";
            }
        }
    }

    @Override
    public String getAlgorithmDescription() {
        return "NGLiDE stands for Neighborhood Graph Linkage of Density Estimate. It is an advanced density-based clustering algorithm that is tailored for multidimensional data";
    }

    @Override
    public boolean isAlgorithmPublic() {
        return false;
    }

    public NGLiDE_ClusteringCore(DistanceMeasure dm) {
        super(dm);
        if (!dm.supportsPrototyping()) {
            throw new IllegalArgumentException("This method requires a distance measure that supports prototyping");
        }
    }
    //private final boolean MUTUAL_NEIGHBORHOOD_REQURED = false;

    /**
     * Clustering algorithm that builds trees of NDatapoints, assigning every
     * NDatapoint to a nearest neighbour having higher density, giving that in
     * the middle between /* the point and the neighbor the density is higher
     * than in the nearest point. /*
     */
    private double getvMFDens(double[] center, double[][] vectors, double bandwidth) {
        int dim = center.length;
        double NC = Math.pow(bandwidth, (dim / 2) - 1) / (Math.pow(Math.PI * 2, dim / 2) * Bessel.bessiN((dim / 2) - 1, bandwidth));
        double dens = 0;
        for (double[] vec : vectors) {
            // logger.print("Weight", kernel.getValue(vec), getvMFWeight(center, vec, bandwidth)*NC);
            dens += getvMFWeight(center, vec, bandwidth);
        }
        dens *= NC / (double) vectors.length;
        return dens;
    }

    private double getvMFWeight(double[] vec1, double[] vec2, double bandwidth) {
        return Math.exp(bandwidth * MatrixOp.getEuclideanCosine(vec1, vec2));
    }
    private int completionCount;

    private synchronized void swCompleted() {
        completionCount++;
    }

    private class EdgeInfo {

        Datapoint source;
        Datapoint target;
        double weight;

        public EdgeInfo(Datapoint source, Datapoint target, double weight) {
            this.source = source;
            this.target = target;
            this.weight = weight;
        }
    }

    @Override
    public Cluster[] doClustering(Dataset ds, Kernel kernel, double mergeThreshold) {
        //Prefetch prefetch = Prefetch.getInstance();
        final Datapoint[] dataset = ds.getDatapoints();
        final int len = dataset.length;

        final ArrayList<Datapoint> roots = new ArrayList<>();
        /*
         @SuppressWarnings("unchecked")
         final DirectedWeightedMultigraph<NDatapoint, DefaultWeightedEdge> dwg = new DirectedWeightedMultigraph<NDatapoint, DefaultWeightedEdge>(DefaultWeightedEdge.class);

         for (NDatapoint f : dataset) {
         dwg.addVertex(f);
         }*/


        logger.print("Getting densities:");

        final HashMap<Datapoint, Double> hashDensities = new HashMap<>();
        double[] dens = null;// prefetch.getDensities(ds, kernel);

        for (int x = 0; x < len; x++) {
            if (dens[x] == 0) {
                logger.print("NULL DENSITY! " + dataset[x]);
            }
            hashDensities.put(dataset[x], dens[x]);
        }

        //HashMap<NDatapoint, VoronoiCell> hmVC = prefetch.getVoronoiCells(ds, 0, 0, false);

        logger.print("Fetching neighbors");
        final VoronoiCell[] VTarray = null;//prefetch.getVoronoiCells(ds, dm);

        logger.print("Assigning to neighbors");

        //final List<EdgeInfo> edges = new LinkedList<>();


        final SparseDoubleMatrix linkMtx = new SparseDoubleMatrix(len, len);
        //int cnt = 0;

        // try{ BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\out\\GWS Controls DS Gabriel Graph Filtered_noTrimming.txt"));

        int submittedCount = 0;
        completionCount = 0;
//        final double maxGrad = 1.000000;

        for (int x = 0; x < len; x++) {

            final double currDens = hashDensities.get(dataset[x]);

            final ArrayList<Datapoint> neighbors = VTarray[x].neighbors;





            final Datapoint currDp = dataset[x];

            SwingWorker<Datapoint, Datapoint> sw = new SwingWorker<Datapoint, Datapoint>() {
                @Override
                protected void done() {
                    super.done();
                    swCompleted();
                }

                @Override
                protected Datapoint doInBackground() throws Exception {
                    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

                    Datapoint parent = null;
                    for (int i = 0; i < neighbors.size(); i++) {
                        Datapoint neigh = neighbors.get(i);
                        if (neigh.equals(currDp)) {
                            throw new IllegalStateException("Self is a neighbor! Chech the neighborhood graph computation");
                        }

                        //REQUIRING MUTUTAL NEIGHBORHOOD
                        if (getFilteringMode().equals(FilteringMode.EXCLUSIVE_NNN_FILTERING)) {
                            if (!VTarray[neigh.getID()].neighbors.contains(currDp)) {
                                continue;
                            }
                        }

                        synchronized (NGLiDE_ClusteringCore.this) {
                            if (hashDensities.get(neigh) > currDens) {
                                linkMtx.set(currDp.getID(), neigh.getID(), dm.getDistance(neigh.getVector(), currDp.getVector()));
                                if (parent == null) {
                                    //logger.print("point #" + currDp.getID() + "is not a root");
                                    parent = neigh;
                                }
                            } else {
                                linkMtx.set(neigh.getID(), currDp.getID(), dm.getDistance(neigh.getVector(), currDp.getVector()));
                            }
                        }
                    }
                    if (parent == null) {
                        roots.add(currDp);
                        logger.print("Point# " + currDp.getID() + ", " + currDp.getName() + " is a root #" + (roots.size()));
                    }
                    return currDp;
                }
            };
            sw.execute();
            submittedCount++;
        }

        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                logger.showException(ex);
            }
            logger.print(completionCount + "/" + submittedCount);
        } while (completionCount < len);
        /*
         logger.print("Adding edges to graph");
         long time =  Calendar.getInstance().getTimeInMillis();
         for (EdgeInfo ei : edges) {
         if(!dwg.containsEdge(ei.target, ei.source)){
         DefaultWeightedEdge edge = dwg.addEdge(ei.source, ei.target);
         dwg.setEdgeWeight(edge, ei.weight);
         }
         }*/
        /*
         logger.print("Adding edges to the matrix");
         for (EdgeInfo ei : edges) {
         if (linkMtx.get(ei.source.getID(), ei.target.getID()) == 0 && linkMtx.get(ei.target.getID(), ei.source.getID()) == 0) {
         linkMtx.set(ei.source.getID(), ei.target.getID(), ei.weight);
         }
         }*/

        logger.print("Finished adding edges");
        //time = Calendar.getInstance().getTimeInMillis();


        final ArrayList<Datapoint> filteredRoots = new ArrayList<>();
        final ArrayList<Datapoint> cRoots = roots;
        for (final Datapoint d : roots) {

            double maxSim = 0;

            Datapoint nn = null;

            for (Datapoint arg : roots) {
                if (!arg.equals(d)) {
                    double sim = dm.getSimilarity(d.getVector(), arg.getVector());
                    if (sim > maxSim) {
                        maxSim = sim;
                        nn = arg;
                    }
                }
            }
            if (maxSim > mergeThreshold) {
                Datapoint parent = nn;
                if (hashDensities.get(parent) > hashDensities.get(d)) {
                    linkMtx.set(d.getID(), parent.getID(), dm.getDistance(d.getVector(), parent.getVector()));
                } else {
                    filteredRoots.add(d);
                }
                /* DefaultWeightedEdge edge = dwg.addEdge(d, parent);
                 dwg.setEdgeWeight(edge, dm.getDistance(d.getVector(), parent.getVector()));*/
            } else {
                filteredRoots.add(d);
            }

        }
        roots.clear();
        roots.addAll(filteredRoots);

        logger.print("Fetching clusters: ");

        ArrayList<Cluster> clusters = new ArrayList<>();

        final HashMap< Datapoint, ArrayList<Entry<Datapoint, Double>>> clusterMembers = new HashMap<>();

        double[][] shPathsLen = new double[roots.size()][];

        FindShotestPathToRootTask[] tasks = new FindShotestPathToRootTask[roots.size()];

        for (int i = 0; i < tasks.length; i++) {
            clusterMembers.put(roots.get(i), new ArrayList<Entry<Datapoint, Double>>());
            tasks[i] = new FindShotestPathToRootTask(roots.get(i));
        }

        GridEngine engine = GridEngine.getInstance();

        ClusteringGraphReusableObject struct = new ClusteringGraphReusableObject(roots, linkMtx, ds);

        logger.print("Finding shortest paths to the root");
        Integer id = engine.submitBatch(tasks, shPathsLen, null, struct);

        GridEngine.getInstance().waitForCompletion(id);
        logger.print("Shortest path search complete");
        engine.deleteReusableObject(id);

        for (int i = 0; i < dataset.length; i++) {
            int maxIDX = -1;
            double minLen = Double.POSITIVE_INFINITY;
            //logger.print(dataset[i]);
            for (int j = 0; j < tasks.length; j++) {
                //   logger.print(shPathsLen[j][i], shPathsLen[j][i] < minLen);
                if (shPathsLen[j][i] < minLen) {
                    minLen = shPathsLen[j][i];
                    maxIDX = j;
                    // logger.print(maxIDX);
                }
            }
            if (maxIDX >= 0) {
                if (!roots.contains(dataset[i])) {
                    clusterMembers.get(roots.get(maxIDX)).add(new AbstractMap.SimpleEntry<>(dataset[i], 1.0));
                }
            } else {
                logger.print("no shortest paths: " + dataset[i], "Is contained in roots: " + roots.contains(dataset[i]));
                for (int j = 0; j < tasks.length; j++) {
                    logger.print(shPathsLen[j][i]);
                }
            }
        }

        /*
         for (int i = 0; i < dataset.length; i++) {
         if(roots.contains(dataset[i])){
         continue;
         }
         NDatapoint closestRoot = null;
         double minDist = Double.MAX_VALUE;
            
         ConnectivityInspector<NDatapoint, DefaultWeightedEdge> ci = new ConnectivityInspector<NDatapoint, DefaultWeightedEdge>(dwg);
            
         roots: for (NDatapoint root : roots) {
                                
         org.jgrapht.alg.DijkstraShortestPath<NDatapoint, DefaultWeightedEdge> sp =new DijkstraShortestPath<NDatapoint, DefaultWeightedEdge>(dwg, dataset[i], root);
                
         List<DefaultWeightedEdge> path =  sp.getPathEdgeList();
         if(path == null){
         continue;
         }                
         double dist = 0;
         NDatapoint prev = dataset[i];
         for (DefaultWeightedEdge edge : path) {
                    
         if(dwg.getEdgeSource(edge).equals(prev)){
         dist += dwg.getEdgeWeight(edge);
         prev = dwg.getEdgeTarget(edge);
         }else{
         continue roots;  
         }
         }
         //dist = Math.acos(Kernel.getEuclideanCosine(root.getVector(), dataset[i].getVector()));
         if(dist < minDist ){
         minDist = dist;
         closestRoot = root;
         }
         }
         NDatapoint chosenRoot = closestRoot;
         if(chosenRoot!= null){
         clusterMembers.get(chosenRoot).add(new DefaultEntry<NDatapoint, Double>(dataset[i], 1.0));
         }
         }
         */

        for (Datapoint root : roots) {
            ArrayList<Entry<Datapoint, Double>> alCl = clusterMembers.get(root);
            if (alCl != null) {
                alCl.add(new DefaultEntry<>(root, 1.0));
                @SuppressWarnings("unchecked")
                Entry<Datapoint, Double>[] dpMembership = alCl.toArray(new Entry[alCl.size()]);
                //AngularDistance dm = new AngularDistance();

                double[][] clusterVectors = new double[dpMembership.length][];

                ProfileAverager pa = new ProfileAverager();

                for (int i = 0; i < clusterVectors.length; i++) {
                    clusterVectors[i] = dpMembership[i].getKey().getVector();
                    pa.addProfile(dpMembership[i].getKey().getSideVector());
                }
                if (clusterVectors.length > 0) {
                    double[] rtVec = root.getVector();
                    try {
                        rtVec = kernel.getWeightedMean(root.getVector(), clusterVectors);
                    } catch (Exception e) {
                    }
                    final double[] refinedRoot = rtVec;
                    Arrays.sort(clusterVectors, new Comparator<double[]>() {
                        @Override
                        public int compare(double[] o1, double[] o2) {
                            return (int) Math.signum(dm.getDistance(refinedRoot, o1) - dm.getDistance(refinedRoot, o2));
                        }
                    });

                    Cluster c = new Cluster(dpMembership, refinedRoot, pa.getAverage(), root.getName(), dm);
                    clusters.add(c);
                }
            }
        }
        return clusters.toArray(new Cluster[clusters.size()]);
    }
}
