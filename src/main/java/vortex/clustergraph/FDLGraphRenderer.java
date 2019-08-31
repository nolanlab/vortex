/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustergraph;

import sandbox.clustering.Cluster;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.UndirectedGraph;
import util.ColorPalette;
import util.ColorScale;
import vortex.clustergraph.panScFDL.ClusterNode;

/**
 *
 * @author Nikolay Samusik
 */
public class FDLGraphRenderer extends javax.swing.JPanel {

    private UndirectedGraph graph = null;

    private int coloringParamIDX = 0;
    private ClusterNode[] cn;
    private boolean showEdges = false;
    private int nodeSize = 2;
    private final Color gr = new Color(127, 127, 127, 127);
    private BufferedImage scaleImg = null;
    private ColorScale colorScale;

    public void setNodeSize(int nodeSize) {
        this.nodeSize = nodeSize;
    }

    private final static int grey_argb = getARGBInt(200, 175, 175, 175);

    public int getNodeSize() {
        return nodeSize;
    }

    public void showEdges(boolean showEdges) {
        this.showEdges = showEdges;
    }

    /**
     * Creates new form GraphRenderer
     */
    private boolean ctrlPressed = false;

    public FDLGraphRenderer(ClusterNode[] cn, ColoringMode cm, ColorScale colorScale) {
        initComponents();
        this.mode = cm;
        this.cn = cn;
        setBackground(Color.WHITE);
        this.colorScale = colorScale;
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e); //To change body of generated methods, choose Tools | Templates.
                if (e.getButton() == MouseEvent.BUTTON1) {
                    int x = e.getX();
                    int y = e.getY();
                    
                    selectNodeByCoord(x, y, e.isControlDown());
                }
            }

        });

      
    }

    private void selectNodeByCoord(int x, int y, boolean keepSel) {
        Node[] nodes = graph.getNodes().toArray();

        if (nodes.length < 1) {
            return;
        }

        float minX = nodes[0].x(), maxX = nodes[0].x(), minY = nodes[0].y(), maxY = nodes[0].y();

        for (Node n : nodes) {
            minX = Math.min(minX, n.x());
            maxX = Math.max(maxX, n.x());
            minY = Math.min(minY, n.y());
            maxY = Math.max(maxY, n.y());
        }

        maxX += nodeSize * 2;
        minX -= nodeSize * 2;
        maxY += nodeSize * 2;
        minY -= nodeSize * 2;

        float scaleX = this.getWidth() / (maxX - minX);
        float scaleY = this.getHeight() / (maxY - minY);

        float graphX = (x / scaleX) + minX;//int x = (int) ((n.x() - minX) * scaleX);
        float graphY = (y / scaleY) + minY;

        float mindist = (10f * nodeSize) / Math.min(scaleX, scaleY);
        Node nn = null;

        for (Node node : nodes) {
            if (graph.contains(node)) {
                float dist = dist(graphX, node.x(), graphY, node.y());
                if (dist < mindist) {
                    mindist = dist;
                    nn = node;
                }
                Cluster c = cn[((Integer) node.getAttribute("clusterNode"))].cluster;

                if (!keepSel) {
                    if (c.isSelected()) {
                        c.setSelected(false);
                    }
                }
            }
        }

        if (nn != null) {
            boolean isSel = cn[((Integer) nn.getAttribute("clusterNode"))].cluster.isSelected();
            cn[((Integer) nn.getAttribute("clusterNode"))].cluster.setSelected(!isSel);
        }
    }

    private float dist(float x1, float x2, float y1, float y2) {

        float dist = (float) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
        return dist;
    }

    public void setGraph(UndirectedGraph graph) {
        this.graph = graph;
    }

    public enum ColoringMode {
        CLUSTER, GROUP, EXPRESSION
    };

    public void setColoringParamIDX(int coloringParamIDX) {
        this.coloringParamIDX = coloringParamIDX;
    }

    public void setScaleImg(BufferedImage scaleImg) {
        this.scaleImg = scaleImg;
    }

    private ColoringMode mode;

    public ColoringMode getColoringMode() {
        return mode;
    }

    public void setColoringMode(ColoringMode mode) {
        this.mode = mode;
    }

    private ColorPalette cp = ColorPalette.NEUTRAL_PALETTE;

    public void setPalette(ColorPalette cp) {
        this.cp = cp;
    }

    private boolean simpleRendering = true;

    public void useSimpleRendering(boolean s) {
        this.simpleRendering = s;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); //To change body of generated methods, choose Tools | Templates.

        if (graph == null) {
            return;
        }

        if (graph.getNodeCount() == 0) {
            return;
        }

        Node[] nodes = graph.getNodes().toArray();

        if (nodes.length < 1) {
            return;
        }

        float minX = nodes[0].x(), maxX = nodes[0].x(), minY = nodes[0].y(), maxY = nodes[0].y();

        for (Node n : nodes) {
            minX = Math.min(minX, n.x());
            maxX = Math.max(maxX, n.x());
            minY = Math.min(minY, n.y());
            maxY = Math.max(maxY, n.y());
        }

        BufferedImage bi = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) bi.createGraphics();

        RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        rh.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        rh.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHints(rh);

        maxX += nodeSize * 2;
        minX -= nodeSize * 2;
        maxY += nodeSize * 2;
        minY -= nodeSize * 2;

        float scaleX = this.getWidth() / (maxX - minX);
        float scaleY = this.getHeight() / (maxY - minY);

        if (!simpleRendering && showEdges) {
            g2.setStroke(new BasicStroke(0.5f));
            g2.setPaint(new Color(100, 100, 100, 20));
            System.out.println("Painting edges");
            for (Node n : nodes) {
                if (!graph.contains(n)) {
                    continue;
                }

                int x = (int) ((n.x() - minX) * scaleX);
                int y = (int) ((n.y() - minY) * scaleY);
                Node[] nei = graph.getNeighbors(n).toArray();
                for (Node n2 : nei) {
                    if (n2.hashCode() < n.hashCode() || !graph.contains(n2)) {
                        continue;
                    }
                    int x2 = (int) ((n2.x() - minX) * scaleX);
                    int y2 = (int) ((n2.y() - minY) * scaleY);
                    //g2.setStroke(new BasicStroke(1.0f)); //new BasicStroke((getNodeSize() / 4.0f) * (float) graph.getEdge(n, n2).getWeight()));
                    g2.drawLine(x, y, x2, y2);
                }

            }
        }

        for (Node n : nodes) {
            //Cluster c = cn[((Integer) n.getAttribute("clusterNode"))].cluster;
            if (!graph.contains(n)) {
                continue;
            }
            int x = (int) ((n.x() - minX) * scaleX);
            int y = (int) ((n.y() - minY) * scaleY);

            g2.setPaint(gr);

            g2.fillOval(x - nodeSize / 2, y - nodeSize / 2, Math.max(nodeSize, 1), Math.max(nodeSize, 1));
        }

        if (simpleRendering || mode == ColoringMode.CLUSTER || mode == ColoringMode.GROUP) {

            for (Node n : nodes) {
                Cluster c = cn[((Integer) n.getAttribute("clusterNode"))].cluster;
                if (!(graph.contains(n) && c.isSelected())) {
                    continue;
                }
                int gid = (int) n.getAttribute("groupID");
                if (gid < 0) {
                    continue;
                }
                int x = (int) ((n.x() - minX) * scaleX);
                int y = (int) ((n.y() - minY) * scaleY);
                Color col2 = null;

                switch (mode) {
                    case CLUSTER:
                        col2 = c.getColorCode();
                        break;
                    case GROUP:
                        col2 = cp.getColor(gid);
                        break;
                    case EXPRESSION:
                        col2 = colorScale.getColor(coloringParamIDX, (double) n.getAttribute("expValue"));
                        break;
                }

                g2.setPaint(new Color(col2.getRed(), col2.getGreen(), col2.getBlue(), 200));
                g2.fillOval(x - nodeSize / 2, y - nodeSize / 2, Math.max(nodeSize, 1), Math.max(nodeSize, 1));

            }
        } else {

            double[][] avgExpVal = new double[bi.getWidth()][bi.getHeight()];
            double[][] weights = new double[bi.getWidth()][bi.getHeight()];
            Node[][] nodeMap = new Node[bi.getWidth()][bi.getHeight()];
            for (Node n : nodes) {
                int x = (int) ((n.x() - minX) * scaleX);
                int y = (int) ((n.y() - minY) * scaleY);

                int groupID = (int) n.getAttribute("groupID");
                Cluster c = cn[((Integer) n.getAttribute("clusterNode"))].cluster;
                if (!c.isSelected()) {
                    groupID = -1;
                }

                double v = ((double) n.getAttribute("expValue"));

                if (x > 0 && y > 0 && x < bi.getWidth() && y < bi.getHeight()) {
                    for (int oX = 0; oX < nodeSize; oX++) {
                        for (int oY = 0; oY < nodeSize; oY++) {
                            try {
                                if (nodeSize > 3 && (oX == 0 || oX == (nodeSize - 1)) && (oY == 0 || oY == (nodeSize - 1))) {
                                    continue;
                                }

                                if (nodeSize == 3 && (oX == 0 || oX == (nodeSize - 1)) && (oY == 0 || oY == (nodeSize - 1))) {
                                    if (groupID >= 0) {
                                        avgExpVal[x + oX - nodeSize / 2][y + oY - nodeSize / 2] += (v / 2);
                                    }
                                    weights[x + oX - nodeSize / 2][y + oY - nodeSize / 2] += 0.5;
                                    nodeMap[x + oX - nodeSize / 2][y + oY - nodeSize / 2] = n;
                                    continue;
                                }

                                if (groupID >= 0) {
                                    avgExpVal[x + oX - nodeSize / 2][y + oY - nodeSize / 2] += (v);
                                    weights[x + oX - nodeSize / 2][y + oY - nodeSize / 2] += 1;
                                }

                                nodeMap[x + oX - nodeSize / 2][y + oY - nodeSize / 2] = n;
                            } catch (java.lang.ArrayIndexOutOfBoundsException e) {

                            }
                        }
                    }
                }
            }

            for (int x = 0; x < bi.getWidth(); x++) {
                for (int y = 0; y < bi.getHeight(); y++) {
                    if (weights[x][y] > 0) {
                        Color col2 = colorScale.getColor(coloringParamIDX, avgExpVal[x][y] / weights[x][y]);
                        int col = getARGBInt((int) (255 * Math.min(weights[x][y], 1.0)), col2.getRed(), col2.getGreen(), col2.getBlue());
                        bi.setRGB(x, y, col);
                    }
                }
            }
        }

        g.drawImage(bi,
                0, 0, null);

        if (scaleImg != null) {
            g.drawImage(scaleImg, 1, getHeight() - scaleImg.getHeight(), this);
        }

    }

    public static int getARGBInt(int a, int r, int g, int b) {
        int color = 0;
        color |= a << 24;

        color |= r << 16;

        color |= g << 8;

        color |= b;

        return color;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
