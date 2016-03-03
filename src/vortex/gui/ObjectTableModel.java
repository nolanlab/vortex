/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.gui;

import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Nikolay
 */
public abstract class ObjectTableModel<T> extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private DefaultTableModel tm;
    private String[] columns;
    private ArrayList<T> al;

    @Override
    public int getRowCount() {
        return tm.getRowCount();
    }

    public void addObject(T c) {
        tm.addRow(toRow(c));
    }

    protected abstract Object[] toRow(T o);

    protected abstract String[] getColumnNames(T o);

    public void removeObject(T c) {
        int idx = al.indexOf(c);
        if (idx >= 0) {
            al.remove(idx);
            tm.removeRow(idx);
            fireTableRowsDeleted(idx, idx);
        }
    }

    public ObjectTableModel() {
        columns = new String[]{""};
        al = new ArrayList<T>();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return tm.getColumnClass(columnIndex);
    }

    @Override
    public int getColumnCount() {
        return tm.getColumnCount();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return tm.getValueAt(rowIndex, columnIndex);
    }
}
