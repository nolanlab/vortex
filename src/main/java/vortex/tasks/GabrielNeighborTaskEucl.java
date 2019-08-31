/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.tasks;

import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map.Entry;
import sandbox.clustering.Datapoint;
import sandbox.clustering.Dataset;
import util.MatrixOp;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class GabrielNeighborTaskEucl extends GabrielNeighborTaskAngular {

    private static final long serialVersionUID = 1L;
    private int centerID;
    private Dataset ds;
    private int instanceNum;
    private int NNNtoAdd = 0;

    public class NeighborInfo implements Serializable {

        private static final long serialVersionUID = 1L;
        public int[] gabrielNeighbors;
        public int[] sortedDatapointIDs;
    }

    /*
     * 
     * localObjects - NDataset;
     */
    public GabrielNeighborTaskEucl(int dpID, int instanceNum, int NNNtoAdd) {
        super(dpID, instanceNum, NNNtoAdd);
        this.centerID = dpID;
        this.NNNtoAdd = 0;
        this.instanceNum = instanceNum;
    }

    public void injectReusableObject(Object localObjects) {
        ds = (Dataset) localObjects;
    }

    @Override
    public int[] execute() {
        double[] mid;
        double[] center = null;
        boolean filter = true;
        Datapoint[] dp = Arrays.copyOf(ds.getDatapoints(), ds.getDatapoints().length);

        ArrayList<Datapoint> neighbors = new ArrayList<>();

        for (Datapoint d : dp) {
            if (d.getID() == centerID) {
                center = d.getVector();
                break;
            }
        }

        //logger.print("GabrielNTaskEucl: (instNum, centerID, centerDP): ", instanceNum, centerID, center);

        ArrayList<Entry<Datapoint, Double>> sorted = new ArrayList<>();

        for (Datapoint d : dp) {
            double dist = MatrixOp.getEuclideanDistance(center, d.getVector());
            sorted.add(new SimpleEntry<>(d, dist));
        }

        Collections.sort(sorted, new Comparator<Entry<Datapoint, Double>>() {
            @Override
            public int compare(Entry<Datapoint, Double> o1, Entry<Datapoint, Double> o2) {
                return (int) Math.signum(o1.getValue() - o2.getValue());
            }
        });

        sorted.remove(0);

        int maxIdx = Math.min(ds.getNumDimensions() * 100, sorted.size());

        dp = new Datapoint[maxIdx];
        for (int i = 0; i < dp.length; i++) {
            dp[i] = sorted.get(i).getKey();
        }

        for (int y = 0; y < maxIdx; y++) {
            boolean YisNeighbor = true;
            if (y < NNNtoAdd) {
                neighbors.add(dp[y]);
                continue;
            }

            Datapoint anotherDP = dp[y];
            Datapoint testedDP;
            Datapoint nnOfTheMid = null;

            double[] anotherArrayList = anotherDP.getVector();

            mid = MatrixOp.sum(center, anotherArrayList);
            MatrixOp.mult(mid, 0.5);

            double distBetween = Math.max(MatrixOp.getEuclideanDistance(mid, center), MatrixOp.getEuclideanDistance(mid, anotherArrayList));
//            assert MatrixOp.mult(mid, center) == Kernel.getEuclideanCosine(new DenseDoubleMatrix1D(mid), new DenseDoubleMatrix1D(center));
            double testedDistToMid;
            double nnDistToMid = Double.MAX_VALUE;
            for (int i = 0; i < maxIdx + neighbors.size(); i++) {
                if (i < neighbors.size()) {
                    testedDP = neighbors.get(i);
                } else {
                    testedDP = dp[i - neighbors.size()];
                }
                if (testedDP.getID() == anotherDP.getID()) {
                    continue;
                }
                testedDistToMid = MatrixOp.getEuclideanDistance(mid, testedDP.getVector());
                if (testedDistToMid < nnDistToMid) {
                    nnOfTheMid = testedDP;
                    nnDistToMid = testedDistToMid;
                    if (testedDistToMid < distBetween) {
                        YisNeighbor = false;
                        break;
                    }
                }
            }
            /*
             if(!YisNeighbor){
             logger.print("not neighbors, IDs: ", nnOfTheMid.getID(), anotherDP.getID(), centerID, cosBetween, nnCosToMid);
             }else{
             logger.print("neighbors", anotherDP.getID(), centerID, cosBetween, nnCosToMid);
             }*/

            if (false && YisNeighbor && nnOfTheMid != null) {

                double a, b, c;
                int dim = ds.getNumDimensions();
                int j;
                double[] midShiftedToNN = new double[dim];

                double shiftWeight = 0.00;
                //weight = shift/angToMid;

                for (j = 0; j < dim; j++) {
                    midShiftedToNN[j] = (mid[j] * (1.0 - shiftWeight)) + (nnOfTheMid.getVector()[j] * shiftWeight);
                }

                a = MatrixOp.getEuclideanDistance(center, midShiftedToNN);// lenShifted;
                b = MatrixOp.getEuclideanDistance(anotherArrayList, midShiftedToNN);
                c = MatrixOp.getEuclideanDistance(nnOfTheMid.getVector(), midShiftedToNN);

                if (c < a || c < b) {
                    YisNeighbor = false;
                }
            }
            if (YisNeighbor) {
                neighbors.add(dp[y]);
            }
        }

        if (neighbors.isEmpty()) {
            logger.print("Zero neighbors!!" + centerID);
        }

        LinkedList<Datapoint> filtered = new LinkedList<>();
        /*
         for (int i = 0; i < ds.getDimension()/2; i++) {        
         filtered.add(dp[i]);
         }*/
        if (filter) {
            for (int i = 0; i < Math.min(neighbors.size() + 1, dp.length); i++) {
                if (neighbors.contains(dp[i])) {
                    filtered.add(dp[i]);
                }
            }
        } else {
            filtered.addAll(neighbors);
        }

        if (filtered.isEmpty()) {
            logger.print("Zero filtered!!" + centerID, neighbors.size());
        }

        //logger.print("GabrielNeighborTaskEucl #" + instanceNum + " ended, found " + filtered.size() + " neighbors");
        int[] res = new int[filtered.size()];
        for (int i = 0; i < filtered.size(); i++) {
            res[i] = filtered.get(i).getID();
        }
        return res;
    }
}
