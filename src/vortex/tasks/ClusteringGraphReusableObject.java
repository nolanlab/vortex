/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.tasks;

import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import executionslave.ReusableObject;
import java.util.ArrayList;
import clustering.Datapoint;
import main.Dataset;
import util.SparseDoubleMatrix;

/**
 *
 * @author Nikolay
 */
public class ClusteringGraphReusableObject implements ReusableObject {

    private static final long serialVersionUID = 1L;

    @Override
    public ReusableObject deepCopy() {
        @SuppressWarnings("unchecked")
        ArrayList<Datapoint> copyRoots = (ArrayList<Datapoint>) roots.clone();
        SparseDoubleMatrix copyDwg = dwg;
        return new ClusteringGraphReusableObject(copyRoots, copyDwg, ds);
    }

    public ClusteringGraphReusableObject(ArrayList<Datapoint> roots, SparseDoubleMatrix dwg, Dataset ds) {
        this.roots = roots;
        this.dwg = dwg;
        this.ds = ds;
    }
    private ArrayList<Datapoint> roots;
    private Dataset ds;
    private SparseDoubleMatrix dwg;

    public ArrayList<Datapoint> getRoots() {
        return roots;
    }

    public Dataset getDataset() {
        return ds;
    }

    public SparseDoubleMatrix getGraph() {
        return dwg;
    }
}
