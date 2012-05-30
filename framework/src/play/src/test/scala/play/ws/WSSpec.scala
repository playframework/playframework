package play.ws

import org.specs2.mutable.Specification
import play.api.libs.ws.WS

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

}