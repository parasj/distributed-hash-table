import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.TreeMap;

public interface RemoteManager extends Remote {
    int registerDataNode() throws RemoteException, NotBoundException;
    void deRegisterDataNode(int id) throws RemoteException;
    int registerClient() throws RemoteException;
    TreeMap<Integer, String> getAliveNodes() throws RemoteException;
    int getMaxDataNodes() throws RemoteException;
}
