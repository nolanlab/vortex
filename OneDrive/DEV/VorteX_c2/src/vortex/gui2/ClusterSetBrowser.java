/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

 /*
 * ClusterSetBrowser.java
 *
 * Created on 17-Sep-2010, 14:07:03
 */
package vortex.gui2;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EventObject;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;
import samusik.glasscmp.GlassDialog;
import samusik.glasscmp.GlassTableHeader;
import samusik.objecttable.ObjectTableModel;
import samusik.objecttable.TableTransferHandler;
import annotations.Annotation;
import vortex.clustergraph.frmMST;
import vortex.clustergraph.frmDMT;
import vortex.clustergraph.frmScFDL;
import clustering.BarCode;
import clustering.Cluster;
import clustering.ClusterSet;
import clustering.Datapoint;
import java.util.HashMap;
import clustering.Dataset;
import vortex.main.TableCellEditorEx;
import vortex.clustering.HierarchicalClusteringCore;
import vortex.clustering.HierarchicalClusterTree;
import vortex.clustering.HierarchicalDendrogramRenderer;
import vortex.clustering.HierarchicalParamPanel;
import util.logger;
import vortex.clustergraph.ClusterPhylogeny;
import vortex.clustergraph.frmModuleMap;
import vortex.clustering.EuclideanDistance;
import vortex.clustering.XShiftClustering;
import vortex.util.ConnectionManager;

/**
 *
 * @author Nikolay
 */
public class ClusterSetBrowser extends javax.swing.JPanel implements ClusteringResultList.ClusterSetSelectionListener, ClipboardOwner {

    private static final long serialVersionUID = 1L;
    private ObjectTableModel<Cluster> tm = null;
    private ClusterBrowserPane cbp;

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }

    public void setClusterBrowserPane(ClusterBrowserPane cbp) {
        this.cbp = cbp;
    }

    public void selectCluster(int CID, boolean clearPreviousSelection) {
        if (tm == null) {
            return;
        }
        if (clearPreviousSelection) {
            table.getSelectionModel().clearSelection();
        }

        for (Cluster cluster : tm.getObjects(new Cluster[tm.getRowCount()])) {

            if (cluster.getID() == CID) {
                int idx = tm.getRowOfObject(cluster);
                int row = table.convertRowIndexToView(idx);
                table.getSelectionModel().addSelectionInterval(row, row);
            }
        }

    }

    /**
     * Creates new form ClusterSetBrowser
     */
    public ClusterSetBrowser() {
        initComponents();

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() || tm == null) {
                    return;
                }
                ArrayList<Cluster> al = new ArrayList<>();
                for (int i : table.getSelectedRows()) {
                    try {
                        if (table.convertRowIndexToModel(i) >= 0) {
                            Cluster cl = tm.getObjects(new Cluster[table.getModel().getRowCount()])[table.convertRowIndexToModel(i)];
                            al.add(cl);
                        }
                    } catch (IndexOutOfBoundsException ex) {
                        logger.showException(ex);
                    }
                }
                try {
                    fireClusterSelected(al.toArray(new Cluster[al.size()]));
                } catch (SQLException ex) {
                    logger.showException(ex);
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {

                    int idx = table.rowAtPoint(e.getPoint());
                    if (!table.isRowSelected(idx)) {
                        table.getSelectionModel().setSelectionInterval(idx, idx);
                    }
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    public void selectClusters(Cluster[] cl) {
        if (tm == null) {
            return;
        }
        table.getSelectionModel().clearSelection();
        for (Cluster cluster : cl) {
            int idx = tm.getRowOfObject(cluster);
            if (idx < 0) {
                return;
            }
            int row = table.convertRowIndexToView(idx);
            table.getSelectionModel().addSelectionInterval(row, row);
        }
    }

    private class CustomTableRowComparator implements Comparator<Object> {

        @Override
        public int compare(Object a, Object b) {
            if (Number.class.isAssignableFrom(a.getClass()) && Number.class.isAssignableFrom(b.getClass())) {
                return (int) Math.signum(((Number) a).doubleValue() - ((Number) b).doubleValue());
            } else {
                return a.toString().compareTo(b.toString());
            }
        }
    }
    private ClusterSet cs = null;

    public ClusterSet getClusterSet() {
        return cs;
    }

    public Cluster[] getSelectedClusters() {
        if (tm.getRowCount() == 0) {
            return new Cluster[0];
        }
        Cluster[] cl = tm.getObjects(new Cluster[tm.getRowCount()]);
        ArrayList<Cluster> al = new ArrayList<>();
        for (int i : table.getSelectedRows()) {
            al.add(cl[table.convertRowIndexToModel(i)]);
        }
        return al.toArray(new Cluster[al.size()]);
    }

    SwingWorker<Cluster[], Cluster> loadingWorker;

    public void setClusterSet(final ClusterSet cs) {
        if (loadingWorker != null) {
            loadingWorker.cancel(true);
        }
        tm = null;

        this.cs = cs;
        frmMain.getInstance().setActionInProgress("Loading clusters");
        loadingWorker = new SwingWorker<Cluster[], Cluster>() {

            @Override
            protected void process(List<Cluster> chunks) {
                Cluster[] cl = chunks.toArray(new Cluster[chunks.size()]);
                if (tm == null) {
                    tm = new ObjectTableModel<Cluster>(cl) {
                        @Override
                        public Class<?> getColumnClass(int columnIndex) {
                            Class cl = super.getColumnClass(columnIndex);
                            if (BarCode.class.isAssignableFrom(cl)) {
                                cl = BarCode.class;
                            }
                            return cl;
                        }
                    };
                    table.setModel(tm);
                    TableRowSorter<ObjectTableModel<Cluster>> trs = new TableRowSorter<>(tm);

                    table.setRowSorter(trs);

                    for (int i = 0; i < tm.getColumnCount(); i++) {
                        trs.setComparator(i, new CustomTableRowComparator());
                        trs.setSortable(i, true);
                    }

                    try {
                        table.getColumnModel().getColumn(table.getColumnModel().getColumnIndex("Barcode")).setPreferredWidth(getClusterSet().getDataset().getDimension() * 15);
                    } catch (Exception e) {
                    }
                    table.setDefaultRenderer(BarCode.class, new BarCodeTableCellRenderer());
                    table.setDefaultRenderer(Color.class, new ColorTableCellRenderer());

                    TableCellEditorEx editor = new TableCellEditorEx();

                    for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
                        table.getColumnModel().getColumn(i).setCellEditor(editor);
                        table.getColumnModel().getColumn(i).setHeaderRenderer(new GlassTableHeader(table.getTableHeader()));
                    }

                    table.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
                    table.setColumnSelectionAllowed(false);
                    table.setShowVerticalLines(false);
                    table.setRowHeight(20);
                    table.setTransferHandler(new TableTransferHandler());
                }
            }

            @Override
            protected Cluster[] doInBackground() throws Exception {
                Cluster[] cl = cs.getClusters();
                publish(cl);
                frmMain.getInstance().setActionInProgress(null);
                return cl;
            }

        };
        loadingWorker.execute();
    }

    @Override
    public void clusterSetSelected(ClusteringResultList.ClusterSetSelectionEvent evt) {
        this.setClusterSet(evt.getSource());
    }

    public interface ClusterSelectionListener {

        public void clusterSelected(ClusterSelectionEvent evt) throws SQLException;
    }

    public class ClusterSelectionEvent extends EventObject {

        private static final long serialVersionUID = 1L;
        Cluster[] clusters;

        @Override
        public Cluster[] getSource() {
            return clusters;
        }

        public ClusterSelectionEvent(Cluster[] cluster) {
            super(cluster);
            this.clusters = cluster;
        }
    }

    public void addClusterSelectionListener(ClusterSelectionListener listner) {
        selectionListeners.remove(listner);
        selectionListeners.add(listner);
    }

    public void removeClusterSelectionListener(ClusterSelectionListener listner) {
        selectionListeners.remove(listner);
    }
    private ArrayList<ClusterSelectionListener> selectionListeners = new ArrayList<>();

    private void fireClusterSelected(Cluster[] cl) throws SQLException {
        for (ClusterSelectionListener csl : selectionListeners) {
            csl.clusterSelected(new ClusterSelectionEvent(cl));
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
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        popup = new javax.swing.JPopupMenu();
        pmiOpen = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        pmiBiaxialPlot = new javax.swing.JMenuItem();
        pmiCopy = new javax.swing.JMenuItem();
        frmMenu = new javax.swing.JMenu();
        pmiMST = new javax.swing.JMenuItem();
        pmiFDL = new javax.swing.JMenuItem();
        pmiDMT = new javax.swing.JMenuItem();
        pmiModuleMap = new javax.swing.JMenuItem();
        pmiEnrichment = new javax.swing.JMenuItem();
        pmiPCA = new javax.swing.JMenuItem();
        pmiPlot = new javax.swing.JMenuItem();
        pmiPlotByTerm = new javax.swing.JMenuItem();
        pmiRegroup = new javax.swing.JMenuItem();
        pmiMerge = new javax.swing.JMenuItem();
        dlgRegroup = new GlassDialog(null, true);
        panHPP = new javax.swing.JPanel();
        glassButton1 = new samusik.glasscmp.GlassButton();
        rbDivisive = new javax.swing.JRadioButton();
        rbAggl = new javax.swing.JRadioButton();
        buttonGroup1 = new javax.swing.ButtonGroup();
        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();

        pmiOpen.setText("Open Cluster");
        pmiOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiOpenActionPerformed(evt);
            }
        });
        popup.add(pmiOpen);
        popup.add(jSeparator1);

        pmiBiaxialPlot.setText("Biaxial Plot");
        pmiBiaxialPlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiBiaxialPlotActionPerformed(evt);
            }
        });
        popup.add(pmiBiaxialPlot);

        pmiCopy.setText("Copy Profiles");
        pmiCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiCopyActionPerformed(evt);
            }
        });
        popup.add(pmiCopy);

        frmMenu.setText("Create graph...");

        pmiMST.setText("Minimum Spanning Tree");
        pmiMST.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiMSTActionPerformed(evt);
            }
        });
        frmMenu.add(pmiMST);

        pmiFDL.setText("Force-Directed Layout");
        pmiFDL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiFDLActionPerformed(evt);
            }
        });
        frmMenu.add(pmiFDL);

        pmiDMT.setText("Divisive Marker Tree");
        pmiDMT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiDMTActionPerformed(evt);
            }
        });
        frmMenu.add(pmiDMT);

        pmiModuleMap.setText("ModuleMap");
        pmiModuleMap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiModuleMapActionPerformed(evt);
            }
        });
        frmMenu.add(pmiModuleMap);

        popup.add(frmMenu);

        pmiEnrichment.setText("Compute Enrichment");
        pmiEnrichment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiEnrichmentActionPerformed(evt);
            }
        });
        popup.add(pmiEnrichment);

        pmiPCA.setText("PCA plot");
        pmiPCA.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiPCAActionPerformed(evt);
            }
        });
        popup.add(pmiPCA);

        pmiPlot.setText("Plot Average Profiles");
        pmiPlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiPlotActionPerformed(evt);
            }
        });
        popup.add(pmiPlot);

        pmiPlotByTerm.setText("Plot Average Profiles by Annotation Term");
        pmiPlotByTerm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiPlotByTermActionPerformed(evt);
            }
        });
        popup.add(pmiPlotByTerm);

        pmiRegroup.setText("Regroup with Hierarchical clustering...");
        pmiRegroup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiRegroupActionPerformed(evt);
            }
        });
        popup.add(pmiRegroup);

        pmiMerge.setText("Merge clusters by comment");
        pmiMerge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiMergeActionPerformed(evt);
            }
        });
        popup.add(pmiMerge);

        dlgRegroup.getContentPane().setLayout(new java.awt.GridBagLayout());

        panHPP.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        dlgRegroup.getContentPane().add(panHPP, gridBagConstraints);

        glassButton1.setText("Do clustering");
        glassButton1.setMaximumSize(new java.awt.Dimension(95, 28));
        glassButton1.setMinimumSize(new java.awt.Dimension(95, 28));
        glassButton1.setPreferredSize(new java.awt.Dimension(95, 28));
        glassButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                glassButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 4);
        dlgRegroup.getContentPane().add(glassButton1, gridBagConstraints);

        buttonGroup1.add(rbDivisive);
        rbDivisive.setText("Divisive");
        dlgRegroup.getContentPane().add(rbDivisive, new java.awt.GridBagConstraints());

        buttonGroup1.add(rbAggl);
        rbAggl.setText("Agglomerative");

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, panHPP, org.jdesktop.beansbinding.ELProperty.create("${enabled}"), rbAggl, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        rbAggl.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbAgglActionPerformed(evt);
            }
        });
        dlgRegroup.getContentPane().add(rbAggl, new java.awt.GridBagConstraints());

        setLayout(new java.awt.BorderLayout());

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane1.setViewportView(table);

        add(jScrollPane1, java.awt.BorderLayout.CENTER);

        bindingGroup.bind();
    }// </editor-fold>//GEN-END:initComponents

    private void pmiEnrichmentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiEnrichmentActionPerformed
        Cluster[] c = getSelectedClusters();
        if (c.length > 0) {
            frmEnrichment frm = frmEnrichment.getInstance(getClusterSet().getDataset());
            for (Cluster cluster : c) {
                try {
                    frm.addCluster(cluster);
                } catch (SQLException ex) {
                    logger.showException(ex);

                }
            }
            frm.setVisible(true);
        }
    }//GEN-LAST:event_pmiEnrichmentActionPerformed

    private void pmiPCAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiPCAActionPerformed
        try {
            (new frmPCA(getSelectedClusters(), false)).setVisible(true);
        } catch (SQLException ex) {
            logger.showException(ex);

        }
    }//GEN-LAST:event_pmiPCAActionPerformed

    private void pmiRegroupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiRegroupActionPerformed
        Cluster[] c = getSelectedClusters();
        if (c.length > 2) {
            panHPP.removeAll();
            HierarchicalParamPanel hpp = new HierarchicalParamPanel();
            panHPP.add(hpp);
            dlgRegroup.setBounds(200, 200, 500, 400);
            dlgRegroup.setVisible(true);
        }
    }//GEN-LAST:event_pmiRegroupActionPerformed

    private void glassButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_glassButton1ActionPerformed

        dlgRegroup.setVisible(false);
        HierarchicalClusterTree dend = null;

        if (rbDivisive.isSelected()) {
            try {
                Cluster[] c = getSelectedClusters();
                ClusterPhylogeny.ClusterTreeNode ctn = new ClusterPhylogeny().getDivisiveMarkerTree(c);
                Datapoint[] d = new Datapoint[ctn.getUserObject().length];
                for (int i = 0; i < d.length; i++) {
                    d[i] = new Datapoint(ctn.getUserObject()[i].getFullName(), ctn.getUserObject()[i].getVector(), ctn.getUserObject()[i].getSideVector(), i);
                }

                Dataset ds = new Dataset("temp", d, ctn.getUserObject()[0].getCluster().getClusterSet().getDataset().getFeatureNames(), ctn.getUserObject()[0].getCluster().getClusterSet().getDataset().getSideVarNames());
                dend = new HierarchicalClusterTree(ctn, ds, new EuclideanDistance(), "Divisive Marker Tree", HierarchicalParamPanel.LinkageType.DivisiveMarkerTree);
            } catch (SQLException e) {
                logger.showException(e);
            }
        } else {
            //NDataset d = getClusterSet().getDataset();
            HierarchicalParamPanel hpp = (HierarchicalParamPanel) panHPP.getComponent(0);
            //new HierarchicalClusteringCore(d).doClustering(d, hpp.getDistanceMeasure(), hpp.getLinkageType(), getSelectedClusters());
            Cluster[] c = getSelectedClusters();
            Datapoint[] dp = new Datapoint[c.length];
            for (int i = 0; i < dp.length; i++) {
                dp[i] = new Datapoint(c[i].getID() + " size: " + c[i].size() + "| " + c[i].getComment(), c[i].getMode().getVector(), c[i].getMode().getSideVector(), i);
            }
            Dataset d = new Dataset("Cluster Modes of CS" + getClusterSet().getID(), dp, getClusterSet().getDataset().getFeatureNames(), getClusterSet().getDataset().getSideVarNames());
            dend = new HierarchicalClusteringCore(getClusterSet().getDistanceMeasure()).doClustering(d, hpp.getLinkageType(), null);

        }

        JDialog dlg = new JDialog(frmMain.getInstance(), true);
        HierarchicalDendrogramRenderer pan = new HierarchicalDendrogramRenderer();
        pan.setDendrogram(dend);

        dlg.setAlwaysOnTop(true);
        dlg.setModal(true);
        dlg.getContentPane().add(new JScrollPane(pan));
        dlg.setBounds(100, 100, 500, 500);
        dlg.setVisible(true);
    }//GEN-LAST:event_glassButton1ActionPerformed
    private void pmiCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiCopyActionPerformed
        String s = "";
        for (Cluster c : getSelectedClusters()) {
            s += c.getMode().toString() + "\n";
        }

        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s.substring(0, s.length() - 1)), this);
    }//GEN-LAST:event_pmiCopyActionPerformed

    private void pmiPlotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiPlotActionPerformed
        if (getSelectedClusters().length >= 0) {
            frmProfilePlot frm = frmMain.getProfilePlot(getClusterSet().getDataset());
            frm.setVisible(true);
            for (Cluster c : getSelectedClusters()) {
                frm.addCluster(c);
            }
        }
    }//GEN-LAST:event_pmiPlotActionPerformed

    private void pmiBiaxialPlotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiBiaxialPlotActionPerformed
        if (getSelectedClusters().length >= 0) {
            frmBiaxialPlot frm = new frmBiaxialPlot(getSelectedClusters());
            frm.setBounds(100, 100, 600, 500);
            frm.setVisible(true);
        }
    }//GEN-LAST:event_pmiBiaxialPlotActionPerformed

    private void pmiMSTActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiMSTActionPerformed

        Cluster[] cl = getSelectedClusters();
        if (cl.length < 2) {
            JOptionPane.showMessageDialog(this, "You must select at least two clusters");
            return;
        }
        // String [] options = new String[]{"feature variables only","feature and functional variables"};
        frmMST frm = new frmMST(cl);//, cl[0].getClusterSet().getDataset(), false, false);
        this.addClusterSelectionListener(frm);
        int w = (int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.9);
        int h = (int) (Toolkit.getDefaultToolkit().getScreenSize().height * 0.9);
        int x = (int) (Toolkit.getDefaultToolkit().getScreenSize().width / 2 - w / 2);
        int y = (int) (Toolkit.getDefaultToolkit().getScreenSize().height / 2 - h / 2);
        frm.setBounds(x, y, w, h);
        frm.setVisible(true);
        frm.setTitle("Minimum Spanning Tree, CS" + cs.getID());

    }//GEN-LAST:event_pmiMSTActionPerformed

    private void pmiOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiOpenActionPerformed
        if (cbp != null && getSelectedClusters() != null) {
            for (Cluster c : getSelectedClusters()) {
                try {
                    cbp.displayCluster(c);
                } catch (SQLException e) {
                    logger.showException(e);
                }
            }
        }
    }//GEN-LAST:event_pmiOpenActionPerformed

    private void pmiPlotByTermActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiPlotByTermActionPerformed
        if (getSelectedClusters().length >= 0) {
            Annotation ann = (Annotation) JOptionPane.showInputDialog(this, "Select the annotation", "Annotations", JOptionPane.QUESTION_MESSAGE, null, getClusterSet().getDataset().getAnnotations(), getClusterSet().getDataset().getAnnotations()[0]);
            frmProfilePlot frm = frmMain.getProfilePlot(getClusterSet().getDataset());
            frm.setVisible(true);
            for (Cluster c : getSelectedClusters()) {
                frm.addCluster(c, ann);
            }
        }
    }//GEN-LAST:event_pmiPlotByTermActionPerformed

    private void pmiFDLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiFDLActionPerformed

        Cluster[] cl = getSelectedClusters();

        // String [] options = new String[]{"feature variables only","feature and functional variables"};
        frmScFDL frm = new frmScFDL(cl);//, cl[0].getClusterSet().getDataset(), false, false);
        int w = (int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.9);
        int h = (int) (Toolkit.getDefaultToolkit().getScreenSize().height * 0.9);
        int x = (int) (Toolkit.getDefaultToolkit().getScreenSize().width / 2 - w / 2);
        int y = (int) (Toolkit.getDefaultToolkit().getScreenSize().height / 2 - h / 2);
        frm.setBounds(x, y, w, h);
        frm.setVisible(true);
        this.addClusterSelectionListener(frm);
        frm.setTitle("Single-cell Force-Directed Layout, CS" + cs.getID());
    }//GEN-LAST:event_pmiFDLActionPerformed

    private void pmiDMTActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiDMTActionPerformed
        try {
            Cluster[] cl = getSelectedClusters();
            if (cl.length < 2) {
                JOptionPane.showMessageDialog(this, "You must select at least two clusters");
                return;
            }
            // String [] options = new String[]{"feature variables only","feature and functional variables"};
            frmDMT frm = new frmDMT(cl);//, cl[0].getClusterSet().getDataset(), false, false);
            int w = (int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.9);
            int h = (int) (Toolkit.getDefaultToolkit().getScreenSize().height * 0.9);
            int x = (int) (Toolkit.getDefaultToolkit().getScreenSize().width / 2 - w / 2);
            int y = (int) (Toolkit.getDefaultToolkit().getScreenSize().height / 2 - h / 2);
            frm.setBounds(x, y, w, h);
            frm.setVisible(true);
            frm.setTitle("Divisive Marker Tree, CS" + cs.getID());
            this.addClusterSelectionListener(frm);
        } catch (SQLException e) {
            logger.showException(e);
        }
    }//GEN-LAST:event_pmiDMTActionPerformed

    private void rbAgglActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbAgglActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_rbAgglActionPerformed

    private void pmiMergeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiMergeActionPerformed
        if (JOptionPane.showConfirmDialog(this, "All clusters with the same comment will be merged together and a new cluster set will be formed. Clusters with no comment won't be merged.") == JOptionPane.OK_OPTION) {
            Cluster[] clus = getSelectedClusters();
            if (clus.length == 0) {
                return;
            }
            ClusterSet cs = clus[0].getClusterSet();
            Dataset d = cs.getDataset();
            HashMap<String, Cluster> hm = new HashMap<>();
            for (Cluster clu : clus) {
                if (clu.getComment().length() < 1) {
                    hm.put(String.valueOf(clu.getID()), clu);
                } else if (hm.get(clu.getComment()) != null) {
                    Cluster c = XShiftClustering.mergeClusters(hm.get(clu.getComment()), clu);
                    c.setComment(clu.getComment());
                    hm.put(clu.getComment(), c);
                } else {
                    hm.put(clu.getComment(), clu);
                }
            }
            Cluster[] out = hm.values().toArray(new Cluster[hm.size()]);
            try {
                for (int i = 0; i < out.length; i++) {
                    Datapoint[] dp = new Datapoint[out[i].getClusterMembers().length];
                    for (int j = 0; j < dp.length; j++) {
                        dp[j] = out[i].getClusterMembers()[j].getDatapoint();
                    }
                    out[i] = new Cluster(dp, out[i].getMedianFeatureVec(), out[i].getMode().getSideVector(), out[i].getComment());
                }

                ClusterSet cs2 = new ClusterSet(ConnectionManager.getStorageEngine().getNextClusterSetBatchID(d.getID()), d, out, cs.getDistanceMeasure(), "Merged " + cs.getClusteringAlgorithm(), cs.getClusteringParameterString(), cs.getMainClusteringParameterValue(), cs.getComment());

                ConnectionManager.getStorageEngine().saveClusterSet(cs2, true);

            } catch (SQLException e) {
                logger.showException(e);
            }
        }

    }//GEN-LAST:event_pmiMergeActionPerformed

    private void pmiModuleMapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiModuleMapActionPerformed
        frmModuleMap frm = null;
        try {
            Cluster[] cl = getSelectedClusters();

            if (cl.length < 2) {
                JOptionPane.showMessageDialog(this, "You must select at least two clusters");
                return;
            }
            frm = new frmModuleMap(cl);
            int w = (int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.9);
            int h = (int) (Toolkit.getDefaultToolkit().getScreenSize().height * 0.9);
            int x = (int) (Toolkit.getDefaultToolkit().getScreenSize().width / 2 - w / 2);
            int y = (int) (Toolkit.getDefaultToolkit().getScreenSize().height / 2 - h / 2);
            frm.setBounds(x, y, w, h);
            frm.setVisible(true);
            frm.setTitle("ModuleMap, CS" + cs.getID());
            this.addClusterSelectionListener(frm);
        } catch (SQLException e) {
            logger.showException(e);
            if(frm!=null)frm.setVisible(false);
        }
    }//GEN-LAST:event_pmiModuleMapActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JDialog dlgRegroup;
    private javax.swing.JMenu frmMenu;
    private samusik.glasscmp.GlassButton glassButton1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPanel panHPP;
    private javax.swing.JMenuItem pmiBiaxialPlot;
    private javax.swing.JMenuItem pmiCopy;
    private javax.swing.JMenuItem pmiDMT;
    private javax.swing.JMenuItem pmiEnrichment;
    private javax.swing.JMenuItem pmiFDL;
    private javax.swing.JMenuItem pmiMST;
    private javax.swing.JMenuItem pmiMerge;
    private javax.swing.JMenuItem pmiModuleMap;
    private javax.swing.JMenuItem pmiOpen;
    private javax.swing.JMenuItem pmiPCA;
    private javax.swing.JMenuItem pmiPlot;
    private javax.swing.JMenuItem pmiPlotByTerm;
    private javax.swing.JMenuItem pmiRegroup;
    private javax.swing.JPopupMenu popup;
    private javax.swing.JRadioButton rbAggl;
    private javax.swing.JRadioButton rbDivisive;
    private javax.swing.JTable table;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables
}
