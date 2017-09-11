package vortex.clustergraph;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.tree.TreeNode;
import clustering.Cluster;
import clustering.ClusterMember;
import clustering.Datapoint;
import util.MatrixOp;
import util.logger;
import vortex.clustering.HierarchicalCentroid;

    /**
     *
     * @author Nikolay
     */
    public class ClusterPhylogeny_old {

        public ClusterTreeNode getDivisiveMarkerTree(Cluster[] clus) throws SQLException {
            ArrayList<ClusterDatapoint> filteredClusters = new ArrayList<>();
            for (Cluster cl : clus) {
                // if (true|| cl.getCaption().startsWith("Hu")) {
                filteredClusters.add(new ClusterDatapoint(cl));
                //}
            }

            ClusterDatapoint[] clDP = filteredClusters.toArray(new ClusterDatapoint[filteredClusters.size()]);
            double[] avgVector = getAvgVec(clDP);
            double[] sdVec = new double[avgVector.length];
            int cnt = 0;

            for (ClusterDatapoint c : clDP) {
                double[] cVec = c.getVector();
                for (ClusterMember cm : c.getCluster().getClusterMembers()) {
                    cnt++;
                    double[] vec = cm.getDatapoint().getVector();
                    for (int i = 0; i < sdVec.length; i++) {
                        double diff = vec[i] - cVec[i];
                        sdVec[i] += diff * diff;
                    }
                }
            }

            MatrixOp.mult(sdVec, 1.0 / cnt);

            for (int i = 0; i < sdVec.length; i++) {
                sdVec[i] = Math.max(0.25, Math.sqrt(sdVec[i]));
            }

            ClusterTreeNode root = new ClusterTreeNode(clDP, "Root", clDP.length);

            String[] colNames = clDP[0].c.getClusterSet().getDataset().getFeatureNames();

            ArrayList<ClusterTreeNode> prevLevel = new ArrayList<>();

            prevLevel.add(root);

            do {
                ArrayList<ClusterTreeNode> nextLevel = new ArrayList<>();
                for (ClusterTreeNode node : prevLevel) {
                    ClusterTreeNode[] children = split(node, colNames, sdVec);
                    for (ClusterTreeNode ch : children) {
                        node.add(ch);
                        if (ch.getUserObject().length > 1) {
                            nextLevel.add(ch);
                        }
                    }
                }
                prevLevel.clear();
                prevLevel.addAll(nextLevel);
            } while (!prevLevel.isEmpty());

            ArrayList<ClusterTreeNode> nodes = new ArrayList<>();
            Enumeration<HierarchicalCentroid> en = root.breadthFirstEnumeration();
            while (en.hasMoreElements()) {
                ClusterTreeNode n = (ClusterTreeNode) en.nextElement();
                if (n.isLeaf()) {
                    nodes.add(n);
                }
            }
        /*
         for (ClusterDatapoint cl : oldClusters) {
         if (!cl.getCaption().startsWith("Hu")) {
         ClusterTreeNode n = new ClusterTreeNode(new ClusterDatapoint[]{cl}, cl.getCaption().substring(0, 1));
         ClusterTreeNode bestMatch = null;
         double maxSim = 0;
         for (ClusterTreeNode node : nodes) {
         double sim = MatrixOp.getEuclideanCosine(cl.getVector(), getAvgVec(node.getUserObject()));
         if(sim > maxSim){
         maxSim = sim;
         bestMatch = node;
         }
         }

         double[] avg1 = getAvgVec(bestMatch.getUserObject());
         double[] avg2 = getAvgVec(n.getUserObject());

         int maxDiffIdx = 0;
         double maxDiff = 0;
         for (int i = 0; i < avg2.length; i++) {
         double diff = Math.abs(avg2[i] - avg1[i]);
         if (diff > maxDiff) {
         maxDiff = diff;
         maxDiffIdx = i;
         }
         }
         if (avg1[maxDiffIdx] > avg2[maxDiffIdx]) {
         n.label = cl.getCaption().substring(0,1)+"\n"+varNames[maxDiffIdx] + "lo";
         } else {
         n.label = cl.getCaption().substring(0,1)+"\n"+varNames[maxDiffIdx] + "hi";
         }
         bestMatch.add(n);
         }
         }        */

            return root;
        }

        ClusterTreeNode[] split(ClusterTreeNode node, String[] colNames, double[] sdVec) {
            ClusterDatapoint[] c = node.getUserObject();
            double dist = ((ClusterTreeNode) node.getRoot()).getUserObject().length - node.getLevel();

            if (c.length == 2) {
                double[] diff = MatrixOp.diff(c[0].getVector(), c[1].getVector());
                int maxCol = 0;
                double maxDiff = 0;
                String breakpoint = "";
                for (int i = 0; i < diff.length; i++) {
                    diff[i] = Math.abs(diff[i]);
                    diff[i] /= sdVec[i];
                    if (diff[i] > maxDiff) {
                        maxDiff = diff[i];
                        maxCol = i;
                        breakpoint = String.format("%3.2f", (c[0].getVector()[i] + c[1].getVector()[i]) / 2);
                    }
                }
                logger.print("splitting " + String.valueOf(node.getID()) + " " + node.getLabel() + "(" + node.getUserObject().length + ") on " + maxCol + "(1,1)");
                String lbl1 = colNames[maxCol].trim() + (c[0].getVector()[maxCol] > c[1].getVector()[maxCol] ? ">" + breakpoint : "<" + breakpoint);
                String lbl2 = colNames[maxCol].trim() + (c[0].getVector()[maxCol] > c[1].getVector()[maxCol] ? "<" + breakpoint : ">" + breakpoint);
                return new ClusterTreeNode[]{new ClusterTreeNode(new ClusterDatapoint[]{c[0]}, true, lbl1, dist), new ClusterTreeNode(new ClusterDatapoint[]{c[1]}, true, lbl2, dist)};
            }
            int bestCol = -1;
            double minSumSq = Double.MAX_VALUE;
            double bestDivVal = -1;
            double bestSepRange = 0;

            for (int i = 0; i < colNames.length; i++) {
                for (int j = 0; j < c.length; j++) {
                    double divVal = c[j].getVector()[i];

                    ClusterDatapoint[][] div = divideClusters(c, i, divVal);

                    if (div != null) {
                        double sumSq = getSumAngDist(div[0], sdVec) + getSumAngDist(div[1], sdVec);
                        //(getSumSq(div[0], sdVec, i) + getSumSq(div[1], sdVec, i))/getSumSq(c,sdVec,i);
                        if (sumSq < minSumSq) {
                            bestDivVal = divVal;
                            bestCol = i;
                            minSumSq = sumSq;

                            double minSepRange = Double.MAX_VALUE;
                            for (int k = 0; k < div[0].length; k++) {
                                for (int l = 0; l < div[1].length; l++) {
                                    double sepR =  Math.abs(div[0][k].getVector()[i] - div[1][l].getVector()[i]) / sdVec[i];
                                    if(sepR < minSepRange){
                                        minSepRange = sepR;
                                    }
                                }
                            }
                            bestSepRange = minSepRange;
                        } else if (sumSq / minSumSq < 1.0001) {
                            double minSepRange = Double.MAX_VALUE;
                            for (int k = 0; k < div[0].length; k++) {
                                for (int l = 0; l < div[1].length; l++) {
                                    double sepR =  Math.abs(div[0][k].getVector()[i] - div[1][l].getVector()[i]) / sdVec[i];
                                    if(sepR < minSepRange){
                                        minSepRange = sepR;
                                    }
                                }
                            }
                            if (minSepRange > bestSepRange) {
                                bestDivVal = divVal;
                                bestCol = i;
                                minSumSq = sumSq;
                                bestSepRange = minSepRange;
                            }
                        }
                    }
                }
            }

            logger.print("bestCol " + bestCol);

            //if(bestCol==-1)
            ClusterDatapoint[][] sep = divideClusters(c, bestCol, bestDivVal);

            double avg1 = getAvgVec(sep[0])[bestCol];
            double avg2 = getAvgVec(sep[1])[bestCol];

            double [] val1 = new double[sep[0].length];
            double [] val2 = new double[sep[1].length];
            for (int i = 0; i < val1.length; i++) {
                val1[i]= sep[0][i].c.getMode().getVector()[bestCol];
            }

            for (int i = 0; i < val2.length; i++) {
                val2[i]= sep[1][i].c.getMode().getVector()[bestCol];
            }

            Arrays.sort(val1);
            Arrays.sort(val2);

            String breakpoint = String.format("%3.2f", avg1 > avg2 ?  ((val1[val1.length-1]+val2[0])/2.0): ((val2[val2.length-1]+val1[0])/2.0));

            String lbl1 = colNames[bestCol].trim() + (avg1 > avg2 ? ">" + breakpoint : "<" + breakpoint);
            String lbl2 = colNames[bestCol].trim() + (avg1 > avg2 ? "<" + breakpoint : ">" + breakpoint);
            logger.print("splitting " + String.valueOf(node.getID()) + " " + node.getLabel() + "(" + node.getUserObject().length + ") on " + bestCol + "(" + sep[0].length + "," + sep[1].length + ")");
            return new ClusterTreeNode[]{new ClusterTreeNode(sep[0], true, lbl1, dist), new ClusterTreeNode(sep[1], true, lbl2, dist)};
        }

        ClusterDatapoint[][] divideClusters(ClusterDatapoint[] c, int col, double divVal) {
            ArrayList<ClusterDatapoint> g1 = new ArrayList<>();
            ArrayList<ClusterDatapoint> g2 = new ArrayList<>();

            for (ClusterDatapoint cl : c) {
                if (cl.getVector()[col] < divVal) {
                    g1.add(cl);
                } else {
                    g2.add(cl);
                }
            }
            if (g1.isEmpty() || g2.isEmpty()) {
                return null;
            }
            return new ClusterDatapoint[][]{g1.toArray(new ClusterDatapoint[g1.size()]), g2.toArray(new ClusterDatapoint[g2.size()])};
        }

        double[] getAvgVec(ClusterDatapoint[] cl) {
            if (cl.length == 0) {
                return null;
            }
            double[] avgVector = new double[cl[0].getVector().length];

            for (ClusterDatapoint c : cl) {
                avgVector = MatrixOp.sum(avgVector, c.getVector());
            }
            MatrixOp.mult(avgVector, 1.0 / cl.length);
            return avgVector;
        }

        double getSumSq(ClusterDatapoint[] cl, double[] sdVec, int leaveOutIdx) {
            double[] avgVector = getAvgVec(cl);
            double sumSq = 0;
            for (ClusterDatapoint c : cl) {
                double[] diff = MatrixOp.diff(c.getVector(), avgVector);
                for (int i = 0; i < diff.length; i++) {
                    if (i != leaveOutIdx) {
                        diff[i] = 0;
                        avgVector[i] = 0;
                    }
                }
                for (int i = 0; i < diff.length; i++) {
                    diff[i] /= sdVec[i];
                }
                sumSq += Math.pow(MatrixOp.lenght(diff), 2);
            }
            return sumSq;
        }

        double getSumAngDist(ClusterDatapoint[] cl, double[] sdVec) {
            double[] avgVector = getAvgVec(cl);
            for (int i = 0; i < avgVector.length; i++) {
                avgVector[i] /= sdVec[i];
            }
            double sumDist = 0;
            for (ClusterDatapoint c : cl) {
                double[] vec = MatrixOp.copy(c.getVector());
                for (int i = 0; i < vec.length; i++) {
                    vec[i] /= sdVec[i];
                }
                sumDist += Math.acos(MatrixOp.getEuclideanCosine(avgVector, vec));
            }
            return sumDist;
        }

        double getVolume(ClusterDatapoint[] cl, double[] sdVec) {
            if (cl.length < 2) {
                return 0;
            }
            double[] minVector = getAvgVec(cl);
            double[] maxVector = getAvgVec(cl);
            for (int i = 0; i < minVector.length; i++) {
                minVector[i] /= sdVec[i];
                maxVector[i] /= sdVec[i];
            }

            for (ClusterDatapoint c : cl) {
                double[] vec = MatrixOp.copy(c.getVector());
                for (int i = 0; i < vec.length; i++) {
                    vec[i] /= sdVec[i];
                    maxVector[i] = Math.max(vec[i], maxVector[i]);
                    minVector[i] = Math.min(vec[i], minVector[i]);
                }
            }
            double vol = 1;
            for (int i = 0; i < minVector.length; i++) {
                vol *= Math.abs(maxVector[i] - minVector[i]);
            }
            return vol;
        }

        private int[] findClosestPair(List<ClusterTreeNode> nodes) throws SQLException {

            double maxAvgSim = -Double.MAX_VALUE;
            int[] closestPairIdx = new int[2];

            for (int i = 0; i < nodes.size(); i++) {
                for (int j = i + 1; j < nodes.size(); j++) {
                    double sim = MatrixOp.getEuclideanCosine(getAvgVec(nodes.get(i).getUserObject()), getAvgVec(nodes.get(j).getUserObject()));
                    if (sim > maxAvgSim) {
                        maxAvgSim = sim;
                        closestPairIdx[0] = i;
                        closestPairIdx[1] = j;
                    }
                }
            }
            return closestPairIdx;
        }

        private static class NodeTuple {

            private final ClusterTreeNode n1;
            private final ClusterTreeNode n2;

            public NodeTuple(ClusterTreeNode n1, ClusterTreeNode n2) {
                this.n1 = n1;
                this.n2 = n2;
            }

            public ClusterTreeNode getN1() {
                return n1;
            }

            public ClusterTreeNode getN2() {
                return n2;
            }

            @Override
            public int hashCode() {
                return (n1.getLabel() + n2.getLabel()).hashCode();
            }

            @Override
            public String toString() {
                return "Node tuple: [" + n1.getLabel() + ", " + n2.getLabel() + "]";
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof NodeTuple)) {
                    return false;
                }
                return obj.hashCode() == this.hashCode();
            }

        }

        private HashMap<NodeTuple, Double> hmSimilarity;

        private double getSimilarity(ClusterTreeNode node1, ClusterTreeNode node2) throws SQLException {
            NodeTuple tuple = new NodeTuple(node1, node2);
            if (hmSimilarity == null) {
                hmSimilarity = new HashMap<>();
            }
            if (hmSimilarity.get(tuple) != null) {
                //  logger.print("cache hit");
                return hmSimilarity.get(tuple);
            }
            logger.print();
            //logger.print("cache miss");

            //logger.print(tuple + " hash: "+ tuple.hashCode());
            final int NUM_SAMPLES = 10000;
            ClusterMember[] vec1 = node1.getClusterMembers();
            ClusterMember[] vec2 = node2.getClusterMembers();

            double[] avg1 = new double[vec1[0].getDatapoint().getVector().length];
            double[] avg2 = new double[vec2[0].getDatapoint().getVector().length];
            double avgSim = 0;
            for (int i = 0; i < NUM_SAMPLES; i++) {
                int idx1 = (int) (Math.random() * vec1.length);
                int idx2 = (int) (Math.random() * vec2.length);
                avg1 = MatrixOp.sum(avg1, vec1[idx1].getDatapoint().getVector());
                avg2 = MatrixOp.sum(avg2, vec2[idx2].getDatapoint().getVector());
                avgSim += MatrixOp.getEuclideanCosine(vec1[idx1].getDatapoint().getVector(), vec2[idx2].getDatapoint().getVector());
            }

            MatrixOp.mult(avg1, 1.0 / NUM_SAMPLES);
            MatrixOp.mult(avg2, 1.0 / NUM_SAMPLES);

            if (true) {
                return avgSim / NUM_SAMPLES;// MatrixOp.getEuclideanCosine(avg1, avg2);
            }

            double[] stdev1 = new double[vec1[0].getDatapoint().getVector().length];
            double[] stdev2 = new double[vec2[0].getDatapoint().getVector().length];

            for (int i = 0; i < NUM_SAMPLES; i++) {
                int idx1 = (int) (Math.random() * vec1.length);
                int idx2 = (int) (Math.random() * vec2.length);
                for (int j = 0; j < stdev1.length; j++) {
                    stdev1[j] += Math.pow(vec1[idx1].getDatapoint().getVector()[j] - avg1[j], 2);
                    stdev2[j] += Math.pow(vec2[idx2].getDatapoint().getVector()[j] - avg2[j], 2);
                }
            }
            MatrixOp.mult(stdev1, 1.0 / NUM_SAMPLES);
            MatrixOp.mult(stdev2, 1.0 / NUM_SAMPLES);

            for (int j = 0; j < stdev1.length; j++) {
                stdev1[j] = Math.sqrt(stdev1[j]);
                stdev2[j] = Math.sqrt(stdev2[j]);
            }

            double numSimilarFeatures = 0;

            for (int i = 0; i < stdev2.length; i++) {
                double avgStdDev = (stdev1[i] + stdev2[i]) / 2.0;
                if (Math.abs(avg1[i] - avg2[i]) / avgStdDev < 1.96) {
                    numSimilarFeatures++;
                }
            }

            hmSimilarity.put(tuple, numSimilarFeatures);
            //logger.print("getting val: " + hmSimilarity.get(tuple));
            return numSimilarFeatures;
        }

        private static HashMap<ClusterTreeNode, double[][]> hmVectorList;

        private static ClusterMember[] getClusterMemberList(ClusterDatapoint[] c) throws SQLException {

            int size = 0;

            for (ClusterDatapoint cluster : c) {
                size += cluster.getCluster().size();
            }

            ClusterMember[] out = new ClusterMember[size];

            int i = 0;
            for (ClusterDatapoint cd : c) {
                for (ClusterMember cm : cd.getCluster().getClusterMembers()) {
                    out[i++] = cm;
                }
            }
            return out;
        }

        public static class ClusterDatapoint extends Datapoint {

            final Cluster c;
            private final static AtomicInteger idTracker = new AtomicInteger(-1);

            public ClusterDatapoint(Cluster c) {
                super(c.toString(), c.getMode().getVector(), c.getMode().getSideVector(), idTracker.addAndGet(1));
                this.c = c;
            }

            public Cluster getCluster() {
                return c;
            }
        }

        public static class ClusterTreeNode extends HierarchicalCentroid {

            private ClusterMember[] cm;
            private final static AtomicInteger idTracker = new AtomicInteger(0);

            public void setCmNull() {
                this.cm = null;
            }

            @Override
            public ClusterTreeNode getChildAfter(TreeNode aChild) {
                return (ClusterTreeNode) super.getChildAfter(aChild); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public ClusterTreeNode getChildAt(int index) {
                return (ClusterTreeNode) super.getChildAt(index); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public ClusterTreeNode getChildBefore(TreeNode aChild) {
                return (ClusterTreeNode) super.getChildBefore(aChild); //To change body of generated methods, choose Tools | Templates.
            }

            public ClusterMember[] getClusterMembers() throws SQLException {
                if (cm == null) {
                    cm = getClusterMemberList((ClusterDatapoint[]) getUserObject());
                }
                return cm;
            }

            @Override
            public ClusterDatapoint[] getUserObject() {
                return (ClusterDatapoint[]) super.getUserObject(); //To change body of generated methods, choose Tools | Templates.
            }

            public ClusterTreeNode(ClusterDatapoint[] clusters, String label, double distBtwChildren) {
                super(clusters, idTracker.addAndGet(1), distBtwChildren);
                this.label = label;
            }

            public ClusterTreeNode(ClusterDatapoint[] clusters, boolean allowsChildren, String label, double distBtwChildren) {
                this(clusters, label, distBtwChildren);
                this.allowsChildren = allowsChildren;
            }

            @Override
            public int hashCode() {
                return centID;
            }

            @Override
            public boolean equals(Object obj) {
                return obj.hashCode() == this.hashCode() && (obj instanceof ClusterTreeNode);
            }

        }

        private static ClusterDatapoint[] concatenate(ClusterDatapoint[] arr1, ClusterDatapoint[] arr2) {
            ClusterDatapoint[] arr = Arrays.copyOf(arr1, arr1.length + arr2.length);
            System.arraycopy(arr2, 0, arr, arr1.length, arr2.length);
            return arr;
        }

    }

