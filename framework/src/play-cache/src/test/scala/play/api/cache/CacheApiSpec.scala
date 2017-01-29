/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.cache

import javax.inject.{ Inject, Provider }

import net.sf.ehcache.CacheManager
import play.api.cache.ehcache.CacheManagerProvider
import play.api.inject._
import play.api.test.{ PlaySpecification, WithApplication }
import scala.concurrent.duration._

import scala.concurrent.{ Future, Await }

class CacheApiSpec extends PlaySpecification {
  sequential

  "CacheApi" should {
    "bind named caches" in new WithApplication(
      _.configure(
        "play.cache.bindCaches" -> Seq("custom")
      )
    ) {
      app.injector.instanceOf[NamedCacheController]
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
  }
}

class CustomCacheManagerProvider @Inject() (cacheManagerProvider: CacheManagerProvider) extends Provider[CacheManager] {
  lazy val get = {
    val mgr = cacheManagerProvider.get
    mgr.removalAll()
    mgr.addCache("custom")
    mgr
  }
}

class NamedCacheController @Inject() (
  @NamedCache("custom") val cache: SyncCacheApi)
