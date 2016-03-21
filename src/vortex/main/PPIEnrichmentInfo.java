/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.main;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Nikolay
 */
public class PPIEnrichmentInfo {

    HashMap<String, Double> hmPValueByMethods = new HashMap<String, Double>();
    private static List<String> availableMethods = Arrays.asList(new String[]{"SumGraphWeights"});
}
