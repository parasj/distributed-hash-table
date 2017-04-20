import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteManager extends Remote {
    int registerDataNode() throws RemoteException, NotBoundException;
    void deRegisterDataNode(int id) throws RemoteException;
    Client registerClient() throws RemoteException;
}
