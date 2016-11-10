/*
 * vonMisesFisherDistribution.java
 *
 * Created on December 18, 2007, 5:41 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package vortex.main;

import clustering.MahalonobisSpace;
import cern.jet.stat.Gamma;
import java.sql.*;
import java.util.Arrays;
import umontreal.iro.lecuyer.probdist.ContinuousDistribution;
import vortex.util.ConnectionManager;
import util.MatrixOp;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class vonMisesFisherDistribution {

    private static TabledFunction2Var integralProbabilityOverKcosAng = null;
    private double[] mu;
    private double K;
    private static cern.colt.matrix.linalg.Algebra algebra = new cern.colt.matrix.linalg.Algebra();
    private MahalonobisSpace mahalonobisSpace;

    /**
     * Creates a new instance of vonMisesFisherDistribution
     */
    public vonMisesFisherDistribution(double[] mu, double K, MahalonobisSpace mahalonobisSpace)
            throws java.lang.IllegalArgumentException {
        if (mahalonobisSpace.getInverseCovMtx().columns() != mu.length || mahalonobisSpace.getInverseCovMtx().rows() != mu.length) {
            throw new java.lang.IllegalArgumentException("Columns/rows of covariation matrix do not match the size of mu");
        }
        this.mahalonobisSpace = mahalonobisSpace;
        this.mu = mu;
        this.K = K;
    }

    public void setMu(double[] mu) {
        if (this.mu.length == mu.length) {
            this.mu = Arrays.copyOf(mu, mu.length);
        } else {
            throw new java.lang.IllegalArgumentException("Dimension of new mu doesn't match the old one");
        }
    }

    public double[] getMu() {
        return mu;
    }

    public void setK(double K) throws java.lang.IllegalArgumentException {
        if (K >= 0) {
            this.K = K;
        } else {
            throw new java.lang.IllegalArgumentException("K must be >= 0");
        }
    }

    public double getK() {
        return K;
    }

    public double getProbabilityDensityEuclLOG(double[] x) throws java.lang.IllegalArgumentException {

        try {
            if (x.length != mu.length) {
                throw new java.lang.IllegalArgumentException("Size of argument vector (" + String.valueOf(x.length) + ") doesn't match size of mu (" + String.valueOf(mu.length) + ")");
            }
        } catch (NullPointerException ex) {
            logger.print(mu);
            logger.print(x);
            logger.print(ex);
        }
        double cosine = MatrixOp.getEuclideanCosine(mu, x);
        double exp = cosine * K;
        int p = x.length;
        double Ck = Math.log(Math.pow(K, (p / 2) - 1) / (Math.pow(Math.PI * 2, p / 2) * Bessel.bessiN((p / 2) - 1, K)));
        double result = exp + Ck;
        return result;
    }

    public double getProbabilityDensity(double[] x) throws java.lang.IllegalArgumentException {
        if (x.length != mu.length) {
            throw new java.lang.IllegalArgumentException("Size of argument vector (" + String.valueOf(x.length) + ") doesn't match size of mu (" + String.valueOf(mu.length) + ")");
        }
        double cosine = Math.min(1.0, mahalonobisSpace.getMahalonobisCosine(mu, x));
        double exp = Math.exp(cosine * K);
        int p = x.length;
        double Ck = Math.pow(K, (p / 2) - 1) / (Math.pow(Math.PI * 2, p / 2) * Bessel.bessiN((p / 2) - 1, K));
        double result = exp * Ck;
        return result;
    }

    public double getProbabilityDensityEucl(double[] x) throws java.lang.IllegalArgumentException {
        if (x.length != mu.length) {
            throw new java.lang.IllegalArgumentException("Size of argument vector (" + String.valueOf(x.length) + ") doesn't match size of mu (" + String.valueOf(mu.length) + ")");
        }
        double cosine = MatrixOp.getEuclideanCosine(mu, x);
        if (cosine > 1.0001 || cosine < -1.00001) {
            throw new IllegalArgumentException("cosine > 1.0");
        }
        double exp = Math.exp(cosine * K);
        int p = x.length;
        double Ck = Math.pow(K, (p / 2) - 1) / (Math.pow(Math.PI * 2, p / 2) * Bessel.bessiN((p / 2) - 1, K));
        double result = exp * Ck;
        return result;
    }

    public MahalonobisSpace getMahalonobisSpace() {
        return mahalonobisSpace;
    }

    public void setMahalonobisSpace(MahalonobisSpace mahalonobisSpace) {
        this.mahalonobisSpace = mahalonobisSpace;
    }

    public double getFittedKByLogLikelihood(double[][] values) {

        double K = 1;
        vonMisesFisherDistribution tmpVMF = new vonMisesFisherDistribution(mu, K, this.mahalonobisSpace);
        double logLikelihood = 0;
        double prevLogLikelihood = 0;
        double prevK = 0;
        double newK = tmpVMF.getK();
        do {
            prevLogLikelihood = logLikelihood;
            logLikelihood = 0;
            for (int i = 0; i < values.length; i++) {
                logLikelihood += tmpVMF.getProbabilityDensityEuclLOG(values[i]);
            }
            newK *= 2;
            prevK = tmpVMF.getK();
            tmpVMF.setK(newK);
        } while (logLikelihood > prevLogLikelihood);

        double logLikelihood1, logLikelihood2;

        tmpVMF.setK(prevK);
        double stepK = prevK / 2;
        do {
            double tmpK = tmpVMF.getK();
            double K1 = tmpK + stepK;
            tmpVMF.setK(K1);
            logLikelihood1 = 0;
            for (int i = 0; i < values.length; i++) {
                logLikelihood1 += tmpVMF.getProbabilityDensityEuclLOG(values[i]);
            }

            double K2 = tmpK - stepK;
            tmpVMF.setK(K2);
            logLikelihood2 = 0;
            for (int i = 0; i < values.length; i++) {
                logLikelihood2 += tmpVMF.getProbabilityDensityEuclLOG(values[i]);
            }

            if (logLikelihood1 > logLikelihood2) {
                tmpVMF.setK(K1);
            } else {
                tmpVMF.setK(K2);
            }

            stepK /= 2;

        } while (Math.abs(logLikelihood1 - logLikelihood2) > prevLogLikelihood / 10000);

        K = tmpVMF.getK();
        //logger.print(String.valueOf(vectorLen) + "\t" + String.valueOf(K));
        this.K = K;
        return K;
    }

    public double getFittedKByChiSqare(double[][] values) {

        if (values.length < 2) {
            return 0.01;
        }

        //Binning the observations
        final int BIN_NUMBER = 21;
        int[] observedFreq = new int[BIN_NUMBER];
        Arrays.fill(observedFreq, 0);

        for (double[] f : values) {
            int index = (int) (Math.floor((MatrixOp.getEuclideanCosine(f, mu) * (BIN_NUMBER / 2)) + (BIN_NUMBER / 2)));
            observedFreq[index]++;
        }
        double uBoundK = 1;
        for (double i = -1 + (0.5 / BIN_NUMBER); i < 1.0; i += (1.0 / BIN_NUMBER)) {
        }
        double peak_observ = -1.0;
        for (int k = 1; k < observedFreq.length; k++) {
            if (observedFreq[k] > observedFreq[k - 1]) {
                peak_observ = ((double) k - (BIN_NUMBER / 2)) / (BIN_NUMBER / 2);
            }
        }
        double peak_theor = -1.0;
        do {
            peak_theor = -1;
            double next, prev;
            do {
                peak_theor += (2.0 / BIN_NUMBER);
                next = Math.log(Math.pow(1 - Math.pow((peak_theor + 0.05), 2), (mu.length - 2) / 2)) + (uBoundK * (peak_theor + 0.05));
                prev = Math.log(Math.pow(1 - Math.pow((peak_theor), 2), (mu.length - 2) / 2)) + (uBoundK * (peak_theor));
                //System.out.print(String.valueOf(prev) + "\t");
            } while (next >= prev && peak_theor <= 1.0 && next != Double.NEGATIVE_INFINITY);
            uBoundK *= 2;
        } while (peak_theor < peak_observ && uBoundK < 512);

        double K = uBoundK / 2;
        double stepK = K / 2;
        double chiSquare1, chiSquare2;
        double prevChiSquare = getVMFChiSquareForK(mu, observedFreq, K);
        do {
            double K1 = K + stepK;
            chiSquare1 = getVMFChiSquareForK(mu, observedFreq, K1);
            double K2 = K - stepK;
            chiSquare2 = getVMFChiSquareForK(mu, observedFreq, K2);
            if (chiSquare1 < chiSquare2) {
                K = K1;
            } else {
                K = K2;
            }
            stepK /= 2;
        } while (Math.abs(chiSquare1 - chiSquare2) > prevChiSquare / 10000);
        double vectorLen = MatrixOp.lenght(mu);
        vectorLen = Math.sqrt(vectorLen);
        //logger.print(String.valueOf(vectorLen) + "\t" + String.valueOf(K));
        return K;
    }

    private static double getVMFChiSquareForK(double[] mu, int[] observedFreq, double K) throws IllegalArgumentException {

        if (K < 0) {
            throw new IllegalArgumentException("K must be >= 0");
        }

        //generating theoretical freqs
        final int BIN_NUMBER = observedFreq.length;
        double[] theoreticalFreq = new double[BIN_NUMBER];
        int maxObservedFreq = 0;
        int sumObservedFreq = 0;

        //!Adding unity to every observation, then logarithmising in order to make log of empty bins eqal to 0
        for (int f : observedFreq) {
            if (f > maxObservedFreq) {
                maxObservedFreq = f;
            }
            sumObservedFreq += f;
        }
        double logMaxTheorFreq = Double.NEGATIVE_INFINITY;
        for (double i = -1 + (0.5 / BIN_NUMBER); i < 1.0; i += (1.0 / BIN_NUMBER)) {
            double freq = Math.log(Math.pow(1 - Math.pow(i, 2), (mu.length - 2) / 2)) + (K * i);
            theoreticalFreq[(int) Math.round((i * BIN_NUMBER / 2) + (BIN_NUMBER / 2))] = freq;
            if (freq > logMaxTheorFreq) {
                logMaxTheorFreq = freq;
            }
        }
        //adjusting amplitude of theoretical freqencies so that maximums of two observations have the same height
        int[] theorFreqAmplitudeAdjusted = new int[BIN_NUMBER];

        //primary adjustment in log scale
        double logAdjustFactor = Math.log(maxObservedFreq) - logMaxTheorFreq;
        for (int i = 0; i < BIN_NUMBER; i++) {
            theorFreqAmplitudeAdjusted[i] = (int) Math.round(Math.exp(theoreticalFreq[i] + logAdjustFactor));
        }

        //readjust in linear scale
        int sumTheorFreq = 0;
        for (int f : theorFreqAmplitudeAdjusted) {
            sumTheorFreq += f;
        }
        double adjustFactor = ((double) sumObservedFreq) / (sumTheorFreq);
        for (int i = 0; i < theorFreqAmplitudeAdjusted.length; i++) {
            int newF = (int) Math.round(((double) theorFreqAmplitudeAdjusted[i]) * adjustFactor);
            theorFreqAmplitudeAdjusted[i] = newF;
        }

        //calculating chi-sqare: JUST SUM OF QUADRATIC DEVIATIONS
        double chiSquare = 0.0;
        for (int i = 0; i < BIN_NUMBER; i++) {
            chiSquare += Math.pow(observedFreq[i] - theorFreqAmplitudeAdjusted[i], 2);
        }
        return chiSquare;
    }

    public double getPValue(double[] vector) throws IllegalArgumentException {
        double cosAng = MatrixOp.getEuclideanCosine(vector, mu);
        if (cosAng >= 0.999999999) {
            return 1;
        }
        double pValue = Double.NaN;
        try {
            if (integralProbabilityOverKcosAng == null) {
                integralProbabilityOverKcosAng = null;//fetchIntegralProbabilityOverKCosAng();
            }
            pValue = integralProbabilityOverKcosAng.get(this.K, cosAng);
        } catch (IllegalArgumentException e) {
        }
        if (Double.isNaN(pValue)) {
            logger.print(cosAng + ", " + K);
        }
        return pValue;
    }

    public double getPValue(double cosAng) throws IllegalArgumentException {
        if (cosAng >= 0.999999999) {
            return 1;
        }
        double pValue = Double.NaN;
        try {
            if (integralProbabilityOverKcosAng == null) {
                integralProbabilityOverKcosAng = null;//fetchIntegralProbabilityOverKCosAng();
            }
            pValue = integralProbabilityOverKcosAng.get(this.K, cosAng);
        } catch (IllegalArgumentException e) {
        }
        if (Double.isNaN(pValue)) {
            logger.print(cosAng + ", " + K);
        }
        return pValue;
    }

    public double getProbabilityDensityOverCosA(double cosA) {
        if (cosA < -1.0000000001 || cosA > 1.0000000001) {
            throw new IllegalArgumentException("CosA = " + String.valueOf(cosA) + "cannot exceed the -1:1 interval");
        }
        double p = mu.length;
        double exp = Math.exp(cosA * K) * Math.pow(1 - Math.pow(cosA, 2), (p - 2.0) / 2.0);
        double Ck = Math.pow(K, (p / 2.0) - 1) / (Math.pow(Math.PI * 2, p / 2.0) * Bessel.bessiN((p / 2) - 1, K));
        double result = exp * Ck;
        if (Double.isNaN(result)) {
            logger.print("NaN in vMF, cosA = " + cosA + ", K = " + K);
        }
        return result;
    }

    public static double getProbabilityDensityOverCosAforK(double cosA, double K, int dim) {
        if (cosA < -1.0 || cosA > 1.0) {
            throw new IllegalArgumentException("CosA = " + String.valueOf(cosA) + "cannot exceed the -1:1 interval");
        }
        if (K == 0) {
            K = 0.001;
        }
        double p = dim;
        double exp = Math.exp(cosA * K) * Math.pow(1 - Math.pow(cosA, 2), (p - 2.0) / 2.0);
        double Ck = Math.pow(K, (p / 2.0) - 1) / (Math.pow(Math.PI * 2, p / 2.0) * Bessel.bessiN((p / 2) - 1, K));
        if (K == 0) {
            Ck = Gamma.gamma((p + 1.0) / 2.0) / (2 * Math.pow(Math.PI, (p + 1.0) / 2.0));
        }
        double result = exp * Ck;
        return result;
    }

    public TabledFunction1Var getProbDensFuncOverLenCosA(ContinuousDistribution dist) {
        //Returns the distribution of projection of random vectors onto Mu, with direction distributed according to this vMF, and length distr
        double sum = 0;
        double X[] = new double[100];
        double Y[] = new double[100];

        double globSum = 0.0;
        for (int n = 0; n <= 100; n++) {
            X[n] = -1.0 + (double) n / 50.0;
            sum = 0;
            for (double t = - 1; t <= 1; t += 0.01) {
                sum += dist.density(X[n] / t) * this.getProbabilityDensityOverCosA(t) * Math.sqrt(1 + Math.pow((X[n] / Math.pow(t, 2)), 2));
            }
            Y[n] = sum;
            globSum += sum;
        }
        for (int i = 0; i < Y.length; i++) {
            Y[i] /= globSum;
        }
        return new TabledFunction1Var(X, Y);
    }
}
