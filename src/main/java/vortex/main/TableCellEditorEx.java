/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.main;

import java.awt.Component;
import javax.swing.DefaultCellEditor;

import javax.swing.JTable;
import javax.swing.JTextField;

/**
 *
 * @author Kola
 */
public class TableCellEditorEx extends DefaultCellEditor {

    public int getLastChangedColumn() {
        return -1;
    }

    public int getLastChangedRow() {
        return -1;
    }

    public TableCellEditorEx() {
        super(new JTextField());
    }

    @Override
    protected void fireEditingCanceled() {

        super.fireEditingCanceled();
    }

    @Override
    public Component getTableCellEditorComponent(JTable arg0, Object arg1, boolean arg2, int arg3, int arg4) {
        if (!(arg1 instanceof String)) {
            return null;
        } else {

            return super.getTableCellEditorComponent(arg0, arg1, arg2, arg3, arg4);
        }
    }
}
