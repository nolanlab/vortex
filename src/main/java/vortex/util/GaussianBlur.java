/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.util;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import umontreal.iro.lecuyer.probdistmulti.MultiNormalDist;

/**
 *
 * @author Nikolay
 */
public class GaussianBlur {

    public static Image getBlurredImg(BufferedImage img) {
        final int kernel_size = 15;
        float[] blurKernel = new float[kernel_size * kernel_size];
        final float blurPeakHeight = 0.35f;
        double[][] sigma = new double[2][2];
        sigma[0] = new double[]{1.2, 0.0};
        sigma[1] = new double[]{0.0, 1.2};

        double mid = (kernel_size / 2.0);
        float cent = (float) MultiNormalDist.density(new double[]{mid, mid}, sigma, new double[]{mid, mid});

        for (int i = 0; i < blurKernel.length; i++) {
            int x = i / kernel_size;
            int y = i % kernel_size;
            blurKernel[i] = blurPeakHeight * (float) MultiNormalDist.density(new double[]{mid, mid}, sigma, new double[]{x, y}) / cent;
        }

        ConvolveOp op = new ConvolveOp(new Kernel(kernel_size, kernel_size, blurKernel));

        return op.filter(img, null);

    }
}
