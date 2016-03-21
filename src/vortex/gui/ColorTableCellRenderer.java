/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JTable;

/**
 *
 * @author Nikolay
 */
public class ColorTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

    private int width = 0;
    private int height = 0;
    private Color bc = null;
    boolean selected = false;
    boolean hasFocus = false;
    Color selectedColor = new Color(120, 120, 120);

    public ColorTableCellRenderer() {
        super();
    }

    @Override
    public Component getTableCellRendererComponent(JTable arg0, Object arg1, boolean arg2, boolean arg3, int arg4, int arg5) {

        selected = arg2;

        hasFocus = arg3;

        return super.getTableCellRendererComponent(arg0, arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    protected void paintComponent(Graphics arg0) {
        super.paintComponent(arg0);

        if (bc != null) {
            Graphics2D g = (Graphics2D) arg0;
            width = this.getWidth();
            height = this.getHeight();


            g.setPaint(bc);
            g.fillRect(0, 0, width, height);
            //if(selected) {g.setPaint(selectedColor); g.fillRect(0,0,width,height);}

        }
    }

    @Override
    protected void setValue(Object value) {
        if (value instanceof Color) {
            bc = (Color) value;
        } else {
            bc = null;
            super.setValue(value);
        }
    }
}
