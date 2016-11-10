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
public class OperationNodeNodeRepulse extends Operation{
    private Node n1;
    private Node n2;
    private RepulsionForce f;
    
    public OperationNodeNodeRepulse(Node n1, Node n2, RepulsionForce f){
        this.n1 = n1;
        this.n2 = n2;
        this.f = f;
    }
    
    @Override
    public void execute() {
        f.apply(n1, n2);
    }
}
