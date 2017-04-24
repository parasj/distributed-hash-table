/**
 * GTStore
 */
public interface Client {
    /**
     * Put an item into the KV store. This function will return
     * successfully if at least WRITE_FACTOR replicas were written to
     *
     * @param key   Hashable key to store under
     * @param value Value to store
     * @return Whether the put request was successful
     */
    boolean put(Object key, Object value);

    /**
     * Get an object from the KV store. This function will return
     * the object if it is conflict free and READ_FACTOR replicas
     * were read from.
     *
     * @param key Hashable key to retrieve
     * @return stored value or null
     */
    Object get(Object key);
}
