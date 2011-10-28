package play.api.cache

import java.io.NotSerializableException
import java.io.Serializable
import java.util.Map
import play.core._
import play.cache._
import play.api._
import play.Logger
import play.libs.Time
import collection.JavaConverters._
import akka.dispatch.Future
/**
 * The Cache. Mainly an interface to memcached or EhCache.
 *
 * Note: When specifying expiration == "0s" (zero seconds) the actual expiration-time may vary between different cache implementations
 */
object Cache {

  /**
   * based on http://blog.crazybob.org/2006/07/hard-core-java-threadlocal.html
   */
  private val mockLocal = new ThreadLocal[Array[CacheAPI]] {
    override protected def initialValue: Array[CacheAPI] = new Array[CacheAPI](1)
  }

  private def api(app: Application) = mockLocal.get.headOption.getOrElse(app.plugin[CachePlugin].api)

  def mock(m: CacheAPI) = (mockLocal.get).update(0, m)

  def getOrSet[T](key: String, expiration: String = "1h")(setter: => T)(implicit m: Manifest[T], app: Application): Option[T] = {
    val res = get[T](key)(m, app)
    res.map(Some(_)).getOrElse {
      set(key, setter.asInstanceOf[AnyRef], expiration)(app)
      None
    }
  }

  def getOrSetAsync[T](key: String,
    expiration: String,
    window: String,
    waitForEvaluation: String = "10s")(setter: => T)(implicit m: Manifest[T], app: Application): Future[T] = throw new Exception("not implemented yet")

  def getAsync[T](key: String, waitForEvaluation: String)(implicit m: Manifest[T], app: Application): Future[T] = throw new Exception("not implemented yet")

  def get[T](key: String)(implicit m: Manifest[T], app: Application): Option[T] = {
    Option(api(app).get(key).asInstanceOf[T])
  }

  def get[T](keys: String*)(implicit m: Manifest[T], app: Application): Map[String, T] = {
    api(app).get(keys.toArray).asScala.asInstanceOf[Map[String, T]]
  }

  /**
   * Add an element only if it doesn't exist.
   * @param key Element key
   * @param value Element value
   * @param expiration Ex: 10s, 3mn, 8h
   */
  def add(key: String, value: AnyRef, expiration: String)(implicit app: Application) {
    checkSerializable(value);
    api(app).add(key, value, Time.parseDuration(expiration));
  }

  /**
   * Add an element only if it doesn't exist and store it indefinitely.
   * @param key Element key
   * @param value Element value
   */
  def add(key: String, value: AnyRef)(implicit app: Application) {
    checkSerializable(value);
    api(app).add(key, value, Time.parseDuration(null));
  }

  /**
   * Set an element.
   * @param key Element key
   * @param value Element value
   * @param expiration Ex: 10s, 3mn, 8h
   */
  def set(key: String, value: AnyRef, expiration: String)(implicit app: Application) {
    checkSerializable(value);
    api(app).set(key, value, Time.parseDuration(expiration));
  }

  /**
   * Set an element and store it indefinitely.
   * @param key Element key
   * @param value Element value
   */
  def set(key: String, value: AnyRef)(implicit app: Application) {
    checkSerializable(value);
    api(app).set(key, value, Time.parseDuration(null));
  }

  /**
   * Replace an element only if it already exists.
   * @param key Element key
   * @param value Element value
   * @param expiration Ex: 10s, 3mn, 8h
   */
  def replace(key: String, value: AnyRef, expiration: String)(implicit app: Application) {
    checkSerializable(value);
    api(app).replace(key, value, Time.parseDuration(expiration));
  }

  /**
   * Replace an element only if it already exists and store it indefinitely.
   * @param key Element key
   * @param value Element value
   */
  def replace(key: String, value: AnyRef)(implicit app: Application) {
    checkSerializable(value);
    api(app).replace(key, value, Time.parseDuration(null));
  }

  /**
   * Increment the element value (must be a Number).
   * @param key Element key
   * @param by The incr value
   * @The new value
   */
  def incr(key: String, by: Int)(implicit app: Application): Long = {
    api(app).incr(key, by);
  }

  /**
   * Increment the element value (must be a Number) by 1.
   * @param key Element key
   * @The new value
   */
  def incr(key: String)(implicit app: Application): Long = {
    api(app).incr(key, 1);
  }

  /**
   * Decrement the element value (must be a Number).
   * @param key Element key
   * @param by The decr value
   * @The new value
   */
  def decr(key: String, by: Int)(implicit app: Application): Long = {
    api(app).decr(key, by);
  }

  /**
   * Decrement the element value (must be a Number) by 1.
   * @param key Element key
   * @The new value
   */
  def decr(key: String)(implicit app: Application): Long = {
    api(app).decr(key, 1);
  }

  /**
   * Delete an element from the cache.
   * @param key The element key
   */
  def delete(key: String)(implicit app: Application) {
    api(app).delete(key);
  }

  /**
   * Clear all data from cache.
   */
  def clear()(implicit app: Application) {
    api(app).clear();
  }

  /**
   * Stop the cache system.
   */
  def stop()(implicit app: Application) {
    api(app).stop();
  }

  /**
   * Utility that check that an object is serializable.
   */
  def checkSerializable(value: AnyRef) {
    if (value != null && !(value.isInstanceOf[Serializable])) {
      throw new Exception("Cannot cache a non-serializable value of type " + value.getClass().getName(), new NotSerializableException(value.getClass().getName()));
    }
  }
  def getAsJava(key: String, waitForEvaluation: String, app: Application): Future[_] = throw new Exception("not implemented")

  def getAsJava(key: String, app: Application): AnyRef = api(app).get(key)

  def getAsJava(keys: Array[String], app: Application): java.util.Map[String, AnyRef] = api(app).get(keys);

  @Deprecated
  def safeDelete(key: String)(implicit app: Application): Boolean = {
    api(app).safeDelete(key);
  }

  @Deprecated
  def safeAdd(key: String, value: AnyRef, expiration: String)(implicit app: Application): Boolean = {
    checkSerializable(value);
    api(app).safeAdd(key, value, Time.parseDuration(expiration));
  }

  @Deprecated
  def safeSet(key: String, value: AnyRef, expiration: String)(implicit app: Application): Boolean = {
    checkSerializable(value);
    api(app).safeSet(key, value, Time.parseDuration(expiration));
  }

  @Deprecated
  def safeReplace(key: String, value: AnyRef, expiration: String)(implicit app: Application): Boolean = {
    checkSerializable(value);
    api(app).safeReplace(key, value, Time.parseDuration(expiration));
  }

  @Deprecated
  def getAsync[T](key: String,
    expiration: String,
    window: String,
    waitForEvaluation: String = "10s")(getter: => T)(implicit app: Application, m: Manifest[T], isDesirable: T => Boolean): T = {
    Logger.warn("use `def getAsync(key: String, waitForEvaluation: String)(implicit m: Manifest[T], app: Application): Future[T]` instead")
    getOrSet[T](key)(getter)(m, app).getOrElse(getter)
  }

  @Deprecated
  def get[T](key: String,
    expiration: String,
    window: String)(getter: => T)(implicit app: Application, m: Manifest[T], isDesirable: T => Boolean): T = { Logger.warn("use `def getOrSet...` instead"); getOrSet[T](key, expiration)(getter)(m, app).getOrElse(getter) }

}
/**
 * Play Plugin to create a cache Store.
 *
 * @param app The application in which registering the plugin.
 */
class CachePlugin(app: Application) extends Plugin {
  import util.control.Exception._

  private lazy val useMemcache = app.configuration.getString("memcached", Some(Set("enabled", "disabled"))).filter(_ == "enabled")

  private lazy val cacheImpl: Option[CacheAPI] = for {
    yes <- useMemcache
    cacheConf <- app.configuration.getSub("memcached")
  } yield {
    val definedhosts = cacheConf.subKeys.filter(_.endsWith("host")).map(host => cacheConf.getString(host).get)
    val hosts: Set[String] = if (definedhosts.isEmpty) Set("localhost") else definedhosts
    //don't freak out, the null-s are there for java interop
    val user = cacheConf.getString("user").getOrElse(null)
    val password = cacheConf.getString("password").getOrElse(null)
    handling(classOf[Exception]) by { _ =>
      Logger.info("Error while connecting to  memcached, Fallback to local cache")
      new EhCacheImpl
    } apply {
      Logger.info("Connecting to memcached")
      new MemcachedImpl(hosts.mkString(" "), user, password)
    }

  }
  def api = cacheImpl getOrElse (new EhCacheImpl)

}
