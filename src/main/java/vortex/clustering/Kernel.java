/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustering;

import sandbox.clustering.DistanceMeasure;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import sandbox.clustering.Dataset;

/**
 *
 * @author Nikolay
 */
public abstract class Kernel implements Serializable {

    public abstract String getName();

    public abstract String getDescription();

    public abstract boolean isPublic();
    protected DistanceMeasure dm;
    protected double bandwidth;
    protected boolean ready;

    public DistanceMeasure getDistanceMeasure() {
        return dm;
    }

    public static Kernel[] getAvailableInstances(DistanceMeasure dm) {
        ArrayList<Kernel> al = new ArrayList<>();
        al.add(new GaussianKernel(dm));
        al.add(new vMFKernel(dm));
        al.add(new NNNKernel(dm));
        return al.toArray(new Kernel[al.size()]);
    }

    public void setBandwidth(double bandwidth) {
        this.bandwidth = bandwidth;
    }

    public double getBandwidth() {
        return bandwidth;
    }

    /**
     *
     * @return the name of the free parameter for this kernel
     */
    public abstract String freeParameterName();

    /**
     *
     * @return whether the kernel resolution increases with the free parameter
     * value
     */
    public abstract boolean isResolutionAscending();

    public abstract void init(Dataset nd, double bandwidth);

    public abstract double[] getWeightedMean(double[] center, double[][] ArrayLists);

    public Kernel(DistanceMeasure distMeasure) {
        this.dm = distMeasure;
        ready = false;
    }

    protected abstract double getNonNormalizedValue(double[] center, double[] ArrayList);

    protected abstract double getNormalizationConst(double[] center);

    public abstract BigDecimal getDensity(double[] center, double[][] ArrayLists);

    public Kernel() {
    }

    @Override
    public abstract Kernel clone();

    public abstract void fitBandwidth(double[] center, double[][] ArrayLists);
}
