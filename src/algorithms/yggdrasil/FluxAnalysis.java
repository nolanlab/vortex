/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package algorithms.yggdrasil;

import java.util.Arrays;

/**
 *
 * @author Nikolay
 */
public class FluxAnalysis {
    public static double [] getEdgeWeights(int[][] edges, boolean [][] edgeMatrix){
        
        for (boolean[] e : edgeMatrix) {
            Arrays.fill(e, false);
        }
        
        for (int[] e : edges) {
            edgeMatrix[e[0]][e[1]]=true;
            edgeMatrix[e[1]][e[0]]=true;
        }
        //not very memory-efficient
        int[][] weightMatrix = new int[edgeMatrix.length][edgeMatrix.length];
        
        return null;
    }
}
