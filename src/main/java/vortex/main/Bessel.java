/*
 * Bessel.java
 *
 * Created on December 19, 2007, 5:07 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package vortex.main;

/**
 *
 * @author Nikolay
 */
public abstract class Bessel {

    public static double bessiN(int order, double x) throws IllegalArgumentException {
        int j;
        double bi, bim, bip, tox, ans;
        final double ACC = 40.0;
        final double BIGNO = 1.0e10;
        final double BIGNI = 1.0e-10;

        if (order == 0) {
            return cern.jet.math.Bessel.i0(x);
        }
        if (order == 1) {
            return cern.jet.math.Bessel.i1(x);
        }

        if (x == 0.0) {
            return 0.0;
        } else {
            tox = 2.0 / Math.abs(x);
            bip = ans = 0.0;
            bi = 1.0;
            for (j = 2 * (order + (int) Math.sqrt(ACC * order)); j > 0; j--) {

                bim = bip + j * tox * bi;
                bip = bi;
                bi = bim;
                if (Math.abs(bi) > BIGNO) {
                    ans *= BIGNI;
                    bi *= BIGNI;
                    bip *= BIGNI;
                }
                if (j == order) {
                    ans = bip;
                }
            }
            ans *= cern.jet.math.Bessel.i0(x) / bi;
            return Math.abs(ans);
        }

    }

    public static double bessiN_LOG(int order, double x) throws IllegalArgumentException {
        int j;
        double bi, bim, bip, tox, ans;
        final double ACC = 40.0;
        final double BIGNO = 1.0e10;
        final double BIGNI = 1.0e-10;

        if (order == 0) {
            return cern.jet.math.Bessel.i0(x);
        }
        if (order == 1) {
            return cern.jet.math.Bessel.i1(x);
        }


        if (x == 0.0) {
            return 0.0;
        } else {
            tox = 2.0 / Math.abs(x);
            bip = ans = 0.0;
            bi = 1.0;
            for (j = 2 * (order + (int) Math.sqrt(ACC * order)); j > 0; j--) {

                bim = Math.exp(bip) + j * tox * Math.exp(bi);

                bip = bi;

                bi = Math.log(bim);
                if (Math.abs(bi) > Math.log(BIGNO)) {
                    ans += Math.log(BIGNI);
                    bi += Math.log(BIGNI);
                    bip += Math.log(BIGNI);
                }
                if (j == order) {
                    ans = bip;
                }
            }
            ans += Math.log(cern.jet.math.Bessel.i0(x)) - bi;
            return ans;
        }

    }

    public static double bessiN(double order, double x) {

        int ord = (int) Math.floor(order);

        double res = order - Math.floor(order);

        return bessiN(ord, x) * res + (1.0 - res) * bessiN(ord + 1, x);

    }
}
