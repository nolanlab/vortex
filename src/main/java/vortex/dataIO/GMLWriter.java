/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.dataIO;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.UndirectedGraph;

/**
 *
 * @author Nikolay
 */
public class GMLWriter {
    public static void writeGML(UndirectedGraph g, File f)throws IOException{
        BufferedWriter br = new BufferedWriter(new FileWriter(f));
        
    br.write("Creator \"X-shift\"\n");
    br.write("Version 1\n");
    br.write("graph\n");
    br.write("[\n");
    br.write("directed 0\n");
        for(Node n: g.getNodes()){
            br.write("node\n");
            br.write("[\n");
            br.write("id "+ n.getAttribute("cluster")+"\n");
            br.write("name \""+ n.getAttribute("cluster")+"\"\n");
            br.write("]\n");
        }
        
        for(Edge e: g.getEdges()){
            br.write("edge\n");
            br.write("[\n");
            br.write("source " + e.getSource().getAttribute("cluster")+"\n");
            br.write("target " + e.getTarget().getAttribute("cluster")+"\n");
            br.write("weight 1.0\n");
            br.write("]\n");
        }
        
         br.write("]\n");
        br.flush();
        br.close();
        
    }
}
