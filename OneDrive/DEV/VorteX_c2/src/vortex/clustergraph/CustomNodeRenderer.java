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

import java.awt.Color;
import org.gephi.graph.api.Node;
import org.gephi.graph.dhns.node.AbstractNode;
import org.gephi.preview.api.Item;
import org.gephi.preview.api.PreviewProperties;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.api.ProcessingTarget;
import org.gephi.preview.plugin.items.NodeItem;
import org.gephi.preview.plugin.renderers.NodeRenderer;
import processing.core.PFont;
import processing.core.PGraphics;
import vortex.gui2.frmMain;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class CustomNodeRenderer extends NodeRenderer {

    private boolean textOn;
    private boolean nodeBordersVisible;
    private boolean renderLabelOutside;

    public enum Shape {
        CIRCLE, SQUARE, TRIANGLE
    }

    public CustomNodeRenderer(boolean textOn, boolean nodeBordersVisible, boolean renderLabelOutside) {
        this.textOn = textOn;
        this.nodeBordersVisible = nodeBordersVisible;
        this.renderLabelOutside = renderLabelOutside;
    }

    

    public void setNodeBordersVisible(boolean nodeBordersVisible) {
        this.nodeBordersVisible = nodeBordersVisible;
    }

    public boolean isNodeBordersVisible() {
        return nodeBordersVisible;
    }

    public void setTextOn(boolean textOn) {
        this.textOn = textOn;
    }

    public boolean isTextOn() {
        return textOn;
    }

    @Override
    public void renderProcessing(Item item, ProcessingTarget target, PreviewProperties properties) {
        //Params

        Float x = item.getData(NodeItem.X);
        Float y = item.getData(NodeItem.Y);
        Float size = item.getData(NodeItem.SIZE);
        Color color = item.getData(NodeItem.COLOR);
        
        Node n = (AbstractNode) item.getSource();
        String label = n.getNodeData().getLabel();
        Shape shape = Shape.CIRCLE;
        
        /*
        if(label.startsWith("R\n")){
            shape = Shape.TRIANGLE;
        }
        if(label.startsWith("C\n")){
            shape = Shape.SQUARE;
        }*/
        
        int alpha = (int) ((properties.getFloatValue(PreviewProperty.NODE_OPACITY) / 100f) * 255f);
        if (alpha > 255) {
            alpha = 255;
        }

        PGraphics graphics = target.getGraphics();

        double d = 0;
        boolean selected = false;
        try {
            d = (Double) n.getAttributes().getValue("logRatio");
        } catch (NullPointerException e) {
            d = 0.5;
        }
        try {
            selected = (Boolean) n.getAttributes().getValue("selected");
            //logger.print(n.getAttributes().getValue("cluster") + Boolean.toString(selected));
        } catch (Exception e) {

        }

        d -= 0.5;
        d *= 2;

        Color borderColor = (d > 0) ? new Color(255, 0, 128) : new Color(0, 255, 128);
        float borderAlpha = 0;
        if (d > 0.9 || d < -0.9) {
            borderAlpha = 200;
        }

        if (alpha > 255) {
            alpha = 255;
        }

        graphics.stroke(color.darker().getRed(), color.darker().getGreen(), color.darker().getBlue(), 50);
        graphics.fill(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        if (selected) {
            graphics.stroke(0, 0, 0, 150);
        }
        graphics.strokeWeight(selected ? 5 : 3);
        if (shape == Shape.CIRCLE) {
            graphics.ellipse(x, y, size, size);
        }
        if (shape == Shape.SQUARE) {
            graphics.rect(x, y, size, size);
        }
        if (shape == Shape.TRIANGLE) {
            graphics.triangle(x, y-size/2, x+((size/2)*(float)(Math.sqrt(3f)/2f)), y+(size/4f), x-((size/2)*(float)(Math.sqrt(3f)/2f)), y+(size/4f));
        }

        graphics.stroke(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), borderAlpha);
        graphics.strokeWeight(properties.getFloatValue(PreviewProperty.NODE_BORDER_WIDTH));
        graphics.fill(0, 0, 0, 0);
        if (nodeBordersVisible) {
            graphics.ellipse(x, y, size + 5, size + 5);
            if (shape == Shape.CIRCLE) {
                graphics.ellipse(x, y, size + 5, size + 5);
            }
            if (shape == Shape.SQUARE) {
                graphics.rect(x, y, size + 5, size + 5);
            }
            if (shape == Shape.TRIANGLE) {
             graphics.triangle(x, y-(size+5)/2, x+(((size+5)/2)*(float)(Math.sqrt(3f)/2f)), y+((size+5)/4f), x-(((size+5)/2)*(float)(Math.sqrt(3f)/2f)), y+((size+5)/4f));
            }
        }

        if (textOn) {
            if (renderLabelOutside) {

                try {
                    graphics.beginText();
                    float textSize = (14);
                    PFont f = new PFont(frmMain.getInstance().getFont().deriveFont(textSize), true);
                    graphics.textFont(f, textSize);
                    graphics.textSize(textSize);

                    float Xoffset = size;
                    float Yoffset = (textSize / 4);
                    graphics.stroke(0, 0, 0);
                    graphics.fill(0, 0, 0);
                    graphics.text(label, x + size / 2, y + Yoffset);
                    graphics.endText();
                } catch (Exception e) {
                    logger.print(e);
                }
            } else {
                try {
                    graphics.beginText();

                    String[] lbls = label.replaceAll(">","\n>").replaceAll("<","\n<").split("\n");

                    float textSize = (size);
                    PFont f = new PFont(frmMain.getInstance().getFont().deriveFont(textSize), true);
                    graphics.textFont(f, textSize);
                    graphics.textSize(textSize);
                    float diffFactor = Math.max(graphics.textWidth(lbls[0]), (graphics.textAscent() + graphics.textDescent()) * lbls.length) / (size * 0.9f);
                    textSize /= diffFactor;
                    f = new PFont(frmMain.getInstance().getFont().deriveFont(textSize), true);
                    graphics.textFont(f, textSize);
                    graphics.textSize(textSize);
                    float YOffset = ((graphics.textAscent() + graphics.textDescent()) / 2) * (float) lbls.length;
                    float height = (graphics.textAscent() + graphics.textDescent()) * 0.8f;
                    YOffset -= graphics.textAscent();
                    for (String l : lbls) {
                        float Xoffset = (graphics.textWidth(l) / 2f) * 1.05f;
                        if (color.equals(Color.WHITE)) {
                            graphics.stroke(0, 0, 0);
                            graphics.fill(0, 0, 0);
                        } else {
                            graphics.fill(255, 255, 255);
                            graphics.stroke(255, 255, 255);
                        }
                        graphics.text(l, x - Xoffset, (y - YOffset));
                        graphics.text(l, x - Xoffset, (y - YOffset));
                        YOffset -= height;
                    }
                    graphics.endText();

                } catch (Exception e) {
                    logger.print(e);
                }
            }
        }
    }
}
