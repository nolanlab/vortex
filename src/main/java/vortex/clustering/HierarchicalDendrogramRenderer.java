/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * HierarchicalDendrogramPanel.java
 *
 * Created on 26-Jan-2011, 16:34:11
 */
package vortex.clustering;

import sandbox.clustering.BarCode;
import sandbox.clustering.DistanceMeasure;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import sandbox.clustering.Cluster;
import sandbox.clustering.ClusterSet;
import sandbox.clustering.Datapoint;
import java.sql.SQLException;
import sandbox.clustering.Dataset;
import util.IO;
import util.MatrixOp;
import vortex.gui.BarCodeTableCellRenderer;
import vortex.gui.frmMain;
import util.logger;
import vortex.util.ConnectionManager;

/**
 *
 * @author Nikolay
 */
public class HierarchicalDendrogramRenderer extends javax.swing.JPanel {

    private static final boolean labels_on = false;
    private static final long serialVersionUID = 1L;
    private HierarchicalClusterTree dg;
    private DistanceMeasure dm;
    private double minDist;
    private double maxDist;
    private Dataset ds;
    private ClusterSet[] css = new ClusterSet[0];
    private int rowHeight = 10;
    private int scaleHeight = 10;

    /**
     * Creates new form HierarchicalDendrogramPanel
     */
    public HierarchicalDendrogramRenderer() {
        initComponents();
    }

    public ClusterSet[] getClusterSets() {
        return css;
    }

    public double getRowHeight() {
        return rowHeight;
    }

    private boolean rainbowRows = false;

    public void setRainbowRows(boolean rbrows) {
        this.rainbowRows = rbrows;
        this.repaint();
    }

    public void setRowHeight(int rowHeight) {
        this.rowHeight = rowHeight;
        if (dg == null) {
            return;
        }
        this.repaint();
        this.setMinimumSize(new Dimension(profile_panel_width + 100, this.dg.getRoot().getDatapoints().length * this.rowHeight + scaleHeight));
        this.setPreferredSize(getMinimumSize());
    }
    Double[] availableThresholds = null;
    boolean[] selectedThresholds = null;

    public void setDendrogram(HierarchicalClusterTree dg) {
        this.dg = dg;
        dm = dg.getDistanceMeasure();
        maxDist = dg.getRoot().getDistBtwChildren();
        minDist = maxDist;
        ds = dg.getDataset();
        setRowHeight(rowHeight);
        minDist = dm.getDistanceBounds()[0];

        ArrayList<Double> thresholds = new ArrayList<Double>();

        HierarchicalCentroid hc = dg.getRoot();
        @SuppressWarnings("unchecked")
        Enumeration<HierarchicalCentroid> enu = hc.breadthFirstEnumeration();

        while (enu.hasMoreElements()) {
            double dist = enu.nextElement().getDistBtwChildren();
            if (dist > minDist * 0.0001) {
                if (!thresholds.contains(dist)) {
                    thresholds.add(dist);
                }
            }
        }

        availableThresholds = thresholds.toArray(new Double[thresholds.size()]);

        Arrays.sort(availableThresholds, new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return (int) Math.signum(o2 - o1);
            }
        });
        selectedThresholds = new boolean[availableThresholds.length];
    }

    public void cutDendrogram() {
        ArrayList<Integer> alIdx = new ArrayList<Integer>();
        for (int i = 0; i < selectedThresholds.length; i++) {
            if (selectedThresholds[i]) {
                alIdx.add(i);
            }
        }
        if (alIdx.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please click on the dendrogram to set the cutting thresholds");
            return;
        }
        css = new ClusterSet[alIdx.size()];
        int batchID = 0;
        try {
            ConnectionManager.getStorageEngine().getNextClusterSetBatchID(ds.getID());
        } catch (SQLException e) {
            logger.showException(e);
        }

        for (int i = 0; i < css.length; i++) {
            double th = availableThresholds[alIdx.get(i)];
            //String s = String.valueOf(th);
            //s = s.substring(0, Math.min(4, s.length()));
            Cluster[] cl = HierarchicalClusteringCore.cutTree(dg, th, dm);
            css[i] = new ClusterSet(batchID, ds, cl, dm, dg.getClusteringMethod(), dg.getLinkageType().toString(), dm.distanceToSimilarity(th), "");
        }
        if (JDialog.class.isAssignableFrom(this.getParent().getClass()) || JFrame.class.isAssignableFrom(this.getParent().getClass())) {
            this.getParent().setVisible(false);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = this.getWidth();
        int h = this.getHeight() - scaleHeight;
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.BLUE);
        if (dg == null) {
            return;
        }

        recursiveDrawSegment((Graphics2D) g, dg.getRoot(), 1, w - (profile_panel_width + label_panel_width), dg.getDataset().getDatapoints().length);
        g.setColor(Color.RED);
        double distScale = (w - (profile_panel_width + label_panel_width)) / (maxDist - minDist);
        for (int i = 0; i < selectedThresholds.length; i++) {
            if (selectedThresholds[i]) {
                int x = (int) (distScale * (maxDist - availableThresholds[i]));
                g.drawLine(x, 0, x, h);
            }
        }
    }
    private int MIN_CLUSTER_SIZE = 1;
    private final int profile_panel_width = 400;
    private int label_panel_width = 250;

    private double recursiveDrawSegment(Graphics2D g2, HierarchicalCentroid h, double yOffset, int globalW, int totalDSsize) {
        if (h.isLeaf()) {
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, (int) (rowHeight * 0.9)));
            BarCodeTableCellRenderer rend = new BarCodeTableCellRenderer();
            final Datapoint[] d = h.getDatapoints();
            for (int i = 0; i < d.length; i++) {
                final int idx = 0;
                rend.setValue(new BarCode() {

                    @Override
                    public String[] getParameterNames() {
                        return ds.getFeatureNamesCombined();
                    }

                    @Override
                    public double[] getProfile() {
                        double[] val = getRawValues();
                        /*double[] max = MatrixOp.concat(ds.getDataBounds()[1], ds.getSideDataBounds()[1]);
                        double[] min = MatrixOp.concat(ds.getDataBounds()[0], ds.getSideDataBounds()[0]);
                        val = MatrixOp.diff(val, min);
                        double[] scl = MatrixOp.diff(max, min);
                        for (int j = 0; j < val.length; j++) {
                            val[j] /= scl[j];
                        }*/
                        val = MatrixOp.toUnityLen(val);
                        MatrixOp.mult(val,5);
                        return val;
                    }

                    @Override
                    public double[] getRawValues() {
                        return MatrixOp.concat(d[idx].getVector(), d[idx].getSideVector());
                    }

                    @Override
                    public int getSideVectorBeginIdx() {
                        return ds.getNumDimensions() + 1;
                    }

                });
                rend.setBounds(0, 0, profile_panel_width, rowHeight);
                //logger.print(d[i].getBarCode().getProfile());

                if (rainbowRows) {
                    BarCodeTableCellRenderer.setConfig(new BarCodeTableCellRenderer.BarCodeOptions(BarCodeTableCellRenderer.getOptions().getCOLOR_UP(), BarCodeTableCellRenderer.getOptions().getCOLOR_UP(), BarCodeTableCellRenderer.BarcodePaintStyle.STRIPES, 1, rainbowRows));
                }

                g2.translate(globalW, (yOffset + i * rowHeight));
                rend.paintComponent(g2);
                g2.translate(-globalW, -(yOffset + i * rowHeight));
            }
            g2.setColor(new Color(0, 50, 150));
            if (d.length > 1) {
                g2.drawRect(globalW, (int) yOffset, profile_panel_width, rowHeight * d.length);
            }
            double mid = yOffset + (rowHeight * (h.getDatapoints().length / 2.0));
            Rectangle r = g2.getClipBounds();
            //g2.setClip(globalW + profile_panel_width, (int) yOffset, label_panel_width, rowHeight * d.length);
            String label = h.label;
            if (h.getDatapoints().length == 1) {
                label = h.getDatapoints()[0].getFullName();
            }

            logger.print(yOffset, label);
            g2.drawString(label, globalW + profile_panel_width + 2, (float) mid + 5);

            if (labels_on) {
                g2.setPaint(Color.BLACK);
                String dist = h.getLabel().replaceAll("\\(.*\\)", "");
                g2.drawString(dist, (float) globalW - g2.getFontMetrics().stringWidth(dist), (float) mid);
            }

            g2.setClip(r);
            return mid;
        }

        double distScale = globalW / (maxDist - minDist);
        double currX = distScale * (maxDist - h.getDistBtwChildren());
        HierarchicalCentroid child1 = (HierarchicalCentroid) h.getChildAt(0);
        HierarchicalCentroid child2 = (HierarchicalCentroid) h.getChildAt(1);
        if (child1.label != null) {
            if (child1.label.contains("<")) {
                child1 = (HierarchicalCentroid) h.getChildAt(1);
                child2 = (HierarchicalCentroid) h.getChildAt(0);
            }
        }

        if (child1.getDatapoints().length < child2.getDatapoints().length) {
            child1 = (HierarchicalCentroid) h.getChildAt(1);
            child2 = (HierarchicalCentroid) h.getChildAt(0);
        }

        double child1X = globalW;
        if (child1.getDatapoints().length > MIN_CLUSTER_SIZE) {
            child1X = distScale * (maxDist - child1.getDistBtwChildren());
        }
        double child2X = globalW;
        if (child2.getDatapoints().length > MIN_CLUSTER_SIZE) {
            child2X = distScale * (maxDist - child2.getDistBtwChildren());
        }

        double child2yOffset = yOffset + (rowHeight * child1.getDatapoints().length);

        double child1Y = recursiveDrawSegment(g2, child1, yOffset, globalW, totalDSsize);
        double child2Y = recursiveDrawSegment(g2, child2, child2yOffset, globalW, totalDSsize);

        double child1Ycent = child1Y; //yOffset + (child1.getDatapoints().length * rowHeight)/2.0;
        double child2Ycent = child2Y; //child2yOffset + (child2.getDatapoints().length * rowHeight)/2.0;

        //g2.setPaint(Color.BLACK);
        g2.setPaint(Color.BLACK);
        g2.drawLine((int) currX, (int) child1Ycent, (int) currX, (int) ((child1Ycent + child2Ycent) / 2.0));
        g2.drawLine((int) currX, (int) child1Ycent, (int) child1X, (int) child1Ycent);
        //g2.setPaint(Color.BLUE);
        g2.drawLine((int) currX, (int) child2Ycent, (int) child2X, (int) child2Ycent);
        g2.drawLine((int) currX, (int) child2Ycent, (int) currX, (int) ((child1Ycent + child2Ycent) / 2.0));

        double mid = (child1Y + child2Y) / 2.0;
        if (labels_on && h.getLabel() != null) {
            g2.setPaint(Color.BLACK);
            String dist = h.getLabel().replaceAll("\\(.*\\)", "");
            g2.drawString(dist, (float) currX, (float) mid);
        }
        return (child1Y + child2Y) / 2.0;

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pmDendrogram = new javax.swing.JPopupMenu();
        pmiSaveImage = new javax.swing.JMenuItem();

        pmiSaveImage.setText("Save Image As...");
        pmiSaveImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiSaveImageActionPerformed(evt);
            }
        });
        pmDendrogram.add(pmiSaveImage);

        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                formMouseReleased(evt);
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                formMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 672, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 512, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void formMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseReleased
        if (dg != null) {
            int w = this.getWidth() - (profile_panel_width + label_panel_width);
            int mouseX = evt.getX();
            double distScale = w / (maxDist - minDist);
            if (mouseX >= w) {
                return;
            }
            int minIdx = -1;
            double minDistance = Double.MAX_VALUE;
            for (int i = 0; i < selectedThresholds.length; i++) {
                int x = (int) (distScale * (maxDist - availableThresholds[i]));
                double currDist = Math.abs(x - mouseX);
                if (currDist < minDistance) {
                    minDistance = currDist;
                    minIdx = i;
                }
            }
            selectedThresholds[minIdx] = !selectedThresholds[minIdx];
            repaint();
        }
    }//GEN-LAST:event_formMouseReleased

    private void pmiSaveImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiSaveImageActionPerformed
        BufferedImage img = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_RGB);
        this.paintComponent(img.createGraphics());
        img.flush();
        try {
            ImageIO.write(img, "PNG", IO.chooseFileWithDialog("HierarchicalDendrogramPanel.SaveImage", "PNG files", new String[]{"*.png"}, true));
        } catch (IOException e) {
            logger.showException(e);
        }
    }//GEN-LAST:event_pmiSaveImageActionPerformed

    private void formMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseClicked
        if (evt.getButton() == MouseEvent.BUTTON3) {
            pmDendrogram.show(this, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_formMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPopupMenu pmDendrogram;
    private javax.swing.JMenuItem pmiSaveImage;
    // End of variables declaration//GEN-END:variables
}
