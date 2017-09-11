/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.codex.pipeline;

import java.util.HashMap;

/**
 *
 * @author Nikolay Samusik
 */
public interface Node {
    public HashMap<String, Object> process (HashMap<String, Object> input );
}
