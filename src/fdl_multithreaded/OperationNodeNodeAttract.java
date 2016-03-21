/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fdl_multithreaded;

import org.gephi.graph.api.Node;
import fdl_multithreaded.ForceFactory.AttractionForce;

/**
 *
 * @author Mathieu
 */
public class OperationNodeNodeAttract extends Operation{
    private Node n1;
    private Node n2;
    private AttractionForce f;
    private double coefficient;
    
    public OperationNodeNodeAttract(Node n1, Node n2, AttractionForce f, double coefficient){
        this.n1 = n1;
        this.n2 = n2;
        this.f = f;
        this.coefficient = coefficient;
    }
    
    @Override
    public void execute() {
        f.apply(n1, n2, coefficient);
    }
    
}
