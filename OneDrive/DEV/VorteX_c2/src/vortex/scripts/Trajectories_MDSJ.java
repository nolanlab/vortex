/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.scripts;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import clustering.Datapoint;
import clustering.Dataset;
import dataIO.DatasetStub;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JFrame;
import org.math.plot.Plot3DPanel;
import util.ColorPalette;
import util.MatrixOp;

/**
 *
 * @author Nikolay Samusik
 */
public class Trajectories_MDSJ {

    static File[] inFiles = new File[]{new File("D:\\YandexDisk\\Working folder\\Bahareh\\Trajectories\\CNS.txt")}; //, new File("D:\\YandexDisk\\Working folder\\Bahareh\\Trajectories\\CNS.txt")};

    private final static int out_dim = 2;

    private static final int numIterations = 10000000;

    private static final int numRestarts = 1;

    private static final double correlThs = 0.2;

    private static double previousCorrel = 0;
    private static DoubleMatrix2D disc;

    private static double globalBestCorrel = 0;
    private static DoubleMatrix2D globalBestDisc;

    public static void main(String[] args) throws Exception {
        for (File f : inFiles) {
            Dataset ds = readFile(f);

            Map<String, List<Datapoint>> dpGroups = Arrays.asList(ds.getDatapoints()).stream().collect(Collectors.groupingBy(d -> d.getNameWithoutFileName().split("_")[0]));

            String[] names = new String[ds.getDatapoints().length];

            double[][] inmtx = new double[ds.getDatapoints().length][];
            int[] groups = new int[ds.getDatapoints().length];
            int groupCnt = 1;
            int dpCnt = 0;
            for (String s : dpGroups.keySet()) {
                List<Datapoint> lst = dpGroups.get(s);
                for (Datapoint d : lst) {
                    inmtx[dpCnt] = d.getVector();
                    groups[dpCnt] = groupCnt;
                    names[dpCnt] = d.getNameWithoutFileName();
                    dpCnt++;
                }
                groupCnt++;
            }

            DoubleMatrix2D mtx = new DenseDoubleMatrix2D(inmtx);

            final double[][] sourceDistMtx = getDistanceMtx(inmtx);

            double[][] disc = mdsj.MDSJ.classicalScaling(mtx.toArray());

            disc = Arrays.copyOf(disc, 2);

            DoubleMatrix2D proj = Algebra.DEFAULT.mult(mtx, Algebra.DEFAULT.transpose(new DenseDoubleMatrix2D(disc)));

            System.out.println("Discriminant");
            System.out.println(disc.toString());

            System.out.println("Projection");
            for (int i = 0; i < groups.length; i++) {
                String out = names[i];

                for (int j = 0; j < out_dim; j++) {
                    double[] ds1 = disc[j];
                    names[i] += "\t" + proj.get(i, j);
                }
                System.out.println(out);
            }
            
            
        }
    }

    

    public static double[][] getDistanceMtx(double[][] vec) {

        double[][] out = new double[vec.length][vec.length];

        for (int i = 0; i < out.length; i++) {
            for (int j = i + 1; j < out.length; j++) {
                out[i][j] = MatrixOp.getEuclideanDistance(vec[i], vec[j]);
            }
        }

        return out;
    }

    public static double correlateDistMatrices(double[][] mtx1, double[][] mtx2) {
        double avgMtx1 = 0, avgMtx2 = 0;
        double cnt = 0;
        for (int i = 0; i < mtx2.length; i++) {
            for (int j = i + 1; j < mtx2.length; j++) {
                avgMtx1 += mtx1[i][j];
                avgMtx2 += mtx2[i][j];
                cnt++;
            }
        }

        avgMtx1 /= cnt;
        avgMtx2 /= cnt;

        double dp = 0, sumsq1 = 0, sumsq2 = 0;

        for (int i = 0; i < mtx2.length; i++) {
            for (int j = i + 1; j < mtx2.length; j++) {
                double d1 = (mtx1[i][j] - avgMtx1);
                double d2 = (mtx2[i][j] - avgMtx2);
                dp += d1 * d2;
                sumsq1 += d1 * d1;
                sumsq2 += d2 * d2;
            }
        }
        return dp / Math.sqrt(sumsq1 * sumsq2);
    }

    public static void addRandom(DoubleMatrix2D in, DoubleMatrix2D out, double temperature) {
        out.assign(in);
        int randomIndexX = (int) (Math.random() * in.columns());
        int randomIndexY = (int) (Math.random() * in.rows());
        out.setQuick(randomIndexX, randomIndexY, in.getQuick(randomIndexX, randomIndexY) + ((Math.random() - 0.5) * temperature));
    }

    public static double getSumDeviations(DoubleMatrix2D proj, int[] groups) {

        int idx = 0;
        int currGroup;

        double[][] projD = proj.toArray();

        do {
            double[] avgGroup = new double[proj.columns()];
            int grpSize = 0;
            currGroup = groups[idx];
            while (idx < groups.length && groups[idx] == currGroup) {
                avgGroup = MatrixOp.sum(avgGroup, projD[idx]);
                idx++;
                grpSize++;
            }

            idx -= grpSize;
            MatrixOp.mult(avgGroup, 1.0 / (double) grpSize);

            while (idx < groups.length && groups[idx] == currGroup) {
                projD[idx] = MatrixOp.diff(projD[idx], avgGroup);
                idx++;
            }
        } while (idx < groups.length);

        double sumSqLen = 0;
        for (double[] ds : projD) {
            sumSqLen += asinh(MatrixOp.mult(ds, ds));
        }
        return sumSqLen;
    }

    public static double asinh(double val) {
        return Math.log(val + Math.sqrt(val * val + 1));
    }

    public static double getMedianDev(DoubleMatrix2D proj, int[] groups) {

        int idx = 0;
        int currGroup;

        double[][] projD = proj.toArray();

        do {
            double[] avgGroup = new double[proj.columns()];
            int grpSize = 0;
            currGroup = groups[idx];
            while (idx < groups.length && groups[idx] == currGroup) {
                avgGroup = MatrixOp.sum(avgGroup, projD[idx]);
                idx++;
                grpSize++;
            }

            idx -= grpSize;
            MatrixOp.mult(avgGroup, 1.0 / (double) grpSize);

            while (idx < groups.length && groups[idx] == currGroup) {
                projD[idx] = MatrixOp.diff(projD[idx], avgGroup);
                idx++;
            }
        } while (idx < groups.length);

        double[] len = new double[groups.length];
        for (int i = 0; i < groups.length; i++) {
            len[i] = Math.sqrt(MatrixOp.mult(projD[i], projD[i]));
        }
        Arrays.sort(len);
        return len[len.length / 2];
    }

    private static Dataset readFile(File f) throws FileNotFoundException, IOException {
        DatasetStub ds = DatasetStub.createFromTXT(f);
        Datapoint[] dp = new Datapoint[(int) ds.getRowCount()];
        for (int i = 0; i < dp.length; i++) {
            dp[i] = new Datapoint(ds.getRowName(i), ds.getRow(i), i);
        }
        System.err.println("Skipped rows: ");
        for (String st : ds.getSkippedRows()) {
            System.err.println(st);
        }

        return new Dataset("trajectories", dp, ds.getColumnNames());
    }

}
