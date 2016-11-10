/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.gridengine;

/**
 *
 * @author Nikolay
 */
public interface TaskCompletionListner<T> {

    public void taskCompleted(T result, int posInRetArray);
}
