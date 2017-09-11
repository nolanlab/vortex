/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.scripts;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import umontreal.iro.lecuyer.util.DMatrix;
import util.DefaultEntry;
import util.MatrixOp;
import util.logger;
import vortex.mahalonobis.CovarianceMatrix;
import vortex.util.ProfilePCA;

/**
 *
 * @author Nikolay Samusik
 */
public class Import65KPBMCmatrix {

    private static float[][] loadMatrixFromSPARSE(File inputFile) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));
        String line = null;
        do {
            line = reader.readLine();
        } while (line.startsWith("%"));

        StringTokenizer tokens = new StringTokenizer(line, " ", false);
        int numRows = Integer.parseInt(tokens.nextToken());
        int numColumns = Integer.parseInt(tokens.nextToken());
        int rows = Integer.parseInt(tokens.nextToken());

        System.out.println("Creating matrix:" + numRows + "x" + numColumns);
        float[][] M = new float[numRows][numColumns];
        System.out.println("Done");
        long cnt = 0;
        while ((line = reader.readLine()) != null) {
            tokens = new StringTokenizer(line, " ", false);
            cnt++;
            if (cnt % 10000 == 0) {
                System.out.println("Import" + ((100 * cnt) / rows) + "%");
            }
            String rawRow = tokens.nextToken();
            String rawCol = tokens.nextToken();
            String rawD = tokens.nextToken();
            M[Integer.parseInt(rawRow) - 1][Integer.parseInt(rawCol) - 1] = Float.parseFloat(rawD);
        }
        return M;
    }

    public static void main(String[] args) throws Exception {

        File selF = new File("D:\\Diseffusion\\transformed_mtx_top1000.object");
        float[][] sel = null;
        if (!selF.exists()) {

            File mtxF = new File("D:\\Diseffusion\\transformed_mtx.object");

            float[][] raw = null;
            if (!mtxF.exists()) {
                System.out.println("Parsing matrix:");
                raw = loadMatrixFromSPARSE(new File("C:\\Users\\Nikolay Samusik\\Dropbox\\Diseffusion\\RNAseq-68K-PBMCs\\filtered_matrices_mex\\hg19\\matrix.mtx"));
                System.out.println("Serializing matrix:");
                FileOutputStream fout = new FileOutputStream(mtxF.getAbsolutePath());
                ObjectOutputStream oos = new ObjectOutputStream(fout);
                oos.writeObject(raw);
            } else {
                System.out.println("Reading matrix object from file:");
                FileInputStream fin = new FileInputStream(mtxF.getAbsolutePath());
                ObjectInputStream ois = new ObjectInputStream(fin);
                raw = (float[][]) ois.readObject();
                System.out.println("Read: " + raw.length + "x" + raw[0].length);
            }

            sel = selectGenes(raw, 1000);
            System.out.println("Serializing selected gene matrix: " + sel.length + "x" + sel[0].length);
            FileOutputStream fout = new FileOutputStream(selF.getAbsolutePath());
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(sel);
        } else {
            System.out.println("Reading selected gene matrix object from file:");
            FileInputStream fin = new FileInputStream(selF.getAbsolutePath());
            ObjectInputStream ois = new ObjectInputStream(fin);
            sel = (float[][]) ois.readObject();
            System.out.println("Read: " + sel.length + "x" + sel[0].length);
        }
        
        double [][] transp = getTranspose(sel);
        
        double [][] pc = getPrincipleComponents(transp, 50);
        
        double [][] proj = new double[transp.length][];
        
        for (int i = 0; i < proj.length; i++) {
           proj[i] =  MatrixOp.mult(pc, transp[i]);
        }
        
        
        File out = new File("C:\\Users\\Nikolay Samusik\\Dropbox\\Diseffusion\\RNAseq-68K-PBMCs\\PCA50_projection.csv");
        BufferedWriter bw = new  BufferedWriter(new FileWriter(out));
        
        for (double[] ds : proj) {
            String res = Arrays.toString(ds);
            res = res.substring(1, res.length()-2);
            bw.write(res + "\n");
        }
        

        /*
        for (int i = 0; i < sel.length; i++) {
            StringBuilder sb = new StringBuilder("Gene#" + i + ": ");
            float avg =computeAvg(sel[i]);
            sb.append("avg: " + avg);
            sb.append(", sd: " + computeSD(sel[i], avg));
            float[] fs = sel[i];
            for (int j = 0; j < fs.length; j++) {
                if(fs[j]==0)continue;
                
                sb.append(fs[j]);
                sb.append(", ");
            }
            System.out.println(sb);

        }*/
    }
    
    public static double [][] getTranspose(float[][] mtx){
        double[][] ret = new double[mtx[0].length][mtx.length];
        for (int i = 0; i < mtx.length; i++) {
            for (int j = 0; j < mtx[0].length; j++) {
                ret[j][i] = mtx[i][j];
            }
            
        }
        return ret;
    }
    
    public static  double[][] getPrincipleComponents(double[][] mtx, int num) {
        double[] lambda = new double[mtx.length];
        double[][] components = DMatrix.PCADecompose(new DenseDoubleMatrix2D(CovarianceMatrix.covarianceMatrix(mtx)), lambda).toArray();

        Entry<Double, double[]> comp[] = new Entry[components.length];
        for (int i = 0; i < comp.length; i++) {
            comp[i] = new DefaultEntry<>(lambda[i], components[i]);
        }
        Arrays.sort(comp, (a, b) -> b.getKey().compareTo(a.getKey()));

        double[][] ret = new double[num][];

        for (int i = 0; i < ret.length; i++) {
            ret[i] = comp[i].getValue();
            System.out.println("PC"+1+" eig:"+comp[i].getKey() + " len:"+ MatrixOp.lenght(comp[i].getValue()) + "size:"  + comp[i].getValue().length);
        }
        return ret;
    }

    public static float[][] selectGenes(float[][] mtx, int numGenes) {
        Entry<Float, float[]>[] dif = new Entry[mtx.length];

        for (int i = 0; i < dif.length; i++) {
            float avg = computeAvg(mtx[i]);
            float sd = computeSD(mtx[i], avg);
            float ratio = sd / avg;
            if (Float.isInfinite(ratio) || Float.isNaN(ratio)) {
                ratio = 0;
            }
            dif[i] = new DefaultEntry<Float, float[]>(ratio, mtx[i]);
        }

        Arrays.sort(dif, (a, b) -> b.getKey().compareTo(a.getKey()));

        float[][] res = new float[numGenes][];

        System.out.println("Returning top " + numGenes + "genes with highest SD/avg, out of " + mtx.length);

        for (int i = 0; i < res.length; i++) {
            System.out.println("#" + i + ": " + dif[i].getKey());
            res[i] = dif[i].getValue();
        }

        return res;
    }

    private static float computeAvg(float[] vec) {
        float avg = 0;
        for (float f : vec) {
            avg += f;
        }
        return avg / vec.length;
    }

    private static float computeSD(float[] vec, final float avg) {
        float SD = 0;
        float diff = 0;
        for (float f : vec) {
            diff = f - avg;
            SD += diff * diff;
        }
        return (float) SD / (vec.length - 1);
    }

}
