/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 *
 * @author Nikolay
 */
public class DistributionRenderer {

    public static BufferedImage getImage(String caption, double[] values, int width, int height, int inset) {
        if (height < inset * 2.0 || width < inset * 2.0) {
            throw new IllegalArgumentException("The provided dimensions are too small");
        }
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = (Graphics2D) img.getGraphics();
        g2.getRenderingHints().put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double d : values) {
            min = Math.min(min, d);
            max = Math.max(max, d);
        }
        g2.setPaint(Color.WHITE);
        g2.fillRect(0, 0, width, height);
        g2.setPaint(new Color(0, 0, 70));
        double xScale = (width - inset * 2.0) / (double) values.length;
        double yScale = (height - inset * 2.0) / (max - min);
        for (int i = 0; i < values.length - 1; i++) {
            double x1 = inset + i * xScale;
            double x2 = inset + (i + 1) * xScale;
            double y1 = height - (inset + values[i] * yScale);
            double y2 = height - (inset + values[i + 1] * yScale);
            g2.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
        }
        g2.drawString(caption, 2, 11);
        g2.drawRect(0, 0, width - 1, height - 1);
        return img;
    }
}
