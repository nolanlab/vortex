/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustergraph;

import annotations.Annotation;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import clustering.Cluster;
import clustering.ClusterMember;
import clustering.Datapoint;
import clustering.Dataset;
import clustering.DistanceMeasure;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeIterable;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2Builder;
import org.gephi.project.api.Project;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.hsqldb.util.CSVWriter;
import org.openide.util.Lookup;
import util.DefaultEntry;
import util.IO;
import util.MatrixOp;
import util.QuantileMap;
import util.ColorScale;
import util.Shuffle;
import util.logger;
import vortex.gui.WrapLayout;
import vortex.util.ProfilePCA;

/**
 *
 * @author Nikolay Samusik
 */
public class panScFDL extends javax.swing.JPanel implements PropertyChangeListener {

    private final FDLGraphRenderer graphRenderer;
    ArrayList<Edge> edges = new ArrayList<>();
    private double[][] scalingBounds;
    private UndirectedGraph graph;
    private String[] paramList;
    private HashMap<Integer, ClusterNode> cid = new HashMap<>();
    private HashMap<ClusterNode, double[]> expArrayLists = new HashMap<>();
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
    private ColorScale colorScale;

    private Node[][] sortedNodes;

    private QuantileMap qm, sqm;

    final ForceAtlas2 layout = new ForceAtlas2(new ForceAtlas2Builder());
    boolean layoutOn = false;
    SwingWorker sw = null;

    public double getCOLOR_MAP_NOISE_THRESHOLD() {
        return COLOR_MAP_NOISE_THRESHOLD;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        graphRenderer.repaint();
    }

    public void setCOLOR_MAP_NOISE_THRESHOLD(double COLOR_MAP_NOISE_THRESHOLD) {
        this.COLOR_MAP_NOISE_THRESHOLD = COLOR_MAP_NOISE_THRESHOLD;
    }

    private void colorNodesByExpressionLevel(int paramIDX) {
        if (paramIDX < 0) {
            return;
        }
        curParamIDX = paramIDX;

        graphRenderer.setColoringMode(FDLGraphRenderer.ColoringMode.EXPRESSION);

        this.colorScale.setMinValue(paramIDX, COLOR_MAP_NOISE_THRESHOLD);

        this.colorScale.setMode(rbLinear.isSelected() ? ColorScale.ScalingMode.LINEAR : ColorScale.ScalingMode.QUANTILE);
        this.graphRenderer.setColoringParamIDX(paramIDX);

        NodeIterable ni = graph.getNodes();
        for (final Node n : ni.toArray()) {
            if (n.getAttribute("cluster") == null) {
                continue;
            }
            ClusterNode cl = cid.get((int) n.getAttribute("clusterNode"));
            if (expArrayLists.get(cl) == null) {
                expArrayLists.put(cl, MatrixOp.concat(cl.dp.getVector(), cl.dp.getSideVector()));
            }
            double[] vec = expArrayLists.get(cl);

            double scaledVal = vec[paramIDX];
            try {
                n.setAttribute("expValue", scaledVal);
            } catch (Exception e) {
                logger.showException(e);
                logger.print((int) (vec[fixedParamVal] * 255));
                logger.print((int) (scaledVal * 255));
            }
        }
        curParamIDX = paramIDX;
        currParam = paramList[paramIDX];
        scaleImg = colorScale.generateScaleImage(paramIDX);
        graphRenderer.setScaleImg(scaleImg);
    }

    private void adjustSizesByAnnotationTerm(Annotation ann, String[] termsAssay) {
        if (termsAssay == null) {
            return;
        }

        String[] terms = ann.getTerms();
        NodeIterable ni = graph.getNodes();
        final Node[] nds = ni.toArray();
        for (Node n : nds) {
            n.setAttribute("groupID", -1);
            n.setAttribute("selected", false);
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
            ClusterNode cl = cid.get((int) n.getAttribute("clusterNode"));
            if (termAssignment[cl.dp.getID()] != null) {
                int idx = Arrays.binarySearch(terms, termAssignment[cl.dp.getID()]);
                n.setAttribute("groupID", idx);
                n.setAttribute("selected", idx >= 0);
            }
        }

        lstParamValueChanged(new ListSelectionEvent(lstParam, lstParam.getSelectedIndex(), lstParam.getSelectedIndex(), false));
        currAnnTerms = termsAssay;
        panGraphContainer.repaint();
    }

    public class ClusterNode {

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
    int MinClusID;

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
                tabModel.setValueAt(0, 0, 0);

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

                tabAnnTerms.setModel(tabModel);
                tabAnnTerms.invalidate();
                tabAnnTerms.validate();
                tabAnnTerms.repaint();

                adjustSizesByAnnotationTerm(currAnn, new String[]{tabAnnTerms.getValueAt(0, 1).toString()});
                panGraph.validate();
                panGraph.repaint();
            } else {
                tabAnnTerms.setModel(new DefaultTableModel(new String[]{"Group", "Term"}, 0));
                adjustSizesByAnnotationTerm(null, null);

            }
        }
    };

    private boolean abort = false;

    private FDLParamPane fdlParamPane;
    
    private static panScFDL Instance;

    public static panScFDL getInstance() {
        return Instance;
    }
    
    

    public panScFDL(Cluster[] clusters) {
        initComponents();
        
        
        if(frmGraph.getInstance()!=null){
            
            frmGraph.getInstance().setVisible(false);
            frmGraph.getInstance().dispose();
        }
        
        Instance = this;
        
        panTop.setLayout(new WrapLayout(WrapLayout.LEADING));
        panBottom.setLayout(new WrapLayout(WrapLayout.LEADING));
        fdlParamPane = new FDLParamPane(clusters[0].getClusterSet().getDataset());
        if (JOptionPane.showConfirmDialog(this, fdlParamPane, "FDL parameters", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            this.abort = true;
            this.setVisible(false);
        }

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        panGraph.setLayout(new GridBagLayout());

        ds = clusters[0].getClusterSet().getDataset();

        qm = QuantileMap.getQuantileMap(ds);
        sqm = QuantileMap.getQuantileMapForSideParam(ds);

        colorScale = new ColorScale(100, ds, COLOR_MAP_NOISE_THRESHOLD, qm, sqm, ColorScale.ScalingMode.QUANTILE);

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

        String[] subsampleAnnTerms = fdlParamPane.getSelectedAnnTerms();
        Annotation ann = fdlParamPane.getSelectedAnnotation();

        boolean[] selectedDPIDs = new boolean[ds.getDatapoints().length];
        Arrays.fill(selectedDPIDs, true);
        if (ann != null) {
            Arrays.fill(selectedDPIDs, false);
            for (String term : subsampleAnnTerms) {
                for (int idx : ann.getDpIDsForTerm(term)) {
                    selectedDPIDs[idx] = true;
                }
            }
        }

        try {
            NUM_NODES = fdlParamPane.getNumNodes();
            KNN = fdlParamPane.getKNN();

            //JOptionPane.showMessageDialog(this, Arrays.toString(subsampleAnnTerms));
            // clust_influence = Double.parseDouble(JOptionPane.showInputDialog("Cluster influence (how much more weight should get the intra-cluster connections):", "1");
        } catch (NumberFormatException e) {
            logger.showException(new IllegalArgumentException("Invalid number format"));

            this.setVisible(false);
            MinClusID = 0;
            graphRenderer = null;
            return;
        } catch (NullPointerException e) {
            logger.showException(e);
            MinClusID = 0;
            graphRenderer = null;
            return;
        }

        DefaultListModel<String> lm = new DefaultListModel<>();

        // NDataset ds = clusters[0].getClusterSet().getDataset();
        scalingBounds = new double[ds.getFeatureNamesCombined().length][];
        System.arraycopy(Algebra.DEFAULT.transpose(new DenseDoubleMatrix2D(ds.getSideDataBounds())).toArray(), 0, scalingBounds, ds.getNumDimensions(), ds.getSideVarNames().length);
        System.arraycopy(Algebra.DEFAULT.transpose(new DenseDoubleMatrix2D(ds.getDataBounds())).toArray(), 0, scalingBounds, 0, ds.getNumDimensions());
        
        lm.addElement("Cluster ID");
        lm.addElement("Annotation Term");
        for (String p : ds.getFeatureNamesCombined()) {
            lm.addElement(p);
        }

        paramList = ds.getFeatureNamesCombined();
        lstParam.setModel(lm);

        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        Project p = pc.getCurrentProject();

        final Workspace workspace1 = pc.newWorkspace(p);

        pc.openWorkspace(workspace1);
        //Get a graph model - it exists because we have a workspace
        GraphController gc = Lookup.getDefault().lookup(GraphController.class);
        GraphModel graphModel = gc.getGraphModel(workspace1);
        graph = graphModel.getUndirectedGraph();

        graph.getModel().getNodeTable().addColumn("selected", Boolean.class);
        graph.getModel().getNodeTable().addColumn("clusterNode", Integer.class);
        graph.getModel().getNodeTable().addColumn("groupID", Integer.class);
        graph.getModel().getNodeTable().addColumn("expValue", Double.class);
        graph.getModel().getNodeTable().addColumn("cluster", Integer.class);
        graph.getModel().getNodeTable().addColumn("clusterZeroBased", Integer.class);
        graph.getModel().getNodeTable().addColumn("dpID", Integer.class);

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
            c.addPropertyChangeListener(this);

            int i = 0;
            int localNUMNODES = NUM_NODES;//(int)(Math.pow(c.size(),0.33)*40);
            for (ClusterMember cm : (new Shuffle<ClusterMember>()).shuffleCopyArray(c.getClusterMembers())) {
                if (selectedDPIDs[cm.getDatapoint().getID()]) {
                    if (i++ > localNUMNODES) {
                        break;
                    }
                    dp.add(cm.getDatapoint());
                    alCN.add(new ClusterNode(c, cm.getDatapoint(), 20));
                }
            }

        }

        logger.print("Num Datapoints: " + dp.size());

        Collections.shuffle(alCN);

        final ClusterNode[] cn = Arrays.copyOf(alCN.toArray(new ClusterNode[alCN.size()]), alCN.size());

        graphRenderer = new FDLGraphRenderer(cn, FDLGraphRenderer.ColoringMode.CLUSTER, colorScale);

        double[][] nodePOX = new double[dp.size()][2];
        DenseDoubleMatrix1D[] ddm = ProfilePCA.getPrincipalComponents(dp.toArray(new Datapoint[dp.size()]), false);

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
        graph.clear();
        try {
            for (int i = 0; i < cn.length; i++) {
                ClusterNode c = cn[i];
                cid.put(i, c);
                Node n0 = graphModel.factory().newNode(c.toString() + " " + Math.random());
                //n0.setLabel(s2[0]);
                n0.setX((float) (nodePOX[i][0] * (5000 / (maxX - minX))));
                n0.setY((float) (nodePOX[i][1] * (5000 / (maxY - minY))));
                n0.setSize(10);
                n0.setLabel("");//c.toString());
                n0.setAttribute("clusterNode", i);
                n0.setAttribute("dpID", c.dp.getID());
                n0.setAttribute("selected", true);
                n0.setAttribute("cluster", c.cluster.getID());
                n0.setAttribute("groupID", 1);
                n0.setAttribute("expValue", 1.0);
                n0.setAttribute("clusterZeroBased", c.cluster.getID() - MinClusID);
                nodes[i] = n0;
                graph.addNode(n0);
            }
        } catch (Exception e) {
            logger.showException(e);
        }

        //Append imported data to GraphAPI
        ///importController.process(container, new DefaultProcessor(), workspace);
        //Preview configuration
        panGraph.setDoubleBuffered(true);

        GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.fill = java.awt.GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.ipadx = 0;
        gbc.ipady = 0;
        panGraph.add(graphRenderer, gbc);

        ComponentAdapter ca = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                panGraphContainer.repaint();
            }
        };
        panGraph.addComponentListener(ca);
        this.setVisible(true);
        ca.componentResized(null);
        graphRenderer.setGraph(graph);
        jSplitPane1.setDividerLocation(0.9);
        lstParam.setSelectedIndex(0);

        cmbAnnotations.setMaximumSize(new Dimension(100, 30));
        cmbAnnotations.setPreferredSize(new Dimension(100, 30));
        cmbAnnotations.setMinimumSize(new Dimension(100, 30));

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
                                if (d % 100 == 0) {
                                    logger.print("Computing KNN graph: " + d);
                                }
                                synchronized (jp) {
                                    jp.setValue(d);
                                    jp.repaint();
                                }
                                ArrayList<Map.Entry<Node, Double>> arr = new ArrayList<Map.Entry<Node, Double>>();
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
                                Collections.sort(arr, (Map.Entry<Node, Double> o1, Map.Entry<Node, Double> o2) -> (int) Math.signum(o2.getValue() - o1.getValue()));
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

    private double getDensity(int i, int k) {
        double d = 0;
        panScFDL.ClusterNode cn = cid.get((int) sortedNodes[i][0].getAttribute("clusterNode"));
        int cnt = 0;
        for (int j = 1; j < Math.min(sortedNodes[i].length, k); j++) {
            panScFDL.ClusterNode c1 = cid.get((int) sortedNodes[i][j].getAttribute("clusterNode"));
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

        // Integer eid = 0;
        for (int i = 0; i < nodes.length; i++) {

            //panScFDL.ClusterNode cl = cid.get((int) nodes[i].getAttribute("clusterNode");
            if (sortedNodes[i].length > 0) {
                int nn = fdlParamPane.varyConn() ? Math.max(((k) * hmDens.get(nodes[i])) / nodes.length, 1) + 1 : k;

                int numEdges = Math.min(nn, sortedNodes[i].length);

                System.err.println("dens: " + hmDens.get(nodes[i]) + " numEdges:" + numEdges);

                Edge e1 = null;
                double dns = hmDens.get(nodes[i]);
                //dns = (hmDens.get(sortedNodes[i][j])+dns)*10;
                for (int j = 1; j < numEdges; j++) {
                    //panScFDL.ClusterNode cl2 = cid.get((int) sortedNodes[i][j].getAttribute("clusterNode");
                    //if (hmDens.get(sortedNodes[i][j]) > dns) {
                    e1 = graph.getModel().factory().newEdge(nodes[i], sortedNodes[i][j], 0, 1, false);
                    dns = hmDens.get(sortedNodes[i][j]);
                    graph.addEdge(e1);
                    //}
                }

            }
        }
        logger.print("Done adding edges");
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

        wrapLayout1 = new vortex.gui.WrapLayout();
        buttonGroup1 = new javax.swing.ButtonGroup();
        jSplitPane1 = new javax.swing.JSplitPane();
        panGraphContainer = new javax.swing.JPanel();
        panGraph = new javax.swing.JPanel();
        jp = new javax.swing.JProgressBar();
        panTop = new javax.swing.JPanel();
        jButton3 = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        spinScalingRatio = new javax.swing.JSpinner();
        chkAdjustSizes = new javax.swing.JCheckBox();
        jButton2 = new javax.swing.JButton();
        spinRescale = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        spinNodeSize = new javax.swing.JSpinner();
        jCheckBox1 = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        panBottom = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        chkCreateStacks = new javax.swing.JCheckBox();
        spinImgPerRow = new javax.swing.JSpinner();
        jLabel8 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
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
        spinZeroCutoff1 = new javax.swing.JSpinner();
        rbQuantile = new javax.swing.JRadioButton();
        rbLinear = new javax.swing.JRadioButton();
        jLabel10 = new javax.swing.JLabel();
        jCheckBox2 = new javax.swing.JCheckBox();
        jLabel11 = new javax.swing.JLabel();
        jCheckBox3 = new javax.swing.JCheckBox();

        setMinimumSize(new java.awt.Dimension(100, 100));
        setPreferredSize(new java.awt.Dimension(2000, 2000));
        setLayout(new java.awt.GridBagLayout());

        jSplitPane1.setDividerLocation(900);
        jSplitPane1.setResizeWeight(1.0);
        jSplitPane1.setMinimumSize(new java.awt.Dimension(100, 100));
        jSplitPane1.setPreferredSize(new java.awt.Dimension(2000, 2000));

        panGraphContainer.setMinimumSize(new java.awt.Dimension(100, 100));
        panGraphContainer.setPreferredSize(new java.awt.Dimension(2000, 2000));
        panGraphContainer.setLayout(new java.awt.GridBagLayout());

        panGraph.setBackground(new java.awt.Color(255, 255, 255));
        panGraph.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panGraph.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        panGraph.setMinimumSize(new java.awt.Dimension(100, 100));
        panGraph.setName(""); // NOI18N
        panGraph.setPreferredSize(new java.awt.Dimension(1000, 1000));
        panGraph.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                panGraphMouseClicked(evt);
            }
        });
        panGraph.setLayout(new java.awt.GridBagLayout());

        jp.setMinimumSize(new java.awt.Dimension(300, 20));
        jp.setPreferredSize(new java.awt.Dimension(300, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        panGraph.add(jp, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panGraphContainer.add(panGraph, gridBagConstraints);

        panTop.setPreferredSize(new java.awt.Dimension(1000, 200));

        org.openide.awt.Mnemonics.setLocalizedText(jButton3, "Start Layout");
        jButton3.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jButton3.setMaximumSize(new java.awt.Dimension(80, 25));
        jButton3.setMinimumSize(new java.awt.Dimension(80, 25));
        jButton3.setPreferredSize(new java.awt.Dimension(80, 25));
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        panTop.add(jButton3);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, "Stretch factor:");
        panTop.add(jLabel5);

        spinScalingRatio.setModel(new javax.swing.SpinnerNumberModel(5.0d, 0.0d, null, 1.0d));
        spinScalingRatio.setMinimumSize(new java.awt.Dimension(40, 20));
        spinScalingRatio.setPreferredSize(new java.awt.Dimension(40, 25));
        panTop.add(spinScalingRatio);

        org.openide.awt.Mnemonics.setLocalizedText(chkAdjustSizes, "Prevent Overlap"); // NOI18N
        panTop.add(chkAdjustSizes);

        org.openide.awt.Mnemonics.setLocalizedText(jButton2, "mult node coord. by:");
        jButton2.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jButton2.setMaximumSize(new java.awt.Dimension(120, 25));
        jButton2.setMinimumSize(new java.awt.Dimension(120, 25));
        jButton2.setPreferredSize(new java.awt.Dimension(105, 25));
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

        org.openide.awt.Mnemonics.setLocalizedText(jLabel9, "node Size:");
        panTop.add(jLabel9);

        spinNodeSize.setModel(new javax.swing.SpinnerNumberModel(3, 1, 10, 1));
        spinNodeSize.setPreferredSize(new java.awt.Dimension(39, 25));
        spinNodeSize.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinNodeSizeStateChanged(evt);
            }
        });
        panTop.add(spinNodeSize);

        org.openide.awt.Mnemonics.setLocalizedText(jCheckBox1, "show edges");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });
        panTop.add(jCheckBox1);

        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(0, 0, 153));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, "Left click on the graph to select clusters. Hold Ctrl for multiple selection. The selection appears in the cluster table. Change cluster color by double-clicking on the color box in the cluster table.");
        panTop.add(jLabel4);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        panGraphContainer.add(panTop, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jButton1, "Export Images");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        panBottom.add(jButton1);

        chkCreateStacks.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(chkCreateStacks, "Create Image Stacks");
        panBottom.add(chkCreateStacks);

        spinImgPerRow.setModel(new javax.swing.SpinnerNumberModel(5, 1, null, 1));
        panBottom.add(spinImgPerRow);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel8, "per row");
        panBottom.add(jLabel8);

        org.openide.awt.Mnemonics.setLocalizedText(jButton4, "Export Graph as GraphML");
        jButton4.setMaximumSize(new java.awt.Dimension(200, 23));
        jButton4.setMinimumSize(new java.awt.Dimension(200, 23));
        jButton4.setPreferredSize(new java.awt.Dimension(200, 23));
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        panBottom.add(jButton4);

        org.openide.awt.Mnemonics.setLocalizedText(jButton5, "Export cell coordinates");
        jButton5.setMaximumSize(new java.awt.Dimension(200, 23));
        jButton5.setMinimumSize(new java.awt.Dimension(200, 23));
        jButton5.setPreferredSize(new java.awt.Dimension(200, 23));
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        panBottom.add(jButton5);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        panGraphContainer.add(panBottom, gridBagConstraints);

        jSplitPane1.setLeftComponent(panGraphContainer);

        jPanel4.setPreferredSize(new java.awt.Dimension(2000, 2000));
        jPanel4.setRequestFocusEnabled(false);
        jPanel4.setLayout(new java.awt.GridBagLayout());

        jPanel5.setPreferredSize(new java.awt.Dimension(2000, 2000));
        jPanel5.setLayout(new java.awt.GridBagLayout());

        cmbAnnotations.setModel(new DefaultComboBoxModel<Annotation>());
        cmbAnnotations.setDoubleBuffered(true);
        cmbAnnotations.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                cmbAnnotationsMouseReleased(evt);
            }
        });
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

        jPanel1.setMinimumSize(new java.awt.Dimension(30, 71));
        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel7.setOpaque(false);
        jPanel7.setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, "<html>Grouping of annotation terms (toggle by click, right-click selects only that term)</html>");
        jLabel2.setMaximumSize(new java.awt.Dimension(200, 32));
        jLabel2.setMinimumSize(new java.awt.Dimension(200, 32));
        jLabel2.setPreferredSize(new java.awt.Dimension(200, 32));
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

        scpColumns.setPreferredSize(new java.awt.Dimension(200, 200));

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

        jPanel6.setPreferredSize(new java.awt.Dimension(200, 2000));
        jPanel6.setLayout(new java.awt.BorderLayout());

        spParamSelectionContainer.setPreferredSize(new java.awt.Dimension(200, 80));

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
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 5, 0);
        panColorControls.add(jLabel7, gridBagConstraints);

        spinZeroCutoff1.setModel(new javax.swing.SpinnerNumberModel(0.19d, null, null, 0.1d));
        spinZeroCutoff1.setMinimumSize(new java.awt.Dimension(61, 20));
        spinZeroCutoff1.setPreferredSize(new java.awt.Dimension(61, 25));
        spinZeroCutoff1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinZeroCutoff1StateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        panColorControls.add(spinZeroCutoff1, gridBagConstraints);

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

        org.openide.awt.Mnemonics.setLocalizedText(jLabel10, "Color scale:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        panColorControls.add(jLabel10, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jCheckBox2, "Show cluster labels");
        jCheckBox2.setEnabled(false);
        jCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox2ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 0);
        panColorControls.add(jCheckBox2, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel11, "min value:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 5);
        panColorControls.add(jLabel11, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jCheckBox3, "black background");
        jCheckBox3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox3ActionPerformed(evt);
            }
        });
        panColorControls.add(jCheckBox3, new java.awt.GridBagConstraints());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel4.add(panColorControls, gridBagConstraints);

        jSplitPane1.setRightComponent(jPanel4);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(jSplitPane1, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void panGraphMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panGraphMouseClicked

    }//GEN-LAST:event_panGraphMouseClicked

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        for (Node n : graph.getNodes()) {
            double f = (Double) spinRescale.getValue();
            n.setX((float) (n.x() * f));
            n.setY((float) (n.y() * f));
        }
        panGraphContainer.repaint();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        try {
            String dest = JOptionPane.showInputDialog("Set export path:", "..somepath\\sample.graphml");
            File f = new File(dest);
            f.getParentFile().mkdirs();
            ec.exportFile(f);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
    }//GEN-LAST:event_jButton4ActionPerformed

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

                    f.getParentFile().mkdirs();

                    ImageIO.write(img, "PNG", f);
                    if (createStack) {
                        BufferedImage bi = ImageIO.read(f);
                        f.delete();
                        Graphics2D g2 = ((Graphics2D) bi.createGraphics());
                        double fsize = (int) (50.0 * (400.0 / bi.getHeight()));
                        g2.setFont(new java.awt.Font("Arial", Font.BOLD, (int) fsize));
                        g2.setColor(jCheckBox3.isSelected() ? Color.WHITE : Color.BLACK);
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
                    f.getParentFile().mkdirs();
                    ImageIO.write(img, "PNG", f);
                }
            }

            for (int i = 0; i < paramList.length; i++) {
                colorNodesByExpressionLevel(i);
                BufferedImage bi = colorScale.generateScaleImage(i);
                File f = new File(path + File.separator + ds.getName() + "_scaleFor" + "_" + getFileSafeString(currParam) + ".png");
                f.getParentFile().mkdirs();
                ImageIO.write(bi, "PNG", f);
            }
        } catch (IOException ex) {
            logger.showException(ex);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    public static String getFileSafeString(String in) {
        return in.replaceAll(":", "-").replaceAll("\\|", "-").replaceAll("\\\\", "-").replaceAll("/", "-");
    }

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        spinScalingRatio.setEnabled(layoutOn);
        chkAdjustSizes.setEnabled(layoutOn);
        if (layoutOn) {
            layoutOn = false;
            sw.cancel(true);
            jButton3.setText("Start Layout");
            graphRenderer.useSimpleRendering(false);
            graphRenderer.repaint();
        } else {
            graphRenderer.useSimpleRendering(true);
            layoutOn = true;
            jButton3.setText("Stop Layout");
            layout.setGraphModel(graph.getModel());
            layout.initAlgo();
            layout.setThreadsCount(Runtime.getRuntime().availableProcessors() / 2);
            layout.setAdjustSizes(chkAdjustSizes.isSelected());
            layout.setBarnesHutOptimize(true);
            layout.setScalingRatio((Double) spinScalingRatio.getValue());

            sw = new SwingWorker() {
                long prevTime = Calendar.getInstance().getTimeInMillis();

                @Override
                protected Object doInBackground() throws Exception {
                    try {
                        while (layoutOn && layout.canAlgo() && !this.isCancelled() && panScFDL.this.isVisible() && panScFDL.this.isEnabled()) {
                            for (int i = 0; i < 10 && layout.canAlgo(); i++) {

                                try {
                                    if (layout.canAlgo()) {
                                        layout.goAlgo();
                                    }
                                } catch (Exception e) {
                                    if (!(e instanceof InterruptedException || e instanceof java.lang.RuntimeException)) {
                                        logger.showException(e);
                                    }
                                }

                                if (Calendar.getInstance().getTimeInMillis() - prevTime > 40) {
                                    lstParamValueChanged(null);
                                    panGraphContainer.repaint();
                                    prevTime = Calendar.getInstance().getTimeInMillis();
                                }
                            }
                        }

                    } catch (Exception e) {
                        logger.print(e);
                    }
                    jButton3.setText("Start Layout");
                    graphRenderer.useSimpleRendering(false);
                    graphRenderer.repaint();
                    return null;
                }
            };
            sw.execute();
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void spinNodeSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinNodeSizeStateChanged
        graphRenderer.setNodeSize((Integer) spinNodeSize.getValue());
        graphRenderer.repaint();
    }//GEN-LAST:event_spinNodeSizeStateChanged

    private void cmbAnnotationsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbAnnotationsActionPerformed

    }//GEN-LAST:event_cmbAnnotationsActionPerformed

    private void lstParamMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lstParamMouseClicked
        if (evt.getButton() == MouseEvent.BUTTON3) {
            if (fixedParamVal != lstParam.getSelectedIndex()) {
                fixedParamVal = lstParam.getSelectedIndex();
            } else {
                fixedParamVal = -1;
            }
        }
    }//GEN-LAST:event_lstParamMouseClicked

    private BufferedImage scaleImg = null;

    private void lstParamValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstParamValueChanged
        if (evt != null) {
            curParamIDX = -1;
            if (lstParam.getSelectedIndex() < 2) {
                curParamIDX = -1;
                if (lstParam.getSelectedIndex() == 0) {
                    graphRenderer.setColoringMode(FDLGraphRenderer.ColoringMode.CLUSTER);
                } else {
                    graphRenderer.setColoringMode(FDLGraphRenderer.ColoringMode.GROUP);
                }

            } else {
                colorNodesByExpressionLevel(lstParam.getSelectedIndex() - 2);
            }
        }

        panGraphContainer.repaint();
    }//GEN-LAST:event_lstParamValueChanged

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        graphRenderer.showEdges(jCheckBox1.isSelected());
        graphRenderer.repaint();
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void cmbAnnotationsMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_cmbAnnotationsMouseReleased
        if (cmbAnnotations.getModel().getSize() != ds.getAnnotations().length) {
            DefaultComboBoxModel cmb = new DefaultComboBoxModel();
            cmb.addElement("---none---");

            for (Annotation a : ds.getAnnotations()) {
                cmb.addElement(a);
            }

            //String[] options = new String[]{"Angular Distance", "Mahalonobis Distance"};
            //mahalonobis = JOptionPane.showInputDialog(this, "Select the distance measure for MST reconstruction", "Distance Measure", JOptionPane.QUESTION_MESSAGE, null, options, options[0]).equals(options[1]);
            cmbAnnotations.setModel(cmb);
        }
    }//GEN-LAST:event_cmbAnnotationsMouseReleased

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed

        File f2 = null;

        Shuffle<Node> s = new Shuffle();

        //final double ths = 1.2;
        //final int column  = 9;
        List<Node> filtered = Arrays.asList(nodes);//.stream().filter((c->ds.getDatapoints()[(int) c.getAttribute("dpID")].getVector()[column]>ths)).collect(Collectors.toList());

        compareDistances(filtered.toArray(new Node[filtered.size()]), 100);

        if ((f2 = IO.chooseFileWithDialog("dlgExportProfiles", "CSV (*.csv)", new String[]{"csv"}, true)) != null) {

            try {
                if (!f2.getName().toLowerCase().endsWith(".csv")) {
                    f2 = new File(f2.getAbsolutePath() + ".csv");
                }

                CSVWriter w = new CSVWriter(f2, null);
                w.writeHeader(new String[]{"EventID", "Filename", "Index_In_File", "X", "Y"});

                for (Node n : nodes) {
                    Datapoint d = ds.getDatapoints()[(int) n.getAttribute("dpID")];
                    w.writeData(
                            new String[]{
                                String.valueOf(d.getID()),
                                d.getFilename(),
                                String.valueOf(d.getIndexInFile()),
                                String.valueOf(n.x()),
                                String.valueOf(n.y())
                            }
                    );
                }

                w.close();

            } catch (Exception e) {
                logger.showException(e);
            }
        }
    }//GEN-LAST:event_jButton5ActionPerformed

    private void compareDistances(Node[] nd, int numCells) {

        int k = 0;

        //List<Datapoint> dp = Arrays.asList(nodes).stream().map((c)->cid.get((int) c.getAttribute("clusterNode")).dp).collect(Collectors.toList());
        double[][] origLen = new double[nd.length][numCells];
        double[][] embLen = new double[nd.length][numCells];

        for (Node n : Arrays.copyOf(nd, numCells)) {

            Datapoint d = ds.getDatapoints()[(int) n.getAttribute("dpID")];

            int t = 0;
            for (Node m2 : nd) {
                ClusterNode cl2 = cid.get((int) m2.getAttribute("clusterNode"));

                origLen[t][k] = MatrixOp.getEuclideanDistance(d.getVector(), cl2.dp.getVector());
                embLen[t][k] = Math.sqrt(Math.pow(m2.x() - n.x(), 2) + Math.pow(m2.y() - n.y(), 2));
                t++;
            }
            k++;
        }

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File("C:\\Users\\Nikolay Samusik\\Dropbox\\Diseffusion\\Nikolay analysis\\BM7_Eucl_scFDL_orig_dist_" + numCells + ".csv")));
            bw.write(MatrixOp.toCSV(origLen));
            bw.flush();
            bw.close();
            bw = new BufferedWriter(new FileWriter(new File("C:\\Users\\Nikolay Samusik\\Dropbox\\Diseffusion\\Nikolay analysis\\BM7_Eucl_scFDL_embed_dist_" + numCells + ".csv")));
            bw.write(MatrixOp.toCSV(embLen));
            bw.flush();
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void spinZeroCutoff1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinZeroCutoff1StateChanged
        COLOR_MAP_NOISE_THRESHOLD = (Double) spinZeroCutoff1.getValue();

        lstParamValueChanged(new ListSelectionEvent(this, lstParam.getSelectedIndex(), lstParam.getSelectedIndex(), false));
    }//GEN-LAST:event_spinZeroCutoff1StateChanged

    private void rbQuantileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbQuantileActionPerformed
        colorScale.setMode(ColorScale.ScalingMode.QUANTILE);
        lstParamValueChanged(new ListSelectionEvent(this, lstParam.getSelectedIndex(), lstParam.getSelectedIndex(), false));
    }//GEN-LAST:event_rbQuantileActionPerformed

    private void rbLinearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbLinearActionPerformed
        colorScale.setMode(ColorScale.ScalingMode.LINEAR);
        lstParamValueChanged(new ListSelectionEvent(this, lstParam.getSelectedIndex(), lstParam.getSelectedIndex(), false));
    }//GEN-LAST:event_rbLinearActionPerformed

    private void jCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox2ActionPerformed

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
    }//GEN-LAST:event_jCheckBox2ActionPerformed

    private void jCheckBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox3ActionPerformed
        this.graphRenderer.setBackground(jCheckBox3.isSelected() ? Color.BLACK : Color.WHITE);
        if (jCheckBox3.isSelected()) {
            colorScale.setForegroundColor(Color.WHITE);
        } else {
            colorScale.setForegroundColor(Color.BLACK);
        }
        lstParamValueChanged(null);
    }//GEN-LAST:event_jCheckBox3ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JCheckBox chkAdjustSizes;
    private javax.swing.JCheckBox chkCreateStacks;
    private samusik.glasscmp.GlassComboBox cmbAnnotations;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
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
    private javax.swing.JPanel panBottom;
    private javax.swing.JPanel panColorControls;
    private javax.swing.JPanel panGraph;
    private javax.swing.JPanel panGraphContainer;
    private javax.swing.JPanel panTop;
    private javax.swing.JRadioButton rbLinear;
    private javax.swing.JRadioButton rbQuantile;
    private javax.swing.JScrollPane scpColumns;
    private javax.swing.JScrollPane spParamSelectionContainer;
    private javax.swing.JSpinner spinImgPerRow;
    private javax.swing.JSpinner spinNodeSize;
    private javax.swing.JSpinner spinRescale;
    private javax.swing.JSpinner spinScalingRatio;
    private javax.swing.JSpinner spinZeroCutoff1;
    private javax.swing.JTable tabAnnTerms;
    private vortex.gui.WrapLayout wrapLayout1;
    // End of variables declaration//GEN-END:variables
}
