package vortex.gui;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import vortex.main.CorrelInfo;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import javax.swing.JTable;

/**
 *
 * @author Nikolay
 */
public class CorrelTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

    Double baseValue;
    Double currValue;
    String toolTipText;
    Object value;
    JTable tab;
    private static boolean renderDifference;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        tab = table;
        if (value instanceof CorrelInfo) {

            CorrelInfo tmp = (CorrelInfo) value;
            currValue = tmp.getTopValue();
            baseValue = tmp.getBottomValue();
            toolTipText = tmp.getToolTipText();
            return this;
        } else {
            currValue = null;
            baseValue = null;
            toolTipText = null;
            this.value = value;
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    public CorrelTableCellRenderer(boolean renderDifference) {
        this.renderDifference = renderDifference;
    }

    public boolean getRenderDifference() {
        return renderDifference;
    }

    public void setRenderDifference(boolean renderDifference) {
        CorrelTableCellRenderer.renderDifference = renderDifference;

    }

    @Override
    public String getToolTipText(MouseEvent arg0) {
        return toolTipText;
    }

    @Override
    protected void paintComponent(Graphics arg0) {

        ((Graphics2D) arg0).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        ((Graphics2D) arg0).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        arg0.setFont(tab.getFont());

        if (currValue != null) {
            if (Math.abs(currValue / baseValue) > 1) {
                arg0.setColor(new Color((int) (255.0 * (1.0 - (Math.abs(currValue) - Math.abs(baseValue)))), 255, (int) (255.0 * (1.0 - (Math.abs(currValue) - Math.abs(baseValue))))));
            } else {
                arg0.setColor(new Color(255, (int) (255.0 * (1.0 - (Math.abs(baseValue) - Math.abs(currValue)))), (int) (255.0 * (1.0 - (Math.abs(baseValue) - Math.abs(currValue))))));
            }

            arg0.fillRect(0, 0, getWidth(), getHeight());
            arg0.setColor(new Color(200, 200, 230, 127));
            arg0.drawRect(0, 0, getWidth(), getHeight());

            if (!renderDifference) {
                arg0.setColor(currValue > 0 ? new Color((int) (255.0 * (1.0 - currValue)), 255, (int) (255.0 * (1.0 - currValue))) : new Color(255, (int) (255.0 * (1.0 + currValue)), (int) (255.0 * (1.0 + currValue))));
                arg0.fillRect(0, 0, getWidth(), getHeight());
                Polygon p = new Polygon(new int[]{0, this.getWidth(), this.getWidth()}, new int[]{this.getHeight(), 0, this.getHeight()}, 3);
                arg0.setColor(baseValue > 0 ? new Color((int) (255.0 * (1.0 - baseValue)), 255, (int) (255.0 * (1.0 - baseValue))) : new Color(255, (int) (255.0 * (1.0 + baseValue)), (int) (255.0 * (1.0 + baseValue))));
                arg0.fillPolygon(p);
            }

            arg0.setColor(new Color(0, 0, 0, 150));
            arg0.drawRect(0, 0, getWidth(), getHeight());

            arg0.drawLine(0, this.getHeight(), this.getWidth(), 0);

            arg0.setColor(new Color(0, 0, 0));
            String str1 = Double.toString(currValue).substring(0, Math.min(Double.toString(currValue).length(), 4));
            String str2 = Double.toString(baseValue).substring(0, Math.min(Double.toString(baseValue).length(), 4));
            arg0.drawString(str1, 2, arg0.getFontMetrics().getAscent());
            arg0.drawString(str2, (this.getWidth() - arg0.getFontMetrics().stringWidth(str2)) - 1, this.getHeight() - 1);
        } else {
            super.paintComponent(arg0);
            if (value == null) {
                value = "null";
            }
            String str = value.toString();
            arg0.drawString(str, 1, arg0.getFontMetrics().getHeight() + 1);
        }

    }

    @Override
    protected void setValue(Object value) {
        if (value instanceof vortex.main.CorrelInfo) {
            CorrelInfo tmp = (CorrelInfo) value;
            currValue = tmp.getTopValue();
            baseValue = tmp.getBottomValue();
            toolTipText = tmp.getToolTipText();
        } else {
            currValue = null;
            baseValue = null;
            toolTipText = null;
            this.value = value;
        }
    }
}
