import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

// TODO Fix up this messy GTKey thing, split the context
// out into its own class and make the function signature
// reflect the split.

public class Client {
    private int id;
    private int maxDataNodes;
    private TreeMap<Integer, String> aliveNodes;
    private static int REPLICATION_FACTOR = 3;

    public Client(String managerHost) {
        try {
            Registry registry = LocateRegistry.getRegistry(managerHost);
            RemoteManager managerStub = (RemoteManager)
                    registry.lookup("RemoteManager");
            Client client = managerStub.registerClient();
            this.id = client.id;
            this.aliveNodes = client.aliveNodes;

            if(aliveNodes.size() < 3)
                throw new Exception("Need at least "
                        + REPLICATION_FACTOR
                        + " DataNodes to ensure replication doesn't fail");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Client(int id, int maxDataNodes, TreeMap<Integer, String> aliveNodes) {
        this.id = id;
        this.maxDataNodes = maxDataNodes;
        this.aliveNodes = aliveNodes;
    }

    int put(Object key, Object value) {
        int pos = getNodePosition(key);
        // we want the node whose position is the smallest value
        // greater than the position of this key, or the first node
        Map.Entry<Integer, String> host = aliveNodes.ceilingEntry(pos);
        host = host != null ? host : aliveNodes.firstEntry();

        int successes = 0;

        while(successes < REPLICATION_FACTOR) {
            try {
                Registry registry = LocateRegistry.getRegistry(host.getValue());
                RemoteDataNode node = (RemoteDataNode)
                        registry.lookup("RemoteDataNode" + host.getKey());
                // TODO Update the vector clock
                if(node.put(new GTKey(id, key), value) == 0)
                    successes++;
                // successful, now move on to next node to replicate
                host = aliveNodes.ceilingEntry(host.getKey() + 1);
                host = host != null ? host : aliveNodes.firstEntry();
            } catch (Exception e) {
                // TODO actually handle the case where the node isn't found
                e.printStackTrace();
                return -1;
            }
        }
        return 0;
    }

    Object get(Object key) {
        // TODO Same deal as the put, basically read from multiple
        // nodes, then reconcile
        return null;
    }

    // MD5 hashes the key and returns the position in the ring
    // that corresponds to it.
    private int getNodePosition(Object obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.close();

            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(baos.toByteArray());

            return new BigInteger(1, m.digest())
                    .mod(BigInteger.valueOf(maxDataNodes))
                    .intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private class GTKey {
        int clientId;
        Object key;
        private TreeMap<Integer, Integer> clock;

        public GTKey(int clientId, Object key) {
            this.clientId = clientId;
            this.key = key;
            clock = new TreeMap<>();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            GTKey gtKey = (GTKey) o;

            if (clientId != gtKey.clientId) return false;
            return key != null ? key.equals(gtKey.key) : gtKey.key == null;
        }

        @Override
        public int hashCode() {
            int result = clientId;
            result = 31 * result + (key != null ? key.hashCode() : 0);
            return result;
        }
    }
}
