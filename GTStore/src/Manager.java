import java.rmi.ConnectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Set;

public class Manager implements RemoteManager {
    // The number of data nodes ever initialized
    private int nodes;
    private Set<Integer> aliveNodes;

    private Manager() {
        nodes = 0;
        aliveNodes = new HashSet<>();
    }

    // Every data node is assigned an id on initialization
    public int registerDataNode() {
        System.out.println("Successfully registered new DataNode with id " + nodes);
        aliveNodes.add(nodes);
        return nodes++;
    }

    public void deRegisterDataNode(int node) {
        aliveNodes.remove(node);
        System.out.println("Successfully deregistered DataNode with id " + node);
    }

    // TODO redirect clients to the appropriate data node
    // Do we do this on a per-request basis or a per-session basis?
    // per-request is more resilient, but then consistency issues

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
