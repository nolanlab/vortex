/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import sandbox.clustering.Datapoint;
import sandbox.clustering.Dataset;
import sandbox.clustering.DistanceMeasure;

/**
 *
 * @author Nikolay
 */
public class DistanceTask  {

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

    public void cancel() {
        Thread.currentThread().interrupt();
    }

    public void injectReusableObject(Object localObjects) {
        ArrayLists = ((Dataset) localObjects).getDatapoints();
    }

    /**
     * Returns the array of distances
     * <code>i</code>
     *
     * @param i
     * @return
     */
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
