/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.tasks;

import vortex.clustering.Kernel;
import java.io.Serializable;
import java.math.BigDecimal;
import sandbox.clustering.Dataset;

/**
 *
 * @author Nikolay
 */
public class DensityTask {

    protected Dataset dataset;
    protected final double[] center;
    public static final long serialVersionUID = 212L;
    protected Kernel kernel;
    protected int[] nIDX;
    double precision;

    public void injectReusableObject(Object localObjects) {
        dataset = (Dataset) localObjects;
    }

    public DensityTask(double[] center, Kernel kernel) {
        if (!(kernel instanceof Serializable)) {
            throw new IllegalArgumentException("The kernel provided is not serializable");
        }
        this.kernel = kernel.clone();
        this.center = center;
    }

    public DensityTask(double[] center, Kernel kernel, int[] neighborhoodIndex, double precision) {
        if (!(kernel instanceof Serializable)) {
            throw new IllegalArgumentException("The kernel provided is not serializable");
        }
        this.kernel = kernel.clone();
        this.center = center;
        nIDX = neighborhoodIndex;
        this.precision = precision;
    }

    public void cancel() {
        Thread.currentThread().interrupt();
    }

    public BigDecimal execute() {
        double[][] ArrayLists = dataset.getVectors();
        BigDecimal dens = kernel.getDensity(center, ArrayLists);
        dataset = null;
        ArrayLists = null;
        return dens;
    }
}
