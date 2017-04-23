import java.math.BigInteger;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.TreeMap;

interface RemoteDataNode extends Remote {
    Context put(Context ctx, BigInteger key, Object value) throws RemoteException;

    Object get(Context ctx, BigInteger key) throws RemoteException;

    void updateMembership(TreeMap<Integer, String> aliveNodes) throws RemoteException;
}
