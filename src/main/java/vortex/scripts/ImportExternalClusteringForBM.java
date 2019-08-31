/*
 * Copyright (C) 2019 Nikolay Samusik and Stanford University
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package vortex.scripts;

import sandbox.dataIO.DatasetStub;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import sandbox.clustering.DistanceMeasure;
import sandbox.clustering.EuclideanDistance;
import sandbox.clustering.Cluster;
import sandbox.clustering.ClusterSet;
import sandbox.clustering.Datapoint;
import sandbox.clustering.Dataset;
import util.IO;
import vortex.util.Config;
import vortex.util.ConnectionManager;
import util.ProfileAverager;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class ImportExternalClusteringForBM {

    static String[] dsNames = new String[]{
        "BM2_cct_normalized_01_non-Neutrophils",
        "BM2_cct_normalized_02_non-Neutrophils",
        "BM2_cct_normalized_03_non-Neutrophils",
        "BM2_cct_normalized_04_non-Neutrophils",
        "BM2_cct_normalized_05_non-Neutrophils",
        "BM2_cct_normalized_06_non-Neutrophils",
        "BM2_cct_normalized_07_non-Neutrophils",
        "BM2_cct_normalized_08_non-Neutrophils",
        "BM2_cct_normalized_09_non-Neutrophils",
        "BM2_cct_normalized_10_non-Neutrophils",/*
     "BM2_cct_normalized_11_non-Neutrophils",
     "BM2_cct_normalized_12_non-Neutrophils",
     "BM2_cct_normalized_13_non-Neutrophils",
     "BM2_cct_normalized_14_non-Neutrophils"*/};

    static String flowMeansFolder = "C:\\Users\\Nikolay\\YandexDisk\\Working folder\\Manuscripts\\X-shift article\\Resubmission\\Clustering comp result\\flowMeans";

    static String SamSpectralFolder = "C:\\Users\\Nikolay\\YandexDisk\\Working folder\\Manuscripts\\X-shift article\\Resubmission\\Clustering comp result\\SamSPECTRAL";

    static String flowPeaksFolder = "C:\\Users\\Nikolay\\YandexDisk\\Working folder\\Manuscripts\\X-shift article\\Resubmission\\Clustering comp result\\flowPeaks";

    static String SwiftFolder = "C:\\Users\\Nikolay\\YandexDisk\\Working folder\\Manuscripts\\X-shift article\\Resubmission\\Clustering comp result\\SWIFT";

    static String SPADEfolder = "D:\\SPADE";
    static String PhenoGraphFolder = "C:\\Users\\Nikolay\\YandexDisk\\Working folder\\Manuscripts\\X-shift article\\Resubmission\\Clustering comp result\\PhenoGraph";

    public static void main(String[] args) throws SQLException, IOException {
        do {

            if (Config.getDefaultDatabaseHost() == null) {
                ConnectionManager.showDlgSelectDatabaseHost();
            }
            if (Config.getDefaultDatabaseHost() == null) {
                System.exit(0);
            }
            ConnectionManager.setDatabaseHost(Config.getDefaultDatabaseHost());

        } while (ConnectionManager.getDatabaseHost() == null);

        //importFlowPeaks();
        //importSamSpectral();
        //deleteClusterSets(393, 499);
        //importSPADE();
        importPhenoGraph();
    }

    public static void deleteClusterSets(int minIdx, int maxIdx) throws SQLException, IOException {
        for (int i = 0; i < dsNames.length; i++) {
            String dsName = dsNames[i];
            Dataset ds = ConnectionManager.getStorageEngine().loadDataset(dsName);
            for (int csid : ConnectionManager.getStorageEngine().getClusterSetIDs(ds.getID())) {
                if (csid >= minIdx && csid <= maxIdx) {
                    logger.print("deleting cs " + csid);
                    ConnectionManager.getStorageEngine().deleteClusterSet(csid);
                }
            }

        }
    }

    public static String getSPADEfolder(int numNodes) {
        return SPADEfolder + File.separator + numNodes;
    }

    public static void importSPADE() throws SQLException, IOException {
        for (int i = 0; i < dsNames.length; i++) {
            String dsName = dsNames[i];
            Dataset ds = ConnectionManager.getStorageEngine().loadDataset(dsName);
            for (int numNodes = 20; numNodes <= 200; numNodes += 10) {
                File f = new File(getSPADEfolder(numNodes) + File.separator + dsName + "_Ungated.fcs.density.fcs.cluster.fcs");
                if (!f.exists()) {
                    logger.print("Does not exist!" + f.getPath());
                    continue;
                }
                logger.print("Importing:" + f.getName());
                ClusterSet cs = createClusterSetFromIndexFile(ds, f, "SPADE", "nodes", numNodes);
                logger.print("Saving" + f.getName());
                ConnectionManager.getStorageEngine().saveClusterSet(cs, true);
            }
        }
    }
    
    public static void importPhenoGraph() throws SQLException, IOException {
        for (int i = 0; i < dsNames.length; i++) {
            String dsName = dsNames[i];
            Dataset ds = ConnectionManager.getStorageEngine().loadDataset(dsName);
            for (int KNN : new int[]{5,10,15,20,25,30}) {
                File f = new File(PhenoGraphFolder + File.separator + dsName + ".fcs");
                if (!f.exists()) {
                    logger.print("Does not exist!" + f.getPath());
                    continue;
                }
                logger.print("Importing:" + f.getName());
                ClusterSet cs = createClusterSetFromIndexFile(ds, f, "PhenoGraph", "KNN", KNN);
                logger.print("Saving" + f.getName());
                ConnectionManager.getStorageEngine().saveClusterSet(cs, true);
            }
        }
    }

    public static void importFlowPeaks() throws SQLException, IOException {
        for (int i = 0; i < dsNames.length; i++) {
            String dsName = dsNames[i];
            Dataset ds = ConnectionManager.getStorageEngine().loadDataset(dsName);

            String fpFileName = "Mouse" + String.format("%02d", i + 1) + "_h0= ";

            for (int j = 1; j <= 10; j += 1) {
                File f = new File(flowPeaksFolder + File.separator + fpFileName + "0." + j);
                if (i >= 6) {
                    f = new File(flowPeaksFolder + File.separator + fpFileName + " 0." + j);
                }
                if (j == 10) {
                    f = new File(flowPeaksFolder + File.separator + fpFileName + "1");
                }

                if (!f.exists()) {
                    logger.print("Does not exist!" + f.getPath());
                    continue;

                }
                logger.print("Importing:" + f.getName());
                ClusterSet cs = createClusterSetFromIndexFile(ds, f, "flowPeaks", "h0", j / 10.0);
                logger.print("Saving" + f.getName());
                ConnectionManager.getStorageEngine().saveClusterSet(cs, true);
            }
        }
    }

    public static void importSWIFT() throws SQLException, IOException {
        for (int i = 0; i < dsNames.length; i++) {
            String dsName = dsNames[i];
            Dataset ds = ConnectionManager.getStorageEngine().loadDataset(dsName);
            String fpFileName = "BM2_cct_normalized_" + String.format("%02d", i + 1) + "_InputClusters-";
            String folder = SwiftFolder;
            for (int j : new int[]{5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100}) {
                File f = new File(folder + File.separator + fpFileName + j + ".txt");
                if (!f.exists()) {
                    logger.print("Does not exist!" + f.getPath());
                    continue;
                }
                logger.print("Importing:" + f.getName());
                ClusterSet cs = createClusterSetFromIndexFile(ds, f, "SWIFT", "initNumPop", j);
                logger.print("Saving" + f.getName());
                ConnectionManager.getStorageEngine().saveClusterSet(cs, true);
            }
        }
    }

    public static void importSamSpectral() throws SQLException, IOException {
        for (int i = 0; i < dsNames.length; i++) {
            String dsName = dsNames[i];
            Dataset ds = ConnectionManager.getStorageEngine().loadDataset(dsName);
            String folder = SamSpectralFolder;
            String fpFileName = dsName + ".csv-sep";

            for (int j = 3; j <= 19; j += 1) {

                File f = new File(folder + File.separator + fpFileName + "0." + j);
                if (j < 10) {
                    if (!f.exists()) {
                        logger.print("Does not exist!" + f.getPath());
                        f = new File(folder + File.separator + fpFileName + " 0." + j);
                    }
                }
                if (j == 10) {
                    f = new File(folder + File.separator + fpFileName + "1");
                    if (!f.exists()) {
                        logger.print("Does not exist!" + f.getPath());
                        f = new File(folder + File.separator + fpFileName + " 1");
                    }
                }
                if (j > 10) {
                    f = new File(folder + File.separator + fpFileName + "1." + (j - 10));
                    if (!f.exists()) {
                        logger.print("Does not exist!" + f.getPath());
                        f = new File(folder + File.separator + fpFileName + " 1." + (j - 10));
                    }
                }

                if (!f.exists()) {
                    logger.print("Does not exist!" + f.getPath());
                    continue;

                }
                logger.print("Importing:" + f.getName());
                ClusterSet cs = createClusterSetFromIndexFile(ds, f, "SamSPECTRAL", "h0", j / 10.0);
                logger.print("Saving" + f.getName());
                ConnectionManager.getStorageEngine().saveClusterSet(cs, true);
            }
        }
    }

    public static int[] createIntArrayFromPhenoGraphFile(File f, int KNN) throws IOException {

        DatasetStub ds = DatasetStub.createFromFCS(f);

        int[] out = new int[(int) ds.getRowCount()];

        int colID = -1;
        for (int i = 0; i < ds.getLongColumnNames().length; i++) {
            if (ds.getLongColumnNames()[i].contains("PhenoGraph Each K" + KNN)) {
                colID = i;
            }
        }

        if (colID == -1) {
            throw new IllegalStateException("Couldn't find a parameter corresponding to KNN = " + KNN);
        }

        for (int i = 0; i < out.length; i++) {
            out[i] = (int) ds.getRow(i)[colID];
        }
        return out;
    }

    public static ClusterSet createClusterSetFromIndexFile(Dataset d, File f, String clusteringAlgorithm, String mainParamName, double mainParamerValue) throws IOException {
        int[] assignments = f.getName().endsWith("fcs") ? (f.getPath().toLowerCase().contains("phenograph") ? createIntArrayFromPhenoGraphFile(f, (int) mainParamerValue) : createIntArrayFromSPADEFile(f)) : createIntArrayFromTEXTFile(f);
        DistanceMeasure dm = new EuclideanDistance();

        assert (assignments.length == d.getDatapoints().length);

        int maxClusID = -1;

        for (int i = 0; i < assignments.length; i++) {
            maxClusID = Math.max(maxClusID, assignments[i]);
        }

        Cluster[] clus = new Cluster[maxClusID + 1];
        ProfileAverager[] pa = new ProfileAverager[clus.length];
        for (int i = 0; i < pa.length; i++) {
            pa[i] = new ProfileAverager();
        }
        List<Datapoint>[] members = new List[clus.length];
        for (int i = 0; i < members.length; i++) {
            members[i] = new ArrayList<>();
        }
        assert (assignments.length == d.getDatapoints().length);

        for (int i = 0; i < assignments.length; i++) {
            if (assignments[i] == -1) {
                continue;
            }
            Datapoint dp = d.getDatapoints()[i];
            pa[assignments[i]].addProfile(dp.getVector());
            members[assignments[i]].add(dp);
        }
        ArrayList<Cluster> alCl = new ArrayList<>();
        for (int i = 0; i < clus.length; i++) {
            if (members[i].size() > 0) {
                alCl.add(new Cluster(members[i].toArray(new Datapoint[members[i].size()]), pa[i].getAverage(), new double[0], "dm", null));
            }
        }

        return new ClusterSet(0, d, alCl.toArray(new Cluster[alCl.size()]), dm, clusteringAlgorithm, mainParamName + "=" + String.valueOf(mainParamerValue), mainParamerValue, "");
    }

    public static int[] createIntArrayFromTEXTFile(File f) throws IOException {
        ArrayList<Integer> al = IO.getListOfIntegersFromStream(new FileInputStream(f), -1);
        int[] out = new int[al.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = al.get(i);
        }
        return out;
    }

    public static int[] createIntArrayFromSPADEFile(File f) throws IOException {

        DatasetStub ds = DatasetStub.createFromFCS(f);

        int[] out = new int[(int) ds.getRowCount()];

        int colID = Arrays.asList(ds.getLongColumnNames()).indexOf("cluster");

        for (int i = 0; i < out.length; i++) {
            out[i] = (int) ds.getRow(i)[colID];
        }
        return out;
    }
}
