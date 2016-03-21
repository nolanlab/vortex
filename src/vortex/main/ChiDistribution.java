/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.main;

import cern.jet.stat.Gamma;
import umontreal.iro.lecuyer.probdist.Distribution;

/**
 *
 * @author Nikolay
 */
public class ChiDistribution extends umontreal.iro.lecuyer.probdist.ContinuousDistribution {

    double Cst;
    double f;

    @Override
    public double getVariance() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getStandardDeviation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double[] getParams() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getMean() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double density(double x) {
        if (x < 0.0) {
            throw new IllegalArgumentException("Argument x = " + String.valueOf(x) + " cannot be < 0");
        }
        double arg = Math.pow(x, 2);
        return Cst * Math.pow(arg, (f / 2.0) - 1) * Math.exp(-arg / 2.0);
    }

    @Override
    public double cdf(double arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ChiDistribution(double numberDegFreedom) {
        if (numberDegFreedom < 0.0) {
            throw new IllegalArgumentException("Number of degrees of freedom n = " + String.valueOf(numberDegFreedom) + " cannot be < 0");
        }
        Cst = Math.pow(0.5, numberDegFreedom / 2) / Gamma.gamma((numberDegFreedom / 2));
        f = numberDegFreedom;
    }
}
