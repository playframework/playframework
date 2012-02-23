package play.api.cache

import play.api._

import reflect.ClassManifest
/**
 * API for a Cache plugin.
 */
trait CacheAPI {

  /**
   * Set a value into the cache.
   *
   * @param key Item key.
   * @param value Item value.
   * @param expiration Expiration time in seconds.
   */
  def set(key: String, value: Any, expiration: Int)

  /**
   * Retrieve a value from the cache.
   *
   * @param key Item key.
   */
  def get(key: String): Option[Any]

}

/**
 * Public Cache API.
 *
 * The underlying Cache implementation is received from plugin.
 */
object Cache {

  private def error = throw new Exception(
    "There is no cache plugin registered. Make sure at least one CachePlugin implementation is enabled."
  )

  /**
   * Sets a value without expiration
   *
   * @param key Item key.
   * @param value Item value.
   * @param expiration expiration period in seconds.
   */
  def set(key: String, value: Any, expiration: Int = 0)(implicit app: Application) = {
    app.plugin[CachePlugin].map(_.api.set(key, value, expiration)).getOrElse(error)
  }

  /**
   * Retrieve a value from the cache.
   *
   * @param key Item key.
   */
  def get(key: String)(implicit app: Application): Option[Any] = {
    app.plugin[CachePlugin].map(_.api.get(key)).getOrElse(error)
  }

  /**
   * Retrieve a value from the cache, or set it from a default function.
   *
   * @param key Item key.
   * @param expiration expiration period in seconds.
   * @param orElse The default function to invoke if the value was found in cache.
   */
  def getOrElse[A](key: String, expiration: Int = 0)(orElse: => A)(implicit app: Application, m: ClassManifest[A]): A = {
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
  def getAs[T](key: String)(implicit app: Application, m: ClassManifest[T]): Option[T] = {
    get(key)(app).map { item =>
      if (m.erasure.isAssignableFrom(item.getClass)) Some(item.asInstanceOf[T]) else None
    }.getOrElse(None)
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

/**
 * EhCache implementation.
 */
class EhCachePlugin(app: Application) extends CachePlugin {

  import net.sf.ehcache._

  lazy val (manager, cache) = {
    val manager = CacheManager.create()
    manager.addCache("play")
    (manager, manager.getCache("play"))
  }

  /**
   * Is this plugin enabled.
   *
   * {{{
   * ehcacheplugin.disabled=true
   * }}}
   */
  override lazy val enabled = {
    !app.configuration.getString("ehcacheplugin").filter(_ == "disabled").isDefined
  }

  override def onStart() {
    cache
  }

  override def onStop() {
    manager.shutdown()
  }

  lazy val api = new CacheAPI {

    def set(key: String, value: Any, expiration: Int) {
      val element = new Element(key, value)
      if (expiration == 0) element.setEternal(true)
      element.setTimeToLive(expiration)
      cache.put(element)
    }

    def get(key: String): Option[Any] = {
      Option(cache.get(key)).map(_.getObjectValue)
    }

  }

}
