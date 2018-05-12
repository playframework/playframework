/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache.ehcache

import javax.inject.{ Inject, Provider, Singleton }

import akka.Done
import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.common.primitives.Primitives
import net.sf.ehcache.{ CacheManager, Ehcache, Element, ObjectExistsException }
import play.api.cache._
import play.api.inject._
import play.api.{ Configuration, Environment }
import play.cache.{ NamedCacheImpl, SyncCacheApiAdapter, AsyncCacheApi => JavaAsyncCacheApi, DefaultAsyncCacheApi => JavaDefaultAsyncCacheApi, SyncCacheApi => JavaSyncCacheApi }

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
  def actorSystem: ActorSystem
  implicit def executionContext: ExecutionContext

  lazy val ehCacheManager: CacheManager = new CacheManagerProvider(environment, configuration, applicationLifecycle).get

  /**
   * Use this to create with the given name.
   */
  def cacheApi(name: String, create: Boolean = true): AsyncCacheApi = {
    val createNamedCaches = configuration.get[Boolean]("play.cache.createBoundCaches")
    val ec = configuration.get[Option[String]]("play.cache.dispatcher")
      .fold(executionContext)(actorSystem.dispatchers.lookup(_))
    new EhCacheApi(NamedEhCacheProvider.getNamedCache(name, ehCacheManager, createNamedCaches))(ec)
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
    val ehcacheKey = bind[Ehcache].qualifiedWith(namedCache)
    val cacheApiKey = bind[AsyncCacheApi].qualifiedWith(namedCache)
    Seq(
      ehcacheKey.to(new NamedEhCacheProvider(name, createBoundCaches)),
      cacheApiKey.to(new NamedAsyncCacheApiProvider(ehcacheKey))
    ) ++ wrapperBindings(cacheApiKey, namedCache)
  }

  def bindDefault[T: ClassTag]: Binding[T] = {
    bind[T].to(bind[T].qualifiedWith(named(defaultCacheName)))
  }

  Seq(
    bind[CacheManager].toProvider[CacheManagerProvider],
    // alias the default cache to the unqualified implementation
    bindDefault[AsyncCacheApi],
    bindDefault[JavaAsyncCacheApi],
    bindDefault[SyncCacheApi],
    bindDefault[JavaSyncCacheApi]
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
  def getNamedCache(name: String, manager: CacheManager, create: Boolean): Ehcache = try {
    if (create) {
      manager.addCache(name)
    }
    manager.getEhcache(name)
  } catch {
    case e: ObjectExistsException =>
      throw EhCacheExistsException(
        s"""An EhCache instance with name '$name' already exists.
           |
           |This usually indicates that multiple instances of a dependent component (e.g. a Play application) have been started at the same time.
         """.stripMargin, e)
  }
}

private[play] class NamedAsyncCacheApiProvider(key: BindingKey[Ehcache]) extends Provider[AsyncCacheApi] {
  @Inject private var injector: Injector = _
  @Inject private var defaultEc: ExecutionContext = _
  @Inject private var config: Configuration = _
  @Inject private var actorSystem: ActorSystem = _
  private lazy val ec: ExecutionContext = config.get[Option[String]]("play.cache.dispatcher").map(actorSystem.dispatchers.lookup(_)).getOrElse(defaultEc)
  lazy val get: AsyncCacheApi =
    new EhCacheApi(injector.instanceOf(key))(ec)
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
  lazy val get: JavaAsyncCacheApi =
    new JavaDefaultAsyncCacheApi(injector.instanceOf(key))
}

private[play] class NamedJavaSyncCacheApiProvider(key: BindingKey[AsyncCacheApi]) extends Provider[JavaSyncCacheApi] {
  @Inject private var injector: Injector = _
  lazy val get: JavaSyncCacheApi = new SyncCacheApiAdapter(injector.instanceOf(key).sync)
}

private[play] class NamedCachedProvider(key: BindingKey[AsyncCacheApi]) extends Provider[Cached] {
  @Inject private var injector: Injector = _
  lazy val get: Cached =
    new Cached(injector.instanceOf(key))(injector.instanceOf[Materializer])
}

private[play] case class EhCacheExistsException(msg: String, cause: Throwable) extends RuntimeException(msg, cause)

class SyncEhCacheApi @Inject() (private[ehcache] val cache: Ehcache) extends SyncCacheApi {

  override def set(key: String, value: Any, expiration: Duration): Unit = {
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
    Done
  }

  override def remove(key: String): Unit = cache.remove(key)

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
    Option(cache.get(key)).map(_.getObjectValue).filter { v =>
      Primitives.wrap(ct.runtimeClass).isInstance(v) ||
        ct == ClassTag.Nothing || (ct == ClassTag.Unit && v == ((): Unit))
    }.asInstanceOf[Option[T]]
  }
}

/**
 * Ehcache implementation of [[AsyncCacheApi]]. Since Ehcache is synchronous by default, this uses [[SyncEhCacheApi]].
 */
class EhCacheApi @Inject() (private[ehcache] val cache: Ehcache)(implicit context: ExecutionContext) extends AsyncCacheApi {

  override lazy val sync: SyncEhCacheApi = new SyncEhCacheApi(cache)

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
    cache.removeAll()
    Done
  }
}
