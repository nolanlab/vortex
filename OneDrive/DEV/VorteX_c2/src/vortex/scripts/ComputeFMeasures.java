/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.scripts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import annotations.Annotation;
import clustering.ClusterMember;
import clustering.ClusterSet;
import clustering.Dataset;
import vortex.util.Config;
import vortex.util.ConnectionManager;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class ComputeFMeasures {

    static public int fMeasureCycles = 1;
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
     "BM2_cct_normalized_10_non-Neutrophils"/*,
     "BM2_cct_normalized_11_non-Neutrophils",
     "BM2_cct_normalized_12_non-Neutrophils",
     "BM2_cct_normalized_13_non-Neutrophils",
     "BM2_cct_normalized_14_non-Neutrophils"*/};
    /*static int[] csID_OfInterest = new int[]{1133};/*
        980,
        993,
        672,
        1015,
        1039,
        1046,
        1065,
        1071,
        965,
        1091};*/

    public static void main(String[] args) throws Exception {

        do {

            if (Config.getDefaultDatabaseHost() == null) {
                ConnectionManager.showDlgSelectDatabaseHost();
            }
            if (Config.getDefaultDatabaseHost() == null) {
                System.exit(0);
            }
            ConnectionManager.setDatabaseHost(Config.getDefaultDatabaseHost());

        } while (ConnectionManager.getDatabaseHost() == null);
        for (int currDS_idx = 0; currDS_idx < dsNames.length; currDS_idx++) {
            String dsName = dsNames[currDS_idx];
            Dataset ds = ConnectionManager.getStorageEngine().loadDataset(dsName);
            if (ds == null) {
                continue;
            }

            logger.print(Arrays.toString(ds.getAnnotations()));//
            Annotation ann = null;

            for (Annotation a : ds.getAnnotations()) {
                if (a.getAnnotationName().toLowerCase().contains("nkz")) {
                    ann = a;
                    break;
                }
            }

            if (ann == null) {
                logger.print("the required annotation hasn't been found for ds " + ds.getName() + ". Aborting");
                continue;
            }

            ArrayList<String> alTerms = new ArrayList<>();
            for (String s : ann.getTerms()) {
                if (!s.toLowerCase().contains("ter11")) {
                    alTerms.add(s);
                }
            }

            String[] terms = alTerms.toArray(new String[alTerms.size()]);
            Arrays.sort(terms);
            System.out.print("\t");
            for (int rep = 0; rep < fMeasureCycles; rep++) {
                for (String o : terms) {
                    System.out.print(o);
                }
            }
            System.out.print("\n");

            int[] csIDs = ConnectionManager.getStorageEngine().getClusterSetIDs(ds.getID());
            Arrays.sort(csIDs);
            ClusterSet[] css = new ClusterSet[csIDs.length];
            int z = 0;

            for (final int clusterSetID : csIDs) {
                css[z++] = ConnectionManager.getStorageEngine().loadClusterSet(clusterSetID, ds);
            }

            for (ClusterSet cs : css) {

                if (cs.getID() < 1132) {
                    continue;
                }

                double[][] adjMatrix = new double[terms.length][cs.getClusters().length];

                for (int i = 0; i < adjMatrix.length; i++) {
                    int[] ids = ann.getDpIDsForTerm(terms[i]);
                    Arrays.sort(ids);
                    for (int j = 0; j < adjMatrix[i].length; j++) {
                        for (ClusterMember cm : cs.getClusters()[j].getClusterMembers()) {
                            if (cm == null) {
                                continue;
                            }
                            if (Arrays.binarySearch(ids, cm.getDatapoint().getID()) >= 0) {
                                adjMatrix[i][j]++;
                            }
                        }
                    }
                }

                double precMatrix[][] = new double[adjMatrix.length][adjMatrix[0].length];
                for (int i = 0; i < adjMatrix[0].length; i++) {
                    double sumCol = 0;
                    for (int j = 0; j < adjMatrix.length; j++) {
                        sumCol += adjMatrix[j][i];
                    }
                    for (int j = 0; j < adjMatrix.length; j++) {
                        precMatrix[j][i] = adjMatrix[j][i] / sumCol;
                    }
                }
                double recallMtx[][] = new double[adjMatrix.length][adjMatrix[0].length];
                for (int i = 0; i < adjMatrix.length; i++) {
                    double sumRow = 0;
                    for (int j = 0; j < adjMatrix[0].length; j++) {
                        sumRow += adjMatrix[i][j];
                    }
                    for (int j = 0; j < adjMatrix[0].length; j++) {
                        recallMtx[i][j] = adjMatrix[i][j] / sumRow;
                    }
                }

                double fMatrix[][] = new double[adjMatrix.length][adjMatrix[0].length];
                for (int i = 0; i < adjMatrix.length; i++) {
                    for (int j = 0; j < adjMatrix[0].length; j++) {
                        fMatrix[i][j] = /*recallMtx[i][j];*/ (2.0 * precMatrix[i][j] * recallMtx[i][j]) / (precMatrix[i][j] + recallMtx[i][j]);
                        if (Double.isInfinite(fMatrix[i][j]) || Double.isNaN(fMatrix[i][j])) {
                            fMatrix[i][j] = 0;
                        }
                    }
                }
                System.out.print(cs + "\t");

                fMatrix = hungarianMax_extern(fMatrix);

                for (int rep = 0; rep < fMeasureCycles; rep++) {
                    double[] ds1 = fMatrix[rep];
                    for (int i = 0; i < terms.length; i++) {
                        double max = 0;
                        int maxIDX = 0;
                        for (int j = 0; j < cs.getClusters().length; j++) {
                            if (fMatrix[i][j] > max) {
                                max = fMatrix[i][j];
                                maxIDX = j;
                            }
                        }
                        System.out.print(fMatrix[i][maxIDX] + "\t");
                        fMatrix[i][maxIDX] = 0;
                    }
                }
                System.out.print("\n");
            }
        }
    }

    private static double[][] hungarianMax_extern(double[][] fMatrix) {
        double[][] out = new double[fMatrix.length][fMatrix[0].length];

        double[][] inter = new double[fMatrix.length][fMatrix[0].length];
        for (int i = 0; i < inter.length; i++) {
            Arrays.fill(inter[i], 1.0);
        }
        for (int i = 0; i < fMatrix.length; i++) {
            for (int j = 0; j < fMatrix[0].length; j++) {
                inter[i][j] = 1.0 - fMatrix[i][j];
            }
        }
        int[] assignments = (new fmeasure.HungarianAlgorithm(inter)).execute();
        for (int i = 0; i < assignments.length; i++) {
            if (assignments[i] != -1) {
                out[i][assignments[i]] = fMatrix[i][assignments[i]];
            }
        }
        return out;
    }

}
