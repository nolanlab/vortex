/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.util;

import java.util.ArrayList;

/**
 *
 * @author Nikki
 */
public abstract class Ext<T extends Object, V extends Object> {

    public abstract T extractProperty(V param);

    public ArrayList<T> pullArray(V[] array) {
        ArrayList<T> arr = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            arr.add(extractProperty(array[i]));
        }
        return arr;
    }
}
