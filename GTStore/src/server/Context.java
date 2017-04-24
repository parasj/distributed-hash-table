package server;

import clock.VectorClock;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.TreeMap;

public class Context implements Serializable {
    private static final long serialVersionUID = 1L;

    // Maps MD5 hash of keys to clocks
    // Each clock is a map of server ids to version numbers
    public TreeMap<BigInteger, VectorClock> clocks;

    // Add a key to this set in the gets call if multiple leaf
    // data items are retrieved and automatic reconciliation is
    // not possible. Subsequent put calls to the key will treat
    // the new value as the canonical reconciled value and update
    // all data nodes correspondingly.
    public HashSet<BigInteger> notReconciled;

    // Has the success value for the most recent put call
    public boolean success;

    // Temporary variable used withing put calls to distinguish
    // between the coordinator node and replica nodes. Is usually
    // set to true; the coordinator node sets it to false before
    // contacting replica nodes.
    public boolean coordinator;

    public Context(TreeMap<BigInteger, VectorClock> clocks, HashSet<BigInteger> notReconciled) {
        this.clocks = clocks;
        this.notReconciled = notReconciled;
    }

    public Context() {
        this(new TreeMap<>(), new HashSet<>());
    }
}
