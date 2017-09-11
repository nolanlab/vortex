/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustering;

import clustering.DistanceMeasure;
import clustering.Dataset;
import vortex.clustering.HierarchicalParamPanel.LinkageType;

/**
 *
 * @author Nikolay
 */
public class HierarchicalClusterTree {

    private final HierarchicalCentroid root;
    private final DistanceMeasure distanceMeasure;
    private final Dataset ds;
    private final String clusteringMethod;
    private final HierarchicalParamPanel.LinkageType linkageType;

    public Dataset getDataset() {
        return ds;
    }

    public String getClusteringMethod() {
        return clusteringMethod;
    }

    public LinkageType getLinkageType() {
        return linkageType;
    }

    public HierarchicalClusterTree(HierarchicalCentroid root, Dataset ds, DistanceMeasure distanceMeasure, String clusteringMethod, HierarchicalParamPanel.LinkageType linkageType) {
        this.root = root;
        this.ds = ds;
        this.distanceMeasure = distanceMeasure;
        this.clusteringMethod = clusteringMethod;
        this.linkageType = linkageType;
    }

    public HierarchicalCentroid getRoot() {
        return root;
    }

    public DistanceMeasure getDistanceMeasure() {
        return distanceMeasure;
    }
}
