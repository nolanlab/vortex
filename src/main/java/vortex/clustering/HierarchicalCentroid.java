/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustering;

import java.util.ArrayList;
import java.util.Enumeration;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import sandbox.clustering.Datapoint;

/**
 *
 * @author Nikolay
 */
public class HierarchicalCentroid extends DefaultMutableTreeNode {

    private static final long serialVersionUID = 1L;
    //int dpCount;
    protected int centID;
    protected String label;
    protected double distBtwChildren;

    public String getLabel() {
        return label;
    }
    
    

    @Override
    public Enumeration<HierarchicalCentroid> breadthFirstEnumeration() {
        return super.breadthFirstEnumeration();
    }

    public int getID() {
        return centID;
    }
    
    
    

    public HierarchicalCentroid(int centID, HierarchicalCentroid child1, HierarchicalCentroid child2, double distBtwChildren) {
        super(centID);
        this.distBtwChildren = distBtwChildren;
        this.centID = centID;
        
        ArrayList<Datapoint> newDP = new ArrayList<Datapoint>();

        if (child1 != null && child1.equals(child2)) {
            throw new IllegalStateException("Children are the same");
        }

        for (Datapoint datapoint : child1.getDatapoints()) {
            if (newDP.contains(datapoint)) {
                throw new IllegalArgumentException("Child1 contains a duplicate datapoint: " + datapoint);
            }
            newDP.add(datapoint);
        }
        //newDP.addAll(Arrays.asList(child1.getDatapoints()));
        super.add(child1);


        if (child1.getDistBtwChildren() > distBtwChildren + 0.0001) {
            throw new IllegalStateException("Current dist btw children lower then in one of children");
        }

        for (Datapoint datapoint : child2.getDatapoints()) {
            if (newDP.contains(datapoint)) {
                throw new IllegalArgumentException("Child2 contains a duplicate datapoint: " + datapoint);
            }
            newDP.add(datapoint);
        }
        //newDP.addAll(Arrays.asList(child2.getDatapoints()));
        super.add(child2);

        setUserObject(newDP.toArray(new Datapoint[newDP.size()]));
    }

    public HierarchicalCentroid(Datapoint[] dp, int centID, double distBtwChildren) {
        super();
        this.distBtwChildren = distBtwChildren;
        setUserObject(dp);
        this.centID = centID;
    }

    public Datapoint[] getDatapoints() {
        return (Datapoint[])getUserObject();
    }

    public double getDistBtwChildren() {
        return distBtwChildren;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HierarchicalCentroid) {
            return ((HierarchicalCentroid) obj).centID == centID;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + this.getDatapoints().length;
        hash = 37 * hash + (this.getDatapoints() != null ? this.getDatapoints().hashCode() : 0);
        hash = 37 * hash + this.centID;
        hash = 37 * hash + (int) (Double.doubleToLongBits(this.distBtwChildren) ^ (Double.doubleToLongBits(this.distBtwChildren) >>> 32));
        return hash;
    }

    @Override
    public void add(MutableTreeNode newChild) {
        super.add(newChild);
    }
}
