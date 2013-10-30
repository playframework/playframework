package play.test

import org.specs2.mutable._
import play.mvc.Result
import scala.concurrent.Future
import play.api.mvc.{Cookie, Results, SimpleResult}

import play.api.libs.json._


/**
 *
 */
object FakeRequestSpec extends Specification {

  "FakeRequest" should {

    "Not override method in with* methods" in {

      val req = new FakeRequest("PUT", "/path")
        .withJsonBody(JsString("blah"))
        .withRawBody(JsString("blah").toString.toCharArray.map(_.toByte))
        .withTextBody(JsString("blah").toString)

      req.fake.method must be equalTo("PUT")
    }
  }
}
