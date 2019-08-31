/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.scripts;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import umontreal.iro.lecuyer.probdist.StudentDist;
import umontreal.iro.lecuyer.randvar.NormalGen;
import umontreal.iro.lecuyer.randvar.StudentGen;
import umontreal.iro.lecuyer.randvarmulti.MultinormalCholeskyGen;
import umontreal.iro.lecuyer.randvarmulti.RandomMultivariateGen;
import umontreal.iro.lecuyer.rng.GenF2w32;
import umontreal.iro.lecuyer.rng.MRG31k3p;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import sandbox.clustering.AngularDistance;
import vortex.clustering.XShiftClustering;
import sandbox.clustering.Cluster;
import sandbox.clustering.ClusterMember;
import sandbox.clustering.ClusterSet;
import sandbox.clustering.Datapoint;
import sandbox.clustering.Dataset;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class GenerateSynthDatasetDim {

    private class MultinomialStudentGenerator extends RandomMultivariateGen {

        private final double[] mu;
        private final DenseDoubleMatrix2D sigma;
        private final StudentGen gen;
        private final StudentDist dist;

        public MultinomialStudentGenerator(double[] mu, DenseDoubleMatrix2D sigma) {
            this.mu = mu;
            this.sigma = sigma;
            dist = new StudentDist(2);
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

    public GenerateSynthDatasetDim() {

    }
    private static int[] SIZES;

    private static int dim = 30;
    private static final double[] scalingBounds = new double[]{0.75, 1.5};
    private static final Random rnd = new Random();

    public static void main(String[] args) throws Exception {

        for (int K = 7; K < 100; K++) {
            logger.print("K="+K);    
        sz:
        for (int di = 3; di <= 40; di += 1) {
            int sz = 1;
            dim = di;
            int SIZE= 10000;
            
               DenseDoubleMatrix1D mu = new DenseDoubleMatrix1D(dim);
              DenseDoubleMatrix2D  sigma = new DenseDoubleMatrix2D(dim, dim);

                for (int j = 0; j < dim; j++) {
                    mu.setQuick(j, 5);
                    sigma.setQuick(j, j, 1);
                }
            
            MultinormalCholeskyGen gen =  new MultinormalCholeskyGen(new NormalGen(new GenF2w32()), mu.toArray(), sigma);
          
            ArrayList<Datapoint> dp = new ArrayList<>();

                for (int j = 0; j < SIZE; j++) {
                    double[] vec = new double[dim];
                    gen.nextPoint(vec);
                    Datapoint d = new Datapoint("c_" + j, vec, new double[]{j}, dp.size());
                    dp.add(d);
                }

            String[] paramNames = new String[dim];
            for (int i = 0; i < paramNames.length; i++) {
                paramNames[i] = "param " + i;
            }

            Dataset ds = new Dataset("Synth_Student_30_2", dp.toArray(new Datapoint[dp.size()]), paramNames, new String[]{"Density"});

            // ConnectionManager.setDatabaseHost(new ConnectionManager.DatabaseHost(DatabaseHost.HOST_HSQLDB, "D:\\hsqldb\\greg", "local file", "sa", ""));
            // ConnectionManager.getStorageEngine().saveDataset(ds, true);
            // ConnectionManager.getStorageEngine().shutdown();
            //System.exit(3);
            logger.setOutputMode(logger.OUTPUT_MODE_NONE);
            XShiftClustering xsc = new XShiftClustering(new AngularDistance(),ds);

            Integer[] Ka = new Integer[]{K};
            

            xsc.setK(Ka);
            xsc.setUseVMF(false);
            xsc.setSave(false);
            long millis = Calendar.getInstance().getTimeInMillis();
            ClusterSet[] cs = xsc.doBatchClustering(ds, null);
            logger.setOutputMode(logger.OUTPUT_MODE_CONSOLE);
         
            logger.print("dim\t"+di+"\tclusters\t"+cs[0].getClusters().length);
            //logger.print(sumSizes+"\t"+(Calendar.getInstance().getTimeInMillis()-millis));
        }
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
