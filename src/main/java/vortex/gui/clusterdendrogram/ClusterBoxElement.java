/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.gui.clusterdendrogram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map.Entry;
import javax.swing.JLabel;
import javax.swing.border.LineBorder;
import javax.swing.event.MouseInputAdapter;
import javax.swing.tree.DefaultMutableTreeNode;
import samusik.hideabletree.HideableMutableTreeNode;
import annotations.Annotation;
import clustering.Cluster;
import clustering.Dataset;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import util.ClusterValidationMeasure;
import util.ColorPalette;
import util.logger;
import vortex.gui.frmMain;

/**
 *
 * @author Nikolay
 */
public class ClusterBoxElement extends JLabel implements zOrdered {

    private static final long serialVersionUID = 1L;
    private GridBagConstraints gbConst;
    public Cluster cluster;
    public double matchToParent;
    private boolean colorCodeGenerated = true;
    private HideableMutableTreeNode<ClusterBoxElement> node = new HideableMutableTreeNode<ClusterBoxElement>(this);
    java.awt.geom.Point2D scale = new java.awt.geom.Point2D.Double(1.0, 1.0);
    private String caption = "";
    private boolean showCaption = false;
    private ClusterValidationMeasure cvm;
    private Rectangle initBounds;
    private int initialDragX = -1;
    private String selectedAnnClass = null;
    private ArrayList<ClassLabel> labels = new ArrayList<>();

    public boolean setSelectedAnnClass(String selectedAnnClass) {
        this.selectedAnnClass = selectedAnnClass;
        if (labels.size() > 0) {
            return labels.get(0).className.equals(selectedAnnClass);
        } else {
            return false;
        }
    }

    @Override
    public int getZOrder() {
        if (labels.size() > 0 && selectedAnnClass != null) {
            if (labels.get(0).className.equals(selectedAnnClass)) {
                return 4;
            }
        }
        if (hasFocus()) {
            return 3;
        }
        if (getCaption().length() > 0) {
            return 2;
        } else {
            return 1;
        }
    }

    private class ClassLabel {

        public final String className;
        public final Color classColor;
        private final int elementsInCluster;
        private final int classSize;

        public int numElementsInCluster() {
            return (int) elementsInCluster;
        }

        public int getClassSize() {
            return classSize;
        }

        public ClassLabel(String className, Color classColor, int elementsInCluster, int classSize) {
            this.className = className;
            this.classColor = classColor;
            this.elementsInCluster = elementsInCluster;
            this.classSize = classSize;
        }
    }

    public HideableMutableTreeNode<ClusterBoxElement> getNode() {
        return node;
    }

    public GridBagConstraints getGridBagConstraints() {
        return gbConst;
    }

    public void setGridBagConstraints(GridBagConstraints gbConst) {
        this.gbConst = gbConst;
    }

    public void setColorCodeGenerated(boolean colorCodeGenerated) {
        this.colorCodeGenerated = colorCodeGenerated;
    }

    public boolean isColorCodeGenerated() {
        return colorCodeGenerated;
    }

    public void cpMouseDragged(java.awt.event.MouseEvent evt) {

        if (initialDragX != -1) {
            super.setBounds(initBounds.x + ((evt.getXOnScreen() - initialDragX)), initBounds.y, initBounds.width, initBounds.height);
        }

        if (evt.getSource().equals(this)) {

            @SuppressWarnings("unchecked")
            Enumeration<HideableMutableTreeNode<ClusterBoxElement>> enu = this.getNode().breadthFirstEnumeration();
            enu.nextElement();
            while (enu.hasMoreElements()) {
                enu.nextElement().getUserObject().cpMouseDragged(evt);
            }

            this.getParent().repaint();
        }
    }

    private void initDrag(MouseEvent e) {
        initialDragX = e.getXOnScreen();

        initBounds = this.getBounds();
    }

    public void setClusterValidation(ClusterValidationMeasure measure, final boolean showPValue) {
        this.cvm = measure;
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                if (cvm == null) {
                    return;
                }
                Container c = ClusterBoxElement.this.getParent();
                if (c != null) {
                    c.repaint();
                }
                cvm.compute();
                if (c != null) {
                    c.repaint();
                }
            }
        });
        th.start();
    }

    private void cpMousePressed(MouseEvent e) {
        Enumeration<HideableMutableTreeNode<ClusterBoxElement>> enu = this.getNode().breadthFirstEnumeration();
        while (enu.hasMoreElements()) {
            enu.nextElement().getUserObject().initDrag(e);
        }
    }

    public ClusterBoxElement(Cluster cluster, double matchToParent) {
        this.cluster = cluster;
        this.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                cpMouseDragged(evt);
            }
        });
        this.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                cpMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                initialDragX = -1;

            }
        });

        this.matchToParent = matchToParent;
        this.setFocusable(true);

        if (cluster.getColorCode().equals(new Color(255, 255, 255))) {
            this.setBackground(new Color((int) Math.rint(Math.random() * 220.0), (int) Math.rint(Math.random() * 220.0), (int) Math.rint(Math.random() * 220.0)));
            colorCodeGenerated = true;
        } else {
            this.setBackground(cluster.getColorCode());
            colorCodeGenerated = false;
        }

        // double pval = Math.min(cluster.getPPIpVal(), 0.5);
        // this.setBackground(new Color(127,127,127));
        // if(pval < 0.4) this.setBackground(new Color(0,(int)(255.0*2.0*(0.5-pval)),0));
        // if(pval >= 0.75) this.setBackground(new Color((int)(255.0*(1.0-pval)),0,0));
        // colorCodeGenerated= false;
        this.setText(String.valueOf(cluster.getID()));
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                //JOptionPane.showMessageDialog(null, "X = " + gbConst.gridx + ", Y = " + gbConst.gridy + " width = " + gbConst.gridwidth);
                super.mouseClicked(evt);
                evt.getComponent().requestFocus();
                ClusterBoxElement.this.showCaption = !ClusterBoxElement.this.showCaption;

                if (evt.getButton() == evt.BUTTON3) {
                    ClusterDendrogramPlot.getPopupCluster().getComponent(0).setVisible(ClusterBoxElement.this.colorCodeGenerated);
                    ClusterDendrogramPlot.getPopupCluster().show(ClusterBoxElement.this, evt.getX(), evt.getY());
                    ClusterBoxElement.this.repaint();
                }
                evt.getComponent().getParent().repaint();

            }
        });

        this.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
               
                frmMain.getInstance().getClusteringResultList().selectClusterSet(cluster.getClusterSet());
                frmMain.getInstance().getClusterSetBrowser().selectCluster(cluster.getID(), true);
                
                frmMain.getInstance().getClusterSetBrowser().selectCluster(cluster.getID(), true);
            }
            
            

            @Override
            public void focusLost(FocusEvent e) {
                cluster.setSelected(false);

            }

        });

    }

    @Override
    public String toString() {
        return cluster.toString();
    }

    @Override
    public void printAll(Graphics g) {
        for (Component f : getComponents()) {
            if (!(f instanceof ClusterBoxElement)) {
                f.setVisible(false);
            }
        }
        paintAll(g);
        for (Component f : getComponents()) {
            f.setVisible(true);
        }
    }

    public void setAnnotation(ColorPalette palette, Annotation ann) {
        if (ann == null) {
            labels.clear();
            return;
        }
        Dataset ds = cluster.getClusterSet().getDataset();
        int i = 0;
        for (String classLabel : ann.getTerms()) {

            int classSize = 0;
            int[] pids = ann.getDpIDsForTerm(classLabel);
            for (int pid : pids) {
                if (ds.getDPByID(pid) != null) {
                    if (cluster.containsDpID(pid)) {
                        classSize++;
                    }
                }
            }
            labels.add(new ClassLabel(classLabel, palette.getColor(i++), classSize, pids.length));
            Collections.sort(labels, new Comparator<ClassLabel>() {
                @Override
                public int compare(ClassLabel o1, ClassLabel o2) {
                    return o2.elementsInCluster - o1.elementsInCluster;
                }
            });
        }
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getCaption() {
        return caption;
    }

    @Override
    protected void paintComponent(Graphics g) {
        final int round_coeff = 1;
        RoundRectangle2D rect = new RoundRectangle2D.Double(0, 0, this.getWidth(), this.getHeight(), round_coeff, round_coeff);
        Graphics2D g2 = (Graphics2D) g;

        if (!showCaption && cvm != null) {
            if (cvm.isComputed() && cvm.getHighlightThresholds() != null) {
                double m = cvm.showPValue() ? cvm.getPValue() : cvm.getMeasure();
                if (cvm.getHighlightThresholds()[0] <= m && cvm.getHighlightThresholds()[1] >= m) {
                    RoundRectangle2D rect2 = new RoundRectangle2D.Double(getX() - 3, getY() - 3, this.getWidth() + 5, this.getHeight() + 6, round_coeff, round_coeff);
                    g2.setPaint(Color.black);
                    g2.fill(rect2);

                    rect2 = new RoundRectangle2D.Double(0, 0, this.getWidth(), this.getHeight(), round_coeff, round_coeff);
                    g2.setPaint(Color.YELLOW);
                    g2.draw(rect2);
                }
            }
        }

        Shape tmp = g2.getClip();
        g2.clip(rect);

        g2.setStroke(new BasicStroke(1));

        if (false && colorCodeGenerated) {
            if (labels.isEmpty()) {
                g2.setPaint(new LinearGradientPaint(0.0f, 0.0f, 1.0f, 1.0f, new float[]{0.0f, 1.0f}, new Color[]{new Color(200, 200, 200), this.getBackground()}, CycleMethod.REFLECT));
            } else {
                g2.setPaint(new LinearGradientPaint(0.0f, 0.0f, 1.0f, 1.0f, new float[]{0.0f, 1.0f}, new Color[]{new Color(150, 150, 150), new Color(100, 100, 100)}, CycleMethod.REFLECT));
            }
        } else {
            g2.setPaint(this.getBackground());
        }
        if (labels.size() > 0) {
            g2.setPaint(this.getBackground());
        }

        if (this.hasFocus()) {
            g2.draw(rect);
        } else {
            g2.fill(rect);
        }

        double scl = (this.getWidth() - 1) / (double) cluster.size();
        int xpos = 0;
        for (ClassLabel lbl : labels) {
            g2.setPaint(new LinearGradientPaint(0.0f, 0.0f, 1.0f, 1.0f, new float[]{0.0f, 1.0f}, new Color[]{new Color(lbl.classColor.getRed(), lbl.classColor.getGreen(), lbl.classColor.getBlue(), 180), lbl.classColor}, CycleMethod.REFLECT));
            g2.fillRect((int) Math.floor(xpos * scl), 0, (int) Math.ceil(lbl.numElementsInCluster() * scl), getHeight());
            xpos += lbl.numElementsInCluster();
        }
        g2.setClip(tmp);
        g2.setPaint(Color.gray);
        // g2.draw(new RoundRectangle2D.Double(0, 0, this.getWidth() - 2, this.getHeight() - 1, round_coeff, round_coeff));
        g2.drawLine(0, 0, 0, this.getHeight());
        g2.drawLine(this.getWidth(), 0, this.getWidth(), this.getHeight());
        if (getNode().isLeaf()) {
            g2.drawLine(0, (getHeight()), (getWidth()), getHeight());
        }
        if (((DefaultMutableTreeNode) getNode().getParent()).isRoot()) {
            g2.drawLine(0, 0, getWidth(), 0);
        }

        if (this.hasFocus()) {
            g2.setPaint(new Color(0, 0, 0));
        } else {
            g2.setPaint(new Color(50, 50, 50));
        }

        String dispStr = cluster.getID() + " " + cluster.getComment();
        if (labels.size() > 0 && selectedAnnClass != null) {
            if (this.selectedAnnClass.equals(labels.get(0).className)) {
                if (labels.get(0).elementsInCluster > 0) {
                    int pct = ((100 * labels.get(0).elementsInCluster) / labels.get(0).classSize);

                    dispStr = (pct >= 5) ? (pct + "%") : "";
                }
            }
        }

        if (dispStr.length() > 0) {

            Font f = g2.getFont().deriveFont(12.0f).deriveFont(Font.BOLD);

            g2.setFont(f);

            FontRenderContext frc = g2.getFontRenderContext();
            TextLayout layout = new TextLayout(dispStr, g2.getFont(), frc);

            Rectangle2D r = layout.getBounds();
            g2.setClip(-100, -100, this.getWidth() + 100, this.getHeight() + 100);
            int strX = 1;
            if (labels.size() > 0) {
                strX = (int) ((labels.get(0).elementsInCluster * scl - r.getWidth()) / 2.0);
            }

            float[] strCoord = new float[]{strX, ((this.getHeight() + (float) r.getHeight()) / 2.0f)};

            if (getWidth() > 2) {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0));

                layout.draw(g2, -1f + strCoord[0], 1f + strCoord[1]);
                layout.draw(g2, -1f + strCoord[0], -1f + strCoord[1]);
                layout.draw(g2, 1f + strCoord[0], 1f + strCoord[1]);
                layout.draw(g2, 1f + strCoord[0], -1f + strCoord[1]);

                layout.draw(g2, 1f + strCoord[0], strCoord[1]);
                layout.draw(g2, strCoord[0], 1f + strCoord[1]);
                layout.draw(g2, -1f + strCoord[0], strCoord[1]);
                layout.draw(g2, strCoord[0], -1f + strCoord[1]);

                g2.setColor(new Color(255, 255, 255));
                layout.draw(g2, strCoord[0], strCoord[1]);
                // g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            }
            g2.setFont(f);
        }

    }
}
