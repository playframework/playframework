package play.api.cache

import play.api.test._
import java.util.concurrent.atomic.AtomicInteger
import play.api.mvc.{Results, Action}

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
      etag must beSome
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

}
