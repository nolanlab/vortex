/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.main;

import sandbox.clustering.Datapoint;

/**
 *
 * @author Nikolay
 */
public interface DatapointFilter {

    public Datapoint[] getFilteredList(Datapoint[] input);
}
