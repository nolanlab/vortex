/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.tasks;

import executionslave.ReusableObject;
import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map.Entry;
import clustering.Datapoint;
import clustering.Dataset;
import clustering.DistanceMeasure;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class GabrielNeighborTaskGeneric extends GabrielNeighborTaskAngular {

    private static final long serialVersionUID = 1L;
    private int centerID;
    private Dataset ds;
    private int instanceNum;
    //private final double searchDistLimit = -1;
    private int NNNtoAdd = 0;
    private DistanceMeasure dm;

    public class NeighborInfo implements Serializable {

        private static final long serialVersionUID = 1L;
        public int[] gabrielNeighbors;
        public int[] sortedDatapointIDs;
    }

    @Override
    public void cancel() {
        Thread.currentThread().interrupt();
    }
    /*
     * Returns the set of IDs of gabriel neighbors of sortedUnityLenArrayLists[0] in the array of sortedUnityLenArrayLists
     * localObjects - NDataset;
     */

    public GabrielNeighborTaskGeneric(int dpID, int instanceNum, int NNNtoAdd, DistanceMeasure dm) {
        super(dpID, instanceNum, NNNtoAdd);
        centerID = dpID;
        this.instanceNum = instanceNum;
        this.NNNtoAdd = 0;//NNNtoAdd;
        this.dm = dm.clone();
    }

    @Override
    public void injectReusableObject(ReusableObject localObjects) {
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
            }
        }

        ArrayList<Entry<Datapoint, Double>> sorted = new ArrayList<>();

        for (Datapoint d : dp) {
            double sim = dm.getSimilarity(center, d.getVector());
            sorted.add(new SimpleEntry<>(d, sim));
        }

        Collections.sort(sorted, new Comparator<Entry<Datapoint, Double>>() {
            @Override
            public int compare(Entry<Datapoint, Double> o1, Entry<Datapoint, Double> o2) {
                return (int) Math.signum(o2.getValue() - o1.getValue());
            }
        });

        sorted.remove(0);

        int maxIdx = Math.max(sorted.size(), ds.getNumDimensions() * 100);

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
            /*
             if(y >= NNNtoAdd){
             break;
             }*/

            Datapoint anotherDP = dp[y];
            Datapoint testedDP;
            Datapoint nnOfTheMid = null;

            double[] anotherArrayList = anotherDP.getVector();

            mid = dm.getPrototype(new double[][]{center, anotherArrayList});

            double simBetween = Math.min(dm.getSimilarity(mid, center), dm.getSimilarity(mid, anotherArrayList));
//            assert MatrixOp.mult(mid, center) == Kernel.getEuclideanCosine(new DenseDoubleMatrix1D(mid), new DenseDoubleMatrix1D(center));
            double testerSimilToMid = 0;
            double nnSimToMid = -1;
            for (int i = 0; i < maxIdx + neighbors.size(); i++) {
                if (i < neighbors.size()) {
                    testedDP = neighbors.get(i);
                } else {
                    testedDP = dp[i - neighbors.size()];
                }
                if (testedDP.equals(anotherDP)) {
                    continue;
                }
                testerSimilToMid = dm.getSimilarity(mid, testedDP.getVector());
                if (testerSimilToMid > nnSimToMid) {
                    nnOfTheMid = testedDP;
                    nnSimToMid = testerSimilToMid;
                    if (testerSimilToMid > simBetween) {
                        YisNeighbor = false;
                        break;
                    }
                }
            }

            if (YisNeighbor && nnOfTheMid != null) {
                //double angToMid = 0;
                double a, b, c;
                //int dim = ds.getDimension();
                int j;
                // new double[dim];
                //double shiftVecLen, tga, cosBeta, shift;
                //shift = (Math.acos(cosBetween) / 10.0);
                //weight = shift/angToMid;
                double coeff = 0.05;
                //tga = Math.tan(shift);
                //cosBeta = Math.cos(angToMid);

                //double[] shiftVec = new double[dim];

                //for (j = 0; j < dim; j++) {
                //    shiftVec[j] = nnOfTheMid.getUnityLengthVector()[j] - mid[j] * cosBeta;
                // }

                //  shiftVecLen = MatrixOp.lenght(shiftVec);

                double[] midShiftedToNN = dm.getPrototype(new double[][]{mid, nnOfTheMid.getVector()}, new double[]{1 - coeff, coeff});

                c = dm.getSimilarity(nnOfTheMid.getVector(), midShiftedToNN);
                a = dm.getSimilarity(center, midShiftedToNN);// lenShifted;
                b = dm.getSimilarity(anotherArrayList, midShiftedToNN);

                if (c > a && c > b) {
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

        //logger.print("GabrielNeighborTask -" + dm.toString() +" #" + instanceNum + " ended, found " + filtered.size() + " neighbors");
        int[] res = new int[filtered.size()];
        for (int i = 0; i < filtered.size(); i++) {
            res[i] = filtered.get(i).getID();
        }
        return res;
    }
}
