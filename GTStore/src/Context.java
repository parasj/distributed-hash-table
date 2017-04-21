import java.io.Serializable;
import java.math.BigInteger;
import java.util.TreeMap;

public class Context implements Serializable {
    public int id;
    // Maps MD5 hash of keys to clocks
    // Each clock is a map of server ids to version numbers
    public TreeMap<BigInteger, TreeMap<Integer, Integer>> clocks;
    public boolean success;
    public int replicas;

    public Context(int id, TreeMap<BigInteger, TreeMap<Integer, Integer>> clocks) {
        this.id = id;
        this.clocks = clocks;
    }
}
