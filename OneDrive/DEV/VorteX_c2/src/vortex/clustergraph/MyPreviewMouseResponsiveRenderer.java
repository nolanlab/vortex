/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustergraph;

import org.gephi.preview.api.Item;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperties;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.api.RenderTarget;
import org.gephi.preview.spi.ItemBuilder;
import org.gephi.preview.spi.MouseResponsiveRenderer;
import org.gephi.preview.spi.PreviewMouseListener;
import org.gephi.preview.spi.Renderer;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Nikolay
 */
@ServiceProvider(service = Renderer.class)
public class MyPreviewMouseResponsiveRenderer implements MouseResponsiveRenderer, Renderer {

    @Override
    public boolean needsPreviewMouseListener(
            PreviewMouseListener previewMouseListener) {
        return previewMouseListener instanceof MyPreviewMouseListener;
    }

    public boolean needsItemBuilder(ItemBuilder itemBuilder, PreviewProperties properties) {
        return true;
    }

    public boolean isRendererForitem(Item item, PreviewProperties properties) {
        //System.out.println(item.getClass());
        return true;// item instanceof NodeItem;
    }

    public void render(Item item, RenderTarget target, PreviewProperties properties) {
    }

    public void preProcess(PreviewModel previewModel) {
        // System.out.println("preProcess Called");
    }

    public PreviewProperty[] getProperties() {
        return new PreviewProperty[0];
    }

    public String getDisplayName() {
        return "My renderer";
    }
}
