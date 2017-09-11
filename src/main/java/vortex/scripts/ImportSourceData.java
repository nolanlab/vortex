/*
 * Copyright (C) 2015 Nikolay
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import clustering.AngularDistance;
import dataIO.DatasetImporter;
import dataIO.DatasetStub;
import dataIO.ImportConfigObject;
import clustering.Dataset;
import util.IO;
import vortex.util.Config;
import vortex.util.ConnectionManager;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class ImportSourceData {

    public static String[] colNames = new String[]{
        "Ter119((In113)Di)", "CD45.2((In115)Di)", "Ly6G((La139)Di)", "IgD((Pr141)Di)", "CD11c((Nd142)Di)", "F480((Nd143)Di)", "CD3((Nd144)Di)", "NKp46((Nd145)Di)", "CD23((Nd146)Di)", "CD34((Sm147)Di)", "CD115((Nd148)Di)", "CD19((Sm149)Di)", "120g8((Nd150)Di)", "CD8((Eu151)Di)", "Ly6C((Sm152)Di)", "CD4((Eu153)Di)", "CD11b((Sm154)Di)", "CD27((Gd155)Di)", "CD16_32((Gd156)Di)", "SiglecF((Gd157)Di)", "Foxp3((Gd158)Di)", "B220((Tb159)Di)", "CD5((Gd160)Di)", "FceR1a((Dy161)Di)", "TCRgd((Dy162)Di)", "CCR7((Dy163)Di)", "Sca1((Dy164)Di)", "CD49b((Ho165)Di)", "cKit((Er166)Di)", "CD150((Er167)Di)", "CD25((Er168)Di)", "TCRb((Tm169)Di)", "CD43((Er170)Di)", "CD64((Yb171)Di)", "CD138((Yb172)Di)", "CD103((Yb173)Di)", "IgM((Yb174)Di)", "CD44((Lu175)Di)", "MHCII((Yb176)Di)"
    };

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
        "BM2_cct_normalized_10_non-Neutrophils",
        /*"BM2_cct_normalized_11_non-Neutrophils",
        "BM2_cct_normalized_12_non-Neutrophils",
        "BM2_cct_normalized_13_non-Neutrophils",
        "BM2_cct_normalized_14_non-Neutrophils"*/
    };

    static String path = "C:\\Users\\Nikolay\\Local Working Folder\\Clustering Comparison\\Source files";

    public static void main(String[] args) throws Exception {
        
        for (final String dsName : dsNames) {
            
            do {

            if (Config.getDefaultDatabaseHost() == null) {
                ConnectionManager.showDlgSelectDatabaseHost();
            }
            if (Config.getDefaultDatabaseHost() == null) {
                System.exit(0);
            }
            ConnectionManager.setDatabaseHost(Config.getDefaultDatabaseHost());

        } while (ConnectionManager.getDatabaseHost() == null);
            
            final File f = new File(path + "\\" + dsName + ".csv");
            final String[] shortColNames = new String[colNames.length];
            final String[] longColNames = new String[colNames.length];
            for (int i = 0; i < colNames.length; i++) {
                shortColNames[i] = colNames[i].replaceAll(".*\\(\\(", "(").replaceAll("Di\\)", "Di");
                longColNames[i] = colNames[i].replaceAll("\\(\\(.*\\)", "");
                logger.print(longColNames[i], shortColNames[i]);
            }

            final BufferedReader br = new BufferedReader(new FileReader(f));

            DatasetStub dss = new DatasetStub() {
                ArrayList<String> stringList;
                final String del = "[\t,]+";
                //String [] firstStringDelimited;
                String[] headStringDelimited;
                ArrayList<String> skippedRows = new ArrayList<>();
                
                @Override
                public String getFileName() {
                    return f.getName().split("\\.")[0];
                }

                @Override
                public void close() {
                    try {
                        br.close();
                    } catch (IOException e) {
                        logger.showException(e);
                    }
                }

                @Override
                public String[] getSkippedRows() {
                    return skippedRows.toArray(new String[skippedRows.size()]);
                }

                @Override
                public String[] getShortColumnNames() {
                    return shortColNames;
                }

                @Override
                public String[] getLongColumnNames() {
                    return longColNames;
                }
                final String formatString = "%0" + 7 + "d";
                @Override
                public String getRowName(int i) {
                   return dsName + ".fcs Event " + String.format(formatString, i);
                }

                @Override
                public double[] getRow(int i) {
                    try {
                        if (stringList == null) {
                            stringList = IO.getListOfStringsFromStream(new FileInputStream(f));
                        }
                        String[] s = stringList.get(i).split(del);
                        if (s.length != getShortColumnNames().length) {
                            skippedRows.add(i + ", reason: invalid number of columns (" + stringList.get(i) + " instead of " + stringList.get(i));
                            return null;
                        }
                        double[] vec = new double[s.length];
                        for (int j = 0; j < vec.length; j++) {
                            try {
                                vec[j] = Double.parseDouble(s[j]);
                            } catch (NumberFormatException e) {
                                skippedRows.add(i + ", reason: invalid numerical format (" + s[j] + ")");
                                return null;
                            }
                        }
                        return vec;
                    } catch (IOException e) {
                        logger.showException(e);
                        return null;
                    }
                }

                @Override
                public long getRowCount() {
                    try {
                        if (stringList == null) {
                            stringList = IO.getListOfStringsFromStream(new FileInputStream(f));
                        }
                    } catch (IOException e) {
                        logger.showException(e);
                        return 0;
                    }
                    return stringList.size();
                }
            };
            ImportConfigObject ico = new ImportConfigObject(
                    dsName,  
                    colNames, 
                    new String[0], 
                    ImportConfigObject.RescaleType.NONE, 
                    ImportConfigObject.TransformationType.NONE, 
                    1, 
                    0.95, 
                   false,
                    -1, 
                    -1, 
                    -1);
            Dataset ds = DatasetImporter.importDataset(new DatasetStub[]{dss}, ico);
            ConnectionManager.getStorageEngine().saveDataset(ds, true);
             
        }
    }
}
