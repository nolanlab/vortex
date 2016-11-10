/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustergraph;

import util.ScaleImageGenerator;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeIterable;
import org.gephi.graph.api.UndirectedGraph;
import fdl_multithreaded.ForceAtlas2;
import fdl_multithreaded.ForceAtlas2Builder;
import javax.swing.SwingWorker;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.api.ProcessingTarget;
import org.gephi.preview.plugin.renderers.NodeLabelRenderer;
import org.gephi.preview.plugin.renderers.NodeRenderer;
import org.gephi.project.api.ProjectController;
import org.openide.util.Lookup;
import annotations.Annotation;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import clustering.Cluster;
import clustering.ClusterMember;
import clustering.Datapoint;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import clustering.Dataset;
import clustering.DistanceMeasure;
import java.awt.BasicStroke;
import java.awt.Font;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.preview.api.Item;
import org.gephi.preview.api.PreviewProperties;
import org.gephi.preview.plugin.items.NodeItem;
import processing.core.PApplet;
import processing.core.PGraphics;
import samusik.glasscmp.GlassFrame;
import util.DefaultEntry;
import util.ColorPalette;
import util.IO;
import util.MatrixOp;
import util.Shuffle;
import util.logger;
import vortex.gui2.ClusterSetBrowser;
import util.QuantileMap;

/**
 *
 * @author Nikolay
 */
public class frmScFDL extends GlassFrame implements ClusterSetBrowser.ClusterSelectionListener {

    private final GraphRenderer graphRenderer = new GraphRenderer();
    ArrayList<Edge> edges = new ArrayList<>();
    private double[][] scalingBounds;
    private UndirectedGraph graph;
    private String[] paramList;
    private HashMap<Integer, ClusterNode> cid = new HashMap<>();
    private HashMap<ClusterNode, double[]> expVectors = new HashMap<>();
    private Dataset ds;
    private Node[] nodes;
    String currParam = "";
    int curParamIDX = -1;
    String[] currAnnTerms = null;
    Annotation currAnn;
    private int fixedParamVal = -1;
    private final Map.Entry<String, Color>[] groupOptionList = new Map.Entry[]{
        new DefaultEntry<>("Assay", new Color(120, 194, 235)),
        new DefaultEntry<>("None", Color.GRAY)};
    private double COLOR_MAP_NOISE_THRESHOLD = 0.19;

    public double getCOLOR_MAP_NOISE_THRESHOLD() {
        return COLOR_MAP_NOISE_THRESHOLD;
    }

    public void setCOLOR_MAP_NOISE_THRESHOLD(double COLOR_MAP_NOISE_THRESHOLD) {
        this.COLOR_MAP_NOISE_THRESHOLD = COLOR_MAP_NOISE_THRESHOLD;
    }

    private Node[][] sortedNodes;

    PApplet applet;

    QuantileMap qm, sqm;
    private BufferedImage scaleImg;

    @Override
    public void clusterSelected(ClusterSetBrowser.ClusterSelectionEvent evt) throws SQLException {
        if (graph == null) {
            return;
        }
        NodeIterable ni = graph.getNodes();
        if (evt.getSource() == null) {
            for (final Node n : ni.toArray()) {
                n.getNodeData().setSize(10f);
            }
        } else {
            List<Cluster> lst = Arrays.asList(evt.getSource());
            for (final Node n : ni.toArray()) {
                ClusterNode cl = cid.get((int) n.getAttributes().getValue("clusterNode"));
                if (lst.contains(cl.cluster)) {
                    n.getNodeData().setSize(10f);
                } else {
                    n.getNodeData().setSize(5f);
                }
            }
        }
        lstParamValueChanged(new ListSelectionEvent(lstParam, lstParam.getSelectedIndex(), lstParam.getSelectedIndex(), false));
    }

    private void paintScale(Graphics g) {
        if (scaleImg != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setPaint(graphRenderer.getBackground());
            g2.fillRect(0, 0, this.getWidth(), this.getHeight());
            g2.drawImage(scaleImg, null, 0, -10);
        }
    }

    private double getScaledValue(int paramIDX, double[] vec) {
        double scaledVal = 0;
        if (qm == null) {
            qm = QuantileMap.getQuantileMap(ds);
            sqm = QuantileMap.getQuantileMapForSideParam(ds);
        }

        if (paramIDX < ds.getFeatureNamesCombined().length) {

            double noiseQuantile = (paramIDX >= ds.getDimension()) ? sqm.getQuantileForValue(paramIDX - ds.getDimension(), COLOR_MAP_NOISE_THRESHOLD) : qm.getQuantileForValue(paramIDX, COLOR_MAP_NOISE_THRESHOLD);

            scaledVal = (paramIDX >= ds.getDimension()) ? (float) sqm.getQuantileForValue(paramIDX - ds.getDimension(), vec[paramIDX]) : (float) qm.getQuantileForValue(paramIDX, vec[paramIDX]);//(float) ((vec[paramIDX] - scalingBounds[paramIDX][0]) / (scalingBounds[paramIDX][1] - scalingBounds[paramIDX][0]));
            scaledVal = (scaledVal - noiseQuantile) / (1.0 - noiseQuantile);
            currParam = paramList[paramIDX];
            scaleImg = ScaleImageGenerator.generateScaleImage(100, paramIDX, ds, COLOR_MAP_NOISE_THRESHOLD, qm, sqm);
        } else {
            double maxSize = 0;
            for (double[] d : expVectors.values()) {
                maxSize = Math.max(d[paramIDX], maxSize);
            }
            scaledVal = vec[paramIDX] / maxSize;
            currParam = "Number of Cells";
            scaleImg = ScaleImageGenerator.generateScaleImage(100, 0, maxSize);
        }
        return Math.min(1.0, Math.max(0, scaledVal));
    }

    private void colorNodesByExpressionLevel(int paramIDX) {
        if (paramIDX < 0) {
            return;
        }

        NodeIterable ni = graph.getNodes();
        for (final Node n : ni.toArray()) {
            if (n.getAttributes().getValue("cluster") == null) {
                continue;
            }
            ClusterNode cl = cid.get((int) n.getAttributes().getValue("clusterNode"));
            if (expVectors.get(cl) == null) {
                expVectors.put(cl, MatrixOp.concat(cl.dp.getVector(), cl.dp.getSideVector()));
            }
            double[] vec = expVectors.get(cl);

            double scaledVal = getScaledValue(paramIDX, vec);
            try {
                if (n.getNodeData().getSize() > 5.01f) {
                    Color c = (fixedParamVal >= 0) ? new Color((int) (getScaledValue(fixedParamVal, vec) * 255), (int) (scaledVal * 255), 0) : getColorForValue(scaledVal);
                    n.getNodeData().setR(c.getRed() / 255f);
                    n.getNodeData().setG(c.getGreen() / 255f);
                    n.getNodeData().setB(c.getBlue() / 255f);
                } else {
                    n.getNodeData().setColor(0.8f, 0.8f, 0.8f);
                }
            } catch (Exception e) {
                logger.showException(e);
                logger.print((int) (getScaledValue(fixedParamVal, vec) * 255));
                logger.print((int) (scaledVal * 255));
            }
        }
        curParamIDX = paramIDX;
        currParam = paramList[paramIDX];
        panScale.repaint();
    }

    private void adjustSizesByAnnotationTerm(Annotation ann, String[] termsAssay) {
        if (termsAssay == null) {
            return;
        }

        String[] terms = ann.getTerms();
        NodeIterable ni = graph.getNodes();
        final Node[] nds = ni.toArray();
        for (Node n : nds) {
            n.getNodeData().setSize(5f);
            n.getAttributes().setValue("colorID", -1);
        }
        Arrays.sort(terms);
        String termAssignment[] = new String[ann.getBaseDataset().size()];

        for (String term : termsAssay) {
            int[] pid = ann.getDpIDsForTerm(term);
            // int[] dpids = new int[pid.length];
            double[] dpIDsAssay = new double[pid.length];
            for (int i = 0; i < pid.length; i++) {
                Datapoint dp = ann.getBaseDataset().getDPByID(pid[i]);
                termAssignment[dp.getID()] = term;
            }
        }

        for (Node n : nds) {
            ClusterNode cl = cid.get((int) n.getAttributes().getValue("clusterNode"));
            if (termAssignment[cl.dp.getID()] != null) {
                n.getNodeData().setSize(10f);
                int idx = Arrays.binarySearch(terms, termAssignment[cl.dp.getID()]);
                n.getAttributes().setValue("colorID", idx);
            }
        }

        lstParamValueChanged(new ListSelectionEvent(lstParam, lstParam.getSelectedIndex(), lstParam.getSelectedIndex(), false));
        currAnnTerms = termsAssay;
        panGraphContainer.repaint();
    }

    private Color getColorForValue(double val) {
        if (Double.isNaN(val)) {
            return Color.GRAY;
        }
        if (val < 0) {
            val = 0;
        }
        if (val > 1) {
            val = 1;
        }
        return new Color(Color.HSBtoRGB(0.60f - (float) val * 0.60f, 1, 0.85f));
    }

    class ClusterNode {

        public ClusterNode(Cluster cluster, Datapoint dp, double density) {
            this.cluster = cluster;
            this.dp = dp;
            this.density = density;
        }
        Cluster cluster;
        Datapoint dp;
        double density;
    }

    /**
     * Creates new form frmClusterGraphX
     */
    private JPanel panScale;

    int MinClusID;
    final ColorPalette pal = ColorPalette.NEUTRAL_PALETTE;

    private ActionListener actl = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            Object i = cmbAnnotations.getSelectedItem();
            if (i instanceof Annotation) {
                currAnn = (Annotation) i;
                String[] terms = currAnn.getTerms();
                Arrays.sort(terms);
                final DefaultTableModel tabModel = new DefaultTableModel(new String[]{"Group", "Term"}, 0) {

                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }

                };

                for (int j = 0; j < terms.length; j++) {
                    tabModel.addRow(new Object[]{1, terms[j]});
                }
                tabAnnTerms.setCellEditor(null);
                tabAnnTerms.setModel(tabModel);
                tabAnnTerms.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        int row = tabAnnTerms.rowAtPoint(e.getPoint());
                        int col = tabAnnTerms.columnAtPoint(e.getPoint());
                        if (col == 0 && row < tabAnnTerms.getRowCount()) {
                            int val = (Integer) ((DefaultTableModel) tabAnnTerms.getModel()).getValueAt(row, col);
                            if (e.getButton() == MouseEvent.BUTTON1) {
                                val++;
                                if (val == groupOptionList.length) {
                                    val = 0;
                                }
                            }
                            if (e.getButton() == MouseEvent.BUTTON3) {
                                for (int j = 0; j < tabAnnTerms.getRowCount(); j++) {
                                    ((DefaultTableModel) tabAnnTerms.getModel()).setValueAt(groupOptionList.length - 1, j, col);
                                }
                                val = 0;
                            }
                            ((DefaultTableModel) tabAnnTerms.getModel()).setValueAt(val, row, col);

                            ArrayList<String> assayTerms = new ArrayList<>();
                            for (int j = 0; j < tabAnnTerms.getModel().getRowCount(); j++) {
                                if ((int) tabAnnTerms.getModel().getValueAt(j, 0) == 0) {
                                    assayTerms.add((String) tabAnnTerms.getModel().getValueAt(j, 1));
                                }

                            }
                            if (assayTerms.size() > 0) {
                                adjustSizesByAnnotationTerm(currAnn, assayTerms.toArray(new String[assayTerms.size()]));
                            } else {
                                adjustSizesByAnnotationTerm(currAnn, null);
                            }

                        }
                    }
                });
                tabAnnTerms.getColumnModel().getColumn(0).setCellRenderer(new TableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        JLabel lbl = new JLabel() {
                            @Override
                            protected void paintComponent(Graphics g) {
                                g.setColor(this.getBackground());
                                g.fillRect(0, 0, this.getWidth(), this.getHeight());
                            }
                        };
                        lbl.setText("");
                        int idx = (Integer) ((DefaultTableModel) tabAnnTerms.getModel()).getValueAt(row, column);
                        lbl.setBackground(idx < groupOptionList.length ? groupOptionList[idx].getValue() : Color.GREEN);
                        return lbl;
                    }
                });

                panGraph.validate();
                panGraph.repaint();
            } else {
                tabAnnTerms.setModel(new DefaultTableModel(new String[]{"Group", "Term"}, 0));
                adjustSizesByAnnotationTerm(null, null);

            }
        }
    };

    private boolean abort = false;

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b && !abort);
    }

    public frmScFDL(Cluster[] clusters) {
        initComponents();
        FDLParamPane fdlParamPane = new FDLParamPane(clusters[0].getClusterSet().getDataset());
        if (JOptionPane.showConfirmDialog(this, fdlParamPane, "FDL parameters", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            this.abort = true;
            this.setVisible(false);
            this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        }

        panScale = new javax.swing.JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                paintScale(g);
            }
        };

        panScale.setBackground(new Color(0, 0, 0, 0));
        panScale.setMaximumSize(new java.awt.Dimension(80, 120));
        panScale.setMinimumSize(new java.awt.Dimension(80, 120));
        panScale.setName(""); // NOI18N
        panScale.setPreferredSize(new java.awt.Dimension(80, 120));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        panGraph.setLayout(new GridBagLayout());
        panGraph.add(panScale, gridBagConstraints);

        ds = clusters[0].getClusterSet().getDataset();
        paramList = ds.getFeatureNamesCombined();
        DefaultComboBoxModel cmb = new DefaultComboBoxModel();
        cmb.addElement("---none---");

        for (Annotation a : ds.getAnnotations()) {
            cmb.addElement(a);
        }

        //String[] options = new String[]{"Angular Distance", "Mahalonobis Distance"};
        //mahalonobis = JOptionPane.showInputDialog(this, "Select the distance measure for MST reconstruction", "Distance Measure", JOptionPane.QUESTION_MESSAGE, null, options, options[0]).equals(options[1]);
        cmbAnnotations.setModel(cmb);
        cmbAnnotations.addActionListener(actl);

        final int NUM_NODES;
        final int KNN;
        //final double clust_influence;
        try {
            NUM_NODES = fdlParamPane.getNumNodes();
            KNN = fdlParamPane.getKNN();
            // clust_influence = Double.parseDouble(JOptionPane.showInputDialog("Cluster influence (how much more weight should get the intra-cluster connections):", "1"));
        } catch (NumberFormatException e) {
            logger.showException(new IllegalArgumentException("Invalid number format"));

            this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            this.setVisible(false);
            MinClusID = 0;
            return;
        } catch (NullPointerException e) {
            this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            logger.showException(e);
            MinClusID = 0;
            return;
        }

        DefaultListModel<String> lm = new DefaultListModel<>();

        // NDataset ds = clusters[0].getClusterSet().getDataset();
        scalingBounds = new double[ds.getFeatureNamesCombined().length][];
        System.arraycopy(Algebra.DEFAULT.transpose(new DenseDoubleMatrix2D(ds.getDataBounds())).toArray(), 0, scalingBounds, 0, ds.getDimension());
        System.arraycopy(Algebra.DEFAULT.transpose(new DenseDoubleMatrix2D(ds.getSideDataBounds())).toArray(), 0, scalingBounds, ds.getDimension(), ds.getSideVarNames().length);

        lm.addElement("Cluster ID");
        lm.addElement("Annotation Term");
        for (String p : ds.getFeatureNamesCombined()) {
            lm.addElement(p);
        }
        paramList = ds.getFeatureNamesCombined();
        lstParam.setModel(lm);

        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();

        //Get a graph model - it exists because we have a workspace
        final GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
        graph = graphModel.getUndirectedGraph();

        //Add boolean column
        //AttributeController ac = Lookup.getDefault().lookup(AttributeController.class);
        ArrayList<ClusterNode> alCN = new ArrayList<>();

        int lMinClusID = Integer.MAX_VALUE;
        //vMFKernel vMF = new vMFKernel(ad);
        for (Cluster c : clusters) {
            lMinClusID = Math.min(lMinClusID, c.getID());
        }
        MinClusID = lMinClusID;

        ArrayList<Datapoint> dp = new ArrayList<>();
        for (Cluster c : clusters) {
            try {
                int i = 0;
                int localNUMNODES = NUM_NODES;//(int)(Math.pow(c.size(),0.33)*40);
                for (ClusterMember cm : (new Shuffle<ClusterMember>()).shuffleCopyArray(c.getClusterMembers())) {
                    if (i++ > localNUMNODES) {
                        break;
                    }
                    dp.add(cm.getDatapoint());
                    alCN.add(new ClusterNode(c, cm.getDatapoint(), 20));
                }
            } catch (SQLException e) {
                logger.print(e);
            }
        }

        logger.print("Num Datapoints: " + dp.size());

        ColorPalette cp = ColorPalette.NEUTRAL_PALETTE;
        Collections.shuffle(alCN);

        final ClusterNode[] cn = Arrays.copyOf(alCN.toArray(new ClusterNode[alCN.size()]), alCN.size());

        double[][] nodePOX = new double[dp.size()][2];
        DenseDoubleMatrix1D[] ddm = new DenseDoubleMatrix1D[2];

        ddm[0] = new DenseDoubleMatrix1D(dp.get(0).getVector());
        ddm[1] = new DenseDoubleMatrix1D(dp.get(1).getVector());

        for (int i = 0; i < nodePOX.length; i++) {
            nodePOX[i] = new double[]{MatrixOp.mult(ddm[0].toArray(), dp.get(i).getUnityLengthVector()), MatrixOp.mult(ddm[1].toArray(), dp.get(i).getUnityLengthVector())};
        }

        double minX = nodePOX[0][0];
        double minY = nodePOX[0][1];
        double maxX = nodePOX[0][0];
        double maxY = nodePOX[0][0];
        for (int i = 0; i < cn.length; i++) {
            minX = Math.min(nodePOX[i][0], minX);
            minY = Math.min(nodePOX[i][1], minY);
            maxX = Math.max(nodePOX[i][0], maxX);
            maxY = Math.max(nodePOX[i][1], maxY);
        }

        nodes = new Node[cn.length];
        try {
            for (int i = 0; i < cn.length; i++) {
                ClusterNode c = cn[i];
                cid.put(i, c);
                Node n0 = graphModel.factory().newNode(c.toString());
                //n0.getNodeData().setLabel(s2[0]);
                n0.getNodeData().setX((float) (nodePOX[i][0] * (5000 / (maxX - minX))));
                n0.getNodeData().setY((float) (nodePOX[i][1] * (5000 / (maxY - minY))));
                n0.getNodeData().setSize(10);
                n0.getNodeData().setLabel("");//c.toString());
                n0.getAttributes().setValue("clusterNode", i);
                n0.getAttributes().setValue("cluster", c.cluster.getID());
                n0.getAttributes().setValue("colorID", -1);
                nodes[i] = n0;
                Color col = cp.getColor(c.cluster.getID() - MinClusID);
                n0.getNodeData().setColor(col.getRed() / 255f, col.getGreen() / 255f, col.getBlue() / 255f);
                graph.addNode(n0);
            }
        } catch (Exception e) {
            logger.showException(e);
        }

        //Append imported data to GraphAPI
        ///importController.process(container, new DefaultProcessor(), workspace);
        //Preview configuration
        panGraph.setDoubleBuffered(true);
        panGraph.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        panGraph.add(graphRenderer, BorderLayout.CENTER);
        ComponentAdapter ca = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                panScale.setBounds(panGraph.getWidth() - 80, panGraph.getHeight() - 100, panScale.getWidth(), panScale.getHeight());
                panGraphContainer.repaint();
            }
        };
        panGraph.addComponentListener(ca);
        this.pack();
        this.setVisible(true);
        ca.componentResized(null);
        graphRenderer.setGraph(graph);
        jSplitPane1.setDividerLocation(0.9);
        lstParam.setSelectedIndex(0);

        buildGraph(KNN, cn, jp, fdlParamPane.getDM(), fdlParamPane.getRestrictionParam(), fdlParamPane.getRestrictionParamRange());
    }

    private void buildGraph(final int KNN, final ClusterNode[] cn, final JProgressBar jp, DistanceMeasure dm, final int paramLim, final double limRange) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                sortedNodes = new Node[nodes.length][];
                int cpu = Runtime.getRuntime().availableProcessors();
                Thread[] t = new Thread[cpu];
                ThreadGroup tg = new ThreadGroup("KNNthreads");
                final AtomicInteger ai = new AtomicInteger(-1);
                jp.setMaximum(nodes.length);
                ExecutorService es = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors());
                for (int i = 0; i < t.length; i++) {
                    es.execute(new Runnable() {
                        @Override
                        public void run() {
                            int d;

                            while ((d = ai.addAndGet(1)) < nodes.length) {
                                if (d % 10 == 0) {
                                    logger.print("Computing KNN graph: " + d);
                                }
                                synchronized (jp) {
                                    jp.setValue(d);
                                    jp.repaint();
                                }
                                ArrayList<Entry<Node, Double>> arr = new ArrayList<Entry<Node, Double>>();
                                double[] vec = cn[d].dp.getVector();
                                double[] cv1 = MatrixOp.concat(cn[d].dp.getVector(), cn[d].dp.getSideVector());
                                for (int j = 0; j < nodes.length; j++) {
                                    boolean allowed = true;
                                    if (paramLim >= 0) {
                                        double[] cv2 = MatrixOp.concat(cn[j].dp.getVector(), cn[j].dp.getSideVector());
                                        allowed = Math.abs(cv1[paramLim] - cv2[paramLim]) <= limRange;
                                    }
                                    if (allowed) {
                                        arr.add(new DefaultEntry<>(nodes[j], dm.getSimilarity(vec, cn[j].dp.getVector())));
                                    }
                                }
                                Collections.sort(arr, (Entry<Node, Double> o1, Entry<Node, Double> o2) -> (int) Math.signum(o2.getValue() - o1.getValue()));
                                int lim = Math.min(KNN, arr.size());
                                sortedNodes[d] = new Node[lim];
                                for (int j = 0; j < lim; j++) {
                                    sortedNodes[d][j] = arr.get(j).getKey();
                                }
                            }
                        }
                    }
                    );
                }
                es.shutdown();
                try {
                    es.awaitTermination(1000, TimeUnit.DAYS);
                } catch (InterruptedException e) {
                    logger.showException(e);
                }
                jp.setIndeterminate(true);
                setKNNEdges(KNN);
                jp.setVisible(false);
                jButton3ActionPerformed(null);
            }
        };
        new Thread(r).start();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        glassEdit1 = new samusik.glasscmp.GlassEdit();
        jSplitPane1 = new javax.swing.JSplitPane();
        panGraphContainer = new javax.swing.JPanel();
        panGraph = new javax.swing.JPanel();
        jp = new javax.swing.JProgressBar();
        spinScalingRatio = new javax.swing.JSpinner();
        chkAdjustSizes = new javax.swing.JCheckBox();
        spinRescale = new javax.swing.JSpinner();
        jButton2 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        spinZeroCutoff = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        chkCreateStacks = new javax.swing.JCheckBox();
        spinImgPerRow = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jButton3 = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        spinNodeSize = new javax.swing.JSpinner();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        cmbAnnotations = new samusik.glasscmp.GlassComboBox();
        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        scpColumns = new javax.swing.JScrollPane();
        tabAnnTerms = new javax.swing.JTable();
        jPanel6 = new javax.swing.JPanel();
        spParamSelectionContainer = new javax.swing.JScrollPane();
        lstParam = new javax.swing.JList();

        glassEdit1.setText(org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmScFDL.glassEdit1.text")); // NOI18N

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle(org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmScFDL.title")); // NOI18N
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jSplitPane1.setDividerLocation(900);
        jSplitPane1.setResizeWeight(1.0);
        jSplitPane1.setMinimumSize(new java.awt.Dimension(300, 158));
        jSplitPane1.setPreferredSize(new java.awt.Dimension(1000, 158));

        panGraphContainer.setLayout(new java.awt.GridBagLayout());

        panGraph.setBackground(new java.awt.Color(255, 255, 255));
        panGraph.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panGraph.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        panGraph.setMinimumSize(new java.awt.Dimension(100, 100));
        panGraph.setName(""); // NOI18N
        panGraph.setPreferredSize(new java.awt.Dimension(1000, 100));
        panGraph.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                panGraphMouseClicked(evt);
            }
        });
        panGraph.setLayout(new java.awt.GridBagLayout());

        jp.setMinimumSize(new java.awt.Dimension(300, 20));
        jp.setPreferredSize(new java.awt.Dimension(300, 20));
        panGraph.add(jp, new java.awt.GridBagConstraints());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panGraphContainer.add(panGraph, gridBagConstraints);

        spinScalingRatio.setModel(new javax.swing.SpinnerNumberModel(5.0d, 0.0d, null, 1.0d));
        spinScalingRatio.setMinimumSize(new java.awt.Dimension(61, 20));
        spinScalingRatio.setPreferredSize(new java.awt.Dimension(61, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        panGraphContainer.add(spinScalingRatio, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(chkAdjustSizes, "Prevent Node Overap"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        panGraphContainer.add(chkAdjustSizes, gridBagConstraints);

        spinRescale.setModel(new javax.swing.SpinnerNumberModel(5.0d, 0.0d, null, 1.0d));
        spinRescale.setMinimumSize(new java.awt.Dimension(61, 20));
        spinRescale.setPreferredSize(new java.awt.Dimension(61, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        panGraphContainer.add(spinRescale, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jButton2, org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmScFDL.jButton2.text")); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        panGraphContainer.add(jButton2, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jButton4, org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmScFDL.jButton4.text")); // NOI18N
        jButton4.setMaximumSize(new java.awt.Dimension(200, 23));
        jButton4.setMinimumSize(new java.awt.Dimension(200, 23));
        jButton4.setPreferredSize(new java.awt.Dimension(200, 23));
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        panGraphContainer.add(jButton4, gridBagConstraints);

        spinZeroCutoff.setModel(new javax.swing.SpinnerNumberModel(0.19d, null, null, 0.001d));
        spinZeroCutoff.setMinimumSize(new java.awt.Dimension(61, 20));
        spinZeroCutoff.setPreferredSize(new java.awt.Dimension(61, 20));
        spinZeroCutoff.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinZeroCutoffStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        panGraphContainer.add(spinZeroCutoff, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmScFDL.jLabel4.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        panGraphContainer.add(jLabel4, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmScFDL.jLabel5.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        panGraphContainer.add(jLabel5, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmScFDL.jButton1.text")); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        panGraphContainer.add(jButton1, gridBagConstraints);

        chkCreateStacks.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(chkCreateStacks, org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmScFDL.chkCreateStacks.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        panGraphContainer.add(chkCreateStacks, gridBagConstraints);

        spinImgPerRow.setModel(new javax.swing.SpinnerNumberModel(5, 1, null, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        panGraphContainer.add(spinImgPerRow, gridBagConstraints);

        jLabel7.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(0, 123, 255));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel7, org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmScFDL.jLabel7.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panGraphContainer.add(jLabel7, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel8, org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmScFDL.jLabel8.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        panGraphContainer.add(jLabel8, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jButton3, org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmScFDL.jButton3.text")); // NOI18N
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        panGraphContainer.add(jButton3, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel9, org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmScFDL.jLabel9.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 0;
        panGraphContainer.add(jLabel9, gridBagConstraints);

        spinNodeSize.setModel(new javax.swing.SpinnerNumberModel(3, 1, 10, 1));
        spinNodeSize.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinNodeSizeStateChanged(evt);
            }
        });
        panGraphContainer.add(spinNodeSize, new java.awt.GridBagConstraints());

        jSplitPane1.setLeftComponent(panGraphContainer);

        jPanel4.setLayout(new java.awt.GridBagLayout());

        jPanel5.setLayout(new java.awt.GridBagLayout());

        cmbAnnotations.setModel(new DefaultComboBoxModel<Annotation>());
        cmbAnnotations.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbAnnotationsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel5.add(cmbAnnotations, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmScFDL.jLabel1.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel5.add(jLabel1, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel7.setOpaque(false);
        jPanel7.setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmScFDL.jLabel2.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 8);
        jPanel7.add(jLabel2, gridBagConstraints);

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/vortex/resources/azure 16x16.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmScFDL.jLabel3.text")); // NOI18N
        jLabel3.setToolTipText(org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmScFDL.jLabel3.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 6, 6, 6);
        jPanel7.add(jLabel3, gridBagConstraints);

        jLabel6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/vortex/resources/grey 16x16.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel6, org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmScFDL.jLabel6.text")); // NOI18N
        jLabel6.setToolTipText(org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmScFDL.jLabel6.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(7, 7, 7, 7);
        jPanel7.add(jLabel6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(jPanel7, gridBagConstraints);

        tabAnnTerms.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Group", "Term"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        scpColumns.setViewportView(tabAnnTerms);
        if (tabAnnTerms.getColumnModel().getColumnCount() > 0) {
            tabAnnTerms.getColumnModel().getColumn(0).setHeaderValue(org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmClusterGraphX.tabAnnTerms.columnModel.title1")); // NOI18N
            tabAnnTerms.getColumnModel().getColumn(1).setHeaderValue(org.openide.util.NbBundle.getMessage(frmScFDL.class, "frmClusterGraphX.tabAnnTerms.columnModel.title2")); // NOI18N
        }

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel1.add(scpColumns, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        jPanel5.add(jPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        jPanel4.add(jPanel5, gridBagConstraints);

        jPanel6.setLayout(new java.awt.BorderLayout());

        lstParam.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lstParamMouseClicked(evt);
            }
        });
        lstParam.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lstParamValueChanged(evt);
            }
        });
        spParamSelectionContainer.setViewportView(lstParam);

        jPanel6.add(spParamSelectionContainer, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        jPanel4.add(jPanel6, gridBagConstraints);

        jSplitPane1.setRightComponent(jPanel4);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jSplitPane1, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void panGraphMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panGraphMouseClicked
    }//GEN-LAST:event_panGraphMouseClicked

    private void lstParamValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstParamValueChanged
        if (evt != null) {
            curParamIDX = -1;
            if (lstParam.getSelectedIndex() == 0) {
                NodeIterable ni = graph.getNodes();
                for (final Node n : ni.toArray()) {
                    if (n.getAttributes().getValue("cluster") == null) {
                        continue;
                    }
                    ClusterNode cl = cid.get((int) n.getAttributes().getValue("clusterNode"));
                    if (n.getNodeData().getSize() > 5.01f) {
                        Color c = pal.getColor(cl.cluster.getID() - MinClusID);
                        n.getNodeData().setR(c.getRed() / 255f);
                        n.getNodeData().setG(c.getGreen() / 255f);
                        n.getNodeData().setB(c.getBlue() / 255f);
                    } else {
                        n.getNodeData().setColor(0.8f, 0.8f, 0.8f);
                    }
                    try {

                    } catch (Exception e) {
                        logger.showException(e);
                    }
                }
            } else if (lstParam.getSelectedIndex() == 1) {
                NodeIterable ni = graph.getNodes();
                Color gray = new Color(200, 200, 200);
                for (final Node n : ni.toArray()) {
                    if (n.getAttributes().getValue("cluster") == null) {
                        continue;
                    }
                    int colorID = (int) n.getAttributes().getValue("colorID");
                    curParamIDX = -1;
                    if (n.getNodeData().getSize() > 5.01f && colorID >= 0) {
                        Color c = pal.getColor(colorID);
                        n.getNodeData().setR(c.getRed() / 255f);
                        n.getNodeData().setG(c.getGreen() / 255f);
                        n.getNodeData().setB(c.getBlue() / 255f);
                    } else {
                        n.getNodeData().setColor(0.8f, 0.8f, 0.8f);
                    }
                    try {
                    } catch (Exception e) {
                        logger.showException(e);
                    }
                }
            } else {
                colorNodesByExpressionLevel(lstParam.getSelectedIndex() - 2);
            }
        }
        panGraphContainer.repaint();
    }//GEN-LAST:event_lstParamValueChanged

    private double getDensity(int i, int k) {
        double d = 0;
        ClusterNode cn = cid.get((int) sortedNodes[i][0].getAttributes().getValue("clusterNode"));
        int cnt = 0;
        for (int j = 1; j < Math.min(sortedNodes[i].length, k); j++) {
            ClusterNode c1 = cid.get((int) sortedNodes[i][j].getAttributes().getValue("clusterNode"));
            double dist = Math.acos(MatrixOp.getEuclideanCosine(cn.dp.getVector(), c1.dp.getVector()));
            d += dist;
            cnt++;
        }
        return cnt > 0 ? d / cnt : 0;
    }

    private void setKNNEdges(final int k) {
        graph.clear();

        final double[] dens = new double[sortedNodes.length];

        int cpu = Runtime.getRuntime().availableProcessors();
        ExecutorService e = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        Callable<Object>[] tasks = new Callable[cpu];

        int step = (int) Math.floor(dens.length / cpu);
        for (int i = 0; i < tasks.length; i++) {
            final int from = i * (dens.length / cpu);
            final int to = Math.min(dens.length, (i + 1) * step);
            tasks[i] = new Callable() {
                @Override
                public Object call() {
                    for (int i = from; i < to; i++) {
                        dens[i] = getDensity(i, k);
                    }
                    return null;
                }
            };
        }

        try {
            e.invokeAll(Arrays.asList(tasks));
        } catch (InterruptedException ex) {
            logger.showException(ex);
        }

        logger.print("done computing densities");
        double[] copydens = MatrixOp.copy(dens);
        Arrays.sort(copydens);

        double ths = copydens[(int) (dens.length * 0.05)];

        logger.print("filtering the nodes");

        HashMap<Node, Integer> hmDens = new HashMap<>();

        for (int i = 0; i < copydens.length; i++) {
            hmDens.put(nodes[i], i);
            graph.addNode(nodes[i]);
        }

        /*if (dens[i] < ths) {
                if (graph.contains(nodes[i])) {
                    graph.removeNode(nodes[i]);
                }
            } else if (!graph.contains(nodes[i])) {
                graph.addNode(nodes[i]);
            }*/
 /*
        for (int i = 0; i < sortedNodes.length; i++) {
            if (sortedNodes[i].length > 0 && dens[i] > ths) {
                graph.addNode(nodes[i]);
            }
        }*/
        logger.print("Adding edges");

        for (int i = 0; i < nodes.length; i++) {

            ClusterNode cl = cid.get((int) nodes[i].getAttributes().getValue("clusterNode"));
            if (sortedNodes[i].length > 0) {
                int nn = Math.max(((k * 2) * hmDens.get(nodes[i])) / nodes.length, 1) + 1;
                for (int j = 1; j < Math.min(nn, sortedNodes[i].length); j++) {

                    graph.addEdge(nodes[i], sortedNodes[i][j]);

                    Edge ed = graph.getEdge(nodes[i], sortedNodes[i][j]);
                    ClusterNode cl2 = cid.get((int) sortedNodes[i][j].getAttributes().getValue("clusterNode"));

                    float df = (float) (2.0f * Math.max(0.01, Math.exp(10 * (MatrixOp.getEuclideanCosine(cl.dp.getVector(), cl2.dp.getVector()) - 1))));
                    ed.setWeight(df);

                }
            }
        }
        logger.print("Done adding edges");
    }

    private void lstParamMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lstParamMouseClicked
        if (evt.getButton() == MouseEvent.BUTTON3) {
            if (fixedParamVal != lstParam.getSelectedIndex()) {
                fixedParamVal = lstParam.getSelectedIndex();
            } else {
                fixedParamVal = -1;
            }
        }
    }//GEN-LAST:event_lstParamMouseClicked

    private void cmbAnnotationsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbAnnotationsActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cmbAnnotationsActionPerformed

    final ForceAtlas2 layout = new ForceAtlas2(new ForceAtlas2Builder());
    boolean layoutOn = false;
    SwingWorker sw = null;

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        spinScalingRatio.setEnabled(layoutOn);
        chkAdjustSizes.setEnabled(layoutOn);
        if (layoutOn) {
            layoutOn = false;
            sw.cancel(true);
            jButton3.setText("Start Layout");
            graphRenderer.showEdges(true);
        } else {
            layoutOn = true;
            graphRenderer.showEdges(false);
            jButton3.setText("Stop Layout");
            layout.setGraphModel(graph.getGraphModel());
            layout.initAlgo();
            layout.setThreadsCount(Runtime.getRuntime().availableProcessors() / 2);
            layout.setAdjustSizes(chkAdjustSizes.isSelected());
            layout.setBarnesHutOptimize(true);
            layout.setScalingRatio((Double) spinScalingRatio.getValue());

            sw = new SwingWorker() {
                long prevTime = Calendar.getInstance().getTimeInMillis();

                @Override
                protected Object doInBackground() throws Exception {
                    while (layoutOn && layout.canAlgo() && !this.isCancelled()) {
                        for (int i = 0; i < 10 && layout.canAlgo(); i++) {
                            layout.goAlgo();
                            if (Calendar.getInstance().getTimeInMillis() - prevTime > 40) {
                                lstParamValueChanged(null);
                                panGraphContainer.repaint();
                                prevTime = Calendar.getInstance().getTimeInMillis();
                            }
                        }
                    }
                    return null;
                }
            };
            sw.execute();
        }

    }//GEN-LAST:event_jButton3ActionPerformed

    public static String getFileSafeString(String in) {
        return in.replaceAll("\\|", "-").replaceAll("\\\\", "-").replaceAll("/", "-");
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        for (Node n : graph.getNodes()) {
            double f = (Double) spinRescale.getValue();
            n.getNodeData().setX((float) (n.getNodeData().x() * f));
            n.getNodeData().setY((float) (n.getNodeData().y() * f));
        }
        panGraphContainer.repaint();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        try {
            String dest = JOptionPane.showInputDialog("Set export path:", "..somepath\\sample.graphml");
            ec.exportFile(new File(dest));
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
    }//GEN-LAST:event_jButton4ActionPerformed

    private void spinZeroCutoffStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinZeroCutoffStateChanged
        COLOR_MAP_NOISE_THRESHOLD = (Double) spinZeroCutoff.getValue();
        lstParamValueChanged(null);
    }//GEN-LAST:event_spinZeroCutoffStateChanged

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        boolean createStack = chkCreateStacks.isSelected() && tabAnnTerms.getModel().getRowCount() > 0;
        int imgPerRow = ((Integer) spinImgPerRow.getValue());
        imgPerRow = Math.min(tabAnnTerms.getModel().getRowCount(), imgPerRow);
        int rows = 1;
        try {
            rows = (tabAnnTerms.getModel().getRowCount() / imgPerRow);
            if (tabAnnTerms.getModel().getRowCount() % imgPerRow != 0) {
                rows++;
            }
        } catch (Exception e) {
        }

        //Export full graph
        File file = IO.chooseFileWithDialog("frmFDLexport", "Directories", null, true);
        if (file == null) {
            return;
        }
        String path = file.getPath();
        int w = graphRenderer.getWidth();
        int h = graphRenderer.getHeight();
        try {
            for (int i = 0; i < paramList.length; i++) {
                BufferedImage stack = (createStack) ? new BufferedImage(w * imgPerRow, h * rows, BufferedImage.TYPE_INT_RGB) : null;
                int curRow = 0;
                int curCol = 0;
                if (createStack) {
                    ((Graphics2D) stack.getGraphics()).setPaint(Color.WHITE);
                    ((Graphics2D) stack.getGraphics()).fillRect(0, 0, w * imgPerRow, h * rows);
                }
                for (int j = 0; j < tabAnnTerms.getModel().getRowCount(); j++) {
                    adjustSizesByAnnotationTerm(currAnn, new String[]{(String) tabAnnTerms.getModel().getValueAt(j, 1)});
                    colorNodesByExpressionLevel(i);
                    panGraphContainer.repaint();

                    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                    graphRenderer.paintAll(img.createGraphics());
                    File f = new File(path + File.separator + "_" + getFileSafeString(currParam) + "_" + currAnnTerms[0] + ".png");
                    ImageIO.write(img, "PNG", f);
                    if (createStack) {
                        BufferedImage bi = ImageIO.read(f);
                        f.delete();
                        Graphics2D g2 = ((Graphics2D) bi.createGraphics());
                        double fsize = (int) (50.0 * (400.0 / bi.getHeight()));
                        g2.setFont(new java.awt.Font("Arial", Font.BOLD, (int) fsize));
                        g2.setColor(new Color(0, 0, 0, 255));
                        g2.drawString(currAnnTerms[0], 5, (int) fsize + 5);
                        g2.setStroke(new BasicStroke(5.0f));
                        g2.drawRect(0, 0, w - 1, h - 1);
                        stack.getGraphics().drawImage(bi, curCol * w, curRow * h, null);
                        curCol++;
                        if (curCol == imgPerRow) {
                            curCol = 0;
                            curRow++;
                        }
                    }
                }
                if (createStack) {
                    ImageIO.write(stack, "PNG", new File(path + File.separator + "stack_" + ds.getName() + "_" + getFileSafeString(currParam) + ".png"));
                }
            }

            if (tabAnnTerms.getModel().getRowCount() == 0) {
                for (int i = 0; i < paramList.length; i++) {
                    colorNodesByExpressionLevel(i);
                    panGraphContainer.repaint();
                    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                    graphRenderer.paintAll(img.createGraphics());
                    File f = new File(path + File.separator + ds.getName() + "_" + getFileSafeString(currParam) + ".png");
                    ImageIO.write(img, "PNG", f);
                }
            }

            for (int i = 0; i < paramList.length; i++) {
                colorNodesByExpressionLevel(i);
                BufferedImage bi = ScaleImageGenerator.generateScaleImage(100, i, ds, COLOR_MAP_NOISE_THRESHOLD, qm, sqm);
                ImageIO.write(bi, "PNG", new File(path + File.separator + ds.getName() + "_scaleFor" + "_" + getFileSafeString(currParam) + ".png"));
            }
        } catch (IOException ex) {
            logger.showException(ex);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void spinNodeSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinNodeSizeStateChanged
        graphRenderer.setNodeSize((Integer) spinNodeSize.getValue() - 1);
        graphRenderer.repaint();
    }//GEN-LAST:event_spinNodeSizeStateChanged
    /**
     * @param args the command line arguments
     */
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox chkAdjustSizes;
    private javax.swing.JCheckBox chkCreateStacks;
    private samusik.glasscmp.GlassComboBox cmbAnnotations;
    private samusik.glasscmp.GlassEdit glassEdit1;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JProgressBar jp;
    private javax.swing.JList lstParam;
    private javax.swing.JPanel panGraph;
    private javax.swing.JPanel panGraphContainer;
    private javax.swing.JScrollPane scpColumns;
    private javax.swing.JScrollPane spParamSelectionContainer;
    private javax.swing.JSpinner spinImgPerRow;
    private javax.swing.JSpinner spinNodeSize;
    private javax.swing.JSpinner spinRescale;
    private javax.swing.JSpinner spinScalingRatio;
    private javax.swing.JSpinner spinZeroCutoff;
    private javax.swing.JTable tabAnnTerms;
    // End of variables declaration//GEN-END:variables
    private NodeLabelRenderer nlr = new CustomNodeLabelRenderer(false);
    private NodeRenderer nr = new NodeRenderer() {
        @Override
        public void renderProcessing(Item item, ProcessingTarget target, PreviewProperties properties) {
            Float x = item.getData(NodeItem.X);
            Float y = item.getData(NodeItem.Y);
            Float size = item.getData(NodeItem.SIZE);
            Color color = item.getData(NodeItem.COLOR);
            int alpha = (int) ((properties.getFloatValue(PreviewProperty.NODE_OPACITY) / 100f) * 255f);
            if (alpha > 255) {
                alpha = 255;
            }
            PGraphics graphics = target.getGraphics();
            graphics.fill(color.getRed(), color.getGreen(), color.getBlue(), alpha);
            if (size > 21) {
                logger.print(size);
                Color stC = color.darker().darker().darker();
                graphics.stroke(stC.getRed(), stC.getGreen(), stC.getBlue(), alpha);
                graphics.strokeWeight(3f);
                graphics.ellipse(x, y, size, size);
            } else {
                graphics.stroke(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                graphics.strokeWeight(1f);
                graphics.ellipse(x, y, size, size);
            }
        }
    };
}
