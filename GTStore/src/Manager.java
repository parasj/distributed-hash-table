import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;
import java.util.TreeMap;

public class Manager implements RemoteManager {
    // The number of data nodes ever initialized
    private int maxDataNodes = 1024;
    private int clients = 0;
    private TreeMap<Integer, String> aliveNodes;

    private Manager() {
        aliveNodes = new TreeMap<>();
    }

    // Every data node is assigned an id on initialization.
    // Nodes are arranged on a ring of size maxDataNodes and
    // given a random position on that ring.
    public int registerDataNode() throws RemoteException {
        Random ran = new Random();
        int id;
        do {
            id = ran.nextInt(maxDataNodes);
        } while(aliveNodes.containsKey(id));
        System.out.println("Successfully registered new DataNode with id " + id);

        try {
            aliveNodes.put(id, UnicastRemoteObject.getClientHost());
        } catch (ServerNotActiveException e) {
            e.printStackTrace();
        }

        updateMembership(id);
        return id;
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
        aliveNodes.remove(id);
        System.out.println("Successfully deregistered DataNode with id " + id);
        updateMembership(-1);
    }

    public TreeMap<Integer, String> getDataNodes() {
        return aliveNodes;
    }

    // TODO redirect clients to the appropriate data node
    // Do we do this on a per-request basis or a per-session basis?
    // per-request is more resilient, but then consistency issues

    private void updateMembership(int exclude) {
        // since this succeeded, we update all data nodes with the new membership
        // information
        for (Integer dNodeId : aliveNodes.keySet()) {
            if(dNodeId != exclude) {
                System.out.println("Updating data node " + dNodeId
                        + " at " + aliveNodes.get(dNodeId));
                try {
                    Registry registry = LocateRegistry
                            .getRegistry(aliveNodes.get(dNodeId));
                    RemoteDataNode node = (RemoteDataNode)
                            registry.lookup("RemoteDataNode" + dNodeId);
                    node.updateMembership(aliveNodes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
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

            // TODO: Make sure that the arguments to this are correct on the DataNode
            // so that it hits whatever server is running the manager, not localhost
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("RemoteManager", stub);
            System.out.println("Successfully registered the manager with the Registry");

            // Register a shutdown hook so that we de register from
            // local registry when the Manager is killed
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    registry.unbind("RemoteManager");
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
        } catch (Exception e) {
            System.err.println("Manager exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
