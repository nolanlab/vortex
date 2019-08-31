/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.Arrays;
import javax.swing.DefaultComboBoxModel;
import samusik.glasscmp.GlassFrame;
import sandbox.annotations.Annotation;
import sandbox.clustering.Cluster;
import sandbox.clustering.ClusterMember;
import sandbox.clustering.Datapoint;
import sandbox.clustering.Dataset;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import util.MatrixOp;
import util.Shuffle;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class frmBiaxialPlot extends GlassFrame implements PropertyChangeListener {

    /**
     * Creates new form frm2DScatter
     */
    private final Cluster[] clusters;
    private Datapoint[] dp;

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        flowScatter.img = flowScatter.generateImage();
        flowScatter.invalidate();
        flowScatter.validate();
        flowScatter.repaint();
    }

    /**
     * Creates new form frmProfilePlot
     *
     * @param clusters
     */
    public frmBiaxialPlot(Cluster[] clusters) {
        super();
        initComponents();

        for (Cluster c : clusters) {
            c.addPropertyChangeListener(this);
        }
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

        populateChart(cmbX.getSelectedIndex(), cmbY.getSelectedIndex(), 127);
        pack();
        flowScatter.invalidate();
        flowScatter.repaint();

    }

    Annotation currAnn;
    String currAnnTerm;

    private void populateChart(int x, int y, int alpha) {
        //ArrayList<String> al = new ArrayList<>();
        flowScatter.reset();

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
        flowScatter.addSeries(new FlowScatter.Series("background", new Color(150, 150, 150, 70), data, 0.36, FlowScatter.Series.Type.SCATTER));

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
            logger.print("populating cluster:" + c);
            flowScatter.addSeries((new FlowScatter.Series(c.toString(), new Paint() {
                @Override
                public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
                    return c.getColorCode().createContext(cm, deviceBounds, userBounds, xform, hints);
                }

                @Override
                public int getTransparency() {
                    return c.getColorCode().getTransparency();
                }

            }, data, 0.76, FlowScatter.Series.Type.SCATTER)));
            r++;
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
        flowScatter = new vortex.gui.FlowScatter();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        cmbY = new samusik.glasscmp.GlassComboBox();
        cmbTerm = new samusik.glasscmp.GlassComboBox();
        cmbX = new samusik.glasscmp.GlassComboBox();
        cmbAnn = new samusik.glasscmp.GlassComboBox();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        spinDot = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Biaxial Scatterplot");
        setUndecorated(false);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jPanel1.setLayout(new java.awt.BorderLayout());
        jPanel1.add(flowScatter, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jPanel1, gridBagConstraints);

        jLabel2.setText("Term:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(jLabel2, gridBagConstraints);

        jLabel3.setText("Subset by annotation:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(jLabel3, gridBagConstraints);

        cmbY.setMinimumSize(new java.awt.Dimension(200, 28));
        cmbY.setPreferredSize(new java.awt.Dimension(200, 28));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        getContentPane().add(cmbY, gridBagConstraints);

        cmbTerm.setMinimumSize(new java.awt.Dimension(200, 26));
        cmbTerm.setPreferredSize(new java.awt.Dimension(200, 26));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        getContentPane().add(cmbTerm, gridBagConstraints);

        cmbX.setMinimumSize(new java.awt.Dimension(200, 28));
        cmbX.setPreferredSize(new java.awt.Dimension(200, 28));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        getContentPane().add(cmbX, gridBagConstraints);

        cmbAnn.setMinimumSize(new java.awt.Dimension(200, 26));
        cmbAnn.setPreferredSize(new java.awt.Dimension(200, 26));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        getContentPane().add(cmbAnn, gridBagConstraints);

        jLabel4.setText("Y:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(jLabel4, gridBagConstraints);

        jLabel5.setText("X:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(jLabel5, gridBagConstraints);

        spinDot.setModel(new javax.swing.SpinnerNumberModel(1, 1, 10, 1));
        spinDot.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinDotStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        getContentPane().add(spinDot, gridBagConstraints);

        jLabel1.setText("Dot size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        getContentPane().add(jLabel1, gridBagConstraints);

        jLabel6.setText("<html>Gray dots show a representative sample of the total dataset</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 1, 0, 0);
        getContentPane().add(jLabel6, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void spinDotStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinDotStateChanged
        flowScatter.setScatterPointSize((int) spinDot.getValue());
    }//GEN-LAST:event_spinDotStateChanged
    private boolean colorsOn = true;

    /**
     * @param args the command line arguments
     */
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private samusik.glasscmp.GlassComboBox cmbAnn;
    private samusik.glasscmp.GlassComboBox cmbTerm;
    private samusik.glasscmp.GlassComboBox cmbX;
    private samusik.glasscmp.GlassComboBox cmbY;
    private vortex.gui.FlowScatter flowScatter;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSpinner spinDot;
    // End of variables declaration//GEN-END:variables
}
