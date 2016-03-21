/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.main;

import java.sql.Connection;
import java.sql.SQLException;
import samusik.objecttable.AbstractRowRepresentable;

/**
 *
 * @author Nikolay
 */
public abstract class PersistentRowRepresentable extends AbstractRowRepresentable {

    private static String[] headerRow;
    private static boolean[] editibilityMask;
    private static String DBtableName;
    private static int idField;
    private static String[] DBcolumnNames;
    private Object[] rowData;
    private static Connection con;

    public abstract void deserializeFromDB();

    @Override
    public void updateValue(int col, Object value) {
        if (editibilityMask[col]) {
            if (value.getClass().equals(rowData[col].getClass())) {
                throw new IllegalArgumentException("The class of the supplied value doesn't match the class of the initial value");
            }
            try {
                con.setAutoCommit(false);
                con.createStatement().execute("UPDATE [" + DBtableName + "] SET [" + DBcolumnNames[col] + "] = " + ((value instanceof Number) ? "" : "'") + value.toString().replace("'", "''") + ((value instanceof Number) ? "" : "'") + " where [" + DBcolumnNames[idField] + "] = " + rowData[idField]);
                con.commit();
                this.deserializeFromDB();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String[] getHeaderRow() {
        return headerRow;
    }

    @Override
    public boolean[] getEditibilityMask() {
        return editibilityMask;
    }
}
