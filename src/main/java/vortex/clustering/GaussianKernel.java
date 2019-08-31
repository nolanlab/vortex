/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustering;

import sandbox.clustering.DistanceMeasure;
import sandbox.clustering.EuclideanDistance;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import java.math.BigDecimal;
import java.math.MathContext;
import sandbox.clustering.Dataset;
import umontreal.iro.lecuyer.probdistmulti.MultiNormalDist;
import util.MatrixOp;
import util.logger;


/**
 *
 * @author Nikolay
 */
public class GaussianKernel extends Kernel {
    // private NDataset ds;

    private double normConst = -1;

    @Override
    public GaussianKernel clone() {
        if (!ready) {
            throw new IllegalStateException("This kernel has to be init() first");
        }
        GaussianKernel newKernel = new GaussianKernel(dm.clone());
        newKernel.bandwidth = bandwidth;
        if (this.sigma != null) {
            newKernel.sigma = new double[sigma.length][];

            for (int i = 0; i < sigma.length; i++) {
                newKernel.sigma[i] = MatrixOp.copy(sigma[i]);
            }
        }
        if (this.invSigma != null) {
            newKernel.invSigma = Algebra.DEFAULT.inverse(new DenseDoubleMatrix2D(sigma)).toArray();
        }
        newKernel.normConst = normConst;
        newKernel.ready = true;
        return newKernel;
    }

    @Override
    public void setBandwidth(double bandwidth) {
        if (sigma != null) {
            this.bandwidth = bandwidth;
            sigma = new double[sigma.length][sigma.length];
            for (int i = 0; i < sigma.length; i++) {
                sigma[i][i] = bandwidth * bandwidth;
            }
            invSigma = Algebra.DEFAULT.inverse(new DenseDoubleMatrix2D(sigma)).toArray();
            normConst = normConst = 1 / Math.sqrt(Math.pow(2 * Math.PI, sigma.length) * Algebra.DEFAULT.det(new DenseDoubleMatrix2D(sigma)));
            this.ready = true;
        }
    }

    @Override
    public String freeParameterName() {
        return "Sigma";
    }

    @Override
    public boolean isResolutionAscending() {
        return false;
    }
    double[][] sigma;
    double[][] invSigma;

    @Override
    public boolean isPublic() {
        return true;
    }

    @Override
    public double[] getWeightedMean(double[] center, double[][] ArrayLists) {
        double avgVec[] = new double[center.length];
        for (int i = 0; i < avgVec.length; i++) {
            avgVec[i] = 0;
        }
        double sumWeight = 0;
        for (double[] d : ArrayLists) {
            double w = getNonNormalizedValueCov(center, d); // MultiNormalDist.density(center, sigma, d);
            //logger.print(w/MultiNormalDist.density(center, sigma, d));
            for (int i = 0; i < d.length; i++) {
                avgVec[i] += d[i] * w;
            }
            sumWeight += w;
        }
        double[] res = new double[center.length];
        for (int i = 0; i < avgVec.length; i++) {
            try {
                res[i] = avgVec[i] / sumWeight;
            } catch (ArithmeticException e) {
                logger.print("Arithmetic ex");
            }
        }

        return res;
    }

    @Override
    public void init(Dataset nd, double bandwidth) {
        this.bandwidth = bandwidth;
        sigma = new double[nd.getNumDimensions()][nd.getNumDimensions()];
        for (int i = 0; i < sigma.length; i++) {
            sigma[i][i] = bandwidth * bandwidth;
        }
        invSigma = Algebra.DEFAULT.inverse(new DenseDoubleMatrix2D(sigma)).toArray();
        normConst = normConst = 1 / Math.sqrt(Math.pow(2 * Math.PI, nd.getNumDimensions()) * Algebra.DEFAULT.det(new DenseDoubleMatrix2D(sigma)));
        this.ready = true;
    }

    public GaussianKernel(DistanceMeasure distMeasure) {
        super(distMeasure);
        if (!EuclideanDistance.class.isAssignableFrom(distMeasure.getClass())) {
            throw new IllegalArgumentException("This kernel works only with Euclidean distance");
        }
    }

    @Override
    public void fitBandwidth(double[] center, double[][] ArrayLists) {
        double avg = 0;
        for (double[] vec : ArrayLists) {
            avg = Math.max(avg, MatrixOp.getEuclideanDistance(center, vec));
        }
        //avg /= ArrayLists.length;
        this.bandwidth = avg / 1.96; //FWHM rule
        this.invSigma = null;
        this.sigma = null;
        normConst = 1 / Math.sqrt(Math.pow(2 * Math.PI, center.length) * bandwidth);
        this.ready = true;
    }

    public void setBandwidthMatrix(double[][] covMtx) {
        sigma = new double[covMtx.length][];
        for (int i = 0; i < covMtx.length; i++) {
            sigma[i] = MatrixOp.copy(covMtx[i]);
        }
        this.bandwidth = Math.sqrt(Algebra.DEFAULT.det(new DenseDoubleMatrix2D(sigma)));
        invSigma = Algebra.DEFAULT.inverse(new DenseDoubleMatrix2D(sigma)).toArray();
        normConst = 1 / Math.sqrt(Math.pow(2 * Math.PI, sigma.length) * Algebra.DEFAULT.det(new DenseDoubleMatrix2D(sigma)));
        this.ready = true;
    }

    public double[][] getBandwidthMatrix() {
        return sigma;
    }
    public static final long serialVersionUID = 214L;

    @Override
    public String toString() {
        return "Gaussian Kernel, Sg=" + bandwidth;
    }

    //@Override
    public double getValue(double[] center, double[] ArrayList) {
        if (!ready) {
            throw new IllegalStateException("kernel not initialized!");
        }

        double val = normConst * getNonNormalizedValue(center, ArrayList);
        //if(Math.random()<1000)logger.print(val/MultiNormalDist.density(center, sigma, ArrayList));
        return val;
    }

    @Override
    public String getDescription() {
        return "Basic kernel function for Euclidean distance measure. f(x1,x2, K) = C(K)*exp(((x1-x2)/sigma)^2) Sigma is a spread parameter that controls the density estimate resolution";
    }

    @Override
    public String getName() {
        return "Gaussian kernel";
    }

    @Override
    protected double getNonNormalizedValue(double[] center, double[] ArrayList) {
        if (!ready) {
            throw new IllegalStateException("kernel not initialized!");
        }
        if (invSigma != null) {
            return getNonNormalizedValueCov(center, ArrayList);
        }
        double diff = 0;
        for (int i = 0; i < ArrayList.length; i++) {
            diff += (center[i] - ArrayList[i]) * (center[i] - ArrayList[i]);
        }
        double len2 = (-0.5 * diff) / (bandwidth * bandwidth);
        return Math.exp(len2);
    }

    protected double getNonNormalizedValueCov(double[] center, double[] ArrayList) {

        //if(true)return  MultiNormalDist.density(center, sigma, ArrayList);
        if (!ready) {
            throw new IllegalStateException("kernel not initialized!");
        }
        if (invSigma == null) {
            invSigma = Algebra.DEFAULT.inverse(new DenseDoubleMatrix2D(sigma)).toArray();
        }
        double[] vec = MatrixOp.copy(ArrayList);
        MatrixOp.mult(vec, -1);
        double[] diff = MatrixOp.sum(center, vec);
        double exp = Math.exp(-0.5 * MatrixOp.mult(MatrixOp.mult(invSigma, diff), diff));
        exp *= normConst;
        if (Double.isNaN(exp)) {
            logger.print("NaN exp", MatrixOp.mult(MatrixOp.mult(invSigma, diff), diff), MultiNormalDist.density(center, sigma, ArrayList));
        }

        if (Double.isInfinite(exp)) {
            logger.print("Inf exp", MatrixOp.mult(MatrixOp.mult(invSigma, diff), diff), MultiNormalDist.density(center, sigma, ArrayList));

        }

        //logger.print((exp*normConst) / MultiNormalDist.density(center, sigma, vec));
        return exp;
    }

    @Override
    protected double getNormalizationConst(double[] center) {
        return normConst;
    }

    @Override
    public BigDecimal getDensity(double[] center, double[][] ArrayLists) {
        BigDecimal result = new BigDecimal(0, MathContext.DECIMAL128);
        for (int i = 0; i < ArrayLists.length; i++) {
            BigDecimal exp = new BigDecimal(getNonNormalizedValue(center, ArrayLists[i]), MathContext.DECIMAL128);
            result = result.add(exp);
        }
        result = result.multiply(new BigDecimal(getNormalizationConst(center), MathContext.DECIMAL128));
        return result;
    }
}
