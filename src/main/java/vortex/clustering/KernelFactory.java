/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * KernelFactory.java
 *
 * Created on 08-Oct-2009, 23:12:39
 */
package vortex.clustering;

import sandbox.clustering.DistanceMeasure;
import sandbox.clustering.EuclideanDistance;
import sandbox.clustering.AngularDistance;
import java.util.Arrays;
import javax.swing.DefaultComboBoxModel;
import javax.swing.SpinnerNumberModel;
import vortex.util.ClassWrapper;

/**
 *
 * @author Nikolay
 */
@SuppressWarnings("serial")
public class KernelFactory extends javax.swing.JPanel {

    double from, to;
    int steps;
    double step;
    DistanceMeasure dm;

    /**
     * Creates new form KernelFactory
     */
    public KernelFactory(DistanceMeasure dm) {
        initComponents();
        this.dm = dm;
        
        from = (Double) ((SpinnerNumberModel) spinFKFrom.getModel()).getNumber();
        to = (Double) ((SpinnerNumberModel) spinFKTo.getModel()).getNumber();
        steps = (Integer) ((SpinnerNumberModel) spinFKStep.getModel()).getNumber();
        step = ((double) to - (double) from) / (double) steps;

        spinMergeThs.setModel(new SpinnerNumberModel(0.95, dm.getSimilarityBounds()[0], dm.getSimilarityBounds()[1], 0.01));
        lblDistBounds.setText(dm.getName().replaceFirst("Distance", "Similarity") + ", range: " + Arrays.toString(dm.getSimilarityBounds()));
        if(dm instanceof EuclideanDistance)((DefaultComboBoxModel) cmbKernels.getModel()).addElement(new ClassWrapper(GaussianKernel.class));
        if(dm instanceof AngularDistance)((DefaultComboBoxModel) cmbKernels.getModel()).addElement(new ClassWrapper(vMFKernel.class));
        ((DefaultComboBoxModel) cmbKernels.getModel()).addElement(new ClassWrapper(NNNKernel.class));
    }

    public double getMergeThreshold() {
        return (Double) spinMergeThs.getValue();
    }

    @SuppressWarnings("unchecked")
    public Kernel[] getKernels() {
        try {
            from = (Double) ((SpinnerNumberModel) spinFKFrom.getModel()).getNumber();
            to = (Double) ((SpinnerNumberModel) spinFKTo.getModel()).getNumber();
            steps = (Integer) ((SpinnerNumberModel) spinFKStep.getModel()).getNumber();
            step = (steps == 1) ? 0 : ((double) to - (double) from) / (double) (steps - 1);
            Kernel[] out = new Kernel[(int) steps];
            for (int i = 0; i < steps; i++) {
                Double K = (from + (step * i));
                out[i] = (Kernel) (((ClassWrapper) cmbKernels.getSelectedItem()).item.getConstructor(DistanceMeasure.class).newInstance(dm));
                out[i].setBandwidth(K);
            }
            return out;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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

        spinFKFrom = new javax.swing.JSpinner();
        lblDistBounds = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        spinFKTo = new javax.swing.JSpinner();
        spinFKStep = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        cmbKernels = new samusik.glasscmp.GlassComboBox();
        jLabel11 = new javax.swing.JLabel();
        spinMergeThs = new javax.swing.JSpinner();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();

        setMinimumSize(new java.awt.Dimension(250, 90));
        setOpaque(false);
        setPreferredSize(new java.awt.Dimension(250, 90));
        setLayout(new java.awt.GridBagLayout());

        spinFKFrom.setModel(new javax.swing.SpinnerNumberModel(Double.valueOf(10.0d), Double.valueOf(0.0d), null, Double.valueOf(0.001d)));
        spinFKFrom.setMaximumSize(new java.awt.Dimension(60, 25));
        spinFKFrom.setMinimumSize(new java.awt.Dimension(60, 25));
        spinFKFrom.setPreferredSize(new java.awt.Dimension(60, 25));
        spinFKFrom.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinFKFromStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        add(spinFKFrom, gridBagConstraints);

        lblDistBounds.setForeground(new java.awt.Color(0, 51, 102));
        lblDistBounds.setText("<Distance bounds>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 5, 0, 0);
        add(lblDistBounds, gridBagConstraints);

        jLabel7.setForeground(new java.awt.Color(0, 51, 102));
        jLabel7.setText("From");
        jLabel7.setMaximumSize(new java.awt.Dimension(60, 14));
        jLabel7.setMinimumSize(new java.awt.Dimension(60, 14));
        jLabel7.setPreferredSize(new java.awt.Dimension(60, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 5, 2, 0);
        add(jLabel7, gridBagConstraints);

        jLabel8.setForeground(new java.awt.Color(0, 51, 102));
        jLabel8.setText("To");
        jLabel8.setMaximumSize(new java.awt.Dimension(60, 14));
        jLabel8.setMinimumSize(new java.awt.Dimension(60, 14));
        jLabel8.setPreferredSize(new java.awt.Dimension(60, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 2, 0);
        add(jLabel8, gridBagConstraints);

        spinFKTo.setModel(new javax.swing.SpinnerNumberModel(Double.valueOf(100.0d), Double.valueOf(0.001d), null, Double.valueOf(0.001d)));
        spinFKTo.setMaximumSize(new java.awt.Dimension(60, 25));
        spinFKTo.setMinimumSize(new java.awt.Dimension(60, 25));
        spinFKTo.setPreferredSize(new java.awt.Dimension(60, 25));
        spinFKTo.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinFKToStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        add(spinFKTo, gridBagConstraints);

        spinFKStep.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(10), Integer.valueOf(1), null, Integer.valueOf(1)));
        spinFKStep.setMaximumSize(new java.awt.Dimension(60, 25));
        spinFKStep.setMinimumSize(new java.awt.Dimension(60, 25));
        spinFKStep.setPreferredSize(new java.awt.Dimension(60, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        add(spinFKStep, gridBagConstraints);

        jLabel9.setForeground(new java.awt.Color(0, 51, 102));
        jLabel9.setText("Steps");
        jLabel9.setMaximumSize(new java.awt.Dimension(60, 14));
        jLabel9.setMinimumSize(new java.awt.Dimension(60, 14));
        jLabel9.setPreferredSize(new java.awt.Dimension(60, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 3, 7);
        add(jLabel9, gridBagConstraints);

        cmbKernels.setModel(new javax.swing.DefaultComboBoxModel(new String[0]));
        cmbKernels.setMinimumSize(new java.awt.Dimension(160, 27));
        cmbKernels.setPreferredSize(new java.awt.Dimension(160, 27));
        cmbKernels.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbKernelsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 3, 2, 6);
        add(cmbKernels, gridBagConstraints);

        jLabel11.setForeground(new java.awt.Color(0, 51, 102));
        jLabel11.setText("Bandwidth:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(6, 8, 7, 4);
        add(jLabel11, gridBagConstraints);

        spinMergeThs.setModel(new javax.swing.SpinnerNumberModel(Double.valueOf(0.95d), null, null, Double.valueOf(0.01d)));
        spinMergeThs.setMaximumSize(new java.awt.Dimension(60, 25));
        spinMergeThs.setMinimumSize(new java.awt.Dimension(60, 25));
        spinMergeThs.setPreferredSize(new java.awt.Dimension(60, 25));
        spinMergeThs.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinMergeThsStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 8);
        add(spinMergeThs, gridBagConstraints);

        jLabel12.setForeground(new java.awt.Color(0, 51, 102));
        jLabel12.setText("Kernel:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(10, 14, 5, 4);
        add(jLabel12, gridBagConstraints);

        jLabel13.setForeground(new java.awt.Color(0, 51, 102));
        jLabel13.setText("Merge clusters with similarity btw centers higher than:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 5, 0, 0);
        add(jLabel13, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void spinFKFromStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinFKFromStateChanged
        // ((SpinnerNumberModel) spinFKTo.getModel()).setMinimum((Integer) spinFKFrom.getValue());
}//GEN-LAST:event_spinFKFromStateChanged

    private void spinFKToStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinFKToStateChanged
        //((SpinnerNumberModel) spinFKFrom.getModel()).setMaximum((Integer) spinFKTo.getValue());
}//GEN-LAST:event_spinFKToStateChanged

    private void spinMergeThsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinMergeThsStateChanged
        // TODO add your handling code here:
    }//GEN-LAST:event_spinMergeThsStateChanged

    private void cmbKernelsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbKernelsActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cmbKernelsActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private samusik.glasscmp.GlassComboBox cmbKernels;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel lblDistBounds;
    private javax.swing.JSpinner spinFKFrom;
    private javax.swing.JSpinner spinFKStep;
    private javax.swing.JSpinner spinFKTo;
    private javax.swing.JSpinner spinMergeThs;
    // End of variables declaration//GEN-END:variables
}
