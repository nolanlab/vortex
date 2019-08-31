/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.util;

import java.io.IOException;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import util.logger;
import vortex.gui.dlgConfiguration;
import vortex.gui.frmMain;
import vortex.storage.HSQLDBStorageEngine;
import vortex.storage.HSQLDBStorageEngine2;
import vortex.storage.StorageEngine;

/**
 *
 * @author Nikolay
 */
public class ConnectionManager {

    private static DatabaseHost dbHost;
    private static StorageEngine storageEngine;

    public static vortex.storage.StorageEngine getStorageEngine() {
        try {
            String[] s = storageEngine.getAvailableDatasetNames();
        } catch (Exception e) {
            try {
                HSQLDBStorageEngine hs = new HSQLDBStorageEngine(dbHost);
                storageEngine = hs;
                if (hs.getSchemaVersion().startsWith("2.")) {
                    storageEngine = new HSQLDBStorageEngine2(dbHost);
                }
            } catch (SQLException | IOException ex) {
                logger.showException(ex);
            }
        }
        return storageEngine;
    }

    public static void setDatabaseHost(DatabaseHost dbHost) throws SQLException, IOException {
        if (dbHost == null) {
            throw new IllegalArgumentException("Cannot set database host to 'null'");
        }

        if (dbHost.type.equals(DatabaseHost.HOST_HSQLDB)) {
            if (storageEngine != null) {
                storageEngine.shutdown();
            }
            HSQLDBStorageEngine2 hs = new HSQLDBStorageEngine2(dbHost);
            storageEngine = hs;
            logger.print("Database version:" + hs.getSchemaVersion());
            if (hs.getSchemaVersion().startsWith("1.")) {
                storageEngine = new HSQLDBStorageEngine(dbHost);
            }
            storageEngine.getAvailableDatasetNames();
            ConnectionManager.dbHost = dbHost;
        }
    }

    public static void showDlgSelectDatabaseHost() {
        dlgConfiguration dlg = new dlgConfiguration(frmMain.getInstance());
        dlg.setVisible(true);
    }

    public static DatabaseHost getDatabaseHost() {
        return dbHost;
    }

    public static class DatabaseHost {

        public static final String HOST_POSTGRES = "PostgreSQL";
        public static final String HOST_HSQLDB = "HSQLDB";
        public final String type;
        public final String address;
        public final String instanceName;
        public final String username;
        public final String password;

        public DatabaseHost(String type, String address, String instanceName, String username, String password) {
            if (!(type.equals(HOST_HSQLDB) || type.equals(HOST_POSTGRES))) {
                throw new IllegalArgumentException("Unknown host type: " + type);
            }
            this.type = type;
            this.address = address;
            this.instanceName = instanceName;
            this.username = username;
            this.password = password;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof DatabaseHost) {
                DatabaseHost aH = (DatabaseHost) obj;
                return (this.type.equals(aH.type) && this.address.equals(aH.address) && this.instanceName.equals(aH.instanceName) && this.username.equals(aH.username));
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return type + " on " + address + "/" + instanceName + ", user:" + username;
        }
    }
}
