/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fdl_multithreaded;

import org.gephi.graph.api.Node;
import fdl_multithreaded.ForceFactory.RepulsionForce;

/**
 *
 * @author Mathieu
 */
public class OperationNodeRegionRepulse extends Operation{
    private Node n;
    private Region r;
    private RepulsionForce f;
    private double theta;
    
    public OperationNodeRegionRepulse(Node n, Region r, RepulsionForce f, double theta){
        this.n = n;
        this.f = f;
        this.r = r;
        this.theta = theta;
    }
    
    @Override
    public void execute() {
        r.applyForce(n, f, theta);
    }
    
}
