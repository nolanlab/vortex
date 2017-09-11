/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustergraph;

import clustering.ClusterMember;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.tree.TreeNode;
import vortex.clustering.HierarchicalCentroid;

/**
 *
 * @author Nikolay Samusik
 */
public class ClusterTreeNode extends HierarchicalCentroid {

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
            cm = getClusterMemberList(this.getUserObject());
        }
        return cm;
    }

    private static ClusterMember[] getClusterMemberList(ClusterPhylogeny.ClusterDatapoint[] c) throws SQLException {

        int size = 0;

        for (ClusterPhylogeny.ClusterDatapoint cluster : c) {
            size += cluster.getCluster().size();
        }

        ClusterMember[] out = new ClusterMember[size];

        int i = 0;
        for (ClusterPhylogeny.ClusterDatapoint cd : c) {
            for (ClusterMember cm : cd.getCluster().getClusterMembers()) {
                out[i++] = cm;
            }
        }
        return out;
    }

    @Override
    public ClusterPhylogeny.ClusterDatapoint[] getUserObject() {
        return (ClusterPhylogeny.ClusterDatapoint[]) super.getUserObject(); //To change body of generated methods, choose Tools | Templates.
    }

    public ClusterTreeNode(ClusterPhylogeny.ClusterDatapoint[] clusters, String label, double distBtwChildren) {
        super(clusters, idTracker.addAndGet(1), distBtwChildren);
        this.label = label;
    }

    public ClusterTreeNode(ClusterPhylogeny.ClusterDatapoint[] clusters, boolean allowsChildren, String label, double distBtwChildren) {
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
