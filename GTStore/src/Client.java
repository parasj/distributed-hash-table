import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

public class Client {
    private int id;
    private int maxDataNodes;
    private TreeMap<Integer, String> aliveNodes;
    private static int REPLICATION_FACTOR = 3;
    private Context ctx;

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
        ctx = new Context(id, new TreeMap<>());
    }

    public Client(int id, int maxDataNodes, TreeMap<Integer, String> aliveNodes) {
        this.id = id;
        this.maxDataNodes = maxDataNodes;
        this.aliveNodes = aliveNodes;
    }

    int put(Object key, Object value) {
        BigInteger keyHash = getKeyMD5(key);
        int pos = keyHash.mod(BigInteger.valueOf(maxDataNodes)).intValue();
        // we want the node whose position is the smallest value
        // greater than the position of this key, or the first node
        Map.Entry<Integer, String> host = aliveNodes.ceilingEntry(pos);
        host = host != null ? host : aliveNodes.firstEntry();
        int firstHost = host.getKey();

        do{
            try {
                Registry registry = LocateRegistry.getRegistry(host.getValue());
                RemoteDataNode node = (RemoteDataNode)
                        registry.lookup("RemoteDataNode" + host.getKey());
                if(node.put(ctx, keyHash, value) == 0)
                    return 0;
                else
                    return -1;
            } catch (Exception e) {
                // move on to the next host
                host = aliveNodes.ceilingEntry(host.getKey() + 1);
                host = host != null ? host : aliveNodes.firstEntry();
                e.printStackTrace();
            }
            // do this until we wrap back around, in the case of failure
        } while(host.getKey() != firstHost);
        return -1;
    }

    Object get(Object key) {
        return null;
    }


    private BigInteger getKeyMD5(Object obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.close();

            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(baos.toByteArray());

            return new BigInteger(1, m.digest());
        } catch (Exception e) {
            return BigInteger.ZERO;
        }
    }

}
