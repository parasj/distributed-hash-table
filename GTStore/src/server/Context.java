package server;

import clock.VectorClock;

import java.io.Serializable;

public class Context implements Serializable {
    private static final long serialVersionUID = 1L;

    // Maps MD5 hash of keys to clocks
    // Each clock is a map of server ids to version numbers
    public VectorClock clock;

    // Has the success value for the most recent put call
    public boolean success;

    // Temporary variable used within put calls to distinguish
    // between the coordinator node and replica nodes. Is usually
    // set to true; the coordinator node sets it to false before
    // contacting replica nodes.
    public boolean coordinator;

    public boolean hinted;
    public int coordinatorID;

    public Context(VectorClock clock)  {
        this.clock = clock;
    }

    public Context() {
        this(new VectorClock());
    }
}
