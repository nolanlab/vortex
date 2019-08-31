/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustering;

import sandbox.clustering.DistanceMeasure;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import sandbox.clustering.Dataset;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class NNNKernel extends Kernel {

    public NNNKernel(DistanceMeasure distMeasure) {
        super(distMeasure);
    }

    @Override
    public boolean isResolutionAscending() {
        return false;
    }

    @Override
    public String freeParameterName() {
        return "N";
    }

    @Override
    public boolean isPublic() {
        return true;
    }

    @Override
    public Kernel clone() {
        NNNKernel k = new NNNKernel(dm);
        k.init(null, bandwidth);
        return k;
    }

    @Override
    public void fitBandwidth(double[] center, double[][] ArrayLists) {
        setBandwidth(ArrayLists.length);
    }

    @Override
    public BigDecimal getDensity(final double[] center, double[][] ArrayLists) {

        int size = (int) bandwidth;

        LinkedList<Double> nnn = new LinkedList<>();

        for (int i = 0; i < size; i++) {
            nnn.add(dm.getSimilarity(center, ArrayLists[i]));
        }

        Collections.sort(nnn, new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return (int) Math.signum(o2 - o1);
            }
        });

        double minSim = nnn.get(size - 1);
        for (int i = size; i < ArrayLists.length; i++) {
            double d = dm.getSimilarity(center, ArrayLists[i]);
            if (d > minSim) {
                for (int j = 0; j < size; j++) {
                    if (nnn.get(j) < d) {
                        nnn.add(j, d);
                        nnn.removeLast();
                        minSim = nnn.getLast();
                        break;
                    }
                }
            }
        }
        double avgDist = 0;
        for (int i = 0; i < size; i++) {
            //assert(dm.similarityToDistance(nnn.get(i-1))< dm.similarityToDistance(nnn.get(i)));
            avgDist += dm.similarityToDistance(nnn.get(i));// (i/Math.pow(),center.length-1));
        }
        avgDist /= size;
        //logger.print(avgDist);
        return new BigDecimal(Math.exp(-avgDist*10));
    }

    @Override
    protected double getNonNormalizedValue(double[] center, double[] ArrayList) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected double getNormalizationConst(double[] center) {
        return 0;
    }

    @Override
    public double[] getWeightedMean(final double[] center, double[][] ArrayLists) {
        ArrayLists = Arrays.copyOf(ArrayLists, ArrayLists.length);

        Arrays.sort(ArrayLists, new Comparator<double[]>() {
            @Override
            public int compare(double[] o1, double[] o2) {
                double dist1 = dm.getDistance(center, o1);

                double dist2 = dm.getDistance(center, o2);

                return (int) Math.signum(dist1 - dist2);
            }
        });
        return dm.getPrototype(Arrays.copyOf(ArrayLists, (int) bandwidth));
    }

    @Override
    public DistanceMeasure getDistanceMeasure() {
        return super.getDistanceMeasure();
    }

    @Override
    public String getDescription() {
        return "This simple adaptive kernel estimates the density as the average similarity to the N nearest neighbors";
    }

    @Override
    public String getName() {
        return "NNN (N nearest neighbors) kernel";
    }

    @Override
    public void init(Dataset nd, double bandwidth) {
        this.bandwidth = (int) bandwidth;
    }

    @Override
    public String toString() {
        return "NNN Kernel, N = " + bandwidth;
    }
}
