/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.scripts;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

import java.util.LinkedList;
import annotations.Annotation;
import clustering.AngularDistance;

import dataIO.DatasetStub;
import vortex.mahalonobis.MahalonobisDistance;
import clustering.Cluster;
import clustering.ClusterSet;
import clustering.Datapoint;
import clustering.Dataset;
import vortex.util.Config;
import vortex.util.ConnectionManager;
import util.MatrixOp;
import util.Optimization;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class CosineClassification_ALL05v2 {


    public static String[] dsNames = new String[]{
        "Healthy Plate 1 viSNE Gates_Concat",
        "Healthy Plate 2 viSNE Gates_Concat",
        "Healthy Plate 3 viSNE Gates Concat"
    };
    
    public static void main(String [] args) throws Exception {
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
        
        for (int dsIDX = 0; dsIDX < dsNames.length; dsIDX++) {
           
        String dsName = dsNames[dsIDX];
        
        File f = new File("C:\\Users\\Nikolay\\Local Working Folder\\Kara\\ALL05 and ALL05v2 Healthy gates cleaned by VISNE\\Plate "+(dsIDX+1)+"\\");
            
        //File f = new File("C:\\Users\\Nikolay\\Dropbox\\for Zina\\ALL07 Lionel-Karla NikolayGating\\Apr 10 Gating ver2\\");

      
            Dataset ds = ConnectionManager.getStorageEngine().loadDataset(dsName);
            if(ds==null){
                logger.print("Dataset is null!" + dsName);
                return;
            }
            
            String[] featureNames= Arrays.copyOf(ds.getFeatureNames(), ds.getFeatureNames().length);
            //featureNames[ds.getFeatureNames().length]="CD24";
            //featureNames[ds.getFeatureNames().length+1]="CD179b";
            //featureNames[ds.getFeatureNames().length+2]="HLADR";
            
            File[] files = f.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(".fcs")&&pathname.getName().contains("basal1");
                }
            });
            //logger.print("Filename:");
            //logger.print(f);
           
            MahalonobisDistance[] ms = new MahalonobisDistance[files.length];

            double[][][] stubVecLists = new double[files.length][][];
            logger.print();
            for (int i = 0; i < files.length; i++) {
                DatasetStub stub = DatasetStub.createFromFCS(files[i]);
                //logger.print("reading: " + files[i].getName() + ", #events: " + stub.getRowCount());
                //stubVecLists[i] = new double[stub.getRowCount()][];
                int[] featureColIdxRaw = new int[featureNames.length];
                int notFound = 0;
                String [] columnNames = stub.getShortColumnNames();
                for (int j = 0; j < columnNames.length; j++) {
                    columnNames[j] =columnNames[j].toLowerCase();
                }
                
                for (int k = 0; k < featureColIdxRaw.length; k++) {
                    featureColIdxRaw[k] = Optimization.indexOf(columnNames, featureNames[k].toLowerCase()); 
                    if (featureColIdxRaw[k] == -1) {
                        logger.print("not found: " + featureNames[k]);
                        notFound++;
                    } else {
                        //logger.print("Found: " + featureNames[k]);
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
                        double val = vec[j][k]-1;
                        val = Math.max(0,val);
                        if(val==0) val = Math.random()/100;
                        val /= 5;
                        val = Math.log(val + Math.sqrt(val * val + 1));
                        vec[j][k] = val;
                    }
                }
                stubVecLists[i] = vec;
                ms[i] = new MahalonobisDistance(vec);
                //logger.print(ms[i].covMtx.toString());
            }

            Annotation ann = ds.getAnnotations()[0];
//logger.print("Filtering  by basal1");
            for (String term : ann.getTerms()) {
                   //
                 if(!(term.toLowerCase().contains("basal1")))continue; 

                LinkedList<Datapoint>[] classifiedLists = new LinkedList[ms.length + 1];
                for (int i = 0; i < classifiedLists.length; i++) {
                    classifiedLists[i] = new LinkedList<>();
                }

                double totalSampleCount = 0;

                //logger.print("\nFiltering by CD34+orCD38+");
                double averageStat5 = 0;
                int Stat5Index = Optimization.indexOf(ds.getSideVarNames(), "pSTAT5");
                double averageTdT = 0;
                int TdTindex = Optimization.indexOf(featureNames, "TdT");
                //logger.print("Stat5 idx" + Stat5Index);
                //logger.print("last column will be the avg value of pStat5");
                for (Datapoint nd : ds.getDatapoints()) {
                    if (nd.getFullName().startsWith(term) /* && (nd.getVector()[8]>3.69||nd.getVector()[3]>3.69)*/) {
                        totalSampleCount++;
                        averageStat5 += nd.getSideVector()[Stat5Index];
                        averageTdT += nd.getVector()[TdTindex];
                        int nnIDX = -1;
                        double minDist = Double.MAX_VALUE;
                        for (int i = 0; i < ms.length; i++) {
                            double [] vec2 = new double[0];
                            //vec2[0] = nd.getSideVector()[Optimization.indexOf(ds.getSideVarNames(),"CD24")];
                            //vec2[1] = nd.getSideVector()[Optimization.indexOf(ds.getSideVarNames(),"CD179b")];
                            //vec2[2] = nd.getSideVector()[Optimization.indexOf(ds.getSideVarNames(),"HLADR")];
                            double dist = Math.acos(MatrixOp.getEuclideanCosine(ms[i].center.toArray(),nd.getVector()));
                            if (dist < minDist) {
                                minDist = dist;
                                nnIDX = i;
                            }
                        }
                        if (minDist < 2) {
                            classifiedLists[nnIDX].add(nd);
                        } else {
                            classifiedLists[files.length].add(nd);
                        }
                    }
                }
                
                averageStat5/=totalSampleCount;
                averageTdT/=totalSampleCount;
                System.out.print("\n" + term);
                //logger.print("Total sample count: "+ totalSampleCount);
                
                Cluster [] c = new Cluster[classifiedLists.length];
                
                for (int i = 0; i < classifiedLists.length; i++) {
                    c[i] = new Cluster(classifiedLists[i].toArray(new Datapoint[classifiedLists[i].size()]), new double[ds.getDimension()], new double[ds.getSideVarNames().length], term, new AngularDistance());
                    c[i].setID(i);
                }
                ClusterSet cs = new ClusterSet(dsIDX, ds, c, new AngularDistance(), term, term, averageStat5, term);
                //dataio.ClusterSetToFCSExporter.exportClusterSet(cs, new File("C:\\Users\\Nikolay\\Dropbox\\Kara-Nik\\ALL05v2\\ALL05v2 healthies to go thru classifier\\Plate 1-3 healthies classified\\"+term+ "_post_viSNE_withPop8.fcs"));
                
                for (int i = 0; i < classifiedLists.length; i++) {
                   /* if (i < files.length) {
                        //logger.print(files[i].getName());
                        ann.addTerm(classifiedLists[i].toArray(new N[classifiedLists[i].size()]), files[i].getName().substring(0, files[i].getName().length() - 4));
                    } else {
                        ann.addTerm(classifiedLists[i].toArray(new String[classifiedLists[i].size()]), "unclassified");
                    }*/
                    System.out.print("\t" + classifiedLists[i].size() / totalSampleCount);
                }
                System.out.print("\t" + averageStat5);
                System.out.print("\t" + averageTdT);
            }
   
        
    }
       
    }
}
