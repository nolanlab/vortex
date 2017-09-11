/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.util;

import java.util.ArrayList;

/**
 *
 * @author Nikolay
 */
public class GraphNode<T> {

    T object;
    ArrayList<GraphNode<T>> neighbors = new ArrayList<GraphNode<T>>();
    int index;

    public GraphNode(T object, int index) {
        this.object = object;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public T getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }

    public void linkTo(GraphNode<T> anotherNode) {
        anotherNode.neighbors.add(this);
        neighbors.add(anotherNode);
    }

    public void unlink(GraphNode<T> anotherNode) {
        anotherNode.neighbors.remove(this);
        neighbors.remove(anotherNode);
    }

    public synchronized ArrayList<GraphNode<T>> getConnectedSubgraph() {
        ArrayList<GraphNode<T>> al = new ArrayList<GraphNode<T>>();
        recursiveAddToSubgraph(al);
        return al;
    }

    private synchronized void recursiveAddToSubgraph(ArrayList<GraphNode<T>> al) {
        if (!al.contains(this)) {
            al.add(this);
        }
        for (GraphNode<T> graphNode : neighbors) {
            if (!al.contains(graphNode)) {
                graphNode.recursiveAddToSubgraph(al);
            }
        }
    }
}
