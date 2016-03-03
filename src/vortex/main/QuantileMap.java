/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.main;

import java.util.Arrays;
import main.Dataset;
import umontreal.iro.lecuyer.probdist.NormalDistQuick;
import umontreal.iro.lecuyer.randvar.NormalACRGen;
import umontreal.iro.lecuyer.randvar.NormalGen;
import umontreal.iro.lecuyer.rng.GenF2w32;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class QuantileMap {

    private double[][] sourceDataset;
    private double[][] targetDataset;

    public double getSourceDatasetQuantile(int dim, double quantile){
        int x = (int)Math.max(0,Math.min(sourceDataset[0].length*quantile, sourceDataset[0].length-1));
        return sourceDataset[dim][x];
    }
    
    public double getQuantileForValue(int dim, double value){
        int idxInSrc = Arrays.binarySearch(sourceDataset[dim], value);
        if (idxInSrc < 0) {
            idxInSrc = -(idxInSrc + 1);
        }
        return idxInSrc/(double)sourceDataset[dim].length;
    }
    
    public double[][] getTargetDataset() {
        return targetDataset;
    }

    public double[][] getSourceDataset() {
        return sourceDataset;
    }
    private double[][] srcBelowZero;
    private double[][] srcAboveZero;
    private double[][] trgBelowZero;
    private double[][] trgAboveZero;
    private int dim = 0;

    public double getDimensionality() {
        return dim;
    }
    
     public static QuantileMap getQuantileMap(Dataset ds) {
            NormalGen ng = new NormalACRGen(new MRG32k3a(), new NormalDistQuick());
            String[] params = ds.getFeatureNames();
            double[][] source = new double[params.length][];
            for (int i = 0; i < params.length; i++) {
                double paramVal[] = new double[ds.getDatapoints().length];
                for (int j = 0; j < paramVal.length; j++) {
                    paramVal[j] = ds.getDatapoints()[j].getVector()[i];
                }
                source[i] = paramVal;
            }
            double[][] target = new double[params.length][ds.getDatapoints().length];
            for (int i = 0; i < params.length; i++) {
                double paramVal[] = new double[ds.getDatapoints().length];
                double max = 0;
                for (int j = 0; j < paramVal.length; j++) {
                    paramVal[j] = ng.nextDouble();
                    max = Math.max(paramVal[j], max);
                }
                for (int j = 0; j < paramVal.length; j++) {
                    paramVal[j] /= max;
                }
                target[i] = paramVal;
            }
            return new QuantileMap(source, target);
    }

    public static QuantileMap getQuantileMapForSideParam(Dataset ds) {
            NormalGen ng = new NormalACRGen(new MRG32k3a(), new NormalDistQuick());
            String[] params = ds.getSideVarNames();
            if (params==null) return null;
            double[][] source = new double[params.length][];
            for (int i = 0; i < params.length; i++) {
                double paramVal[] = new double[ds.getDatapoints().length];
                for (int j = 0; j < paramVal.length; j++) {
                    if (ds.getDatapoints()[j].getSideVector() == null) {
                        return null;
                    }
                    paramVal[j] = ds.getDatapoints()[j].getSideVector()[i];
                }
                source[i] = paramVal;
            }
            double[][] target = new double[params.length][ds.getDatapoints().length];
            for (int i = 0; i < params.length; i++) {
                double paramVal[] = new double[ds.getDatapoints().length];
                double max = 0;
                for (int j = 0; j < paramVal.length; j++) {
                    paramVal[j] = ng.nextDouble();
                    max = Math.max(paramVal[j], max);
                }
                for (int j = 0; j < paramVal.length; j++) {
                    paramVal[j] /= max;
                }
                target[i] = paramVal;
            }
            return new QuantileMap(source, target);
    }

    public static QuantileMap getQuantileMapUnityLen(Dataset ds) {
        NormalGen ng = new NormalACRGen(new GenF2w32(), new NormalDistQuick());
            String[] params = ds.getFeatureNames();
            double[][] source = new double[params.length][];
            for (int i = 0; i < params.length; i++) {
                double paramVal[] = new double[ds.getDatapoints().length];
                for (int j = 0; j < paramVal.length; j++) {
                    paramVal[j] = ds.getDatapoints()[j].getUnityLengthVector()[i];
                }
                source[i] = paramVal;
            }
            double[][] target = new double[params.length][ds.getDatapoints().length];
            for (int i = 0; i < params.length; i++) {
                double paramVal[] = new double[ds.getDatapoints().length];
                double max = 0;
                for (int j = 0; j < paramVal.length; j++) {
                    paramVal[j] = ng.nextDouble();
                    max = Math.max(paramVal[j], max);
                }
                for (int j = 0; j < paramVal.length; j++) {
                    paramVal[j] /= max;
                }
                target[i] = paramVal;
            }
            return new QuantileMap(source, target);
        
    }
    
    public QuantileMap(double[][] sourceDataset, double[][] targetDataset) {
        this.sourceDataset = sourceDataset;
        this.targetDataset = targetDataset;
        if (sourceDataset.length != targetDataset.length) {
            throw new IllegalArgumentException("Provided datasets differ in dimensionality");
        }
        dim = sourceDataset.length;

        srcBelowZero = new double[dim][];
        srcAboveZero = new double[dim][];
        trgBelowZero = new double[dim][];
        trgAboveZero = new double[dim][];

        for (int i = 0; i < sourceDataset.length; i++) {
            Arrays.sort(sourceDataset[i]);

            int idxZero = Arrays.binarySearch(sourceDataset[i], 0);

            if (idxZero < 0) {
                idxZero = Math.min(Math.max(0, -(idxZero + 1)), sourceDataset[i].length);
            }
            srcBelowZero[i] = Arrays.copyOfRange(sourceDataset[i], 0, idxZero);
            srcAboveZero[i] = Arrays.copyOfRange(sourceDataset[i], idxZero, sourceDataset[i].length);
        }
        for (int i = 0; i < targetDataset.length; i++) {
            Arrays.sort(targetDataset[i]);
            int idxZero = Arrays.binarySearch(targetDataset[i], 0);

            if (idxZero < 0) {
                idxZero = Math.min(Math.max(0, -(idxZero + 1)), targetDataset[i].length);
            }
            trgBelowZero[i] = Arrays.copyOfRange(targetDataset[i], 0, idxZero);
            trgAboveZero[i] = Arrays.copyOfRange(targetDataset[i], idxZero, targetDataset[i].length);
        }

        for (int i = 0; i < targetDataset.length; i++) {
        }

    }

    public double getValueMappedToTarget(int d, double sourceValue, boolean preserveZero) {

        double[] src = sourceDataset[d];
        double[] trg = targetDataset[d];

        if (preserveZero) {
            if (Math.min(srcAboveZero[d].length, srcBelowZero[d].length) > 0) {
                if (sourceValue > 0) {
                    src = srcAboveZero[d];
                } else {
                    src = srcBelowZero[d];
                }
            }
            if (Math.min(trgAboveZero[d].length, trgBelowZero[d].length) > 0) {
                if (sourceValue > 0) {
                    trg = trgAboveZero[d];
                } else {
                    trg = trgBelowZero[d];
                }
            }
        }


        int idxInSrc = Arrays.binarySearch(src, sourceValue);
        if (idxInSrc < 0) {
            idxInSrc = -(idxInSrc + 1);
        }

        if (idxInSrc == 0) {
            return trg[0];
        }

        if (idxInSrc >= src.length - 1) {
            return trg[trg.length - 1];
        }

        double Y = src[idxInSrc];
        double dY = src[idxInSrc + 1] - src[idxInSrc];

        double srcX = idxInSrc + ((dY == 0) ? 0 : ((sourceValue - Y) / dY));

        double tgX = srcX * ((double) trg.length / (double) src.length);
        if (tgX <= 0) {
            return trg[0];
        }
        if (tgX >= trg.length - 2) {
            return trg[trg.length - 1];
        }
        int tgIDX1 = (int) Math.floor(tgX);
        int tgIDX2 = (int) Math.ceil(tgX);

        double dX = tgX - (double) tgIDX1;

        double tgY = ((trg[tgIDX2] - trg[tgIDX1]) * dX) + trg[tgIDX1];
        if (Double.isNaN(tgY)) {
            logger.print("NaN at Quantile Mapping, source value: " + sourceValue);
        }
        return tgY;
    }
}