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
package vortex.scripts;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 *
 * @author Nikolay
 */
public class gen2Dscale {
    public static void main(String[] args) throws Exception {
        BufferedImage bi = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        for (int i = 0; i < 128; i++) {
             for (int j = 0; j < 128; j++) {
                 g.setPaint(new Color(i*2,j*2,0));
                 g.fillRect(i, j, 1, 1);
             }
        }
        ImageIO.write(bi, "PNG", new File("C:\\Users\\Nikolay\\Box Sync\\Ovarian Cancer Project\\Heterogeneity Manuscript\\Figures\\Nik-Vero Working Folder\\Action Items 11-30-2015\\VIM-ECAD scale.png"));
    }
}
