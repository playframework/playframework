/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache.caffeine

import java.util.concurrent.TimeUnit
import javax.inject.{ Inject, Provider, Singleton }
import javax.cache.CacheException

import akka.Done
import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.common.primitives.Primitives
import play.cache.caffeine.NamedCaffeineCache
import play.api.cache._
import play.api.inject._
import play.api.Configuration
import play.cache.{ NamedCacheImpl, SyncCacheApiAdapter, AsyncCacheApi => JavaAsyncCacheApi, DefaultAsyncCacheApi => JavaDefaultAsyncCacheApi, SyncCacheApi => JavaSyncCacheApi }

import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag

/**
 * CaffeineCache components for compile time injection
 */
trait CaffeineCacheComponents {
  def configuration: Configuration
  def actorSystem: ActorSystem
  implicit def executionContext: ExecutionContext

  lazy val caffeineCacheManager: CaffeineCacheManager = new CaffeineCacheManager(configuration.underlying.getConfig("play.cache.caffeine"))

  /**
   * Use this to create with the given name.
   */
  def cacheApi(name: String): AsyncCacheApi = {
    val ec = configuration.get[Option[String]]("play.cache.dispatcher")
      .fold(executionContext)(actorSystem.dispatchers.lookup(_))
    new CaffeineCacheApi(NamedCaffeineCacheProvider.getNamedCache(name, caffeineCacheManager, configuration))(ec)
  }

  lazy val defaultCacheApi: AsyncCacheApi = cacheApi(configuration.underlying.getString("play.cache.defaultCache"))
}

/**
 * CaffeineCache implementation.
 */
class CaffeineCacheModule extends SimpleModule((environment, configuration) => {

  import scala.collection.JavaConverters._

  val defaultCacheName = configuration.underlying.getString("play.cache.defaultCache")
  val bindCaches = configuration.underlying.getStringList("play.cache.bindCaches").asScala

  // Creates a named cache qualifier
  def named(name: String): NamedCache = {
    new NamedCacheImpl(name)
  }

  // bind wrapper classes
  def wrapperBindings(cacheApiKey: BindingKey[AsyncCacheApi], namedCache: NamedCache): Seq[Binding[_]] = Seq(
    bind[JavaAsyncCacheApi].qualifiedWith(namedCache).to(new NamedJavaAsyncCacheApiProvider(cacheApiKey)),
    bind[Cached].qualifiedWith(namedCache).to(new NamedCachedProvider(cacheApiKey)),
    bind[SyncCacheApi].qualifiedWith(namedCache).to(new NamedSyncCacheApiProvider(cacheApiKey)),
    bind[JavaSyncCacheApi].qualifiedWith(namedCache).to(new NamedJavaSyncCacheApiProvider(cacheApiKey))
  )

  // bind a cache with the given name
  def bindCache(name: String) = {
    val namedCache = named(name)
    val caffeineCacheKey = bind[NamedCaffeineCache[Any, Any]].qualifiedWith(namedCache)
    val cacheApiKey = bind[AsyncCacheApi].qualifiedWith(namedCache)
    Seq(
      caffeineCacheKey.to(new NamedCaffeineCacheProvider(name, configuration)),
      cacheApiKey.to(new NamedAsyncCacheApiProvider(caffeineCacheKey))
    ) ++ wrapperBindings(cacheApiKey, namedCache)
  }

  def bindDefault[T: ClassTag]: Binding[T] = {
    bind[T].to(bind[T].qualifiedWith(named(defaultCacheName)))
  }

  Seq(
    bind[CaffeineCacheManager].toProvider[CacheManagerProvider],
    // alias the default cache to the unqualified implementation
    bindDefault[AsyncCacheApi],
    bindDefault[JavaAsyncCacheApi],
    bindDefault[SyncCacheApi],
    bindDefault[JavaSyncCacheApi]
  ) ++ bindCache(defaultCacheName) ++ bindCaches.flatMap(bindCache)
})

@Singleton
class CacheManagerProvider @Inject() (configuration: Configuration) extends Provider[CaffeineCacheManager] {
  lazy val get: CaffeineCacheManager = {
    val cacheManager: CaffeineCacheManager = new CaffeineCacheManager(configuration.underlying.getConfig("play.cache.caffeine"))
    cacheManager
  }
}

private[play] class NamedCaffeineCacheProvider(name: String, configuration: Configuration) extends Provider[NamedCaffeineCache[Any, Any]] {
  @Inject private var manager: CaffeineCacheManager = _
  lazy val get: NamedCaffeineCache[Any, Any] = NamedCaffeineCacheProvider.getNamedCache(name, manager, configuration)
}

private[play] object NamedCaffeineCacheProvider {
  def getNamedCache(name: String, manager: CaffeineCacheManager, configuration: Configuration) = try {
    manager.getCache(name).asInstanceOf[NamedCaffeineCache[Any, Any]]
  } catch {
    case e: CacheException =>
      throw new CaffeineCacheExistsException(
        s"""A CaffeineCache instance with name '$name' already exists.
           |
           |This usually indicates that multiple instances of a dependent component (e.g. a Play application) have been started at the same time.
         """.stripMargin, e)
  }
}

private[play] class NamedAsyncCacheApiProvider(key: BindingKey[NamedCaffeineCache[Any, Any]]) extends Provider[AsyncCacheApi] {
  @Inject private var injector: Injector = _
  @Inject private var defaultEc: ExecutionContext = _
  @Inject private var configuration: Configuration = _
  @Inject private var actorSystem: ActorSystem = _
  private lazy val ec: ExecutionContext = configuration.get[Option[String]]("play.cache.dispatcher").map(actorSystem.dispatchers.lookup(_)).getOrElse(defaultEc)
  lazy val get: AsyncCacheApi =
    new CaffeineCacheApi(injector.instanceOf(key))(ec)
}

private[play] class NamedSyncCacheApiProvider(key: BindingKey[AsyncCacheApi])
  extends Provider[SyncCacheApi] {
  @Inject private var injector: Injector = _

  lazy val get: SyncCacheApi = {
    val async = injector.instanceOf(key)
    async.sync match {
      case sync: SyncCacheApi => sync
      case _ => new DefaultSyncCacheApi(async)
    }
  }
}

private[play] class NamedJavaAsyncCacheApiProvider(key: BindingKey[AsyncCacheApi]) extends Provider[JavaAsyncCacheApi] {
  @Inject private var injector: Injector = _
  lazy val get: JavaAsyncCacheApi = {
    new JavaDefaultAsyncCacheApi(injector.instanceOf(key))
  }

}

private[play] class NamedJavaSyncCacheApiProvider(key: BindingKey[AsyncCacheApi])
  extends Provider[JavaSyncCacheApi] {
  @Inject private var injector: Injector = _
  lazy val get: JavaSyncCacheApi =
    new SyncCacheApiAdapter(injector.instanceOf(key).sync)
}

private[play] class NamedCachedProvider(key: BindingKey[AsyncCacheApi]) extends Provider[Cached] {
  @Inject private var injector: Injector = _
  lazy val get: Cached =
    new Cached(injector.instanceOf(key))(injector.instanceOf[Materializer])
}

private[play] case class CaffeineCacheExistsException(msg: String, cause: Throwable) extends RuntimeException(msg, cause)

class SyncCaffeineCacheApi @Inject() (val cache: NamedCaffeineCache[Any, Any]) extends SyncCacheApi {

  override def set(key: String, value: Any, expiration: Duration): Unit = {
    expiration match {
      case infinite: Duration.Infinite => cache.policy().expireVariably().get().put(key, value, Long.MaxValue, TimeUnit.DAYS)
      case finite: FiniteDuration =>
        val seconds = finite.toSeconds
        if (seconds <= 0) {
          cache.policy().expireVariably().get().put(key, value, 1, TimeUnit.SECONDS)
        } else {
          cache.policy().expireVariably().get().put(key, value, seconds.toInt, TimeUnit.SECONDS)
        }
    }

    Done
  }

  override def remove(key: String): Unit = cache.invalidate(key)

  override def getOrElseUpdate[A: ClassTag](key: String, expiration: Duration)(orElse: => A): A = {
    get[A](key) match {
      case Some(value) => value
      case None =>
        val value = orElse
        set(key, value, expiration)
        value
    }
  }

  override def get[T](key: String)(implicit ct: ClassTag[T]): Option[T] = {
    Option(cache.getIfPresent(key)).filter { v =>
      Primitives.wrap(ct.runtimeClass).isInstance(v) ||
        ct == ClassTag.Nothing || (ct == ClassTag.Unit && v == ((): Unit))
    }.asInstanceOf[Option[T]]
  }
}

/**
 * Cache implementation of [[AsyncCacheApi]]. Since Cache is synchronous by default, this uses [[SyncCaffeineCacheApi]].
 */
class CaffeineCacheApi @Inject() (val cache: NamedCaffeineCache[Any, Any])(implicit context: ExecutionContext) extends AsyncCacheApi {

  override lazy val sync: SyncCaffeineCacheApi = new SyncCaffeineCacheApi(cache)

  def set(key: String, value: Any, expiration: Duration): Future[Done] = Future {
    sync.set(key, value, expiration)
    Done
  }

  def get[T: ClassTag](key: String): Future[Option[T]] = Future {
    sync.get(key)
  }

  def remove(key: String): Future[Done] = Future {
    sync.remove(key)
    Done
  }

  def getOrElseUpdate[A: ClassTag](key: String, expiration: Duration)(orElse: => Future[A]): Future[A] = {
    get[A](key).flatMap {
      case Some(value) => Future.successful(value)
      case None => orElse.flatMap(value => set(key, value, expiration).map(_ => value))
    }
  }

  def removeAll(): Future[Done] = Future {
    cache.invalidateAll()
    Done
  }
}
