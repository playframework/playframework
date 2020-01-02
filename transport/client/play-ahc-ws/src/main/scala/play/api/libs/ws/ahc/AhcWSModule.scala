/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.ws.ahc

import java.net.URI
import javax.cache.configuration.FactoryBuilder.SingletonFactory
import javax.cache.configuration.MutableConfiguration
import javax.cache.expiry.EternalExpiryPolicy
import javax.cache.CacheManager
import javax.cache.Caching
import javax.cache.{ Cache => JCache }
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import akka.stream.Materializer
import play.api.inject.ApplicationLifecycle
import play.api.inject.SimpleModule
import play.api.inject.bind
import play.api.libs.ws._
import play.api.libs.ws.ahc.cache._
import play.api.Configuration
import play.api.Environment
import play.api.Logger
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 * A Play binding for the Scala WS API to the AsyncHTTPClient implementation.
 */
class AhcWSModule
    extends SimpleModule(
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
)(implicit executionContext: ExecutionContext)
    extends Provider[AsyncHttpClient] {
  lazy val get: AsyncHttpClient = {
    val cacheProvider = new OptionalAhcHttpCacheProvider(environment, configuration, applicationLifecycle)
    val client        = new DefaultAsyncHttpClient(asyncHttpClientConfig)
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

  // Always close the AsyncHttpClient afterwards.
  applicationLifecycle.addStopHook(() => Future.successful(get.close()))
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
)(implicit executionContext: ExecutionContext)
    extends Provider[Option[AhcHttpCache]] {
  lazy val get: Option[AhcHttpCache] = {
    optionalCache.map { cache =>
      new AhcHttpCache(cache, cacheConfig.heuristicsEnabled)
    }
  }

  private val cacheConfig: AhcHttpCacheConfiguration = AhcHttpCacheParser.fromConfiguration(configuration)
  private val logger                                 = play.api.Logger(getClass)

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
            case _ =>
              Caching.getCachingProvider(environment.classLoader)
          }
        val cacheManagerURI =
          cacheConfig.cacheManagerURI match {
            case uriString if uriString.nonEmpty => new URI(uriString)
            // null means use #getDefaultURI
            case _ => null
          }
        cachingProvider.getCacheManager(cacheManagerURI, environment.classLoader)
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
      logger.trace(
        s"getOrCreateCache: getting ${cacheConfig.cacheName} from cacheManager $cacheManager: cache = $cache"
      )
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

    val cache: JCache[EffectiveURIKey, ResponseEntry] =
      cacheManager.createCache(cacheConfig.cacheName, cacheConfiguration)
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
      cachingProviderName: String
  )

  object AhcHttpCacheParser {
    // For the sake of compatibility, parse both cacheManagerResource and cacheManagerURI, use cacheManagerURI first.
    // Treat cacheManagerResource as a resource on the classpath and convert it to an URI string.
    def fromConfiguration(configuration: Configuration): AhcHttpCacheConfiguration = {
      val cacheManagerURI = configuration.get[Option[String]]("play.ws.cache.cacheManagerURI") match {
        case Some(uriString) =>
          Logger(AhcHttpCacheParser.getClass)
            .warn(
              "play.ws.cache.cacheManagerURI is deprecated, use play.ws.cache.cacheManagerResource with a path on the classpath instead."
            )
          uriString
        case None =>
          configuration
            .get[Option[String]]("play.ws.cache.cacheManagerResource")
            .filter(_.nonEmpty)
            .flatMap(environment.resource)
            .map(_.toURI.toString)
            .getOrElse("")
      }
      AhcHttpCacheConfiguration(
        enabled = configuration.get[Boolean]("play.ws.cache.enabled"),
        cacheName = configuration.get[String]("play.ws.cache.name"),
        heuristicsEnabled = configuration.get[Boolean]("play.ws.cache.heuristics.enabled"),
        cacheManagerURI = cacheManagerURI,
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
