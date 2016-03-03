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
public class NodesThread implements Runnable{
    private Node[] nodes;
    private int from;
    private int to;
    private Region rootRegion;
    private boolean barnesHutOptimize;
    private RepulsionForce Repulsion;
    private double barnesHutTheta;
    private double gravity;
    private RepulsionForce GravityForce;
    private double scaling;
    
    public NodesThread(Node[] nodes, int from, int to, boolean barnesHutOptimize, double barnesHutTheta, double gravity, RepulsionForce GravityForce, double scaling, Region rootRegion, RepulsionForce Repulsion){
        this.nodes = nodes;
        this.from = from;
        this.to = to;
        this.rootRegion = rootRegion;
        this.barnesHutOptimize = barnesHutOptimize;
        this.Repulsion = Repulsion;
        this.barnesHutTheta = barnesHutTheta;
        this.gravity = gravity;
        this.GravityForce = GravityForce;
        this.scaling = scaling;
    }
    
    @Override
    public void run() {
        // Repulsion
        if (barnesHutOptimize) {
            for (int nIndex = from; nIndex < to; nIndex++) {
                Node n = nodes[nIndex];
                rootRegion.applyForce(n, Repulsion, barnesHutTheta);
            }
        } else {
            for (int n1Index = from; n1Index < to; n1Index++) {
                Node n1 = nodes[n1Index];
                for (int n2Index = 0; n2Index < n1Index; n2Index++) {
                    Node n2 = nodes[n2Index];
                    Repulsion.apply(n1, n2);
                }
            }
        }

        // Gravity
        for (int nIndex = from; nIndex < to; nIndex++) {
            Node n = nodes[nIndex];
            GravityForce.apply(n, gravity / scaling);
        }
    }
    
}
