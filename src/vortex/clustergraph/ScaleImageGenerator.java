/*
 * Copyright (C) 2015 Nikolay
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package vortex.clustergraph;

import com.itextpdf.text.Font;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import main.Dataset;
import vortex.main.QuantileMap;

/**
 *
 * @author Nikolay
 */
public class ScaleImageGenerator {
    
    public static BufferedImage generateScaleImage(int height, int paramIDX, Dataset ds, double COLOR_MAP_NOISE_THRESHOLD, QuantileMap qm, QuantileMap sqm) {
        int w = height / 3;
        BufferedImage bi = new BufferedImage(w + 100, height + 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.setPaint(Color.WHITE);
        g.translate(0, 20);
        //g.fillRect(0, -20, w+100, height+50);
        for (int i = 0; i < height; i++) {
            double val = (i / (double) height);
            g.setPaint(getColorForValue(val));
            g.drawLine(0, height - i, w, height - i);
        }
        //logger.print("end of color fill");
        g.setFont(g.getFont().deriveFont(14f).deriveFont(Font.BOLD));

        double noiseQuantile = (paramIDX >= ds.getDimension()) ? sqm.getQuantileForValue(paramIDX - ds.getDimension(), COLOR_MAP_NOISE_THRESHOLD) : qm.getQuantileForValue(paramIDX, COLOR_MAP_NOISE_THRESHOLD);

        for (double i = noiseQuantile; i <= 1; i += ((0.999 - noiseQuantile) / 4)) {
            int y = (int) (((i - noiseQuantile) / (1 - noiseQuantile)) * height);
            double val = (paramIDX >= ds.getDimension()) ? sqm.getSourceDatasetQuantile(paramIDX - ds.getDimension(), i) : qm.getSourceDatasetQuantile(paramIDX, i);
            g.setPaint(Color.BLACK);
            g.drawLine(w, height - y, w + 10, height - y);
            try{
                g.drawString(String.valueOf(val).substring(0, 4), w + 12, (height - y) + 5);
            }catch(StringIndexOutOfBoundsException e){
                g.drawString(String.valueOf(val), w + 12, (height - y) + 5);
            }
            //String.valueOf((int) (Math.sinh(val) * 5)), w + 12, (height - y) + 5);
        }
        g.setPaint(Color.WHITE);
        return bi;
    }
    
    public static BufferedImage generateScaleImage(int height, double minVal, double maxVal) {
        int w = height / 3;
        BufferedImage bi = new BufferedImage(w + 100, height + 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.setPaint(Color.WHITE);
        g.translate(0, 20);
        //g.fillRect(0, -20, w+100, height+50);
        for (int i = 0; i < height; i++) {
            double val = (i / (double) height);
            g.setPaint(getColorForValue(val));
            g.drawLine(0, height - i, w, height - i);
        }
        //logger.print("end of color fill");
        g.setFont(g.getFont().deriveFont(14f).deriveFont(Font.BOLD));


        for (double i = minVal; i <= 1; i += ((maxVal - minVal) / 4)) {
            int y = (int) (((i - minVal) / (maxVal - minVal)) * height);
            double val = i;
            g.setPaint(Color.BLACK);
            g.drawLine(w, height - y, w + 10, height - y);
            try{
                g.drawString(String.valueOf(val).substring(0, 4), w + 12, (height - y) + 5);
            }catch(StringIndexOutOfBoundsException e){
                g.drawString(String.valueOf(val), w + 12, (height - y) + 5);
            }
            //String.valueOf((int) (Math.sinh(val) * 5)), w + 12, (height - y) + 5);
        }
        g.setPaint(Color.WHITE);
        return bi;
    }
    
    public static Color getColorForValue(double val) {
        if (Double.isNaN(val)) {
            return Color.GRAY;
        }
        
        return new Color(Color.HSBtoRGB(0.60f - (float) val * 0.60f, 1, 1f));
    }
}
