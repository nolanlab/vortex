/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.util;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import umontreal.iro.lecuyer.util.DMatrix;
import vortex.mahalonobis.CovarianceMatrix;
import clustering.Datapoint;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class ProfilePCA {

    public static DenseDoubleMatrix1D[] getPrincipalComponents(Datapoint[] datapoints, boolean unityLen) {
        double[][] data = new double[datapoints.length][datapoints[0].getVector().length];
        /*try {
         MahalonobisSpace ms = new MahalonobisSpace(new NDataset("null", datapoints, new String[datapoints[0].getVector().length]));
         } catch (IllegalArgumentException e) {
         JOptionPane.showMessageDialog(null, "Covariance matrix of the dataset is singular and the PCA cannot be computed.\n"
         + "The reasons for this may be:\n"
         + "1) Duplicated columns (parameters) present in the dataset\n"
         + "2) Number of profiles(measurements) is less than the number of columns (parameters)");
         return null;
         }
         */
        int dim = datapoints[0].getVector().length;
        int len = datapoints.length;

        for (int i = 0; i < len; i++) {
            for (int j = 0; j < dim; j++) {
                data[i][j] = (unityLen) ? datapoints[i].getUnityLengthVector()[j] : datapoints[i].getVector()[j];
            }
        }
        
        
        

       
        double[][] components = null;
        try {
            components = DMatrix.PCADecompose(new DenseDoubleMatrix2D(CovarianceMatrix.covarianceMatrix(data)),new double[data.length]).toArray();
        } catch (Exception e) {
            logger.print("Caught Exception");
            logger.showException(e);
        }
        //components[0] = new double []{0,1};
        //components[1] = new double []{1,0};

        DenseDoubleMatrix1D[] out = new DenseDoubleMatrix1D[components.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = new DenseDoubleMatrix1D(components[i]);
        }
        return out;
    }
}
