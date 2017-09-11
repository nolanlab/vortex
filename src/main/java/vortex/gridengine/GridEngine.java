/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.gridengine;

import executionslave.ReusableObject;
import executionslave.Task;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import util.DefaultEntry;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class GridEngine {

    //private static ClassFileServer srv;
    Host[] hosts = null;
    int maxNumThreads = 0;
    private static GridEngine instance = null;

    public void killHosts() {

        for (Host h : hosts) {
            try {
                h.getExecutionSlave().die();
            } catch (RemoteException ex) {
                logger.print(ex);
            }
        }
    }

    /**
     * @return returns an instance of GridEngine
     */
    public static synchronized GridEngine getInstance() {
        if (instance == null) {
            instance = new GridEngine();
        }
        return instance;
    }

    public boolean waitForCompletion(int batchID) {
        do {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                logger.showException(e);
                cancelExecution();
                return false;
            }
        } while (getNumberOfTasksToGo(batchID) > 0);
        return true;
    }

    public void cancelExecution() {
//        executionQueue.clear();

        for (SubmissionThread st : submissionThreads) {
            if (st != null) {
                st.cancelExecution();
            }
        }
        /* for (SubmissionThread st : executionQueue) {
         st.cancelExecution();
         }*/
        try {
            for (Host host : hosts) {
                host.getExecutionSlave().stopAllThreads();
            }
        } catch (RemoteException e) {
            logger.print();
        }
    }
    private SubmissionThread[] submissionThreads;
    private Host[] threadHostIdx;

    private GridEngine() {
        /*
         System.setSecurityManager(new RMISecurityManager() {
         @Override
         public void checkConnect(String host, int port) {
         }
         @Override
         public void checkConnect(String host, int port, Object context) {
         }
         });

         try {
         //initializing the Class File Server
         int port = 6026;
         if (srv == null) {
         do{
         try{
         logger.print("Binding ClassServer on port " + port);
         srv = new ClassFileServer(port, System.getProperty("java.class.path"));
         }catch(Exception e){
         logger.print(e.getMessage());
         logger.print("ClassServer already bound on port "+  (port++) + ". Rebinding on "+ port );
         }
         }while(srv == null && port < 7000);
         }

         //Reading the Host Table
         BufferedReader in = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("hostTable.txt")));
         ArrayList<String> lines = new ArrayList<String>();
         String s = null;
         while ((s = in.readLine()) != null) {
         s = s.trim();
         if (!s.equals("") && !s.startsWith("//")) {
         lines.add(s);
         }
         }

         ArrayList<Host> alHosts = new ArrayList<Host>();

         for (int i = 0; i < lines.size(); i++) {
         Host h = new Host(lines.get(i).split(",")[0]);
         if(h!=null) alHosts.add(h);
         }

         hosts = alHosts.toArray(new Host[alHosts.size()]);

         if(getSupercomputerHost()== null){
               
         }

         hosts = alHosts.toArray(new Host[alHosts.size()]);

         } catch (FileNotFoundException e) {
         logger.print("GridEngine host table file not found");
         e.printStackTrace();
         } catch (IOException e) {
         e.printStackTrace();
         }
         for (Host f : hosts) {
         maxNumThreads += f.getNumThreads();
         }
         */
        hosts = new Host[]{Host.createLocalHost()};
//        executionQueue = new LinkedList<>();
        for (Host f : hosts) {
            maxNumThreads += f.getNumThreads();
        }
        threadHostIdx = new Host[maxNumThreads];
        int i = 0;
        for (Host f : hosts) {
            for (int j = 0; j < f.getNumThreads(); j++) {
                threadHostIdx[i++] = f;
            }
        }
        submissionThreads = new SubmissionThread[maxNumThreads];
    }
    private HashMap<Integer, Integer> hmDeleteReusableObjectAfterExecution = new HashMap<>();

    public int getMaxNumThreads() {
        return maxNumThreads;
    }

    public Host[] getHosts() {
        return hosts;
    }

    public synchronized void deleteReusableObject(Integer objID) {
        logger.print("removing reusable object #" + objID + " from cache");
        for (Host h : hosts) {
            try {
                h.getExecutionSlave().removeReusableObject(objID);
            } catch (RemoteException e) {
                logger.showException(e);
            }
        }
        System.gc();
    }
    //private LinkedList<SubmissionThread> executionQueue;
    private int lastExecBatchID;

    public synchronized int getNextExecutionBatchID() {
        return lastExecBatchID++;
    }
    HashMap<Integer, Integer[]> hmExecutionTracking = new HashMap<>();

    public synchronized Integer getNumberOfTasksToGo(Integer batchID) {
        return hmExecutionTracking.get(batchID)[0];
    }

    private synchronized Integer getBatchSize(Integer batchID) {
        return hmExecutionTracking.get(batchID)[1];
    }

    private synchronized void decrementBatchTrack(Integer batchID) {
        if (hmDeleteReusableObjectAfterExecution.get(batchID) != null) {
            if (hmExecutionTracking.get(batchID)[0] == 1) {
                deleteReusableObject(hmDeleteReusableObjectAfterExecution.get(batchID));
            }
        }
        hmExecutionTracking.get(batchID)[0]--;

        double fractionComplete = 1.0 - (getNumberOfTasksToGo(batchID) / (double) getBatchSize(batchID));
        double prevFractionComplete = 1.0 - ((getNumberOfTasksToGo(batchID) - 1) / (double) getBatchSize(batchID));

        if (((int) (fractionComplete * 200.0)) != ((int) (prevFractionComplete * 200.0))) {
            logger.print("batch #" + batchID + " progress: " + (((int) (fractionComplete * 200.0))) / 2.0 + "%");
        }
        //logger.print("batchID = " + batchID + "tasks to go: " +  hmExecutionTracking.get(batchID));
    }

    public <T> Integer submitBatch(Task<T>[] tasks, final T[] returnArray, final TaskCompletionListner<T> completionListener, ReusableObject object) {
        final Integer oid = submitReusableObject(object);
        final Integer bid = submitBatch(tasks, returnArray, completionListener, oid);
        hmDeleteReusableObjectAfterExecution.put(bid, oid);
        return bid;
    }

    public Integer submitReusableObject(ReusableObject obj) {
        Integer batchID = null;
        if (obj != null) {
            boolean batchIDReserved;
            Integer tentBatchID = 0;
            do {
                tentBatchID = getNextExecutionBatchID();
                batchIDReserved = true;
                for (Host h : getHosts()) {
                    try {
                        batchIDReserved &= h.getExecutionSlave().reserveBatchID(tentBatchID);
                    } catch (RemoteException r) {
                        logger.showException(r);
                        return null;
                    }
                }
            } while (!batchIDReserved);

            batchID = tentBatchID;

            for (Host h : getHosts()) {
                try {
                    h.getExecutionSlave().putReusableObject(batchID, obj);
                } catch (RemoteException r) {
                    logger.showException(r);
                    return null;
                }
            }
        }
        return batchID;
    }

    public <T> Integer submitBatch(Task<T>[] tasks, final T[] returnArray, final TaskCompletionListner<T> completionListener, Integer objectID) {

        final Integer batchID = getNextExecutionBatchID();
//
//        double chunkSize = ((double)tasks.length / (double)submissionThreads.length);
//        int[] indices = new int[tasks.length];
//        for (int i = 0; i < indices.length; i++) {
//            indices[i] = i;
//        }
//        
        final Queue<Entry<Integer, Task<T>>> lstTasks = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < tasks.length; i++) {
            if (tasks[i] != null) {
                lstTasks.add(new DefaultEntry<>(i, tasks[i]));
            }
        }

//        int from = 0;
//        int to = 0;
//        
        for (int i = 0; i < submissionThreads.length; i++) {
//            from = to;
//            to = Math.min(tasks.length, (int)Math.floor((i+1) * chunkSize));
//           
            //logger.print("from: " + from + ", to: "+ to);
//            Task<T>[] chunk = Arrays.copyOfRange(tasks, from, to);
//            int[] chunkIdx = Arrays.copyOfRange(indices, from, to);
            submissionThreads[i] = new SubmissionThread(threadHostIdx[i], lstTasks, new TaskCompletionListner<T>() {
                @Override
                public void taskCompleted(T result, int posInRetArray) {
                    if (result == null) {
                        logger.print("null result " + posInRetArray);
                    }
                    if (returnArray != null) {
                        returnArray[posInRetArray] = result;
                    }
                    if (completionListener != null) {
                        completionListener.taskCompleted(result, posInRetArray);
                    }
                    decrementBatchTrack(batchID);
                    //logger.print(getNumberOfTasksToGo(batchID));
                }
            }, objectID);
        }

        hmExecutionTracking.put(batchID, new Integer[]{lstTasks.size(), lstTasks.size()});

        for (int k = 0; k < submissionThreads.length; k++) {
            if (submissionThreads[k] != null) {
                new Thread(submissionThreads[k]).start();
            }
        }
        try {
            logger.print("task batch #" + batchID + " consisting of " + lstTasks.size() + " '" + lstTasks.peek().getValue().getClass().getSimpleName() + "' tasks has been submitted");
        } catch (NullPointerException e) {
            logger.print("task batch #" + batchID + " consisting of " + lstTasks.size() + " has been completed");
        }
        return batchID;
    }

    public <T> Integer submitBatch(Task<T>[] tasks, final T[] returnArray, final TaskCompletionListner<T> completionListener) {
        return submitBatch(tasks, returnArray, completionListener, (Integer) null);
    }
}
