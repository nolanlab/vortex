/*
 * Copyright (C) 2016 Nikolay
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

import annotations.Annotation;
import clustering.Cluster;
import clustering.ClusterSet;
import clustering.Datapoint;
import dataIO.DatasetImporter;
import dataIO.DatasetStub;
import dataIO.ImportConfigObject;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import clustering.Dataset;
import util.Optimization;
import util.logger;
import vortex.mahalonobis.MahalonobisDistance;
import vortex.util.Config;
import vortex.util.ConnectionManager;

/**
 *
 * @author Nikolay
 */
public class SLL_Patient_analysis {
    /*
     * To change this template, choose Tools | Templates
     * and open the template in the editor.
     */

    static double MAH_LEN_THS = 6;

    public static void main(String[] args) throws Exception {
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

        File dir  = new File("C:\\Users\\Nikolay\\Local Working Folder\\Ovarian Cancer\\SLL patient");
        
        File[] files = dir.listFiles();
        //File f = new File("C:\\Users\\Nikolay\\Dropbox\\for Zina\\ALL07 Lionel-Karla NikolayGating\\Apr 10 Gating ver2\\");
        String dsName = "OCFinal TumorCD45-CD31- newDebar CleanPanel";
        Dataset ds = ConnectionManager.getStorageEngine().loadDataset(dsName);
        
       

        ClusterSet cs = ConnectionManager.getStorageEngine().loadClusterSet(269, ds);
        Cluster[] cl = cs.getClusters();
        MahalonobisDistance[] ms = new MahalonobisDistance[cl.length];

        for (int i = 0; i < ms.length; i++) {
            double[][] expMtx = new double[cl[i].size()][];
            for (int j = 0; j < expMtx.length; j++) {
                expMtx[j] = cl[i].getClusterMembers()[j].getDatapoint().getVector();
            }
            ms[i] = new MahalonobisDistance(expMtx);
        }
        logger.setOutputMode(logger.OUTPUT_MODE_NONE);
        for (File file : files) {
            
        DatasetStub stub = DatasetStub.createFromFCS(file);
        /*
        for (int i = 0; i < stub.getColumnNames().length; i++) {
            logger.print(stub.getColumnNames()[i], stub.getShortColumnNames()[i]);
        }*/

        Dataset sll = DatasetImporter.importDataset(new DatasetStub[]{stub}, new ImportConfigObject(
                "SLL", ds.getFeatureNames(), new String[0], ImportConfigObject.RescaleType.NONE, ImportConfigObject.TransformationType.ASINH, 5, 0.99, false, -1, 1, 1));
        
        LinkedList<String>[] classifiedLists = new LinkedList[ms.length + 1];
        for (int i = 0; i < classifiedLists.length; i++) {
            classifiedLists[i] = new LinkedList<>();
        }

        
        
        
        
        double totalSampleCount = 0;
        
        
        for (Datapoint nd : sll.getDatapoints()) {

            totalSampleCount++;

            int nnIDX = -1;
            double minDist = Double.MAX_VALUE;
            for (int i = 0; i < ms.length; i++) {
                double dist = ms[i].distTo(nd.getVector());

                //MatrixOp.getEuclideanDistance(ms[i].center.toArray(), nd.getVector()); //
                if (dist < minDist) {
                    minDist = dist;
                    nnIDX = i;
                }
            }
            if (minDist < MAH_LEN_THS) {
                classifiedLists[nnIDX].add(nd.getFullName());
            } else {
                classifiedLists[cl.length].add(nd.getFullName());
            }

        }
        
        System.out.print("\n" + file.getName());
        //logger.print("Total sample count: "+ totalSampleCount);
        for (int i = 0; i < classifiedLists.length; i++) {
            if (i < cl.length) {
                System.out.print("\t" + classifiedLists[i].size());
            } else {
                System.out.println("\t" + classifiedLists[i].size());
            }

            /*EuclideanDistance  ed = new EuclideanDistance();
             double [] [] vec = new double[classifiedLists[i].size()][];
             for (int j = 0; j < vec.length; j++) {
             vec[j] = ds.getDPbyName(classifiedLists[i].get(j)).getVector();
             }
             logger.print(Arrays.toString(ed.getPrototype(vec)));*/
        }
    }
    }

}
