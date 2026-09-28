/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache.ehcache

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.Executors

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import jakarta.inject.Inject
import jakarta.inject.Provider
import net.sf.ehcache.CacheManager
import play.api.cache.AsyncCacheApi
import play.api.cache.SyncCacheApi
import play.api.inject._
import play.api.test.PlaySpecification
import play.api.test.WithApplication
import play.cache.NamedCache

class EhCacheApiSpec extends PlaySpecification {
  sequential

  "SyncCacheApi" should {
    "bind named caches" in new WithApplication(
      _.configure(
        "play.cache.bindCaches" -> Seq("custom")
      )
    ) {
      override def running() = {
        val controller    = app.injector.instanceOf[NamedCacheController]
        val syncCacheName =
          controller.cache.asInstanceOf[SyncEhCacheApi].cache.getName
        val asyncCacheName =
          controller.asyncCache.asInstanceOf[EhCacheApi].cache.getName

        syncCacheName must_== "custom"
        asyncCacheName must_== "custom"
      }
    }
    "bind already created named caches" in new WithApplication(
      _.overrides(
        bind[CacheManager].toProvider[CustomCacheManagerProvider]
      ).configure(
        "play.cache.createBoundCaches" -> false,
        "play.cache.bindCaches"        -> Seq("custom")
      )
    ) {
      override def running() = {
        app.injector.instanceOf[NamedCacheController]
      }
    }
    "get values from cache" in new WithApplication() {
      override def running() = {
        val cacheApi     = app.injector.instanceOf[AsyncCacheApi]
        val syncCacheApi = app.injector.instanceOf[SyncCacheApi]
        syncCacheApi.set("foo", "bar")
        Await.result(cacheApi.getOrElseUpdate[String]("foo")(Future.successful("baz")), 1.second) must_== "bar"
        syncCacheApi.getOrElseUpdate("foo")("baz") must_== "bar"
      }
    }

    "not retain values with a non-positive expiration" in new WithApplication() {
      override def running() = {
        val asyncCacheApi = app.injector.instanceOf[AsyncCacheApi]
        val syncCacheApi  = app.injector.instanceOf[SyncCacheApi]

        Seq(Duration.Zero, -1.second, Duration.MinusInf).zipWithIndex.foreach {
          case (expiration, index) =>
            val syncSetKey = s"sync-set-$index"
            syncCacheApi.set(syncSetKey, "old")
            syncCacheApi.set(syncSetKey, "new", expiration)
            syncCacheApi.get[String](syncSetKey) must beNone

            val asyncSetKey = s"async-set-$index"
            Await.result(asyncCacheApi.set(asyncSetKey, "old"), 2.seconds)
            Await.result(asyncCacheApi.set(asyncSetKey, "new", expiration), 2.seconds)
            Await.result(asyncCacheApi.get[String](asyncSetKey), 2.seconds) must beNone

            val syncUpdateKey = s"sync-update-$index"
            syncCacheApi.getOrElseUpdate(syncUpdateKey, expiration)("value") must_== "value"
            syncCacheApi.get[String](syncUpdateKey) must beNone

            val asyncUpdateKey = s"async-update-$index"
            Await.result(
              asyncCacheApi.getOrElseUpdate[String](asyncUpdateKey, expiration)(Future.successful("value")),
              2.seconds
            ) must_== "value"
            Await.result(asyncCacheApi.get[String](asyncUpdateKey), 2.seconds) must beNone
        }
      }
    }

    "derive expiration from a newly computed value exactly once" in new WithApplication() {
      override def running() = {
        val asyncCacheApi = app.injector.instanceOf[AsyncCacheApi]
        val syncCacheApi  = app.injector.instanceOf[SyncCacheApi]

        val syncLoads       = new AtomicInteger()
        val syncExpirations = new AtomicInteger()
        val syncSeenValue   = new AtomicInteger()
        syncCacheApi.getOrElseUpdate[Int](
          "sync-derived-expiration",
          (value: Int) => {
            syncExpirations.incrementAndGet()
            syncSeenValue.set(value)
            10.seconds
          }
        )(syncLoads.incrementAndGet()) must_== 1
        syncCacheApi.getOrElseUpdate[Int](
          "sync-derived-expiration",
          (_: Int) => {
            syncExpirations.incrementAndGet()
            10.seconds
          }
        )(syncLoads.incrementAndGet()) must_== 1
        syncLoads.get() must_== 1
        syncExpirations.get() must_== 1
        syncSeenValue.get() must_== 1

        val asyncLoads       = new AtomicInteger()
        val asyncExpirations = new AtomicInteger()
        val asyncSeenValue   = new AtomicInteger()
        Await.result(
          asyncCacheApi.getOrElseUpdate[Int](
            "async-derived-expiration",
            (value: Int) => {
              asyncExpirations.incrementAndGet()
              asyncSeenValue.set(value)
              10.seconds
            }
          )(Future.successful(asyncLoads.incrementAndGet())),
          2.seconds
        ) must_== 1
        Await.result(
          asyncCacheApi.getOrElseUpdate[Int](
            "async-derived-expiration",
            (_: Int) => {
              asyncExpirations.incrementAndGet()
              10.seconds
            }
          )(Future.successful(asyncLoads.incrementAndGet())),
          2.seconds
        ) must_== 1
        asyncLoads.get() must_== 1
        asyncExpirations.get() must_== 1
        asyncSeenValue.get() must_== 1
      }
    }

    "round a positive sub-second expiration up to Ehcache's one-second resolution" in new WithApplication() {
      override def running() = {
        val cacheApi = app.injector.instanceOf[SyncCacheApi].asInstanceOf[SyncEhCacheApi]
        cacheApi.set("foo", "bar", 500.millis)

        cacheApi.get[String]("foo") must beSome("bar")
        cacheApi.cache.get("foo").getTimeToLive must_== 1
      }
    }

    "get values from cache without deadlocking" in new WithApplication(
      _.overrides(
        bind[ExecutionContext].toInstance(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1)))
      )
    ) {
      override def running() = {
        val syncCacheApi = app.injector.instanceOf[SyncCacheApi]
        syncCacheApi.set("foo", "bar")
        syncCacheApi.getOrElseUpdate[String]("foo")("baz") must_== "bar"
      }
    }

    "remove values from cache" in new WithApplication() {
      override def running() = {
        val cacheApi     = app.injector.instanceOf[AsyncCacheApi]
        val syncCacheApi = app.injector.instanceOf[SyncCacheApi]
        syncCacheApi.set("foo", "bar")
        Await.result(cacheApi.getOrElseUpdate[String]("foo")(Future.successful("baz")), 1.second) must_== "bar"
        syncCacheApi.remove("foo")
        Await.result(cacheApi.get[String]("foo"), 1.second) must beNone
      }
    }

    "remove all values from cache" in new WithApplication() {
      override def running() = {
        val cacheApi     = app.injector.instanceOf[AsyncCacheApi]
        val syncCacheApi = app.injector.instanceOf[SyncCacheApi]
        syncCacheApi.set("foo", "bar")
        Await.result(cacheApi.getOrElseUpdate[String]("foo")(Future.successful("baz")), 1.second) must_== "bar"
        Await.result(cacheApi.removeAll(), 1.second) must be(org.apache.pekko.Done)
        Await.result(cacheApi.get[String]("foo"), 1.second) must beNone
      }
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
