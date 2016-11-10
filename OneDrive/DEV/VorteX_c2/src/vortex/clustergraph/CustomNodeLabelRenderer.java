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
import java.util.Arrays;
import org.gephi.graph.api.Node;
import org.gephi.preview.api.Item;
import org.gephi.preview.api.PDFTarget;
import org.gephi.preview.api.PreviewProperties;
import org.gephi.preview.api.ProcessingTarget;
import org.gephi.preview.api.RenderTarget;
import org.gephi.preview.api.SVGTarget;
import org.gephi.preview.plugin.renderers.NodeLabelRenderer;
import processing.core.PFont;
import processing.core.PGraphics;
import vortex.gui2.frmMain;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class CustomNodeLabelRenderer extends NodeLabelRenderer{
    private boolean textOn;

    public CustomNodeLabelRenderer(boolean textOn) {
        this.textOn = textOn;
    }

    public boolean isTextOn() {
        return textOn;
    }

    public void setTextOn(boolean textOn) {
        this.textOn = textOn;
    }
    
        @Override
        public void render(Item item, RenderTarget target, PreviewProperties properties) {
            super.render(item, target, properties); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void renderPDF(PDFTarget target, Node node, String label, float x, float y, int fontSize, Color color, float outlineSize, Color outlineColor, boolean showBox, Color boxColor) {
            super.renderPDF(target, node, label, x, y, fontSize, color, outlineSize, outlineColor, showBox, boxColor); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void renderProcessing(ProcessingTarget target, String label, float x, float y, int fontSize, Color color, float outlineSize, Color outlineColor, boolean showBox, Color boxColor) {
            if(!textOn)return;
            if (label != null) {
                
                PGraphics graphics = target.getGraphics();
                //
                graphics.fill(color.getRGB(), 255f);

                //target.getGraphics().text //graphics.textFont;
                //System.out.println(label);
                if (label.contains("|")) {
                    
                    try {
                        graphics.beginText();
                        PFont f = new PFont(frmMain.getInstance().getFont(), true);
                        graphics.textFont = f;
                        graphics.textSize = 8;
                        String[] l = label.split(":");
                        //System.out.println(Arrays.toString(l));
                        graphics.text(l[l.length-1], 15 + x + graphics.textSize, y);
                        graphics.endText();
                    } catch (Exception e) {
                        logger.print(e);
                    }
                }
                // 
            }
        }

        @Override
        public void renderSVG(SVGTarget target, Node node, String label, float x, float y, int fontSize, Color color, float outlineSize, Color outlineColor, boolean showBox, Color boxColor) {
            super.renderSVG(target, node, label, x, y, fontSize, color, outlineSize, outlineColor, showBox, boxColor); //To change body of generated methods, choose Tools | Templates.
        }
    
}
