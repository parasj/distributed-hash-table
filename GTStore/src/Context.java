import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.TreeMap;

class Context implements Serializable {
    // Maps MD5 hash of keys to clocks
    // Each clock is a map of server ids to version numbers
    TreeMap<BigInteger, VectorClock> clocks;

    // Add a key to this set in the gets call if multiple leaf
    // data items are retrieved and automatic reconciliation is
    // not possible. Subsequent put calls to the key will treat
    // the new value as the canonical reconciled value and update
    // all data nodes correspondingly.
    HashSet<BigInteger> notReconciled;

    // Has the success value for the most recent put call
    boolean success;

    // Temporary variable used withing put calls to distinguish
    // between the coordinator node and replica nodes. Is usually
    // set to true; the coordinator node sets it to false before
    // contacting replica nodes.
    boolean coordinator;
}
