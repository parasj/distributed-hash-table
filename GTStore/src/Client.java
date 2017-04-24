import clock.ConflictSet;
import clock.VersionedValue;
import server.Context;
import server.RemoteDataNode;
import server.RemoteManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public class Client<K, V> {
    private int id;
    private int maxDataNodes;
    private TreeMap<Integer, String> aliveNodes;
    private Context ctx;
    private Function<List<VersionedValue<V>>, VersionedValue<V>> conflictResolver;

    public Client(String managerHost, Function<List<VersionedValue<V>>, VersionedValue<V>> conflictResolver) {
        try {
            Registry registry = LocateRegistry.getRegistry(managerHost);
            RemoteManager managerStub = (RemoteManager) registry.lookup("server.RemoteManager");
            this.id = managerStub.registerClient();
            this.aliveNodes = managerStub.getAliveNodes();
            this.maxDataNodes = managerStub.getMaxDataNodes();

            if (aliveNodes.size() < 3) {
                System.err.println("Need at least 3 DataNodes to " +
                        "ensure replication doesn't fail");
                System.exit(0);
            }
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
        ctx = new Context();
        this.conflictResolver = conflictResolver;
    }

    public Client(String managerHost) {
        this(managerHost,
                (cs) -> cs.stream()
                    .max((s1, s2) -> Long.compare(s1.getClock().getLastUpdate(), s2.getClock().getLastUpdate()))
                    .orElse(null));
    }

    /**
     * Put an item into the KV store. This function will return
     * successfully if at least WRITE_FACTOR replicas were written to
     *
     * @param key   Hashable key to store under
     * @param value Value to store
     * @return Whether the put request was successful
     */
    public boolean put(K key, V value) {
        BigInteger keyHash = getKeyMD5(id, key);
        int pos = keyHash.mod(BigInteger.valueOf(maxDataNodes)).intValue();

        // we want the node whose position is the smallest value
        // greater than the position of this key, or the first node
        Map.Entry<Integer, String> host = aliveNodes.ceilingEntry(pos);
        host = host != null ? host : aliveNodes.firstEntry();
        int firstHost = host.getKey();

        ctx.coordinator = true;
        do {
            try {
                Registry registry = LocateRegistry.getRegistry(host.getValue());
                RemoteDataNode node = (RemoteDataNode) registry.lookup("server.RemoteDataNode" + host.getKey());
                ctx = node.put(ctx, keyHash, value);
                return ctx.success;
            } catch (RemoteException | NotBoundException e) {
                // move on to the next host
                host = aliveNodes.ceilingEntry(host.getKey() + 1);
                host = host != null ? host : aliveNodes.firstEntry();
                e.printStackTrace();
            }
            // do this until we wrap back around, in the case of failure
        } while (host.getKey() != firstHost);
        return false;
    }

    /**
     * Get an object from the KV store. This function will return
     * the object if it is conflict free and READ_FACTOR replicas
     * were read from.
     *
     * @param key Hashable key to retrieve
     * @return stored value or null
     */
    public V get(K key) {
        BigInteger keyHash = getKeyMD5(id, key);
        int pos = keyHash.mod(BigInteger.valueOf(maxDataNodes)).intValue();

        // we want the node whose position is the smallest value
        // greater than the position of this key, or the first node
        Map.Entry<Integer, String> host = aliveNodes.ceilingEntry(pos);
        host = host != null ? host : aliveNodes.firstEntry();
        int firstHost = host.getKey();

        ConflictSet<V> cs = null;

        ctx.coordinator = true;
        do {
            try {
                Registry registry = LocateRegistry.getRegistry(host.getValue());
                RemoteDataNode node = (RemoteDataNode) registry.lookup("server.RemoteDataNode" + host.getKey());
                cs = (ConflictSet<V>) node.get(ctx, keyHash);
            } catch (RemoteException | NotBoundException e) {
                host = aliveNodes.ceilingEntry(host.getKey() + 1);
                host = host != null ? host : aliveNodes.firstEntry();
                e.printStackTrace();
            }
        } while (cs == null && host.getKey() != firstHost);

        if (cs != null && cs.isQuorumReached() && cs.getValues().size() > 0) {
            VersionedValue<V> result = conflictResolver.apply(cs.getValues());
            cs.reconcile();
            ctx.clocks.get(keyHash).merge(result.getClock());
            if (cs.getValues().size() > 1)
                put(key, result.getValue());
            return result.getValue();
        }

        return null;
    }

    private BigInteger getKeyMD5(int id, K obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(id);
            oos.writeObject(obj);
            oos.close();

            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(baos.toByteArray());

            return new BigInteger(1, m.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return BigInteger.ZERO;
        }
    }
}
