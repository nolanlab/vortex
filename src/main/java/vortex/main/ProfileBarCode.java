/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.main;

import clustering.BarCode;

/**
 *
 * @author Nikolay
 */
public class ProfileBarCode implements BarCode {

    private double[] data;
    private String[] params;
    private int sideArrayListBeginIdx;

    @Override
    public double[] getRawValues() {
        return data;
    }

    public ProfileBarCode(double[] data, String[] paramNames, int sideArrayListBeginIdx) throws IllegalArgumentException {
        this.sideArrayListBeginIdx = sideArrayListBeginIdx;
        for (double f : data) {
            if (f < -1.0 || f > 1.0) {
                if (Math.abs(f) - 1.0 < 0.05) {
                    f = Math.round(f);
                } else {
                    throw new IllegalArgumentException("Absloute value(s) of parameter(s) " + String.valueOf(f) + " exceed 1.0");
                }
            }
        }
        this.data = data;
        this.params = paramNames;
    }

    @Override
    public double[] getProfile() {
        return data;
    }

    @Override
    public String[] getParameterNames() {
        return params;
    }

    @Override
    public int getSideVectorBeginIdx() {
        return sideArrayListBeginIdx;
    }
}
