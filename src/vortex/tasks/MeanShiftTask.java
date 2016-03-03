/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.tasks;

import cern.colt.Arrays;
import executionslave.ReusableObject;
import clustering.Datapoint;
import main.Dataset;
import vortex.clustering.Kernel;
import util.MatrixOp;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class MeanShiftTask implements executionslave.ReusingTask<double[]> {

    public static final long serialVersionUID = 205L;
    double[] center, initCenter;
    Dataset nd;
    private double minimalShiftRatio;
    private int maxNumIterations = Integer.MAX_VALUE;
    private Kernel kernel;

    public MeanShiftTask(double[] center, Kernel kernel, double minShiftRatio, String lbl) {

        this.center = java.util.Arrays.copyOf(center, center.length);
        this.initCenter = MatrixOp.copy(center);
        this.minimalShiftRatio = minShiftRatio;
        this.kernel = kernel.clone();

    }

    public double[] getInitCenter() {
        return initCenter;
    }

    @Override
    public void cancel() {
        Thread.currentThread().interrupt();
    }

    public MeanShiftTask(double[] center, Kernel kernel, int numIterations, String lbl) {
        this.center = java.util.Arrays.copyOf(center, center.length);
        this.initCenter = MatrixOp.copy(center);
        this.minimalShiftRatio = 0.0;
        this.maxNumIterations = numIterations;
        this.kernel = kernel.clone();
    }

    @Override
    public void injectReusableObject(ReusableObject localObject) {
        nd = ((Dataset) localObject);
    }

    @Override
    public double[] execute() {
        //NDatapoint[] dp = nd.getDatapoints();
        double shiftRatio;
        int numCycles = 0;


        // try {
        do {
            double[] nextVec = kernel.getWeightedMean(center, nd.getVectors());
            shiftRatio = kernel.getDistanceMeasure().getDistance(nextVec, center) / kernel.getDistanceMeasure().getDistance(nextVec, initCenter);
            //logger.print("Cycle#"+numCycles + Arrays.toString(nextVec) + Arrays.toString(center));
            numCycles++;
            if (Double.isNaN(shiftRatio)) {
                if (kernel.getDistanceMeasure().getDistance(nextVec, initCenter) == 0) {
                    return center;
                } else {
                    logger.print("Shift is NAN!!!");
                    logger.print(Arrays.toString(nextVec));
                    return initCenter;
                }

                //throw new IllegalStateException("Shift is NaN!!!");
            }
            center = nextVec;
        } while (shiftRatio > minimalShiftRatio && numCycles < maxNumIterations);
        //logger.print("Mean-shift task ended after " + numCycles + " cycles");
        /* } catch (IllegalStateException e) {
         logger.print("Zero weight on Cycle#" + numCycles);

         Entry<NDatapoint, Double> entry = new Optimization<NDatapoint>() {
         @Override
         public double scoringFunction(NDatapoint arg) {
         return kernel.getDistanceMeasure().getDistance(center, arg.getVector());
         }
         }.getArgMax(dp);
         logger.print("Nearest dp: " + entry.getKey() + ", dist " + entry.getValue());
         return center;
         }*/
        //logger.print("Mean Shift Task ready after "+numCycles+" cycles");
        return center;
    }
}
