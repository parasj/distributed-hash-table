import java.math.BigInteger;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class DataNode implements RemoteDataNode {
    private int id;
    private TreeMap<Integer, String> aliveNodes;
    private Map<BigInteger, Object> map;

    private DataNode(int id) {
        this.id = id;
        map = new HashMap<>();
    }

    @Override
    public int put(Context ctx, BigInteger key, Object value) throws RemoteException {
        // TODO replicate
        // TODO update vector clocks
        map.put(key, value);
        return 0;
    }

    @Override
    public Object get(Context ctx, BigInteger key) throws RemoteException {
        // TODO contact replica nodes
        // TODO reconcile differences
        return map.get(key);
    }

    @Override
    public void updateMembership(TreeMap<Integer, String> aliveNodes) {
        this.aliveNodes = aliveNodes;
    }

    public static void main(String[] args) {
        String host = (args.length < 1) ? null : args[0];
        try {
            // Register the DataNode with the Manager
            Registry registry = LocateRegistry.getRegistry(host);
            RemoteManager managerStub = (RemoteManager)
                    registry.lookup("RemoteManager");
            DataNode node = new DataNode(managerStub.registerDataNode());
            System.out.println("Successfully initialized new DataNode with id " + node.id);

            // The DataNode has been registered with the Manager
            // We now register it with the local registry so that
            // it can accept connections from clients
            RemoteDataNode stub = (RemoteDataNode)
                    UnicastRemoteObject.exportObject(node, 0);
            // Separate registry for data nodes, on the localhost of the
            // data node. Must add the id of the data node to avoid
            // polluting the registry.
            Registry localRegistry = LocateRegistry.getRegistry();
            localRegistry.bind("RemoteDataNode" + node.id, stub);
            System.out.println("Successfully registered the DataNode with the Registry");


            // Register a shutdown hook so that we de register from
            // the manager and the local registry when the DataNode is killed
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    managerStub.deRegisterDataNode(node.id);
                    System.out.println("Successfully deregistered from the Manager");
                    localRegistry.unbind("RemoteDataNode" + node.id);
                    System.out.println("Successfully unbound from the local Registry");
                } catch (Exception e) {
                    System.err.println("DataNode shutdown exception: " + e.toString());
                    e.printStackTrace();
                }
            }));

        } catch (ConnectException e) {
            System.err.println("Manager exception: Please first start the RMI Registry " +
                    "(run rmiregistry in the folder this class resides in)");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("DataNode exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
