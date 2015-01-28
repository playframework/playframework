package play.api.cache

import play.api.test._
import java.util.concurrent.atomic.AtomicInteger
import play.api.mvc.{Results, Action}
import play.api.http

import scala.concurrent.duration._
import scala.util.Random

import org.joda.time.DateTime

class CachedSpec extends PlaySpecification {

  "the cached action" should {
    "cache values" in new WithApplication() {
      val invoked = new AtomicInteger()
      val action =  Cached(_ => "foo")(Action(Results.Ok("" + invoked.incrementAndGet())))
      val result1 = action(FakeRequest()).run
      contentAsString(result1) must_== "1"
      invoked.get() must_== 1
      val result2 = action(FakeRequest()).run
      contentAsString(result2) must_== "1"

      // Test that the same headers are added
      header(ETAG, result2) must_== header(ETAG, result1)
      header(EXPIRES, result2) must_== header(EXPIRES, result1)

      invoked.get() must_== 1
    }

    "use etags for values" in new WithApplication() {
      val invoked = new AtomicInteger()
      val action =  Cached(_ => "foo")(Action(Results.Ok("" + invoked.incrementAndGet())))
      val result1 = action(FakeRequest()).run
      status(result1) must_== 200
      invoked.get() must_== 1
      val etag = header(ETAG, result1)
      etag must beSome(matching("""([wW]/)?"([^"]|\\")*""""))
      val result2 = action(FakeRequest().withHeaders(IF_NONE_MATCH -> etag.get)).run
      status(result2) must_== NOT_MODIFIED
      invoked.get() must_== 1
    }

    "support wildcard etags" in new WithApplication() {
      val invoked = new AtomicInteger()
      val action =  Cached(_ => "foo")(Action(Results.Ok("" + invoked.incrementAndGet())))
      val result1 = action(FakeRequest()).run
      status(result1) must_== 200
      invoked.get() must_== 1
      val result2 = action(FakeRequest().withHeaders(IF_NONE_MATCH -> "*")).run
      status(result2) must_== NOT_MODIFIED
      invoked.get() must_== 1
    }

    "work with etag cache misses" in new WithApplication() {
      val action =  Cached(_.uri)(Action(Results.Ok))
      val resultA = action(FakeRequest("GET", "/a")).run
      status(resultA) must_== 200
      status(action(FakeRequest("GET", "/a").withHeaders(IF_NONE_MATCH -> "foo")).run) must_== 200
      status(action(FakeRequest("GET", "/b").withHeaders(IF_NONE_MATCH -> header(ETAG, resultA).get)).run) must_== 200
      status(action(FakeRequest("GET", "/c").withHeaders(IF_NONE_MATCH -> "*")).run) must_== 200
    }
  }

  val dummyAction = Action { request =>
    Results.Ok{
      Random.nextInt().toString
    }
  }

  val notFoundAction = Action { request =>
    Results.NotFound(Random.nextInt().toString)
  }

  "Cached EssentialAction composition" should {
    "cache infinite ok results" in new WithApplication() {
      val cacheOk = Cached.empty{x =>
        x.uri
      }.includeStatus(200)

      val actionOk = cacheOk.build(dummyAction)
      val actionNotFound = cacheOk.build(notFoundAction)

      val res0 = contentAsString(actionOk(FakeRequest("GET", "/a")).run)
      val res1 = contentAsString(actionOk(FakeRequest("GET", "/a")).run)

      println("res0", header(EXPIRES, actionOk(FakeRequest("GET", "/a")).run))

      res0 must equalTo(res1)

      val res2 = contentAsString(actionNotFound(FakeRequest("GET", "/b")).run)
      val res3 = contentAsString(actionNotFound(FakeRequest("GET", "/b")).run)

      res2 must not equalTo(res3)
    }

    "cache everything for infinite" in new WithApplication() {
      val cache = Cached.everything{x =>
        x.uri
      }

      val actionOk = cache.build(dummyAction)
      val actionNotFound = cache.build(notFoundAction)

      val res0 = contentAsString(actionOk(FakeRequest("GET", "/a")).run)
      val res1 = contentAsString(actionOk(FakeRequest("GET", "/a")).run)

      res0 must equalTo(res1)

      val res2 = contentAsString(actionNotFound(FakeRequest("GET", "/b")).run)
      val res3 = contentAsString(actionNotFound(FakeRequest("GET", "/b")).run)

      res2 must equalTo(res3)
    }

    "cache everything one hour" in new WithApplication() {
      val cache = Cached.everything(x => x.uri, 3600)

      val actionOk = cache.build(dummyAction)
      val actionNotFound = cache.build(notFoundAction)

      val res0 = header(EXPIRES, actionOk(FakeRequest("GET", "/a")).run)
      val res1 = header(EXPIRES, actionNotFound(FakeRequest("GET", "/b")).run)

      def toDuration(header: String) = {
        val now = DateTime.now().getMillis
        val target = http.dateFormat.parseDateTime(header).getMillis
        Duration(target - now, MILLISECONDS)
      }

      val beInOneHour = beBetween(
        (Duration(1, HOURS) - Duration(10, SECONDS)).toMillis,
        Duration(1, HOURS).toMillis)

      res0.map(toDuration).map(_.toMillis) must beSome(beInOneHour)
      res1.map(toDuration).map(_.toMillis) must beSome(beInOneHour)

    }
  }

}
