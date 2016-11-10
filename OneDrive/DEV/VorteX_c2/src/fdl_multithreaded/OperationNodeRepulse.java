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
public class OperationNodeRepulse extends Operation{
    private Node n;
    private RepulsionForce f;
    private double coefficient;
    
    public OperationNodeRepulse(Node n, RepulsionForce f, double coefficient){
        this.n = n;
        this.f = f;
        this.coefficient = coefficient;
    }
    
    @Override
    public void execute() {
        f.apply(n, coefficient);
    }
}
