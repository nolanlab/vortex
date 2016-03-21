/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustering;

import clustering.ClusteringCore;
import clustering.DistanceMatrix;
import clustering.DistanceMeasure;
import clustering.ClusterSet;
import clustering.Cluster;
import main.Dataset;
import clustering.Datapoint;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import util.DefaultEntry;
import util.ProfileAverager;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class HierarchicalClusteringCore extends ClusteringCore {

    private DistanceMatrix distMtx;

    @Override
    public String getAlgorithmName() {
        return "Hierarchical Clustering";
    }

    @Override
    public String getAlgorithmDescription() {
        return "Standard hierarchical agglomerative clustering, works with every distance measure. Once tree is built, the tree dendrogram is displayed for tree cutting";
    }

    @Override
    public boolean isAlgorithmPublic() {
        return true;
    }
    private HierarchicalParamPanel panel;

    @Override
    public JPanel getParamControlPanel() {
        return panel;
    }

    public HierarchicalClusteringCore(DistanceMeasure dm) {
        super(dm);
        panel = new HierarchicalParamPanel();
    }

    public static Cluster[] cutTree(HierarchicalClusterTree dg, double distanceThreshold, DistanceMeasure dm) {
        @SuppressWarnings("unchecked")
        Enumeration<HierarchicalCentroid> enu = dg.getRoot().breadthFirstEnumeration();
        ArrayList<HierarchicalCentroid> roots = new ArrayList<>();
        while (enu.hasMoreElements()) {
            HierarchicalCentroid hc = enu.nextElement();
            if (hc.getParent() == null) {
                continue;
            }
            HierarchicalCentroid parent = (HierarchicalCentroid) hc.getParent();
            if ((hc.isLeaf() || hc.getDistBtwChildren() < distanceThreshold) && parent.getDistBtwChildren() >= distanceThreshold) {
                roots.add(hc);
            }
        }

        Cluster[] cl = new Cluster[roots.size()];
        for (int i = 0; i < roots.size(); i++) {
            HierarchicalCentroid hc = roots.get(i);
            ProfileAverager pa = new ProfileAverager();
            ProfileAverager pa2 = new ProfileAverager();

            for (Datapoint d : hc.getDatapoints()) {
                pa.addProfile(d.getVector());
                pa2.addProfile(d.getSideVector());
            }


            Cluster c = new Cluster(hc.getDatapoints(), pa.getAverageUnityLen(), pa2.getAverageUnityLen(), "", dm);
            cl[i] = c;
        }
        return cl;
    }

    @Override
    public ClusterSet[] doBatchClustering(Dataset ds, String comment) {
        //HierarchicalDendrogram dend = doClustering(ds, panel.getDistanceMeasure());
        HierarchicalClusterTree dend = doClustering(ds, panel.getLinkageType(), null);
        JDialog dlg = new JDialog();
        HierarchicalDendrogramPane pan = new HierarchicalDendrogramPane();
        pan.setDendrogram(dend);
        //pan.setRowHeight(2);

        //dlg.setModal(true);
        dlg.setAlwaysOnTop(true);
        dlg.setModal(true);
        dlg.getContentPane().add(new JScrollPane(pan));
        dlg.setBounds(100, 100, 500, 500);
        dlg.setVisible(true);

        //HierarchicalDendrogram dend = doClustering(ds, panel.getDistanceMeasure());

        return pan.getClusterSets();
    }

    public HierarchicalClusterTree doClustering(Dataset ds, HierarchicalParamPanel.LinkageType type, Datapoint[][] initClusters) {

        HierarchicalCentroid root = null;
        final Datapoint[] dp = ds.getDatapoints();

        distMtx = new DistanceMatrix(ds, dm);

        ArrayList<HierarchicalCentroid> centroids = new ArrayList<>();
        ArrayList<DefaultEntry<HierarchicalCentroid, Double>> nNeighbors = new ArrayList<>();

        if (initClusters == null) {
            for (int x = 0; x < dp.length; x++) {
                HierarchicalCentroid hc = new HierarchicalCentroid(Arrays.copyOfRange(dp, x, x + 1), x, dm.getDistanceBounds()[0]);
                hc.label = dp[x].getName();
                centroids.add(hc);
                //logger.print(hc);
                nNeighbors.add(new DefaultEntry<HierarchicalCentroid, Double>(null, null));
            }

        } else {
            int idx = 0;
            for (Datapoint[] d : initClusters) {
                ArrayList<Datapoint> cdp = new ArrayList<Datapoint>();
                cdp.addAll(Arrays.asList(d));
                HierarchicalCentroid hc = new HierarchicalCentroid(cdp.toArray(new Datapoint[cdp.size()]), idx++, dm.getDistanceBounds()[0]);
                centroids.add(hc);
                //logger.print(hc);
                nNeighbors.add(new DefaultEntry<HierarchicalCentroid, Double>(null, null));
            }

        }

        int nnIdx;

        ArrayList<Integer> alToRecompute = new ArrayList<Integer>(dp.length);

        for (int x = 0; x < centroids.size(); x++) {
            alToRecompute.add(x);
        }
        logger.print("Building the tree");
        int cnt = 0;
        do {
            //logger.print("ToRecompute", alToRecompute);
            for (Integer x : alToRecompute) {
                double currDist = 0;
                double minDist = Double.MAX_VALUE;
                HierarchicalCentroid nn = null;
                int size = centroids.size();

                for (int y = 0; y < size; y++) {
                    if (x == y) {
                        continue;
                    }
                    if (centroids.get(y).isRoot()) {
                        currDist = getDistance(centroids.get(x), centroids.get(y), type);
                        /*
                         if(centroids.get(x).centID==801){
                         //logger.print(nn, currDist, getDistance(centroids.get(y), centroids.get(x), type));
                         if (currDist < minDist || Arrays.asList(new Integer[]{154, 111, 87, 1320, 1109, 1309}).contains(centroids.get(y).centID)){
                         logger.print(centroids.get(y), currDist, getDistance(centroids.get(y), centroids.get(x), type));
                         }
                         }                       
                         */
                        if (currDist < minDist) {
                            nn = centroids.get(y);
                            minDist = currDist;
                        }
                    }
                }
                DefaultEntry<HierarchicalCentroid, Double> entry = nNeighbors.get(x);
                entry.setKey(nn);
                entry.setValue(minDist);
            }


            double minDist = Double.MAX_VALUE;
            nnIdx = -1;
            for (int i = 0; i < centroids.size(); i++) {
                if (centroids.get(i).isRoot()) {
                    DefaultEntry<HierarchicalCentroid, Double> entry = nNeighbors.get(i);
                    if (entry.getKey() != null) {
                        double currDist = entry.getValue();
                        if (currDist < minDist) {
                            nnIdx = i;
                            minDist = currDist;
                        }
                    } else {
                        root = centroids.get(i);
                    }
                }
            }

            if (nnIdx < 0) {
                break;
            }

            alToRecompute = new ArrayList<Integer>();
            HierarchicalCentroid nn1 = centroids.get(nnIdx);
            HierarchicalCentroid nn2 = nNeighbors.get(nnIdx).getKey();
            int newCentIDX = centroids.size();
            //logger.print("Linking ", nn1.getLeaves(), nn2.getLeaves(), minDist);
            HierarchicalCentroid newCent = new HierarchicalCentroid(newCentIDX, nn1, nn2, minDist);
            for (int i = 0; i < centroids.size(); i++) {
                HierarchicalCentroid cent = nNeighbors.get(i).getKey();
                //logger.print(cent);
                if (cent.equals(nn1) || cent.equals(nn2)) {
                    if (centroids.get(i).isRoot()) {
                        alToRecompute.add(i);
                    }
                }
            }

            alToRecompute.add(newCentIDX);
            centroids.add(newCent);
            nNeighbors.add(new DefaultEntry<HierarchicalCentroid, Double>(null, null));
            if (cnt % 10 == 0) {
                logger.print("Step " + cnt);
            }
            cnt++;

        } while (true);

        String linkageType = "";

        switch (type) {
            case AverageLinkage:
                linkageType = "Avg Link";
                break;
            case MinimalLinkage:
                linkageType = "Min Link";
                break;
            case CompleteLinkage:
                linkageType = "Comp Link";
                break;
        }

        return new HierarchicalClusterTree(root, ds, dm, this.getAlgorithmName(), type);
    }

    private double getDistance(HierarchicalCentroid c1, HierarchicalCentroid c2, HierarchicalParamPanel.LinkageType lt) {

        Datapoint[] l1 = c1.getDatapoints();
        Datapoint[] l2 = c2.getDatapoints();
        double dist = 0;
        //logger.print("Leaves1: " + l1.size() + ", " + l1);
        //logger.print("Leaves2: " + l2.size() + ", " + l2);
        switch (lt) {
            case AverageLinkage:
                for (Datapoint ll1 : l1) {
                    for (Datapoint ll2 : l2) {
                        dist += distMtx.getValue(ll1, ll2);
                    }
                }
                return dist / (l1.length * l2.length);
            case MinimalLinkage:
                dist = Double.MAX_VALUE;
                for (Datapoint ll1 : l1) {
                    for (Datapoint ll2 : l2) {
                        dist = Math.min(dist, distMtx.getValue(ll1, ll2));
                    }
                }
                return dist;
            case CompleteLinkage:
                dist = -Double.MAX_VALUE;
                for (Datapoint ll1 : l1) {
                    for (Datapoint ll2 : l2) {
                        dist = Math.max(dist, distMtx.getValue(ll1, ll2));
                    }
                }
                return dist;
            default:
                return Double.POSITIVE_INFINITY;
        }

    }
}
