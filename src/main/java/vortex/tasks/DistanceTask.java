/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.tasks;

import executionslave.ReusableObject;
import java.util.ArrayList;
import java.util.Arrays;
import clustering.Datapoint;
import clustering.Dataset;
import clustering.DistanceMeasure;

/**
 *
 * @author Nikolay
 */
public class DistanceTask implements executionslave.ReusingTask<double[]> {

    private Datapoint[] ArrayLists;
    private DistanceMeasure dm;
    public static final long serialVersionUID = 205L;
    double[] center;
    int from;
    int to;

    public DistanceTask(double[] center, int from, int to, DistanceMeasure dm) {
        //this.i = i;
        this.center = Arrays.copyOf(center, center.length);
        this.from = from;
        this.to = to;
        this.dm = dm;
    }

    @Override
    public void cancel() {
        Thread.currentThread().interrupt();
    }

    @Override
    public void injectReusableObject(ReusableObject localObjects) {
        ArrayLists = ((Dataset) localObjects).getDatapoints();
    }

    /**
     * Returns the array of distances
     * <code>i</code>
     *
     * @param i
     * @return
     */
    @Override
    public double[] execute() {
        double[] arr = new double[to - from];
        double dist = 0;
        //double[] vecI = Arrays.copyOf(ArrayLists[i].getVector(), ArrayLists[i].getVector().length);
        double[] vecY = null;

        for (int y = from; y < to; y++) {
            dist = 0;
            vecY = ArrayLists[y].getVector();
            dist = dm.getDistance(vecY, center);
            arr[y - from] = dist;
        }
        return arr;
    }
}
