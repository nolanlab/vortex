/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.tasks;

import vortex.clustering.Kernel;
import executionslave.ReusableObject;
import java.io.Serializable;
import java.math.BigDecimal;
import main.Dataset;

/**
 *
 * @author Nikolay
 */
public class DensityTask implements executionslave.ReusingTask<BigDecimal> {

    protected Dataset dataset;
    protected final double[] center;
    public static final long serialVersionUID = 212L;
    protected Kernel kernel;
    protected int[] nIDX;
    double precision;

    @Override
    public void injectReusableObject(ReusableObject localObjects) {
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

    @Override
    public void cancel() {
        Thread.currentThread().interrupt();
    }

    @Override
    public BigDecimal execute() {
        double[][] vectors = dataset.getVectors();
        BigDecimal dens = kernel.getDensity(center, vectors);
        dataset = null;
        vectors = null;
        return dens;
    }
}
