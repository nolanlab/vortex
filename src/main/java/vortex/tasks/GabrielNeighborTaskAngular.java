/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.tasks;

import executionslave.ReusableObject;
import executionslave.ReusingTask;
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
import util.MatrixOp;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class GabrielNeighborTaskAngular implements ReusingTask<int[]> {

    private static final long serialVersionUID = 1L;
    private int centerID;
    private Dataset ds;
    //private int instanceNum;
    private final double searchDistLimit = -1;
    private int NNNtoAdd = 0;
    double trim_alpha = 0;
    
    boolean NNNfilter=true;
 

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

    public GabrielNeighborTaskAngular(int dpID, int instanceNum, int NNNtoAdd) {
        centerID = dpID;
        //this.instanceNum = instanceNum;
        this.NNNtoAdd = 0;// NNNtoAdd;        this.NNNtoAdd = 0;// NNNtoAdd;

    }

    @Override
    public void injectReusableObject(ReusableObject localObjects) {
        ds = (Dataset) localObjects;
    }

    @Override
    public int[] execute() {
        double[] mid;
        double[] center = null;

        Datapoint[] dp = Arrays.copyOf(ds.getDatapoints(), ds.getDatapoints().length);
        //NNNtoAdd = ds.getDimension()*2;
        ArrayList<Datapoint> neighbors = new ArrayList<>();

        for (Datapoint d : dp) {
            if (d.getID() == centerID) {
                center = d.getUnityLengthVector();
            }
        }

        ArrayList<Entry<Datapoint, Double>> sorted = new ArrayList<>();

        for (Datapoint d : dp) {
            double correl = Math.min(1.0, MatrixOp.mult(center, d.getUnityLengthVector()));
            if (correl > searchDistLimit && correl < 1.0 - 1E-10) {
                sorted.add(new SimpleEntry<>(d, correl));
            }
        }

        Collections.sort(sorted, new Comparator<Entry<Datapoint, Double>>() {
            @Override
            public int compare(Entry<Datapoint, Double> o1, Entry<Datapoint, Double> o2) {
                return (int) Math.signum(o2.getValue() - o1.getValue());
            }
        });

        int maxIdx = Math.min(ds.getNumDimensions() * 25, sorted.size());

        dp = new Datapoint[maxIdx];
        for (int i = 0; i < dp.length; i++) {
            dp[i] = sorted.get(i).getKey();
        }

        for (int y = 0; y < maxIdx; y++) {
            boolean YisNeighbor = true;
            if (y < NNNtoAdd) {
                neighbors.add(dp[y]);
                continue;
            } else {
                //   if(true)break;
            }
            /*
             if(y >= NNNtoAdd){
             break;
             }*/

            Datapoint anotherDP = dp[y];
            Datapoint testedDP;
            Datapoint nnOfTheMid = null;

            double[] anotherArrayList = anotherDP.getUnityLengthVector();

            mid = MatrixOp.toUnityLen(MatrixOp.sum(center, anotherArrayList));

            double cosBetween = Math.min(1.0, Math.min(MatrixOp.mult(mid, center), MatrixOp.mult(mid, anotherArrayList)));
//            assert MatrixOp.mult(mid, center) == Kernel.getEuclideanCosine(new DenseDoubleMatrix1D(mid), new DenseDoubleMatrix1D(center));
            double testedCosToMid = 0;
            double nnCosToMid = -1;
            for (int i = 0; i < maxIdx + neighbors.size(); i++) {
                if (i < neighbors.size()) {
                    testedDP = neighbors.get(i);
                } else {
                    testedDP = dp[i - neighbors.size()];
                }
                if (testedDP.equals(anotherDP)) {
                    continue;
                }
                testedCosToMid = Math.min(1.0, MatrixOp.mult(mid, testedDP.getUnityLengthVector()));
                if (testedCosToMid > nnCosToMid) {
                    nnOfTheMid = testedDP;
                    nnCosToMid = testedCosToMid;
                    if (testedCosToMid > cosBetween) {
                        YisNeighbor = false;
                        break;
                    }
                }
            }

            if (YisNeighbor && nnOfTheMid != null && trim_alpha > 0.000001) {
                //double angToMid = 0;
                double a, b, c;
                int dim = ds.getNumDimensions();
                int j;
                double[] midShiftedToNN = new double[dim];
                //double shiftVecLen, tga, cosBeta, shift;
                //shift = (Math.acos(cosBetween) / 10.0);
                //weight = shift/angToMid;
                //double trim_alpha = 0.000;
                //tga = Math.tan(shift);
                //cosBeta = Math.cos(angToMid);

                //double[] shiftVec = new double[dim];

                //for (j = 0; j < dim; j++) {
                //    shiftVec[j] = nnOfTheMid.getUnityLengthVector()[j] - mid[j] * cosBeta;
                // }

                //  shiftVecLen = MatrixOp.lenght(shiftVec);

                for (j = 0; j < dim; j++) {
                    midShiftedToNN[j] = (mid[j] * (1 - trim_alpha)) + (nnOfTheMid.getUnityLengthVector()[j] * trim_alpha); //(tga * shiftVec[j]) / shiftVecLen;
                }

                c = Math.min(1.0, MatrixOp.mult(nnOfTheMid.getUnityLengthVector(), midShiftedToNN));
                a = Math.min(1.0, MatrixOp.mult(center, midShiftedToNN));// lenShifted;
                b = Math.min(1.0, MatrixOp.mult(anotherArrayList, midShiftedToNN));

                if (c > a || c > b) {
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


        


        /*
         for (int i = 0; i < ds.getDimension()/2; i++) {        
         filtered.add(dp[i]);
         }*/
        if (NNNfilter) {
            LinkedList<Datapoint> filtered = new LinkedList<>();
            for (int i = 0; i < Math.min(neighbors.size() + 1, dp.length); i++) {
                if (neighbors.contains(dp[i])) {
                    filtered.add(dp[i]);
                }
            }
            neighbors.clear();
            neighbors.addAll(filtered);
        }
        if (neighbors.isEmpty()) {
            logger.print("Zero neighbors!!" + centerID, neighbors.size());
        }

        //logger.print("GabrielNeighborTaskAngular ended, found " + filtered.size() + " neighbors");
        int[] res = new int[neighbors.size()];
        for (int i = 0; i < neighbors.size(); i++) {
            res[i] = neighbors.get(i).getID();
        }
        return res;
    }
}
