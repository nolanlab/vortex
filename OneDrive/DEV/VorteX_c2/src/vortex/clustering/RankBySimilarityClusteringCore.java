/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustering;

import clustering.ClusteringCore;
import clustering.DistanceMeasure;
import java.util.ArrayList;
import java.util.Map.Entry;
import javax.swing.JPanel;
import clustering.Cluster;
import clustering.ClusterSet;
import clustering.Datapoint;
import java.sql.SQLException;
import clustering.Dataset;
import util.DefaultEntry;
import util.MatrixOp;
import util.ProfileAverager;
import util.logger;
import vortex.util.ConnectionManager;

/**
 *
 * @author Nikolay
 */
public class RankBySimilarityClusteringCore extends ClusteringCore {

    RankBySimilarityControlPanel pan = null;

    @Override
    public JPanel getParamControlPanel() {
        if (pan == null) {
            pan = new RankBySimilarityControlPanel();
        }
        return pan;
    }

    @Override
    public String getAlgorithmDescription() {
        return "This method creates a cluster of profiles that are selected based on a similarity to an arbitrary user-provided profile.\nThe dimensionality of the provided profile has to match the dimensionality of the dataset.";
    }

    @Override
    public String getAlgorithmName() {
        return "Find similar profiles";
    }

    public RankBySimilarityClusteringCore(DistanceMeasure dm) {
        super(dm);
    }

    @Override
    public ClusterSet[] doBatchClustering(Dataset ds, String comment) {
        ds.getDatapoints();
        double ths = pan.getSimilarityCutoff();
        Entry<String, double[]>[] modes = pan.getMode();
        Cluster[] cl = new Cluster[modes.length];
        int k = 0;
        for (Entry<String, double[]> mode : modes) {
            if (mode.getValue().length != ds.getDimension()) {
                throw new IllegalArgumentException("The dimensionality of the provided profile (" + mode.getValue().length + ") doesn't match the dimensiolality of the dataset (" + ds.getDimension() + ")");
            }
            ArrayList<Entry<Datapoint, Double>> al = new ArrayList<>();
            ProfileAverager pa = new ProfileAverager();
            for (Datapoint nd : ds.getDatapoints()) {
                if (dm.getSimilarity(nd.getVector(), mode.getValue()) > ths) {
                    al.add(new DefaultEntry<>(nd, 1.0));
                    pa.addProfile(nd.getSideVector());
                }
            }

            cl[k++] = new Cluster(al.toArray(new Entry[al.size()]), MatrixOp.toUnityLen(mode.getValue()), pa.getAverageUnityLen(), mode.getKey(), dm);
        }
        int batchID = 0;
        try{
            ConnectionManager.getStorageEngine().getNextClusterSetBatchID(ds.getID());
        }catch(SQLException e){
            logger.showException(e);
        }
        ClusterSet cs = new ClusterSet(batchID, ds, cl, dm, "Search for similar profiles", "Similarity thrs:" + ths, ths, comment);
        return new ClusterSet[]{cs};
    }

    @Override
    public boolean isAlgorithmPublic() {
        return true;
    }
}
