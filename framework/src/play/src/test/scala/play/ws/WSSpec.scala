package play.ws

import org.specs2.mutable.Specification
import play.api.libs.ws.WS
import play.api.libs.ws.ResponseHeaders

object WSSpec extends Specification {

  "WS" should {
    "support several query string values for a parameter" in {
      val req = WS.url("http://playframework.org/")
          .withQueryString("foo"->"foo1", "foo"->"foo2")
          .prepare("GET").build
       req.getQueryParams.get("foo").contains("foo1") must beTrue
       req.getQueryParams.get("foo").contains("foo2") must beTrue
       req.getQueryParams.get("foo").size must equalTo (2)
    }
  }

  "ResponseHeaders" should {
    "have status with 3 digits" in {
      // RFC 1945 6.1.1 (http://www.w3.org/Protocols/rfc1945/rfc1945)
      //  states that status codes must be 3 digits
      ResponseHeaders(700, Map()) // should throw no exceptions

      ResponseHeaders(-200, Map()) must throwA[IllegalArgumentException]
      ResponseHeaders(99, Map()) must throwA[IllegalArgumentException]
      ResponseHeaders(1000, Map()) must throwA[IllegalArgumentException]
    }
  }
}