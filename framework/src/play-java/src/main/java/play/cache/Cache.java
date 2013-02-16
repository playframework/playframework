package play.cache;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;

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

  protected static int durationToExpiration(Duration duration) {
      double asSecs = duration.toUnit(TimeUnit.SECONDS);
      if (asSecs == Double.POSITIVE_INFINITY) return 0;     //return 0 (Infinite cache) for the case of positive Infinity
      else return (int)Math.max(asSecs, 1);                 //Return the time in seconds, returning times < 1s as the minimum specifiable time of one second
  }

  /**
   * Retrieve a value from the cache, or set it from a default Callable function.
   * 
   * @param key Item key.
   * @param block callable to set if key does not exist
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
   * Retrieve a value from the cache, or set it from a default Callable function.
   *
   * @param key Item key.
   * @param block callable to set if key does not exist
   * @param expiration expiration period a Duration, rounded to seconds;  Use Duration.Inf() for infinite cache
   * @return value
   */
  @SuppressWarnings("unchecked")
  public static <T> T getOrElse(String key, Callable<T> block, Duration expiration) throws Exception{
      return getOrElse(key,block,durationToExpiration(expiration));
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
    * Sets a value with expiration.
    *
    * @param expiration expiration as a Duration, rounded to seconds;  Use Duration.Inf() for infinite cache
    */
   public static void set(String key, Object value, Duration expiration) {
      set(key,value, durationToExpiration(expiration));
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