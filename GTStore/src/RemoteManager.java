import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.TreeMap;

interface RemoteManager extends Remote {
    int registerDataNode() throws RemoteException;
    void deRegisterDataNode(int id) throws RemoteException;
    int registerClient() throws RemoteException;
    TreeMap<Integer, String> getAliveNodes() throws RemoteException;
    int getMaxDataNodes() throws RemoteException;
}
