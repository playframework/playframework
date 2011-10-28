package play.cache;

import akka.dispatch.Future;
public class Cache {
   /** 
     * Convenient clazz to get a value a class type;
     * @param <T> The needed type
     * @param key The element key
     * @param clazz The type class
     * @return The element value or null
     */
    @SuppressWarnings("unchecked")
  public static <T> T get(String key, Class<T> clazz) {
       return (T) play.api.cache.Cache.getAsJava(key,play.api.Play.unsafeApplication());
  }  

  public static java.util.Map<String,Object> get(String... keys) {
       return play.api.cache.Cache.getAsJava(keys,play.api.Play.unsafeApplication());
  }

  @SuppressWarnings("unchecked")
  public static <T> Future<T> getAsync(String key, String waitForEvaluation, Class<T> clazz) {
     return (Future<T>) play.api.cache.Cache.getAsJava(key, waitForEvaluation, play.api.Play.unsafeApplication());
  }  

   /** 
   * Delete an element from the cache.
   * @param key The element key
   */
  public static void delete(String key) {
     play.api.cache.Cache.delete(key,play.api.Play.unsafeApplication());
  }

     /**
   * Add an element only if it doesn't exist.
   * @param key Element key
   * @param value Element value
   * @param expiration Ex: 10s, 3mn, 8h
   */
  public static void add(String key, Object value, String expiration) {
    play.api.cache.Cache.add(key,value,expiration,play.api.Play.unsafeApplication());
  }


   /**
   * Add an element only if it doesn't exist and store it indefinitely.
   * @param key Element key
   * @param value Element value
   */
  public static void add(String key, Object value) {
    play.api.cache.Cache.add(key,value,play.api.Play.unsafeApplication());
  }

      /**
   * Set an element.
   * @param key Element key
   * @param value Element value
   * @param expiration Ex: 10s, 3mn, 8h
   */
  public static void set(String key, Object value, String expiration) {
     play.api.cache.Cache.set(key,value,expiration,play.api.Play.unsafeApplication());
  }


  /**
   * Set an element and store it indefinitely.
   * @param key Element key
   * @param value Element value
   */
  public static void set(String key, Object value) {
     play.api.cache.Cache.set(key,value,play.api.Play.unsafeApplication());
  }



      /**
   * Replace an element only if it already exists.
   * @param key Element key
   * @param value Element value
   * @param expiration Ex: 10s, 3mn, 8h
   */
  public static void replace(String key, Object value, String expiration) {
     play.api.cache.Cache.replace(key,value,expiration,play.api.Play.unsafeApplication());
  }


      /**
   * Replace an element only if it already exists and store it indefinitely.
   * @param key Element key
   * @param value Element value
   */
  public static void replace(String key, Object value)  {
    play.api.cache.Cache.replace(key,value,play.api.Play.unsafeApplication());
  }

      /**
   * Increment the element value (must be a Number).
   * @param key Element key·
   * @param by The incr value
   * @return The new value
   */
  public static long incr(String key, int by) {
    return play.api.cache.Cache.incr(key,by,play.api.Play.unsafeApplication());
  }


     /**
   * Increment the element value (must be a Number) by 1.
   * @param key Element key·
   * @return The new value
   */
  public static long incr(String key) {
    return play.api.cache.Cache.incr(key,play.api.Play.unsafeApplication());
  }

      /**
   * Decrement the element value (must be a Number).
   * @param key Element key·
   * @param by The decr value
   * @return The new value
   */
  public static long decr(String key, int by) {
     return play.api.cache.Cache.decr(key,by,play.api.Play.unsafeApplication());
  }

      /**
   * Decrement the element value (must be a Number) by 1.
   * @param key Element key·
   * @return The new value
   */
  public static long decr(String key) {
     return play.api.cache.Cache.decr(key,play.api.Play.unsafeApplication());
  } 


      /**
   * Clear all data from cache.
   */
  public static void clear() {
    play.api.cache.Cache.stop(play.api.Play.unsafeApplication());
  }


     /**
   * Stop the cache system.
   */
  public static void stop() {
       play.api.cache.Cache.stop(play.api.Play.unsafeApplication());
  }

  /**
   * Utility that check that an object is serializable.
   */
  static void checkSerializable(Object value) {
    play.api.cache.Cache.checkSerializable(play.api.Play.unsafeApplication());
  } 
    
  @Deprecated
  public static boolean safeSet(String key, Object value, String expiration) {
    return play.api.cache.Cache.safeSet(key, value, expiration, play.api.Play.unsafeApplication());
  }
  @Deprecated
  public static boolean safeAdd(String key, Object value, String expiration) {
    return play.api.cache.Cache.safeAdd(key,value,expiration,play.api.Play.unsafeApplication());
  }
  @Deprecated
  public static boolean safeReplace(String key, Object value, String expiration) {
    return play.api.cache.Cache.safeReplace(key,value,expiration, play.api.Play.unsafeApplication());
  }

}
