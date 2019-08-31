/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustering;

import sandbox.clustering.ClusteringAlgorithm;
import sandbox.clustering.DistanceMeasure;
import sandbox.clustering.ClusterSet;
import sandbox.clustering.Cluster;
import java.sql.SQLException;
import sandbox.clustering.Dataset;
import javax.swing.JPanel;
import util.logger;
import vortex.util.ConnectionManager;

/**
 *
 * @author Nikolay
 */
public abstract class KernelClusteringCore extends ClusteringAlgorithm {

    Kernel kernel;
    KernelFactory kf;

    public KernelClusteringCore(DistanceMeasure dm) {
        super(dm);
    }

    @Override
    public JPanel getParamControlPanel() {
        if (kf == null) {
            kf = new KernelFactory(dm);
        }
        return kf;
    }

    @Override
    public ClusterSet[] doBatchClustering(Dataset ds, String comment) {
        Kernel[] kernels = kf.getKernels();
        ClusterSet[] css = new ClusterSet[kernels.length];

int batchID = 0;
        try{
            ConnectionManager.getStorageEngine().getNextClusterSetBatchID(ds.getID());
        }catch(SQLException e){
            logger.showException(e);
        }

        for (int i = 0; i < css.length; i++) {
            logger.print("Clustering with " + getAlgorithmName() + ", kernel " + kernels[i].toString());
            css[i] = new ClusterSet(batchID, ds, this.doClustering(ds, kernels[i], kf.getMergeThreshold()), dm, this.getAlgorithmName(), kernels[i].toString() + ", merge " + kf.getMergeThreshold(), kernels[i].getBandwidth(), comment);
        }
        return css;
    }

    public abstract Cluster[] doClustering(Dataset ds, Kernel kernel, double mergeThreshold);
}
