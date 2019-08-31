/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

 /*
 * DatasetBrowser.java
 *
 * Created on 15-Sep-2010, 20:54:53
 */
package vortex.gui;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import samusik.glasscmp.GlassDialog;
import samusik.glasscmp.GlassTableHeader;
import samusik.objecttable.ObjectTableModel;
import samusik.objecttable.TableTransferHandler;
import sandbox.clustering.Datapoint;
import vortex.main.TableCellEditorEx;
import sandbox.clustering.DistanceMatrix;
import sandbox.clustering.DistanceMeasure;
import sandbox.clustering.Dataset;
import java.awt.Color;
import util.IO;
import util.Shuffle;
import vortex.util.ClassWrapper;
import vortex.util.Config;
import vortex.util.ConnectionManager;

import util.logger;

/**
 *
 * @author Nikolay
 */
public class DatasetBrowser extends javax.swing.JPanel implements DatasetDeletionListener {

    private static final long serialVersionUID = 1L;
    private DatasetLazyLoader dll = null;

    /**
     * Creates new form DatasetBrowser
     */
    public DatasetBrowser() {
        initComponents();
        //table.getTableHeader().setPreferredSize(new Dimension(100, Config.getHeaderHeight()));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(Color.LIGHT_GRAY);

    }
    private boolean toolbarVisible = true;
    private DefaultTableModel tm = null;

    public void selectDataset(String datasetName) {
        if (tm == null) {
            return;
        }
        if (tm.getRowCount() > 0) {
            table.getSelectionModel().removeSelectionInterval(0, tm.getRowCount() - 1);
        }
        for (int i = 0; i < tm.getRowCount(); i++) {
            if (((Dataset) tm.getValueAt(i, 0)).getName().equals(datasetName)) {
                table.getSelectionModel().setSelectionInterval(i, i);
            }
        }

    }

    public void setToolbarVisible(boolean toolbarVisible) {
        this.toolbarVisible = toolbarVisible;
        this.toolbar.setVisible(toolbarVisible);
    }

    public boolean isToolbarVisible() {
        return toolbarVisible;
    }

    @Override
    public void datasetDeleted(DatasetDeletionEvent e) {
        if (tm == null) {
            return;
        }
        for (int i = 0; i < tm.getRowCount(); i++) {
            if (tm.getValueAt(i, 0).equals(e.getSource())) {
                tm.removeRow(i);
                selectDataset(null);
            }

        }

    }

    public class DatasetSelectionEvent extends EventObject {

        private static final long serialVersionUID = 1L;
        Dataset ds[];

        @Override
        public Dataset[] getSource() {
            return ds;
        }

        public DatasetSelectionEvent(Dataset[] ds) {
            super(ds);
            this.ds = ds;
        }
    }

    public interface DatasetSelectionListener extends EventListener {

        public void datasetSelected(DatasetSelectionEvent evt);
    }

    public void addDatasetSelectionListener(DatasetSelectionListener listner) {
        selectionListeners.remove(listner);
        selectionListeners.add(listner);
    }

    public void removeDatasetSelectionListener(DatasetSelectionListener listner) {
        selectionListeners.remove(listner);
    }
    private ArrayList<DatasetSelectionListener> selectionListeners = new ArrayList<DatasetSelectionListener>();

    private void fireDatasetSelected(Dataset ds[]) {
        for (DatasetSelectionListener dsl : selectionListeners) {
            dsl.datasetSelected(new DatasetSelectionEvent(ds));
        }
    }

    public void loadDatasets(int[] dsIDs) {
        if (dsIDs == null) {
            return;
        }
        if (dll != null) {
            dll.cancel(true);
        }

        Arrays.sort(dsIDs);

        tm = new DefaultTableModel(new String[]{"Dataset Name", "Size", "Dim"}, 0);

        table.setModel(tm);
        TableCellEditorEx editor = new TableCellEditorEx();
        for (int k = 0; k < table.getColumnModel().getColumnCount(); k++) {
            table.getColumnModel().getColumn(k).setHeaderRenderer(new GlassTableHeader(table.getTableHeader()));
            table.getColumnModel().getColumn(k).setCellEditor(editor);
        }

        table.setModel(tm);

        //table.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        table.setColumnSelectionAllowed(false);
        table.setShowVerticalLines(true);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    //frmMain.getInstance().getProfilePopup().show(table, e.getX(), e.getY());
                }
            }
        });
        table.setTransferHandler(new TableTransferHandler());
        table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                Dataset[] ds = getSelectedDatasets();
                if (ds != null) {
                    DatasetBrowser.this.fireDatasetSelected(ds);
                }
            }
        });

        dll = new DatasetLazyLoader(dsIDs);
        dll.execute();

        table.getColumnModel().getColumn(0).setPreferredWidth(500);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(30);
        
        try{
            frmMain.getInstance().cssb.setClusterSets(new int[0]);
        }catch(NullPointerException e){
            
        }
    }

    private class DatasetLazyLoader extends javax.swing.SwingWorker<String, Dataset> {

        double increment;
        double dProgress;
        private final int[] dsIDs;

        public DatasetLazyLoader(int[] dsIDs) {
            this.dsIDs = dsIDs;
            if (dsIDs.length > 0) {
                increment = 100 / dsIDs.length;
            }
        }

        @Override
        public synchronized String doInBackground() {

            int[] s = dsIDs;
            for (int i = 0; i < s.length; i++) {
                if (this.isCancelled()) {
                    break;
                }
                int dsName = s[i];
                Dataset ds = null;
                try {
                    ds = ConnectionManager.getStorageEngine().loadDataset(dsName);
                } catch (Exception e) {
                    logger.showException(e);
                }
                dProgress += increment;
                this.setProgress((int) dProgress);
                if (ds == null) {
                    continue;
                }
                if (!this.isCancelled()) {
                    publish(ds);
                }
            }
            this.setProgress(100);
            return "Loading Done";
        }

        @Override
        protected void process(List<Dataset> chunks) {
            for (Dataset dataset : chunks) {
                if (!this.isCancelled()) {
                    tm.addRow(new Object[]{dataset, dataset.size(), dataset.getNumDimensions()});
                }
            }
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

        dsbPopup = new javax.swing.JPopupMenu();
        pmiCopyPanel = new javax.swing.JMenuItem();
        pmiRandomSubset = new javax.swing.JMenuItem();
        pmiExportDS = new javax.swing.JMenuItem();
        pmiExportSimilarityMatrix = new javax.swing.JMenuItem();
        pmiProperties = new javax.swing.JMenuItem();
        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable(){

            @Override
            public int getColumnCount() {
                //Thread.currentThread().dumpStack();
                if(getModel() instanceof ObjectTableModel){
                    return ((ObjectTableModel)getModel()).getColumnCount();

                }else{
                    return super.getColumnCount();
                }
            }

            @Override
            public Object getValueAt(int row, int column) {
                if(row >= getModel().getRowCount()) return null;
                if(column >= getModel().getColumnCount()) return null;
                return super.getValueAt(row, column);
            }

        };
        toolbar = new samusik.glasscmp.GlassToolBar();
        cmdNewDataset = new samusik.glasscmp.GlassButton();
        cmdConfigureDatasetDisplay = new samusik.glasscmp.GlassButton();
        cmdDeleteDataset = new samusik.glasscmp.GlassButton();
        jLabel1 = new javax.swing.JLabel();

        pmiCopyPanel.setText("Copy parameter panel");
        pmiCopyPanel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiCopyPanelActionPerformed(evt);
            }
        });
        dsbPopup.add(pmiCopyPanel);

        pmiRandomSubset.setText("Create Random Subset");
        pmiRandomSubset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiRandomSubsetActionPerformed(evt);
            }
        });
        dsbPopup.add(pmiRandomSubset);

        pmiExportDS.setText("Export Dataset");
        pmiExportDS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiExportDSActionPerformed(evt);
            }
        });
        dsbPopup.add(pmiExportDS);

        pmiExportSimilarityMatrix.setText("Export Similarity Matrix");
        pmiExportSimilarityMatrix.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiExportSimilarityMatrixActionPerformed(evt);
            }
        });
        dsbPopup.add(pmiExportSimilarityMatrix);

        pmiProperties.setText("Properties and annotations");
        pmiProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiPropertiesActionPerformed(evt);
            }
        });
        dsbPopup.add(pmiProperties);

        setLayout(new java.awt.GridBagLayout());

        jScrollPane1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jScrollPane1MouseClicked(evt);
            }
        });

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        table.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                tableMousePressed(evt);
            }
        });
        jScrollPane1.setViewportView(table);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(jScrollPane1, gridBagConstraints);

        toolbar.setMinimumSize(new java.awt.Dimension(80, 50));
        toolbar.setPreferredSize(new java.awt.Dimension(80, 50));
        toolbar.setLayout(new java.awt.GridBagLayout());

        cmdNewDataset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon-plus_32.png"))); // NOI18N
        cmdNewDataset.setToolTipText("New dataset");
        cmdNewDataset.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cmdNewDataset.setIconTextGap(0);
        cmdNewDataset.setMargin(new java.awt.Insets(2, 2, 2, 2));
        cmdNewDataset.setMaximumSize(new java.awt.Dimension(36, 35));
        cmdNewDataset.setMinimumSize(new java.awt.Dimension(36, 35));
        cmdNewDataset.setPreferredSize(new java.awt.Dimension(36, 35));
        cmdNewDataset.setVerifyInputWhenFocusTarget(false);
        cmdNewDataset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdNewDatasetActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(7, 8, 7, 0);
        toolbar.add(cmdNewDataset, gridBagConstraints);

        cmdConfigureDatasetDisplay.setIcon(new javax.swing.ImageIcon(getClass().getResource("/gear_32.png"))); // NOI18N
        cmdConfigureDatasetDisplay.setToolTipText("Select datasets to display");
        cmdConfigureDatasetDisplay.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cmdConfigureDatasetDisplay.setIconTextGap(0);
        cmdConfigureDatasetDisplay.setMargin(new java.awt.Insets(2, 2, 2, 2));
        cmdConfigureDatasetDisplay.setMaximumSize(new java.awt.Dimension(36, 35));
        cmdConfigureDatasetDisplay.setMinimumSize(new java.awt.Dimension(36, 35));
        cmdConfigureDatasetDisplay.setPreferredSize(new java.awt.Dimension(36, 35));
        cmdConfigureDatasetDisplay.setVerifyInputWhenFocusTarget(false);
        cmdConfigureDatasetDisplay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdConfigureDatasetDisplayActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(7, 4, 7, 7);
        toolbar.add(cmdConfigureDatasetDisplay, gridBagConstraints);

        cmdDeleteDataset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Close.png"))); // NOI18N
        cmdDeleteDataset.setToolTipText("Delete dataset");
        cmdDeleteDataset.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cmdDeleteDataset.setIconTextGap(0);
        cmdDeleteDataset.setMargin(new java.awt.Insets(2, 2, 2, 2));
        cmdDeleteDataset.setMaximumSize(new java.awt.Dimension(36, 35));
        cmdDeleteDataset.setMinimumSize(new java.awt.Dimension(36, 35));
        cmdDeleteDataset.setPreferredSize(new java.awt.Dimension(36, 35));
        cmdDeleteDataset.setVerifyInputWhenFocusTarget(false);
        cmdDeleteDataset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdDeleteDatasetActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(7, 4, 7, 0);
        toolbar.add(cmdDeleteDataset, gridBagConstraints);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(16, 140, 171));
        jLabel1.setText("Datasets");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        toolbar.add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 0, 0);
        add(toolbar, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    public Dataset[] getSelectedDatasets() {
        if (tm == null) {
            return null;
        }
        ArrayList<Dataset> al = new ArrayList<>(0);
        for (int idx = 0; idx < table.getRowCount(); idx++) {
            if (table.getSelectionModel().isSelectedIndex(idx)) {
                al.add((Dataset) table.getValueAt(idx, 0));
            }
        }
        return al.toArray(new Dataset[al.size()]);
    }

    private void cmdConfigureDatasetDisplayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdConfigureDatasetDisplayActionPerformed
        (new dlgConfiguration(frmMain.getInstance())).setVisible(true);
        try {
            loadDatasets(ConnectionManager.getStorageEngine().getAvailableDatasetIDs());
        } catch (SQLException e) {
            logger.showException(e);
        }
    }//GEN-LAST:event_cmdConfigureDatasetDisplayActionPerformed

    private void cmdNewDatasetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdNewDatasetActionPerformed
        try{
        if(ConnectionManager.getStorageEngine().getSchemaVersion().startsWith("1.")){
            JOptionPane.showMessageDialog(frmMain.getInstance(), "Warning: you have selected a database that was created in an older version (1.x). This database format is deprecated and may not be fully supported in the future.\n While you can safely browse the existing data, you cannot import new datasets or create analyses. \n Please setup a new database connection, which will be automatically created in a new format.");
            return;
        }
        }catch(SQLException e){
            logger.showException(e);
        }
        dlgImportDataset frm = new dlgImportDataset(frmMain.getInstance(), true);
        frm.setBounds(100, 100, frmMain.getInstance().getWidth() - 200, frmMain.getInstance().getHeight() - 200);
        frm.setVisible(true);
        try {
            loadDatasets(ConnectionManager.getStorageEngine().getAvailableDatasetIDs());
        } catch (SQLException e) {
            logger.showException(e);
        }
    }//GEN-LAST:event_cmdNewDatasetActionPerformed

    private void cmdDeleteDatasetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdDeleteDatasetActionPerformed
        if (tm == null) {
            return;
        }
        final Dataset[] ds = getSelectedDatasets();
        //table.getSelectionModel().setSelectionInterval(0, 0);
        if (ds.length > 0) {
            if (JOptionPane.showConfirmDialog(frmMain.getInstance(), "Are you sure you want to delete " + ds.length + " dataset(s)?\n All clustering data will be deleted as well.\nThis operation cannot be undone.", "Confirm dataset deletion", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                SwingWorker<Boolean, Dataset> sw = new SwingWorker<Boolean, Dataset>() {

                    @Override
                    protected Boolean doInBackground() throws Exception {
                        for (Dataset d : ds) {
                            ConnectionManager.getStorageEngine().deleteDataset(d);
                            publish(d);
                        }
                        return true;
                    }

                    @Override
                    protected void process(List<Dataset> chunks) {
                        for (Dataset chunk : chunks) {
                            datasetDeleted(new DatasetDeletionEvent(chunk));
                        }
                    }

                };
                sw.execute();
            }
        }
    }//GEN-LAST:event_cmdDeleteDatasetActionPerformed
    JFileChooser fc = new JFileChooser();

    private void jScrollPane1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jScrollPane1MouseClicked
    }//GEN-LAST:event_jScrollPane1MouseClicked

    private void tableMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMousePressed
        if (evt.getButton() == MouseEvent.BUTTON3) {
            int idx = table.rowAtPoint(evt.getPoint());
            if (!table.isRowSelected(idx)) {
                table.getSelectionModel().setSelectionInterval(idx, idx);
            }
            dsbPopup.show(table, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_tableMousePressed

    private void pmiPropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiPropertiesActionPerformed
        if (getSelectedDatasets() != null) {
            dlgDatasetProperties dlg = new dlgDatasetProperties(getSelectedDatasets()[0], frmMain.getInstance(), false);
            dlg.setBounds(200, 200, 1200, 800);
            dlg.setAlwaysOnTop(false);
            dlg.setVisible(true);
        }
    }//GEN-LAST:event_pmiPropertiesActionPerformed

    private void pmiExportSimilarityMatrixActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiExportSimilarityMatrixActionPerformed
        if (getSelectedDatasets() == null) {
            return;
        }

        final File f = IO.chooseFileWithDialog("Export Distance Matrix", "Text file, tab-delimited (*.txt)", new String[]{"txt"}, true);

        if (f == null) {
            return;
        }
        final Dataset nd = getSelectedDatasets()[0];

        Class<? extends DistanceMeasure>[] dmClasses = DistanceMeasure.getAvailableSubclasses();

        ClassWrapper[] c = new ClassWrapper[dmClasses.length];

        for (int i = 0; i < c.length; i++) {
            c[i] = new ClassWrapper(dmClasses[i]);
        }

        final Class<? extends DistanceMeasure> dmClass = ((vortex.util.ClassWrapper) JOptionPane.showInputDialog(null, "Available Distance Measures: ", "Choose distance measure:", JOptionPane.QUESTION_MESSAGE, null, c, null)).getItem();

        final JFrame jf = new JFrame();
        JProgressBar pb = new JProgressBar();
        pb.setIndeterminate(true);
        pb.setString("Computing similarity matrix...");
        jf.setTitle("Computing similarity matrix...");
        jf.setAlwaysOnTop(true);
        jf.add(pb);
        jf.setBounds(300, 300, 300, 100);
        jf.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        jf.setVisible(true);
        new Thread() {
            @Override
            public void run() {
                try {
                    DistanceMeasure dm = dmClass.getConstructor().newInstance();
                    DistanceMatrix dmtx = new DistanceMatrix(nd, dm);
                    jf.setVisible(false);
                    dmtx.writeToFile(f);
                } catch (Exception e) {
                    logger.showException(e);
                }
            }
        }.start();
    }//GEN-LAST:event_pmiExportSimilarityMatrixActionPerformed

    private void pmiExportDSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiExportDSActionPerformed
        File f = IO.chooseFileWithDialog("Export Dataset", "Directory", new String[]{}, true);
        if(!f.isDirectory()){
            f = f.getParentFile();
        }
        if (f != null) {
            try {
                for (Dataset ds : getSelectedDatasets()) {

                    File f2 = new File( f.getAbsolutePath()+ File.separator + ds.getName() + ".csv");
                    ds.writeToFile(f2, true, true);
                }
            } catch (IOException e) {
                logger.showException(e);
            }
        }
    }//GEN-LAST:event_pmiExportDSActionPerformed

    private void pmiRandomSubsetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiRandomSubsetActionPerformed
        if (getSelectedDatasets().length == 0) {
            return;
        }
        int size = Integer.parseInt(JOptionPane.showInputDialog("How many datapoints?", getSelectedDatasets()[0].size()));
        String name = JOptionPane.showInputDialog("New dataset name", getSelectedDatasets()[0].getName() + "_subset_" + size);
        assert (size <= getSelectedDatasets()[0].size());
        assert (!name.equals(getSelectedDatasets()[0].getName()));
        Shuffle<Datapoint> s = new Shuffle<>();
        logger.print("Shuffling");
        Datapoint[] shuffled = s.shuffleCopyArray(getSelectedDatasets()[0].getDatapoints());
        Datapoint[] subset = Arrays.copyOf(shuffled, size);

        for (int i = 0; i < subset.length; i++) {
            if (i % 10000 == 0) {
                logger.print("Cloning:" + i);
            }
            subset[i] = subset[i].clone();
            subset[i].setID(i);
        }
        logger.print("Creating dataset");

        Dataset nd = new Dataset(name, subset, getSelectedDatasets()[0].getFeatureNames(), getSelectedDatasets()[0].getSideVarNames());
        try {
            logger.print("Saving dataset");
            ConnectionManager.getStorageEngine().saveDataset(nd, true);
            Config.addDatasetIDsForLoading(name);
        } catch (SQLException e) {
            logger.showException(e);
        }
        if (JOptionPane.showConfirmDialog(this, "Do you want to save the rest of datapoints into another dataset?") == JOptionPane.YES_OPTION) {
            Datapoint[] subset2 = Arrays.copyOfRange(shuffled, size, shuffled.length);
            for (int i = 0; i < subset2.length; i++) {
                subset2[i] = subset2[i].clone();
                subset2[i].setID(i);
            }
            name += "_rest";
            Dataset nd2 = new Dataset(name, subset2, getSelectedDatasets()[0].getFeatureNames(), getSelectedDatasets()[0].getSideVarNames());
            try {
                ConnectionManager.getStorageEngine().saveDataset(nd2, true);
                Config.addDatasetIDsForLoading(name);
                //this.loadDatasets(dsIDs);
            } catch (SQLException e) {
                logger.showException(e);
            }
        }

    }//GEN-LAST:event_pmiRandomSubsetActionPerformed

    private void pmiCopyPanelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiCopyPanelActionPerformed
        Clipboard clipboard = getToolkit().getSystemClipboard();
        clipboard.setContents(getSelectedDatasets()[0].getPanel(), new ClipboardOwner() {
            @Override
            public void lostOwnership(Clipboard clipboard, Transferable contents) {
                logger.print("Lost ownership");
            }
        });

    }//GEN-LAST:event_pmiCopyPanelActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private samusik.glasscmp.GlassButton cmdConfigureDatasetDisplay;
    private samusik.glasscmp.GlassButton cmdDeleteDataset;
    private samusik.glasscmp.GlassButton cmdNewDataset;
    private javax.swing.JPopupMenu dsbPopup;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JMenuItem pmiCopyPanel;
    private javax.swing.JMenuItem pmiExportDS;
    private javax.swing.JMenuItem pmiExportSimilarityMatrix;
    private javax.swing.JMenuItem pmiProperties;
    private javax.swing.JMenuItem pmiRandomSubset;
    private javax.swing.JTable table;
    private samusik.glasscmp.GlassToolBar toolbar;
    // End of variables declaration//GEN-END:variables
}
