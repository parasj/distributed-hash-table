import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteManager extends Remote {
    int registerDataNode() throws RemoteException;
    void deRegisterDataNode(int id) throws RemoteException;
    Client registerClient() throws RemoteException;
}
