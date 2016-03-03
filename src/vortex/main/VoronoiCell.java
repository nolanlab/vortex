/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.main;

import clustering.Datapoint;
import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author Nikolay
 */
public class VoronoiCell implements Serializable {

    /**
     * Initial Version 1.00
     */
    public static final long serialVersionUID = 100L;
    public Datapoint dp;
    public double point_count, volume;
    public ArrayList<Datapoint> neighbors;

    public VoronoiCell(Datapoint dp, double point_count, double volume) {
        this.dp = dp;
        this.point_count = point_count;
        this.volume = volume;
        neighbors = new ArrayList<>();
    }
}
