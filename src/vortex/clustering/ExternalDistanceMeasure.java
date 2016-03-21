/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustering;

import clustering.DistanceMeasure;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import javax.swing.JOptionPane;
import main.Dataset;
import vortex.gui.ExternalDistanceConfigPanel;

/**
 *
 * @author Nikolay
 */
public class ExternalDistanceMeasure extends DistanceMeasure {

    public static enum RuleType {

        N_MINUS, RECIPROCAL
    };
    private DistToSimTransform transform;

    public static class DistToSimTransform {

        private double N;
        private RuleType rule;

        public DistToSimTransform(double N, RuleType rule) {
            this.N = N;
            this.rule = rule;
        }

        public double convert(double dist) {
            switch (rule) {
                case N_MINUS:
                    return N - dist;
                case RECIPROCAL:
                    return N / dist;
                default:
                    return Double.NaN;

            }
        }
    }

    @Override
    public boolean supportsPrototyping() {
        return false;
    }

    @Override
    public boolean isPublic() {
        return true;
    }

    @Override
    public double getDistance(DenseDoubleMatrix1D vec1, DenseDoubleMatrix1D vec2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double distanceToSimilarity(double distance) {
        return transform.convert(distance);
    }

    @Override
    public double getDistance(double[] vec1, double[] vec2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getDistance(double[] vec1, double[] vec2, String name1, String name2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getSimilarity(DenseDoubleMatrix1D vec1, DenseDoubleMatrix1D vec2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ExternalDistanceMeasure() {
    }

    @Override
    public double[] getDistanceBounds() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getName() {
        return "External Distance";
    }

    @Override
    public double[] getPrototype(double[][] vectors) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double[] getPrototype(double[][] vectors, double[] weights) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getSimilarity(double[] vec1, double[] vec2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double[] getSimilarityBounds() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double similarityToDistance(double similarity) {
        return transform.convert(similarity);
    }

    @Override
    public String getDescription() {
        return "External Distance Measure - allows user to plug in an external distance matrix for clustering and statistics";
    }

    @Override
    public DistanceMeasure clone() {
        return new ExternalDistanceMeasure();
    }

    @Override
    public void init(Dataset ds) {
        ExternalDistanceConfigPanel pan = new ExternalDistanceConfigPanel();
        JOptionPane.showInputDialog(null, pan);

    }
}
