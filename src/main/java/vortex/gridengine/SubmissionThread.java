/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.gridengine;

import executionslave.ReusingTask;
import executionslave.Task;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class SubmissionThread<T> implements Runnable {

    Host host;
    TaskCompletionListner<T> listener;
    private Integer batchID;
    private boolean hasFinished = false;
    private Queue<Entry<Integer, Task<T>>> lstTasks;
//    Task<T>[] t;
//    int[] posInRetArray;

    public void setHost(Host host) {
        this.host = host;
    }

    public boolean hasFinished() {
        return hasFinished;
    }

    public Host getHost() {
        return host;
    }

    public SubmissionThread(Host host, final Queue<Entry<Integer, Task<T>>> lstTasks, TaskCompletionListner<T> listener, Integer batchID) {

        this.host = host;
        this.lstTasks = lstTasks;
        //this.t = t;
        //this.posInRetArray = posInArray;
        this.listener = listener;
        this.batchID = batchID;
    }

    public void cancelExecution() {
        hasFinished = true;
    }
    private static int cntExecuting = 0;

    private static synchronized void incrementCntExecuting() {
        logger.print("number of threads executing: " + (++cntExecuting));
    }

    private static synchronized void decrementExecuting() {
        logger.print("number of threads executing: " + (--cntExecuting));
    }

    @Override
    public void run() {

        T result = null;
        do {
            final Entry<Integer, Task<T>> nextEntry;

            try {
                Thread.currentThread().sleep((int) Math.rint(100));
            } catch (InterruptedException e) {
                logger.showException(e);
                return;
            }
            nextEntry = lstTasks.poll();

            if (nextEntry != null && !hasFinished) {

                //for (int i = 0; i < t.length && !hasFinished; i++) {
                //      T result = null;
                //logger.print("executing task# " + nextEntry.getKey());    
                if (nextEntry.getValue() instanceof ReusingTask) {
                    try {
                        //incrementCntExecuting();
                        result = host.getExecutionSlave().executeReusingTask(batchID, (ReusingTask<T>) nextEntry.getValue());
                        //decrementExecuting();
                    } catch (RemoteException ex) {
                        logger.showException(ex);
                    }
                } else {
                    try {
                        //incrementCntExecuting();
                        result = host.getExecutionSlave().executeTask((Task<T>) nextEntry.getValue());
                        //decrementExecuting();
                    } catch (RemoteException ex) {
                        logger.showException(ex);
                    }
                }
                if (listener != null) {
                    listener.taskCompleted(result, nextEntry.getKey());
                }
            }
            //logger.print("task# " + nextEntry.getKey() +" completed");    
        } while (result != null);
        hasFinished = true;
        lstTasks = null;
        this.listener = null;
    }
}
