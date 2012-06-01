package play.api.libs.openid

import org.specs2.mutable.Specification
import org.specs2.mock._
import scala.io.Source
import org.jboss.netty.handler.codec.http.QueryStringDecoder
import java.net.{MalformedURLException, URL}
import util.control.Exception._
import collection.JavaConverters._
import play.api.http.HeaderNames

object DiscoverySpec extends Specification with Mockito {

  type Params = Map[String, Seq[String]]

  val ws = new WSMock
  val discovery = new Discovery(ws.url)

  private def normalize(s: String) = discovery.normalizeIdentifier(s)

  "Discovery normalization" should {
    // Adapted from org.openid4java.discovery.NormalizationTest
    // Original authors: Marius Scurtescu, Johnny Bufu
    "normalize uppercase URL identifiers" in {
      normalize("HTTP://EXAMPLE.COM/") must be equalTo "http://example.com/"
    }
    "normalize percent encoded URLs" in {
      normalize("HTTP://EXAMPLE.COM/%3d") must be equalTo "http://example.com/%3D"
      normalize("HTTP://EXAMPLE.COM/a?%3d") must be equalTo "http://example.com/a?%3D"
      normalize("HTTP://EXAMPLE.COM/a?q#%3d") must be equalTo "http://example.com/a?q#%3D"
    }.pendingUntilFixed
    "normalize percent signs" in {
      normalize("HTTP://EXAMPLE.COM/%63") must be equalTo "http://example.com/c"
    }
    "normalize port" in {
      normalize("HTTP://EXAMPLE.COM:80/A/B?Q=Z#") must be equalTo "http://example.com/A/B?Q=Z"
      normalize("https://example.com:443") must be equalTo "https://example.com/"
    }
    "normalize paths" in {
      normalize("http://example.com//a/./b/../b/c/") must be equalTo "http://example.com/a/b/c/"
      normalize("http://example.com?bla") must be equalTo "http://example.com/?bla"
    }
  }

  "Discovery normalization" should {
    // http://openid.net/specs/openid-authentication-2_0.html#normalization_example
    "normalize URLs according to he OpenID example in the spec" in {
      "A URI with a missing scheme is normalized to a http URI" in {
        normalize("example.com") must be equalTo "http://example.com/"
      }
      "An empty path component is normalized to a slash" in {
        normalize("http://example.com") must be equalTo "http://example.com/"
      }
      "https URIs remain https URIs" in {
        normalize("https://example.com/") must be equalTo "https://example.com/"
      }
      "No trailing slash is added to non-empty path components" in {
        normalize("http://example.com/user") must be equalTo "http://example.com/user"
      }
      "Trailing slashes are preserved on non-empty path components" in {
        normalize("http://example.com/user/") must be equalTo "http://example.com/user/"
      }
      "Trailing slashes are preserved when the path is empty" in {
        normalize("http://example.com/") must be equalTo "http://example.com/"
      }
    }

    "normalize URLs according to he OpenID 2.0 spec" in {
      // XRIs are currently not supported
      // 1. If the user's input starts with the "xri://" prefix, it MUST be stripped off, so that XRIs are used in the canonical form.
      // 2. If the first character of the resulting string is an XRI Global Context Symbol ("=", "@", "+", "$", "!") or "(", as defined in Section 2.2.1 of [XRI_Syntax_2.0], then the input SHOULD be treated as an XRI.

      "The input SHOULD be treated as an http URL; if it does not include a \"http\" or \"https\" scheme, the Identifier MUST be prefixed with the string \"http://\"." in {
        normalize("example.com") must be equalTo "http://example.com/"
      }

      "If the URL contains a fragment part, it MUST be stripped off together with the fragment delimiter character \"#\"." in {
        normalize("example.com#thefragment") must be equalTo "http://example.com/"
        normalize("example.com/#thefragment") must be equalTo "http://example.com/"
        normalize("http://example.com#thefragment") must be equalTo "http://example.com/"
        normalize("https://example.com/#thefragment") must be equalTo "https://example.com/"
      }
    }
  }

  "Discovery" should {

    "resolve an OpenID server via Yadis" in {
      "with a single service element" in {
        val ws = new WSMock
        ws.response.xml returns scala.xml.XML.loadString(readFixture("discovery/xrds/simple-op.xml"))
        ws.response.header(HeaderNames.CONTENT_TYPE) returns Some("application/xrds+xml")

        val returnTo = "http://foo.bar.com/openid"
        val openId = "http://abc.example.com/foo"
        val redirectUrl = new OpenIDClient(ws.url).redirectURL(openId, returnTo).value.get

        there was one(ws.request).get()
        new URL(redirectUrl).hostAndPath must be equalTo "http://openidprovider.example.com"
        verifyValidOpenIDRequest(parseQueryString(redirectUrl), openId, returnTo)
      }

      "with multiple service element" in {
        val ws = new WSMock
        ws.response.xml returns scala.xml.XML.loadString(readFixture("discovery/xrds/google-multiple-services.xml"))
        ws.response.header(HeaderNames.CONTENT_TYPE) returns Some("application/xrds+xml")

        val returnTo = "http://foo.bar.com/openid"
        val openId = "http://abc.example.com/foo"
        val redirectUrl = new OpenIDClient(ws.url).redirectURL(openId, returnTo).value.get

        there was one(ws.request).get()
        new URL(redirectUrl).hostAndPath must be startingWith "https://www.google.com/a/example.com/o8/ud"
        verifyValidOpenIDRequest(parseQueryString(redirectUrl), openId, returnTo)
      }

      "should fall back to HTML based discovery if OP Identifier cannot be found in the XRDS" in {
        val ws = new WSMock
        ws.response.body returns readFixture("discovery/html/openIDProvider.html")
        ws.response.xml returns scala.xml.XML.loadString(readFixture("discovery/xrds/invalid-op-identifier.xml"))
        ws.response.header(HeaderNames.CONTENT_TYPE) returns Some("application/xrds+xml")

        val returnTo = "http://foo.bar.com/openid"
        val openId = "http://abc.example.com/foo"
        val redirectUrl = new OpenIDClient(ws.url).redirectURL(openId, returnTo).value.get

        there was one(ws.request).get()

        new URL(redirectUrl).hostAndPath must be equalTo "https://www.example.com/openidserver/openid.server"
//        verifyValidOpenIDRequest(parseQueryString(redirectUrl), openId, returnTo)
      }.pendingUntilFixed
    }

    "resolve an OpenID server via HTML" in {

      "when given a response that includes openid meta information" in {
        val ws = new WSMock
        ws.response.body returns readFixture("discovery/html/openIDProvider.html")

        val returnTo = "http://foo.bar.com/openid"
        val openId = "http://abc.example.com/foo"
        val redirectUrl = new OpenIDClient(ws.url).redirectURL(openId, returnTo).value.get

        there was one(ws.request).get()

        new URL(redirectUrl).hostAndPath must be equalTo "https://www.example.com/openidserver/openid.server"
        verifyValidOpenIDRequest(parseQueryString(redirectUrl), openId, returnTo)
      }

      "when given a response that includes a local identifier (using openid2.local_id openid.delegate)" in {
        val ws = new WSMock
        ws.response.body returns readFixture("discovery/html/opLocalIdentityPage.html")

        val returnTo = "http://foo.bar.com/openid"
        val redirectUrl = new OpenIDClient(ws.url).redirectURL("http://example.com", returnTo).value.get

        there was one(ws.request).get()

        new URL(redirectUrl).hostAndPath must be equalTo "http://www.example.com:8080/openidserver/openid.server"
        verifyValidOpenIDRequest(parseQueryString(redirectUrl), "http://example.com/", returnTo,
          opLocalIdentifier = Some("http://exampleuser.example.com/"))
      }
    }
  }

  // See 9.1 http://openid.net/specs/openid-authentication-2_0.html#anchor27
  private def verifyValidOpenIDRequest(params: Map[String, Seq[String]],
                                       claimedId: String,
                                       returnTo: String,
                                       opLocalIdentifier: Option[String] = None,
                                       realm: Option[String] = None) = {
    "valid request parameters need to be present" in {
      params.get("openid.ns") must beSome(Seq("http://specs.openid.net/auth/2.0"))
      params.get("openid.mode") must beSome(Seq("checkid_setup"))
      params.get("openid.claimed_id") must beSome(Seq(claimedId))
      params.get("openid.return_to") must beSome(Seq(returnTo))
    }

    "realm must be handled correctly (absent if not defined)" in {
      verifyOptionalParam(params, "openid.realm", realm)
    }

    "OP-Local Identifiers must be handled correctly (if a different OP-Local Identifier is not specified, the claimed identifier MUST be used as the value for openid.identity." in {
      val value = params.get("openid.identity")
      opLocalIdentifier match {
        case Some(id) => value must beSome(Seq(id))
        case _ => value must be equalTo params.get("openid.claimed_id")
      }
    }

    "request parameters need to be absent in stateless mode" in {
      params.get("openid.assoc_handle") must beNone
    }
  }

  // Define matchers based on the expected value. Param must be absent if the expected value is None, it must match otherwise
  private def verifyOptionalParam(params: Params, key: String, expected: Option[String] = None) = expected match {
    case Some(value) => params.get(key) must beSome(Seq(value))
    case _ => params.get(key) must beNone
  }

  private def readFixture(filePath: String) = Source.fromInputStream(this.getClass.getResourceAsStream(filePath)).mkString

  private def parseQueryString(url: String): Params = {
    catching(classOf[MalformedURLException]) opt new URL(url) map {
      url =>
        new QueryStringDecoder(url.toURI.getRawQuery, false).getParameters.asScala.mapValues(_.asScala.toSeq).toMap
    } getOrElse Map()
  }
}
