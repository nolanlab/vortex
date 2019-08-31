/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.gui.clusterdendrogram;

import java.awt.geom.Point2D;

/**
 *
 * @author Nikolay
 */
public class ClusterPlotRelation {

    public Point2D start;
    public Point2D end;
    public String label;

    public ClusterPlotRelation(Point2D start, Point2D end, String label) {
        this.start = start;
        this.end = end;
        this.label = label;
    }
}
