/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.cache.ehcache

import javax.inject.{ Inject, Provider, Singleton }

import akka.Done
import akka.stream.Materializer
import com.google.common.primitives.Primitives
import net.sf.ehcache.{ CacheManager, Ehcache, Element, ObjectExistsException }
import play.api.cache._
import play.api.inject._
import play.api.{ Configuration, Environment }
import play.cache.{ AsyncCacheApi => JavaAsyncCacheApi, SyncCacheApi => JavaSyncCacheApi, CacheApi => JavaCacheApi, DefaultAsyncCacheApi => DefaultJavaAsyncCacheApi, DefaultSyncCacheApi => JavaDefaultSyncCacheApi, NamedCacheImpl }

import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag

/**
 * EhCache components for compile time injection
 */
trait EhCacheComponents {
  def environment: Environment
  def configuration: Configuration
  def applicationLifecycle: ApplicationLifecycle
  implicit def executionContext: ExecutionContext

  lazy val ehCacheManager: CacheManager = new CacheManagerProvider(environment, configuration, applicationLifecycle).get

  /**
   * Use this to create with the given name.
   */
  def cacheApi(name: String, create: Boolean = true): AsyncCacheApi = {
    val createNamedCaches = configuration.underlying.getBoolean("play.cache.createBoundCaches")
    new EhCacheApi(NamedEhCacheProvider.getNamedCache(name, ehCacheManager, createNamedCaches))
  }

  lazy val defaultCacheApi: AsyncCacheApi = cacheApi("play")
}

/**
 * EhCache implementation.
 */
class EhCacheModule extends SimpleModule((environment, configuration) => {

  import scala.collection.JavaConverters._

  val defaultCacheName = configuration.underlying.getString("play.cache.defaultCache")
  val bindCaches = configuration.underlying.getStringList("play.cache.bindCaches").asScala
  val createBoundCaches = configuration.underlying.getBoolean("play.cache.createBoundCaches")

  // Creates a named cache qualifier
  def named(name: String): NamedCache = {
    new NamedCacheImpl(name)
  }

  // bind a cache with the given name
  def bindCache(name: String) = {
    val namedCache = named(name)
    val ehcacheKey = bind[Ehcache].qualifiedWith(namedCache)
    val cacheApiKey = bind[AsyncCacheApi].qualifiedWith(namedCache)
    Seq(
      ehcacheKey.to(new NamedEhCacheProvider(name, createBoundCaches)),
      cacheApiKey.to(new NamedCacheApiProvider(ehcacheKey)),
      bind[JavaAsyncCacheApi].qualifiedWith(namedCache).to(new NamedJavaAsyncCacheApiProvider(cacheApiKey)),
      bind[Cached].qualifiedWith(namedCache).to(new NamedCachedProvider(cacheApiKey)),
      bind[SyncCacheApi].qualifiedWith(namedCache).to[DefaultSyncCacheApi],
      bind[CacheApi].qualifiedWith(namedCache).to[DefaultSyncCacheApi],
      bind[JavaCacheApi].qualifiedWith(namedCache).to[JavaDefaultSyncCacheApi],
      bind[JavaSyncCacheApi].qualifiedWith(namedCache).to[JavaDefaultSyncCacheApi]
    )
  }

  Seq(
    bind[CacheManager].toProvider[CacheManagerProvider],
    // alias the default cache to the unqualified implementation
    bind[AsyncCacheApi].to(bind[AsyncCacheApi].qualifiedWith(named(defaultCacheName))),
    bind[JavaAsyncCacheApi].to[DefaultJavaAsyncCacheApi],
    bind[SyncCacheApi].to[DefaultSyncCacheApi],
    bind[CacheApi].to[DefaultSyncCacheApi],
    bind[JavaCacheApi].to[JavaDefaultSyncCacheApi],
    bind[JavaSyncCacheApi].to[JavaDefaultSyncCacheApi]
  ) ++ bindCache(defaultCacheName) ++ bindCaches.flatMap(bindCache)
})

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

private[play] class NamedEhCacheProvider(name: String, create: Boolean) extends Provider[Ehcache] {
  @Inject private var manager: CacheManager = _
  lazy val get: Ehcache = NamedEhCacheProvider.getNamedCache(name, manager, create)
}

private[play] object NamedEhCacheProvider {
  def getNamedCache(name: String, manager: CacheManager, create: Boolean) = try {
    if (create) {
      manager.addCache(name)
    }
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

private[play] class NamedCacheApiProvider(key: BindingKey[Ehcache]) extends Provider[AsyncCacheApi] {
  @Inject private var injector: Injector = _
  lazy val get: AsyncCacheApi = {
    new EhCacheApi(injector.instanceOf(key))(injector.instanceOf[ExecutionContext])
  }
}

private[play] class NamedJavaAsyncCacheApiProvider(key: BindingKey[AsyncCacheApi]) extends Provider[JavaAsyncCacheApi] {
  @Inject private var injector: Injector = _
  lazy val get: JavaAsyncCacheApi = {
    new DefaultJavaAsyncCacheApi(injector.instanceOf(key))
  }
}

private[play] class NamedCachedProvider(key: BindingKey[AsyncCacheApi]) extends Provider[Cached] {
  @Inject private var injector: Injector = _
  lazy val get: Cached = {
    new Cached(injector.instanceOf(key))(injector.instanceOf[Materializer])
  }
}

private[play] case class EhCacheExistsException(msg: String, cause: Throwable) extends RuntimeException(msg, cause)

@Singleton
class EhCacheApi @Inject() (cache: Ehcache)(implicit context: ExecutionContext) extends AsyncCacheApi {

  def set(key: String, value: Any, expiration: Duration): Future[Done] = {
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
    Future.successful {
      cache.put(element)
      Done
    }
  }

  def get[T](key: String)(implicit ct: ClassTag[T]): Future[Option[T]] = {
    val result = Option(cache.get(key)).map(_.getObjectValue).filter { v =>
      Primitives.wrap(ct.runtimeClass).isInstance(v) ||
        ct == ClassTag.Nothing || (ct == ClassTag.Unit && v == ((): Unit))
    }.asInstanceOf[Option[T]]
    Future.successful(result)
  }

  def getOrElseUpdate[A: ClassTag](key: String, expiration: Duration)(orElse: => Future[A]): Future[A] = {
    get[A](key).flatMap {
      case Some(value) => Future.successful(value)
      case None => orElse.flatMap(value => set(key, value, expiration).map(_ => value))
    }
  }

  def remove(key: String): Future[Done] = {
    Future.successful {
      cache.remove(key)
      Done
    }
  }

  def clearAll: Future[Done] = {
    Future.successful {
      cache.removeAll
      Done
    }
  }
}

