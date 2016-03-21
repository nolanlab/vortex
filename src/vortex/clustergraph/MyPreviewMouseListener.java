/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustergraph;

import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeIterable;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewMouseEvent;
import org.gephi.preview.api.PreviewProperties;
import org.gephi.preview.spi.PreviewMouseListener;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import vortex.gui.frmMain;

/**
 *
 * @author Nikolay
 */
@ServiceProvider(service = PreviewMouseListener.class)
public class MyPreviewMouseListener implements PreviewMouseListener {

    @Override
    public void mouseClicked(PreviewMouseEvent event, PreviewProperties properties, Workspace workspace) {
        return;
    }

    @Override
    public void mousePressed(PreviewMouseEvent event,
            PreviewProperties properties, Workspace workspace) {
        //System.out.println("I'm pressed!!: x:" + event.x + ", y:" + event.y);     
        // if(true) return;

        float x = event.x;
        float y = -event.y;
        GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
        Graph g = graphModel.getGraph();

        float minDist = Float.MAX_VALUE;
        Node nnNode = null;
        NodeIterable ni = g.getNodes();
        boolean clearSelection = event.keyEvent == null;
        if (event.keyEvent != null) {
            if (event.keyEvent.isAltDown() || event.keyEvent.isAltGraphDown()) {

                for (Node n : ni.toArray()) {
                    if ((Boolean) n.getAttributes().getValue("selected")) {
                        n.getNodeData().setX(x);
                        n.getNodeData().setY(y);
                    }
                }
                PreviewController previewController = Lookup.getDefault().lookup(PreviewController.class);
                //nnNode.getNodeData().setColor(0, 0.99f, 0);
                previewController.refreshPreview();
                return;
            }
        }

        if (event.keyEvent != null) {
            clearSelection = !event.keyEvent.isControlDown();
        }

        for (Node n : ni.toArray()) {
            if (clearSelection) {
                n.getAttributes().setValue("selected", false);
            }
            float sqdist = (n.getNodeData().x() - x) * (n.getNodeData().x() - x);
            sqdist += (n.getNodeData().y() - y) * (n.getNodeData().y() - y);
            if (sqdist < minDist) {
                nnNode = n;
                minDist = sqdist;
            }
        }

        System.out.println("Nearest node: " + nnNode.getNodeData().getId() + ", " + nnNode.getNodeData().getLabel() + " minDist " + minDist);
        if (minDist < (float) nnNode.getNodeData().getSize() * 10) {
            if (nnNode.getAttributes().getValue("cluster") != null) {
                int clID = (int) nnNode.getAttributes().getValue("cluster");
                frmMain.getInstance().getClusterSetBrowser().selectCluster(clID, clearSelection);
                nnNode.getAttributes().setValue("selected", true);
            }
        }

        //properties.putValue("display-label.node.id", nnNode.getNodeData().getId());
        PreviewController previewController = Lookup.getDefault().lookup(PreviewController.class);
        //nnNode.getNodeData().setColor(0, 0.99f, 0);
        previewController.refreshPreview();
        //nnNode.getNodeData().setColor(0, 0.99f, 0);

    }

    @Override
    public void mouseDragged(PreviewMouseEvent event,
            PreviewProperties properties, Workspace workspace) {
    }

    @Override
    public void mouseReleased(PreviewMouseEvent event,
            PreviewProperties properties, Workspace workspace) {
    }
}
