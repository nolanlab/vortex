/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hex;

import clustering.Dataset;
import dataIO.DatasetImporter;
import dataIO.DatasetStub;
import dataIO.ImportConfigObject;
import flowcyt_fcs.ExportFCS;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import util.IO;
import util.MatrixOp;

/**
 *
 * @author Nikolay Samusik
 */
public class AppendSOMtoFCS {
    
    public static void main(String[] args) throws Exception{
        
        DatasetStub dss = DatasetStub.createFromFCS(new File(args[0]));
        
        int xDim = Integer.parseInt(args[2]);
        int yDim = Integer.parseInt(args[3]);
        int numEpochs = Integer.parseInt(args[4]);
        
        String [] colNames = IO.getListOfStringsFromStream(new FileInputStream(new File(args[1]))).stream().toArray(String[]::new);
        
        ImportConfigObject ico = new ImportConfigObject(
                    "tempSOM",  
                    colNames, 
                    new String[0], 
                    ImportConfigObject.RescaleType.NONE, 
                    ImportConfigObject.TransformationType.ASINH, 
                    5, 
                    0.95, 
                   false,
                    -1, 
                    -1, 
                    -1);
        Dataset ds = DatasetImporter.importDataset(new DatasetStub[]{dss}, ico);
        
        double[][] mtx = new double[ds.getDatapoints().length][];
        
        for (int i = 0; i < mtx.length; i++) {
             mtx[i] = ds.getDatapoints()[i].getVector();
        }
        
       //SOM som = new SOM(mtx, xDim, yDim, numEpochs);
        
       int[] nodes =null; //= som.getNodes();
       
       float[][] outMtx = new float[nodes.length][];
       
        for (int i = 0; i < nodes.length; i++) {
            int node = nodes[i];
            double [] coord = new double[]{node%xDim, node/xDim};
            double [] out= MatrixOp.concat(dss.getRow(i), coord);
            for (int j = 0; j < out.length; j++) {
                outMtx[i][j] = (float) out[j];
            }
        }
        
        ExportFCS export = new ExportFCS();
        String [] scn = Arrays.copyOf(dss.getShortColumnNames(), dss.getShortColumnNames().length+2);
        String [] lcn = Arrays.copyOf(dss.getShortColumnNames(), dss.getShortColumnNames().length+2);
        scn[dss.getShortColumnNames().length] = lcn[dss.getShortColumnNames().length] = "cluster_SOM_X";
        scn[dss.getShortColumnNames().length+1] = scn[dss.getShortColumnNames().length+1] = "cluster_SOM_Y";
        
        export.writeFCSAsFloat(args[0]+"_SOM.fcs", outMtx, scn , lcn);
            
    }
    
}
