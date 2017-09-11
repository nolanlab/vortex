/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.main;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

/**
 *
 * @author Nikolay
 */
public interface DatapointScorer {

    public void setNoiseCovarianceMatrix(DenseDoubleMatrix2D covMtx);
}
