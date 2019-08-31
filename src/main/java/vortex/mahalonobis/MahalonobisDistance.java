/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.mahalonobis;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import java.sql.SQLException;
import util.MatrixOp;
import util.logger;

/**
 *
 * @author Zina
 */
public class MahalonobisDistance {

    public DenseDoubleMatrix2D covMtx;
    public DenseDoubleMatrix1D center;
    private DenseDoubleMatrix2D invCovMtx;
    private boolean Covariance = false;

    public MahalonobisDistance( DenseDoubleMatrix1D center, DenseDoubleMatrix2D covMtx) {
        this.covMtx = covMtx;
        this.center = center;
        this.invCovMtx = (DenseDoubleMatrix2D)Algebra.DEFAULT.inverse(covMtx);
    }

    @Override
    public String toString() {
        return "Center: " + center.toString()+"\nCovMtx\n"+
         covMtx.toString();
    }
    
    
    
    
    public MahalonobisDistance(sandbox.clustering.Cluster c) {
        //Compute the center of the cluster
        
            //logger.print("starting Mahalonobis Distance init");
            double[][] dataTable = new double[c.size()][];

            for (int i = 0; i < c.size(); i++) {
                dataTable[i] = c.getClusterMembers()[i].getDatapoint().getVector();
            }
            sandbox.clustering.ClusterMember[] cm = c.getClusterMembers();
            double[] vec = MatrixOp.copy(cm[0].getDatapoint().getVector());
            for (int i = 1; i < cm.length; i++) {
                double[] vec2 = cm[i].getDatapoint().getVector();
                for (int dim = 0; dim < vec2.length; dim++) {
                    vec[dim] += vec2[dim];
                }
            }

            for (int dim = 0; dim < vec.length; dim++) {
                vec[dim] /= c.size();
            }
            center = new DenseDoubleMatrix1D(vec);
            covMtx = new DenseDoubleMatrix2D(CovarianceMatrix.covarianceMatrix(dataTable));
            if (!Covariance) {
                //logger.print("no covariance, var = 0.25");
                for (int i = 0; i < center.size(); i++) {
                    for (int j = 0; j < center.size(); j++) {
                        if (i != j) {
                            covMtx.setQuick(i, j, 0);
                        }else{
                            covMtx.setQuick(i, j, Math.max(0.19,covMtx.getQuick(i, j)));
                        }
                    }
                }
            }
            invCovMtx = (DenseDoubleMatrix2D) Algebra.DEFAULT.transpose(covMtx);



        //Compute covariance matrix of the cluster

        //Take the inverse cov matrix
        invCovMtx = (DenseDoubleMatrix2D) Algebra.DEFAULT.inverse(covMtx);
    }

    public MahalonobisDistance(double[][] dataTable) {
       // logger.print("init MD");
        //Compute the center of the cluster

        double[] vec = MatrixOp.copy(dataTable[0]);
        for (int i = 1; i < dataTable.length; i++) {
            double[] vec2 = dataTable[i];
            for (int dim = 0; dim < vec2.length; dim++) {
                vec[dim] += vec2[dim];
            }
        }

        for (int dim = 0; dim < vec.length; dim++) {
            vec[dim] /= (double) dataTable.length;
        }
        center = new DenseDoubleMatrix1D(vec);

        covMtx = new DenseDoubleMatrix2D(CovarianceMatrix.covarianceMatrix(dataTable));
        
        if ( !Covariance) {
                logger.print("no covariance, var = 0.19");
                for (int i = 0; i < center.size(); i++) {
                    for (int j = 0; j < center.size(); j++) {
                        if (i != j) {
                            covMtx.setQuick(i, j, 0);
                        }else{
                            covMtx.setQuick(i, j, Math.max(0.19,covMtx.getQuick(i, j)));
                        }
                    }
                }
            }
        
        invCovMtx = (DenseDoubleMatrix2D) Algebra.DEFAULT.transpose(covMtx);

        

        //Compute covariance matrix of the cluster

        //Take the inverse cov matrix
        invCovMtx = (DenseDoubleMatrix2D) Algebra.DEFAULT.inverse(covMtx);
    }

    public double distTo(double[] x) {
        DenseDoubleMatrix1D diff = new DenseDoubleMatrix1D(x.length);
        for (int i = 0; i < x.length; i++) {
            diff.setQuick(i, x[i] - center.getQuick(i));
        }
        double dist = Algebra.DEFAULT.mult(diff, Algebra.DEFAULT.mult(invCovMtx, diff));
        return Math.sqrt(dist);
    }
}
