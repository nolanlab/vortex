/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * frmProfilePlot.java
 *
 * Created on 20-Nov-2009, 19:02:37
 */
package vortex.gui2;

import clustering.Cluster;
import clustering.ClusterMember;
import clustering.Dataset;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import samusik.glasscmp.GlassBorder;
import samusik.glasscmp.GlassFrame;
import java.util.Arrays;
import java.util.LinkedList;
import javax.swing.event.TableModelListener;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
//import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import annotations.Annotation;
import clustering.Datapoint;
import util.ColorPalette;
import util.MatrixOp;
import util.logger;

/**
 *
 * @author Nikolay
 */
@SuppressWarnings("serial")
public class frmProfilePlot extends GlassFrame {

    private JFreeChart chart;
    private DefaultCategoryDataset graphDS = new DefaultCategoryDataset();
    private DefaultCategoryDataset quantileDS = new DefaultCategoryDataset();
    private ChartPanel chartPane;
    private Dataset nd;
    /**
     * Creates new form frmProfilePlot
     */
    final int NUM_QUANT = 0;

    public frmProfilePlot(Dataset nd) {
        super();
        initComponents();
        this.nd = nd;

        chart = ChartFactory.createLineChart(
                "Profiles", // chart title
                "Parameters", // domain axis label
                "Value", // range axis label
                graphDS, // data
                PlotOrientation.VERTICAL, // orientation
                true, // include legend
                true, // tooltips
                false // urls
                );
        chartPane = new ChartPanel(chart, true, true, true, true, true);
        chart.setBackgroundPaint(new Color(255, 255, 255));
        chartPane.setOpaque(true);
        chartPane.setBorder(new GlassBorder());
        chartPane.setBackground(new Color(255, 255, 255, 255));
        chartPane.setInitialDelay(10);
        chart.getCategoryPlot().setBackgroundPaint(new Color(255, 255, 255));

        jSplitPane1.setRightComponent(chartPane);

        tabProfiles.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int[] selectedRows = tabProfiles.getSelectedRows();
                Arrays.sort(selectedRows);
                for (int i = 0; i < tabProfiles.getRowCount(); i++) {
                    if (Arrays.binarySearch(selectedRows, i) > -1) {
                        Color col = (Color) chart.getCategoryPlot().getRendererForDataset(graphDS).getSeriesPaint(i);
                        chart.getCategoryPlot().getRendererForDataset(graphDS).setSeriesPaint(i, new Color(col.getRed(), col.getGreen(), col.getBlue(), 255));
                        chart.getCategoryPlot().getRendererForDataset(graphDS).setSeriesItemLabelsVisible(i, true);
                        chart.getCategoryPlot().getRendererForDataset(graphDS).setSeriesItemLabelPaint(i, new Color(col.getRed(), col.getGreen(), col.getBlue(), 255));
                        chart.setTitle(graphDS.getRowKey(i).toString());
                    } else {
                        Color col = (Color) chart.getCategoryPlot().getRendererForDataset(graphDS).getSeriesPaint(i);
                        chart.getCategoryPlot().getRendererForDataset(graphDS).setSeriesPaint(i, new Color(col.getRed(), col.getGreen(), col.getBlue(), 20));
                        chart.getCategoryPlot().getRendererForDataset(graphDS).setSeriesItemLabelsVisible(i, false);
                    }
                }
            }
        });

        tabProfiles.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getColumn() == 0) {
                    int row = e.getFirstRow();
                    chart.getCategoryPlot().getRendererForDataset(graphDS).setSeriesPaint(row, (Color) tabProfiles.getValueAt(row, 0));
                }
            }
        });
        String[] s = nd.getFeatureNamesCombined();

        StackedBarRenderer r = new StackedBarRenderer();

        r.setSeriesPaint(0, new Color(0, 0, 0, 0));
        r.setSeriesPaint(1, new Color(0, 0, 0, 127));
        try {
            r.setShadowVisible(false);
        } catch (NoSuchMethodError e) {
            logger.print("check JFreeChart library version");
        }
        chart.getCategoryPlot().setRenderer(1, r);
        r.setItemMargin(0.0);
        chart.getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);
        chart.getCategoryPlot().getDomainAxis().setMaximumCategoryLabelWidthRatio(0.5f);
        chart.getLegend().setItemFont(chart.getLegend().getItemFont().deriveFont(9.0f));
        chart.getCategoryPlot().setDataset(1, quantileDS);
        LineAndShapeRenderer br = new LineAndShapeRenderer();
        chart.getCategoryPlot().setRenderer(0, br);
        chart.setBorderPaint(Color.WHITE);
        br.setItemMargin(0.1);
        tabProfiles.getTableHeader().setResizingAllowed(true);
        jSplitPane1.setDividerLocation(300);
    }
    private int batchesAdded = 0;

    public void addClusterMembers(ClusterMember[] batch) {
        return;
    }

    public void addCluster(Cluster c) {
        if (c == null) {
            return;
        }
        Color col = ColorPalette.NEUTRAL_PALETTE.getColor(batchesAdded++);
        for (int i = 0; i < tabProfiles.getRowCount(); i++) {
            if (tabProfiles.getValueAt(i, 1).equals(c.toString())) {
                return;
            }
        }
        for (int i = 0; i < nd.getFeatureNames().length; i++) {
            double [] avgVec = c.getMode().getVector();
            graphDS.addValue(avgVec[i], c.toString(), i + 1 + " " + nd.getFeatureNames()[i]);
        }
       
        if(nd.getSideVarNames()!=null){
        for (int i = 0; i < nd.getSideVarNames().length; i++) {
            double [] avgVec = c.getMode().getSideVector();
            graphDS.addValue(avgVec[i], c.toString(), (nd.getDimension() + i + 1) + " " + nd.getSideVarNames()[i]);
        }
        }
        chart.getCategoryPlot().getRendererForDataset(graphDS).setSeriesPaint(tabProfiles.getRowCount(), col);
        DefaultTableModel tm = (DefaultTableModel) tabProfiles.getModel();
        tm.addRow(new Object[]{col, c.toString()});
    }

    public void addCluster(Cluster c, Annotation ann) {
        if (c == null&& ann==null) {
            return;
        }
        for (String term : ann.getTerms()) {
            ArrayList<Integer> dpIDsAssayAL = new ArrayList<>();
            int[] pid = ann.getDpIDsForTerm(term);
            // int[] dpids = new int[pid.length];
            for (int i = 0; i < pid.length; i++) {
                Datapoint dp = ann.getBaseDataset().getDPByID(pid[i]);
                if (dp != null) {
                    dpIDsAssayAL.add(dp.getID());
                }
            }
            int[] dpIDsAssay = new int[dpIDsAssayAL.size()];
            for (int i = 0; i < dpIDsAssay.length; i++) {
                dpIDsAssay[i] = dpIDsAssayAL.get(i);
            }
            Arrays.sort(dpIDsAssay);
            double count = 0;
            double [] avgFeatureVec = new double[c.getMode().getVector().length];
            double [] avgSideVec = new double[c.getMode().getSideVector().length];
            try {
                LinkedList<Datapoint> dp = new LinkedList<>();
                for (ClusterMember cm : c.getClusterMembers()) {
                    if (Arrays.binarySearch(dpIDsAssay, cm.getDatapoint().getID()) >= 0) {
                        count++;
                        avgFeatureVec = MatrixOp.sum(avgFeatureVec, cm.getDatapoint().getVector());
                        avgSideVec = MatrixOp.sum(avgSideVec, cm.getDatapoint().getSideVector());
                        dp.add(cm.getDatapoint());
                    }
                }
                MatrixOp.mult(avgFeatureVec, 1.0 / (double) count);
                MatrixOp.mult(avgSideVec, 1.0 / (double) count);
                logger.print(term, Arrays.toString(avgFeatureVec), Arrays.toString(avgSideVec));
                if (count > 0) {
                String name = "id" + c.getID() + " (" + ((int)count) + ") - " + term;
                Cluster clus = new Cluster(dp.toArray(new Datapoint[dp.size()]), avgFeatureVec, avgSideVec, name, null);
                clus.setComment(name);
                clus.setClusterSet(c.getClusterSet());
                addCluster(clus);
                }
            } catch (SQLException e) {
                logger.showException(e);
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

        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        tabProfiles = new javax.swing.JTable();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        jSplitPane1.setBackground(new java.awt.Color(0,0,0,0));
        jSplitPane1.setBorder(null);
        jSplitPane1.setDividerLocation(300);
        jSplitPane1.setOpaque(false);
        jSplitPane1.setPreferredSize(new java.awt.Dimension(2000, 2000));
        jSplitPane1.setRequestFocusEnabled(false);

        jScrollPane1.setBorder(new samusik.glasscmp.GlassBorder());
        jScrollPane1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jScrollPane1KeyTyped(evt);
            }
        });

        tabProfiles.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Color", "ProfileID"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tabProfiles.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_NEXT_COLUMN);
        tabProfiles.setColumnSelectionAllowed(true);
        tabProfiles.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tabProfiles.getTableHeader().setReorderingAllowed(false);
        tabProfiles.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                tabProfilesKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                tabProfilesKeyTyped(evt);
            }
        });
        jScrollPane1.setViewportView(tabProfiles);
        tabProfiles.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        if (tabProfiles.getColumnModel().getColumnCount() > 0) {
            tabProfiles.getColumnModel().getColumn(0).setResizable(false);
            tabProfiles.getColumnModel().getColumn(0).setPreferredWidth(20);
            tabProfiles.getColumnModel().getColumn(0).setCellEditor(ColorCodeTableEditor.getInstance());
            tabProfiles.getColumnModel().getColumn(0).setCellRenderer(new ColorTableCellRenderer());
        }

        jSplitPane1.setLeftComponent(jScrollPane1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jSplitPane1, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jScrollPane1KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jScrollPane1KeyTyped
    }//GEN-LAST:event_jScrollPane1KeyTyped

    private void tabProfilesKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tabProfilesKeyTyped
    }//GEN-LAST:event_tabProfilesKeyTyped

    private void tabProfilesKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tabProfilesKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_DELETE||evt.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            int k = 0;
            DefaultTableModel tm = ((DefaultTableModel) tabProfiles.getModel());
            for (int i : tabProfiles.getSelectedRows()) {
                try {
                    graphDS.removeRow(i - k);
                } catch (java.lang.IndexOutOfBoundsException e) {
                }
                tm.removeRow(i - k);
                k++;
            }
            for (int i = 0; i < tabProfiles.getRowCount(); i++) {
                chart.getCategoryPlot().getRendererForDataset(graphDS).setSeriesPaint(i, (Color) tabProfiles.getValueAt(i, 0));
            }
            chartPane.invalidate();
            jSplitPane1.invalidate();
            jSplitPane1.repaint();
            chartPane.repaint();
        }
    }//GEN-LAST:event_tabProfilesKeyPressed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTable tabProfiles;
    // End of variables declaration//GEN-END:variables
}
