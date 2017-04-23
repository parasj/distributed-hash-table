import java.io.Serializable;

/**
 * GTStore
 */
public class VersionedValue implements Serializable {
    private final VectorClock clock;
    private final Object value;

    public VersionedValue(VectorClock clock, Object value) {
        this.clock = clock;
        this.value = value;
    }

    public VersionedValue(Object value) {
        this(new VectorClock(), value);
    }

    public VectorClock getClock() {
        return clock;
    }

    public Object getValue() {
        return value;
    }
}
