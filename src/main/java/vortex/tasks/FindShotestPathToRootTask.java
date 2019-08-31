/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import sandbox.clustering.Datapoint;
import sandbox.clustering.Dataset;
import util.SparseDoubleMatrix;

/**
 *
 * @author Nikolay
 */
public class FindShotestPathToRootTask  {

    private static final long serialVersionUID = 1L;
    SparseDoubleMatrix dwg;
    Datapoint root;
    Dataset ds;

    public FindShotestPathToRootTask(Datapoint root) {
        this.root = root;
    }

    public void cancel() {
        Thread.currentThread().interrupt();
    }

    public void injectReusableObject(Object localObjects) {
        dwg = null;//((ClusteringGraphReusableObject) localObjects).getGraph();
        ds = null;//((ClusteringGraphReusableObject) localObjects).getDataset();
    }

    private Queue<Integer> incomingEdgesOf(SparseDoubleMatrix mtx, int i) {

        return mtx.getNonemptyRowsForColumn(i);
    }

    public double[] execute() {

        HashMap<Datapoint, Double> hmShortestPathWeights = new HashMap<>();
        ArrayList<Datapoint> sourcesQueue = new ArrayList<>();

        sourcesQueue.add(root);
        hmShortestPathWeights.put(root, 0.0);

        do {
            ArrayList<Datapoint> nextSourcesQueue = new ArrayList<>();
            for (Datapoint currDP : sourcesQueue) {
                Double currShortestDist = hmShortestPathWeights.get(currDP);
                Queue<Integer> incoming = incomingEdgesOf(dwg, currDP.getID());
                if (incoming != null) {
                    for (Integer childIDX : incoming) {
                        Datapoint nextDp = ds.getDPByID(childIDX);
                        if (hmShortestPathWeights.get(nextDp) == null) {
                            nextSourcesQueue.add(nextDp);
                            hmShortestPathWeights.put(nextDp, currShortestDist + dwg.get(childIDX, currDP.getID()));
                        } else {
                            hmShortestPathWeights.put(nextDp, Math.min(currShortestDist + dwg.get(childIDX, currDP.getID()), hmShortestPathWeights.get(nextDp)));
                        }
                    }
                }
            }
            sourcesQueue = nextSourcesQueue;
        } while (!sourcesQueue.isEmpty());

        double[] shortestPaths = new double[ds.getDatapoints().length];

        for (int i = 0; i < ds.getDatapoints().length; i++) {
            Double sp = hmShortestPathWeights.get(ds.getDatapoints()[i]);
            shortestPaths[i] = (sp == null) ? Double.POSITIVE_INFINITY : sp;
        }
        return shortestPaths;
    }
}
