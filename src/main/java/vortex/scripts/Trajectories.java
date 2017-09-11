/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.scripts;

import cern.colt.function.DoubleFunction;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import clustering.Datapoint;
import clustering.Dataset;
import dataIO.DatasetStub;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import util.MatrixOp;

/**
 *
 * @author Nikolay Samusik
 */
public class Trajectories {

    static File[] inFiles = new File[]{new File("D:\\YandexDisk\\Working folder\\Bahareh\\Trajectories\\CNS.txt")}; //, new File("D:\\YandexDisk\\Working folder\\Bahareh\\Trajectories\\CNS.txt")};

    private final static int out_dim = 2;

    private static final int numIterations = 10000000;

    private static final int numRestarts = 100;

    private static final double correlThs = 0.2;

    private static double previousRatio = 1000;
    private static DoubleMatrix2D disc;

    private static double globalBestRatio = 1000;
    private static DoubleMatrix2D globalBestDisc;

    public static void main(String[] args) throws Exception {
        for (File f : inFiles) {
            Dataset ds = readFile(f);

            Map<String, List<Datapoint>> dpGroups = Arrays.asList(ds.getDatapoints()).stream().collect(Collectors.groupingBy(d -> d.getName().split("_")[0]));

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
                    names[dpCnt] = d.getName();
                    dpCnt++;
                }
                groupCnt++;
            }

            DoubleMatrix2D mtx = new DenseDoubleMatrix2D(inmtx);

            final AtomicInteger ai = new AtomicInteger();

            int[] oneGroup = new int[groups.length];
            Arrays.fill(oneGroup, 1);

            for (int g = 0; g < numRestarts; g++) {
                previousRatio = 1000;
                System.out.println("Global iteration: " + g);

                disc = new DenseDoubleMatrix2D(ds.getNumDimensions(), out_dim);
                disc.assign(new DoubleFunction() {
                    @Override
                    public double apply(double d) {
                        return Math.random() - 0.5;
                    }
                });

                addRandom(disc, disc, 1);

                ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

                for (int c = 0; c < Runtime.getRuntime().availableProcessors(); c++) {
                    final int cpu = c;

                    final DoubleMatrix2D nextDisc = disc.copy();

                    final int localNumIt = numIterations / Runtime.getRuntime().availableProcessors();

                    es.execute(new Runnable() {
                        @Override
                        public void run() {

                            iter:
                            for (int i = 0; i < localNumIt; i++) {

                                synchronized (ai) {
                                    addRandom(disc, nextDisc, 1); //, (localNumIt - i) / (double) localNumIt);
                                }

                                double[][] ndD = Algebra.DEFAULT.transpose(nextDisc).toArray();

                                for (int j = 0; j < ndD.length; j++) {
                                    for (int k = j + 1; k < ndD.length; k++) {
                                        if (MatrixOp.lenght(ndD[j]) == 0 || MatrixOp.lenght(ndD[k]) == 0 || Math.abs(MatrixOp.getEuclideanCosine(ndD[j], ndD[k])) > correlThs) {
                                            continue iter;
                                        }
                                    }
                                }

                                DoubleMatrix2D proj = Algebra.DEFAULT.mult(mtx, nextDisc);

                                double[][] projD = Algebra.DEFAULT.transpose(nextDisc).toArray();

                                for (int j = 0; j < ndD.length; j++) {
                                    for (int k = j + 1; k < ndD.length; k++) {
                                        if (Math.abs(MatrixOp.getEuclideanCosine(projD[j], projD[k])) > correlThs) {
                                            continue iter;
                                        }
                                    }
                                }

                                double varGroup = getSumDeviations(proj, groups);
                                double varTotal = getSumDeviations(proj, oneGroup);

                                double varRatio = varGroup / varTotal;

                                synchronized (ai) {
                                    if (varRatio < previousRatio) {
                                        //System.out.println("Th" + cpu + " % " + ((int) (((100 * i) / localNumIt))) + ": " + varRatio + "<" + previousRatio);
                                        previousRatio = varRatio;
                                        disc.assign(nextDisc);
                                    }
                                }

                            }
                        }
                    });
                }
                es.shutdown();
                es.awaitTermination(1000, TimeUnit.DAYS);

                if (previousRatio < globalBestRatio) {
                    System.out.println("Global improvement:" + previousRatio + "<" + globalBestRatio);
                    globalBestRatio = previousRatio;
                    globalBestDisc = disc.copy();
                }

            }

            DoubleMatrix2D proj = Algebra.DEFAULT.mult(mtx, globalBestDisc);

            System.out.println("Discriminant");
            System.out.println(globalBestDisc.toString());

            System.out.println("Projection");
            for (int i = 0; i < groups.length; i++) {
                System.out.println(names[i] + "\t" + proj.get(i, 0) + "\t" + proj.get(i, 1));
            }
        }
    }

    public static double[][] getDistanceMtx(DoubleMatrix2D in) {
        double[][] vec = in.toArray();
        double[][] out = new double[vec.length][vec.length];

        for (int i = 0; i < out.length; i++) {
            for (int j = i + 1; j < out.length; j++) {
                out[i][j] = MatrixOp.getEuclideanDistance(vec[i], vec[j]);
            }

        }
        return out;
    }

    public static double correlateDistMatrices(double[][] mtx1, double[][] mtx2) {
        double dp = 0, sumsq1 = 0, sumsq2 = 0;

        for (int i = 0; i < mtx2.length; i++) {
            for (int j = i + 1; j < mtx2.length; j++) {
                dp += mtx1[i][j] * mtx2[i][j];

                sumsq1 += mtx1[i][j] * mtx1[i][j];
                sumsq2 += mtx2[i][j] * mtx2[i][j];
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

        return new Dataset("trajectories", dp, ds.getLongColumnNames());
    }

}
