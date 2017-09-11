/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.scripts;

import dataIO.DatasetStub;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import util.Correlation;
import util.MatrixOp;
import util.Shuffle;

/**
 *
 * @author Nikolay Samusik
 */
public class BootstrapClusterFreq  {

    public static void main(String[] args) throws Exception{

        DatasetStub dss = DatasetStub.createFromTXT(new File("D:\\YandexDisk\\Working folder\\OC\\CancerCell revision\\OC3_minusFive_K35_reclustering.txt"));

        BufferedWriter  out = new BufferedWriter(new FileWriter(new File("D:\\YandexDisk\\Working folder\\OC\\CancerCell revision\\OC3_minusFive_K35_reclustering_bootstrappedUC.txt")));
        
        int numIterations = 1000;

        double[][] vec = new double[(int) dss.getRowCount()][];

        for (int i = 0; i < vec.length; i++) {
            vec[i] = dss.getRow(i);
            double[] zero = new double[137072-vec[i].length];
            vec[i] = MatrixOp.concat(vec[i], zero);
        }
        //

        for (int i = 0; i < numIterations; i++) {
            
            for (int j = 0; j < vec.length; j++) {
                Shuffle.shuffleArray(vec[j]);
            }
            
            double [] sum = new double[vec[0].length];
            
           
            for (int j = 0; j < vec.length; j++) {
                sum = MatrixOp.sum(sum,vec[j]);
            }
            
            int countNonzero = 0;
            
            for (int j = 0; j < sum.length; j++) {
                if(sum[j]>0) countNonzero++;
                
            }
            
            System.out.println(countNonzero);
            

            for (int x = 0; x < vec.length; x++) {
                for (int y = x+1; y < vec.length; y++) {
                    double corr = Correlation.getCenteredCorrelation(vec[y], vec[x]);
                    out.write(corr+"\n");
                }
            }

        }

    }

}
