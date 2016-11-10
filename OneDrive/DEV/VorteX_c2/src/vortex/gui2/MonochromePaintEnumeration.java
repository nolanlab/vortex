/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.gui2;

import java.awt.Color;
import java.awt.Paint;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Enumeration;
import javax.imageio.ImageIO;

/**
 *
 * @author Nikolay
 */
public class MonochromePaintEnumeration implements Enumeration<Paint> {

    private BufferedImage[] textures;
    private Color[] colors = new Color[]{
        new Color(255, 255, 255),
        new Color(170, 170, 170),
        new Color(85, 85, 85),
        new Color(0, 0, 0)
    };
    int i = -1;

    @Override
    public boolean hasMoreElements() {
        return true;
    }

    public Paint nextElement() {
        i++;
        return colors[Math.min(i, colors.length - 1)];
    }

    public MonochromePaintEnumeration() {
        try {
            textures = new BufferedImage[]{
                ImageIO.read(this.getClass().getResourceAsStream("/vortex/resources/texture1.PNG")),
                ImageIO.read(this.getClass().getResourceAsStream("/vortex/resources/texture2.PNG")),
                ImageIO.read(this.getClass().getResourceAsStream("/vortex/resources/texture3.PNG")),
                ImageIO.read(this.getClass().getResourceAsStream("/vortex/resources/texture4.PNG"))};
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("one of the images couldn't be loaded");
        }
    }
}
