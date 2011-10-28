package play.cache;

import java.util.Map;

/**
 * A cache implementation.
 * expiration is specified in seconds
 * @see play.cache.Cache
 */
public interface CacheAPI {

    public void add(String key, Object value, int expiration);

    public boolean safeAdd(String key, Object value, int expiration);

    public void set(String key, Object value, int expiration);

    public boolean safeSet(String key, Object value, int expiration);

    public void replace(String key, Object value, int expiration);

    public boolean safeReplace(String key, Object value, int expiration);

    public Object get(String key);

    public Map<String, Object> get(String[] keys);

    public long incr(String key, int by);

    public long decr(String key, int by);

    public void clear();

    public void delete(String key);

    public boolean safeDelete(String key);

    public void stop();
}
