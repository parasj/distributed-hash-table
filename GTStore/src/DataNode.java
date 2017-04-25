import clock.ConflictSet;
import clock.VectorClock;
import clock.VersionedValue;
import server.Context;
import server.RemoteDataNode;
import server.RemoteManager;

import java.math.BigInteger;
import java.rmi.AlreadyBoundException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class DataNode implements RemoteDataNode {
    private final static int REPLICATION_FACTOR = 5;
    private final static int WRITE_FACTOR = 3; // Minimum number of successful writes
    private static int READ_FACTOR = 3; // Minimum number of successful reads

    private int id; // DataNode ID
    private TreeMap<Integer, String> aliveNodes; // current view of alive nodes
    private Map<BigInteger, Object> map; // backing data-store for DataNode
    private Map<Integer, Map<BigInteger, Object>> hintedMap; // backing data-store for DataNode
    private TreeMap<BigInteger, VectorClock> clocks; // local clocks per key

    private DataNode(RemoteManager manager) {
        try {
            this.id = manager.registerDataNode();
            // aliveNodes should also contain the current
            // data node right now, since we just registered
            // it
            this.aliveNodes = manager.getAliveNodes();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        map = new HashMap<>();
        hintedMap = new HashMap<>();
        clocks = new TreeMap<>();
    }

    public static void main(String[] args) {
        String host = (args.length < 1) ? null : args[0];
        try {
            // Register the DataNode with the Manager
            Registry registry = LocateRegistry.getRegistry(host);
            RemoteManager managerStub = (RemoteManager)
                    registry.lookup("server.RemoteManager");
            DataNode node = new DataNode(managerStub);
            System.out.println("Successfully initialized new DataNode with id " + node.id);

            // The DataNode has been registered with the Manager
            // We now register it with the local registry so that
            // it can accept connections from clients
            RemoteDataNode stub = (RemoteDataNode) UnicastRemoteObject.exportObject(node, 0);
            // Separate registry for data nodes, on the localhost of the
            // data node. Must add the id of the data node to avoid
            // polluting the registry.
            Registry localRegistry = LocateRegistry.getRegistry();
            localRegistry.bind("server.RemoteDataNode" + node.id, stub);
            System.out.println("Successfully registered the DataNode with the Registry");


            // Register a shutdown hook so that we de register from
            // the manager and the local registry when the DataNode is killed
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    managerStub.deRegisterDataNode(node.id);
                    System.out.println("Successfully deregistered from the Manager");
                    localRegistry.unbind("server.RemoteDataNode" + node.id);
                    System.out.println("Successfully unbound from the local Registry");
                } catch (RemoteException | NotBoundException e) {
                    System.err.println("DataNode shutdown exception: " + e.toString());
                    e.printStackTrace();
                }
            }));

        } catch (ConnectException e) {
            System.err.println("Manager exception: Please first start the RMI Registry " +
                    "(run rmiregistry in the folder this class resides in)");
            e.printStackTrace();
        } catch (RemoteException | AlreadyBoundException | NotBoundException e) {
            System.err.println("DataNode exception: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public Context put(Context ctx, BigInteger key, Object value) throws RemoteException {
        if(ctx.hinted) {
            System.err.println("Storing " + key + " in the hinted map");
            if(hintedMap.containsKey(ctx.coordinatorID))
                hintedMap.get(ctx.coordinatorID).put(key, value);
            else {
                Map<BigInteger, Object> tempMap = new HashMap<>();
                tempMap.put(key, value);
                hintedMap.put(ctx.coordinatorID, tempMap);
            }
            return ctx;
        } else {
//            System.err.println("Storing " + key + " in the regular map");
            map.put(key, value);
        }

        VectorClock clock = clocks.getOrDefault(key, new VectorClock());
        clock.increment(id);
        clock.merge(ctx.clock);
        ctx.clock.merge(clock);
        clocks.put(key, clock);

        if (ctx.coordinator) {
            // Is the coordinator node for the put; must initiate replication
            // We set this to true here so that the nested put calls won't
            // also initiate replicating, resulting in an infinite loop.
            int replicas = 1;
            ctx.coordinator = false;
            Map.Entry<Integer, String> host = aliveNodes.ceilingEntry(id + 1);
            host = host != null ? host : aliveNodes.firstEntry();
            for (int i = 1; i < REPLICATION_FACTOR && host.getKey() != id; i++) {
                try {
                    Registry registry = LocateRegistry.getRegistry(host.getValue());
                    RemoteDataNode node = (RemoteDataNode) registry.lookup("server.RemoteDataNode" + host.getKey());
                    ctx = node.put(ctx, key, value);
                    replicas++;
                } catch (Exception e) {
//                    System.err.printf("Couldn't access node %d on host %s\n", host.getKey(), host.getValue());
//                    e.printStackTrace();
                }
                host = aliveNodes.ceilingEntry(host.getKey() + 1);
                host = host != null ? host : aliveNodes.firstEntry();
            }

            // If not enough replicas, we try hinted-handoff
            ctx.hinted = true;
            while ((replicas < REPLICATION_FACTOR) && (host.getKey() != id)) {
                try {
                    Registry registry = LocateRegistry.getRegistry(host.getValue());
                    RemoteDataNode node = (RemoteDataNode) registry.lookup("server.RemoteDataNode" + host.getKey());
                    ctx = node.put(ctx, key, value);
                    replicas++;
                } catch (NotBoundException e) {
                    System.err.printf("Couldn't access node %d on host %s\n", host.getKey(), host.getValue());
//                    e.printStackTrace();
                }
                host = aliveNodes.ceilingEntry(host.getKey() + 1);
                host = host != null ? host : aliveNodes.firstEntry();
            }
            ctx.hinted = false;

            ctx.coordinator = true;
            ctx.success = replicas >= WRITE_FACTOR;
//            System.err.println("Got " + replicas + " writes successfully");
        }

        return ctx;
    }

    @Override
    public ConflictSet<Object> get(Context ctx, BigInteger key) throws RemoteException {
        int replicas = 0;

        ConflictSet<Object> cs = new ConflictSet<>();
        if (map.containsKey(key)) {
            cs.add(new VersionedValue<>(clocks.getOrDefault(key, new VectorClock()), map.get(key)));
            replicas++;
        }

        if (ctx.coordinator) {
            Map.Entry<Integer, String> host = aliveNodes.ceilingEntry(id + 1);
            host = host != null ? host : aliveNodes.firstEntry();
            for (int i = 1; i < REPLICATION_FACTOR && host.getKey() != id; i++) {
                try {
                    Registry registry = LocateRegistry.getRegistry(host.getValue());
                    RemoteDataNode node = (RemoteDataNode) registry.lookup("server.RemoteDataNode" + host.getKey());
                    ctx.coordinator = false;
                    ConflictSet<Object> remoteCS = node.get(ctx, key);
                    cs.addAll(remoteCS);
                    replicas++;
                } catch (NotBoundException e) {
                    System.err.printf("Couldn't access node %d on host %s\n", host.getKey(), host.getValue());
//                    e.printStackTrace();
                }
                host = aliveNodes.ceilingEntry(host.getKey() + 1);
                host = host != null ? host : aliveNodes.firstEntry();
            }
        }

        cs.setQuorumReached(replicas >= READ_FACTOR);
        cs.reconcile();
        return cs;
    }

    @Override
    public void updateMembership(TreeMap<Integer, String> aliveNodes) throws RemoteException {
        Set<Integer> newMembers = new TreeSet<>(aliveNodes.keySet());
        newMembers.removeAll(this.aliveNodes.keySet());

        for(int i : newMembers) {
            if (hintedMap.containsKey(i)) {
                Map<BigInteger, Object> map = hintedMap.get(i);
                for(BigInteger key : map.keySet()) {
                    Context ctx = new Context(clocks.get(key));
                    ctx.coordinator = true;
                    put(ctx, key, map.get(key));
                    ctx.coordinator = false;
                }
            }
        }

        System.out.println("New membership set: " + aliveNodes.toString()
                + " of size " + aliveNodes.size());
        this.aliveNodes = aliveNodes;
    }

    public boolean aliveCheck() {
        return true;
    }
}
