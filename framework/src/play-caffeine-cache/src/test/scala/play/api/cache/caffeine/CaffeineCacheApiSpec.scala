/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache.caffeine

import java.util.concurrent.Executors
import javax.inject.{ Inject, Provider }

import play.api.cache.{ AsyncCacheApi, SyncCacheApi }
import play.api.inject._
import play.api.test.{ PlaySpecification, WithApplication }
import play.cache.NamedCache

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

class CaffeineCacheApiSpec extends PlaySpecification {
  sequential

  "CacheApi" should {
    "bind named caches" in new WithApplication(
      _.configure(
        "play.cache.bindCaches" -> Seq("custom")
      )
    ) {
      val controller = app.injector.instanceOf[NamedCacheController]
      val syncCacheName =
        controller.cache.asInstanceOf[SyncCaffeineCacheApi].cache.getName
      val asyncCacheName =
        controller.asyncCache.asInstanceOf[CaffeineCacheApi].cache.getName

      syncCacheName must_== "custom"
      asyncCacheName must_== "custom"
    }

    "configure cache builder by name" in new WithApplication(
      _.configure(
        "play.cache.caffeine.caches.custom.initial-capacity" -> 130,
        "play.cache.caffeine.caches.custom.maximum-size" -> 50,
        "play.cache.caffeine.caches.custom.weak-keys" -> true,
        "play.cache.caffeine.caches.custom.weak-values" -> true,
        "play.cache.caffeine.caches.custom.record-stats" -> true,

        "play.cache.caffeine.caches.custom-two.initial-capacity" -> 140,
        "play.cache.caffeine.caches.custom-two.soft-values" -> true
      )
    ) {
      val caffeineCacheManager: CaffeineCacheManager = app.injector.instanceOf[CaffeineCacheManager]

      val cacheBuilderStrCustom: String = caffeineCacheManager.getCacheBuilder("custom").toString
      val cacheBuilderStrCustomTwo: String = caffeineCacheManager.getCacheBuilder("custom-two").toString

      cacheBuilderStrCustom.contains("initialCapacity=130") must be
      cacheBuilderStrCustom.contains("maximumSize=50") must be
      cacheBuilderStrCustom.contains("keyStrength=weak") must be
      cacheBuilderStrCustom.contains("valueStrength=weak") must be

      cacheBuilderStrCustomTwo.contains("initialCapacity=140") must be
      cacheBuilderStrCustomTwo.contains("valueStrength=soft") must be
    }

    "get values from cache" in new WithApplication() {
      val cacheApi = app.injector.instanceOf[AsyncCacheApi]
      val syncCacheApi = app.injector.instanceOf[SyncCacheApi]
      syncCacheApi.set("foo", "bar")
      Await.result(cacheApi.getOrElseUpdate[String]("foo")(Future.successful("baz")), 1.second) must_== "bar"
      syncCacheApi.getOrElseUpdate("foo")("baz") must_== "bar"
    }

    "get values from cache without deadlocking" in new WithApplication(
      _.overrides(
        bind[ExecutionContext].toInstance(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1)))
      )
    ) {
      val syncCacheApi = app.injector.instanceOf[SyncCacheApi]
      syncCacheApi.set("foo", "bar")
      syncCacheApi.getOrElseUpdate[String]("foo")("baz") must_== "bar"
    }

    "remove values from cache" in new WithApplication() {
      val cacheApi = app.injector.instanceOf[AsyncCacheApi]
      val syncCacheApi = app.injector.instanceOf[SyncCacheApi]
      syncCacheApi.set("foo", "bar")
      Await.result(cacheApi.getOrElseUpdate[String]("foo")(Future.successful("baz")), 1.second) must_== "bar"
      syncCacheApi.remove("foo")
      Await.result(cacheApi.get("foo"), 1.second) must beNone
    }

    "remove all values from cache" in new WithApplication() {
      val cacheApi = app.injector.instanceOf[AsyncCacheApi]
      val syncCacheApi = app.injector.instanceOf[SyncCacheApi]
      syncCacheApi.set("foo", "bar")
      Await.result(cacheApi.getOrElseUpdate[String]("foo")(Future.successful("baz")), 1.second) must_== "bar"
      Await.result(cacheApi.removeAll(), 1.second) must be(akka.Done)
      Await.result(cacheApi.get("foo"), 1.second) must beNone
    }
  }
}

class CustomCacheManagerProvider @Inject() (cacheManagerProvider: CacheManagerProvider) extends Provider[CaffeineCacheManager] {
  lazy val get = {
    val mgr = cacheManagerProvider.get
    mgr
  }
}

class NamedCacheController @Inject() (
    @NamedCache("custom") val cache: SyncCacheApi,
    @NamedCache("custom") val asyncCache: AsyncCacheApi
)
