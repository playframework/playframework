/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.ws.ahc

import javax.cache.configuration.FactoryBuilder.SingletonFactory
import javax.cache.configuration.MutableConfiguration
import javax.cache.expiry.EternalExpiryPolicy
import javax.cache.{ CacheManager, Caching, Cache => JCache }
import javax.inject.{ Inject, Provider, Singleton }

import akka.stream.Materializer
import com.typesafe.sslconfig.ssl.SystemConfiguration
import com.typesafe.sslconfig.ssl.debug.DebugConfiguration
import play.api.inject.{ ApplicationLifecycle, SimpleModule, bind }
import play.api.libs.ws._
import play.api.libs.ws.ahc.cache._
import play.api.{ Configuration, Environment }
import play.shaded.ahc.org.asynchttpclient.{ AsyncHttpClient, DefaultAsyncHttpClient }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * A Play binding for the Scala WS API to the AsyncHTTPClient implementation.
 */
class AhcWSModule extends SimpleModule(
  bind[AsyncHttpClient].toProvider[AsyncHttpClientProvider],
  bind[WSClient].toProvider[AhcWSClientProvider]
)

/**
 * Provides an instance of AsyncHttpClient configured from the Configuration object.
 *
 * @param configuration        the Play configuration
 * @param environment          the Play environment
 * @param applicationLifecycle app lifecycle, instance is closed automatically.
 */
@Singleton
class AsyncHttpClientProvider @Inject() (
    environment: Environment,
    configuration: Configuration,
    applicationLifecycle: ApplicationLifecycle
)(implicit executionContext: ExecutionContext) extends Provider[AsyncHttpClient] {

  lazy val get: AsyncHttpClient = {
    configure()
    val cacheProvider = new OptionalAhcHttpCacheProvider(environment, configuration, applicationLifecycle)
    val client = new DefaultAsyncHttpClient(asyncHttpClientConfig)
    cacheProvider.get match {
      case Some(ahcHttpCache) =>
        new CachingAsyncHttpClient(client, ahcHttpCache)
      case None =>
        client
    }
  }
  private val wsClientConfig: WSClientConfig = {
    new WSConfigParser(configuration.underlying, environment.classLoader).parse()
  }

  private val ahcWsClientConfig: AhcWSClientConfig = {
    new AhcWSClientConfigParser(wsClientConfig, configuration.underlying, environment.classLoader).parse()
  }

  private val asyncHttpClientConfig = new AhcConfigBuilder(ahcWsClientConfig).build()

  private def configure(): Unit = {
    // JSSE depends on various system properties which must be set before JSSE classes
    // are pulled into memory, so these must come first.
    val loggerFactory = StandaloneAhcWSClient.loggerFactory
    if (wsClientConfig.ssl.debug.enabled) {
      new DebugConfiguration(loggerFactory).configure(wsClientConfig.ssl.debug)
    }
    new SystemConfiguration(loggerFactory).configure(wsClientConfig.ssl)
  }

  // Always close the AsyncHttpClient afterwards.
  applicationLifecycle.addStopHook(() =>
    Future.successful(get.close())
  )
}

/**
 * A provider of HTTP cache.
 *
 * Unfortunately this can't be bound directly through Play's DI system because
 * it doesn't support type literals (and JSR 330 doesn't support optional).
 */
@Singleton
class OptionalAhcHttpCacheProvider @Inject() (
    environment: Environment,
    configuration: Configuration,
    applicationLifecycle: ApplicationLifecycle
)(implicit executionContext: ExecutionContext) extends Provider[Option[AhcHttpCache]] {

  lazy val get: Option[AhcHttpCache] = {
    optionalCache.map { cache =>
      new AhcHttpCache(cache, cacheConfig.heuristicsEnabled)
    }
  }

  private val cacheConfig: AhcHttpCacheConfiguration = AhcHttpCacheParser.fromConfiguration(configuration)
  private val logger = play.api.Logger(getClass)

  private def optionalCache = {
    if (cacheConfig.enabled) {
      // There is no general OptionalBinder you can use -- in order to use a caching option
      // but have it seamlessly integrated into WS, you have to use something other than
      // constructor based dependency injection.
      // The good news is you can override the AhcHttpCache binding elsewhere, rather than
      // relying on the jcache binding here.
      val cacheManager: CacheManager = {
        val cachingProvider =
          cacheConfig.cachingProviderName match {
            case name if name.nonEmpty =>
              Caching.getCachingProvider(name, environment.classLoader)
            case other =>
              Caching.getCachingProvider(environment.classLoader)
          }
        cacheConfig.cacheManagerURI match {
          case uriString: String if uriString.nonEmpty =>
            cachingProvider.getCacheManager(new java.net.URI(uriString), environment.classLoader)
          case other =>
            // null means use #getDefaultURI
            cachingProvider.getCacheManager(null, environment.classLoader)
        }
      }

      Option(cacheManager).map { cm =>
        val jcache = getOrCreateCache(cm)
        applicationLifecycle.addStopHook(() => Future.successful(jcache.close()))
        new JCacheAdapter(jcache)
      }
    } else {
      None
    }
  }

  private def getOrCreateCache(cacheManager: CacheManager): JCache[EffectiveURIKey, ResponseEntry] = {
    Option {
      val cache = cacheManager.getCache[EffectiveURIKey, ResponseEntry](cacheConfig.cacheName)
      logger.trace(s"getOrCreateCache: getting ${cacheConfig.cacheName} from cacheManager $cacheManager: cache = $cache")
      cache
    }.getOrElse(createCache(cacheManager))
  }

  private def createCache(cacheManager: CacheManager): JCache[EffectiveURIKey, ResponseEntry] = {
    // If there is no preconfigured cache found, then set up a simple cache.
    val expiryPolicyFactory = {
      new SingletonFactory(new EternalExpiryPolicy())
    }

    val cacheConfiguration = new MutableConfiguration()
      .setTypes(classOf[EffectiveURIKey], classOf[ResponseEntry])
      .setStoreByValue(false)
      .setExpiryPolicyFactory(expiryPolicyFactory)

    val cache: JCache[EffectiveURIKey, ResponseEntry] = cacheManager.createCache(cacheConfig.cacheName, cacheConfiguration)
    logger.trace(s"createCache: Creating new cache ${cacheConfig.cacheName} with $cacheConfiguration: cache = $cache")
    cache
  }

  // Adapter to JCache that assumes HTTP cache only exists in memory, i.e. no blocking IO requiring a different dispatcher
  class JCacheAdapter(jcache: JCache[EffectiveURIKey, ResponseEntry]) extends Cache {
    override def get(key: EffectiveURIKey): Future[Option[ResponseEntry]] = {
      Future.successful(Option(jcache.get(key)))
    }

    override def put(key: EffectiveURIKey, entry: ResponseEntry): Future[Unit] = {
      Future.successful(jcache.put(key, entry))
    }

    override def remove(key: EffectiveURIKey): Future[Unit] = {
      Future.successful(jcache.remove(key): Unit)
    }

    override def close(): Unit = jcache.close()
  }

  case class AhcHttpCacheConfiguration(
    enabled: Boolean,
    cacheName: String,
    heuristicsEnabled: Boolean,
    cacheManagerURI: String,
    cachingProviderName: String)

  object AhcHttpCacheParser {
    def fromConfiguration(configuration: Configuration): AhcHttpCacheConfiguration = {
      AhcHttpCacheConfiguration(
        enabled = configuration.get[Boolean]("play.ws.cache.enabled"),
        cacheName = configuration.get[String]("play.ws.cache.name"),
        heuristicsEnabled = configuration.get[Boolean]("play.ws.cache.heuristics.enabled"),
        cacheManagerURI = configuration.get[String]("play.ws.cache.cacheManagerURI"),
        cachingProviderName = configuration.get[String]("play.ws.cache.cachingProviderName")
      )
    }
  }
}

/**
 * AHC provider for WSClient instance.
 */
@Singleton
class AhcWSClientProvider @Inject() (asyncHttpClient: AsyncHttpClient)(implicit materializer: Materializer)
    extends Provider[WSClient] {

  lazy val get: WSClient = {
    new AhcWSClient(new StandaloneAhcWSClient(asyncHttpClient))
  }
}
