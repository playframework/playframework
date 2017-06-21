/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.cache.ehcache

import javax.inject.{ Inject, Provider }

import net.sf.ehcache.CacheManager
import play.api.cache.{ AsyncCacheApi, DefaultSyncCacheApi, SyncCacheApi }
import play.api.inject._
import play.api.test.{ PlaySpecification, WithApplication }
import play.cache.NamedCache

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

class EhCacheApiSpec extends PlaySpecification {
  sequential

  "CacheApi" should {
    "bind named caches" in new WithApplication(
      _.configure(
        "play.cache.bindCaches" -> Seq("custom")
      )
    ) {
      val controller = app.injector.instanceOf[NamedCacheController]
      val syncCacheName =
        controller.cache.asInstanceOf[DefaultSyncCacheApi].cacheApi.asInstanceOf[EhCacheApi].cache.getName
      val asyncCacheName =
        controller.asyncCache.asInstanceOf[EhCacheApi].cache.getName

      syncCacheName must_== "custom"
      asyncCacheName must_== "custom"
    }
    "bind already created named caches" in new WithApplication(
      _.overrides(
        bind[CacheManager].toProvider[CustomCacheManagerProvider]
      ).configure(
        "play.cache.createBoundCaches" -> false,
        "play.cache.bindCaches" -> Seq("custom")
      )
    ) {
      app.injector.instanceOf[NamedCacheController]
    }
    "get values from cache" in new WithApplication() {
      val cacheApi = app.injector.instanceOf[AsyncCacheApi]
      val syncCacheApi = app.injector.instanceOf[SyncCacheApi]
      syncCacheApi.set("foo", "bar")
      Await.result(cacheApi.getOrElseUpdate[String]("foo")(Future.successful("baz")), 1.second) must_== "bar"
      syncCacheApi.getOrElseUpdate("foo")("baz") must_== "bar"
    }

    "remove values from cache" in new WithApplication() {
      val cacheApi = app.injector.instanceOf[AsyncCacheApi]
      val syncCacheApi = app.injector.instanceOf[SyncCacheApi]
      syncCacheApi.set("foo", "bar")
      Await.result(cacheApi.getOrElseUpdate[String]("foo")(Future.successful("baz")), 1.second) must_== "bar"
      cacheApi.remove("foo")
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

class CustomCacheManagerProvider @Inject() (cacheManagerProvider: CacheManagerProvider) extends Provider[CacheManager] {
  lazy val get = {
    val mgr = cacheManagerProvider.get
    mgr.removeAllCaches()
    mgr.addCache("custom")
    mgr
  }
}

class NamedCacheController @Inject() (
  @NamedCache("custom") val cache: SyncCacheApi,
  @NamedCache("custom") val asyncCache: AsyncCacheApi
)
