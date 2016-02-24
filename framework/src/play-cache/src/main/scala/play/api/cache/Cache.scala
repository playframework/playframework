/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.cache

import javax.inject._
import play.api._
import play.api.inject.{ BindingKey, Injector, ApplicationLifecycle, Module }
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.concurrent.duration._
import play.cache.{ CacheApi => JavaCacheApi, DefaultCacheApi => DefaultJavaCacheApi, NamedCacheImpl }

import net.sf.ehcache._
import com.google.common.primitives.Primitives

/**
 * The cache API
 */
trait CacheApi {

  /**
   * Set a value into the cache.
   *
   * @param key Item key.
   * @param value Item value.
   * @param expiration Expiration time.
   */
  def set(key: String, value: Any, expiration: Duration = Duration.Inf)

  /**
   * Remove a value from the cache
   */
  def remove(key: String)

  /**
   * Retrieve a value from the cache, or set it from a default function.
   *
   * @param key Item key.
   * @param expiration expiration period in seconds.
   * @param orElse The default function to invoke if the value was not found in cache.
   */
  def getOrElse[A: ClassTag](key: String, expiration: Duration = Duration.Inf)(orElse: => A): A

  /**
   * Retrieve a value from the cache for the given type
   *
   * @param key Item key.
   * @return result as Option[T]
   */
  def get[T: ClassTag](key: String): Option[T]
}

/**
 * Public Cache API.
 *
 * The underlying Cache implementation is received from plugin.
 */
object Cache {

  private val cacheApiCache = Application.instanceCache[CacheApi]
  private[cache] def cacheApi(implicit app: Application) = cacheApiCache(app)

  private def intToDuration(seconds: Int): Duration = if (seconds == 0) Duration.Inf else seconds.seconds

  /**
   * Set a value into the cache.
   *
   * @param key Item key.
   * @param value Item value.
   * @param expiration Expiration time as a [[scala.concurrent.duration.Duration]].
   */
  @deprecated("Inject CacheApi into your component", "2.5.0")
  def set(key: String, value: Any, expiration: Duration = Duration.Inf)(implicit app: Application): Unit = {
    cacheApi.set(key, value, expiration)
  }

  /**
   * Set a value into the cache.
   *
   * @param key Item key.
   * @param value Item value.
   * @param expiration Expiration time in seconds (0 second means eternity).
   */
  @deprecated("Inject CacheApi into your component", "2.5.0")
  def set(key: String, value: Any, expiration: Int)(implicit app: Application): Unit = {
    set(key, value, intToDuration(expiration))
  }

  /**
   * Retrieve a value from the cache.
   *
   * @param key Item key.
   */
  @deprecated("Inject CacheApi into your component", "2.5.0")
  def get(key: String)(implicit app: Application): Option[Any] = {
    cacheApi.get[Any](key)
  }

  /**
   * Retrieve a value from the cache, or set it from a default function.
   *
   * @param key Item key.
   * @param expiration expiration period as a [[scala.concurrent.duration.Duration]].
   * @param orElse The default function to invoke if the value was not found in cache.
   */
  @deprecated("Inject CacheApi into your component", "2.5.0")
  def getOrElse[A](key: String, expiration: Duration = Duration.Inf)(orElse: => A)(implicit app: Application, ct: ClassTag[A]): A = {
    cacheApi.getOrElse(key, expiration)(orElse)
  }

  /**
   * Retrieve a value from the cache, or set it from a default function.
   *
   * @param key Item key.
   * @param expiration expiration period in seconds.
   * @param orElse The default function to invoke if the value was not found in cache.
   */
  @deprecated("Inject CacheApi into your component", "2.5.0")
  def getOrElse[A](key: String, expiration: Int)(orElse: => A)(implicit app: Application, ct: ClassTag[A]): A = {
    getOrElse(key, intToDuration(expiration))(orElse)
  }

  /**
   * Retrieve a value from the cache for the given type
   *
   * @param key Item key.
   * @return result as Option[T]
   */
  @deprecated("Inject CacheApi into your component", "2.5.0")
  def getAs[T](key: String)(implicit app: Application, ct: ClassTag[T]): Option[T] = {
    cacheApi.get[T](key)
  }

  @deprecated("Inject CacheApi into your component", "2.5.0")
  def remove(key: String)(implicit app: Application): Unit = {
    cacheApi.remove(key)
  }
}

/**
 * EhCache components for compile time injection
 */
trait EhCacheComponents {
  def environment: Environment
  def configuration: Configuration
  def applicationLifecycle: ApplicationLifecycle

  lazy val ehCacheManager: CacheManager = new CacheManagerProvider(environment, configuration, applicationLifecycle).get

  /**
   * Use this to create with the given name.
   */
  def cacheApi(name: String): CacheApi = {
    new EhCacheApi(NamedEhCacheProvider.getNamedCache(name, ehCacheManager))
  }

  lazy val defaultCacheApi: CacheApi = cacheApi("play")
}

/**
 * EhCache implementation.
 */
@Singleton
class EhCacheModule extends Module {

  import scala.collection.JavaConversions._

  def bindings(environment: Environment, configuration: Configuration) = {
    val defaultCacheName = configuration.underlying.getString("play.cache.defaultCache")
    val bindCaches = configuration.underlying.getStringList("play.cache.bindCaches").toSeq

    // Creates a named cache qualifier
    def named(name: String): NamedCache = {
      new NamedCacheImpl(name)
    }

    // bind a cache with the given name
    def bindCache(name: String) = {
      val namedCache = named(name)
      val ehcacheKey = bind[Ehcache].qualifiedWith(namedCache)
      val cacheApiKey = bind[CacheApi].qualifiedWith(namedCache)
      Seq(
        ehcacheKey.to(new NamedEhCacheProvider(name)),
        cacheApiKey.to(new NamedCacheApiProvider(ehcacheKey)),
        bind[JavaCacheApi].qualifiedWith(namedCache).to(new NamedJavaCacheApiProvider(cacheApiKey)),
        bind[Cached].qualifiedWith(namedCache).to(new NamedCachedProvider(cacheApiKey))
      )
    }

    Seq(
      bind[CacheManager].toProvider[CacheManagerProvider],
      // alias the default cache to the unqualified implementation
      bind[CacheApi].to(bind[CacheApi].qualifiedWith(named(defaultCacheName))),
      bind[JavaCacheApi].to[DefaultJavaCacheApi]
    ) ++ bindCache(defaultCacheName) ++ bindCaches.flatMap(bindCache)
  }
}

@Singleton
class CacheManagerProvider @Inject() (env: Environment, config: Configuration, lifecycle: ApplicationLifecycle) extends Provider[CacheManager] {
  lazy val get: CacheManager = {
    val resourceName = config.underlying.getString("play.cache.configResource")
    val configResource = env.resource(resourceName).getOrElse(env.classLoader.getResource("ehcache-default.xml"))
    val manager = CacheManager.create(configResource)
    lifecycle.addStopHook(() => Future.successful(manager.shutdown()))
    manager
  }
}

private[play] class NamedEhCacheProvider(name: String) extends Provider[Ehcache] {
  @Inject private var manager: CacheManager = _
  lazy val get: Ehcache = NamedEhCacheProvider.getNamedCache(name, manager)
}

private[play] object NamedEhCacheProvider {
  def getNamedCache(name: String, manager: CacheManager) = try {
    manager.addCache(name)
    manager.getEhcache(name)
  } catch {
    case e: ObjectExistsException =>
      throw new EhCacheExistsException(
        s"""An EhCache instance with name '$name' already exists.
           |
           |This usually indicates that multiple instances of a dependent component (e.g. a Play application) have been started at the same time.
         """.stripMargin, e)
  }
}

private[play] class NamedCacheApiProvider(key: BindingKey[Ehcache]) extends Provider[CacheApi] {
  @Inject private var injector: Injector = _
  lazy val get: CacheApi = {
    new EhCacheApi(injector.instanceOf(key))
  }
}

private[play] class NamedJavaCacheApiProvider(key: BindingKey[CacheApi]) extends Provider[JavaCacheApi] {
  @Inject private var injector: Injector = _
  lazy val get: JavaCacheApi = {
    new DefaultJavaCacheApi(injector.instanceOf(key))
  }
}

private[play] class NamedCachedProvider(key: BindingKey[CacheApi]) extends Provider[Cached] {
  @Inject private var injector: Injector = _
  lazy val get: Cached = {
    new Cached(injector.instanceOf(key))
  }
}

private[play] case class EhCacheExistsException(msg: String, cause: Throwable) extends RuntimeException(msg, cause)

@Singleton
class EhCacheApi @Inject() (cache: Ehcache) extends CacheApi {

  def set(key: String, value: Any, expiration: Duration) = {
    val element = new Element(key, value)
    expiration match {
      case infinite: Duration.Infinite => element.setEternal(true)
      case finite: FiniteDuration =>
        val seconds = finite.toSeconds
        if (seconds <= 0) {
          element.setTimeToLive(1)
        } else if (seconds > Int.MaxValue) {
          element.setTimeToLive(Int.MaxValue)
        } else {
          element.setTimeToLive(seconds.toInt)
        }
    }
    cache.put(element)
  }

  def get[T](key: String)(implicit ct: ClassTag[T]): Option[T] = {
    Option(cache.get(key)).map(_.getObjectValue).filter { v =>
      Primitives.wrap(ct.runtimeClass).isInstance(v) ||
        ct == ClassTag.Nothing || (ct == ClassTag.Unit && v == ((): Unit))
    }.asInstanceOf[Option[T]]
  }

  def getOrElse[A: ClassTag](key: String, expiration: Duration)(orElse: => A) = {
    get[A](key).getOrElse {
      val value = orElse
      set(key, value, expiration)
      value
    }
  }

  def remove(key: String) = {
    cache.remove(key)
  }
}
