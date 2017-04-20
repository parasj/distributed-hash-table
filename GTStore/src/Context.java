import java.util.TreeMap;

public class Context {
    private int id;
    // Maps MD5 hash of keys to clocks
    // Each clock is a map of server ids to version numbers
    private TreeMap<String, TreeMap<Integer, Integer>> clocks;

    public Context(int id, TreeMap<String, TreeMap<Integer, Integer>> clocks) {
        this.id = id;
        this.clocks = clocks;
    }
}
