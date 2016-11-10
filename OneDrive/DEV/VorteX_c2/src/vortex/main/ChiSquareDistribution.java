/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.main;

import cern.jet.stat.Gamma;

/**
 *
 * @author Nikolay
 */
public class ChiSquareDistribution {

    double Cst;
    double f;

    public ChiSquareDistribution(double numberDegFreedom) {
        if (numberDegFreedom < 0.0) {
            throw new IllegalArgumentException("Number of degrees of freedom n = " + String.valueOf(numberDegFreedom) + " cannot be < 0");
        }
        Cst = Math.pow(0.5, numberDegFreedom / 2) / Gamma.gamma((numberDegFreedom / 2));
        f = numberDegFreedom;
    }

    public double getprobabilityDensity(double x) {
        if (x < 0) {
            throw new IllegalArgumentException("Argument x = " + String.valueOf(x) + " cannot be < 0");
        }
        return Cst * Math.pow(x, (f / 2.0) - 1) * Math.exp(-x / 2.0);
    }
}
