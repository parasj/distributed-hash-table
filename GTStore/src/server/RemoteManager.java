package server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.TreeMap;

public interface RemoteManager extends Remote {
    public int registerDataNode() throws RemoteException;
    public int registerDataNode(int id) throws RemoteException;

    public void deRegisterDataNode(int id) throws RemoteException;

    public int registerClient() throws RemoteException;

    public TreeMap<Integer, String> getAliveNodes() throws RemoteException;

    public int getMaxDataNodes() throws RemoteException;
}
