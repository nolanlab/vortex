/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustering;

import clustering.Dataset;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import clustering.IsotropicDistanceMeasure;
import util.MatrixOp;
import util.ProfileAverager;


/**
 *
 * @author Nikolay
 */
public class AngularDistance extends IsotropicDistanceMeasure {

    private static final long serialVersionUID = -1375125722961479262L;

    @Override
    public AngularDistance clone() {
        return new AngularDistance();

    }

    @Override
    public boolean supportsPrototyping() {
        return true;
    }

    @Override
    public double[] getPrototype(double[][] vectors, double[] weights) {
        if (vectors.length == 0) {
            return null;
        }
        if (vectors.length != weights.length) {
            throw new IllegalArgumentException("The weights array and vectors array have different lengths");
        }
        double[] prototype = new double[vectors[0].length];

        for (int i = 0; i < weights.length; i++) {
            double[] vec = MatrixOp.toUnityLen(vectors[i]);
            for (int j = 0; j < prototype.length; j++) {
                prototype[j] += vec[j] * weights[i];
            }
        }
        return MatrixOp.toUnityLen(prototype);
    }

    @Override
    public double[] getPrototype(double[][] vectors) {
        if (vectors.length == 0) {
            return null;
        }
        ProfileAverager pa = new ProfileAverager();
        for (double[] vec : vectors) {
            if (vec != null) {
                pa.addProfile(MatrixOp.toUnityLen(vec));
            }
        }
        return pa.getAverageUnityLen();
    }

    //private EuclideanDistance ed = new EuclideanDistance();
    @Override
    public double similarityToDistance(double similarity) {
        return Math.acos(similarity) / Math.PI;
    }

    @Override
    public double distanceToSimilarity(double distance) {
        return Math.cos(distance * Math.PI);
    }

    @Override
    public double getDistance(double[] vec1, double[] vec2, String name1, String name2) {
        return getDistance(vec1, vec2);
    }

    @Override
    public void init(Dataset aov) {
        return;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public double getDistance(double[] vector1, double[] vector2) {

        return (Math.acos(getSimilarity(vector1, vector2))) / Math.PI;
    }

    @Override
    public double getDistance(DenseDoubleMatrix1D vector1, DenseDoubleMatrix1D vector2) {
        return (Math.acos(getSimilarity(vector1, vector2))) / Math.PI;
    }

    @Override
    public double[] getDistanceBounds() {
        return new double[]{MIN_DISTANCE, MAX_DISTANCE};
    }

    @Override
    public String getDescription() {
        return "Distance between two vectors is expressed as the angle between, and the similarity as a cosine of angle (uncentered Pearson correlation).";
    }

    @Override
    public String getName() {
        return "Angular Distance";
    }

    @Override
    public boolean isPublic() {
        return true;
    }

    @Override
    public double[] getSimilarityBounds() {
        return new double[]{MIN_SIMILARITY, MAX_SIMILARITY};
    }
    private static final double MIN_DISTANCE = 0;
    private static final double MAX_DISTANCE = 1.0; //2.0* Math.PI;
    private static final double MIN_SIMILARITY = -1;
    private static final double MAX_SIMILARITY = 1;

    @Override
    public double getSimilarity(DenseDoubleMatrix1D vector1, DenseDoubleMatrix1D vector2) {
        return Math.min(MAX_SIMILARITY, Math.max(MIN_SIMILARITY, MatrixOp.mult(vector1.toArray(), vector2.toArray()) / (MatrixOp.lenght(vector1.toArray()) * MatrixOp.lenght(vector2.toArray()))));
    }

    @Override
    public double getSimilarity(double[] vector1, double[] vector2) {
        return Math.min(MAX_SIMILARITY, Math.max(MIN_SIMILARITY, MatrixOp.mult(vector1, vector2) / (MatrixOp.lenght(vector1) * MatrixOp.lenght(vector2))));
    }
}
