package play.cache;

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
   * Sets a value with expiration.
   * 
   * @param expiration expiration in seconds
   */
  public static void set(String key, Object value, int expiration) {
      play.api.cache.Cache.set(key,value,expiration, play.api.Play.unsafeApplication());
  }

}
