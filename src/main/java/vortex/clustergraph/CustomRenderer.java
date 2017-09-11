/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustergraph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.logging.Logger;
import org.gephi.graph.api.Node;
import org.gephi.preview.api.CanvasSize;
import org.gephi.preview.api.G2DTarget;
import org.gephi.preview.api.Item;
import org.gephi.preview.api.PDFTarget;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperties;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.api.RenderTarget;
import org.gephi.preview.api.SVGTarget;
import org.gephi.preview.spi.ItemBuilder;
import org.gephi.preview.spi.MouseResponsiveRenderer;
import org.gephi.preview.spi.PreviewMouseListener;
import org.gephi.preview.spi.Renderer;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = Renderer.class)
public class CustomRenderer implements Renderer, MouseResponsiveRenderer {

    @Override
    public String getDisplayName() {
        return "CustomRenderer";
    }

    @Override
    public void preProcess(PreviewModel previewModel) {
    }

    @Override
    public void render(Item item, RenderTarget target, PreviewProperties properties) {
        //Retrieve clicked node for the label:
        CustomItem label = (CustomItem) item;
        Node node = label.getNode();
        
        Logger.getLogger("").info(label.toString());
        
        //Finally draw your graphics for the node label in each target (or just java2d):
        if (target instanceof G2DTarget) {
            Graphics2D g = ((G2DTarget) target).getGraphics();
            
            g.setColor(Color.BLACK);
            
            float x = node.x();
            float y = - node.y();//Note that y axis is inverse for node coordinates
            float size = 6;
            
            x = x - (size / 2f);
            y = y - (size / 2f);
            Ellipse2D.Float ellipse = new Ellipse2D.Float(x, y, size, size);
            g.fill(ellipse);
            g.setColor(Color.red);
            g.draw(ellipse);
            
        } else if (target instanceof PDFTarget) {
        } else if (target instanceof SVGTarget) {
        }
    }

    @Override
    public PreviewProperty[] getProperties() {
        return new PreviewProperty[0];
    }

    @Override
    public boolean isRendererForitem(Item item, PreviewProperties properties) {
        return item instanceof CustomItem;
    }

    @Override
    public boolean needsItemBuilder(ItemBuilder itemBuilder, PreviewProperties properties) {
        return itemBuilder instanceof ItemBuilder;
    }

    @Override
    public boolean needsPreviewMouseListener(PreviewMouseListener pl) {
        return pl instanceof CustomMouseListener;
    }

    @Override
    public CanvasSize getCanvasSize(Item item, PreviewProperties props) {
        CustomItem label = (CustomItem) item;
        Node node = label.getNode();
        return new CanvasSize(node.x(), node.y(), 5, 5);
    }
}