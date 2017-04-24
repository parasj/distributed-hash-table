package clock;

import java.io.Serializable;

/**
 * GTStore
 */
public class VersionedValue<T> implements Serializable {
    private final VectorClock clock;
    private final T value;

    public VersionedValue(VectorClock clock, T value) {
        this.clock = clock;
        this.value = value;
    }

    public VectorClock getClock() {
        return clock;
    }

    public T getValue() {
        return value;
    }
}
