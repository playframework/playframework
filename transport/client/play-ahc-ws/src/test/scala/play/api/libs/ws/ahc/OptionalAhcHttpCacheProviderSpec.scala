/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.ws.ahc

import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider
import org.ehcache.jcache.JCacheCachingProvider
import org.specs2.concurrent.ExecutionEnv
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.DefaultApplicationLifecycle
import play.api.libs.ws.ahc.cache.AhcHttpCache
import play.api.test.PlaySpecification
import play.api.test.WithApplication
import play.api.Configuration
import play.api.Environment

/**
 * Runs through the AHC cache provider.
 */
class OptionalAhcHttpCacheProviderSpec(implicit ee: ExecutionEnv) extends PlaySpecification {
  "OptionalAhcHttpCacheProvider" should {
    "work with default (cache disabled)" in {
      val environment          = play.api.Environment.simple()
      val configuration        = play.api.Configuration.reference
      val applicationLifecycle = new DefaultApplicationLifecycle
      val provider             = new OptionalAhcHttpCacheProvider(environment, configuration, applicationLifecycle)
      provider.get must beNone
    }

    "work with a cache defined using ehcache through jcache" in new WithApplication(
      GuiceApplicationBuilder(loadConfiguration = { (env: Environment) =>
        val settings = Map(
          "play.ws.cache.enabled"              -> "true",
          "play.ws.cache.cachingProviderName"  -> classOf[JCacheCachingProvider].getName,
          "play.ws.cache.cacheManagerResource" -> "ehcache-play-ws-cache.xml"
        )
        Configuration.load(env, settings)
      }).build()
    ) {
      val provider = app.injector.instanceOf[OptionalAhcHttpCacheProvider]
      provider.get must beSome[AhcHttpCache].which { cache => cache.isShared must beFalse }
    }

    "work with a cache defined using caffeine through jcache" in new WithApplication(
      GuiceApplicationBuilder(loadConfiguration = { (env: Environment) =>
        val settings = Map(
          "play.ws.cache.enabled"              -> "true",
          "play.ws.cache.cachingProviderName"  -> classOf[CaffeineCachingProvider].getName,
          "play.ws.cache.cacheManagerResource" -> "caffeine.conf"
        )
        Configuration.load(env, settings)
      }).build()
    ) {
      val provider = app.injector.instanceOf[OptionalAhcHttpCacheProvider]
      provider.get must beSome[AhcHttpCache].which { cache => cache.isShared must beFalse }
    }
  }
}
