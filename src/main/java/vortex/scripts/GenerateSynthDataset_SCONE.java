/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.scripts;

import sandbox.clustering.ClusterSet;
import sandbox.clustering.Cluster;
import sandbox.clustering.Datapoint;
import sandbox.clustering.Dataset;
import sandbox.clustering.ClusterMember;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import umontreal.iro.lecuyer.probdist.StudentDist;
import umontreal.iro.lecuyer.probdistmulti.MultiNormalDist;
import umontreal.iro.lecuyer.randvar.NormalGen;
import umontreal.iro.lecuyer.randvar.StudentGen;
import umontreal.iro.lecuyer.randvarmulti.MultinormalPCAGen;
import umontreal.iro.lecuyer.randvarmulti.RandomMultivariateGen;
import umontreal.iro.lecuyer.rng.MRG31k3p;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import util.LinePlusExponent;
import util.MatrixOp;
import util.logger;
import vortex.clustering.XShiftClustering;
import vortex.util.Config;
import vortex.util.ConnectionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

/**
 *
 * @author Nikolay
 */
public class GenerateSynthDataset_SCONE {

    private static class MultinomialStudentGenerator extends RandomMultivariateGen {

        private final double[] mu;
        private final DenseDoubleMatrix2D sigma;
        private final StudentGen gen;
        private final StudentDist dist;

        public MultinomialStudentGenerator(double[] mu, DenseDoubleMatrix2D sigma, int degFreedom) {
            this.mu = mu;
            this.sigma = sigma;
            dist = new StudentDist(degFreedom);
            gen = new StudentGen(new MRG32k3a(), dist);
        }

        @Override
        public int getDimension() {
            return mu.length;
        }

        @Override
        public void nextPoint(double[] p) {
            for (int i = 0; i < p.length; i++) {
                p[i] = mu[i] + (gen.nextDouble() * sigma.getQuick(i, i));
            }
        }

        public double density(double[] p) {
            double d = 1;
            for (int i = 0; i < p.length; i++) {
                d *= dist.density((p[i] - mu[i]) / sigma.getQuick(i, i));
            }
            return d;
        }

    }

    public GenerateSynthDataset_SCONE() {
    }
    private static int[] SIZES;

    private static int dim = 50;
    private static final double[] scalingBounds = new double[]{0.5, 2};
    private static final Random rnd = new Random();

    public static void main(String[] args) throws Exception {

        int z = 0;
        sz:
        for (int sz = 10; sz <= 10; sz += 10) {
            SIZES = new int[sz];
            //dim = 50sz + 5;
            for (int i = 0; i < SIZES.length; i++) {
                SIZES[i] = (int) (10000 * Math.random()) + 200;
            }

            DenseDoubleMatrix1D[] mu = new DenseDoubleMatrix1D[SIZES.length];
            DenseDoubleMatrix2D[] sigma = new DenseDoubleMatrix2D[SIZES.length];
            RandomMultivariateGen[] gen = new RandomMultivariateGen[SIZES.length];
            MultiNormalDist[] dist = new MultiNormalDist[SIZES.length];

            i: for (int i = 0; i < SIZES.length; i++) {
                mu[i] = new DenseDoubleMatrix1D(dim);
                sigma[i] = new DenseDoubleMatrix2D(dim, dim);

                for (int j = 0; j < dim; j++) {
                    mu[i].setQuick(j, Math.signum(rnd.nextDouble() - 0.5) * getScaledRnd() * 2);
                    if (i > 0 && Math.random() > 0.1) {
                        mu[i].setQuick(j, mu[i - 1].getQuick(j));
                    }
                    sigma[i].setQuick(j, j, getScaledRnd());
                }

                if (i > 0) {
                    if (MatrixOp.getEuclideanDistance(mu[i].toArray(), mu[i - 1].toArray()) < 2.0) {
                        i--;
                        continue i;
                    }
                }

                double[] sig = new double[sigma[i].columns()];
                for (int j = 0; j < sig.length; j++) {
                    sig[j] = sigma[i].get(j, j);
                }
                gen[i] = (i % 2 == 0) ? new MultinormalPCAGen(new NormalGen(new MRG31k3p()), mu[i].toArray(), sigma[i].toArray()) : new MultinomialStudentGenerator(mu[i].toArray(), sigma[i], 3);

                dist[i] = new MultiNormalDist(mu[i].toArray(), sigma[i].toArray());

                if (i % 2 == 0) {
                    logger.print("Component#" + i + "\nNormal Distribution\n");
                } else {
                    logger.print("Component#" + i + "\nStudent Distribution\n");
                }
                logger.print("mu:\n " + mu[i]);
                logger.print("sigma:\n " + sigma[i]);
            }

            /*
            i:
            for (int i = 0; i < SIZES.length; i += 2) {
                int j = i + 1;
                if (j < SIZES.length) {
                    double targetDist = (3.1 + (sz / 10.0)) + Math.random(); //* 5;
                    double[] diff = new double[dim];
                    for (int k = 0; k < diff.length; k++) {
                        diff[k] = Math.random();
                    }
                    diff = MatrixOp.toUnityLen(diff);
                    MatrixOp.mult(diff, targetDist);
                    mu[j] = new DenseDoubleMatrix1D(MatrixOp.sum(diff, mu[i].toArray()));
                }
            }*/
 /*

            for (int i = 0; i < SIZES.length; i++) {
                for (int j = i + 1; j < SIZES.length; j++) {
                    double [] diff = MatrixOp.diff(mu[i].toArray(), mu[j].toArray());
                    for (int k = 0; k < diff.length; k++) {
                        diff[k]/=(sigma[i].get(k,k)+sigma[j].get(k,k))/2;
                    }
                    if (MatrixOp.lenght(diff) < (2.5 + (sz / 20.0))) {
                        sz -= 10;
                        logger.print("Too close: " + MatrixOp.lenght(MatrixOp.diff(mu[i].toArray(), mu[j].toArray())) + " Continuing");
                        continue sz;
                    }
                }
            }*/
            int sumSizes = 0;

            for (int i = 0; i < SIZES.length; i++) {
                sumSizes += SIZES[i];
            }

            ArrayList<Datapoint> dp = new ArrayList<>();

            for (int i = 0; i < gen.length; i++) {
                //logger.print("***********************working on dist: " + i);
                for (int j = 0; j < SIZES[i]; j++) {
                    //if(i==1) return;
                    if (j % 100 == 0) {
                        //logger.print("generating " + j);
                    }
                    double[] vec = new double[dim];
                    gen[i].nextPoint(vec);

                    double dens = 0;
                    for (int k = 0; k < SIZES.length; k++) {
                        dens += ((k % 2 == 0) ? dist[k].density(vec) : ((MultinomialStudentGenerator) gen[k]).density(vec)) * (SIZES[k] / (double) sumSizes);
                    }
                    //  logger.print(Arrays.toString(vec));
                    Datapoint d = new Datapoint("c_" + i + "_" + j, vec, new double[]{dens, i}, dp.size());
                    dp.add(d);
                }
            }

            String[] paramNames = new String[dim];
            for (int i = 0; i < paramNames.length; i++) {
                paramNames[i] = "param " + i;
            }

            Dataset ds = new Dataset("Synth_Norm+Student_10comp_50dim_carryover=0.9_SCONE", dp.toArray(new Datapoint[dp.size()]), paramNames, new String[]{"Density", "ComponentID"});



            ConnectionManager.setDatabaseHost(Config.getDefaultDatabaseHost());//new ConnectionManager.get(DatabaseHost.HOST_HSQLDB, "D:\\hsqldb\\greg", "local file", "sa", ""));
            ConnectionManager.getStorageEngine().saveDataset(ds, true);
            ConnectionManager.getStorageEngine().shutdown();
            System.exit(3);

            /*
            logger.setOutputMode(logger.OUTPUT_MODE_NONE);
            XShiftClustering xsc = new XShiftClustering(new AngularDistance());

            Integer[] Ka = new Integer[49];
            int n = 0;

            for (int i = 100; i >= 4; i -= 2) {
                Ka[n++] = i;
            }

            xsc.setK(Ka);
            xsc.setUseVMF(false);
            xsc.setSave(false);
            long millis = Calendar.getInstance().getTimeInMillis();
            ClusterSet[] cs = xsc.doBatchClustering(ds, null);
            logger.setOutputMode(logger.OUTPUT_MODE_CONSOLE);
            logger.print("K\tnClus");
            double[] x = new double[Ka.length];
            double[] y = new double[Ka.length];
            for (int i = 0; i < cs.length; i++) {
                x[i] = Ka[i];
                y[i] = cs[i].getNumberOfClusters();
                logger.print(cs[i].getMainClusteringParameterValue() + "\t" + cs[i].getNumberOfClusters());
            }

            int elboX = (int) (LinePlusExponent.findElbowPointLinePlusExp(y, x));
            logger.setOutputMode(logger.OUTPUT_MODE_CONSOLE);
            for (int i = 0; i < cs.length; i++) {
                if (Ka[i] == 74 || Ka[i] == 2 * (elboX / 2) || Ka[i] == 6) {
                    printContigencyTable(cs[i], gen.length);
                }
            }

            logger.print("Optimal K =" + elboX);
            z++;
            //logger.print(sumSizes+"\t"+(Calendar.getInstance().getTimeInMillis()-millis));
            */
        }

        //
    }

    private static double getScaledRnd() {
        return (rnd.nextDouble() * (scalingBounds[1] - scalingBounds[0])) + scalingBounds[0];
    }

    private static void printContigencyTable(ClusterSet cs, int numClasses) {
        try {
            double[][] contigTable = new double[cs.getClusters().length][numClasses];
            Cluster[] cl = cs.getClusters();
            for (int i = 0; i < cl.length; i++) {
                for (ClusterMember cm : cl[i].getClusterMembers()) {
                    int cid = Integer.parseInt(cm.getDatapoint().getFullName().substring(2, cm.getDatapoint().getFullName().indexOf("_", 2)));
                    contigTable[i][cid] += 1.0 / SIZES[cid];
                }
            }
            logger.print(new DenseDoubleMatrix2D(contigTable));
        } catch (Exception e) {
            logger.print(e);
        }

    }
}
