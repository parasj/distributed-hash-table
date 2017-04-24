package server;

import clock.ConflictSet;

import java.math.BigInteger;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.TreeMap;

public interface RemoteDataNode extends Remote {
    public Context put(Context ctx, BigInteger key, Object value) throws RemoteException;

    public ConflictSet<Object> get(Context ctx, BigInteger key) throws RemoteException;

    public void updateMembership(TreeMap<Integer, String> aliveNodes) throws RemoteException;
}
