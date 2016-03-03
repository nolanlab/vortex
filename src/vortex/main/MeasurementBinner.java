/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.main;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import java.util.Comparator;

/**
 *
 * @author Nikolay
 */
public class MeasurementBinner {

    public static int[][] equallyPopulatedBinning(double[][] mtx, int numBins) {
        double[][] mtx2 = (new DenseDoubleMatrix2D(mtx).copy()).toArray();
        double binSize = mtx2.length / numBins;
        int[][] mtx3 = new int[mtx2.length][mtx2[0].length];
        for (int i = 0; i < mtx2[0].length; i++) {
            final int i2 = i;
            java.util.Arrays.sort(mtx2, new Comparator<double[]>() {
                @Override
                public int compare(double[] o1, double[] o2) {
                    return (int) Math.signum(o1[i2] - o2[i2]);
                }
            });
            for (int x = 0; x < mtx2.length; x++) {
                mtx2[x][i] = Math.floor(x / binSize);
            }
        }
        java.util.Arrays.sort(mtx2, new Comparator<double[]>() {
            @Override
            public int compare(double[] o1, double[] o2) {
                return (int) Math.signum(o1[0] - o2[0]);
            }
        });
        for (int x = 0; x < mtx2.length; x++) {
            for (int y = 0; y < mtx2[0].length; y++) {
                mtx3[x][y] = (int) Math.min(numBins - 1, Math.min(3, Math.max(-3, Math.floor(mtx[x][y] / 2.0))) + 3);
            }
        }
        return mtx3;
    }
}
