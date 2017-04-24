package clock;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GTStore
 */
public class ConflictSet<T> implements Serializable {
    private List<VersionedValue<T>> values;
    private boolean quorumReached; // was this conflict set the result of a quorum?

    public ConflictSet(List<VersionedValue<T>> values) {
        this.values = new ArrayList<>();
        this.values.addAll(values);
        this.reconcile();
        this.quorumReached = false;
    }

    public ConflictSet() {
        this(new ArrayList<>());
    }

    public List<VersionedValue<T>> getValues() {
        return values;
    }

    /**
     * Will remove any redundant objects
     */
    public void reconcile() {
        List<VersionedValue<T>> posetMax = values.stream()
                .filter(a -> values.stream()
                        .filter(b -> a != b)
                        .map(b -> VectorClock.compare(a.getClock(), b.getClock()))
                        .allMatch(d -> d == VectorDelta.CONFLICT || d == VectorDelta.GREATER_THAN))
                .collect(Collectors.toList());
        values.clear();
        values.addAll(posetMax);
    }

    public void addAll(ConflictSet<T> cs) {
        this.values.addAll(cs.values);
        this.reconcile();
    }

    public void add(VersionedValue<T> v) {
        this.values.add(v);
        this.reconcile();
    }

    public boolean isQuorumReached() {
        return quorumReached;
    }

    public void setQuorumReached(boolean quorumReached) {
        this.quorumReached = quorumReached;
    }
}
