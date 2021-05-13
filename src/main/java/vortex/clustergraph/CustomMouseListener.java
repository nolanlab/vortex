/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustergraph;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.Node;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewMouseEvent;
import org.gephi.preview.api.PreviewProperties;

import org.gephi.preview.spi.PreviewMouseListener;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import vortex.gui.frmMain;

@ServiceProvider(service = PreviewMouseListener.class, position = 001)
public class CustomMouseListener implements PreviewMouseListener {

    private PreviewController previewController;

    @Override
    public void mouseClicked(PreviewMouseEvent event, PreviewProperties properties, Workspace workspace) {
        
        System.out.println("MouseClicked: " + event.toString());
        if (previewController == null) {
            previewController = Lookup.getDefault().lookup(PreviewController.class);
        }

        for (Node node : Lookup.getDefault().lookup(GraphController.class).getGraphModel(workspace).getGraph().getNodes()) {
            if (clickingInNode(node, event)) {
                properties.putValue("display-label.node.id", node.getId());
                //Logger.getLogger("").log(Level.INFO, "Node {0} clicked!", node.getLabel());
                //JOptionPane.showMessageDialog(null, "Node " + node.getLabel() + " clicked!");
                frmMain.getInstance().getClusterSetBrowser().selectCluster((int) node.getAttribute("cluster"), true);
                event.setConsumed(true);//So the renderer is executed and the graph repainted
                previewController.refreshPreview(workspace);//So our ItemBuilderTemplate gets called and builds the new item
                return;
            }
        }
        
        /*
                    if (clickingInNode(node, event)) {
                node.setAttribute("selected", true);
                logger.print("selected:" + node.getAttribute("cluster"));
                event.setConsumed(true);//So the renderer is executed and the graph repainted
                previewController.refreshPreview(workspace);//So our ItemBuilderTemplate gets called and builds the new item
                return;
            } else {
                node.setAttribute("selected", false);
            }
            if ((boolean) node.getAttribute("selected")) {
                frmMain.getInstance().getClusterSetBrowser().selectCluster((int) node.getAttribute("cluster"), true);
            }
        */

        properties.removeSimpleValue("display-label.node.id");
        event.setConsumed(true);//So the renderer is executed and the graph repainted
        previewController.refreshPreview(workspace);//So our ItemBuilderTemplate gets called and no longer builds the item
    }

    @Override
    public void mousePressed(PreviewMouseEvent event, PreviewProperties properties, Workspace workspace) {
    }

    @Override
    public void mouseDragged(PreviewMouseEvent event, PreviewProperties properties, Workspace workspace) {
    }

    @Override
    public void mouseReleased(PreviewMouseEvent event, PreviewProperties properties, Workspace workspace) {
    }

    private boolean clickingInNode(Node node, PreviewMouseEvent event) {
        float xdiff = node.x() - event.x;
        float ydiff = -node.y() - event.y;//Note that y axis is inverse for node coordinates
        float radius = node.size();
        
       double distSq =  xdiff * xdiff + ydiff * ydiff;
       double radiusSq = radius * radius;
              
        System.out.println("DistSq: "+ distSq + ", radiusSq = " + radiusSq);

        return distSq < radiusSq;
    }
}