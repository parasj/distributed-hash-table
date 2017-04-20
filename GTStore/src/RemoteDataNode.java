import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteDataNode extends Remote {
    int put(Object key, Object value) throws RemoteException;
    Object get(Object key) throws RemoteException;
}
