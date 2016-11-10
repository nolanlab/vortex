/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.gridengine;

import executionslave.IExecutionSlave;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class Host {

    private String hostName;
    private IExecutionSlave slave;
    private int numThreads;

    public String getHostName() {
        return hostName;
    }

    public IExecutionSlave getExecutionSlave() {
        return slave;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public static Host createLocalHost() {
        return new Host();
    }

    /**
     * creates a local host
     */
    private Host() {
        numThreads = Runtime.getRuntime().availableProcessors();
        this.slave = (executionslave.IExecutionSlave) new executionslave.ExecutionSlave(numThreads);
    }

    public Host(String hostName) {
        this.hostName = hostName;
        String name = "ExecutionSlave";
        int port = -1;
        if (hostName.contains(":")) {
            port = Integer.parseInt(hostName.split(":")[1].trim());
            this.hostName = hostName.split(":")[0];
        }


        try {
            Registry registry = (port == -1) ? LocateRegistry.getRegistry(this.hostName) : LocateRegistry.getRegistry(this.hostName, port);
            this.slave = (executionslave.IExecutionSlave) registry.lookup(name);
        } catch (NotBoundException e) {
            logger.print("Error: no ExecutionSlave bound on the host " + this.hostName);
            logger.showException(e);
        } catch (RemoteException e) {
            logger.showException(e);
        }
        try {
            this.numThreads = slave.getNumberOfThreads();
        } catch (RemoteException e) {
            logger.showException(e);
        }
        this.numThreads = 0;
    }
}
