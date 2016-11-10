/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.scripts;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import clustering.AngularDistance;
import vortex.clustering.XShiftClustering;
import clustering.ClusterSet;
import clustering.Dataset;
import util.LinePlusExponent;
import vortex.util.Config;
import vortex.util.ConnectionManager;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class AutomaticXshiftClustering implements Script {

    //public int fMeasureCycles = 1;
    String[] dsNames = new String[]{
        //"BM2_01-14_non-Neutrophils"
        "BM2_cct_normalized_01_non-Neutrophils",
        "BM2_cct_normalized_02_non-Neutrophils",
        "BM2_cct_normalized_03_non-Neutrophils",
        "BM2_cct_normalized_04_non-Neutrophils",
        "BM2_cct_normalized_05_non-Neutrophils",
        "BM2_cct_normalized_06_non-Neutrophils",
        "BM2_cct_normalized_07_non-Neutrophils",
        "BM2_cct_normalized_08_non-Neutrophils",
        "BM2_cct_normalized_09_non-Neutrophils",
        "BM2_cct_normalized_10_non-Neutrophils",
        "BM2_cct_normalized_11_non-Neutrophils",
        "BM2_cct_normalized_12_non-Neutrophils",
        "BM2_cct_normalized_13_non-Neutrophils",
        "BM2_cct_normalized_14_non-Neutrophils"
    };

    @Override
    public Object runScript() throws Exception {
        do {
            try {
                if (Config.getDefaultDatabaseHost() == null) {
                    ConnectionManager.showDlgSelectDatabaseHost();
                }
                if (Config.getDefaultDatabaseHost() == null) {
                    System.exit(0);
                }
                ConnectionManager.setDatabaseHost(Config.getDefaultDatabaseHost());
            } catch (SQLException | IOException e) {
                logger.showException(e);
                ConnectionManager.showDlgSelectDatabaseHost();
            }
        } while (ConnectionManager.getDatabaseHost() == null);
        for (String dsName : dsNames) {
            Dataset ds = ConnectionManager.getStorageEngine().loadDataset(dsName);
            if (ds == null) {
                continue;
            }
           
            ArrayList<Double> alx = new ArrayList<>();
             ArrayList<Double> aly = new ArrayList<>();
            for (int clusterSetID : ConnectionManager.getStorageEngine().getClusterSetIDs(ds.getID())) {
                ClusterSet cs = ConnectionManager.getStorageEngine().loadClusterSet(clusterSetID, ds);
                if(!cs.getClusteringAlgorithm().toLowerCase().contains("x-shift")) continue;
                alx.add(cs.getMainClusteringParameterValue());
                aly.add(new Double(cs.getNumberOfClusters()));
            }
            double[]x = new double[alx.size()];
            double[]y = new double[alx.size()];
            for (int i = 0; i < y.length; i++) {
                x[i]=alx.get(i);
                y[i]=aly.get(i);
            }
            XShiftClustering xs = new XShiftClustering(new AngularDistance());
            Integer [] bestX = new Integer[]{ (int)Math.round(LinePlusExponent.findElbowPointLinePlusExp(y,x))};
            xs.setK(bestX);
            xs.setUseVMF(Boolean.FALSE);
            xs.setnSize(null);
            logger.print("Clustering " + ds + " with K " + bestX[0] );
            ClusterSet [] cs = xs.doBatchClustering(ds, dsName);
            
        }
        return null;
    }
}
