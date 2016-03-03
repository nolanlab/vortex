/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package algorithms.yggdrasil;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Nikolay
 */
public class MinimumSpanningTree {

    int nodeNumber; // total number of nodes
    int[][] edges; // an array of tuples of node indices
    //TODO replace with triangular matrices; 

    public int[][] getEdges() {
        return edges;
    }
    
    

    /**
     * @param edgeLengthMatrix - will be corrupted in the process of
     */
    public MinimumSpanningTree(final double[][] edgeLenghMatrix) {
        if (edgeLenghMatrix.length != edgeLenghMatrix[0].length) {
            throw new IllegalArgumentException("The matrix must be square!");
        }
        this.nodeNumber = edgeLenghMatrix.length;

        //first doing binary search for the smallest edge that makes the graph connected;
        double min = edgeLenghMatrix[0][0];
        double max = edgeLenghMatrix[0][0];

        for (int i = 0; i < nodeNumber; i++) {
            for (int j = 0; j < i; j++) {
                min = Math.min(edgeLenghMatrix[i][j], min);
                max = Math.max(edgeLenghMatrix[i][j], max);
            }
        }

        boolean[][] edgeMtx = new boolean[nodeNumber][nodeNumber];
        boolean[] visitedNodes = new boolean[nodeNumber];
        System.err.println("finding the threshold");
        int comp = 0;
        for (int optRound = 0; optRound < 10; optRound++) {
            System.err.println("OptRound " + optRound + "min=" + min + ", max=" + max);
            double mid = (min + max) / 2d;
            fillConnectivityMatrix(edgeLenghMatrix, edgeMtx, mid);
            comp = getNumConnectedComponents(edgeMtx, visitedNodes);
            if (comp == 1) {
                max = mid;
            } else {
                min = mid;
            }
            System.err.println("comp = " + comp);
        }

        double ths = (min + max) / 2f;

        do {
            System.err.println("OptRound " + ths);
            fillConnectivityMatrix(edgeLenghMatrix, edgeMtx, ths * 1.01f);
        } while ((comp = getNumConnectedComponents(edgeMtx, visitedNodes)) > 1);

        System.err.println("Num components:" + comp);

        //now that we know the threshold, we can iterate through all edges between absMin and Ths in the descending order;
        System.err.println("Creating Edge List");

        List<int[]> edgeList = new LinkedList<>();

        for (int i = 0; i < nodeNumber; i++) {
            for (int j = 0; j < i; j++) {
                if (edgeLenghMatrix[i][j] < ths) {
                    edgeList.add(new int[]{i, j});
                }
            }
        }
        System.err.println("Edge list to array");

        edges = edgeList.toArray(new int[edgeList.size()][]);
        System.err.println("Sorting edge list");
        //Sorting edges so that the longest come first (descending length)
        Arrays.sort(edges, new Comparator<int[]>() {
            @Override
            public int compare(int[] o1, int[] o2) {
                return (int) Math.signum(edgeLenghMatrix[o1[0]][o1[1]] - edgeLenghMatrix[o2[0]][o2[1]]);
            }
        });

        //reverse-delete algorithm: starting with a connected graph, iterating through edges in a descending order of lenght and selecting only those, removal of which renders the graph disconnected. Such edges form an MST
        //such edges form an MST
        System.err.println("Doing one-by-one addition, total edges: " + edges.length);

        edgeList = new LinkedList<>();

        fillConnectivityMatrixWithFalse(edgeMtx);
        boolean[] nextLayer = new boolean[nodeNumber];
        boolean[] idxVisited = new boolean[nodeNumber];
        for (int i = 0; i < edges.length; i++) {
            if (i % 10000 == 0) {
                System.err.println(i);
            }
            if (edges[i] == null) {
                continue;
            }
            if (areConnected(edges[i][0], edges[i][1], edgeMtx, idxVisited, nextLayer)) {
                edges[i] = null;
            } else {
                edgeList.add(edges[i]);
                edgeMtx[edges[i][0]][edges[i][1]] = true;
                edgeMtx[edges[i][1]][edges[i][0]] = true;

                boolean[] connectedComp = getConnectedComponentOf(edges[i][0], edgeMtx, idxVisited, nextLayer);
                
                
                for (int j = i + 1; j < edges.length; j++) {
                    if (edges[j] == null) {
                        //System.err.println("edges["+j+"] is null");
                        continue;
                    }
                    if (connectedComp[edges[j][0]] && connectedComp[edges[j][1]]) {
                        edges[j] = null;
                    }
                }

                System.err.println("is a part of the MST:" + Arrays.toString(edges[i]));
            }
        }
        System.err.println("Found edges in the MST:" + edgeList.size());
        edges = edgeList.toArray(new int[edgeList.size()][]);
    }

    private boolean[] getConnectedComponentOf(int source, boolean[][] mtx, boolean[] hasBeenVisited, boolean[] nextLayer) {

        LinkedList<Integer> currLayer = new LinkedList<>();
        currLayer.add(source);
        int dim = hasBeenVisited.length;

        Arrays.fill(hasBeenVisited, false);
        Arrays.fill(nextLayer, false);
        //System.err.println("areConnected: " + source + ", " + dest);
        //System.err.println("nextLayer[source]= " + nextLayer[source]);
        do {
            Iterator<Integer> it = currLayer.iterator();
            while (it.hasNext()) {
                int idx = it.next();
                hasBeenVisited[idx] = true;
                //      System.err.println("visited:"+idx);
                for (int i = 0; i < dim; i++) {
                    if (i == idx) {
                        continue;
                    }
                    if ((mtx[idx][i] || mtx[i][idx]) && !hasBeenVisited[i]) {
                        //           System.err.println("nextLayer:" + i);
                        nextLayer[i] = true;

                    }

                }
            }
            currLayer.clear();
            for (int i = 0; i < nextLayer.length; i++) {
                if (nextLayer[i]) {
                    currLayer.add(i);
                    //    System.err.println("adding" + i);
                    nextLayer[i] = false;
                }
            }

            //  System.err.println("cnt" + cnt++);
        } while (!currLayer.isEmpty());
        return hasBeenVisited;

    }

    private boolean areConnected(int source, int dest, boolean[][] mtx, boolean[] hasBeenVisited, boolean[] nextLayer) {

        LinkedList<Integer> currLayer = new LinkedList<>();
        currLayer.add(source);
        int dim = hasBeenVisited.length;
        int cnt = 0;
        Arrays.fill(hasBeenVisited, false);
        Arrays.fill(nextLayer, false);
        //System.err.println("areConnected: " + source + ", " + dest);
        //System.err.println("nextLayer[source]= " + nextLayer[source]);
        do {
            Iterator<Integer> it = currLayer.iterator();
            while (it.hasNext()) {
                int idx = it.next();
                hasBeenVisited[idx] = true;
                //      System.err.println("visited:"+idx);
                for (int i = 0; i < dim; i++) {
                    if (i == idx) {
                        continue;
                    }
                    if ((mtx[idx][i] || mtx[i][idx]) && !hasBeenVisited[i]) {
                        //           System.err.println("nextLayer:" + i);
                        nextLayer[i] = true;
                        if (i == dest) {
                            return true;
                        }
                    }

                }
            }
            currLayer.clear();
            for (int i = 0; i < nextLayer.length; i++) {
                if (nextLayer[i]) {
                    currLayer.add(i);
                    //    System.err.println("adding" + i);
                    nextLayer[i] = false;
                }
            }

            //  System.err.println("cnt" + cnt++);
        } while (!currLayer.isEmpty());
        return false;

    }

    private void fillConnectivityMatrixWithFalse(boolean[][] edgeMtx) {
        for (boolean[] edgeMtx1 : edgeMtx) {
            Arrays.fill(edgeMtx1, false);
        }
    }

    private void fillConnectivityMatrix(double[][] edgeLenMtx, boolean[][] edgeMtx, double maxEdgeLen) {
        boolean val;
        for (int i = 0; i < edgeMtx.length; i++) {
            for (int j = 0; j < i; j++) {
                val = edgeLenMtx[i][j] < maxEdgeLen;
                edgeMtx[i][j] = val;
                edgeMtx[j][i] = val;
            }
        }
    }

    private int getNumConnectedComponents(boolean[][] mtx, boolean[] idxVisited) {
        boolean haveUnvisitedNodes = true;
        int dim = mtx.length;
        boolean[] nextLayer = new boolean[dim];
        Arrays.fill(idxVisited, false);
        LinkedList<Integer> currLayer = new LinkedList<>();
        currLayer.add(0);
        idxVisited[0] = true;
        int numConnectedComponents = 0;

        do {

            Arrays.fill(nextLayer, false);
            int cnt = 0;
            do {
                Iterator<Integer> it = currLayer.iterator();
                while (it.hasNext()) {
                    int idx = it.next();
                    for (int i = 0; i < dim; i++) {
                        if (mtx[idx][i]) {
                            mtx[idx][i] = false;
                            mtx[i][idx] = false;
                            idxVisited[i] = true;
                            nextLayer[i] = true;
                        }
                    }
                }
                currLayer.clear();
                for (int i = 0; i < nextLayer.length; i++) {
                    if (nextLayer[i]) {
                        currLayer.add(i);
                    }
                    nextLayer[i] = false;
                }
                //System.err.println("isGraphConnected round#"+(++cnt) + " currLayer"+ currLayer.size());
            } while (!currLayer.isEmpty());
            numConnectedComponents++;
            haveUnvisitedNodes = false;

            for (int i = 0; i < idxVisited.length && !haveUnvisitedNodes; i++) {
                if (!idxVisited[i]) {
                    currLayer.add(i);
                    idxVisited[i] = true;
                    haveUnvisitedNodes = true;
                }
            }
        } while (haveUnvisitedNodes);

        return numConnectedComponents;
    }

}
