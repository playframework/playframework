package play.cache;

/**
 * provides an access point for Play's cache service
 */
public class Cache {

  /**
   * @key 
   * retrieve an object by key
   * @return object
   */
  public static Object get(String key) {
    return play.api.cache.Cache.getAsJava(key,play.api.Play.unsafeApplication());
  }

  /**
   * @key 
   * retrieve an object by key
   * @return generic type T
   */
  @SuppressWarnings("unchecked")
  public static <T> T get(String key, Class<T> clazz) {
   return (T)  play.api.cache.Cache.getAsJava(key,play.api.Play.unsafeApplication());
  }

  /**
   * method provides multi value access to play's cache store
   * @keys  
   * retrieve an object by keys
   * @return a key value list of cache keys and corresponding values
   */
  public static java.util.Map<String,Object> get(String... keys) {
    return play.api.cache.Cache.getAsJava(keys, play.api.Play.unsafeApplication());
  }

  /**
   * sets a value with expiration
   * @key
   * @value 
   * @expiration in seconds
   */
  public static void set(String key, Object value, int expiration) {
    play.api.cache.Cache.set(key,value,expiration, play.api.Play.unsafeApplication());
  }

  /**
   * sets a value with expiration
   * @key
   * @value 
   * expiration is set to 1800 sec or 30 min by default
   */
  public static void set(String key, Object value) {
    play.api.cache.Cache.set(key,value, 1800,play.api.Play.unsafeApplication());
  }

}
