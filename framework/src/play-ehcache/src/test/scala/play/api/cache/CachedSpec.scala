/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache

import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import javax.inject._

import play.api.cache.ehcache.EhCacheApi
import play.api.mvc._
import play.api.test._
import play.api.{ Application, http }

import scala.concurrent.duration._
import scala.util.Random

class CachedSpec extends PlaySpecification {

  sequential

  def cached(implicit app: Application) = {
    new Cached(app.injector.instanceOf[AsyncCacheApi])(app.materializer)
  }

  // Tests here don't use the body
  val Action = ActionBuilder.ignoringBody

  "the cached action" should {
    "cache values using injected CachedApi" in new WithApplication() {

      val controller = app.injector.instanceOf[CachedController]
      val result1 = controller.action(FakeRequest()).run()
      contentAsString(result1) must_== "1"
      controller.invoked.get() must_== 1
      val result2 = controller.action(FakeRequest()).run()
      contentAsString(result2) must_== "1"
      controller.invoked.get() must_== 1

      // Test that the same headers are added
      header(ETAG, result2) must_== header(ETAG, result1)
      header(EXPIRES, result2) must_== header(EXPIRES, result1)
    }

    "cache values using named injected CachedApi" >> new WithApplication(
      _.configure("play.cache.bindCaches" -> Seq("custom"))
    ) {
      val controller = app.injector.instanceOf[NamedCachedController]
      val result1 = controller.action(FakeRequest()).run()
      contentAsString(result1) must_== "1"
      controller.invoked.get() must_== 1
      val result2 = controller.action(FakeRequest()).run()
      contentAsString(result2) must_== "1"
      controller.invoked.get() must_== 1

      // Test that the same headers are added
      header(ETAG, result2) must_== header(ETAG, result1)
      header(EXPIRES, result2) must_== header(EXPIRES, result1)

      // Test that the values are in the right cache
      app.injector.instanceOf[SyncCacheApi].get("foo") must beNone
      controller.isCached("foo-etag") must beTrue
    }

    "cache values to disk using injected CachedApi" in new WithApplication() {
      import net.sf.ehcache._
      import net.sf.ehcache.config._
      import net.sf.ehcache.store.MemoryStoreEvictionPolicy
      // FIXME: Do this properly
      val cacheManager = app.injector.instanceOf[CacheManager]
      val diskEhcache = new Cache(
        new CacheConfiguration("disk", 30)
          .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU)
          .eternal(false)
          .timeToLiveSeconds(60)
          .timeToIdleSeconds(30)
          .diskExpiryThreadIntervalSeconds(0)
          .persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.LOCALTEMPSWAP)))
      cacheManager.addCache(diskEhcache)
      val diskEhcache2 = cacheManager.getCache("disk")
      assert(diskEhcache2 != null)
      val diskCache = new EhCacheApi(diskEhcache2)(app.materializer.executionContext)
      val diskCached = new Cached(diskCache)
      val invoked = new AtomicInteger()
      val action = diskCached(_ => "foo")(Action(Results.Ok("" + invoked.incrementAndGet())))
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

    "cache values using Application's Cached" in new WithApplication() {

      val invoked = new AtomicInteger()
      val action = cached(app)(_ => "foo") {
        (Action(Results.Ok("" + invoked.incrementAndGet())))
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

    "use etags for values" in new WithApplication() {
      val invoked = new AtomicInteger()
      val action = cached(app)(_ => "foo")(Action(Results.Ok("" + invoked.incrementAndGet())))
      val result1 = action(FakeRequest()).run()
      status(result1) must_== 200
      invoked.get() must_== 1
      val etag = header(ETAG, result1)
      etag must beSome(matching("""([wW]/)?"([^"]|\\")*"""")) //"""
      val result2 = action(FakeRequest().withHeaders(IF_NONE_MATCH -> etag.get)).run()
      status(result2) must_== NOT_MODIFIED
      invoked.get() must_== 1
    }

    "support wildcard etags" in new WithApplication() {
      val invoked = new AtomicInteger()
      val action = cached(app)(_ => "foo")(Action(Results.Ok("" + invoked.incrementAndGet())))
      val result1 = action(FakeRequest()).run()
      status(result1) must_== 200
      invoked.get() must_== 1
      val result2 = action(FakeRequest().withHeaders(IF_NONE_MATCH -> "*")).run()
      status(result2) must_== NOT_MODIFIED
      invoked.get() must_== 1
    }

    "use etags weak comparison" in new WithApplication() {
      val invoked = new AtomicInteger()
      val action = cached(app)(_ => "foo")(Action(Results.Ok("" + invoked.incrementAndGet())))
      val result1 = action(FakeRequest()).run()
      status(result1) must_== 200
      invoked.get() must_== 1
      val etag = header(ETAG, result1).map("W/" + _)
      etag must beSome(matching("""([wW]/)?"([^"]|\\")*"""")) //"""
      val result2 = action(FakeRequest().withHeaders(IF_NONE_MATCH -> etag.get)).run()
      status(result2) must_== NOT_MODIFIED
      invoked.get() must_== 1
    }

    "work with etag cache misses" in new WithApplication() {
      val action = cached(app)(_.uri)(Action(Results.Ok))
      val resultA = action(FakeRequest("GET", "/a")).run()
      status(resultA) must_== 200
      status(action(FakeRequest("GET", "/a").withHeaders(IF_NONE_MATCH -> "\"foo\"")).run) must_== 200
      status(action(FakeRequest("GET", "/b").withHeaders(IF_NONE_MATCH -> header(ETAG, resultA).get)).run) must_== 200
      status(action(FakeRequest("GET", "/c").withHeaders(IF_NONE_MATCH -> "*")).run) must_== 200
      status(action(FakeRequest("GET", "/d").withHeaders(IF_NONE_MATCH -> "illegal")).run) must_== 200
    }
  }

  val dummyAction = Action { request: Request[_] =>
    Results.Ok(Random.nextInt().toString)
  }

  val notFoundAction = Action { request: Request[_] =>
    Results.NotFound(Random.nextInt().toString)
  }

  "Cached EssentialAction composition" should {
    "cache infinite ok results" in new WithApplication() {
      val cacheOk = cached(app).empty { x =>
        x.uri
      }.includeStatus(200)

      val actionOk = cacheOk.build(dummyAction)
      val actionNotFound = cacheOk.build(notFoundAction)

      val res0 = contentAsString(actionOk(FakeRequest("GET", "/a")).run())
      val res1 = contentAsString(actionOk(FakeRequest("GET", "/a")).run())

      // println(("res0", header(EXPIRES, actionOk(FakeRequest("GET", "/a")).run)))

      res0 must equalTo(res1)

      val res2 = contentAsString(actionNotFound(FakeRequest("GET", "/b")).run())
      val res3 = contentAsString(actionNotFound(FakeRequest("GET", "/b")).run())

      res2 must not equalTo (res3)
    }

    "cache everything for infinite" in new WithApplication() {
      val cache = cached(app).everything { x =>
        x.uri
      }

      val actionOk = cache.build(dummyAction)
      val actionNotFound = cache.build(notFoundAction)

      val res0 = contentAsString(actionOk(FakeRequest("GET", "/a")).run())
      val res1 = contentAsString(actionOk(FakeRequest("GET", "/a")).run)

      res0 must equalTo(res1)

      val res2 = contentAsString(actionNotFound(FakeRequest("GET", "/b")).run())
      val res3 = contentAsString(actionNotFound(FakeRequest("GET", "/b")).run())

      res2 must equalTo(res3)
    }

    "cache everything one hour" in new WithApplication() {
      val cache = cached(app).everything((x: RequestHeader) => x.uri, 3600)

      val actionOk = cache.build(dummyAction)
      val actionNotFound = cache.build(notFoundAction)

      val res0 = header(EXPIRES, actionOk(FakeRequest("GET", "/a")).run())
      val res1 = header(EXPIRES, actionNotFound(FakeRequest("GET", "/b")).run())

      def toDuration(header: String) = {
        val now = Instant.now().toEpochMilli
        val target = Instant.from(http.dateFormat.parse(header)).toEpochMilli
        Duration(target - now, MILLISECONDS)
      }

      val beInOneHour = beBetween(
        (Duration(1, HOURS) - Duration(10, SECONDS)).toMillis,
        Duration(1, HOURS).toMillis)

      res0.map(toDuration).map(_.toMillis) must beSome(beInOneHour)
      res1.map(toDuration).map(_.toMillis) must beSome(beInOneHour)

    }

    "cache everything for a given duration" in new WithApplication {
      val duration = 15.minutes
      val cache = cached.everything((x: RequestHeader) => x.uri, duration)

      val actionOk = cache.build(dummyAction)
      val actionNotFound = cache.build(notFoundAction)

      val res0 = header(EXPIRES, actionOk(FakeRequest("GET", "/a")).run())
      val res1 = header(EXPIRES, actionNotFound(FakeRequest("GET", "/b")).run())

      def toDuration(header: String) = {
        val now = Instant.now().toEpochMilli
        val target = Instant.from(http.dateFormat.parse(header)).toEpochMilli
        Duration(target - now, MILLISECONDS)
      }

      res0.map(toDuration) must beSome(beBetween((duration - 10.seconds), duration))
      res1.map(toDuration) must beSome(beBetween((duration - 10.seconds), duration))
    }

    "cache 200 OK results for a given duration" in new WithApplication {
      val duration = 15.minutes
      val cache = cached.status((x: RequestHeader) => x.uri, OK, duration)

      val actionOk = cache.build(dummyAction)
      val actionNotFound = cache.build(notFoundAction)

      val res0 = header(EXPIRES, actionOk(FakeRequest("GET", "/a")).run())
      val res1 = header(EXPIRES, actionNotFound(FakeRequest("GET", "/b")).run())

      def toDuration(header: String) = {
        val now = Instant.now().toEpochMilli
        val target = Instant.from(http.dateFormat.parse(header)).toEpochMilli
        Duration(target - now, MILLISECONDS)
      }

      res0.map(toDuration) must beSome(beBetween((duration - 10.seconds), duration))
      res1.map(toDuration) must beNone
    }
  }

  "AsyncCacheApi" should {
    "get items from cache" in new WithApplication() {
      val defaultCache = app.injector.instanceOf[AsyncCacheApi].sync
      defaultCache.set("foo", "bar")
      defaultCache.get[String]("foo") must beSome("bar")

      defaultCache.set("int", 31)
      defaultCache.get[Int]("int") must beSome(31)

      defaultCache.set("long", 31l)
      defaultCache.get[Long]("long") must beSome(31l)

      defaultCache.set("double", 3.14)
      defaultCache.get[Double]("double") must beSome(3.14)

      defaultCache.set("boolean", true)
      defaultCache.get[Boolean]("boolean") must beSome(true)

      defaultCache.set("unit", ())
      defaultCache.get[Unit]("unit") must beSome(())
    }

    "doesnt give items from cache with wrong type" in new WithApplication() {
      val defaultCache = app.injector.instanceOf[AsyncCacheApi].sync
      defaultCache.set("foo", "bar")
      defaultCache.set("int", 31)
      defaultCache.set("long", 31l)
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

    "get items from the cache without giving the type" in new WithApplication() {
      val defaultCache = app.injector.instanceOf[AsyncCacheApi].sync
      defaultCache.set("foo", "bar")
      defaultCache.get("foo") must beSome("bar")
      defaultCache.get[Any]("foo") must beSome("bar")

      defaultCache.set("baz", false)
      defaultCache.get("baz") must beSome(false)
      defaultCache.get[Any]("baz") must beSome(false)

      defaultCache.set("int", 31)
      defaultCache.get("int") must beSome(31)
      defaultCache.get[Any]("int") must beSome(31)

      defaultCache.set("unit", ())
      defaultCache.get("unit") must beSome(())
      defaultCache.get[Any]("unit") must beSome(())
    }
  }

  "EhCacheModule" should {
    "support binding multiple different caches" in new WithApplication(
      _.configure("play.cache.bindCaches" -> Seq("custom"))
    ) {
      val component = app.injector.instanceOf[SomeComponent]
      val defaultCache = app.injector.instanceOf[AsyncCacheApi]
      component.set("foo", "bar")
      defaultCache.sync.get("foo") must beNone
      component.get("foo") must beSome("bar")
    }
  }

}

class SomeComponent @Inject() (@NamedCache("custom") cache: AsyncCacheApi) {
  def get(key: String) = cache.sync.get[String](key)
  def set(key: String, value: String) = cache.sync.set(key, value)
}

class CachedController @Inject() (cached: Cached, c: ControllerComponents) extends AbstractController(c) {
  val invoked = new AtomicInteger()
  val action = cached(_ => "foo")(Action(Results.Ok("" + invoked.incrementAndGet())))
}

class NamedCachedController @Inject() (
    @NamedCache("custom") val cache: AsyncCacheApi,
    @NamedCache("custom") val cached: Cached,
    components: ControllerComponents
) extends AbstractController(components) {
  val invoked = new AtomicInteger()
  val action = cached(_ => "foo")(Action(Results.Ok("" + invoked.incrementAndGet())))
  def isCached(key: String): Boolean = cache.sync.get[String](key).isDefined
}
