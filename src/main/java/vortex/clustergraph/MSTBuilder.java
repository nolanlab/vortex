/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustergraph;

import sandbox.clustering.Cluster;
import sandbox.clustering.ClusterMember;
import sandbox.clustering.Datapoint;
import sandbox.clustering.DistanceMeasure;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2Builder;
import org.gephi.project.api.ProjectController;
import org.gephi.statistics.plugin.ConnectedComponents;
import org.openide.util.Lookup;
import util.MatrixOp;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class MSTBuilder {
    
    private final float EDGE_SCALING = 5.0f;
    private final boolean use_all_params = false;
    
    public UndirectedGraph buildGraph(Cluster[] clusters, final boolean quantile_dist) {
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();

        //Get a graph model - it exists because we have a workspace
        GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
        UndirectedGraph localGraph = buildMSTGraph(clusters, clusters[0].getClusterSet().getDistanceMeasure(), graphModel);
        
        

        ForceAtlas2 layout = new ForceAtlas2(new ForceAtlas2Builder());
        // DAGLayout layout = new DAGLayout(new DAGLayoutBuilder());
        layout.setGraphModel(graphModel);
        layout.initAlgo();
        //  layout.resetPropertiesValues();
        /* layout.setBarnesHutOptimize(false);
         layout.setStrongGravityMode(false);
        
         layout.setScalingRatio(15.0);*/
        //layout.setAdjustSizes(true);
        //layout.se
        // layout.setGravity(10d);
        layout.setScalingRatio(layout.getScalingRatio() * 1.5);
        for (Node n : localGraph.getNodes()) {
            n.setSize(n.size()/ 1000f);
        }
        layout.setScalingRatio(layout.getScalingRatio() / 1.5);
        for (int i = 0; i < 10000 && layout.canAlgo(); i++) {
            layout.goAlgo();
        }

        for (Node n : localGraph.getNodes()) {
            n.setSize(n.size()* 1000f);
        }

        layout.setAdjustSizes(true);

        for (int i = 0; i < 10000 && layout.canAlgo(); i++) {
            layout.goAlgo();
        }

        layout.endAlgo();

        for (Edge n : localGraph.getEdges()) {
            n.setWeight(n.getWeight() * 3);
        }
        return localGraph;
    }
    
    private double[] getVectorFor(Datapoint d) {
        return use_all_params ? MatrixOp.concat(d.getVector(), Arrays.copyOfRange(d.getSideVector(), 1, d.getSideVector().length - 3)) : d.getVector();
    }
    
    private class MyEdge {
        public final Node n1;
        public final Node n2;
        public final float weight;
        public MyEdge(Node n1, Node n2, float weight) {
            this.n1 = n1;
            this.n2 = n2;
            this.weight = weight;
        }
    }
    
    private HashMap<Integer, Cluster> cid = new HashMap<>();
    
    private UndirectedGraph buildMSTGraph(Cluster[] clusters, DistanceMeasure dm, GraphModel gm) {

        UndirectedGraph graph = gm.getUndirectedGraph();

        graph.getModel().getNodeTable().addColumn("cluster", Integer.class);
        graph.getModel().getNodeTable().addColumn("name", String.class);
        graph.getModel().getNodeTable().addColumn("logRatio", Double.class);
        graph.getModel().getNodeTable().addColumn("hd-label", String.class);

        for (Cluster c : clusters) {
            cid.put(c.getID(), c);
        }

        final Node[] nodes = new Node[clusters.length];
        int idx = 0;

        for (Cluster c : clusters) {
            Node n0 = graph.getModel().factory().newNode("id"+idx);
            //n0.getNodeData().setLabel( + c.toString());
            n0.setX((float) c.getMode().getVector()[0]);
            n0.setY((float) c.getMode().getVector()[1]);
            n0.setSize((float) Math.max(1, Math.pow(c.getAvgSize(), 0.33)));
            String label = "id" + c.getID() + (c.getComment().trim().length() > 0 ? (": " + c.getComment()) : "");
            n0.setLabel(label);//c.toString());
            n0.setAttribute("hd-label", label);
            n0.setAttribute("name", (c.getComment().trim().length() > 0 ? (": " + c.getComment()) : ""));
            n0.setAttribute("cluster", c.getID());
            graph.addNode(n0);
            nodes[idx++] = n0;
        }

        Datapoint[] clusterCentroids = new Datapoint[nodes.length];

        for (int i = 0; i < clusterCentroids.length; i++) {
            clusterCentroids[i] = new Datapoint("avg" + clusters[i].toString(), clusters[i].getMode().getVector(), clusters[i].getMode().getSideVector(), i);
        }

        //final DistanceMeasure dm = clusters[0].getClusterSet().getDistanceMeasure();
        List<Edge> edges = new ArrayList<>();

        double maxSim = 0;

        for (int i = 0; i < clusterCentroids.length; i++) {
            for (int j = i + 1; j < clusterCentroids.length; j++) {
                if (true) {// nnOfMid.equals(clusterCentroids[i]) || nnOfMid.equals(clusterCentroids[j])) {
                    Double similarity = dm.getSimilarity(clusters[i].getMode().getVector(), clusters[j].getMode().getVector());

                    if (Double.isInfinite(similarity) || Double.isNaN(similarity)) {
                        logger.print(clusters[i].getMode().getVector());
                        logger.print(clusters[j].getMode().getVector());
                    }
                    maxSim = Math.max(maxSim, similarity);
                    Edge e = graph.getModel().factory().newEdge(nodes[i], nodes[j], 0, false);
                    e.setWeight(similarity);
                    edges.add(e);
                    graph.addEdge(e);
                }
            }
        }

        Edge[] e = edges.toArray(new Edge[edges.size()]);
        for (int i = 0; i < e.length; i++) {
            e[i].setWeight((EDGE_SCALING * e[i].getWeight()) / maxSim);
        }

        Arrays.sort(e, new Comparator<Edge>() {
            @Override
            public int compare(Edge o1, Edge o2) {
                return (int) Math.signum(o1.getWeight() - o2.getWeight());
            }
        });

        for (int i = 0; i < e.length; i++) {
            Double w = e[i].getWeight();
            graph.removeEdge(e[i]);
            ConnectedComponents cc = new ConnectedComponents();
            cc.execute(graph.getModel());
            if (cc.getConnectedComponentsCount() > 1) {
                e[i] = graph.getModel().factory().newEdge(e[i].getSource(), e[i].getTarget(), 0, false);
                e[i].setWeight(w * EDGE_SCALING);
                graph.addEdge(e[i]);
                logger.print("keeping edge:" + e[i].getTarget() + "<->" + e[i].getSource() + "" + e[i].getWeight());
            }
        }

        for (Edge ed : graph.getEdges().toArray()) {
            logger.print(ed.getWeight());
        }
        
        return graph;
    }

}
