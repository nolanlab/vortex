/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.tasks;

import sandbox.clustering.Dataset;
import sandbox.clustering.Datapoint;
import java.util.Arrays;
import java.util.Comparator;
import sandbox.clustering.DistanceMeasure;
import util.MatrixOp;

/**
 *
 * @author Nikolay
 */
public class SortingTask {

    private Datapoint[] d;
    private double[] centerD;
    public static final long serialVersionUID = 205L;
    private DistanceMeasure dm;
    private double[] arr;

    public SortingTask(double[] center, DistanceMeasure dm) {
        //this.limit = returnSizeLimit;
        this.dim = center.length;
        this.centerD = center;
        this.dm = dm;

    }

    public void cancel() {
        Thread.currentThread().interrupt();
    }
    double prod;
    int x;
    int dim;

    public void injectReusableObject(Object localObjects) {
        d = ((Dataset) localObjects).getDatapoints();
    }

    /**
     * Returns the array of indices of datapoints in the initial array sorted by
     * proximity to
     * <code>i</code>
     *
     * @param i
     * @return
     */
    public int[] execute() {

        arr = new double[d.length];
        Integer[] idxArr = new Integer[d.length];

        for (int i = 0; i < d.length; i++) {
            arr[i] = dm.getSimilarity(centerD, d[i].getVector());
            idxArr[i] = d[i].getID();
        }

        Arrays.sort(idxArr, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return (int) Math.signum(arr[o2] - arr[o1]);
            }
        });

        centerD = null;
        dm = null;
        arr = null;
        d = null;

        int[] ret = new int[idxArr.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = idxArr[i];
        }
        idxArr = null;
        return ret;
    }
}
