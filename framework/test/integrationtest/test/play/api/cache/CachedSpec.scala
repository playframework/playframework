package play.api.cache

import org.specs2.mutable.Specification

import play.api.test._
import play.api.test.Helpers._
import java.util.concurrent.TimeUnit
import scala.concurrent.Promise

object CachedSpec extends Specification {

  "Cached combinator" should {

    "Cache result on server-side and client-side" in new WithApplication() {

      import play.api.Play.current
      import play.api.mvc.Results.Ok
      import play.api.mvc.Action
      import scala.concurrent.ExecutionContext.Implicits.global

      var count = 0
      val action = Action {
        count = count + 1
        Ok(count.toString)
      }

      val cachedAction = Cached(_.uri, 1)(action)

      val request = FakeRequest(GET, "/")

      val result1 = cachedAction(request).run

      // Result has been cached, so we’ll get the same as `result1`
      val result2 = cachedAction(request).run
      contentAsString(result2) must equalTo (contentAsString(result1))
      header(EXPIRES, result1) must equalTo (header(EXPIRES, result2))

      // Ask for freshness
      val etag = header(ETAG, result1).get
      val result3 = cachedAction(request.withHeaders(IF_NONE_MATCH -> etag)).run
      status(result3) must equalTo (NOT_MODIFIED)

      // Let the resource expire
      Thread.sleep(1000)

      // Now we get a new result
      val result4 = cachedAction(request).run
      contentAsString(result4) must not equalTo (contentAsString(result1))

      // Even using `If-None-Match`, we get the new version of the resource
      val result5 = cachedAction(request.withHeaders(IF_NONE_MATCH -> etag)).run
      status(result5) must not equalTo (NOT_MODIFIED)
      contentAsString(result5) must equalTo (contentAsString(result4))
    }

  }

}
