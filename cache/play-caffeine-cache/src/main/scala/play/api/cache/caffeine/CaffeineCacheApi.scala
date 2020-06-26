/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache.caffeine

import java.util.concurrent.Executor

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import javax.cache.CacheException
import akka.Done
import akka.actor.ActorSystem
import akka.stream.Materializer
import com.github.benmanes.caffeine.cache.Cache
import com.google.common.primitives.Primitives
import play.cache.caffeine.NamedCaffeineCache
import play.api.cache._
import play.api.inject._
import play.api.Configuration
import play.api.libs.streams.Execution.trampoline
import play.cache.NamedCacheImpl
import play.cache.SyncCacheApiAdapter
import play.cache.{ AsyncCacheApi => JavaAsyncCacheApi }
import play.cache.{ DefaultAsyncCacheApi => JavaDefaultAsyncCacheApi }
import play.cache.{ SyncCacheApi => JavaSyncCacheApi }

import scala.compat.java8.FutureConverters
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * CaffeineCache components for compile time injection
 */
trait CaffeineCacheComponents {
  def configuration: Configuration
  def actorSystem: ActorSystem
  implicit def executionContext: ExecutionContext

  lazy val caffeineCacheManager: CaffeineCacheManager = new CaffeineCacheManager(
    configuration.underlying.getConfig("play.cache.caffeine"),
    actorSystem
  )

  /**
   * Use this to create with the given name.
   */
  def cacheApi(name: String): AsyncCacheApi = {
    new CaffeineCacheApi(NamedCaffeineCacheProvider.getNamedCache(name, caffeineCacheManager, configuration))
  }

  lazy val defaultCacheApi: AsyncCacheApi = cacheApi(configuration.underlying.getString("play.cache.defaultCache"))
}

/**
 * CaffeineCache implementation.
 */
class CaffeineCacheModule
    extends SimpleModule((environment, configuration) => {
      import scala.collection.JavaConverters._

      val defaultCacheName = configuration.underlying.getString("play.cache.defaultCache")
      val bindCaches       = configuration.underlying.getStringList("play.cache.bindCaches").asScala

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
        val namedCache       = named(name)
        val caffeineCacheKey = bind[NamedCaffeineCache[Any, Any]].qualifiedWith(namedCache)
        val cacheApiKey      = bind[AsyncCacheApi].qualifiedWith(namedCache)
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
        bindDefault[NamedCaffeineCache[Any, Any]],
        bindDefault[AsyncCacheApi],
        bindDefault[JavaAsyncCacheApi],
        bindDefault[SyncCacheApi],
        bindDefault[JavaSyncCacheApi]
      ) ++ bindCache(defaultCacheName) ++ bindCaches.flatMap(bindCache)
    })

@Singleton
class CacheManagerProvider @Inject() (configuration: Configuration, actorSystem: ActorSystem)
    extends Provider[CaffeineCacheManager] {
  lazy val get: CaffeineCacheManager = {
    val cacheManager: CaffeineCacheManager = new CaffeineCacheManager(
      configuration.underlying.getConfig("play.cache.caffeine"),
      actorSystem
    )
    cacheManager
  }
}

private[play] class NamedCaffeineCacheProvider(name: String, configuration: Configuration)
    extends Provider[NamedCaffeineCache[Any, Any]] {
  @Inject private var manager: CaffeineCacheManager = _
  lazy val get: NamedCaffeineCache[Any, Any]        = NamedCaffeineCacheProvider.getNamedCache(name, manager, configuration)
}

private[play] object NamedCaffeineCacheProvider {
  def getNamedCache(name: String, manager: CaffeineCacheManager, configuration: Configuration) =
    try {
      manager.getCache(name).asInstanceOf[NamedCaffeineCache[Any, Any]]
    } catch {
      case e: CacheException =>
        throw new CaffeineCacheExistsException(s"""A CaffeineCache instance with name '$name' already exists.
                                                  |
                                                  |This usually indicates that multiple instances of a dependent component (e.g. a Play application) have been started at the same time.
         """.stripMargin, e)
    }
}

private[play] class NamedAsyncCacheApiProvider(key: BindingKey[NamedCaffeineCache[Any, Any]])
    extends Provider[AsyncCacheApi] {
  @Inject private var injector: Injector           = _
  @Inject private var defaultEc: ExecutionContext  = _
  @Inject private var configuration: Configuration = _
  @Inject private var actorSystem: ActorSystem     = _
  lazy val get: AsyncCacheApi =
    new CaffeineCacheApi(injector.instanceOf(key))
}

private[play] class NamedSyncCacheApiProvider(key: BindingKey[AsyncCacheApi]) extends Provider[SyncCacheApi] {
  @Inject private var injector: Injector = _

  lazy val get: SyncCacheApi = {
    val async = injector.instanceOf(key)
    async.sync match {
      case sync: SyncCacheApi => sync
      case _                  => new DefaultSyncCacheApi(async)
    }
  }
}

private[play] class NamedJavaAsyncCacheApiProvider(key: BindingKey[AsyncCacheApi]) extends Provider[JavaAsyncCacheApi] {
  @Inject private var injector: Injector = _
  lazy val get: JavaAsyncCacheApi = {
    new JavaDefaultAsyncCacheApi(injector.instanceOf(key))
  }
}

private[play] class NamedJavaSyncCacheApiProvider(key: BindingKey[AsyncCacheApi]) extends Provider[JavaSyncCacheApi] {
  @Inject private var injector: Injector = _
  lazy val get: JavaSyncCacheApi =
    new SyncCacheApiAdapter(injector.instanceOf(key).sync)
}

private[play] class NamedCachedProvider(key: BindingKey[AsyncCacheApi]) extends Provider[Cached] {
  @Inject private var injector: Injector = _
  lazy val get: Cached =
    new Cached(injector.instanceOf(key))(injector.instanceOf[Materializer])
}

private[play] case class CaffeineCacheExistsException(msg: String, cause: Throwable)
    extends RuntimeException(msg, cause)

class SyncCaffeineCacheApi @Inject() (val cache: NamedCaffeineCache[Any, Any]) extends SyncCacheApi {
  private val syncCache: Cache[Any, Any] = cache.synchronous()

  override def set(key: String, value: Any, expiration: Duration): Unit = {
    syncCache.put(key, ExpirableCacheValue(value, Some(expiration)))
    Done
  }

  override def remove(key: String): Unit = syncCache.invalidate(key)

  override def getOrElseUpdate[A: ClassTag](key: String, expiration: Duration)(orElse: => A): A = {
    syncCache.get(key, _ => ExpirableCacheValue(orElse, Some(expiration))).asInstanceOf[ExpirableCacheValue[A]].value
  }

  override def get[T](key: String)(implicit ct: ClassTag[T]): Option[T] = {
    Option(syncCache.getIfPresent(key).asInstanceOf[ExpirableCacheValue[T]])
      .filter { v =>
        Primitives.wrap(ct.runtimeClass).isInstance(v.value) ||
        ct == ClassTag.Nothing || (ct == ClassTag.Unit && v.value == ((): Unit))
      }
      .map(_.value)
  }
}

/**
 * Cache implementation of [[AsyncCacheApi]]
 */
class CaffeineCacheApi @Inject() (val cache: NamedCaffeineCache[Any, Any]) extends AsyncCacheApi {
  override lazy val sync: SyncCaffeineCacheApi = new SyncCaffeineCacheApi(cache)

  def set(key: String, value: Any, expiration: Duration): Future[Done] = {
    sync.set(key, value, expiration)
    Future.successful(Done)
  }

  def get[T: ClassTag](key: String): Future[Option[T]] = {
    val resultJFuture = cache.getIfPresent(key)
    if (resultJFuture == null) Future.successful(None)
    else
      FutureConverters
        .toScala(resultJFuture)
        .map(valueFromCache => Some(valueFromCache.asInstanceOf[ExpirableCacheValue[T]].value))(trampoline)
  }

  def remove(key: String): Future[Done] = {
    sync.remove(key)
    Future.successful(Done)
  }

  def getOrElseUpdate[A: ClassTag](key: String, expiration: Duration)(orElse: => Future[A]): Future[A] = {
    lazy val orElseAsJavaFuture = FutureConverters
      .toJava(orElse.map(ExpirableCacheValue(_, Some(expiration)).asInstanceOf[Any])(trampoline))
      .toCompletableFuture

    val resultAsJavaFuture = cache.get(key, (_: Any, _: Executor) => orElseAsJavaFuture)
    FutureConverters.toScala(resultAsJavaFuture).map(_.asInstanceOf[ExpirableCacheValue[A]].value)(trampoline)
  }

  def removeAll(): Future[Done] = {
    cache.synchronous.invalidateAll
    Future.successful(Done)
  }
}
