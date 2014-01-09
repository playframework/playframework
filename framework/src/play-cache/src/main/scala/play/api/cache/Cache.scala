package play.api.cache

import play.api._

import reflect.{ ClassTag, ClassManifest }
import org.apache.commons.lang3.reflect.TypeUtils

import scala.concurrent.duration.Duration
/**
 * API for a Cache plugin.
 */
trait CacheAPI {

  /**
   * Set a value into the cache.
   *
   * @param key Item key.
   * @param value Item value.
   * @param expiration Expiration time in seconds (0 second means eternity).
   */
  def set(key: String, value: Any, expiration: Int)

  /**
   * Retrieve a value from the cache.
   *
   * @param key Item key.
   */
  def get(key: String): Option[Any]

  /**
   * Remove a value from the cache
   */
  def remove(key: String)
}

/**
 * Public Cache API.
 *
 * The underlying Cache implementation is received from plugin.
 */
object Cache {

  private def cacheAPI(implicit app: Application): CacheAPI = {
    app.plugin[CachePlugin] match {
      case Some(plugin) => plugin.api
      case None => throw new Exception("There is no cache plugin registered. Make sure at least one CachePlugin implementation is enabled.")
    }
  }

  /**
   * Set a value into the cache.
   *
   * @param key Item key.
   * @param value Item value.
   * @param expiration Expiration time in seconds (0 second means eternity).
   */
  def set(key: String, value: Any, expiration: Int = 0)(implicit app: Application): Unit = {
    cacheAPI.set(key, value, expiration)
  }

  /**
   * Set a value into the cache.
   *
   * @param key Item key.
   * @param value Item value.
   * @param expiration Expiration time as a [[scala.concurrent.duration.Duration]].
   */
  def set(key: String, value: Any, expiration: Duration)(implicit app: Application): Unit = {
    set(key, value, expiration.toSeconds.toInt)
  }
  /**
   * Retrieve a value from the cache.
   *
   * @param key Item key.
   */
  def get(key: String)(implicit app: Application): Option[Any] = {
    cacheAPI.get(key)
  }

  /**
   * Retrieve a value from the cache, or set it from a default function.
   *
   * @param key Item key.
   * @param expiration expiration period in seconds.
   * @param orElse The default function to invoke if the value was not found in cache.
   */
  def getOrElse[A](key: String, expiration: Int = 0)(orElse: => A)(implicit app: Application, ct: ClassTag[A]): A = {
    getAs[A](key).getOrElse {
      val value = orElse
      set(key, value, expiration)
      value
    }
  }

  /**
   * Retrieve a value from the cache for the given type
   *
   * @param key Item key.
   * @return result as Option[T]
   */
  def getAs[T](key: String)(implicit app: Application, ct: ClassTag[T]): Option[T] = {
    get(key)(app).map { item =>
      if (TypeUtils.isInstance(item, ct.runtimeClass)) Some(item.asInstanceOf[T]) else None
    }.getOrElse(None)
  }

  def remove(key: String)(implicit app: Application) {
    cacheAPI.remove(key)
  }
}

/**
 * A Cache Plugin provides an implementation of the Cache API.
 */
abstract class CachePlugin extends Plugin {

  /**
   * Implementation of the the Cache plugin
   * provided by this plugin.
   */
  def api: CacheAPI

}

import net.sf.ehcache._

/**
 * EhCache implementation.
 */
class EhCachePlugin(app: Application) extends CachePlugin {

  @volatile var loaded = false

  lazy val manager = {
    loaded = true
    // See if there's an ehcache.xml, or fall back to the built in ehcache-default.xml
    val ehcacheXml = Option(app.classloader.getResource("ehcache.xml"))
      .getOrElse(app.classloader.getResource("ehcache-default.xml"))
    CacheManager.create(ehcacheXml)
  }

  lazy val cache: Ehcache = {
    manager.addCache("play")
    manager.getEhcache("play")
  }

  /**
   * Is this plugin enabled.
   *
   * {{{
   * ehcacheplugin=disabled
   * }}}
   */
  override lazy val enabled = {
    !app.configuration.getString("ehcacheplugin").filter(_ == "disabled").isDefined
  }

  override def onStart() {
    cache
  }

  override def onStop() {
    if (loaded) {
      manager.shutdown()
    }
  }

  lazy val api = new EhCacheImpl(cache)

}

class EhCacheImpl(private val cache: Ehcache) extends CacheAPI {

  def set(key: String, value: Any, expiration: Int) {
    val element = new Element(key, value)
    if (expiration == 0) element.setEternal(true)
    element.setTimeToLive(expiration)
    cache.put(element)
  }

  def get(key: String): Option[Any] = {
    Option(cache.get(key)).map(_.getObjectValue)
  }

  def remove(key: String) {
    cache.remove(key)
  }
}
