/*
 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.gui2.clusterdendrogram;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JPanel;
import clustering.ClusterSet;
import util.ClusterSetValidationFactory;
import util.ClusterSetValidationMeasure;
import java.lang.Math.*;
import javax.swing.BoxLayout;

/**
 *
 * @author Nikolay
 */
public class VerticalBarPlotPanel extends JPanel {

    private String caption;
    private int rowHeight;
    private static final int DEFAULT_WIDTH = 200;
    private VerticalBarPlotCell[] cells;
    private double[] valueRange;
    boolean plain = true;
    private ClusterSetValidationMeasure[] csvm;

    public VerticalBarPlotPanel(String caption, int rowHeight, ClusterSet[] clusterSets, ClusterSetValidationFactory csvf, Color plotColor) {
        initComponents();
        this.caption = caption;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        csvm = new ClusterSetValidationMeasure[clusterSets.length];
        valueRange = new double[2];
        valueRange[0] = 0;
        valueRange[1] = 0;

        for (int i = 0; i < clusterSets.length; i++) {
            csvm[i] = csvf.getClusterSetValidationMeasure(clusterSets[i]);
            valueRange[0] = Math.min(valueRange[0], csvm[i].getMeasure() * 1.1);
            valueRange[1] = Math.max(valueRange[1], csvm[i].getMeasure() * 1.1);
        }

        this.cells = new VerticalBarPlotCell[clusterSets.length];
        for (int i = 0; i < clusterSets.length; i++) {
            cells[i] = new VerticalBarPlotCell(caption, csvm[i].getMeasure(), valueRange);
            cells[i].setForeground(plotColor);
        }
        addCells();
    }

    /**
     * Creates a category label axis with plain black-n-white boxes
     */
    public VerticalBarPlotPanel(String caption, int rowHeight, double[] labels) {
        initComponents();
        this.caption = caption;
        this.rowHeight = rowHeight;
        this.plain = true;

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        csvm = new ClusterSetValidationMeasure[labels.length];
        valueRange = new double[2];
        valueRange[0] = 0;
        valueRange[1] = 0;

        for (int i = 0; i < labels.length; i++) {
            cells[i] = new VerticalBarPlotCell(caption, labels[i], null);
            cells[i].setForeground(Color.white);
        }
        addCells();
    }

    private void addCells() {
        for (int i = 0; i < cells.length; i++) {
            this.add(cells[i]);
            Dimension d = new Dimension(DEFAULT_WIDTH, this.rowHeight);
            cells[i].setMinimumSize(d);
            cells[i].setPreferredSize(d);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jToggleButton1 = new javax.swing.JToggleButton();

        setLayout(new java.awt.BorderLayout());

        jLabel1.setText("jLabel1");
        add(jLabel1, java.awt.BorderLayout.CENTER);

        jToggleButton1.setText("jToggleButton1");
        add(jToggleButton1, java.awt.BorderLayout.PAGE_START);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JToggleButton jToggleButton1;
    // End of variables declaration//GEN-END:variables
}
