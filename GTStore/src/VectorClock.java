import java.io.Serializable;
import java.util.*;

/**
 * GTStore
 */
public class VectorClock implements Serializable {
    private final TreeMap<Integer, Integer> clock;
    private long lastUpdate;

    public VectorClock() {
        clock = new TreeMap<>();
        lastUpdate = System.nanoTime();
    }

    public TreeMap<Integer, Integer> getClock() {
        return clock;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    // todo check that vector clock is not capped
    public void increment(int node) {
        lastUpdate = System.nanoTime();
        clock.put(node, clock.getOrDefault(node, 0) + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VectorClock that = (VectorClock) o;
        return Objects.equals(clock, that.clock);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clock);
    }

    public void merge(VectorClock other) {
        lastUpdate = System.nanoTime();
        other.clock.forEach((k, v) -> clock.put(k, Math.max(clock.getOrDefault(k, -1), v)));
    }

    public static VectorDelta compare(VectorClock a, VectorClock b) {
        Set<Integer> intersection = new HashSet<>(a.clock.keySet());
        intersection.retainAll(b.clock.keySet()); // intersect

        boolean v1HasElementsGreaterThanV2 =
                a.clock.entrySet().stream()
                        .filter(e -> e.getValue() > b.clock.getOrDefault(e.getKey(), -1))
                        .count() > 0;
        boolean v2HasElementsGreaterThanV1 = b.clock.entrySet().stream().filter(e -> e.getValue() > a.clock.getOrDefault(e.getKey(), -1)).count() > 0;

        boolean aBigger = (a.clock.size() > intersection.size()) || v1HasElementsGreaterThanV2; // v1 > v2
        boolean bBigger = (b.clock.size() > intersection.size()) || v2HasElementsGreaterThanV1; // v2 < v1

        if (aBigger && !bBigger) // a > b
            return VectorDelta.GREATER_THAN;
        else if (bBigger && !aBigger) // a < b
            return VectorDelta.LESS_THAN;
        else if (aBigger && bBigger) // a > b and b > a
            return VectorDelta.CONFLICT;
        else if (!aBigger && !bBigger) // a == b
            return VectorDelta.EQUAL;
        return VectorDelta.CONFLICT;
    }

    public void clear() {
        lastUpdate = System.nanoTime();
        clock.clear();
    }

    @Override
    public VectorClock clone() {
        VectorClock newClock = new VectorClock();
        newClock.merge(this);
        newClock.lastUpdate = this.lastUpdate;
        return newClock;
    }
}
