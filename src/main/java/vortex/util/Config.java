/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.util;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.util.ArrayList;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import util.logger;
import vortex.gui.BarCodeTableCellRenderer;

/**
 *
 * @author Nikolay
 */
public abstract class Config {

    private final static String DELIMITER = "\t";
    private final static String DELIMITER2 = ",";
    private final static Preferences prefRoot = Preferences.userRoot().node("vtshift/prefs");
    private final static Preferences prefBarcodes = prefRoot.node("Barcodes");
    private static final String KEY_BARCODE_TYPE = "BARCODE_TYPE";
    private static final String KEY_BARCODE_COLOR_UP = "BARCODE_COLOR_UP";
    private static final String KEY_BARCODE_COLOR_DOWN = "BARCODE_COLOR_DOWN";
    private static final String KEY_BARCODE_POS_NEG_RATIO = "BARCODE_POS_NEG_RATIO";
    private final static Preferences prefDBHosts = prefRoot.node("vtshift/DBhosts");
    private static final String KEY_DS = "DatasetsForLoading";
    private static final String KEY_DEF_DBHOST = "DefaultDatabaseHost";
    private static final String KEY_DB_HOSTS = "DatabaseHosts";
    private static final String KEY_BARCODE_RAINBOW = "BARCODE_RAINBOW";
    private static boolean developmentMode = false;
    private final static Preferences prefFileChooserPaths = prefRoot.node("FilePaths");

    private static final int HEADER_HEIGHT = 20;

    public static int getHeaderHeight() {
        return HEADER_HEIGHT;
    }

    public static File getFilePath(String FilePathID) {
        String path = prefFileChooserPaths.get(FilePathID, "def");
        if (path.equals("def")) {
            return null;
        } else {
            return new File(path);
        }
    }

    public static boolean showWeightedAvgClusterProfiles() {
        return false;
    }

    public static void putFilePath(String FilePathID, File f) {
        try {
            prefFileChooserPaths.put(FilePathID, f.getCanonicalPath());
        } catch (IOException e) {
            logger.showException(e);
        }
    }

    private static Preferences getDBHostSpecificPreferences() {
        ConnectionManager.DatabaseHost host = ConnectionManager.getDatabaseHost();
        if (host != null) {
            String s = host.toString().replaceAll("\\\\+", Matcher.quoteReplacement("\\")).replaceAll("/+", "/");
            if (s.length() > AbstractPreferences.MAX_NAME_LENGTH) {
                s = String.valueOf(s.hashCode());
            }
            return prefDBHosts.node(s);
        } else {
            throw new IllegalStateException("There's no current database host connection");
        }
    }

    public static boolean isDevelopmentMode() {
        return developmentMode;
    }

    public static void setDevelopmentMode(boolean devMode) {
        developmentMode = devMode;
    }

    public static void setDatasetIDsForLoading(String[] dsIDs) {
        String s = "";
        for (int i = 0; i < dsIDs.length; i++) {
            s += dsIDs[i] + ((i == dsIDs.length - 1) ? "" : DELIMITER);
        }
        getDBHostSpecificPreferences().put(KEY_DS, s);
    }

    public static void addDatasetIDsForLoading(String dsID) {
        Preferences pref = getDBHostSpecificPreferences();
        String s = pref.get(KEY_DS, "def");
        if (s.equals("def") || s.trim().equals("")) {
            setDatasetIDsForLoading(new String[]{dsID});
        } else if (!s.contains(DELIMITER + s + DELIMITER)) {
            s += DELIMITER + dsID;
            pref.put(KEY_DS, s);
        }
    }

    public static void setDefaultDatabaseHost(ConnectionManager.DatabaseHost host) {
        if (host == null) {
            prefDBHosts.put(KEY_DEF_DBHOST, "");
            return;
        }
        String hostDesc = hostToString(host);
        prefDBHosts.put(KEY_DEF_DBHOST, hostDesc);
    }

    public static ConnectionManager.DatabaseHost getDefaultDatabaseHost() {
        String s = prefDBHosts.get(KEY_DEF_DBHOST, "def");
        if (s.equals("def") || s.length() < 5) {
            if (getAvailableDatabaseHosts().length > 0) {
                setDefaultDatabaseHost(getAvailableDatabaseHosts()[0]);
                return getAvailableDatabaseHosts()[0];
            } else {
                return null;
            }
        } else {
            try {
                ConnectionManager.DatabaseHost dbHost = makeHostFromString(s);
                return dbHost;
            } catch (Exception e) {
                setDefaultDatabaseHost(null);
                throw new IllegalStateException("Default database host '" + s + "' is not available. Please specify a different host.", e);
            }
        }
    }

    public static ConnectionManager.DatabaseHost[] getAvailableDatabaseHosts() {
        String s = prefDBHosts.get(KEY_DB_HOSTS, "def").trim();
        if (s.equals("def") || s.length() <= 5) {
            return new ConnectionManager.DatabaseHost[0];
        } else {
            String[] s2 = s.split(DELIMITER);
            ArrayList<ConnectionManager.DatabaseHost> al = new ArrayList<>();
            for (String s3 : s2) {
                try {
                    ConnectionManager.DatabaseHost dbHost = makeHostFromString(s3);
                    al.add(dbHost);
                } catch (Exception e) {
                    removeDatabaseHost(s3);
                }

            }
            return al.toArray(new ConnectionManager.DatabaseHost[al.size()]);
        }
    }

    private static ConnectionManager.DatabaseHost makeHostFromString(String s) {
        String[] s4 = s.split(DELIMITER2);
        ConnectionManager.DatabaseHost dbHost = new ConnectionManager.DatabaseHost(s4[0], s4[1], s4[2], s4[3], s4.length == 5 ? s4[4] : "");
        return dbHost;
    }

    private static String hostToString(ConnectionManager.DatabaseHost host) {
        return host.type + DELIMITER2 + host.address + DELIMITER2 + host.instanceName + DELIMITER2 + host.username + DELIMITER2 + host.password;

    }

    public static void addAvailableDatabaseHost(ConnectionManager.DatabaseHost host) throws SQLException {
        try {
            try {
                ConnectionManager.setDatabaseHost(host);
            } catch (java.sql.SQLInvalidAuthorizationSpecException e) {
                host = new ConnectionManager.DatabaseHost(host.type, host.address, host.instanceName, host.username, "");
                ConnectionManager.setDatabaseHost(host);

            }
        } catch (IOException e) {
            logger.showException(e);
        }

        String hostDesc = hostToString(host);
        String s = prefDBHosts.get(KEY_DB_HOSTS, "def");
        if (s.equals("def")) {
            prefDBHosts.put(KEY_DB_HOSTS, hostDesc);
        } else {
            prefDBHosts.put(KEY_DB_HOSTS, s + DELIMITER + hostDesc);
        }
    }

    public static void removeDatabaseHost(ConnectionManager.DatabaseHost h) {
        if (h == null) {
            return;
        }
        ConnectionManager.DatabaseHost[] hosts = getAvailableDatabaseHosts();
        prefDBHosts.put(KEY_DB_HOSTS, "");
        for (ConnectionManager.DatabaseHost databaseHost : hosts) {
            if (!databaseHost.equals(h)) {
                try {
                    try {
                        addAvailableDatabaseHost(databaseHost);
                    } catch (SQLInvalidAuthorizationSpecException e) {
                        if (!databaseHost.password.equals("")) {
                            databaseHost = new ConnectionManager.DatabaseHost(databaseHost.type, databaseHost.address, databaseHost.instanceName, databaseHost.username, "");

                            addAvailableDatabaseHost(databaseHost);

                        }

                    }
                } catch (SQLException e2) {
                    logger.showException(e2);
                }
            }
        }
        if (h.equals(getDefaultDatabaseHost())) {
            if (getAvailableDatabaseHosts().length > 0) {
                setDefaultDatabaseHost(getAvailableDatabaseHosts()[0]);
            } else {
                setDefaultDatabaseHost(null);
            }
        }
    }

    public static void removeDatabaseHost(String h) {
        if (h == null) {
            return;
        }
        String s = prefDBHosts.get(KEY_DB_HOSTS, "");
        if (s != null) {
            s = s.replace(h + DELIMITER, "");
            prefDBHosts.put(KEY_DB_HOSTS, s);
        }
    }

    /**
     * This method shall load all the default options into the static fields of
     * the classes
     */
    public static BarCodeTableCellRenderer.BarCodeOptions loadBarCodeOptions() {
        BarCodeTableCellRenderer.BarcodePaintStyle bcStyle = BarCodeTableCellRenderer.BarcodePaintStyle.valueOf(prefBarcodes.get(KEY_BARCODE_TYPE, "STRIPES"));
        Color up = new Color(Integer.parseInt(prefBarcodes.get(KEY_BARCODE_COLOR_UP, String.valueOf(Color.GREEN.getRGB()))));
        Color down = new Color(Integer.parseInt(prefBarcodes.get(KEY_BARCODE_COLOR_DOWN, String.valueOf(Color.RED.getRGB()))));
        double up_down_ratio = Double.parseDouble(prefBarcodes.get(KEY_BARCODE_POS_NEG_RATIO, "0.5"));
        boolean rainbow = Boolean.parseBoolean(prefBarcodes.get(KEY_BARCODE_RAINBOW, "true"));
        return new BarCodeTableCellRenderer.BarCodeOptions(up, down, bcStyle, up_down_ratio, rainbow);
    }

    public static void saveBarCodeOptions(BarCodeTableCellRenderer.BarCodeOptions options) {
        prefBarcodes.put(KEY_BARCODE_TYPE, options.getStyle().name());
        prefBarcodes.put(KEY_BARCODE_COLOR_UP, String.valueOf(options.getCOLOR_UP().getRGB()));
        prefBarcodes.put(KEY_BARCODE_COLOR_DOWN, String.valueOf(options.getCOLOR_DOWN().getRGB()));
        prefBarcodes.put(KEY_BARCODE_POS_NEG_RATIO, String.valueOf(options.getPos_neg_ratio()));
    }

}
