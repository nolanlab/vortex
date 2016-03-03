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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import clustering.Datapoint;
import vortex.main.TableCellEditorEx;
import clustering.DistanceMatrix;
import clustering.DistanceMeasure;
import main.Dataset;
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
    }
    private boolean toolbarVisible = true;
    private DefaultTableModel tm = null;

    public void selectDataset(String datasetName) {
        if (tm == null) {
            return;
        }
         for (int i = 0; i < tm.getRowCount(); i++) {
           if(((Dataset)tm.getValueAt(i, 0)).getName().equals(datasetName)) table.getSelectionModel().setSelectionInterval(i,i);
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
           if(tm.getValueAt(i, 0).equals(e.getSource())) tm.removeRow(i);
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

    public void loadDatasets(String[] dsIDs) {
        if (dsIDs == null) {
            return;
        }
        if(dll!=null){
            dll.cancel(true);
        }
        
        Arrays.sort(dsIDs);

        tm = new DefaultTableModel(new String[]{"Dataset Name", "Size", "Num. Dimensions"},0);
        
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
    }

    private class DatasetLazyLoader extends javax.swing.SwingWorker<String, Dataset> {

        double increment;
        double dProgress;
        private final String[] dsIDs;

        public DatasetLazyLoader(String[] dsIDs) {
            this.dsIDs = dsIDs;
            if(dsIDs.length>0) increment = 100 / dsIDs.length;
        }

        @Override
        public synchronized String doInBackground() {

            String[] s = dsIDs;
            for (int i = 0; i < s.length; i++) {
                if (this.isCancelled()) {
                    break;
                }
                String dsName = s[i];
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
                if(!this.isCancelled()){
                    tm.addRow(new Object[]{dataset, dataset.size(), dataset.getDimension()});
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
        jMenu1 = new javax.swing.JMenu();
        pmiCleanPrefetch = new javax.swing.JMenuItem();
        pmiRemoveGG = new javax.swing.JMenuItem();
        pmiCopyPanel = new javax.swing.JMenuItem();
        pmiSubset = new javax.swing.JMenuItem();
        pmiRandomSubset = new javax.swing.JMenuItem();
        pmiExportDS = new javax.swing.JMenuItem();
        pmiExportSimilarityMatrix = new javax.swing.JMenuItem();
        pmiFeatureSelection = new javax.swing.JMenuItem();
        pmiProperties = new javax.swing.JMenuItem();
        dlgSubset = new GlassDialog(frmMain.getInstance(), true);
        cmdCreateSubset = new samusik.glasscmp.GlassButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        lstPID = new javax.swing.JList();
        txtDsID = new samusik.glasscmp.GlassEdit();
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

        jMenu1.setText("Clean precomputed data...");

        pmiCleanPrefetch.setText("Delete everything");
        pmiCleanPrefetch.setToolTipText("");
        pmiCleanPrefetch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiCleanPrefetchActionPerformed(evt);
            }
        });
        jMenu1.add(pmiCleanPrefetch);

        pmiRemoveGG.setText("Delete Gabriel Graphs");
        pmiRemoveGG.setToolTipText("");
        pmiRemoveGG.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiRemoveGGActionPerformed(evt);
            }
        });
        jMenu1.add(pmiRemoveGG);

        dsbPopup.add(jMenu1);

        pmiCopyPanel.setText("Copy parameter panel");
        pmiCopyPanel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiCopyPanelActionPerformed(evt);
            }
        });
        dsbPopup.add(pmiCopyPanel);

        pmiSubset.setText("Create a subset");
        pmiSubset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiSubsetActionPerformed(evt);
            }
        });
        dsbPopup.add(pmiSubset);

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

        pmiFeatureSelection.setText("Feature Selection");
        pmiFeatureSelection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiFeatureSelectionActionPerformed(evt);
            }
        });
        dsbPopup.add(pmiFeatureSelection);

        pmiProperties.setText("Properties and annotations");
        pmiProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pmiPropertiesActionPerformed(evt);
            }
        });
        dsbPopup.add(pmiProperties);

        dlgSubset.setBounds(new java.awt.Rectangle(100, 100, 600, 400));
        dlgSubset.setMinimumSize(new java.awt.Dimension(600, 400));
        dlgSubset.getContentPane().setLayout(new java.awt.GridBagLayout());

        cmdCreateSubset.setText("Done");
        cmdCreateSubset.setMaximumSize(new java.awt.Dimension(70, 28));
        cmdCreateSubset.setMinimumSize(new java.awt.Dimension(70, 28));
        cmdCreateSubset.setPreferredSize(new java.awt.Dimension(70, 28));
        cmdCreateSubset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdCreateSubsetActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        dlgSubset.getContentPane().add(cmdCreateSubset, gridBagConstraints);

        jScrollPane2.setViewportView(lstPID);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        dlgSubset.getContentPane().add(jScrollPane2, gridBagConstraints);

        txtDsID.setText("Enter a new dataset ID");
        txtDsID.setMinimumSize(new java.awt.Dimension(200, 28));
        txtDsID.setPreferredSize(new java.awt.Dimension(200, 28));
        txtDsID.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                txtDsIDMousePressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        dlgSubset.getContentPane().add(txtDsID, gridBagConstraints);

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

        cmdNewDataset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/vortex/resources/icon-plus_32.png"))); // NOI18N
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

        cmdConfigureDatasetDisplay.setIcon(new javax.swing.ImageIcon(getClass().getResource("/vortex/resources/gear_32.png"))); // NOI18N
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

        cmdDeleteDataset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/vortex/resources/CloseHighlight.png"))); // NOI18N
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
    
     public Dataset[] getSelectedDatasets(){
        if (tm == null) {
            return null;
        }

        ArrayList<Dataset> al = new ArrayList<>(0);
        for (int idx = 0; idx < table.getRowCount(); idx++) {
            if (table.getSelectionModel().isSelectedIndex(idx)) {
               al.add((Dataset)table.getValueAt(idx, 0));
            }
        }
        return al.toArray(new Dataset[al.size()]);
    }

    private void cmdConfigureDatasetDisplayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdConfigureDatasetDisplayActionPerformed
        (new dlgConfiguration(frmMain.getInstance())).setVisible(true);
        loadDatasets(Config.getDatasetIDsForLoading());
    }//GEN-LAST:event_cmdConfigureDatasetDisplayActionPerformed

    private void cmdNewDatasetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdNewDatasetActionPerformed
        dlgImportDataset frm = new dlgImportDataset(frmMain.getInstance(), true);
        frm.setBounds(100, 100, frmMain.getInstance().getWidth() - 200, frmMain.getInstance().getHeight() - 200);
        frm.setVisible(true);
        loadDatasets(Config.getDatasetIDsForLoading());
    }//GEN-LAST:event_cmdNewDatasetActionPerformed

    private void cmdDeleteDatasetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdDeleteDatasetActionPerformed
        if (tm == null) {
            return;
        }
        final Dataset [] ds = getSelectedDatasets();
        table.getSelectionModel().setSelectionInterval(0, 0);
        if (ds.length > 0) {
            if (JOptionPane.showConfirmDialog(frmMain.getInstance(), "Are you sure you want to delete " + ds.length + " dataset(s)?\n All clustering data will be deleted as well.\nThis operation cannot be undone.", "Confirm dataset deletion", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                SwingWorker <Boolean, Dataset> sw = new SwingWorker<Boolean, Dataset>() {

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

    private void cmdCreateSubsetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdCreateSubsetActionPerformed
        if (ConnectionManager.getStorageEngine() == null) {
            throw new IllegalStateException("This command is not supported in MS SQL configuration. Use HSQLDB instead.");
        }
        if (txtDsID.getText().equals("Enter a new dataset ID")) {
            JOptionPane.showMessageDialog(this, "Enter a new Dataset ID", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (lstPID.getModel().getSize() == 0) {
            JOptionPane.showMessageDialog(this, "The ProfileID list is empty", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DefaultListModel dlm = (DefaultListModel) lstPID.getModel();

        ArrayList<Datapoint> dp = new ArrayList<>();
        
        if(getSelectedDatasets().length==0)return;
        
        Dataset ds = getSelectedDatasets()[0];

        for (int i = 0; i < dlm.getSize(); i++) {
            String pid = (String) dlm.getElementAt(i);
            Datapoint nd = ds.getDPbyName(pid).clone();
            nd.setID(i);
            dp.add(nd);
        }

        String dsID = txtDsID.getText();

        try {
            Dataset newDS = new Dataset(dsID, dp.toArray(new Datapoint[dp.size()]), ds.getFeatureNames(), ds.getSideVarNames());
            ConnectionManager.getStorageEngine().saveDataset(newDS, true);
            Config.addDatasetIDsForLoading(dsID);
        } catch (SQLException e) {
            logger.showException(e);
        }
        dlgSubset.setVisible(false);

    }//GEN-LAST:event_cmdCreateSubsetActionPerformed

    private void txtDsIDMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_txtDsIDMousePressed
        if (txtDsID.getText().equals("Enter a new dataset ID")) {
            txtDsID.setText("");
        }
    }//GEN-LAST:event_txtDsIDMousePressed

    private void pmiSubsetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiSubsetActionPerformed
        Dataset ds = this.getSelectedDatasets()[0];

        if (ds == null) {
            return;
        }

        File f = IO.chooseFileWithDialog("DatasetBrowser.CreateSubset", "Plain Text (*.csv, *.txt)", new String[]{"csv", "txt"}, false);

        if (f == null) {
            return;
        }

        try {
            ArrayList<String> al = IO.getListOfStringsFromStream(new FileInputStream(f));
            DefaultListModel dlm = new DefaultListModel();
            for (String s : al) {
                if (ds.getDPbyName(s) != null) {
                    dlm.addElement(s);
                } else {
                    System.err.println("NOT FOUND: " + s);
                }
            }
            lstPID.setModel(dlm);
            dlgSubset.setVisible(true);
        } catch (FileNotFoundException e) {
            logger.showException(e);
        }
    }//GEN-LAST:event_pmiSubsetActionPerformed

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

    private void pmiCleanPrefetchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiCleanPrefetchActionPerformed
        if (getSelectedDatasets() != null) {
            for (int i = 0; i < getSelectedDatasets().length; i++) {
                try {
                    ConnectionManager.getStorageEngine().clearPrecomputedData(getSelectedDatasets()[i].getID());
                } catch (SQLException e) {
                    logger.showException(e);
                }

            }

        }
    }//GEN-LAST:event_pmiCleanPrefetchActionPerformed

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

    private void pmiFeatureSelectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiFeatureSelectionActionPerformed
    }//GEN-LAST:event_pmiFeatureSelectionActionPerformed

    private void pmiExportDSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiExportDSActionPerformed
        File f = IO.chooseFileWithDialog("Export Dataset", "Directory", new String[]{}, true);
        if (f != null) {
            try {
                               for (Dataset ds : getSelectedDatasets()) {
                    File f2 = new File(f.getParent() + File.separator + ds.getName() + ".csv");
                    ds.writeToFile(f2, true, true);
                }
            } catch (IOException e) {
                logger.showException(e);
            }
        }
    }//GEN-LAST:event_pmiExportDSActionPerformed

    private void pmiRemoveGGActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiRemoveGGActionPerformed
        if (getSelectedDatasets() != null) {
            try {
               ConnectionManager.getStorageEngine().deleteVoronoiCells(getSelectedDatasets()[0].getID(), null);
            } catch (SQLException e) {
                logger.showException(e);
            }
        }
    }//GEN-LAST:event_pmiRemoveGGActionPerformed

    private void pmiRandomSubsetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pmiRandomSubsetActionPerformed
        if(getSelectedDatasets().length==0){
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
    private samusik.glasscmp.GlassButton cmdCreateSubset;
    private samusik.glasscmp.GlassButton cmdDeleteDataset;
    private samusik.glasscmp.GlassButton cmdNewDataset;
    private javax.swing.JDialog dlgSubset;
    private javax.swing.JPopupMenu dsbPopup;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JList lstPID;
    private javax.swing.JMenuItem pmiCleanPrefetch;
    private javax.swing.JMenuItem pmiCopyPanel;
    private javax.swing.JMenuItem pmiExportDS;
    private javax.swing.JMenuItem pmiExportSimilarityMatrix;
    private javax.swing.JMenuItem pmiFeatureSelection;
    private javax.swing.JMenuItem pmiProperties;
    private javax.swing.JMenuItem pmiRandomSubset;
    private javax.swing.JMenuItem pmiRemoveGG;
    private javax.swing.JMenuItem pmiSubset;
    private javax.swing.JTable table;
    private samusik.glasscmp.GlassToolBar toolbar;
    private samusik.glasscmp.GlassEdit txtDsID;
    // End of variables declaration//GEN-END:variables
}
