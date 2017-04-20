import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteDataNode extends Remote {
    int put(String key, Object value) throws RemoteException;
    Object get(String key) throws RemoteException;
}
