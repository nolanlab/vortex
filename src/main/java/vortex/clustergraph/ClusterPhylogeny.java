/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustergraph;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.tree.TreeNode;
import sandbox.clustering.Cluster;
import sandbox.clustering.ClusterMember;
import sandbox.clustering.Datapoint;
import util.MatrixOp;
import util.logger;
import vortex.clustering.HierarchicalCentroid;

/**
 *
 * @author Nikolay
 */
public class ClusterPhylogeny {

    public ClusterTreeNode getDivisiveMarkerTree(Cluster[] clus, boolean useSideVariables, boolean simpleMode) throws SQLException {
        ArrayList<ClusterDatapoint> filteredClusters = new ArrayList<>();
        for (Cluster cl : clus) {
            // if (true|| cl.getCaption().startsWith("Hu")) {
            filteredClusters.add(new ClusterDatapoint(cl));
            //}
        }

        ClusterDatapoint[] clDP = filteredClusters.toArray(new ClusterDatapoint[filteredClusters.size()]);
        double[] avgArrayList = getAvgVec(clDP, useSideVariables);
        double[] sdVec = new double[avgArrayList.length];
        int cnt = 0;

        for (ClusterDatapoint c : clDP) {
            double[] cVec = useSideVariables?c.getSideVector():c.getVector();
            for (ClusterMember cm : c.getCluster().getClusterMembers()) {
                cnt++;
                double[] vec = useSideVariables?cm.getDatapoint().getSideVector():cm.getDatapoint().getVector();
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

        String[] colNames = useSideVariables?clDP[0].c.getClusterSet().getDataset().getSideVarNames():clDP[0].c.getClusterSet().getDataset().getFeatureNames();
        for (int i = 0; i < colNames.length; i++) {
            colNames[i]=colNames[i].replaceAll("\\:.*", "");
        }

        ArrayList<ClusterTreeNode> prevLevel = new ArrayList<>();

        prevLevel.add(root);

        do {
            ArrayList<ClusterTreeNode> nextLevel = new ArrayList<>();
            for (ClusterTreeNode node : prevLevel) {
                ClusterTreeNode[] children = split(node, colNames, sdVec, useSideVariables, simpleMode);
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

    ClusterTreeNode[] split(ClusterTreeNode node, String[] colNames, double[] sdVec, boolean useSideVectors, boolean simpleMode) {
        ClusterDatapoint[] c = node.getUserObject();
        double dist = ((ClusterTreeNode) node.getRoot()).getUserObject().length - node.getLevel();
        
       

        if (c.length == 2) {
            double[] vec1 = useSideVectors?c[0].getSideVector():c[0].getVector();
            double[] vec2 = useSideVectors?c[1].getSideVector():c[1].getVector();
            
            double[] diff = MatrixOp.diff(vec1, vec2);
            int maxCol = 0;
            double maxDiff = 0;
            String breakpoint = "";
            for (int i = 0; i < diff.length; i++) {
                diff[i] = Math.abs(diff[i]);
                diff[i] /= sdVec[i];
                if (diff[i] > maxDiff) {
                    maxDiff = diff[i];
                    maxCol = i;
                    breakpoint = String.format("%3.2f", (vec1[i] + vec2[i]) / 2);
                }
            }
            logger.print("splitting " + String.valueOf(node.getID()) + " " + node.getLabel() + "(" + node.getUserObject().length + ") on " + maxCol + "(1,1)");
            String lbl1 = colNames[maxCol].trim() + (simpleMode? (vec1[maxCol] > vec2[maxCol] ? "⁺" : "⁻") : (vec1[maxCol] > vec2[maxCol] ? "\n>" + breakpoint : "\n<" + breakpoint)) + c[0].getCluster().getComment();
            String lbl2 = colNames[maxCol].trim() + (simpleMode? (vec1[maxCol] > vec2[maxCol] ? "⁻" : "⁺") : (vec1[maxCol] > vec2[maxCol] ? "\n<" + breakpoint : "\n>" + breakpoint)) + c[1].getCluster().getComment();
            return new ClusterTreeNode[]{new ClusterTreeNode(new ClusterDatapoint[]{c[0]}, true, lbl1, dist), new ClusterTreeNode(new ClusterDatapoint[]{c[1]}, true, lbl2, dist)};
        }
        int bestCol = -1;
        double minSumSq = Double.MAX_VALUE;
        double bestDivVal = -1;
        double bestSepRange = 0;
        
        for (int i = 0; i < colNames.length; i++) {
            for (int j = 0; j < c.length; j++) {
                double divVal = (useSideVectors?c[j].getSideVector():c[j].getVector())[i];

                ClusterDatapoint[][] div = divideClusters(c, i, divVal, useSideVectors);

                if (div != null) {
                    double sumSq = getSumAngDist(div[0], sdVec, useSideVectors) + getSumAngDist(div[1], sdVec, useSideVectors);
                    //(getSumSq(div[0], sdVec, i) + getSumSq(div[1], sdVec, i))/getSumSq(c,sdVec,i);
                    if (sumSq < minSumSq) {
                        bestDivVal = divVal;
                        bestCol = i;
                        minSumSq = sumSq;

                        double minSepRange = Double.MAX_VALUE;
                        for (int k = 0; k < div[0].length; k++) {
                            for (int l = 0; l < div[1].length; l++) {
                                double [] vec1 = useSideVectors?div[0][k].getSideVector():div[0][k].getVector();
                                double [] vec2 = useSideVectors?div[1][l].getSideVector():div[1][l].getVector();
                                
                                double sepR =  Math.abs(vec1[i] - vec2[i]) / sdVec[i];
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
                                double [] vec1 = useSideVectors?div[0][k].getSideVector():div[0][k].getVector();
                                double [] vec2 = useSideVectors?div[1][l].getSideVector():div[1][l].getVector();
                                
                                double sepR =  Math.abs(vec1[i] - vec2[i]) / sdVec[i];
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
        ClusterDatapoint[][] sep = divideClusters(c, bestCol, bestDivVal, useSideVectors);

        double avg1 = getAvgVec(sep[0], useSideVectors)[bestCol];
        double avg2 = getAvgVec(sep[1], useSideVectors)[bestCol];
        
        double [] val1 = new double[sep[0].length];
        double [] val2 = new double[sep[1].length];
        for (int i = 0; i < val1.length; i++) {
             val1[i]= useSideVectors?sep[0][i].c.getMode().getSideVector()[bestCol]:sep[0][i].c.getMode().getVector()[bestCol];
        }
        
        for (int i = 0; i < val2.length; i++) {
             val2[i]= useSideVectors?sep[1][i].c.getMode().getSideVector()[bestCol]:sep[1][i].c.getMode().getVector()[bestCol];
        }
        
        Arrays.sort(val1);
        Arrays.sort(val2);
        
        String breakpoint = String.format("%3.2f", avg1 > avg2 ?  ((val1[val1.length-1]+val2[0])/2.0): ((val2[val2.length-1]+val1[0])/2.0));
        
        String lbl1 = colNames[bestCol].trim() + (simpleMode? (avg1 > avg2 ? "⁺" : "⁻"): (avg1 > avg2 ? ">" + breakpoint : "<" + breakpoint));
        String lbl2 = colNames[bestCol].trim() + (simpleMode? (avg1 > avg2 ? "⁻":  "⁺") :(avg1 > avg2 ? "<" + breakpoint : ">" + breakpoint));
        logger.print("splitting " + String.valueOf(node.getID()) + " " + node.getLabel() + "(" + node.getUserObject().length + ") on " + bestCol + "(" + sep[0].length + "," + sep[1].length + ")");
        return new ClusterTreeNode[]{new ClusterTreeNode(sep[0], true, lbl1, dist), new ClusterTreeNode(sep[1], true, lbl2, dist)};
    }

    ClusterDatapoint[][] divideClusters(ClusterDatapoint[] c, int col, double divVal, boolean useSideVectors) {
        ArrayList<ClusterDatapoint> g1 = new ArrayList<>();
        ArrayList<ClusterDatapoint> g2 = new ArrayList<>();

        for (ClusterDatapoint cl : c) {
            if ((useSideVectors?cl.getSideVector():cl.getVector())[col] < divVal) {
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

    double[] getAvgVec(ClusterDatapoint[] cl, boolean useSideVectors) {
        if (cl.length == 0) {
            return null;
        }
        double[] avgArrayList = new double[(useSideVectors?cl[0].getSideVector():cl[0].getVector()).length];

        for (ClusterDatapoint c : cl) {
            avgArrayList = MatrixOp.sum(avgArrayList, (useSideVectors?c.getSideVector():c.getVector()));
        }
        MatrixOp.mult(avgArrayList, 1.0 / cl.length);
        return avgArrayList;
    }

    double getSumSq(ClusterDatapoint[] cl, double[] sdVec, int leaveOutIdx, boolean useSideVectors) {
        double[] avgArrayList = getAvgVec(cl, useSideVectors);
        double sumSq = 0;
        for (ClusterDatapoint c : cl) {
            double[] diff = MatrixOp.diff(useSideVectors?c.getSideVector():c.getVector(), avgArrayList);
            for (int i = 0; i < diff.length; i++) {
                if (i != leaveOutIdx) {
                    diff[i] = 0;
                    avgArrayList[i] = 0;
                }
            }
            for (int i = 0; i < diff.length; i++) {
                diff[i] /= sdVec[i];
            }
            sumSq += Math.pow(MatrixOp.lenght(diff), 2);
        }
        return sumSq;
    }

    double getSumAngDist(ClusterDatapoint[] cl, double[] sdVec , boolean useSideVectors) {
        double[] avgArrayList = getAvgVec(cl, useSideVectors);
        for (int i = 0; i < avgArrayList.length; i++) {
            avgArrayList[i] /= sdVec[i];
        }
        double sumDist = 0;
        for (ClusterDatapoint c : cl) {
            double[] vec = MatrixOp.copy(useSideVectors?c.getSideVector():c.getVector());
            for (int i = 0; i < vec.length; i++) {
                vec[i] /= sdVec[i];
            }
            sumDist += Math.acos(MatrixOp.getEuclideanCosine(avgArrayList, vec));
        }
        return sumDist;
    }

    double getVolume(ClusterDatapoint[] cl, double[] sdVec, boolean useSideVectors) {
        if (cl.length < 2) {
            return 0;
        }
        double[] minArrayList = getAvgVec(cl, useSideVectors);
        double[] maxArrayList = getAvgVec(cl, useSideVectors);
        for (int i = 0; i < minArrayList.length; i++) {
            minArrayList[i] /= sdVec[i];
            maxArrayList[i] /= sdVec[i];
        }

        for (ClusterDatapoint c : cl) {
            double[] vec = MatrixOp.copy(useSideVectors?c.getSideVector():c.getVector());
            for (int i = 0; i < vec.length; i++) {
                vec[i] /= sdVec[i];
                maxArrayList[i] = Math.max(vec[i], maxArrayList[i]);
                minArrayList[i] = Math.min(vec[i], minArrayList[i]);
            }
        }
        double vol = 1;
        for (int i = 0; i < minArrayList.length; i++) {
            vol *= Math.abs(maxArrayList[i] - minArrayList[i]);
        }
        return vol;
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

}
