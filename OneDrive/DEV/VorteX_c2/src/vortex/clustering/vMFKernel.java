/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustering;

import clustering.DistanceMeasure;
import clustering.AngularDistance;
import vortex.main.Bessel;
import clustering.Dataset;
import clustering.Datapoint;
import java.math.BigDecimal;
import java.math.MathContext;
import util.MatrixOp;
import util.ProfileAverager;

/**
 *
 * @author Nikolay
 */
public class vMFKernel extends Kernel {

    @Override
    public vMFKernel clone() {
        vMFKernel newKernel = new vMFKernel(dm.clone());
        newKernel.init(null, bandwidth);
        return newKernel;
    }

    public static double exp(double val) {
        final long tmp = (long) (1512775 * val + 1072632447);
        return Double.longBitsToDouble(tmp << 32);
    }

    @Override
    public boolean isPublic() {
        return true;
    }

    @Override
    public boolean isResolutionAscending() {
        return true;
    }

    @Override
    public String freeParameterName() {
        return "K";
    }

    @Override
    public double[] getWeightedMean(double[] center, double[][] vectors) {
        ProfileAverager pa = new ProfileAverager();
        for (double[] d : vectors) {
            pa.addProfile(d, this.getNonNormalizedValue(center, d));
        }
        return pa.getAverageUnityLen();
    }

    public Datapoint getWeightedMean(Datapoint center, Datapoint[] dp) {
        ProfileAverager pa = new ProfileAverager();
        ProfileAverager pa2 = new ProfileAverager();
        for (Datapoint d : dp) {
            double w = this.getNonNormalizedValue(center.getUnityLengthVector(), d.getUnityLengthVector());
            pa.addProfile(d.getVector(), w);
            pa2.addProfile(d.getSideVector(), w);
        }
        return new Datapoint("WeightedMean", pa.getAverage(), pa2.getAverage(), -1);
    }

    @Override
    public void init(Dataset nd, double bandwidth) {
        this.bandwidth = bandwidth;
        this.ready = true;
    }

    public vMFKernel(DistanceMeasure distMeasure) {
        super(distMeasure);
        if (!AngularDistance.class.isAssignableFrom(distMeasure.getClass())) {
            throw new IllegalArgumentException("This kernel works only with Angular distance");
        }
    }

    @Override
    public void fitBandwidth(double[] center, double[][] vectors) {
        double maxProdWeight =0;
        int maxK = 0;
        for (int i = 0; i < 500; i++) {
            setBandwidth(i);
            
            double sumLogWeight = Math.log(getVMFConst(center.length, bandwidth))*vectors.length;
            
            for (double[] v : vectors) {
                sumLogWeight+= bandwidth*MatrixOp.getEuclideanCosine(center, v);
            }
            if(sumLogWeight> maxProdWeight){
                maxProdWeight = sumLogWeight;
                maxK = i;
            }
        }
        setBandwidth(maxK);
        ready = true;
    }
    public static final long serialVersionUID = 214L;

    @Override
    public String toString() {
        return "vMF Kernel, K=" + bandwidth;
    }

    public static double getVMFConst(int dim, double bandwidth) {
        return Math.pow(bandwidth, (dim / 2) - 1) / (Math.pow(Math.PI * 2, dim / 2) * Bessel.bessiN((dim / 2) - 1, bandwidth));
    }

    public double getValue(double[] center, double[] vector) {
        if (!ready) {
            throw new IllegalStateException("kernel not initialized!");
        }
        return getVMFConst(center.length, bandwidth) * exp(MatrixOp.getEuclideanCosine(center, vector) * bandwidth);
    }

    @Override
    public String getDescription() {
        return "Basic kernel function for angular distance measure. f(x1,x2, K) = C(K)*exp(K*cos(x1,x2)). K is a concetrnation parameter that controls the density estimate resolution";
    }

    @Override
    public String getName() {
        return "von Mises-Fisher Kernel";
    }

    @Override
    protected double getNonNormalizedValue(double[] center, double[] vector) {
        if (!ready) {
            throw new IllegalStateException("kernel not initialized!");
        }
        return exp(bandwidth * MatrixOp.getEuclideanCosine(vector, center));
    }

    @Override
    protected double getNormalizationConst(double[] center) {
        int dim = center.length;
        return getVMFConst(dim, bandwidth);
    }

    @Override
    public BigDecimal getDensity(double[] center, double[][] vectors) {
        center = MatrixOp.toUnityLen(center);
        BigDecimal result = new BigDecimal(0, MathContext.DECIMAL128);
        double val = 0;

        for (int i = 0; i < vectors.length; i++) {
            val = Math.exp(bandwidth * ((MatrixOp.mult(center, vectors[i]) / MatrixOp.lenght(vectors[i]))));
            if (Double.isInfinite(val) || Double.isNaN(val)) {
                val = Double.MAX_VALUE;
            }
            BigDecimal exp = new BigDecimal(val, MathContext.DECIMAL128);
            result = result.add(exp);
        }

        result = result.multiply(new BigDecimal(getVMFConst(center.length, bandwidth), MathContext.DECIMAL128));

        return result;
    }
    /*
     private double LogOnePlusX(double x){
     if (x <= -1.0) throw new IllegalArgumentException("x must be greater than ")

     if (fabs(x) > 1e-4)
     {
     // x is large enough that the obvious evaluation is OK
     return log(1.0 + x);
     }

     // Use Taylor approx. log(1 + x) = x - x^2/2 with error roughly x^3/3
     // Since |x| < 10^-4, |x|^3 < 10^-12, relative error less than 10^-8

     return (-0.5*x + 1.0)*x;
     }*/
    /**
     * Compute the natural logarithm of x to a given scale, x > 0.
     */
}
