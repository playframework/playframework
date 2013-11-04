package play.api.libs.ws

import org.specs2.mutable._
import org.specs2.mock.Mockito

import com.ning.http.client.{
  Response => AHCResponse,
  Cookie => AHCCookie
}
import java.util
import play.api.Configuration

object WSSpec extends Specification with Mockito {

  "WS" should {
    "support several query string values for a parameter" in {
      val req = WS.url("http://playframework.com/")
          .withQueryString("foo"->"foo1", "foo"->"foo2")
          .prepare("GET").build
       req.getQueryParams.get("foo").contains("foo1") must beTrue
       req.getQueryParams.get("foo").contains("foo2") must beTrue
       req.getQueryParams.get("foo").size must equalTo (2)
    }

    "obey a basic timeout config" in {
      val config = Some(Configuration.from(Map("ws.timeout" -> 2000)))
      WS.clientConfig(config).getConnectionTimeoutInMs must equalTo (2000)
      WS.clientConfig(config).getIdleConnectionTimeoutInMs must equalTo (2000)
    }

    "obey a detailed timeout.config" in {
      val config = Some(Configuration.from(Map(
        "ws.timeout.connection" -> 500,
        "ws.timeout.idle" -> 5000,
        "ws.timeout.request" -> 2000
      )))
      WS.clientConfig(config).getConnectionTimeoutInMs must equalTo (500)
      WS.clientConfig(config).getIdleConnectionTimeoutInMs must equalTo (5000)
      WS.clientConfig(config).getRequestTimeoutInMs must equalTo (2000)
    }
  }

  "WS Response" should {
    "get cookies from an AHC response" in {

      val ahcResponse : AHCResponse = mock[AHCResponse]
      val (domain, name, value, path, maxAge, secure) = ("example.com", "someName", "someValue", "/", 1000, false)

      val ahcCookie : AHCCookie = new AHCCookie(domain, name, value, path, maxAge, secure)
      ahcResponse.getCookies returns util.Arrays.asList(ahcCookie)

      val response = Response(ahcResponse)

      val cookies : Seq[Cookie] = response.cookies
      val cookie = cookies(0)

      cookie.domain must ===("example.com")
      cookie.name must beSome("someName")
      cookie.value must beSome("someValue")
      cookie.path must ===("/")
      cookie.maxAge must ===(1000)
      cookie.secure must beFalse
    }

    "get a single cookie from an AHC response" in {
      val ahcResponse : AHCResponse = mock[AHCResponse]
      val (domain, name, value, path, maxAge, secure) = ("example.com", "someName", "someValue", "/", 1000, false)

      val ahcCookie : AHCCookie = new AHCCookie(domain, name, value, path, maxAge, secure)
      ahcResponse.getCookies returns util.Arrays.asList(ahcCookie)

      val response = Response(ahcResponse)

      val optionCookie = response.cookie("someName")
      optionCookie must beSome[Cookie].which { cookie =>
        cookie.domain must ===("example.com")
        cookie.name must beSome("someName")
        cookie.value must beSome("someValue")
        cookie.path must ===("/")
        cookie.maxAge must ===(1000)
        cookie.secure must beFalse
      }
    }
  }
}
