/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustering;

import clustering.DistanceMeasure;
import clustering.Cluster;
import clustering.Dataset;
import clustering.Datapoint;

import java.util.ArrayList;
import vortex.gridengine.GridEngine;
import vortex.tasks.MeanShiftTask;
import util.logger;

/**
 * @author Nikolay
 */
public class MeanShiftClusteringCore extends KernelClusteringCore {

    @Override
    public String getAlgorithmName() {
        return "Mean-Shift";
    }

    @Override
    public String getAlgorithmDescription() {
        return "Mean-shift iteratively shift datapoints against the density gradient until they converge to modes forming clusters.\nConvergence is determined by similarity threshold";
    }

    @Override
    public boolean isAlgorithmPublic() {
        return true;
    }

    public MeanShiftClusteringCore(DistanceMeasure dm) {
        super(dm);
        if (!dm.supportsPrototyping()) {
            throw new IllegalArgumentException("This method requires a distance measure that supports prototyping");
        }
    }

    /**
     * @param ds - a dataset to be clustered
     * @param kernel - kernel used for density estimate
     * @return
     */
    @Override
    public Cluster[] doClustering(final Dataset ds, Kernel kernel, double mergeThreshold) {
        //AngularDistance ad = new AngularDistance();

        GridEngine engine = GridEngine.getInstance();

        Datapoint[] dp = ds.getDatapoints();
        MeanShiftTask[] tasks = new MeanShiftTask[dp.length];

        for (int i = 0; i < tasks.length; i++) {
            tasks[i] = new MeanShiftTask(dp[i].getVector(), kernel, 0.0001, dp[i].getFullName());
        }

        double[][] result = new double[dp.length][];
        
        
//        if(!GridEngine.getInstance().waitForCompletion(engine.submitBatch(tasks, result, null, ds)))return null;


        /*
         for (int i = 0; i < result.length; i++) {
         logger.print(ds.getDatapointByID(i).getName() + " " + Arrays.toString(result[i]));
         }*/

        logger.print("Fetching clusters:");
        ArrayList<Cluster> clusters = new ArrayList<>();
        int size = result.length;
        int x = 0;
        do {
            x = 0;
            while (x < size) {
                if (result[x] != null) {
                    break;
                } else {
                    x++;
                }
            }
            if (x < size) {
                double[] cent = result[x];
                //logger.print(cent);
                ArrayList<Datapoint> cluster = new ArrayList<>();
                for (int y = x; y < size; y++) {
                    if (result[y] != null) {
                        if (kernel.getDistanceMeasure().getSimilarity(cent, result[y]) > mergeThreshold) {
                            cluster.add(dp[y]);
                            result[y] = null;
                        }
                    }
                }
                if (cluster.size() > 1) {
                    double[][] sidevec = new double[cluster.size()][];
                    for (int i = 0; i < sidevec.length; i++) {
                        sidevec[i] = cluster.get(i).getSideVector();
                    }
                    clusters.add(new Cluster(cluster.toArray(new Datapoint[cluster.size()]), cent, kernel.getDistanceMeasure().getPrototype(sidevec), "", kernel.getDistanceMeasure()));
                }
            }
        } while (x < size);
        logger.print(clusters.size() + " cluster");

        return clusters.toArray(new Cluster[clusters.size()]);
    }
}
