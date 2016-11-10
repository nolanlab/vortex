/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.gui2;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.Arrays;
import javax.swing.DefaultComboBoxModel;
import samusik.glasscmp.GlassFrame;
import annotations.Annotation;
import clustering.Cluster;
import clustering.ClusterMember;
import clustering.Datapoint;
import clustering.Dataset;
import util.ColorPalette;
import util.MatrixOp;
import util.Shuffle;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class frmBiaxialPlot extends GlassFrame {

    /**
     * Creates new form frm2DScatter
     */
    private final Cluster[] clusters;
    private Datapoint[] dp;

    /**
     * Creates new form frmProfilePlot
     *
     * @param clusters
     */
    public frmBiaxialPlot(Cluster[] clusters) {
        super();
        initComponents();

        this.clusters = clusters;
        Dataset d = clusters[0].getClusterSet().getDataset();
        int len = Math.min(10000, d.size());
        this.dp = Arrays.copyOf(new Shuffle<Datapoint>().shuffleCopyArray(d.getDatapoints()), len);

        DefaultComboBoxModel<String> cmb = new DefaultComboBoxModel<>();

        for (String s : d.getFeatureNamesCombined()) {
            cmb.addElement(s);
        }

        cmbX.setModel(cmb);

        cmb = new DefaultComboBoxModel<>();

        for (String s : d.getFeatureNamesCombined()) {
            cmb.addElement(s);
        }

        cmbY.setModel(cmb);

        cmbX.setSelectedIndex(0);
        cmbY.setSelectedIndex(1);
        ActionListener lis = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                populateChart(cmbX.getSelectedIndex(), cmbY.getSelectedIndex(), 127);
                flowScatter.invalidate();
                flowScatter.repaint();
            }
        };

        cmbY.addActionListener(lis);
        cmbX.addActionListener(lis);

        cmbAnn.setModel(new DefaultComboBoxModel<>(d.getAnnotations()));

        cmbAnn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Annotation ann = (Annotation) cmbAnn.getSelectedItem();
                if (ann == null) {
                    return;
                }
                cmbTerm.setModel(new DefaultComboBoxModel<>(ann.getTerms()));
                currAnn = ann;
                currAnnTerm = null;
            }
        });

        cmbTerm.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String term = (String) cmbTerm.getSelectedItem();
                if (term == null) {
                    return;
                }
                currAnnTerm = term;
                populateChart(cmbX.getSelectedIndex(), cmbY.getSelectedIndex(), 127);
                flowScatter.invalidate();
                flowScatter.repaint();
            }
        });
        pack();
        flowScatter.invalidate();
        flowScatter.repaint();

    }

    Annotation currAnn;
    String currAnnTerm;

    private void populateChart(int x, int y, int alpha) {
        //ArrayList<String> al = new ArrayList<>();
        flowScatter.reset();
        try {
            int r = 0;

            double[][] data = new double[dp.length][2];

            int k = 0;
            for (int i = 0; i < dp.length; i++) {
                if (currAnn != null && currAnnTerm != null) {
                    boolean contains = false;
                    for (String term : currAnn.getTermsForDpID(dp[i].getID())) {
                        contains |= term.equals(currAnnTerm);
                    }
                    if (!contains) {
                        continue;
                    }
                }
                double[] vec = MatrixOp.concat(dp[i].getVector(), dp[i].getSideVector());
                data[k][0] = vec[x];
                data[k][1] = vec[y];
                k++;

            }
            data = Arrays.copyOf(data, k);
            flowScatter.addSeries(new FlowScatter.Series("background", new Color(0, 0, 0, 70), data, 0.36, FlowScatter.Series.Type.SCATTER));

            int minClID = Integer.MAX_VALUE;
            
            for (Cluster c : clusters[0].getClusterSet().getClusters()) {
                minClID = Math.min(c.getID(), minClID);
            }
            
            for (Cluster c : clusters) {
                data = new double[c.size()][2];
                ClusterMember[] cm = c.getClusterMembers();
                k = 0;
                for (int i = 0; i < c.size(); i++) {
                    if (currAnn != null && currAnnTerm != null) {
                        boolean contains = false;
                        for (String term : currAnn.getTermsForDpID(cm[i].getDatapoint().getID())) {
                            contains |= term.equals(currAnnTerm);
                        }
                        if (!contains) {
                            continue;
                        }
                    }
                    double[] vec = MatrixOp.concat(cm[i].getDatapoint().getVector(), cm[i].getDatapoint().getSideVector());
                    data[k][0] = vec[x];
                    data[k][1] = vec[y];
                    k++;
                }
                data = Arrays.copyOf(data, k);
                Color col = ColorPalette.BRIGHT_PALETTE.getColor(r);
                logger.print("populating cluster:"+c);
                flowScatter.addSeries((new FlowScatter.Series(c.toString(), new Color(col.getRed(), col.getGreen(), col.getBlue(), 127), data, 0.76, FlowScatter.Series.Type.CONTOUR)));
                r++;
            }

        } catch (SQLException e) {
            logger.showException(e);
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

        jPanel1 = new javax.swing.JPanel();
        flowScatter = new vortex.gui2.FlowScatter();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        cmbY = new samusik.glasscmp.GlassComboBox();
        cmbTerm = new samusik.glasscmp.GlassComboBox();
        cmbX = new samusik.glasscmp.GlassComboBox();
        cmbAnn = new samusik.glasscmp.GlassComboBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tabProfiles = new javax.swing.JTable();
        jLabel6 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setUndecorated(false);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jPanel1.setLayout(new java.awt.BorderLayout());
        jPanel1.add(flowScatter, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jPanel1, gridBagConstraints);

        jLabel2.setText("Term:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        getContentPane().add(jLabel2, gridBagConstraints);

        jLabel3.setText("Annotation:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        getContentPane().add(jLabel3, gridBagConstraints);

        cmbY.setMinimumSize(new java.awt.Dimension(200, 28));
        cmbY.setPreferredSize(new java.awt.Dimension(200, 28));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(cmbY, gridBagConstraints);

        cmbTerm.setMinimumSize(new java.awt.Dimension(200, 26));
        cmbTerm.setPreferredSize(new java.awt.Dimension(200, 26));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 5, 5);
        getContentPane().add(cmbTerm, gridBagConstraints);

        cmbX.setMinimumSize(new java.awt.Dimension(200, 28));
        cmbX.setPreferredSize(new java.awt.Dimension(200, 28));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(cmbX, gridBagConstraints);

        cmbAnn.setMinimumSize(new java.awt.Dimension(200, 26));
        cmbAnn.setPreferredSize(new java.awt.Dimension(200, 26));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 2, 5);
        getContentPane().add(cmbAnn, gridBagConstraints);

        jLabel1.setText("Subset by:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 2;
        getContentPane().add(jLabel1, gridBagConstraints);

        jLabel4.setText("Y:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 2;
        getContentPane().add(jLabel4, gridBagConstraints);

        jLabel5.setText("X:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(jLabel5, gridBagConstraints);

        jScrollPane1.setMinimumSize(new java.awt.Dimension(200, 27));
        jScrollPane1.setPreferredSize(new java.awt.Dimension(200, 427));

        tabProfiles.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Cluster", "Color", "Type"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true
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
        tabProfiles.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
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
        tabProfiles.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jScrollPane1, gridBagConstraints);

        jLabel6.setText("Clusters:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        getContentPane().add(jLabel6, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void tabProfilesKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tabProfilesKeyPressed
       /* if (evt.getKeyCode() == KeyEvent.VK_DELETE||evt.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
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
        }*/
    }//GEN-LAST:event_tabProfilesKeyPressed

    private void tabProfilesKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tabProfilesKeyTyped

    }//GEN-LAST:event_tabProfilesKeyTyped
    private boolean colorsOn = true;

    /**
     * @param args the command line arguments
     */
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private samusik.glasscmp.GlassComboBox cmbAnn;
    private samusik.glasscmp.GlassComboBox cmbTerm;
    private samusik.glasscmp.GlassComboBox cmbX;
    private samusik.glasscmp.GlassComboBox cmbY;
    private vortex.gui2.FlowScatter flowScatter;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tabProfiles;
    // End of variables declaration//GEN-END:variables
}
