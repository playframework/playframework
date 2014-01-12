package play.cache;

import java.util.concurrent.Callable;

/**
 * Provides an access point for Play's cache service.
 */
public class Cache {

  /**
   * Retrieves an object by key.
   *
   * @return object
   */
  public static Object get(String key) {
      return play.libs.Scala.orNull(play.api.cache.Cache.get(key,play.api.Play.unsafeApplication()));
  }

  /**
   * Retrieve a value from the cache, or set it from a default Callable function.
   * 
   * @param key Item key.
   * @param block block returning value to set if key does not exist
   * @param expiration expiration period in seconds.
   * @return value 
   */
  @SuppressWarnings("unchecked")
  public static <T> T getOrElse(String key, Callable<T> block, int expiration) throws Exception{
     Object r = play.libs.Scala.orNull(play.api.cache.Cache.get(key,play.api.Play.unsafeApplication()));
     if (r == null) {
         T value = block.call();
         set(key,value,expiration);
         return value;
     } else return (T)r;
     
  }
  
  /**
   * Sets a value with expiration.
   * 
   * @param expiration expiration in seconds
   */
  public static void set(String key, Object value, int expiration) {
      play.api.cache.Cache.set(key,value,expiration, play.api.Play.unsafeApplication());
  }

  /**
   * Sets a value without expiration.
   *
   */
  public static void set(String key, Object value) {
      play.api.cache.Cache.set(key,value, 0, play.api.Play.unsafeApplication());
  }

  public static void remove(String key) {
      play.api.cache.Cache.remove(key, play.api.Play.unsafeApplication());
  }
}