/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustergraph;


import java.util.logging.Level;
import java.util.logging.Logger;
import org.gephi.graph.api.Graph;
import org.gephi.preview.api.Item;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewProperties;
import org.gephi.preview.spi.ItemBuilder;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = ItemBuilder.class)
public class CustomItemBuilder implements ItemBuilder {

    @Override
    public Item[] getItems(Graph graph) {
        Workspace workspace = Lookup.getDefault().lookup(ProjectController.class).getCurrentWorkspace();
        PreviewProperties properties = Lookup.getDefault().lookup(PreviewController.class).getModel(workspace).getProperties();

        Logger.getLogger("").log(Level.INFO, "Has node: {0}", properties.hasProperty("display-label.node.id"));
        if (properties.hasProperty("display-label.node.id")) {
            String nodeId = properties.getStringValue("display-label.node.id");
            return new Item[]{new CustomItem(graph.getNode(nodeId))};
        } else {
            return new Item[0];
        }
    }

    @Override
    public String getType() {
        return "some.type-label";
    }
}