/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.main;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Nikolay
 */
public class CorrelInfo {

    double topValue;
    double bottomValue;
    String toolTipText;

    public CorrelInfo(double topValue, double bottomValue, String toolTipText) {
        this.topValue = topValue;
        this.bottomValue = bottomValue;
        this.toolTipText = toolTipText;
        JTable tab;
    }

    public String getToolTipText() {
        return toolTipText;
    }

    public Double getBottomValue() {
        return bottomValue;
    }

    public Double getTopValue() {
        return topValue;
    }
}
