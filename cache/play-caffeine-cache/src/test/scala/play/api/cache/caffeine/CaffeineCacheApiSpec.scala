/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache.caffeine

import java.util.concurrent.Executors

import javax.inject.Inject
import javax.inject.Provider
import org.specs2.mock.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.never
import play.api.cache.AsyncCacheApi
import play.api.cache.SyncCacheApi
import play.api.inject._
import play.api.test.PlaySpecification
import play.api.test.WithApplication
import play.cache.NamedCache

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

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
        "play.cache.caffeine.caches.custom.initial-capacity"     -> 130,
        "play.cache.caffeine.caches.custom.maximum-size"         -> 50,
        "play.cache.caffeine.caches.custom.weak-keys"            -> true,
        "play.cache.caffeine.caches.custom.weak-values"          -> true,
        "play.cache.caffeine.caches.custom.record-stats"         -> true,
        "play.cache.caffeine.caches.custom-two.initial-capacity" -> 140,
        "play.cache.caffeine.caches.custom-two.soft-values"      -> true
      )
    ) {
      val caffeineCacheManager: CaffeineCacheManager = app.injector.instanceOf[CaffeineCacheManager]

      val cacheBuilderStrCustom: String    = caffeineCacheManager.getCacheBuilder("custom").toString
      val cacheBuilderStrCustomTwo: String = caffeineCacheManager.getCacheBuilder("custom-two").toString

      cacheBuilderStrCustom.contains("initialCapacity=130") must be
      cacheBuilderStrCustom.contains("maximumSize=50") must be
      cacheBuilderStrCustom.contains("keyStrength=weak") must be
      cacheBuilderStrCustom.contains("valueStrength=weak") must be

      cacheBuilderStrCustomTwo.contains("initialCapacity=140") must be
      cacheBuilderStrCustomTwo.contains("valueStrength=soft") must be
    }

    "get cache names" in new WithApplication(
      _.configure(
        "play.cache.bindCaches" -> Seq("custom")
      )
    ) {
      import scala.collection.JavaConverters._
      val caffeineCacheManager: CaffeineCacheManager = app.injector.instanceOf[CaffeineCacheManager]
      caffeineCacheManager.getCache("custom")
      caffeineCacheManager.getCache("custom-two")
      caffeineCacheManager.getCache("random")

      val caches = caffeineCacheManager.cacheNames
      caches must have size (3)
      caches must contain(exactly("custom", "custom-two", "random"))

      caffeineCacheManager.getCache("new-cache")
      val cacheNames = caffeineCacheManager.getCacheNames()
      cacheNames.asScala must have size (4)
      cacheNames.asScala must contain(exactly("custom", "custom-two", "random", "new-cache"))
    }

    "get values from cache" in new WithApplication() {
      val cacheApi     = app.injector.instanceOf[AsyncCacheApi]
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
      val cacheApi     = app.injector.instanceOf[AsyncCacheApi]
      val syncCacheApi = app.injector.instanceOf[SyncCacheApi]
      syncCacheApi.set("foo", "bar")
      Await.result(cacheApi.getOrElseUpdate[String]("foo")(Future.successful("baz")), 1.second) must_== "bar"
      syncCacheApi.remove("foo")
      Await.result(cacheApi.get("foo"), 1.second) must beNone
    }

    "remove all values from cache" in new WithApplication() {
      val cacheApi     = app.injector.instanceOf[AsyncCacheApi]
      val syncCacheApi = app.injector.instanceOf[SyncCacheApi]
      syncCacheApi.set("foo", "bar")
      Await.result(cacheApi.getOrElseUpdate[String]("foo")(Future.successful("baz")), 1.second) must_== "bar"
      Await.result(cacheApi.removeAll(), 1.second) must be(akka.Done)
      Await.result(cacheApi.get("foo"), 1.second) must beNone
    }

    "put and return the value given with orElse function if there is no value with the given key" in new WithApplication() {
      val syncCacheApi   = app.injector.instanceOf[SyncCacheApi]
      val result: String = syncCacheApi.getOrElseUpdate("aaa")("ddd")
      result mustEqual "ddd"
      val resultFromCacheMaybe = syncCacheApi.get("aaa")
      resultFromCacheMaybe must beSome("ddd")
    }

    "asynchronously put and return the value given with orElse function if there is no value with the given key" in new WithApplication() {
      val asyncCacheApi = app.injector.instanceOf[AsyncCacheApi]
      val resultFuture  = asyncCacheApi.getOrElseUpdate[String]("aaa")(Future.successful("ddd"))
      val result        = Await.result(resultFuture, 2.seconds)
      result mustEqual "ddd"
      val resultFromCacheFuture = asyncCacheApi.get("aaa")
      val resultFromCacheMaybe  = Await.result(resultFromCacheFuture, 2.seconds)
      resultFromCacheMaybe must beSome("ddd")
    }

    "expire the item after the given amount of time is passed" in new WithApplication() {
      val syncCacheApi   = app.injector.instanceOf[SyncCacheApi]
      val expiration     = 1.second
      val result: String = syncCacheApi.getOrElseUpdate("aaa", expiration)("ddd")
      result mustEqual "ddd"
      Thread.sleep(expiration.toMillis + 100) // be sure that expire duration passes
      val resultMaybe = syncCacheApi.get("aaa")
      resultMaybe must beNone
    }

    "SyncCacheApi.getOrElseUpdate method should not evaluate the orElse part if the cache contains an item with the given key" in new WithApplication() {
      val syncCacheApi = app.injector.instanceOf[SyncCacheApi]
      syncCacheApi.set("aaa", "bbb")
      trait OrElse { lazy val orElse: String = "ccc" }
      val mockOrElse = Mockito.mock[OrElse]
      val result     = syncCacheApi.getOrElseUpdate[String]("aaa")(mockOrElse.orElse)
      result mustEqual "bbb"
      verify(mockOrElse, never).orElse
    }

    "AsyncCacheApi.getOrElseUpdate method should not evaluate the orElse part if the cache contains an item with the given key" in new WithApplication() {
      val asyncCacheApi = app.injector.instanceOf[AsyncCacheApi]
      asyncCacheApi.set("aaa", "bbb")
      trait OrElse { lazy val orElse: Future[String] = Future.successful("ccc") }
      val mockOrElse   = Mockito.mock[OrElse]
      val resultFuture = asyncCacheApi.getOrElseUpdate[String]("aaa")(mockOrElse.orElse)
      val result       = Await.result(resultFuture, 2.seconds)
      result mustEqual "bbb"
      verify(mockOrElse, never).orElse
    }
  }
}

class CustomCacheManagerProvider @Inject() (cacheManagerProvider: CacheManagerProvider)
    extends Provider[CaffeineCacheManager] {
  lazy val get = {
    val mgr = cacheManagerProvider.get
    mgr
  }
}

class NamedCacheController @Inject() (
    @NamedCache("custom") val cache: SyncCacheApi,
    @NamedCache("custom") val asyncCache: AsyncCacheApi
)
