/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache

import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration.*
import scala.concurrent.Future
import scala.util.Random

import jakarta.inject.*
import play.api.cache.ehcache.EhCacheApi
import play.api.http
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.*
import play.api.test.*
import play.api.Application

class CachedSpec extends PlaySpecification {
  sequential

  // Tests here don't use the body
  val Action = ActionBuilder.ignoringBody

  testCached(hashResponse = true)
  testCached(hashResponse = false)
  def testCached(hashResponse: Boolean) = {
    class WithCachedApplication(
        app: Application = GuiceApplicationBuilder().configure("play.cache.hashResponse" -> hashResponse).build()
    ) extends WithApplication(app) {
      def this(builder: GuiceApplicationBuilder => GuiceApplicationBuilder) = {
        this(builder(GuiceApplicationBuilder()).build())
      }

      def cached(implicit app: Application) = new Cached(app.injector.instanceOf[AsyncCacheApi], hashResponse)(
        using app.materializer
      )
    }
    s"the cached action with hashResponse=${hashResponse}" should {
      "cache values using injected CachedApi" in new WithCachedApplication() {
        override def running() = {
          val controller = app.injector.instanceOf[CachedController]
          val result1    = controller.action(FakeRequest()).run()
          contentAsString(result1) must_== "1"
          controller.invoked.get() must_== 1
          val result2 = controller.action(FakeRequest()).run()
          contentAsString(result2) must_== "1"
          controller.invoked.get() must_== 1

          // Test that the same headers are added
          header(ETAG, result2) must_== header(ETAG, result1)
          header(EXPIRES, result2) must_== header(EXPIRES, result1)
        }
      }

      "cache values using named injected CachedApi" >> new WithCachedApplication(
        _.configure("play.cache.bindCaches" -> Seq("custom"))
      ) {
        override def running() = {
          val controller = app.injector.instanceOf[NamedCachedController]
          val result1    = controller.action(FakeRequest()).run()
          contentAsString(result1) must_== "1"
          controller.invoked.get() must_== 1
          val result2 = controller.action(FakeRequest()).run()
          contentAsString(result2) must_== "1"
          controller.invoked.get() must_== 1

          // Test that the same headers are added
          header(ETAG, result2) must_== header(ETAG, result1)
          header(EXPIRES, result2) must_== header(EXPIRES, result1)

          // Test that the values are in the right cache
          app.injector.instanceOf[SyncCacheApi].get[Any]("foo-headers") must beNone
          controller.isCached("foo-headers") must beTrue
        }
      }

      "cache values to disk using injected CachedApi" in new WithCachedApplication() {
        override def running() = {
          import net.sf.ehcache._
          import net.sf.ehcache.config._
          import net.sf.ehcache.store.MemoryStoreEvictionPolicy
          // FIXME: Do this properly
          val cacheManager = app.injector.instanceOf[CacheManager]
          val diskEhcache  = new Cache(
            new CacheConfiguration("disk", 30)
              .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU)
              .eternal(false)
              .timeToLiveSeconds(60)
              .timeToIdleSeconds(30)
              .diskExpiryThreadIntervalSeconds(0)
              .persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.LOCALTEMPSWAP))
          )
          cacheManager.addCache(diskEhcache)
          val diskEhcache2 = cacheManager.getCache("disk")
          assert(diskEhcache2 != null)
          val diskCache  = new EhCacheApi(diskEhcache2)(using app.materializer.executionContext)
          val diskCached = new Cached(diskCache)
          val invoked    = new AtomicInteger()
          val action     = diskCached(_ => "foo")(Action(Results.Ok("" + invoked.incrementAndGet())))
          val result1    = action(FakeRequest()).run()
          contentAsString(result1) must_== "1"
          invoked.get() must_== 1
          val result2 = action(FakeRequest()).run()
          contentAsString(result2) must_== "1"

          // Test that the same headers are added
          header(ETAG, result2) must_== header(ETAG, result1)
          header(EXPIRES, result2) must_== header(EXPIRES, result1)

          invoked.get() must_== 1
        }
      }

      "cache values using Application's Cached" in new WithCachedApplication() {
        override def running() = {
          val invoked = new AtomicInteger()
          val action  = cached(using app)(_ => "foo") {
            Action(Results.Ok("" + invoked.incrementAndGet()))
          }
          val result1 = action(FakeRequest()).run()
          contentAsString(result1) must_== "1"
          invoked.get() must_== 1
          val result2 = action(FakeRequest()).run()
          contentAsString(result2) must_== "1"

          // Test that the same headers are added
          header(ETAG, result2) must_== header(ETAG, result1)
          header(EXPIRES, result2) must_== header(EXPIRES, result1)

          invoked.get() must_== 1
        }
      }

      "use etags for values" in new WithCachedApplication() {
        override def running() = {
          val invoked = new AtomicInteger()
          val action  = cached(using app)(_ => "foo")(Action(Results.Ok("" + invoked.incrementAndGet())))
          val result1 = action(FakeRequest()).run()
          status(result1) must_== 200
          invoked.get() must_== 1
          val etag = header(ETAG, result1)
          etag must beSome(matching("""([wW]/)?"([^"]|\\")*"""")) // """
          val result2 = action(FakeRequest().withHeaders(IF_NONE_MATCH -> etag.get)).run()
          status(result2) must_== NOT_MODIFIED
          invoked.get() must_== 1
        }
      }

      "support wildcard etags" in new WithCachedApplication() {
        override def running() = {
          val invoked = new AtomicInteger()
          val action  = cached(using app)(_ => "foo")(Action(Results.Ok("" + invoked.incrementAndGet())))
          val result1 = action(FakeRequest()).run()
          status(result1) must_== 200
          invoked.get() must_== 1
          val result2 = action(FakeRequest().withHeaders(IF_NONE_MATCH -> "*")).run()
          status(result2) must_== NOT_MODIFIED
          invoked.get() must_== 1
        }
      }

      "use etags weak comparison" in new WithCachedApplication() {
        override def running() = {
          val invoked = new AtomicInteger()
          val action  = cached(using app)(_ => "foo")(Action(Results.Ok("" + invoked.incrementAndGet())))
          val result1 = action(FakeRequest()).run()
          status(result1) must_== 200
          invoked.get() must_== 1
          val etag = header(ETAG, result1).map("W/" + _)
          etag must beSome(matching("""([wW]/)?"([^"]|\\")*"""")) // """
          val result2 = action(FakeRequest().withHeaders(IF_NONE_MATCH -> etag.get)).run()
          status(result2) must_== NOT_MODIFIED
          invoked.get() must_== 1
        }
      }

      "work with etag cache misses" in new WithCachedApplication() {
        override def running() = {
          val action  = cached(using app)(_.uri)(Action(Results.Ok))
          val resultA = action(FakeRequest("GET", "/a")).run()
          status(resultA) must_== 200
          status(action(FakeRequest("GET", "/a").withHeaders(IF_NONE_MATCH -> "\"foo\"")).run()) must_== 200
          status(
            action(FakeRequest("GET", "/b").withHeaders(IF_NONE_MATCH -> header(ETAG, resultA).get)).run()
          ) must_== 200
          status(action(FakeRequest("GET", "/c").withHeaders(IF_NONE_MATCH -> "*")).run()) must_== 200
          status(action(FakeRequest("GET", "/d").withHeaders(IF_NONE_MATCH -> "illegal")).run()) must_== 200
        }
      }

      if (hashResponse) {
        "refresh only if content changed" in new WithCachedApplication() {
          override def running() = {
            var content = "1"
            val action  = cached(using app)(_.uri, duration = 1)(Action(Results.Ok(content)))
            val resultA = action(FakeRequest("GET", "/a")).run()
            status(resultA) must_== 200
            contentAsString(resultA) must_== content

            def useOldEtag(from: Future[Result] = resultA) =
              action(FakeRequest("GET", "/a").withHeaders(IF_NONE_MATCH -> header(ETAG, from).get)).run()

            status(useOldEtag()) must_== NOT_MODIFIED
            Thread.sleep(1500) // sleep to expire cache
            val resultAA = useOldEtag() // old etag refreshes cache
            status(resultAA) must_== 200 // same content is returned on first request
            header(ETAG, resultAA).get must_== header(ETAG, resultA).get
            header(EXPIRES, resultAA).get must_!= header(EXPIRES, resultA).get // expires is updated
            val resultAAA = useOldEtag() // subsequent requests have same etag and expires
            status(resultAAA) must_== NOT_MODIFIED
            header(ETAG, resultAA).get must_== header(ETAG, resultAAA).get
            header(EXPIRES, resultAA).get must_== header(EXPIRES, resultAAA).get
            // but updating content invalidates the old etag
            content = "2"
            Thread.sleep(1500) // sleep to expire cache
            val resultB = useOldEtag()
            status(resultB) must_== 200
            contentAsString(resultB) must_== content
            status(useOldEtag()) must_== 200
            status(useOldEtag(from = resultB)) must_== NOT_MODIFIED
          }
        }
      } else {
        "refresh even if content didn't change" in new WithCachedApplication() {
          override def running() = {
            val action  = cached(using app)(_.uri, duration = 1)(Action(Results.Ok))
            val resultA = action(FakeRequest("GET", "/a")).run()
            status(resultA) must_== 200

            def useOldEtag =
              action(FakeRequest("GET", "/a").withHeaders(IF_NONE_MATCH -> header(ETAG, resultA).get)).run()

            status(useOldEtag) must_== NOT_MODIFIED
            // sleep to ensure etag is recalculated
            Thread.sleep(1500)
            // all requests with old etag are invalidated
            status(useOldEtag) must_== 200
            status(useOldEtag) must_== 200
          }
        }
      }
    }

    val dummyAction = Action { (request: Request[?]) => Results.Ok(Random.nextInt().toString) }

    val notFoundAction = Action { (request: Request[?]) => Results.NotFound(Random.nextInt().toString) }

    s"Cached EssentialAction composition with hashResponse=${hashResponse}" should {
      "cache infinite ok results" in new WithCachedApplication() {
        override def running() = {
          val cacheOk = cached(using app)
            .empty { x => x.uri }
            .includeStatus(200)

          val actionOk       = cacheOk.build(dummyAction)
          val actionNotFound = cacheOk.build(notFoundAction)

          val res0 = contentAsString(actionOk(FakeRequest("GET", "/a")).run())
          val res1 = contentAsString(actionOk(FakeRequest("GET", "/a")).run())

          // println(("res0", header(EXPIRES, actionOk(FakeRequest("GET", "/a")).run)))

          res0 must equalTo(res1)

          val res2 = contentAsString(actionNotFound(FakeRequest("GET", "/b")).run())
          val res3 = contentAsString(actionNotFound(FakeRequest("GET", "/b")).run())

          (res2 must not).equalTo(res3)
        }
      }

      "cache everything for infinite" in new WithCachedApplication() {
        override def running() = {
          val cache = cached(using app).everything { x => x.uri }

          val actionOk       = cache.build(dummyAction)
          val actionNotFound = cache.build(notFoundAction)

          val res0 = contentAsString(actionOk(FakeRequest("GET", "/a")).run())
          val res1 = contentAsString(actionOk(FakeRequest("GET", "/a")).run())

          res0 must equalTo(res1)

          val res2 = contentAsString(actionNotFound(FakeRequest("GET", "/b")).run())
          val res3 = contentAsString(actionNotFound(FakeRequest("GET", "/b")).run())

          res2 must equalTo(res3)
        }
      }

      "cache everything one hour" in new WithCachedApplication() {
        override def running() = {
          val cache = cached(using app).everything((x: RequestHeader) => x.uri, 3600)

          val actionOk       = cache.build(dummyAction)
          val actionNotFound = cache.build(notFoundAction)

          val res0 = header(EXPIRES, actionOk(FakeRequest("GET", "/a")).run())
          val res1 = header(EXPIRES, actionNotFound(FakeRequest("GET", "/b")).run())

          def toDuration(header: String) = {
            val now    = Instant.now().toEpochMilli
            val target = Instant.from(http.dateFormat.parse(header)).toEpochMilli
            Duration(target - now, MILLISECONDS)
          }

          val beInOneHour =
            beBetween((Duration(1, HOURS) - Duration(10, SECONDS)).toMillis, Duration(1, HOURS).toMillis)

          res0.map(toDuration).map(_.toMillis) must beSome(beInOneHour)
          res1.map(toDuration).map(_.toMillis) must beSome(beInOneHour)
        }
      }

      "cache everything for a given duration" in new WithCachedApplication {
        override def running() = {
          val duration = 15.minutes
          val cache    = cached.everything((x: RequestHeader) => x.uri, duration)

          val actionOk       = cache.build(dummyAction)
          val actionNotFound = cache.build(notFoundAction)

          val res0 = header(EXPIRES, actionOk(FakeRequest("GET", "/a")).run())
          val res1 = header(EXPIRES, actionNotFound(FakeRequest("GET", "/b")).run())

          def toDuration(header: String) = {
            val now    = Instant.now().toEpochMilli
            val target = Instant.from(http.dateFormat.parse(header)).toEpochMilli
            Duration(target - now, MILLISECONDS)
          }

          res0.map(toDuration) must beSome(beBetween(duration - 10.seconds, duration))
          res1.map(toDuration) must beSome(beBetween(duration - 10.seconds, duration))
        }
      }

      "cache 200 OK results for a given duration" in new WithCachedApplication {
        override def running() = {
          val duration = 15.minutes
          val cache    = cached.status((x: RequestHeader) => x.uri, OK, duration)

          val actionOk       = cache.build(dummyAction)
          val actionNotFound = cache.build(notFoundAction)

          val res0 = header(EXPIRES, actionOk(FakeRequest("GET", "/a")).run())
          val res1 = header(EXPIRES, actionNotFound(FakeRequest("GET", "/b")).run())

          def toDuration(header: String) = {
            val now    = Instant.now().toEpochMilli
            val target = Instant.from(http.dateFormat.parse(header)).toEpochMilli
            Duration(target - now, MILLISECONDS)
          }

          res0.map(toDuration) must beSome(beBetween(duration - 10.seconds, duration))
          res1.map(toDuration) must beNone
        }
      }
    }
  }

  "AsyncCacheApi" should {
    "get items from cache" in new WithApplication() {
      override def running() = {
        val defaultCache = app.injector.instanceOf[AsyncCacheApi].sync
        defaultCache.set("foo", "bar")
        defaultCache.get[String]("foo") must beSome("bar")

        defaultCache.set("int", 31)
        defaultCache.get[Int]("int") must beSome(31)

        defaultCache.set("long", 31L)
        defaultCache.get[Long]("long") must beSome(31L)

        defaultCache.set("double", 3.14)
        defaultCache.get[Double]("double") must beSome(3.14)

        defaultCache.set("boolean", true)
        defaultCache.get[Boolean]("boolean") must beSome(true)

        defaultCache.set("unit", ())
        defaultCache.get[Unit]("unit") must beSome(())
      }
    }

    "doesnt give items from cache with wrong type" in new WithApplication() {
      override def running() = {
        val defaultCache = app.injector.instanceOf[AsyncCacheApi].sync
        defaultCache.set("foo", "bar")
        defaultCache.set("int", 31)
        defaultCache.set("long", 31L)
        defaultCache.set("double", 3.14)
        defaultCache.set("boolean", true)
        defaultCache.set("unit", ())

        defaultCache.get[Int]("foo") must beNone
        defaultCache.get[Long]("foo") must beNone
        defaultCache.get[Double]("foo") must beNone
        defaultCache.get[Boolean]("foo") must beNone
        defaultCache.get[String]("int") must beNone
        defaultCache.get[Long]("int") must beNone
        defaultCache.get[Double]("int") must beNone
        defaultCache.get[Boolean]("int") must beNone
        defaultCache.get[Unit]("foo") must beNone
        defaultCache.get[Int]("unit") must beNone
      }
    }

    "get items from the cache without giving the type" in new WithApplication() {
      override def running() = {
        val defaultCache = app.injector.instanceOf[AsyncCacheApi].sync
        defaultCache.set("foo", "bar")
        defaultCache.get[String]("foo") must beSome("bar")
        defaultCache.get[Any]("foo") must beSome("bar")

        defaultCache.set("baz", false)
        defaultCache.get[Boolean]("baz") must beSome(false)
        defaultCache.get[Any]("baz") must beSome(false)

        defaultCache.set("int", 31)
        defaultCache.get[Int]("int") must beSome(31)
        defaultCache.get[Any]("int") must beSome(31)

        defaultCache.set("unit", ())
        defaultCache.get[Unit]("unit") must beSome(())
        defaultCache.get[Any]("unit") must beSome(())
      }
    }
  }

  "EhCacheModule" should {
    "support binding multiple different caches" in new WithApplication(
      _.configure("play.cache.bindCaches" -> Seq("custom"))
    ) {
      override def running() = {
        val component    = app.injector.instanceOf[SomeComponent]
        val defaultCache = app.injector.instanceOf[AsyncCacheApi]
        component.set("foo", "bar")
        defaultCache.sync.get[String]("foo") must beNone
        component.get("foo") must beSome("bar")
      }
    }
  }
}

class SomeComponent @Inject() (@NamedCache("custom") cache: AsyncCacheApi) {
  def get(key: String)                = cache.sync.get[String](key)
  def set(key: String, value: String) = cache.sync.set(key, value)
}

class CachedController @Inject() (cached: Cached, c: ControllerComponents) extends AbstractController(c) {
  val invoked = new AtomicInteger()
  val action  = cached(_ => "foo")(Action(Results.Ok("" + invoked.incrementAndGet())))
}

class NamedCachedController @Inject() (
    @NamedCache("custom") val cache: AsyncCacheApi,
    @NamedCache("custom") val cached: Cached,
    components: ControllerComponents
) extends AbstractController(components) {
  val invoked                        = new AtomicInteger()
  val action                         = cached(_ => "foo")(Action(Results.Ok("" + invoked.incrementAndGet())))
  def isCached(key: String): Boolean = cache.sync.get[Any](key).isDefined
}
