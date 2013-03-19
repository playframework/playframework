package play.filters

import org.specs2.mutable.Specification
import play.api.mvc.{AsyncResult, Result, EssentialAction}
import play.api.mvc.Codec.utf_8
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers._
import concurrent.{Future, Await}
import concurrent.duration.Duration
import play.api.http.MimeTypes._
import play.api.libs.iteratee.Done
import play.api.libs.json.Json

object JsonpSpec extends Specification {

  "Jsonp filter" should {

    val filter = new Jsonp()(utf_8, play.api.libs.concurrent.Execution.Implicits.defaultContext)

    val textAction = EssentialAction(_ => Done(Ok("foo")))

    val jsonAction = EssentialAction(_ => Done(Ok(Json.obj("bar" -> "baz"))))

    val asyncAction = EssentialAction(_ => Done(AsyncResult(Future.successful(Ok(Json.obj("bar" -> "baz"))))))

    def run(uri: String)(action: EssentialAction): Result =
      Await.result(filter(action)(FakeRequest("GET", uri)).run, Duration.Inf)

    "leave non-JSON results untouched" in {
      val result = run("/")(textAction)
      contentType(result) must equalTo (Some(TEXT))
      contentAsString(result) must equalTo ("foo")
    }

    "leave JSON results untouched if there is no callback parameter in the query string" in {
      val result = run("/")(jsonAction)
      contentType(result) must equalTo (Some(JSON))
      contentAsJson(result) must equalTo (Json.obj("bar" -> "baz"))
    }

    "transform JSON results into JavaScript if there is a callback parameter in the query string" in {
      val result = run("/?callback=foo")(jsonAction)
      contentType(result) must equalTo (Some(JAVASCRIPT))
      contentAsString(result) must equalTo ("""foo({"bar":"baz"});""")
    }

    "leave non-JSON results untouched even if there is a callback parameter in the query string" in {
      val result = run("/?callback=foo")(textAction)
      contentType(result) must equalTo (Some(TEXT))
      contentAsString(result) must equalTo ("foo")
    }

    "support async results" in {
      val result = run("/?callback=foo")(asyncAction)
      contentType(result) must equalTo (Some(JAVASCRIPT))
      contentAsString(result) must equalTo ("""foo({"bar":"baz"});""")
    }
  }
}
