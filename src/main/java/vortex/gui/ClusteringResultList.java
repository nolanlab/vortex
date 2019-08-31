/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

 /*
 * ClusterSetsBrowser.java
 *
 * Created on 16-Sep-2010, 15:02:01
 */
package vortex.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import samusik.glasscmp.GlassTableHeader;
import samusik.objecttable.ObjectTableModel;
import samusik.objecttable.TableTransferHandler;
import sandbox.clustering.ClusterSet;
import sandbox.clustering.Dataset;
import vortex.main.TableCellEditorEx;
import sandbox.annotations.Annotation;
import vortex.gui.clusterdendrogram.frmHierarchyPlot;
import sandbox.clustering.ClusterMember;
import sandbox.dataIO.ClusterSetToFCSExporter;
import java.awt.Color;
import java.io.IOException;
import util.IO;
import util.LinePlusExponent;
import vortex.util.ConnectionManager;
import vortex.util.GroupStatisticsInClusters;
import util.logger;
import vortex.main.ClusterSetCache;
import vortex.util.ClusterSetExporter;

/**
 *
 * @author Nikolay
 */
public class ClusteringResultList extends javax.swing.JPanel implements DatasetBrowser.DatasetSelectionListener {

    private static final long serialVersionUID = 1L;
    private Dataset[] ds = null;
    private boolean toolbarVisible = true;
    ClusterSetsLazyLoader cl = null;
    Thread clThread = null;
    ObjectTableModel<ClusterSet> tm = null;

    public ClusterSet[] getSelectedClusterSets() {
        if (tm == null) {
            return new ClusterSet[0];
        }
        int[] idx = table.getSelectedRows();
        ClusterSet[] cs = new ClusterSet[idx.length];
        ClusterSet[] data = tm.getObjects(cs);
        for (int i = 0; i < cs.length; i++) {
            cs[i] = data[table.convertRowIndexToModel(idx[i])];
        }
        if (idx.length > 0) {
            return cs;
        } else {
            return data;
        }
    }

    public boolean isToolbarVisible() {
        return toolbarVisible;
    }

    public void setToolbarVisible(boolean toolbarVisible) {
        this.toolbarVisible = toolbarVisible;
        this.toolbar.setVisible(toolbarVisible);
    }

    /**
     * Creates new form ClusterSetsBrowser
     */
    public ClusteringResultList() {
        initComponents();
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(Color.LIGHT_GRAY);
    }

    public void addClusterSetSelectionListener(ClusterSetSelectionListener listener) {
        listeners.add(listener);
    }

    public void removeClusterSetSelectionListener(ClusterSetSelectionListener listener) {
        listeners.remove(listener);
    }

    private class CustomTableRowComparator implements Comparator {

        @Override
        public int compare(Object a, Object b) {
            if (a instanceof Number && b instanceof Number) {
                return (int) Math.signum(((Number) a).doubleValue() - ((Number) b).doubleValue());
            } else {
                return a.toString().compareTo(b.toString());
            }
        }
    }

    public void setClusterSets(int[] csIDs) {

        if (cl != null) {
            if (!cl.isDone()) {
                cl.cancel(true);
            }
        }

        if (csIDs == null) {
            table.setModel(new DefaultTableModel());
            return;
        }

        if (csIDs.length == 0) {
            table.setModel(new DefaultTableModel());
            return;
        }

        ClusterSet cs = ClusterSetCache.getInstance(ds[0], csIDs[0]);

        tm = new ObjectTableModel<>(new ClusterSet[]{cs});
        table.setModel(tm);

        DefaultTableColumnModel cm = new DefaultTableColumnModel();

        for (int k = 0; k < tm.getColumnCount(); k++) {
            TableColumn col = new TableColumn(k, 70);
            col.setHeaderValue(tm.getColumnName(k));
            cm.addColumn(col);
        }

        table.setColumnModel(cm);

        cm.getColumn(0).setPreferredWidth(50);
        cm.getColumn(1).setPreferredWidth(300);

        TableRowSorter<ObjectTableModel<ClusterSet>> trs = new TableRowSorter<ObjectTableModel<ClusterSet>>(tm) {
            @Override
            public void rowsInserted(int firstRow, int endRow) {
                allRowsChanged();
            }
        };

        table.setRowSorter(trs);
        TableCellEditorEx editor = new TableCellEditorEx();

        for (int p = 0; p < table.getColumnCount(); p++) {
            table.getColumnModel().getColumn(p).setCellEditor(editor);
            trs.setComparator(p, new CustomTableRowComparator());
            table.getColumnModel().getColumn(p).setHeaderRenderer(new GlassTableHeader(table.getTableHeader()));
        }

        table.getSelectionModel().addListSelectionListener(lsl);
        table.setColumnSelectionAllowed(false);
        table.setShowVerticalLines(false);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    // frmMain.getInstance().getProfilePopup().show(table, e.getX(), e.getY());
                }
            }
        });
        table.setTransferHandler(new TableTransferHandler());

        if (csIDs.length > 1) {
            cl = new ClusterSetsLazyLoader(Arrays.copyOfRange(csIDs, 1, csIDs.length));
            cl.execute();
        }
    }

    ArrayList<ClusterSetSelectionListener> listeners = new ArrayList<>();

    private void fireClusterSetSelected(ClusterSet cs) {
        for (ClusterSetSelectionListener listener : listeners) {
            listener.clusterSetSelected(new ClusterSetSelectionEvent(cs));
        }
    }

    public class ClusterSetSelectionEvent extends EventObject {

        ClusterSet cs;

        @Override
        public ClusterSet getSource() {
            return cs;
        }

        public ClusterSetSelectionEvent(ClusterSet cs) {
            super(cs);
            this.cs = cs;
        }
    }

    public interface ClusterSetSelectionListener extends EventListener {

        public void clusterSetSelected(ClusterSetSelectionEvent evt);
    }

    public ClusterSet[] getClusterSets() {
        if (tm == null) {
            return new ClusterSet[0];
        }
        return tm.getObjects(new ClusterSet[table.getRowCount()]);
    }

    @Override
    public void datasetSelected(DatasetBrowser.DatasetSelectionEvent evt) {
        try {
            this.ds = evt.getSource();
            if (ConnectionManager.getStorageEngine() != null) {
                if (evt.getSource().length > 0) {
                    setClusterSets(ConnectionManager.getStorageEngine().getClusterSetIDs(evt.getSource()[0].getID()));
                }

            }
        } catch (SQLException ex) {
            logger.showException(ex);
        }
    }

    private class ClusterSetsLazyLoader extends SwingWorker<String, ClusterSet> {

        double increment;
        double dProgress;
        private int[] clusterSetIDs;

        public void setProgress(double progress) {
            this.dProgress = progress;
        }

        public ClusterSetsLazyLoader(int[] clusterSetIDs) {
            this.clusterSetIDs = clusterSetIDs;
        }

        @Override
        protected void process(List<ClusterSet> chunks) {
            for (ClusterSet cs : chunks) {
                tm.addRow(cs);
            }
        }

        @Override
        public synchronized String doInBackground() {
            try {
                if (clusterSetIDs == null) {
                    return null;
                }
                if (ds.length == 0) {
                    return null;
                }

                for (int i = 0; i < clusterSetIDs.length; i++) {
                    int csID = clusterSetIDs[i];
                    ClusterSet cs = ClusterSetCache.getInstance(ds[0], csID);
                    if (cs == null) {
                        continue;
                    }
                    if (this.isCancelled()) {
                        break;
                    }
                    publish(cs);
                    dProgress += increment;
                    this.setProgress((int) dProgress);
                }

            } catch (Exception e) {
                this.setProgress(100);
                if (!(e instanceof InterruptedException || e instanceof IllegalStateException)) {
                    logger.showException(e);
                }
                return null;
            }
            this.setProgress(100);
            return "Loading Done";
        }
    }
    private ListSelectionListener lsl = new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int idx = table.getSelectionModel().getLeadSelectionIndex();
            if (!(table.getModel() instanceof ObjectTableModel)) {
                return;
            }
            ClusterSet[] css = ((ObjectTableModel<ClusterSet>) table.getModel()).getObjects(new ClusterSet[table.getModel().getRowCount()]);

            try {
                if (table.convertRowIndexToModel(idx) >= 0) {
                    ClusterSet cs = css[table.convertRowIndexToModel(idx)];
                    fireClusterSetSelected(cs);
                    for (int i = 0; i < css.length; i++) {
                        if (i == idx) {
                            continue;
                        }
                        css[i].close();
                    }
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
            } catch (IndexOutOfBoundsException ex) {
            }
        }
    };

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        popup = new javax.swing.JPopupMenu();
        pmiStatsPerGroup = new javax.swing.JMenuItem();
        pmiExport = new javax.swing.JMenuItem();
        pmiExportIndexString = new javax.swing.JMenuItem();
        pmiExportFCS = new javax.swing.JMenuItem();
        menuHierarchyPlot = new javax.swing.JMenu();
        pmiAddToHierarchyPlot = new javax.swing.JMenuItem();
        pmiCreateHierarchyPlot = new javax.swing.JMenuItem();
        pmiPCAPlot = new javax.swing.JMenuItem();
        pmiValidation = new javax.swing.JMenu();
        pmiElbow = new javax.swing.JMenuItem();
        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        toolbar = new samusik.glasscmp.GlassToolBar();
        cmdNewClustering = new samusik.glasscmp.GlassButton();
        cmdDeleteClusterSets = new samusik.glasscmp.GlassButton();
        jLabel1 = new javax.swing.JLabel();

        pmiStatsPerGroup.setText("Compute group statistics per cluster");
        pmiStatsPerGroup.setToolTipText("");
        pmiStatsPerGroup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiStatsPerGroupActionPerformed(evt);
            }
        });
        popup.add(pmiStatsPerGroup);

        pmiExport.setText("Export As CSV");
        pmiExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiExportActionPerformed(evt);
            }
        });
        popup.add(pmiExport);

        pmiExportIndexString.setText("Export cluster index string");
        pmiExportIndexString.setEnabled(false);
        pmiExportIndexString.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiExportIndexStringActionPerformed(evt);
            }
        });
        popup.add(pmiExportIndexString);

        pmiExportFCS.setText("Export As FCS");
        pmiExportFCS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiExportFCSActionPerformed(evt);
            }
        });
        popup.add(pmiExportFCS);

        menuHierarchyPlot.setText("Hieararchy plot...");
        menuHierarchyPlot.setToolTipText("");
        menuHierarchyPlot.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                menuHierarchyPlotMouseEntered(evt);
            }
        });

        pmiAddToHierarchyPlot.setText("Add to existing");
        pmiAddToHierarchyPlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiAddToHierarchyPlotActionPerformed(evt);
            }
        });
        menuHierarchyPlot.add(pmiAddToHierarchyPlot);

        pmiCreateHierarchyPlot.setText("Create new");
        pmiCreateHierarchyPlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiCreateHierarchyPlotActionPerformed(evt);
            }
        });
        menuHierarchyPlot.add(pmiCreateHierarchyPlot);

        popup.add(menuHierarchyPlot);

        pmiPCAPlot.setText("PCA Plot");
        pmiPCAPlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiPCAPlotActionPerformed(evt);
            }
        });
        popup.add(pmiPCAPlot);

        pmiValidation.setText("Validation");

        pmiElbow.setText("find Elbow Point for Cluster Number");
        pmiElbow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiElbowActionPerformed(evt);
            }
        });
        pmiValidation.add(pmiElbow);

        popup.add(pmiValidation);

        setLayout(new java.awt.BorderLayout());

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableMouseReleased(evt);
            }
        });
        jScrollPane1.setViewportView(table);

        add(jScrollPane1, java.awt.BorderLayout.CENTER);

        toolbar.setMaximumSize(new java.awt.Dimension(2147483647, 50));
        toolbar.setMinimumSize(new java.awt.Dimension(100, 50));
        toolbar.setPreferredSize(new java.awt.Dimension(100, 50));
        toolbar.setLayout(new java.awt.GridBagLayout());

        cmdNewClustering.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon-plus_32.png"))); // NOI18N
        cmdNewClustering.setToolTipText("New clustering");
        cmdNewClustering.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cmdNewClustering.setIconTextGap(0);
        cmdNewClustering.setMargin(new java.awt.Insets(2, 2, 2, 2));
        cmdNewClustering.setMaximumSize(new java.awt.Dimension(36, 35));
        cmdNewClustering.setMinimumSize(new java.awt.Dimension(36, 35));
        cmdNewClustering.setPreferredSize(new java.awt.Dimension(36, 35));
        cmdNewClustering.setVerifyInputWhenFocusTarget(false);
        cmdNewClustering.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdNewClusteringActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(7, 8, 7, 0);
        toolbar.add(cmdNewClustering, gridBagConstraints);

        cmdDeleteClusterSets.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Close.png"))); // NOI18N
        cmdDeleteClusterSets.setToolTipText("Delete Cluster Sets");
        cmdDeleteClusterSets.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cmdDeleteClusterSets.setIconTextGap(0);
        cmdDeleteClusterSets.setMargin(new java.awt.Insets(2, 2, 2, 2));
        cmdDeleteClusterSets.setMaximumSize(new java.awt.Dimension(36, 35));
        cmdDeleteClusterSets.setMinimumSize(new java.awt.Dimension(36, 35));
        cmdDeleteClusterSets.setPreferredSize(new java.awt.Dimension(36, 35));
        cmdDeleteClusterSets.setVerifyInputWhenFocusTarget(false);
        cmdDeleteClusterSets.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdDeleteClusterSetsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(7, 5, 7, 0);
        toolbar.add(cmdDeleteClusterSets, gridBagConstraints);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(16, 140, 171));
        jLabel1.setText("Clustering");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        toolbar.add(jLabel1, gridBagConstraints);

        add(toolbar, java.awt.BorderLayout.PAGE_START);
    }// </editor-fold>//GEN-END:initComponents

    private void cmdNewClusteringActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdNewClusteringActionPerformed
        if (ds == null) {
            JOptionPane.showMessageDialog(frmMain.getInstance(), "Select a dataset first", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (ds.length == 0) {
            JOptionPane.showMessageDialog(frmMain.getInstance(), "Select a dataset first", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try{
        if(ConnectionManager.getStorageEngine().getSchemaVersion().startsWith("1.")){
            JOptionPane.showMessageDialog(frmMain.getInstance(), "Warning: you have selected a database that was created in an older version (1.x). This database format is deprecated and may not be fully supported in the future.\n While you can safely browse the existing data, you cannot import new datasets or create analyses. \n Please setup a new database connection, which will be automatically created in a new format.");
            return;
        }
        }catch(SQLException e){
            logger.showException(e);
        }
        
        dlgNewClustering dlg = new dlgNewClustering(frmMain.getInstance(), ds);
        dlg.setVisible(true);
        try {
            if (ConnectionManager.getStorageEngine() != null) {
                setClusterSets(ConnectionManager.getStorageEngine().getClusterSetIDs(ds[0].getID()));
            }
        } catch (SQLException e) {
            logger.showException(e);
        }
    }//GEN-LAST:event_cmdNewClusteringActionPerformed

    private void tableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMouseReleased
        if (evt.getButton() == MouseEvent.BUTTON3) {
            int idx = table.rowAtPoint(evt.getPoint());
            if (!table.isRowSelected(idx)) {
                table.getSelectionModel().setSelectionInterval(idx, idx);
            }
            popup.show(table, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_tableMouseReleased

    private void pmiAddToHierarchyPlotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiAddToHierarchyPlotActionPerformed
        if (table.getSelectedRows().length > 0) {
            ClusterSet[] css = new ClusterSet[table.getSelectedRows().length];
            ClusterSet[] rows = ((ObjectTableModel<ClusterSet>) table.getModel()).getObjects(new ClusterSet[table.getRowCount()]);
            for (int i = 0; i < css.length; i++) {
                css[i] = rows[table.convertRowIndexToModel(table.getSelectedRows()[i])];
            }
            frmHierarchyPlot.getInstances().get(0).addClusterSets(css);
            frmHierarchyPlot.getInstances().get(0).setVisible(true);
        }

    }//GEN-LAST:event_pmiAddToHierarchyPlotActionPerformed

    private void pmiPCAPlotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiPCAPlotActionPerformed
        int idx = table.convertRowIndexToModel(table.getSelectedRow());
        ClusterSet[] rows = ((ObjectTableModel<ClusterSet>) table.getModel()).getObjects(new ClusterSet[table.getRowCount()]);
        frmPCA pca;
        try {
            pca = new frmPCA(rows[idx], false);
            pca.setBounds(200, 300, 800, 600);
            pca.setVisible(true);
        } catch (SQLException ex) {
            logger.showException(ex);

        }

    }//GEN-LAST:event_pmiPCAPlotActionPerformed

    private void cmdDeleteClusterSetsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdDeleteClusterSetsActionPerformed
        int[] idx = table.getSelectedRows();
        if (idx.length == 0) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to delete selected cluster sets?", "Confirm deletion", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            ClusterSet[] rows = ((ObjectTableModel<ClusterSet>) table.getModel()).getObjects(new ClusterSet[table.getRowCount()]);
            final ArrayList<ClusterSet> alCS = new ArrayList<ClusterSet>();
            for (int i = 0; i < idx.length; i++) {
                ClusterSet cs = rows[table.convertRowIndexToModel(idx[i])];
                alCS.add(cs);
                idx = table.getSelectedRows();
            }
            ObjectTableModel<ClusterSet> cs = ((ObjectTableModel<ClusterSet>) table.getModel());
            int row = cs.getRowOfObject(alCS.get(0)) - 1;
            for (ClusterSet clusterSet : alCS) {
                cs.removeRow(clusterSet);
            }
            if (table.getRowCount() > row) {
                table.getSelectionModel().setSelectionInterval(row, row);
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (ClusterSet clusterSet : alCS) {
                            clusterSet.delete();
                            ClusterSetCache.remove(clusterSet.getDataset(), clusterSet.getID());
                        }
                    } catch (SQLException e) {
                        logger.showException(e);
                    }
                    JOptionPane.showMessageDialog(frmMain.getInstance(), alCS.size() + "Cluster sets deleted");
                }
            }).start();
        }
    }//GEN-LAST:event_cmdDeleteClusterSetsActionPerformed

    public void selectClusterSet(ClusterSet cs) {
        if (tm == null) {
            return;
        }
        if (getSelectedClusterSets().length == 1) {
            if (getSelectedClusterSets()[0].equals(cs)) {
                return;
            }
        }

        int idx = tm.getRowOfObject(cs);
        int row = table.convertRowIndexToView(idx);
        table.getSelectionModel().setSelectionInterval(row, row);
    }

    private void pmiCreateHierarchyPlotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiCreateHierarchyPlotActionPerformed
        if (table.getSelectedRows().length > 0) {
            ClusterSet[] css = new ClusterSet[table.getSelectedRows().length];
            ClusterSet[] rows = ((ObjectTableModel<ClusterSet>) table.getModel()).getObjects(new ClusterSet[table.getRowCount()]);
            for (int i = 0; i < css.length; i++) {
                css[i] = rows[table.convertRowIndexToModel(table.getSelectedRows()[i])];
            }
            frmHierarchyPlot plot = frmHierarchyPlot.createInstance(css);
            plot.setVisible(true);
        }
    }//GEN-LAST:event_pmiCreateHierarchyPlotActionPerformed

    private void menuHierarchyPlotMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_menuHierarchyPlotMouseEntered
        pmiAddToHierarchyPlot.setEnabled(!frmHierarchyPlot.getInstances().isEmpty());
    }//GEN-LAST:event_menuHierarchyPlotMouseEntered

    private void pmiExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiExportActionPerformed
        if (getSelectedClusterSets().length > 0) {
            File f2;
            if ((f2 = IO.chooseFileWithDialog("dlgExportProfiles", "Comma Separated Values (*.csv)", new String[]{"txt"}, true)) != null) {
                if (!f2.getName().endsWith(".csv")) {
                    f2 = new File(f2.getAbsolutePath() + ".csv");
                }
                ClusterSetExporter.exportClusterSetAsCSV(f2, getSelectedClusterSets()[0]);

                JOptionPane.showMessageDialog(frmMain.getInstance(), "Export done!");
            }
        }
    }//GEN-LAST:event_pmiExportActionPerformed

    private void pmiStatsPerGroupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiStatsPerGroupActionPerformed
        /*JOptionPane.showMessageDialog(this, "This procedure will compute the statistics about the distribution \nof user-defined measurement groups to individual clusters \nand the average values of side-variables for each group in every cluster.\n"
         + "Next, you will be asked to provide a grouping file in tab-delimited format.\n Example:\n"
         + "Cell_Measurement_ID1 <tab-or-comma> Patient 1\n"
         + "Cell_Measurement_ID2 <tab-or-comma> Patient 1\n"
         + "Cell_Measurement_ID3 <tab-or-comma> Patient 2\n.........");*/
        //File f = frmMain.chooseFileWithDialog("GroupStatsPerCluster", "Tab- or Comma-delimited text", new String[]{"txt", "csv"}, false);
        Dataset ds = getSelectedClusterSets()[0].getDataset();
        Annotation ann = (Annotation) JOptionPane.showInputDialog(this, "Select annotation:", "Select Annotation", JOptionPane.QUESTION_MESSAGE, null, ds.getAnnotations(), null);
        if (ann == null) {
            return;
        }

        double euclLenThs = 0;//Double.parseDouble(JOptionPane.showInputDialog(this, "Disregard measurements with Eucl len less than", "2"));
        if (ann == null) {
            return;
        }

        try {
            DefaultTableModel otm = GroupStatisticsInClusters.getGroupStatistics(getSelectedClusterSets()[0].getClusters(), ann, false);
            JTable tab = new JTable(otm);
            tab.getTableHeader().setDefaultRenderer(new GlassTableHeader(tab.getTableHeader()));
            JFrame frm = new JFrame();
            frm.setTitle("Groups statistics per cluster");
            JScrollPane scr = new JScrollPane(tab);
            tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            tab.setTransferHandler(new TableTransferHandler());
            frm.getContentPane().add(scr);//, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
            frm.validate();
            frm.setBounds(100, 200, 800, 600);
            frm.setVisible(true);//JOptionPane.showMessageDialog(this, new JScrollPane());
        } catch (SQLException e) {
            logger.showException(e);
        }


    }//GEN-LAST:event_pmiStatsPerGroupActionPerformed

    private void pmiExportFCSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiExportFCSActionPerformed
        try {
            File f = IO.chooseFileWithDialog("ExportToFCS", "FCS file", new String[]{"fcs"}, toolbarVisible);
            Dataset ds = getSelectedClusterSets()[0].getDataset();
            ClusterSet cs = getSelectedClusterSets()[0];
            String[] btf = new String[]{"ASINH", "LOG", "NONE"};
            boolean sep = JOptionPane.showConfirmDialog(this, "Export clusters into separate files?", "Export type", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
            if (sep) {
                ClusterSetToFCSExporter.exportDatapointsSeparateClusters(ds, cs, f.getPath(), (Annotation) JOptionPane.showInputDialog(this, "Choose annotation to split the files", "Choose annotation", JOptionPane.QUESTION_MESSAGE, null, ds.getAnnotations(), ds.getAnnotations()[0]), (String) JOptionPane.showInputDialog(this, "Choose back-transformation", "Choose back-transformation", JOptionPane.QUESTION_MESSAGE, null, btf, btf[0]));

            } else {
                ClusterSetToFCSExporter.exportDatapoints(ds, cs, f.getPath(), (Annotation) JOptionPane.showInputDialog(this, "Choose annotation to split the files", "Choose annotation", JOptionPane.QUESTION_MESSAGE, null, ds.getAnnotations(), ds.getAnnotations()[0]), (String) JOptionPane.showInputDialog(this, "Choose back-transformation", "Choose back-transformation", JOptionPane.QUESTION_MESSAGE, null, btf, btf[0]));

            }
        } catch (SQLException | IOException e) {
            logger.showException(e);
        }
    }//GEN-LAST:event_pmiExportFCSActionPerformed

    private void pmiElbowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiElbowActionPerformed
        ClusterSet[] cs = getSelectedClusterSets();

        double[] x = new double[cs.length];
        double[] y = new double[cs.length];

        for (int i = 0; i < y.length; i++) {
            y[i] = cs[i].getNumberOfClusters();
            x[i] = cs[i].getMainClusteringParameterValue();
        }
        double bestX = LinePlusExponent.findElbowPointLinePlusExp(y, x);
        JOptionPane.showMessageDialog(this, "Elbow point is located at K = " + bestX);
    }//GEN-LAST:event_pmiElbowActionPerformed

    private void pmiExportIndexStringActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiExportIndexStringActionPerformed
        try {
            File f = IO.chooseFileWithDialog("pmiExportIndexStringActionPerformed", "Directories", null, true);
            ClusterSet[] css = getSelectedClusterSets();
            for (ClusterSet cs : css) {
                File dir = new File(f.getPath());//; + File.separator + "CS" + cs.getID());
                dir.mkdirs();

                Dataset ds = cs.getDataset();
                Annotation ann = (Annotation) JOptionPane.showInputDialog(this, "Select annotation:", "Select Annotation", JOptionPane.QUESTION_MESSAGE, null, ds.getAnnotations(), null);
                if (ann == null) {
                    return;
                }
                for (String term : ann.getTerms()) {
                    int[] pidT = ann.getDpIDsForTerm(term);
                    int[] cidArray = new int[pidT.length];
                    File out = new File(dir.getPath() + File.separator + term + "_assgnments.txt");
                    for (int cid = 0; cid < cs.getClusters().length; cid++) {
                        for (ClusterMember cm : cs.getClusters()[cid].getClusterMembers()) {
                            String[] s = cm.getDatapointName().split(" Event ");
                            if (s.length > 1) {
                                if (s[0].equals(term)) {
                                    cidArray[Integer.parseInt(s[1])] = cid;
                                }
                            } else {
                                cidArray[cm.getDatapoint().getID()] = cid;
                            }
                        }
                    }
                    BufferedWriter bw = new BufferedWriter(new FileWriter(out));
                    for (int i = 0; i < cidArray.length; i++) {
                        bw.write(cidArray[i] + 1 + "\n");
                    }
                    bw.flush();
                    bw.close();
                }
            }
        } catch (Exception e) {
            logger.showException(e);
        }
    }//GEN-LAST:event_pmiExportIndexStringActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private samusik.glasscmp.GlassButton cmdDeleteClusterSets;
    private samusik.glasscmp.GlassButton cmdNewClustering;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JMenu menuHierarchyPlot;
    private javax.swing.JMenuItem pmiAddToHierarchyPlot;
    private javax.swing.JMenuItem pmiCreateHierarchyPlot;
    private javax.swing.JMenuItem pmiElbow;
    private javax.swing.JMenuItem pmiExport;
    private javax.swing.JMenuItem pmiExportFCS;
    private javax.swing.JMenuItem pmiExportIndexString;
    private javax.swing.JMenuItem pmiPCAPlot;
    private javax.swing.JMenuItem pmiStatsPerGroup;
    private javax.swing.JMenu pmiValidation;
    private javax.swing.JPopupMenu popup;
    private javax.swing.JTable table;
    private samusik.glasscmp.GlassToolBar toolbar;
    // End of variables declaration//GEN-END:variables
}
