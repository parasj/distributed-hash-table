import server.RemoteDataNode;
import server.RemoteManager;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.SECONDS;

public class Manager implements RemoteManager {
    // The number of data nodes ever initialized
    private int maxDataNodes = 1024;
    private int clients = 0;
    private TreeMap<Integer, String> aliveNodes;
    private ScheduledExecutorService aliveChecker;
    private TreeMap<Integer, ScheduledFuture<?>> nodeFutures;

    public Manager() {
        aliveNodes = new TreeMap<>();
        aliveChecker = new ScheduledThreadPoolExecutor(4);
        nodeFutures = new TreeMap<>();
    }

    public static void main(String args[]) {
        try {
            Manager manager = new Manager();
            // exports the manager object to receive incoming remote
            // methods calls in an anonymous TCP port
            RemoteManager stub = (RemoteManager)
                    UnicastRemoteObject.exportObject(manager, 0);

            // RMI gives you a registry API so that clients can look up remote objects
            // by name in order to obtain the corresponding stubs
            // The getRegistry call will fail if there is no registry running.

            Registry registry = LocateRegistry.getRegistry();
            registry.bind("server.RemoteManager", stub);
            System.out.println("Successfully registered the manager with the Registry");

            // Register a shutdown hook so that we de register from
            // local registry when the Manager is killed
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    registry.unbind("server.RemoteManager");
                    System.out.println("Successfully unbound from the local Registry");
                } catch (Exception e) {
                    System.err.println("Manager shutdown exception: " + e.toString());
                    e.printStackTrace();
                }
            }));

        } catch (ConnectException e) {
            System.err.println("Manager exception: Please first start the RMI Registry " +
                    "(run rmiregistry in the folder this class resides in)");
            e.printStackTrace();
        } catch (RemoteException | AlreadyBoundException e) {
            System.err.println("Manager exception: " + e.toString());
            e.printStackTrace();
        }
    }

    // Every data node is assigned an id on initialization.
    // Nodes are arranged on a ring of size maxDataNodes and
    // given a random position on that ring. Supports DataNodes
    // resuming the same id after a shutdown.
    public int registerDataNode(int id) throws RemoteException {
        Random ran = new Random();
        while(aliveNodes.containsKey(id))
            id = ran.nextInt(maxDataNodes);
        System.out.println("Successfully registered new DataNode with id " + id);

        try {
            synchronized (this) {
                aliveNodes.put(id, UnicastRemoteObject.getClientHost());
            }
        } catch (ServerNotActiveException e) {
            e.printStackTrace();
        }

        updateMembership(id);

        int finalId = id;

        final ScheduledFuture<?> checker = aliveChecker.scheduleAtFixedRate(() ->
        {
            try {
                Registry registry = LocateRegistry.getRegistry(aliveNodes.get(finalId));
                RemoteDataNode node = (RemoteDataNode) registry.lookup("server.RemoteDataNode" + finalId);
                if (!node.aliveCheck()) {
                    aliveNodes.remove(finalId);
                    updateMembership(-1);
                }
            } catch (Exception e) {
                System.err.println("Error in heartbeat function on id " + finalId);
                aliveNodes.remove(finalId);
                updateMembership(-1);
//                e.printStackTrace();
            }
        }, 10, 10, SECONDS);
        nodeFutures.put(id, checker);

        return id;
    }

    public int registerDataNode() throws RemoteException {
        return registerDataNode((new Random())
                .nextInt(maxDataNodes));
    }

    public int registerClient() {
        return clients++;
    }

    public TreeMap<Integer, String> getAliveNodes() {
        return aliveNodes;
    }

    public int getMaxDataNodes() {
        return maxDataNodes;
    }

    public void deRegisterDataNode(int id) {
        synchronized (this) {aliveNodes.remove(id);}
        nodeFutures.get(id).cancel(true);
        nodeFutures.remove(id);
        System.out.println("Successfully deregistered DataNode with id " + id);
        updateMembership(-1);
    }

    private void updateMembership(int exclude) {
        // since this succeeded, we update all data nodes with the new membership
        // information
        synchronized (this) {
            for (Integer dNodeId : aliveNodes.keySet()) {
                if (dNodeId != exclude) {
                    System.out.println("Updating data node " + dNodeId
                            + " at " + aliveNodes.get(dNodeId));
                    try {
                        Registry registry = LocateRegistry.getRegistry(aliveNodes.get(dNodeId));
                        RemoteDataNode node = (RemoteDataNode) registry.lookup("server.RemoteDataNode" + dNodeId);
                        node.updateMembership(aliveNodes);
                    } catch (RemoteException | NotBoundException e) {
                        System.err.printf("Couldn't access node %d on host %s\n", dNodeId, aliveNodes.get(dNodeId));
//                    e.printStackTrace();
                    }
                }
            }
        }
    }
}
