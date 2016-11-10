package vortex.gui2.clusterdendrogram;

import clustering.Cluster;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map.Entry;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.border.LineBorder;
import samusik.hideabletree.*;
import javax.swing.tree.DefaultTreeModel;
import annotations.Annotation;
import clustering.ClusterSet;
import vortex.gui2.frmMain;
import util.ClusterSetValidationFactory;
import util.ClusterValidationFactory;
import util.IO;
import util.Optimization;
import util.ColorPalette;
import vortex.util.ConnectionManager;
import util.logger;

/**
 *
 * @author Kola
 */
public class ClusterDendrogramPlot extends JPanel {

    private static final long serialVersionUID = 1L;
    ArrayList<ClusterPlotRelation> relationsToDraw = new ArrayList<>();
    boolean showHidden;
    ArrayList<ClusterSet> clusterSets = new ArrayList<>();
    HashMap<Cluster, ClusterBoxElement> hmCP = new HashMap<>();
    Cluster selectedCluster = null;
    float zoomFactor = 1 / 3.0f;
    boolean TYPE_PPI_PLOT = false;
    int rowHeight = (int) ((TYPE_PPI_PLOT ? 50 : 80) * zoomFactor);
    int rowGap = 2; //(int) (0 * zoomFactor);
    ArrayList<ClusterBoxElement>[] clusterPlots = null;
    ArrayList<ClusterSet> highlight = null;
    int MIN_CLUSTER_SIZE = 10;
    Color CLUSTER_PLOT_BGCOLOR = new Color(190, 190, 190);
    Color CLUSTER_LINK_FILLCOLOR = new Color(200, 200, 200, 157);
    Color CLUSTER_BORDER_COLOR = new Color(50, 50, 50);
    //private boolean addAnnotationDistance = false;
    Annotation annontation = null;
    DefaultTreeModel model;
    private FocusListener cpFocusListener = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
            if (e.getComponent() instanceof ClusterBoxElement) {
                ClusterBoxElement c2 = (ClusterBoxElement) e.getComponent();
                ClusterDendrogramPlot.this.selectedCluster = c2.cluster;
                frmMain.getInstance().getDatasetBrowser().selectDataset(ClusterDendrogramPlot.this.selectedCluster.getClusterSet().getDataset().getName());
                frmMain.getInstance().getClusterSetsBrowser().selectClusterSet(ClusterDendrogramPlot.this.selectedCluster.getClusterSet());
                frmMain.getInstance().getClusterSetBrowser().selectClusters(new Cluster[]{ClusterDendrogramPlot.this.selectedCluster});
            }
        }
    };
    private FocusListener legendBoxFocusListener = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
            if (e.getComponent() instanceof ClusterDendrogramLegendBox) {
                ClusterDendrogramLegendBox lbl = (ClusterDendrogramLegendBox) e.getComponent();
                String selectedClassName = lbl.getCaption();
                for (Component c : ClusterDendrogramPlot.this.getComponents()) {
                    if (c instanceof ClusterBoxElement) {
                        if (((ClusterBoxElement) c).setSelectedAnnClass(selectedClassName)) {
                            setComponentZOrder(c, 1);
                        } else {
                            setComponentZOrder(c, 2);
                        }
                    }
                }
                ClusterDendrogramPlot.this.repaint();
            }
        }
    };

    public void setClusterValidation(ClusterValidationFactory f) {
        Component[] c = this.getComponents();
        for (Component cmp : c) {
            if (cmp instanceof ClusterBoxElement) {
                ClusterBoxElement cp = ((ClusterBoxElement) cmp);
                cp.setClusterValidation(f.getClusterValidationMeasure(cp.cluster), true);
            }
        }
    }

    private void clearRelations() {
        relationsToDraw = new ArrayList<ClusterPlotRelation>();
    }

    public void highlightCluster(Cluster c) {
        for (Component f : getComponents()) {
            if (f instanceof ClusterBoxElement) {
                if (((ClusterBoxElement) f).cluster.equals(c)) {
                    if (getParent() instanceof JViewport) {
                        if (!((JViewport) getParent()).contains(f.getBounds().x, f.getBounds().y)) {
                            ((JViewport) getParent()).setViewPosition(new java.awt.Point(Math.max(0, Math.min(this.getWidth() - ((JViewport) getParent()).getWidth(), f.getBounds().x - ((JViewport) getParent()).getWidth() / 2)),
                                    Math.max(0, Math.min(this.getHeight() - ((JViewport) getParent()).getHeight(), f.getBounds().y - ((JViewport) getParent()).getWidth() / 2))));
                        }
                    }
                    f.requestFocus();
                }
            }
        }
    }

    public static JPopupMenu getPopupCluster() {
        return popupCluster;
    }

    public Annotation getAnnontation() {
        return annontation;
    }

    public void highlightClusterSet(ArrayList<ClusterSet> cs) {
        highlight = cs;
        repaint();
    }

    private int getGBWrecursive(HideableMutableTreeNode<ClusterBoxElement> node) {
        if (node.isLeaf()) {
            if (node.getUserObject().getGridBagConstraints() == null) {
                node.getUserObject().setGridBagConstraints(new GridBagConstraints());
                node.getUserObject().getGridBagConstraints().gridwidth = 1;
            }
            return node.getUserObject().getGridBagConstraints().gridwidth;
        }
        Enumeration<HideableMutableTreeNode<ClusterBoxElement>> enu = node.children();
        int w = 0;
        while (enu.hasMoreElements()) {
            w += getGBWrecursive(enu.nextElement());
        }
        if (node.getUserObject().getGridBagConstraints() == null) {
            node.getUserObject().setGridBagConstraints(new GridBagConstraints());
        }
        node.getUserObject().getGridBagConstraints().gridwidth = w;
        return w;
    }

    public void setAnnotation(Annotation ann) throws SQLException {
        int maxGBwid = 0;
        if(ann == null) return;

        for (Component c : getComponents()) {
            if (c instanceof ClusterBoxElement) {
                ((ClusterBoxElement) c).setAnnotation(util.ColorPalette.NEUTRAL_PALETTE, ann);
                ((ClusterBoxElement) c).setBackground(CLUSTER_PLOT_BGCOLOR);
                maxGBwid = Math.max(((ClusterBoxElement) c).getGridBagConstraints().gridx + ((ClusterBoxElement) c).getGridBagConstraints().gridwidth, maxGBwid);
            }
            if (c instanceof VerticalBarPlotCell) {
                this.remove(c);
            }
        }


        //ClusterSetValidationFactory silF = TYPE_PPI_PLOT ? null : new ClusterSetValidationMeasureCollection.ClusterSilhouetteIndexFactory(new DistanceMatrix(this.clusterSets.get(0).getDataset(), this.clusterSets.get(0).getDistanceMeasure()));
        //ClusterSetValidationFactory ppiF1= TYPE_PPI_PLOT?new ClusterSetValidationMeasureCollection.PPIEnrichmentFactory(this.clusterSets.get(0).getDataset(), 0.7, 0.0):null; 
        //ClusterSetValidationFactory ppiF2= TYPE_PPI_PLOT?new ClusterSetValidationMeasureCollection.PPIEnrichmentFactory(this.clusterSets.get(0).getDataset(), 0.7, 0.90):null; 
        // 
/*
         for (int j = 0; j < clusterSets.size(); j++) {
         ClusterSet cs = clusterSets.get(j);
         ClusterValidation.ValidationInfo[] viClass = ClusterValidation.getStatisticsPerClass(0.5, ann, cs);

         double pctUnclustered = 0;
         int numClusters = 0;
         for (Cluster c : cs.getClusters()) {
         if (c.getSize() < MIN_CLUSTER_SIZE) {
         pctUnclustered += c.getSize();
         } else {
         numClusters++;
         }
         }

         pctUnclustered /= cs.getDataset().getSize();

         for (ClusterValidation.ValidationInfo vi : viClass) {
         if (vi.bestMatchCluster != null) {
         if (hmCP.get(vi.bestMatchCluster) != null) {
         double val = vi.recall;
         String s = (val == 1.0) ? ("1.0") : String.valueOf(val).substring(1, Math.min(String.valueOf(val).length(), 4));
         hmCP.get(vi.bestMatchCluster).setCaption(s);
         }
         }
         }

         ClusterValidation.ValidationInfo[] vin = ClusterValidation.getStatisticsPerClass(0.5, ann, cs);
         double overallRecall = 0;
         double classifiedSize = 0;
         double TPR = 0;
         double minClassSize = Double.MAX_VALUE;

         for (int i = 0; i < vin.length; i++) {
         minClassSize = Math.min(minClassSize, ann.get(i).getValue().size());
         if (vin[i].bestMatchCluster != null) {
         TPR++;
         overallRecall += vin[i].recall * 1;//ann.get(i).getValue().size();
         }
         classifiedSize++; //= ann.get(i).getValue().size();
         }
         double FPR = 0;
         int count_mock = 1;
         for (Cluster c : cs.getClusters()) {
         if (c.getSize() > minClassSize / 2) {
         FPR++;
         }
         if (c.toString().contains("MOCK")) {
         count_mock++;
         }
         }

         overallRecall /= classifiedSize;
         VerticalBarPlotCell[] fm = new VerticalBarPlotCell[TYPE_PPI_PLOT ? 2 : 4];

         if (!TYPE_PPI_PLOT) {
         //  ClusterSetValidationMeasure sil = silF.getClusterSetValidationMeasure(cs);

         fm[0] = new VerticalBarPlotCell(String.valueOf(sil.getMeasure()).substring(0, Math.min(4, String.valueOf(sil.getMeasure()).length())), sil.getMeasure(), new double[]{0, 0.35});
         fm[1] = new VerticalBarPlotCell(String.valueOf((int) TPR), TPR, new double[]{0, ann.size()});
         fm[2] = new VerticalBarPlotCell(String.valueOf(overallRecall).substring(0, Math.min(4, String.valueOf(overallRecall).length())), overallRecall, new double[]{0, 1});
         fm[3] = new VerticalBarPlotCell(String.valueOf(pctUnclustered).substring(0, Math.min(4, String.valueOf(pctUnclustered).length())), pctUnclustered, new double[]{0, 1.0});

         fm[0].setForeground(new Color(245, 178, 182));
         fm[2].setForeground(new Color(59, 206, 250));
         fm[1].setForeground(new Color(205, 235, 170));
         fm[3].setForeground(new Color(100, 235, 205));
         System.out.print(cs.getClusteringAlgorithm() + ";" + cs.getMainClusteringParameterValue() + "; ");

         System.out.print(numClusters);
         for (int i = 0; i < fm.length; i++) {
         System.out.print("; " + fm[i].getValue());
         GridBagConstraints gbc = new GridBagConstraints();
         gbc.gridx = 2 + i;
         gbc.gridwidth = 1;
         gbc.gridheight = 1;
         gbc.gridy = j + 1;
         if (i == fm.length - 1) {
         gbc.insets.right = 5;
         }
         //if (i==0) gbc.insets.left = 20;
         fm[i].setPreferredSize(new Dimension((int) (160 * zoomFactor), rowGap + rowHeight));
         fm[i].setMaximumSize(getPreferredSize());
         fm[i].setMinimumSize(getPreferredSize());
         fm[i].setFontColor(new Color(0, 0, 50));
         fm[i].setFont(this.getFont().deriveFont((rowHeight + rowGap) * 0.90f));
         fm[i].setBackground(new Color(70, 70, 100));
         this.add(fm[i], gbc);
         }
         System.out.print("\n");
         }
         }*/

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridheight = 1;
        gbc.gridx = clusterSets.size() + 1;
        gbc.gridx = 0;
        gbc.gridwidth = maxGBwid;
        gbc.weightx = 0.0;
        //gbc.ipadx = 1185;
        //gbc.ipady = 20;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets.top = 10;
        gbc.anchor = GridBagConstraints.WEST;
        JPanel strut = new JPanel() {
        };


        strut.setMinimumSize(new Dimension(1000, rowHeight));
        strut.setMaximumSize(new Dimension(1000, rowHeight));
        strut.setPreferredSize(new Dimension(1000, rowHeight));
        strut.setBorder(new LineBorder(Color.BLACK));
        strut.setLayout(new BoxLayout(strut, BoxLayout.X_AXIS));
        for (int i = 0; i < ann.getTerms().length; i++) {
            ClusterDendrogramLegendBox lb = new ClusterDendrogramLegendBox(ann.getTerms()[i], ColorPalette.NEUTRAL_PALETTE.getColor(i));
            lb.setMinimumSize(new Dimension(150, rowHeight));
            lb.setPreferredSize(new Dimension(150, rowHeight));
            lb.setMaximumSize(new Dimension(150, rowHeight));
            lb.setFocusable(true);
            lb.addFocusListener(legendBoxFocusListener);
            strut.add(lb);

        }

        this.add(strut, gbc);
        strut.doLayout();


        int maxX = 0;
        int maxY = 0;
        for (Component c : getComponents()) {
            maxX += Math.max(c.getBounds().x + c.getBounds().width, maxX);
            maxY += Math.max(c.getBounds().y + c.getBounds().height, maxY);
        }
        if (this.getSize().width != maxX || this.getSize().height != maxY) {
            this.setSize(maxX, maxY);
        }

        annontation = ann;

        this.invalidate();
        this.validate();
        this.doLayout();
        this.repaint();

    }

    @Override
    public void doLayout() {
        super.doLayout();
    }

    public void addClusterSetValidationMeasure(ClusterSetValidationFactory factory) {
    }

    public void setClusterSets(ClusterSet[] css) throws SQLException {
        this.setLayout(new GridBagLayout());
        this.clusterSets = new ArrayList(Arrays.asList(css));
        this.removeAll();
        if (css.length == 0) {
            return;
        }
        this.showHidden = true;

        clusterPlots = new ArrayList[css.length + 1];

        for (int i = 0; i < clusterPlots.length; i++) {
            clusterPlots[i] = new ArrayList<>();
        }

        //building up cluster tree

        MIN_CLUSTER_SIZE = Math.min(50, css[0].getDataset().size() / 300);

        HideableMutableTreeNode<ClusterBoxElement> root = new HideableMutableTreeNode<>();

        for (Cluster c : clusterSets.get(0).getClusters()) {
            if (c.size() >= MIN_CLUSTER_SIZE) {
                ClusterBoxElement cp = new ClusterBoxElement(c, 1);
                cp.setBorder(new LineBorder(CLUSTER_BORDER_COLOR));
                String s = c.toString();
                s = s.split(":")[0];
                root.add(cp.getNode());
                hmCP.put(c, cp);
            }
        }

        model = new HideableTreeModel(root);

        for (int i = 1; i < css.length; i++) {
            ClusterSet prev = clusterSets.get(i - 1);
            ClusterSet next = clusterSets.get(i);
            for (final Cluster c : next.getClusters()) {
                if (c.size() < MIN_CLUSTER_SIZE) {
                    continue;
                }
                Entry<Cluster, Double> e = new Optimization<Cluster>() {
                    @Override
                    public double scoringFunction(Cluster arg) {
                        //double match = 0;
                        if (arg.size() < MIN_CLUSTER_SIZE) {
                            return -1;
                        }
                        return c.getOverlap(arg);
                    }
                }.getArgMax(prev.getClusters());
                ClusterBoxElement cp = new ClusterBoxElement(c, e.getValue());

                try {
                    hmCP.get(e.getKey()).getNode().add(cp.getNode());
                } catch (NullPointerException ex) {
                    logger.showException(ex);
                }
                hmCP.put(c, cp);
            }
        }

        for (ClusterBoxElement cp : hmCP.values()) {
            if (cp.getNode().isLeaf()) {
                cp.setGridBagConstraints(new GridBagConstraints());
                cp.getGridBagConstraints().gridwidth = 1;
            }
        }

        @SuppressWarnings("unchecked")
        Enumeration<HideableMutableTreeNode<ClusterBoxElement>> ch = root.children();

        while (ch.hasMoreElements()) {
            getGBWrecursive(ch.nextElement());
        }

        @SuppressWarnings("unchecked")
        Enumeration<HideableMutableTreeNode<ClusterBoxElement>> enu = root.breadthFirstEnumeration();
        enu.nextElement();
        HideableMutableTreeNode<ClusterBoxElement> parent = root;
        int xpos = 7;
        while (enu.hasMoreElements()) {
            HideableMutableTreeNode<ClusterBoxElement> node = enu.nextElement();
            if (!node.getParent().equals(parent)) {
                parent = (HideableMutableTreeNode) node.getParent();
                xpos = parent.getUserObject().getGridBagConstraints().gridx;
            }
            ClusterBoxElement cp = node.getUserObject();
            if (parent.getIndex(node) == 0) {
                if (parent.getUserObject() instanceof ClusterBoxElement) {

                    //transferring colorCode to children
                    if (cp.cluster.getColorCode().equals(Color.white)) {
                        cp.setBackground(parent.getUserObject().getBackground());
                    } else {
                        //transferring colorCode to predecessors
                        Enumeration<HideableMutableTreeNode<ClusterBoxElement>> enuToRoot = node.pathFromAncestorEnumeration(root);
                        while (enuToRoot.hasMoreElements()) {
                            HideableMutableTreeNode<ClusterBoxElement> pred = enuToRoot.nextElement();
                            if (pred.getParent() != null) {
                                HideableMutableTreeNode<ClusterBoxElement> predParent = (HideableMutableTreeNode<ClusterBoxElement>) pred.getParent();
                                if (predParent.getUserObject() != null) {
                                    if (predParent.getUserObject().isColorCodeGenerated()) {
                                        if (predParent.getIndex(pred) == 0) {
                                            predParent.getUserObject().setColorCodeGenerated(false);
                                            predParent.getUserObject().setBackground(cp.cluster.getColorCode());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            cp.getGridBagConstraints().gridx = xpos;
            cp.getGridBagConstraints().gridy = node.getLevel();
            clusterPlots[cp.getGridBagConstraints().gridy].add(cp);
            xpos += cp.getGridBagConstraints().gridwidth;
            cp.setPreferredSize(new Dimension((int) (Math.sqrt(cp.cluster.size()) * zoomFactor), rowHeight));
            cp.getGridBagConstraints().insets = new Insets((int) Math.ceil(rowGap / 2.0), 2, (int) Math.floor(rowGap / 2.0), 0);
            cp.getGridBagConstraints().anchor = GridBagConstraints.CENTER;
           
            cp.setFont(new Font(Font.MONOSPACED, Font.PLAIN, (int)(rowHeight * 0.9)));
            cp.setMinimumSize(cp.getPreferredSize());
            cp.setMaximumSize(cp.getPreferredSize());
            cp.addFocusListener(cpFocusListener);
            this.add(cp, cp.getGridBagConstraints());
        }

        for (int i = 0; i < css.length; i++) {
            ClusterSet clusterSet = css[i];
            int size = 0;

            for (Cluster c : clusterSet.getClusters()) {
                if (c.size() >= MIN_CLUSTER_SIZE) {
                    size++;
                }
            }

            String s = String.valueOf(Math.round(clusterSet.getMainClusteringParameterValue() * 100.0) / 100.0);

            if (clusterSet.getMainClusteringParameterValue() > 2 && s.contains(".")) {
                s = s.split("\\.")[0];
            }

            JLabel lbl = new JLabel(s, JLabel.CENTER) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    ((Graphics2D) g).setStroke(new BasicStroke(1));
                    g.setColor(Color.GRAY);
                    g.drawRect(0, 0, getWidth(), getHeight());
                }
            };

            lbl.setFont(lbl.getFont().deriveFont(rowHeight * 1.1f));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = 1;
            gbc.gridheight = 1;
            gbc.gridx = 0;
            gbc.gridy = 1 + i;
            lbl.setMinimumSize(new Dimension((int) (200 * zoomFactor), rowGap + rowHeight));
            lbl.setMaximumSize(lbl.getMinimumSize());
            lbl.setPreferredSize(lbl.getMinimumSize());
            this.add(lbl, gbc);

            lbl = new JLabel(String.valueOf(size), JLabel.CENTER) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.setColor(Color.GRAY);
                    ((Graphics2D) g).setStroke(new BasicStroke(1));
                    g.drawRect(0, 0, getWidth(), getHeight());
                }
            };
            lbl.setFont(lbl.getFont().deriveFont(rowHeight * 1.1f));

            gbc = new GridBagConstraints();
            gbc.gridwidth = 1;
            gbc.gridheight = 1;
            gbc.gridx = 1;
            gbc.gridy = 1 + i;
            lbl.setMinimumSize(new Dimension((int) (200 * zoomFactor), rowGap + rowHeight));
            lbl.setMaximumSize(lbl.getMinimumSize());
            lbl.setPreferredSize(lbl.getMinimumSize());
            this.add(lbl, gbc);
        }
        doLayout();
    }

    public void setXScale(double scale) {
        scale *= zoomFactor;
        for (Component c : this.getComponents()) {
            if (c instanceof ClusterBoxElement) {
                c.setPreferredSize(new Dimension((int) Math.max((((ClusterBoxElement) c).cluster.size()) * scale, 3), rowHeight));
            }
        }
        doLayout();
        repaint();
    }

    public ClusterDendrogramPlot() {
        initComponents();

    }

    public void focusGained(FocusEvent evt) {
        if (evt.getOppositeComponent() instanceof ClusterBoxElement) {
            clearRelations();
        }
        this.repaint();
    }

    public void focusLost(FocusEvent evt) {
        clearRelations();
        ((Component) evt.getSource()).repaint();
    }

    public void getShowHidden(boolean showHidden) {
        this.showHidden = showHidden;
    }

    public ClusterSet[] getClusterSets() {
        return clusterSets.toArray(new ClusterSet[clusterSets.size()]);
    }

    public ClusterDendrogramPlot(ClusterSet[] css) throws SQLException {
        initComponents();
        initComponents();
        setClusterSets(css);
    }

    public Cluster getSelectedCluster() {
        return selectedCluster;
    }
    /*
     @Override
     public Component getComponent(int n) {
     Component[] comp = this.getComponents();

     Arrays.sort(comp, new Comparator<Component>() {
     @Override
     public int compare(Component o1, Component o2) {
     int zOrder1 = 0;
     int zOrder2 = 0;
     if (zOrdered.class.isAssignableFrom(o1.getClass())) {
     zOrder1 = ((zOrdered) o1).getZOrder();
     }
     if (zOrdered.class.isAssignableFrom(o2.getClass())) {
     zOrder2 = ((zOrdered) o2).getZOrder();
     }
     return zOrder1 - zOrder2;
     }
     });
     return comp[n];
     }*/

    @Override
    public void paintComponent(Graphics g) {
        if (this.clusterSets.isEmpty()) {
            super.paintComponent(g);
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);

        ((Graphics2D) g).setPaint(new Color(255, 255, 255));
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        ((Graphics2D) g).setPaint(new Color(0, 0, 0));

        ArrayList<ClusterBoxElement> alCP = new ArrayList<ClusterBoxElement>();

        for (ArrayList<ClusterBoxElement> al : clusterPlots) {
            alCP.addAll(al);
        }





        int minY = Integer.MAX_VALUE;

        for (ClusterBoxElement cp : alCP) {
            minY = Math.min(cp.getY(), minY);
        }

        if (highlight != null) {
            for (ClusterSet cs : highlight) {
                int highlightClusterSet = clusterSets.indexOf(cs);
                if (highlightClusterSet >= 0) {
                    g2.setPaint(new Color(255, 255, 0, 127));
                    g2.fillRect(0, minY + highlightClusterSet * (rowGap + rowHeight), this.getWidth(), rowHeight);
                }
            }
        }

        g2.setStroke(new BasicStroke(6.0f * zoomFactor));
        g2.setPaint(Color.BLACK);

        for (ClusterBoxElement cp : alCP) {

            if (cp.getNode().isLeaf()) {
                continue;
            }
            Enumeration<HideableMutableTreeNode<ClusterBoxElement>> enu = cp.getNode().children();

            ArrayList<ClusterBoxElement> alChildren = new ArrayList<>();

            while (enu.hasMoreElements()) {
                alChildren.add(enu.nextElement().getUserObject());
            }

            Collections.sort(alChildren, new Comparator<ClusterBoxElement>() {
                @Override
                public int compare(ClusterBoxElement o1, ClusterBoxElement o2) {
                    return o1.getX() - o2.getX();
                }
            });

            double sumSize = 0;
            for (ClusterBoxElement cbe : alChildren) {
                sumSize += cbe.getWidth();
            }

            double scaleFactor = cp.getWidth() / sumSize;

            double upperLeftCornerX = cp.getX();

            for (ClusterBoxElement cbe : alChildren) {
                GeneralPath gp = new GeneralPath();
                Rectangle2D p = cp.getBounds();
                Rectangle2D c = cbe.getBounds();
                gp.moveTo(upperLeftCornerX, p.getY() + p.getHeight());
                gp.lineTo(upperLeftCornerX + c.getWidth() * scaleFactor, p.getY() + p.getHeight());
                gp.lineTo(c.getX() + c.getWidth(), c.getY());
                gp.lineTo(c.getX(), c.getY());
                gp.closePath();
                g2.setStroke(new BasicStroke(1.0f));
                g2.setPaint(CLUSTER_LINK_FILLCOLOR);
                g2.fill(gp);
                g2.setPaint(CLUSTER_BORDER_COLOR);
                g2.drawLine((int) upperLeftCornerX, (int) (p.getY() + p.getHeight()), (int) c.getX(), (int) c.getY());
                g2.drawLine((int) (upperLeftCornerX + c.getWidth() * scaleFactor) + 1, (int) (p.getY() + p.getHeight()), (int) (c.getX() + c.getWidth()) + 1, (int) c.getY());
                upperLeftCornerX += c.getWidth() * scaleFactor;
            }

        }
        /*
         for (java.awt.Component c : comp) {
         g2.translate(c.getBounds().x, c.getBounds().y);
         //Graphics gn = g2.create(c.getBounds().x, c.getBounds().y, c.getBounds().width + 1, c.getBounds().height + 1);
         c.paint(g);
         //c.paintAll(gn);
         g2.translate(-c.getBounds().x, -c.getBounds().y);
         }*/

        Stroke tmp = g2.getStroke();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int i = 0; i < relationsToDraw.size(); i++) {
            g2.setStroke(tmp);
            g2.setPaint(new Color(255, 255, 255));
            g2.draw(new Line2D.Float(relationsToDraw.get(i).start, relationsToDraw.get(i).end));
            g2.setPaint(new Color(155, 50, 0));
            g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{3.0f, 3.0f}, 0.0f));
            g2.draw(new Line2D.Float(relationsToDraw.get(i).start, relationsToDraw.get(i).end));
        }

        for (int i = 0; i < relationsToDraw.size(); i++) {
            g2.setPaint(new Color(255, 255, 255, 210));
            g2.fill(new Rectangle2D.Float((float) relationsToDraw.get(i).end.getX() - 15.0f, (float) relationsToDraw.get(i).end.getY() - 5.0f, 30.0f, 10.0f));
            g2.setPaint(new Color(0, 0, 0));
            g2.drawString(relationsToDraw.get(i).label, (float) relationsToDraw.get(i).end.getX() - 15.0f, (float) relationsToDraw.get(i).end.getY() + 4.0f);
        }
        g2.setStroke(tmp);
        g2.setPaintMode();
        paintChildren(g);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        popupCluster = new javax.swing.JPopupMenu();
        pmiFixColorCode = new javax.swing.JMenuItem();
        pmiShowRelations = new javax.swing.JMenuItem();
        pmiSaveImage = new javax.swing.JMenuItem();
        pmiComputeAnnotationDistance = new javax.swing.JMenuItem();

        pmiFixColorCode.setText("Fix Proposed Color Code and Transfer Comment");
        pmiFixColorCode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiFixColorCodeActionPerformed(evt);
            }
        });
        popupCluster.add(pmiFixColorCode);

        pmiShowRelations.setText("Show Relations");
        pmiShowRelations.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiShowRelationsActionPerformed(evt);
            }
        });
        popupCluster.add(pmiShowRelations);

        pmiSaveImage.setText("Save image...");
        pmiSaveImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiSaveImageActionPerformed(evt);
            }
        });
        popupCluster.add(pmiSaveImage);

        pmiComputeAnnotationDistance.setText("Compute Annotation Distance P-value");
        pmiComputeAnnotationDistance.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiComputeAnnotationDistanceActionPerformed(evt);
            }
        });
        popupCluster.add(pmiComputeAnnotationDistance);

        setBackground(new java.awt.Color(255, 255, 255));
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                formMouseClicked(evt);
            }
        });
        setLayout(new java.awt.GridBagLayout());
    }// </editor-fold>//GEN-END:initComponents

    public void highlightBestMatches(ClusterSet cs) {
    }

    private void formMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseClicked
        relationsToDraw.clear();
        repaint();
        if (evt.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu menu = new JPopupMenu("");
            menu.add(pmiSaveImage);
            menu.show(this, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_formMouseClicked

private void pmiFixColorCodeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiFixColorCodeActionPerformed
    Component c = ((JPopupMenu) ((JMenuItem) evt.getSource()).getParent()).getInvoker();
    if (c instanceof ClusterBoxElement) {
        ClusterBoxElement c1 = (ClusterBoxElement) c;
       for (Component f : this.getComponents()) {
            if (f instanceof ClusterBoxElement) {
                ClusterBoxElement c2 = (ClusterBoxElement) f;
                if ((c1.getGridBagConstraints().gridy - c2.getGridBagConstraints().gridy) == 1) {
                    if (c1.getGridBagConstraints().gridx >= c2.getGridBagConstraints().gridx && c1.getGridBagConstraints().gridx < c2.getGridBagConstraints().gridx+c2.getGridBagConstraints().gridwidth){
                        c1.cluster.setComment(c2.cluster.getComment());
                        try{
                        ConnectionManager.getStorageEngine().saveCluster(c1.cluster, false);
                        }catch(SQLException e){
                            logger.showException(e);
                        }
                    }
                }
            }
       }
    }

}//GEN-LAST:event_pmiFixColorCodeActionPerformed

private void pmiShowRelationsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiShowRelationsActionPerformed
    Component c = ((JPopupMenu) ((JMenuItem) evt.getSource()).getParent()).getInvoker();
    if (c instanceof ClusterBoxElement) {
        ClusterBoxElement c1 = (ClusterBoxElement) c;
        relationsToDraw = new ArrayList<>();
        for (Component f : this.getComponents()) {
            if (f instanceof ClusterBoxElement) {
                ClusterBoxElement c2 = (ClusterBoxElement) f;
                if (Math.abs(c2.getGridBagConstraints().gridy - c1.getGridBagConstraints().gridy) == 1) {
                    int share = 0;
                    c1.cluster.getOverlap(c2.cluster);
                    if (share > 0) {
                        int ov = (c1.cluster.getOverlap(c2.cluster) * 100) / c1.cluster.size();
                        String pValStr = ov + "%";
                        //pValStr = pValStr.substring(0, Math.min(pValStr.length(), 4));
                        relationsToDraw.add(new ClusterPlotRelation(new Point2D.Float((c1.getBounds().x + c1.getBounds().width / 2.0f), (c1.getBounds().y + c1.getBounds().height / 2.0f)), new Point2D.Float((c2.getBounds().x + c2.getBounds().width / 2.0f), (c2.getBounds().y + c2.getBounds().height / 2.0f)), pValStr));
                        this.repaint();
                    }
                }
            }
        }

    }
}//GEN-LAST:event_pmiShowRelationsActionPerformed

private void pmiSaveImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiSaveImageActionPerformed

    File f = IO.chooseFileWithDialog("ClusterHierarchyPlot.SaveImage", "Portable Network Graphics (*.png)", new String[]{"png"}, true);
    if (f != null) {
        BufferedImage bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = bi.createGraphics();
        this.paint(g);
        bi.flush();
        try {
            ImageIO.write(bi, "PNG", f);
        } catch (IOException e) {
            logger.showException(e);
        }
    }

}//GEN-LAST:event_pmiSaveImageActionPerformed

private void pmiComputeAnnotationDistanceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiComputeAnnotationDistanceActionPerformed
//    addAnnotationDistance = true;
}//GEN-LAST:event_pmiComputeAnnotationDistanceActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem pmiComputeAnnotationDistance;
    private javax.swing.JMenuItem pmiFixColorCode;
    private javax.swing.JMenuItem pmiSaveImage;
    private javax.swing.JMenuItem pmiShowRelations;
    private static javax.swing.JPopupMenu popupCluster;
    // End of variables declaration//GEN-END:variables
}
