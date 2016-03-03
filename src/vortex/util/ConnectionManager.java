/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import util.logger;
import vortex.gui.dlgSelectDatabaseHost;
import vss.HSQLDBStorageEngine;
import vss.StorageEngine;

/**
 *
 * @author Nikolay
 */
public class ConnectionManager {

    private static DatabaseHost dbHost;
    private static Connection storageConnection;
    private static Connection prefetchConnection;
    private static StorageEngine storageEngine;

     public static vss.StorageEngine getStorageEngine() {
        try {
            String[] s = storageEngine.getAvailableDatasetNames();
        } catch (SQLException e) {
            try {
                storageEngine = new HSQLDBStorageEngine(dbHost);
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
            if(storageEngine!=null){
                storageEngine.shutdown();
            }
            storageEngine = new HSQLDBStorageEngine(dbHost);
            ConnectionManager.dbHost = dbHost;
            storageConnection = null;
            prefetchConnection = null;
            return;
        }
    }

   

    public static void showDlgSelectDatabaseHost() {
        dlgSelectDatabaseHost dlg = new dlgSelectDatabaseHost(null, true);
        dlg.setBounds(100, 100, 500, 400);
        dlg.setVisible(true);
    }
    public static DatabaseHost getDatabaseHost() {
        return dbHost;
    }

    private static void initDefaultHost() {
        try {
            setDatabaseHost(Config.getDefaultDatabaseHost());
        } catch (Exception e) {
            logger.showException(e);
        }
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
            if (!(type.equals(HOST_HSQLDB) || type.equals(HOST_POSTGRES))){
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
