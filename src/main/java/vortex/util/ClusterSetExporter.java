/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.util;

import sandbox.clustering.Cluster;
import sandbox.clustering.ClusterMember;
import sandbox.clustering.ClusterSet;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import util.logger;

/**
 *
 * @author Nikolay Samusik
 */
public class ClusterSetExporter {

    public static void exportClusterSetAsCSV(File f2, ClusterSet cs) {

        boolean firstRow = true;
        try (BufferedWriter br = new BufferedWriter(new FileWriter(f2))) {
            StringBuilder sb = new StringBuilder();
            for (Cluster c : cs.getClusters()) {

                for (ClusterMember cm : c.getClusterMembers()) {
                    if (firstRow) {
                        br.write("ClusterID,");
                        br.write(arrToString(cm.getHeaderRow()));
                        br.write("\n");
                        firstRow = false;
                    }
                    br.write(c.getID() + ",");
                    br.write(arrToString(cm.toRow()));
                    br.write("\n");
                }

            }

            br.write(sb.toString());
            br.flush();
            br.close();
        } catch (IOException e) {
            logger.showException(e);
        }

    }

    private static String arrToString(Object[] arr) {
        StringBuilder out = new StringBuilder();
        for (Object o : arr) {
            if (o.toString().contains("Barcode") || o.toString().contains("clustering.ClusterMember")) {
                continue;
            }
            out.append(o.toString()).append(",");
        }
        return out.substring(0, out.length() - 1);
    }
}
