/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.supervised;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import clustering.Datapoint;
import main.Dataset;
import util.Correlation;
import util.MatrixOp;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class LDAModel {

    private double[] discriminant;
    private boolean useSideParams;
    private double[] referencePoint;
    private double cutoff;

    public int classify(Datapoint d) {
        double[] vec = useSideParams ? d.getSideVector() : d.getVector();
        double proj = MatrixOp.mult(discriminant, MatrixOp.diff(vec, referencePoint));
        return (proj > cutoff) ? 1 : 0;
    }

    public LDAModel(Dataset ds, int[] group1idx, int[] group2idx, boolean sideParams, boolean covariance) {
        createModel(ds, group1idx, group2idx, sideParams, covariance);
    }

    private void createModel(Dataset ds, int[] group1idx, int[] group2idx, boolean sideParams, boolean covariance) {
        this.useSideParams = sideParams;
        double[] avg1 = getGroupAverage(ds, group1idx, sideParams);
        referencePoint = avg1;
        double[] avg2 = getGroupAverage(ds, group2idx, sideParams);
        double[] disc = MatrixOp.diff(avg2, avg1);

        DenseDoubleMatrix2D covMatrix = new DenseDoubleMatrix2D(getGroupCovarianceMatrix(ds, group1idx, sideParams));
        DenseDoubleMatrix2D covMatrix2 = new DenseDoubleMatrix2D(getGroupCovarianceMatrix(ds, group2idx, sideParams));

        for (int i = 0; i < disc.length; i++) {
            for (int j = 0; j < disc.length; j++) {
                covMatrix.setQuick(i, j, covMatrix.getQuick(i, j) + covMatrix2.getQuick(i, j));
            }
        }

        double det = Algebra.DEFAULT.det(covMatrix);
        if (Math.abs(det) < 0.01 || !covariance) {
            logger.print("Covariance matrix is singular. Using diagonal only");
            for (int i = 0; i < disc.length; i++) {
                for (int j = 0; j < disc.length; j++) {
                    if (i != j) {
                        covMatrix.set(i, j, 0);
                    }
                }
            }
        }

        DoubleMatrix2D invCov = Algebra.DEFAULT.inverse(covMatrix);
        discriminant = Algebra.DEFAULT.mult(invCov, new DenseDoubleMatrix1D(disc)).toArray();

        //Normalizing the discriminant such that the diff between averages is 1.0
        MatrixOp.mult(disc, 1.0 / MatrixOp.mult(MatrixOp.diff(avg2, avg1), discriminant));
        findOptimalThreshold(ds, group1idx, group2idx);
    }

    private double findOptimalThreshold(Dataset ds, int[] group1idx, int[] group2idx) {
        double from = 0;
        double to = 1;
        double step = 0.01;
        double bestThreshold = from;
        double classR = 0.0;
        for (double t = from; t < to; t += step) {
            cutoff = t;
            double classR1 = 0;
            for (int i : group1idx) {
                if (classify(ds.getDatapointByID(i)) == 0) {
                    classR1++;
                }
            }
            classR1 /= group1idx.length;

            double classR2 = 0;
            for (int i : group2idx) {
                if (classify(ds.getDatapointByID(i)) == 1) {
                    classR2++;
                }
            }
            classR1 /= group2idx.length;
            double currClassR = 2.0 / (1.0 / classR1 + 1.0 / classR2);
            if (currClassR < classR) {
                classR = currClassR;
                bestThreshold = t;
            }
        }
        return bestThreshold;
    }

    public double[] getDiscriminant() {
        return discriminant;
    }

    public double[][] getGroupCovarianceMatrix(Dataset ds, int[] groupIDX, boolean sideParams) {
        int dim = (sideParams) ? ds.getSideVarNames().length : ds.getDimension();
        double[][] data = new double[groupIDX.length][dim];
        for (int i = 0; i < data.length; i++) {
            data[i] = (sideParams) ? ds.getDatapointByID(groupIDX[i]).getSideVector() : ds.getDatapointByID(groupIDX[i]).getVector();
        }
        double[][] dataT = Algebra.DEFAULT.transpose(new DenseDoubleMatrix2D(data)).toArray();
        double[][] covMtx = new double[dim][dim];
        for (int i = 0; i < covMtx.length; i++) {
            for (int j = 0; j < covMtx.length; j++) {
                covMtx[i][j] = Correlation.getCenteredCovariance(dataT[i], dataT[j]);
            }
        }
        return covMtx;
    }

    public double[] getGroupAverage(Dataset ds, int[] groupIDX, boolean sideParams) {
        int dim = (sideParams) ? ds.getSideVarNames().length : ds.getDimension();
        double[] currAvg = new double[dim];

        for (int j : groupIDX) {
            double[] vec = (sideParams) ? ds.getDatapointByID(j).getSideVector() : ds.getDatapointByID(j).getVector();
            MatrixOp.sum(currAvg, vec);
        }
        MatrixOp.mult(currAvg, 1.0 / groupIDX.length);
        return currAvg;
    }
}
