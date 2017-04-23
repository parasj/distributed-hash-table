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
    private final static int REPLICATION_FACTOR = 3;
    // Minimum number of successful writes
    private final static int WRITE_FACTOR = 3;
    // Minimum number of successful reads
    private static int READ_FACTOR = 3;

    private DataNode(RemoteManager manager) {
        try {
            this.id = manager.registerDataNode();
            // aliveNodes should also contain the current
            // data node right now, since we just registered
            // it
            this.aliveNodes = manager.getAliveNodes();
        } catch (Exception e) {
            e.printStackTrace();
        }
        map = new HashMap<>();
    }

    @Override
    public Context put(Context ctx, BigInteger key, Object value) throws RemoteException {
        // TODO: Implement hinted handoff

        map.put(key, value);
        System.out.println("Storing (" + key + "," + value + ") in node " + id);

        // Only update the clock if it is the master copy
        if (ctx.coordinator ) {
            // Check to see if the data item already has clocks
            if(ctx.clocks.containsKey(key)) {
                // Check to see if a previous get found multiple irreconcilable leaves
                if (ctx.notReconciled.contains(key)) {
                    // Only need to set the version number properly here,
                    // the replication code below will take care of updating
                    // replica nodes appropriately.
                    int maxVersion = ctx.clocks.get(key).values()
                            .stream().max(Integer::compareTo).get();
                    ctx.clocks.get(key).clear();
                    ctx.clocks.get(key).put(id, maxVersion);
                    // Done reconciling
                    ctx.notReconciled.remove(key);
                } else {
                    // Already has clocks, only one latest version leaf, so update the clock
                    TreeMap<Integer, Integer> clock = ctx.clocks.get(key);
                    if (clock.containsKey(id))
                        clock.put(id, clock.get(id) + 1); // update the version
                    else
                        clock.put(id, 1);
                }
            } else {
                // Doesn't  have associated clock, add a new clock
                // and add a version timestamp to it
                TreeMap<Integer, Integer> clock = new TreeMap<>();
                clock.put(id, 1);
                ctx.clocks.put(key, clock);
            }

            // Is the coordinator node for the put; must initiate replication
            // We set this to true here so that the nested put calls won't
            // also initiate replicating, resulting in an infinite loop.
            int replicas = 1;
            ctx.coordinator = false;
            Map.Entry<Integer, String> host = aliveNodes.ceilingEntry(id + 1);
            host = host != null ? host : aliveNodes.firstEntry();
            for(int i = 1; i < REPLICATION_FACTOR; i++) {
                try {
                    Registry registry = LocateRegistry.getRegistry(host.getValue());
                    RemoteDataNode node = (RemoteDataNode)
                            registry.lookup("RemoteDataNode" + host.getKey());
                    ctx = node.put(ctx, key, value);
                    host = aliveNodes.ceilingEntry(host.getKey() + 1);
                    host = host != null ? host : aliveNodes.firstEntry();
                    replicas++;
                } catch (Exception e) {
                    System.err.println("Couldn't access node " + host.getKey()
                            + " on host " + host.getValue());
                    e.printStackTrace();
                }
            }
            ctx.coordinator = true;
            System.out.println("Successfully stored " + replicas + " replicas/" + WRITE_FACTOR);
            ctx.success = replicas == WRITE_FACTOR;
        }

        return ctx;
    }

    @Override
    public Object get(Context ctx, BigInteger key) throws RemoteException {
        // TODO contact replica nodes clock.get(id)
        // TODO reconcile differences
        return map.get(key);
    }

    @Override
    public void updateMembership(TreeMap<Integer, String> aliveNodes) {
        System.out.println("New membership set: " + aliveNodes.toString()
                + " of size " + aliveNodes.size());
        this.aliveNodes = aliveNodes;
    }

    public static void main(String[] args) {
        String host = (args.length < 1) ? null : args[0];
        try {
            // Register the DataNode with the Manager
            Registry registry = LocateRegistry.getRegistry(host);
            RemoteManager managerStub = (RemoteManager)
                    registry.lookup("RemoteManager");
            DataNode node = new DataNode(managerStub);
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
