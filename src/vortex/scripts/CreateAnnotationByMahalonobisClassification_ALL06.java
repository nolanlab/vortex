/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.scripts;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import annotations.Annotation;
import vortex.mahalonobis.MahalonobisDistance;
import clustering.Datapoint;
import dataIO.DatasetStub;
import main.Dataset;
import vortex.util.Config;
import vortex.util.ConnectionManager;
import util.MatrixOp;
import util.Optimization;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class CreateAnnotationByMahalonobisClassification_ALL06 implements Script {

    public String[] dsNames = new String[]{
        "ALL06_live_basal_9param"
    };
    
    double MAH_LEN_THS = 10;

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

        File f = new File("C:\\Users\\Nikolay\\Dropbox\\for Zina\\ALL07 Lionel-Karla NikolayGating\\May23 Gating without CD24");

        //File f = new File("C:\\Users\\Nikolay\\Dropbox\\for Zina\\ALL07 Lionel-Karla NikolayGating\\Apr 10 Gating ver2\\");

        for (String dsName : dsNames) {
            Dataset ds = ConnectionManager.getStorageEngine().loadDataset(dsName);
            File[] files = f.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(".fcs");
                }
            });

            MahalonobisDistance[] ms = new MahalonobisDistance[files.length];

            double[][][] stubVecLists = new double[files.length][][];

            for (int i = 0; i < files.length; i++) {
                DatasetStub stub = DatasetStub.createFromFCS(files[i]);
                logger.print("reading: " + files[i].getName() + ", #events: " + stub.getRowCount());
                //stubVecLists[i] = new double[stub.getRowCount()][];
                int[] featureColIdxRaw = new int[ds.getFeatureNames().length];
                int notFound = 0;
                String [] columnNames = stub.getShortColumnNames();
                for (int j = 0; j < columnNames.length; j++) {
                    columnNames[j] =columnNames[j].toLowerCase();
                }
                
                
                
                for (int k = 0; k < featureColIdxRaw.length; k++) {
                    //featureColIdxRaw[k] = Optimization.indexOf(columnNames, ds.getFeatureNames()[k].replace("CD20","cd20_surf").replace("IgMi","igm_").toLowerCase());
                    featureColIdxRaw[k] = Optimization.indexOf(columnNames, ds.getFeatureNames()[k].replace("IgM-i","IgMi").toLowerCase());
                    
                    if (featureColIdxRaw[k] == -1) {
                        logger.print("not found: " + ds.getFeatureNames()[k]);
                        notFound++;
                        
                    } else {
                        logger.print("Found: " + ds.getFeatureNames()[k]);
                    }
                }
                if(notFound>0){
                    System.exit(0);
                }

                int[] featureColIdx = new int[featureColIdxRaw.length - notFound];
                int cnt = 0;
                for (int j = 0; j < featureColIdx.length; j++) {
                    if (featureColIdxRaw[j] >= 0) {
                        featureColIdx[cnt++] = featureColIdxRaw[j];
                    }
                }
                
                double[][] vec = new double[(int)stub.getRowCount()][];
                for (int j = 0; j < stub.getRowCount(); j++) {
                    vec[j] = MatrixOp.subset(stub.getRow(j), featureColIdx);
                    for (int k = 0; k < vec[j].length; k++) {
                        double val = vec[j][k];
                        val /= 5;
                        val = Math.log(val + Math.sqrt(val * val + 1));
                        vec[j][k] = val;
                    }
                }
                stubVecLists[i] = vec;
                ms[i] = new MahalonobisDistance(vec);
                logger.print(ms[i].covMtx.toString());
            }

            Annotation ann = ds.getAnnotations()[0];

            for (String term : ann.getTerms()) {
               //   logger.print("Filtering  by l_basal1");
                 //if(!term.contains("das"))continue;

                LinkedList<String>[] classifiedLists = new LinkedList[ms.length + 1];
                for (int i = 0; i < classifiedLists.length; i++) {
                    classifiedLists[i] = new LinkedList<>();
                }

                Annotation ann2 = new Annotation(ds, ds.getName() + " Classification by MahDist_withCovar to Lionel Nikolay-gates(Apr10v2) Thersh15_LastTP");
                double totalSampleCount = 0;

                //logger.print("\nFiltering by CD34+orCD38+");
                double averageStat5 = 0;
                int Stat5Index = Optimization.indexOf(ds.getSideVarNames(), "pSTAT5");
                double averageTdT = 0;
                int TdTindex = Optimization.indexOf(ds.getFeatureNames(), "TdT");
                //logger.print("Stat5 idx" + Stat5Index);
                //logger.print("last column will be the avg value of pStat5");
                for (Datapoint nd : ds.getDatapoints()) {
                    /*if (nd.getID() % 1000 == 0) {
                     logger.print(nd.getID());
                     }*/
                    
                    if (nd.getName().startsWith(term)  /*&& (nd.getVector()[8]>3.69||nd.getVector()[3]>3.69)*/) {
                        totalSampleCount++;
                        averageStat5+=nd.getSideVector()[Stat5Index];
                        averageTdT+= nd.getVector()[TdTindex];
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
                            classifiedLists[nnIDX].add(nd.getName());
                        } else {
                            classifiedLists[files.length].add(nd.getName());
                        }
                    }
                }
                averageStat5/=totalSampleCount;
                averageTdT/=totalSampleCount;
                System.err.print("\n" + term);
                //logger.print("Total sample count: "+ totalSampleCount);
                for (int i = 0; i < classifiedLists.length; i++) {
                    if (i < files.length) {
                        ann.addTerm(classifiedLists[i].toArray(new String[classifiedLists[i].size()]), files[i].getName().substring(0, files[i].getName().length() - 4));
                    } else {
                        ann.addTerm(classifiedLists[i].toArray(new String[classifiedLists[i].size()]), "unclassified");
                    }
                    System.err.print("\t" + classifiedLists[i].size() / totalSampleCount);
                    /*EuclideanDistance  ed = new EuclideanDistance();
                     double [] [] vec = new double[classifiedLists[i].size()][];
                     for (int j = 0; j < vec.length; j++) {
                     vec[j] = ds.getDPbyName(classifiedLists[i].get(j)).getVector();
                     }
                     logger.print(Arrays.toString(ed.getPrototype(vec)));*/
                }
                System.err.print("\t" + averageStat5);
                System.err.print("\t" + averageTdT);
            }
            // ConnectionManager.getStorageEngine().saveAnnotation(ann);




        }
        return null;
    }
}
