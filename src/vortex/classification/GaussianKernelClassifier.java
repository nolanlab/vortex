/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.classification;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import umontreal.iro.lecuyer.probdistmulti.MultiNormalDist;
import clustering.MahalonobisSpace;
import clustering.Datapoint;
import util.ProfileAverager;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class GaussianKernelClassifier {

    Datapoint[] trainingSet;
    MultiNormalDist dist;

    public GaussianKernelClassifier(Datapoint[] trainingSet, DenseDoubleMatrix2D totalSetCovMtx) {
        this.trainingSet = trainingSet;
        DenseDoubleMatrix2D covMtx = MahalonobisSpace.computeCovarMtxForDP(trainingSet);
        MahalonobisSpace ms = new MahalonobisSpace(covMtx);
        double k = ms.getMahalonobisLength(trainingSet[0].getVector());
        double x = Algebra.DEFAULT.det(covMtx);
        logger.print(covMtx);
        logger.print(totalSetCovMtx);
        ProfileAverager pa = new ProfileAverager();
        for (Datapoint d : trainingSet) {
            pa.addProfile(d.getVector());
        }
        dist = new MultiNormalDist(pa.getAverage(), totalSetCovMtx.toArray());
    }

    public double getProbability(DenseDoubleMatrix1D point) {
        double prob = 1;

        for (Datapoint d : trainingSet) {
            dist.setParams(d.getVector(), dist.getSigma());
            prob *= dist.density(point.toArray());
        }
        return prob;
    }

    public double getDensity(DenseDoubleMatrix1D point) {
        double dens = 0;
        for (Datapoint d : trainingSet) {
            dist.setParams(d.getVector(), dist.getSigma());
            dens += dist.density(point.toArray());
        }
        return dens / (double) trainingSet.length;
    }
}
