/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustergraph;

import util.ColorScale;
import com.itextpdf.text.Font;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeIterable;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2Builder;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.api.RenderTarget;
import org.gephi.preview.types.DependantColor;
import org.gephi.preview.types.DependantOriginalColor;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.ProjectController;
import org.openide.util.Lookup;
import umontreal.iro.lecuyer.probdist.NormalDist;
import sandbox.annotations.Annotation;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import sandbox.clustering.AngularDistance;
import sandbox.clustering.Cluster;
import sandbox.clustering.ClusterMember;
import util.HypergeometricDist;
import sandbox.clustering.Datapoint;
import sandbox.clustering.Dataset;
import sandbox.clustering.DistanceMeasure;
import sandbox.clustering.EuclideanDistance;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.RenderedImage;
import java.util.Comparator;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.preview.api.G2DTarget;
import org.gephi.project.api.Project;
import org.gephi.project.api.Workspace;
import org.gephi.statistics.plugin.ConnectedComponents;
import util.DefaultEntry;
import util.IO;
import util.MatrixOp;
import util.logger;
import vortex.clustering.HierarchicalCentroid;
import util.QuantileMap;
import vortex.gui.WrapLayout;
import vortex.util.Config;
import vortex.util.GroupStatisticsInClusters;

/**
 *
 * @author Nikolay
 */
public class frmGraph extends javax.swing.JFrame {
    
    private static frmGraph Instance = null;

    private final double EDGE_SCALING = 2;
    private final double[][] scalingBounds;
    private String[] paramList;
    private ColorScale colorScale;
    private GraphModel graphModel;
    private HashMap<Integer, Cluster> cid = new HashMap<>();
    private HashMap<Cluster, double[]> expArrayLists = new HashMap<>();
    private HashMap<Cluster, double[]> pValArrayLists = new HashMap<>();
    private final PreviewController previewController;
    private final Dataset ds;
    String currParam = "";
    String[] currAnnTerms = null;
    private final G2DTarget target;
    Annotation currAnn;
    private int fixedParamVal = -1;
    private final Map.Entry<String, Color>[] groupOptionList = new Map.Entry[]{
        new DefaultEntry<>("Assay", new Color(120, 194, 235)),
        new DefaultEntry<>("None", Color.GRAY)};
    private Graph graph;

    private ForceAtlas2 layout;
    double COLOR_MAP_NOISE_THRESHOLD = 0.19;

    QuantileMap qm, sqm;

    final PreviewSketch previewSketch;


    private ColorScale.ScalingMode getColorScalingMode() {
        if (rbQuantile.isSelected()) {
            return ColorScale.ScalingMode.QUANTILE;
        } else {
            return ColorScale.ScalingMode.LINEAR;
        }
    }

    public static frmGraph getInstance() {
        return Instance;
    }
    
    

    public frmGraph(Cluster[] clusters, String graphType) {
        initComponents();
        
        if(Instance!=null){
            Instance.setVisible(false);
            Instance.dispose();
        }
        
        if(panScFDL.getInstance()!=null){
            Component parent = panScFDL.getInstance();
            do{
                parent = parent.getParent();
            }while(! (parent instanceof JFrame));
           parent.setVisible(false);
           ((JFrame)parent).dispose();
        }
        
        
        Instance = this;

        panTop.setLayout(new WrapLayout(WrapLayout.LEADING));
        panBottom.setLayout(new WrapLayout(WrapLayout.LEADING));

        ds = clusters[0].getClusterSet().getDataset();

        qm = QuantileMap.getQuantileMap(ds);
        sqm = QuantileMap.getQuantileMapForSideParam(ds);

        colorScale = new ColorScale(100, ds, COLOR_MAP_NOISE_THRESHOLD, qm, sqm, getColorScalingMode());

        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        Project p = pc.getCurrentProject();

        final Workspace workspace1 = pc.newWorkspace(p);

        pc.openWorkspace(workspace1);
        //Get a graph model - it exists because we have a workspace
        GraphController gc = Lookup.getDefault().lookup(GraphController.class);
        graphModel = gc.getGraphModel();

        switch (graphType) {
            case "MST":
                String[] options = new String[]{"Angular Distance", "Euclidean Distance"};
                String dist = options[0];
                try {
                    dist = (String) JOptionPane.showInputDialog(this, "Select the distance measure for MST reconstruction", "Distance Measure", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                } catch (NullPointerException e) {
                    logger.showException(e);
                    this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                }
                DistanceMeasure dm = dist.equals(options[0]) ? new AngularDistance() : new EuclideanDistance();
                buildMSTGraph(clusters, dm);
                break;
            case "DMT":
                buildDMTgraph(clusters);
                break;
            case "ModuleMap":
                buildModuleMapGraph(clusters, currAnn, graphType, EDGE_SCALING);
                break;
            default:
                throw new IllegalStateException("Unsupported graph type:" + graphType + ", allowed types are MST, DMT, ModuleMap");
        }

        paramList = ds.getFeatureNamesCombined();
        DefaultComboBoxModel cmb = new DefaultComboBoxModel();
        cmb.addElement("---none---");

        for (Annotation a : ds.getAnnotations()) {
            cmb.addElement(a);
        }

        // mahalonobis = JOptionPane.showInputDialog(this, "Select the distance measure for MST reconstruction", "Distance Measure", JOptionPane.QUESTION_MESSAGE, null, options, options[0]).equals(options[1]);
        //    final boolean quantile_dist = JOptionPane.showConfirmDialog(this, "Use Quantile Linkage?") == JOptionPane.OK_OPTION;
        // MahalonobisDistance[] mds = new MahalonobisDistance[clusters.length];
        /*   if (mahalonobis) {
         for (int i = 0; i < clusters.length; i++) {
         mds[i] = new MahalonobisDistance(clusters[i]);
         }
         }*/
        cmbAnnotations.setModel(cmb);
        cmbAnnotations.addActionListener(lis);

        cmbAnnotations.setMaximumSize(new Dimension(100, 30));
        cmbAnnotations.setPreferredSize(new Dimension(100, 30));
        cmbAnnotations.setMinimumSize(new Dimension(100, 30));

        DefaultListModel<String> lm = new DefaultListModel<>();

        // NDataset ds = clusters[0].getClusterSet().getDataset();
        scalingBounds = new double[ds.getFeatureNamesCombined().length][];
        System.arraycopy(Algebra.DEFAULT.transpose(new DenseDoubleMatrix2D(ds.getDataBounds())).toArray(), 0, scalingBounds, 0, ds.getNumDimensions());
        System.arraycopy(Algebra.DEFAULT.transpose(new DenseDoubleMatrix2D(ds.getSideDataBounds())).toArray(), 0, scalingBounds, ds.getNumDimensions(), ds.getSideVarNames().length);

        for (String e : ds.getFeatureNamesCombined()) {
            lm.addElement(e);
        }
        paramList = ds.getFeatureNamesCombined();
        lstParam.setModel(lm);

        //Get a graph model - it exists because we have a workspace
        //Preview configuration
        previewController = Lookup.getDefault().lookup(PreviewController.class);
        PreviewModel previewModel = previewController.getModel();
        previewModel.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_COLOR, new DependantOriginalColor(Color.BLACK));
        previewModel.getProperties().putValue(PreviewProperty.EDGE_CURVED, Boolean.FALSE);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_OPACITY, 50);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_RADIUS, 1f);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(Color.BLACK));
        previewModel.getProperties().putValue(PreviewProperty.BACKGROUND_COLOR, Color.WHITE);
        previewModel.getProperties().putValue(PreviewProperty.NODE_BORDER_WIDTH, 0);
        previewModel.getProperties().putValue(PreviewProperty.NODE_BORDER_COLOR, new DependantColor(new Color(100, 100, 100)));
        previewModel.getProperties().putValue(PreviewProperty.NODE_OPACITY, 100);
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_PROPORTIONAL_SIZE, true);
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, this.getFont().deriveFont(3.0f));

        previewController.refreshPreview();

        CustomMouseListener cml = new CustomMouseListener();

        target = (G2DTarget) previewController.getRenderTarget(RenderTarget.G2D_TARGET);

        previewSketch = new PreviewSketch(target);

        //previewSketch.setBorder(new LineBorder(Color.red, 3));
        previewSketch.setDoubleBuffered(true);
        
        previewSketch.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e); //To change body of generated methods, choose Tools | Templates.
                lstParamValueChanged(null);
            }

               
        
        });
        //New Processing target, get the PApplet

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        panGraphContainer.add(previewSketch, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        this.pack();
        this.setVisible(true);
        jSplitPane1.setDividerLocation(0.9);
        lstParam.setSelectedIndex(0);

        initLayout();

        previewSketch.resetZoom();
        lstParamValueChanged(null);

        //Column selectedCol = graph.getModel().getNodeTable().addColumn("selected", Boolean.class);
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                previewSketch.resetZoom();
                lstParamValueChanged(null);
            }
        });
    }

    private void colorNodesByExpressionLevel(int paramIDX) {
        if (paramIDX < 0) {
            return;
        }

        colorScale.setMinValue(paramIDX, COLOR_MAP_NOISE_THRESHOLD);

        currParam = paramList[paramIDX];

        NodeIterable ni = graph.getNodes();
        for (final Node n : ni.toArray()) {
            if (n.getAttribute("cluster") == null) {
                continue;
            }
            Cluster cl = cid.get((int) n.getAttribute("cluster"));
            if (cl == null) {
                continue;
            }
            if (expArrayLists.get(cl) == null) {
                expArrayLists.put(cl, MatrixOp.concat(MatrixOp.concat(cl.getMode().getVector(), cl.getMode().getSideVector()), new double[]{cl.getAvgSize()}));
            }
            double[] vec = expArrayLists.get(cl);

            //double scaledVal = colorScale.getScaledValue(paramIDX, vec);
            //Color c = (fixedParamVal >= 0) ? new Color((int) (getScaledValue(fixedParamVal, vec) * 255), (int) (scaledVal * 255), 0) : getColorForValue(scaledVal);
            /*if (pValArrayLists.get(cl) != null) {
                    
                }*/
            Color c = colorScale.getColor(paramIDX, vec);

            n.setR(c.getRed() / 255f);
            n.setG(c.getGreen() / 255f);
            n.setB(c.getBlue() / 255f);
        }
        logger.print("finished coloring:" + paramIDX);
    }

    private void adjustSizesByAnnotationTerm(final Annotation ann, final String[] termsAssay, final String[] termsControl, boolean inBackground) {
        logger.print("termsAssay: " + Arrays.toString(termsAssay));
        logger.print("termsControl: " + Arrays.toString(termsControl));
        if (groupingWorker != null) {
            groupingWorker.cancel(true);
        }

        groupingWorker = new GroupingWorker(ann, termsAssay, termsControl);

        if (inBackground) {
            groupingWorker.execute();
        } else {
            try {
                groupingWorker.doInBackground();
            } catch (Exception e) {
                logger.print(e);
            }
        }

    }
    private BufferedImage scaleImg = null;

    ActionListener lis = new ActionListener() {
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
                for (String term : terms) {
                    tabModel.addRow(new Object[]{1, term});
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
                        }

                        ArrayList<String> assayTerms = new ArrayList<>();
                        ArrayList<String> controlTerms = new ArrayList<>();
                        for (int j = 0; j < tabAnnTerms.getModel().getRowCount(); j++) {
                            if ((int) tabAnnTerms.getModel().getValueAt(j, 0) == 0) {
                                assayTerms.add((String) tabAnnTerms.getModel().getValueAt(j, 1));
                            }
                            /*if ((int) tabAnnTerms.getModel().getValueAt(j, 0) == 1) {
                                controlTerms.add((String) tabAnnTerms.getModel().getValueAt(j, 1));
                            }*/
                        }
                        if (assayTerms.size() > 0) {
                            adjustSizesByAnnotationTerm(currAnn, assayTerms.toArray(new String[assayTerms.size()]), controlTerms.toArray(new String[controlTerms.size()]), true);
                        } else {
                            adjustSizesByAnnotationTerm(currAnn, null, null, true);
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

                previewSketch.validate();
                previewSketch.repaint();
            } else {
                tabAnnTerms.setModel(new DefaultTableModel(new String[]{"Group", "Term"}, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                });

                adjustSizesByAnnotationTerm(null, null, null, true);

            }
        }
    };

    private void initLayout() {
        layout = new ForceAtlas2(new ForceAtlas2Builder());

        layout.setGraphModel(graph.getModel());
        layout.initAlgo();

        layout.setAdjustSizes(false);
        layout.setScalingRatio(25.0);
        layout.setBarnesHutOptimize(false);
        layout.setScalingRatio((Double) spinScalingRatio.getValue());
        for (int i = 0; i < 2000 && layout.canAlgo(); i++) {
            layout.goAlgo();
        }

        layout.setScalingRatio(5.0);
        layout.setAdjustSizes(true);
        layout.setScalingRatio((Double) spinScalingRatio.getValue());
        for (int i = 0; i < 3000 && layout.canAlgo(); i++) {
            layout.goAlgo();
        }

        previewSketch.resetZoom();
        lstParamValueChanged(null);
    }

    private void buildModuleMapGraph(Cluster[] clusters, Annotation ann, String MODE_STR, Double K) {
        if (ann == null) {
            ann = (Annotation) JOptionPane.showInputDialog(this, "Select annotation:", "Select Annotation", JOptionPane.QUESTION_MESSAGE, null, clusters[0].getClusterSet().getDataset().getAnnotations(), null);
        }

        MODE_STR = (String) JOptionPane.showInputDialog(this, "Similarity mode", "Compute strenght of edges based on:", JOptionPane.QUESTION_MESSAGE, null, new String[]{"Population frequencies across conditions", "Functional variable patterns across conditions"}, null);
        if (MODE_STR == null) {
            this.setVisible(false);
        }
        boolean MODE_FUNC = false;
        if (MODE_STR.startsWith("Functional")) {
            MODE_FUNC = true;
        }
        try {
            double[][] annFreq = MODE_FUNC ? GroupStatisticsInClusters.getGroupStatisticsFunctionalArrayLists(clusters, ann, false) : GroupStatisticsInClusters.getGroupStatisticsArrayLists(clusters, ann, false);

            if (K == null) {
                K = Double.parseDouble((String) JOptionPane.showInputDialog("Choose von Mises-Fisher K (suggested range: 1-500, higher values->sparser graph)", "10.0"));
            }

            graph = buildModuleMapGraph(clusters, annFreq, K);
            adjustModuleMapGraph(K, clusters, annFreq);

        } catch (SQLException e) {
            logger.showException(e);
        }
    }

    private void adjustModuleMapGraph(double K, Cluster[] clusters, double[][] annFreq) {

        graph.clearEdges();

        Node[] nodes = graph.getNodes().toArray();

        Datapoint[] clusterCentroids = new Datapoint[nodes.length];

        for (int i = 0; i < clusterCentroids.length; i++) {
            clusterCentroids[i] = new Datapoint("avg" + clusters[i].toString(), clusters[i].getMode().getVector(), clusters[i].getMode().getSideVector(), i);
        }

        //final DistanceMeasure dm = clusters[0].getClusterSet().getDistanceMeasure();
        for (int i = 0; i < clusterCentroids.length; i++) {
            for (int j = i + 1; j < clusterCentroids.length; j++) {
                double cos = MatrixOp.getEuclideanCosine(annFreq[i], annFreq[j]);
                boolean neg = cos < 0;
                cos = Math.abs(cos);
                double weight = Math.exp(K * cos) / Math.exp(K);

                Double dist = Math.max(0, (weight * EDGE_SCALING));
                if (dist / EDGE_SCALING > 0.01) {
                    Edge e = graph.getModel().factory().newEdge(nodes[i], nodes[j], 0, dist, false);
                    if (neg) {
                        e.setR(0.0f);
                        e.setG(0.7f);
                        e.setB(0.0f);
                    } else {
                        e.setR(0.7f);
                        e.setG(0.0f);
                        e.setB(0.0f);
                    }
                    graph.addEdge(e);
                }
            }
        }
    }

    private UndirectedGraph buildModuleMapGraph(Cluster[] clusters, double[][] annFreq, double K) {

        for (Cluster c : clusters) {
            cid.put(c.getID(), c);
        }

        UndirectedGraph localGraph = graphModel.getUndirectedGraph();

        localGraph.getModel().getNodeTable().addColumn("selected", Boolean.class);
        localGraph.getModel().getNodeTable().addColumn("cluster", Integer.class);
        Node[] nodes = new Node[clusters.length];
        int idx = 0;
        try {
            for (Cluster c : clusters) {
                Node n0 = graphModel.factory().newNode(c.toString());
                n0.setX((float) c.getMode().getVector()[0]);
                n0.setY((float) c.getMode().getVector()[1]);
                n0.setSize((float) Math.max(1, Math.pow(c.size(), 0.33)));
                n0.setLabel("id" + c.getID() + (c.getComment().length() > 0 ? (": " + c.getComment()) : ""));//c.toString());
                n0.setAttribute("cluster", c.getID());
                localGraph.addNode(n0);
                nodes[idx++] = n0;
            }
        } catch (Exception e) {
            logger.showException(e);
        }

        Datapoint[] clusterCentroids = new Datapoint[nodes.length];

        for (int i = 0; i < clusterCentroids.length; i++) {
            clusterCentroids[i] = new Datapoint("avg" + clusters[i].toString(), clusters[i].getMode().getVector(), clusters[i].getMode().getSideVector(), i);
        }

        //final DistanceMeasure dm = clusters[0].getClusterSet().getDistanceMeasure();
        for (int i = 0; i < clusterCentroids.length; i++) {
            for (int j = i + 1; j < clusterCentroids.length; j++) {
                double cos = MatrixOp.getEuclideanCosine(annFreq[i], annFreq[j]);
                double weight = Math.exp(K * cos) / Math.exp(K);
                Double dist = (weight * EDGE_SCALING);
                if (dist / EDGE_SCALING > 0.01) {
                    localGraph.addEdge(localGraph.getModel().factory().newEdge(nodes[i], nodes[j], 0, dist, false));
                }
            }
        }

        layout = new ForceAtlas2(new ForceAtlas2Builder());
        layout.setGraphModel(graphModel);
        layout.initAlgo();
        layout.setScalingRatio(layout.getScalingRatio() / 2);
        for (int i = 0; i < 1000 && layout.canAlgo(); i++) {
            layout.goAlgo();
        }

        return localGraph;
    }

    private void buildMSTGraph(Cluster[] clusters, DistanceMeasure dm) {

        graph = graphModel.getUndirectedGraph();

        graph.getModel().getNodeTable().addColumn("cluster", Integer.class);
        graph.getModel().getNodeTable().addColumn("logRatio", Double.class);
        graph.getModel().getNodeTable().addColumn("hd-label", String.class);

        for (Cluster c : clusters) {
            cid.put(c.getID(), c);
        }

        final Node[] nodes = new Node[clusters.length];
        int idx = 0;

        for (Cluster c : clusters) {
            Node n0 = graph.getModel().factory().newNode(String.valueOf(c.getID()));
            //n0.getNodeData().setLabel( + c.toString());
            n0.setX((float) c.getMode().getVector()[0]);
            n0.setY((float) c.getMode().getVector()[1]);
            n0.setSize((float) Math.max(1, Math.pow(c.getAvgSize(), 0.33)));
            String label = "id" + c.getID() + (c.getComment().trim().length() > 0 ? (": " + c.getComment()) : "");
            n0.setLabel(label);//c.toString());
            n0.setAttribute("hd-label", label);
            n0.setAttribute("cluster", c.getID());
            graph.addNode(n0);
            nodes[idx++] = n0;
        }

        Datapoint[] clusterCentroids = new Datapoint[nodes.length];

        for (int i = 0; i < clusterCentroids.length; i++) {
            clusterCentroids[i] = new Datapoint("avg" + clusters[i].toString(), clusters[i].getMode().getVector(), clusters[i].getMode().getSideVector(), i);
        }

        //final DistanceMeasure dm = clusters[0].getClusterSet().getDistanceMeasure();
        List<Edge> edges = new ArrayList<>();

        double maxSim = 0;

        for (int i = 0; i < clusterCentroids.length; i++) {
            for (int j = i + 1; j < clusterCentroids.length; j++) {
                if (true) {// nnOfMid.equals(clusterCentroids[i]) || nnOfMid.equals(clusterCentroids[j])) {
                    Double similarity = dm.getSimilarity(clusters[i].getMode().getVector(), clusters[j].getMode().getVector());

                    if (Double.isInfinite(similarity) || Double.isNaN(similarity)) {
                        logger.print(clusters[i].getMode().getVector());
                        logger.print(clusters[j].getMode().getVector());
                    }
                    maxSim = Math.max(maxSim, similarity);
                    Edge e = graph.getModel().factory().newEdge(nodes[i], nodes[j], 0, false);
                    e.setWeight(similarity);
                    edges.add(e);
                    graph.addEdge(e);
                }
            }
        }

        Edge[] e = edges.toArray(new Edge[edges.size()]);
        for (int i = 0; i < e.length; i++) {
            e[i].setWeight((EDGE_SCALING * e[i].getWeight()) / maxSim);
        }

        Arrays.sort(e, new Comparator<Edge>() {
            @Override
            public int compare(Edge o1, Edge o2) {
                return (int) Math.signum(o1.getWeight() - o2.getWeight());
            }
        });

        for (int i = 0; i < e.length; i++) {
            Double w = e[i].getWeight();
            graph.removeEdge(e[i]);
            ConnectedComponents cc = new ConnectedComponents();
            cc.execute(graph.getModel());
            if (cc.getConnectedComponentsCount() > 1) {
                e[i] = graph.getModel().factory().newEdge(e[i].getSource(), e[i].getTarget(), 0, false);
                e[i].setWeight(w * EDGE_SCALING);
                graph.addEdge(e[i]);
                logger.print("keeping edge:" + e[i].getTarget() + "<->" + e[i].getSource() + "" + e[i].getWeight());
            }
        }

        for (Edge ed : graph.getEdges().toArray()) {
            logger.print(ed.getWeight());
        }

    }

    private void buildDMTgraph(Cluster[] clusters) {
        try {
            for (Cluster c : clusters) {
                cid.put(c.getID(), c);
            }

            graph = graphModel.getDirectedGraph();

            HashMap<ClusterTreeNode, Node> hmNodes = new HashMap<>();

            ClusterTreeNode root = (new ClusterPhylogeny()).getDivisiveMarkerTree(clusters);

            Enumeration<HierarchicalCentroid> enu = root.breadthFirstEnumeration();

            graph.getModel().getNodeTable().addColumn("cluster", Integer.class);
            graph.getModel().getNodeTable().addColumn("logRatio", Double.class);
            graph.getModel().getNodeTable().addColumn("hd-label", String.class);

            while (enu.hasMoreElements()) {
                ClusterTreeNode cn = (ClusterTreeNode) enu.nextElement();
                Node n0 = cn.isRoot() ? graph.getModel().factory().newNode("root") : graph.getModel().factory().newNode();
                if (cn.getUserObject().length == 1) {
                    Cluster c = cn.getUserObject()[0].getCluster();

                    n0.setX((float) c.getMode().getVector()[0]);
                    n0.setY((float) c.getMode().getVector()[1]);
                    n0.setSize((float) Math.max(5, Math.pow(c.getAvgSize(), 0.33)));
                    String label = cn.getLabel() + " | id" + c.getID() + (c.getComment().trim().length() > 0 ? (": " + c.getComment()) : "");
                    n0.setLabel(label);//c.toString());
                    n0.setAttribute("hd-label", label);
                    n0.setAttribute("cluster", c.getID());
                } else {
                    int size = 0;
                    for (ClusterPhylogeny.ClusterDatapoint cluster : cn.getUserObject()) {
                        size += cluster.c.getAvgSize();
                    }
                    n0.setX((float) cn.getClusterMembers()[0].getDatapoint().getVector()[0]);
                    n0.setY((float) cn.getClusterMembers()[0].getDatapoint().getVector()[1]);
                    n0.setSize((float) Math.pow(size, 0.33));
                    String label = cn.getLabel();
                    n0.setLabel(label);
                    n0.setAttribute("hd-label", label);

                    n0.setAttribute("cluster", -1);
                    n0.setR(0.7f);
                    n0.setG(0.7f);
                    n0.setB(0.7f);
                    n0.setAlpha(0.3f);

                }
                hmNodes.put(cn, n0);
                graph.addNode(n0);
            }
            enu = root.breadthFirstEnumeration();

            while (enu.hasMoreElements()) {
                ClusterTreeNode cn = (ClusterTreeNode) enu.nextElement();
                Node currNode = hmNodes.get(cn);
                for (int i = 0; i < cn.getChildCount(); i++) {
                    Node childNode = hmNodes.get(cn.getChildAt(i));
                    graph.addEdge(graph.getModel().factory().newEdge(currNode, childNode, 0, true));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
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

        buttonGroup1 = new javax.swing.ButtonGroup();
        jSplitPane1 = new javax.swing.JSplitPane();
        panGraphContainer = new javax.swing.JPanel();
        panTop = new javax.swing.JPanel();
        jButton3 = new javax.swing.JButton();
        spinScalingRatio = new javax.swing.JSpinner();
        chkAdjustSizes = new javax.swing.JCheckBox();
        togFlip = new javax.swing.JToggleButton();
        jButton2 = new javax.swing.JButton();
        spinRescale = new javax.swing.JSpinner();
        panBottom = new javax.swing.JPanel();
        cmdExport = new javax.swing.JButton();
        chkCreateStacks = new javax.swing.JCheckBox();
        spinImgPerRow1 = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
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
        panColorControls = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        spinZeroCutoff = new javax.swing.JSpinner();
        rbQuantile = new javax.swing.JRadioButton();
        rbLinear = new javax.swing.JRadioButton();
        jLabel4 = new javax.swing.JLabel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jLabel8 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jSplitPane1.setDividerLocation(1000);
        jSplitPane1.setResizeWeight(1.0);
        jSplitPane1.setLastDividerLocation(2000);
        jSplitPane1.setMinimumSize(new java.awt.Dimension(300, 158));
        jSplitPane1.setPreferredSize(new java.awt.Dimension(1000, 158));

        panGraphContainer.setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(jButton3, "re-run layout");
        jButton3.setMaximumSize(new java.awt.Dimension(170, 25));
        jButton3.setMinimumSize(new java.awt.Dimension(170, 25));
        jButton3.setPreferredSize(new java.awt.Dimension(170, 25));
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        panTop.add(jButton3);

        spinScalingRatio.setModel(new javax.swing.SpinnerNumberModel(5.0d, 0.0d, null, 1.0d));
        spinScalingRatio.setMinimumSize(new java.awt.Dimension(61, 20));
        spinScalingRatio.setPreferredSize(new java.awt.Dimension(61, 25));
        panTop.add(spinScalingRatio);

        org.openide.awt.Mnemonics.setLocalizedText(chkAdjustSizes, "Prevent Node Overlap");
        panTop.add(chkAdjustSizes);

        org.openide.awt.Mnemonics.setLocalizedText(togFlip, "rotate by an angle...");
        togFlip.setMaximumSize(new java.awt.Dimension(170, 25));
        togFlip.setMinimumSize(new java.awt.Dimension(170, 25));
        togFlip.setPreferredSize(new java.awt.Dimension(170, 25));
        togFlip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                togFlipActionPerformed(evt);
            }
        });
        panTop.add(togFlip);

        org.openide.awt.Mnemonics.setLocalizedText(jButton2, "mult node coord. by:");
        jButton2.setMaximumSize(new java.awt.Dimension(170, 25));
        jButton2.setMinimumSize(new java.awt.Dimension(170, 25));
        jButton2.setPreferredSize(new java.awt.Dimension(170, 25));
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        panTop.add(jButton2);

        spinRescale.setModel(new javax.swing.SpinnerNumberModel(5.0d, 0.0d, null, 1.0d));
        spinRescale.setMinimumSize(new java.awt.Dimension(61, 20));
        spinRescale.setPreferredSize(new java.awt.Dimension(61, 25));
        panTop.add(spinRescale);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        panGraphContainer.add(panTop, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(cmdExport, "Export images");
        cmdExport.setMaximumSize(new java.awt.Dimension(170, 25));
        cmdExport.setMinimumSize(new java.awt.Dimension(170, 25));
        cmdExport.setPreferredSize(new java.awt.Dimension(170, 25));
        cmdExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdExportActionPerformed(evt);
            }
        });
        panBottom.add(cmdExport);

        chkCreateStacks.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(chkCreateStacks, "Create Image Stacks");
        panBottom.add(chkCreateStacks);

        spinImgPerRow1.setModel(new javax.swing.SpinnerNumberModel(5, 1, null, 1));
        spinImgPerRow1.setPreferredSize(new java.awt.Dimension(31, 25));
        panBottom.add(spinImgPerRow1);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, "per row");
        panBottom.add(jLabel5);

        org.openide.awt.Mnemonics.setLocalizedText(jButton1, "Save as PDF");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        panBottom.add(jButton1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        panGraphContainer.add(panBottom, gridBagConstraints);

        jSplitPane1.setLeftComponent(panGraphContainer);

        jPanel4.setPreferredSize(new java.awt.Dimension(200, 610));
        jPanel4.setLayout(new java.awt.GridBagLayout());

        jPanel5.setPreferredSize(new java.awt.Dimension(200, 400));
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

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, "Break down by annotation:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel5.add(jLabel1, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel7.setMinimumSize(new java.awt.Dimension(200, 80));
        jPanel7.setOpaque(false);
        jPanel7.setPreferredSize(new java.awt.Dimension(200, 80));
        jPanel7.setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, "<html>Grouping of annotation terms (toggle by clicking on grey bars, right-click selects one)</html>");
        jLabel2.setMaximumSize(new java.awt.Dimension(0, 0));
        jLabel2.setMinimumSize(new java.awt.Dimension(48, 32));
        jLabel2.setPreferredSize(new java.awt.Dimension(48, 32));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 8);
        jPanel7.add(jLabel2, gridBagConstraints);

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/azure 16x16.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, "Group of interest");
        jLabel3.setToolTipText("This variable will be imported as a part of the main dataset. It will be taken into account for clustering and scoring.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 6, 6, 6);
        jPanel7.add(jLabel3, gridBagConstraints);

        jLabel6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/grey 16x16.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel6, "None");
        jLabel6.setToolTipText("This column will be skipped");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(7, 7, 7, 7);
        jPanel7.add(jLabel6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(jPanel7, gridBagConstraints);

        scpColumns.setPreferredSize(new java.awt.Dimension(200, 402));

        tabAnnTerms.setModel(new DefaultTableModel(new String[]{"Group","Term"}, 0)
        );
        scpColumns.setViewportView(tabAnnTerms);

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
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        jPanel4.add(jPanel5, gridBagConstraints);

        jPanel6.setPreferredSize(new java.awt.Dimension(200, 130));
        jPanel6.setLayout(new java.awt.BorderLayout());

        spParamSelectionContainer.setPreferredSize(new java.awt.Dimension(200, 130));

        lstParam.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
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
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        jPanel4.add(jPanel6, gridBagConstraints);

        panColorControls.setMinimumSize(new java.awt.Dimension(200, 80));
        panColorControls.setPreferredSize(new java.awt.Dimension(200, 80));
        panColorControls.setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(jLabel7, "Max value (auto): 99th percentile");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 6, 0);
        panColorControls.add(jLabel7, gridBagConstraints);

        spinZeroCutoff.setModel(new javax.swing.SpinnerNumberModel(0.19d, null, null, 0.1d));
        spinZeroCutoff.setMinimumSize(new java.awt.Dimension(61, 20));
        spinZeroCutoff.setPreferredSize(new java.awt.Dimension(61, 25));
        spinZeroCutoff.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinZeroCutoffStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        panColorControls.add(spinZeroCutoff, gridBagConstraints);

        buttonGroup1.add(rbQuantile);
        rbQuantile.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(rbQuantile, "Quantile");
        rbQuantile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbQuantileActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        panColorControls.add(rbQuantile, gridBagConstraints);

        buttonGroup1.add(rbLinear);
        org.openide.awt.Mnemonics.setLocalizedText(rbLinear, "Linear");
        rbLinear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbLinearActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        panColorControls.add(rbLinear, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, "Color scale:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        panColorControls.add(jLabel4, gridBagConstraints);

        jCheckBox1.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(jCheckBox1, "Show cluster labels");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 0);
        panColorControls.add(jCheckBox1, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel8, "min value:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 6, 5);
        panColorControls.add(jLabel8, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel4.add(panColorControls, gridBagConstraints);

        jSplitPane1.setRightComponent(jPanel4);

        getContentPane().add(jSplitPane1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void lstParamValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstParamValueChanged
        try {
            if (evt != null) {
                if (!evt.getValueIsAdjusting()) {
                    colorNodesByExpressionLevel(lstParam.getSelectedIndex());
                }
            }

            scaleImg = colorScale.generateScaleImage(lstParam.getSelectedIndex());
            previewSketch.setScaleImg(scaleImg);
            previewController.render(target);
            //target.reset();
            target.refresh();
            previewController.refreshPreview();
            previewSketch.repaint();
            previewController.render(target);
            // target.reset();
            target.refresh();
            previewController.refreshPreview();
            previewSketch.repaint();
        } catch (NullPointerException e) {
            logger.print(e);
        }
    }//GEN-LAST:event_lstParamValueChanged

    public static boolean textOn = true;

    private void cmdExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdExportActionPerformed
        boolean createStack = chkCreateStacks.isSelected() && tabAnnTerms.getModel().getRowCount() > 0;
        int imgPerRow = ((Integer) spinImgPerRow1.getValue());
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
        File file = IO.chooseFileWithDialog("frmDMTexport", "Directories", null, true);
        if (file == null) {
            return;
        }
        String path = file.getPath();
        int w = previewSketch.getWidth();
        int h = previewSketch.getHeight();
        try {
            for (int i = 0; i < paramList.length; i++) {
                BufferedImage stack = (createStack) ? new BufferedImage(w * imgPerRow, h * rows, BufferedImage.TYPE_INT_RGB) : null;
                int curRow = 0;
                int curCol = 0;
                for (int j = 0; j < tabAnnTerms.getModel().getRowCount(); j++) {
                    adjustSizesByAnnotationTerm(currAnn, new String[]{(String) tabAnnTerms.getValueAt(j, 1)}, new String[0], false);
                    lstParam.setSelectedIndex(i);
                    RenderedImage img = (RenderedImage) target.getImage();

                    if (createStack) {

                        Graphics2D g2 = ((Graphics2D) stack.getGraphics());
                        g2.drawImage((Image) img, curCol * w, curRow * h, null);
                        g2.setStroke(new BasicStroke(3.0f));
                        g2.drawRect(5, 0, w, h);
                        stack.getGraphics().setClip(curCol * w, curRow * h, w, h);

                        g2.setFont(new java.awt.Font("Arial", Font.BOLD, 30));
                        g2.setColor(new Color(0, 0, 0, 127));
                        g2.drawString(currAnnTerms[0], (curCol * w) + 5, (curRow * h) + 35);

                        curCol++;
                        if (curCol == imgPerRow) {
                            curCol = 0;
                            curRow++;
                        }
                    } else {
                        File f = new File(path + File.separator + "_" + panScFDL.getFileSafeString(currParam) + "_" + currAnnTerms[0] + ".png");
                        f.getParentFile().mkdirs();
                        ImageIO.write(img, "PNG", f);
                    }
                }
                if (createStack) {
                    File f2 = new File(path + File.separator + "stack_" + ds.getName() + "_" + panScFDL.getFileSafeString(currParam) + ".png");
                    f2.getParentFile().mkdirs();
                    ImageIO.write(stack, "PNG", f2);
                }
            }

            if (tabAnnTerms.getModel().getRowCount() == 0) {
                for (int i = 0; i < paramList.length; i++) {
                    lstParam.setSelectedIndex(i);
                    RenderedImage img = (RenderedImage) target.getImage();
                    File f = new File(path + File.separator + ds.getName() + "_" + panScFDL.getFileSafeString(currParam) + ".png");
                    f.getParentFile().mkdirs();
                    ImageIO.write(img, "PNG", f);
                }
            }

            for (int i = 0; i < paramList.length; i++) {
                BufferedImage bi = colorScale.generateScaleImage(i);
                File f = new File(path + File.separator + ds.getName() + "_scaleFor" + "_" + panScFDL.getFileSafeString((String) lstParam.getModel().getElementAt(i)) + ".png");
                f.getParentFile().mkdirs();
                ImageIO.write(bi, "PNG", f);
            }

        } catch (IOException ex) {
            logger.showException(ex);
        }
    }//GEN-LAST:event_cmdExportActionPerformed

    private void lstParamMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lstParamMouseClicked
        if (evt.getButton() == MouseEvent.BUTTON3) {
            if (fixedParamVal != lstParam.getSelectedIndex()) {
                fixedParamVal = lstParam.getSelectedIndex();
            } else {
                fixedParamVal = -1;
            }
            lstParamValueChanged(new ListSelectionEvent(this, lstParam.getSelectedIndex(), lstParam.getSelectedIndex(), false));
        }
    }//GEN-LAST:event_lstParamMouseClicked

    private void cmbAnnotationsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbAnnotationsActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cmbAnnotationsActionPerformed

    private void togFlipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_togFlipActionPerformed
        ArrayList<Node> alN = new ArrayList<>();

        for (Node n : graph.getNodes()) {
            alN.add(n);
        }

        DenseDoubleMatrix2D coord = new DenseDoubleMatrix2D(alN.size(), 2);

        for (int i = 0; i < alN.size(); i++) {
            Node n = alN.get(i);
            coord.set(i, 0, n.x());
            coord.set(i, 1, n.y());
        }

        JSpinner jsp = new JSpinner(new SpinnerNumberModel(0, 0, 360, 1));

        JOptionPane.showMessageDialog(this, jsp, "Rotation angle in degrees", JOptionPane.QUESTION_MESSAGE);

        double theta = ((Integer) jsp.getValue() / 360.0) * (2 * Math.PI);

        DenseDoubleMatrix2D rot = new DenseDoubleMatrix2D(2, 2);

        rot.set(0, 0, Math.cos(theta));
        rot.set(0, 1, -Math.sin(theta));
        rot.set(1, 0, Math.sin(theta));
        rot.set(1, 1, Math.cos(theta));

        DoubleMatrix2D res = Algebra.DEFAULT.mult(coord, rot);

        for (int i = 0; i < alN.size(); i++) {
            Node n = alN.get(i);
            n.setX((float) res.get(i, 0));
            n.setY((float) res.get(i, 1));
        }
        previewSketch.resetZoom();
        lstParamValueChanged(null);
    }//GEN-LAST:event_togFlipActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        for (Node n : graph.getNodes()) {
            double f = (Double) spinRescale.getValue();
            n.setX((float) (n.x() * f));
            n.setY((float) (n.y() * f));
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        layout.setAdjustSizes(chkAdjustSizes.isSelected());
        layout.setBarnesHutOptimize(true);
        layout.setScalingRatio((Double) spinScalingRatio.getValue());
        for (int i = 0; i < 1000 && layout.canAlgo(); i++) {
            layout.goAlgo();
        }
        previewSketch.resetZoom();
        lstParamValueChanged(null);
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed

        Node[] n = graph.getNodes().toArray();

        if (jCheckBox1.isSelected()) {
            for (Node node : n) {
                node.setLabel((String) node.getAttribute("hd-label"));
            }
        } else {
            for (Node node : n) {
                node.setLabel("");
            }
        }

        lstParamValueChanged(null);
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void spinZeroCutoffStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinZeroCutoffStateChanged
        COLOR_MAP_NOISE_THRESHOLD = (Double) spinZeroCutoff.getValue();
        lstParamValueChanged(new ListSelectionEvent(this, lstParam.getSelectedIndex(), lstParam.getSelectedIndex(), false));
        lstParamValueChanged(null);
    }//GEN-LAST:event_spinZeroCutoffStateChanged

    private void rbLinearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbLinearActionPerformed
        colorScale.setMode(ColorScale.ScalingMode.LINEAR);
        lstParamValueChanged(new ListSelectionEvent(this, lstParam.getSelectedIndex(), lstParam.getSelectedIndex(), false));
    }//GEN-LAST:event_rbLinearActionPerformed

    private void rbQuantileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbQuantileActionPerformed
        colorScale.setMode(ColorScale.ScalingMode.QUANTILE);
        lstParamValueChanged(new ListSelectionEvent(this, lstParam.getSelectedIndex(), lstParam.getSelectedIndex(), false));
    }//GEN-LAST:event_rbQuantileActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        try {
            File f = IO.chooseFileWithDialog("frmGraph", "Portable Document Format (PDF)", new String[]{"pdf", "PDF"}, true);
            if (f.isDirectory()) {
                f = new File(f.getAbsolutePath() + File.separator + currParam + ".pdf");
            }
            if (!f.getName().toLowerCase().endsWith(".pdf")) {
                f = new File(f.getAbsolutePath() + ".pdf");
            }
            //f.mkdirs();
            ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
            ec.exportFile(f, pc.getCurrentWorkspace());
        } catch (IOException e) {
            logger.showException(e);
        }
    }//GEN-LAST:event_jButton1ActionPerformed
    /**
     * @param args the command line arguments
     */
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JCheckBox chkAdjustSizes;
    private javax.swing.JCheckBox chkCreateStacks;
    private samusik.glasscmp.GlassComboBox cmbAnnotations;
    private javax.swing.JButton cmdExport;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JList lstParam;
    private javax.swing.JPanel panBottom;
    private javax.swing.JPanel panColorControls;
    private javax.swing.JPanel panGraphContainer;
    private javax.swing.JPanel panTop;
    private javax.swing.JRadioButton rbLinear;
    private javax.swing.JRadioButton rbQuantile;
    private javax.swing.JScrollPane scpColumns;
    private javax.swing.JScrollPane spParamSelectionContainer;
    private javax.swing.JSpinner spinImgPerRow1;
    private javax.swing.JSpinner spinRescale;
    private javax.swing.JSpinner spinScalingRatio;
    private javax.swing.JSpinner spinZeroCutoff;
    private javax.swing.JTable tabAnnTerms;
    private javax.swing.JToggleButton togFlip;
    // End of variables declaration//GEN-END:variables

    private GroupingWorker groupingWorker;

    private class GroupingWorker extends SwingWorker<Object, Object> {

        private final Annotation ann;
        private final String[] termsAssay;
        private final String[] termsControl;

        public GroupingWorker(Annotation ann, String[] termsAssay, String[] termsControl) {
            this.ann = ann;
            this.termsAssay = termsAssay;
            this.termsControl = termsControl;
        }

        @Override
        protected void process(List<Object> chunks) {
            if (chunks.get(0).equals("no groups")) {
                //  legend.setText("<html>Node fill is showing <b>expression levels (rainbow scale)</b><br>Node border is showing <b>nothing</b></html>");
            }
            if (chunks.get(0).equals("one group")) {
                // legend.setText("<html>Node fill is showing <b>expression levels (rainbow scale)</b><br>Node border is showing <b>nothing</b></html>");
            }
            lstParamValueChanged(null);
        }

        @Override
        public Object doInBackground() throws Exception {
            logger.print("Grouping worker started");
            if (ann == null || termsAssay == null) {
                NodeIterable ni = graph.getNodes();
                for (final Node n : ni.toArray()) {
                    Cluster cl = cid.get((int) n.getAttribute("cluster"));
                    n.setSize((float) Math.pow(cl.size(), 0.33));
                    n.setAttribute("logRatio", 0.5);
                }
                currAnnTerms = null;
                expArrayLists = new HashMap<>();
                publish("no groups");
                return null;
            }

            ArrayList<String> dpIDsAssayAL = new ArrayList<>();

            for (String term : termsAssay) {
                int[] pid = ann.getDpIDsForTerm(term);
                // int[] dpids = new int[pid.length];
                for (int i = 0; i < pid.length; i++) {
                    Datapoint dp = ann.getBaseDataset().getDPByID(pid[i]);
                    if (dp != null) {
                        dpIDsAssayAL.add(dp.getFullName());
                    }
                }
            }
            Collections.sort(dpIDsAssayAL);

            String[] dpIDsAssay = new String[dpIDsAssayAL.size()];
            for (int i = 0; i < dpIDsAssay.length; i++) {
                dpIDsAssay[i] = dpIDsAssayAL.get(i);
            }

            String[] dpIDsControl = null;
            if (termsControl != null) {
                ArrayList<String> dpIDsControlAL = new ArrayList<>();

                for (String term : termsControl) {
                    int[] pid = ann.getDpIDsForTerm(term);
                    // int[] dpids = new int[pid.length];
                    for (int i = 0; i < pid.length; i++) {
                        Datapoint dp = ann.getBaseDataset().getDPByID(pid[i]);
                        if (dp != null) {
                            dpIDsControlAL.add(dp.getFullName());
                        }
                    }
                }
                Collections.sort(dpIDsControlAL);

                dpIDsControl = new String[dpIDsControlAL.size()];
                for (int i = 0; i < dpIDsControl.length; i++) {
                    dpIDsControl[i] = dpIDsControlAL.get(i);
                }
            }

            NodeIterable ni = graph.getNodes();
            for (final Node n : ni.toArray()) {
                Cluster cl = cid.get((int) n.getAttribute("cluster"));
                if (cl == null) {
                    continue;
                }
                int count = 0;
                int countControl = 0;

                double[] avgVec = new double[ann.getBaseDataset().getFeatureNamesCombined().length];
                for (ClusterMember cm : cl.getClusterMembers()) {
                    if (Arrays.binarySearch(dpIDsAssay, cm.getDatapoint().getFullName()) >= 0) {
                        count++;
                        avgVec = MatrixOp.sum(avgVec, MatrixOp.concat(cm.getDatapoint().getVector(), cm.getDatapoint().getSideVector()));
                    }
                }
                MatrixOp.mult(avgVec, 1.0 / (double) count);

                //Arrays.fill(avgVec, count/(double)cl.size());
                expArrayLists.put(cl, avgVec);

                if (termsControl != null && termsControl.length > 0) {
                    double[] avgVecControl = new double[ann.getBaseDataset().getFeatureNamesCombined().length];
                    for (ClusterMember cm : cl.getClusterMembers()) {
                        if (Arrays.binarySearch(dpIDsControl, cm.getDatapoint().getFullName()) >= 0) {
                            countControl++;
                            avgVecControl = MatrixOp.sum(avgVecControl, MatrixOp.concat(cm.getDatapoint().getVector(), cm.getDatapoint().getSideVector()));
                        }
                    }
                    if (countControl > 1) {
                        MatrixOp.mult(avgVecControl, 1.0 / (double) countControl);

                    }

                    double[] sErrArrayList = new double[ann.getBaseDataset().getFeatureNamesCombined().length];

                    for (ClusterMember cm : cl.getClusterMembers()) {
                        if (Arrays.binarySearch(dpIDsControl, cm.getDatapoint().getFullName()) >= 0) {
                            double[] vec = MatrixOp.concat(cm.getDatapoint().getVector(), cm.getDatapoint().getSideVector());

                            for (int i = 0; i < vec.length; i++) {
                                sErrArrayList[i] += Math.pow(vec[i] - avgVecControl[i], 2);
                            }
                        }
                        if (Arrays.binarySearch(dpIDsAssay, cm.getDatapoint().getFullName()) >= 0) {
                            double[] vec = MatrixOp.concat(cm.getDatapoint().getVector(), cm.getDatapoint().getSideVector());

                            for (int i = 0; i < vec.length; i++) {
                                sErrArrayList[i] += Math.pow(vec[i] - avgVec[i], 2);
                            }
                        }
                    }

                    if (count + countControl > 2) {
                        for (int i = 0; i < sErrArrayList.length; i++) {
                            sErrArrayList[i] = Math.sqrt(sErrArrayList[i] / (count + countControl));
                        }
                        System.out.println(cl.toString());
                        double[] pValArrayList = new double[ann.getBaseDataset().getFeatureNamesCombined().length];
                        for (int i = 0; i < pValArrayList.length; i++) {
                            if (sErrArrayList[i] > 0) {
                                NormalDist dist = new NormalDist(avgVecControl[i], sErrArrayList[i]);
                                pValArrayList[i] = Math.abs(dist.cdf(avgVec[i]) - 0.5) * 2;
                            } else {
                                pValArrayList[i] = 0.5;
                            }
                        }
                        pValArrayLists.put(cl, pValArrayList);
                    } else {
                        double[] pValArrayList = new double[ann.getBaseDataset().getFeatureNamesCombined().length];
                        pValArrayLists.put(cl, pValArrayList);
                    }
                } else {
                    pValArrayLists.remove(cl);
                }

                n.setSize(Math.max(1, (float) Math.pow(count * ((ds.size()) / (double) dpIDsAssay.length), 0.33)));
                if (termsAssay.length > 0) {
                    if (termsControl != null && termsControl.length > 0) {
                        double ratio = countControl == 0 ? 1 : ((double) count / (double) countControl);
                        if (dpIDsControl.length > 0) {
                            ratio /= (dpIDsAssay.length / (double) dpIDsControl.length);
                        }
                        double logRatio = 0.5 + (Math.log(ratio) / 4);
                        //logger.print("logRatio" + ratio);
                        n.setAttribute("logRatio", 0.5);//new Double(Math.max(0, Math.min(1, logRatio))));
                        publish("two groups");
                    } else {
                        double ratio = 1 - HypergeometricDist.pValHyperGeom(ds.size(), dpIDsAssay.length, cl.size(), count);
                        n.setAttribute("logRatio", ratio);
                        publish("one group");
                    }
                } else {
                    n.setAttribute("logRatio", 0.5);
                    publish("no groups");
                }
                //logger.print(n.getAttributes().getVgalue("logRatio"));
            }
            currAnnTerms = termsAssay;
            publish("done");
            lstParamValueChanged(new ListSelectionEvent(this, lstParam.getSelectedIndex(), lstParam.getSelectedIndex(), false));
            logger.print("Grouping worker ended");
            return null;
        }
    }

}
