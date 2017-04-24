package clock;

import java.util.List;
import java.util.function.Function;

/**
 * GTStore
 */
public class ConflictResolutionStrategy {
    public static final Function<List<VersionedValue>, VersionedValue> LAST_UPDATE =
            (cs) -> cs.stream()
                    .max((s1, s2) -> Long.compare(s1.getClock().getLastUpdate(), s2.getClock().getLastUpdate()))
                    .orElse(null);

    public static final Function<List<VersionedValue>, VersionedValue> FIRST =
            (cs) -> cs.stream()
                    .findFirst()
                    .orElse(null);
}
