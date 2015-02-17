/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.openid

import org.specs2.mutable.Specification
import scala.Predef._
import org.specs2.mock.Mockito
import org.mockito._
import play.api.mvc.Request
import play.api.http._
import play.api.http.Status._
import play.api.libs.openid.Errors.{ BAD_RESPONSE, AUTH_ERROR }
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

object OpenIDSpec extends Specification with Mockito {

  val claimedId = "http://example.com/openid?id=C123"
  val identity = "http://example.com/openid?id=C123&id"
  val defaultSigned = "op_endpoint,claimed_id,identity,return_to,response_nonce,assoc_handle"
  val dur = Duration(10, TimeUnit.SECONDS)

  // 9.1 Request parameters - http://openid.net/specs/openid-authentication-2_0.html#anchor27
  def isValidOpenIDRequest(query: Params) = {
    query.get("openid.mode") must_== Some(Seq("checkid_setup"))
    query.get("openid.ns") must_== Some(Seq("http://specs.openid.net/auth/2.0"))
  }

  "OpenID" should {
    "initiate discovery" in {
      val ws = createMockWithValidOpDiscoveryAndVerification
      val openId = new WsOpenIdClient(ws, new WsDiscovery(ws))
      openId.redirectURL("http://example.com", "http://foo.bar.com/openid")
      there was one(ws.request).get()
    }

    "generate a valid redirectUrl" in {
      val ws = createMockWithValidOpDiscoveryAndVerification
      val openId = new WsOpenIdClient(ws, new WsDiscovery(ws))
      val redirectUrl = Await.result(openId.redirectURL("http://example.com", "http://foo.bar.com/returnto"), dur)

      val query = parseQueryString(redirectUrl)

      isValidOpenIDRequest(query)

      query.get("openid.return_to") must_== Some(Seq("http://foo.bar.com/returnto"))
      query.get("openid.realm") must beNone
    }

    "generate a valid redirectUrl with a proper required extended attributes request" in {
      val ws = createMockWithValidOpDiscoveryAndVerification
      val openId = new WsOpenIdClient(ws, new WsDiscovery(ws))
      val redirectUrl = Await.result(openId.redirectURL("http://example.com", "http://foo.bar.com/returnto",
        axRequired = Seq("email" -> "http://schema.openid.net/contact/email")), dur)

      val query = parseQueryString(redirectUrl)

      isValidOpenIDRequest(query)

      query.get("openid.ax.mode") must_== Some(Seq("fetch_request"))
      query.get("openid.ns.ax") must_== Some(Seq("http://openid.net/srv/ax/1.0"))
      query.get("openid.ax.required") must_== Some(Seq("email"))
      query.get("openid.ax.type.email") must_== Some(Seq("http://schema.openid.net/contact/email"))
    }

    "generate a valid redirectUrl with a proper 'if_available' extended attributes request" in {
      val ws = createMockWithValidOpDiscoveryAndVerification
      val openId = new WsOpenIdClient(ws, new WsDiscovery(ws))
      val redirectUrl = Await.result(openId.redirectURL("http://example.com", "http://foo.bar.com/returnto",
        axOptional = Seq("email" -> "http://schema.openid.net/contact/email")), dur)

      val query = parseQueryString(redirectUrl)

      isValidOpenIDRequest(query)

      query.get("openid.ax.mode") must_== Some(Seq("fetch_request"))
      query.get("openid.ns.ax") must_== Some(Seq("http://openid.net/srv/ax/1.0"))
      query.get("openid.ax.if_available") must_== Some(Seq("email"))
      query.get("openid.ax.type.email") must_== Some(Seq("http://schema.openid.net/contact/email"))
    }

    "generate a valid redirectUrl with a proper 'if_available' AND required extended attributes request" in {
      val ws = createMockWithValidOpDiscoveryAndVerification
      val openId = new WsOpenIdClient(ws, new WsDiscovery(ws))
      val redirectUrl = Await.result(openId.redirectURL("http://example.com", "http://foo.bar.com/returnto",
        axRequired = Seq("first" -> "http://axschema.org/namePerson/first"),
        axOptional = Seq("email" -> "http://schema.openid.net/contact/email")), dur)

      val query = parseQueryString(redirectUrl)

      isValidOpenIDRequest(query)

      query.get("openid.ax.mode") must_== Some(Seq("fetch_request"))
      query.get("openid.ns.ax") must_== Some(Seq("http://openid.net/srv/ax/1.0"))
      query.get("openid.ax.required") must_== Some(Seq("first"))
      query.get("openid.ax.type.first") must_== Some(Seq("http://axschema.org/namePerson/first"))
      query.get("openid.ax.if_available") must_== Some(Seq("email"))
      query.get("openid.ax.type.email") must_== Some(Seq("http://schema.openid.net/contact/email"))
    }

    "verify the response" in {
      val ws = createMockWithValidOpDiscoveryAndVerification

      val openId = new WsOpenIdClient(ws, new WsDiscovery(ws))

      val responseQueryString = openIdResponse
      val userInfo = Await.result(openId.verifiedId(setupMockRequest(responseQueryString)), dur)

      "the claimedId must be present" in {
        userInfo.id must be equalTo claimedId
      }

      val argument = ArgumentCaptor.forClass(classOf[Params])
      "direct verification using a POST request was used" in {
        there was one(ws.request).post(argument.capture())(any[Writeable[Params]])

        val verificationQuery = argument.getValue

        "openid.mode was set to check_authentication" in {
          verificationQuery.get("openid.mode") must_== Some(Seq("check_authentication"))
        }

        "every query parameter apart from openid.mode is used in the verification request" in {
          (verificationQuery - "openid.mode") forall { case (key, value) => responseQueryString.get(key) == Some(value) } must beTrue
        }
      }
    }

    // 11.2 If the Claimed Identifier was not previously discovered by the Relying Party
    // (the "openid.identity" in the request was "http://specs.openid.net/auth/2.0/identifier_select" or a different Identifier,
    // or if the OP is sending an unsolicited positive assertion), the Relying Party MUST perform discovery on the
    // Claimed Identifier in the response to make sure that the OP is authorized to make assertions about the Claimed Identifier.
    "verify the response using discovery on the claimed Identifier" in {
      val ws = createMockWithValidOpDiscoveryAndVerification
      val openId = new WsOpenIdClient(ws, new WsDiscovery(ws))

      val spoofedEndpoint = "http://evilhackerendpoint.com"
      val responseQueryString = openIdResponse - "openid.op_endpoint" + ("openid.op_endpoint" -> Seq(spoofedEndpoint))

      Await.result(openId.verifiedId(setupMockRequest(responseQueryString)), dur)

      "direct verification does not use the openid.op_endpoint that is part of the query string" in {
        ws.urls contains (spoofedEndpoint) must beFalse
      }
      "the endpoint is resolved using discovery on the claimed Id" in {
        ws.urls(0) must be equalTo claimedId
      }
      "use endpoint discovery and then direct verification" in {
        got {
          // Use discovery to resolve the endpoint
          one(ws.request).get()
          // Verify the response
          one(ws.request).post(any[Params])(any[Writeable[Params]])
        }
      }
      "use direct verification on the discovered endpoint" in {
        ws.urls(1) must be equalTo "https://www.google.com/a/example.com/o8/ud?be=o8" // From the mock XRDS
      }
    }

    "fail response verification if direct verification fails" in {
      val ws = new WSMock

      ws.response.status returns OK thenReturns OK
      ws.response.header(HeaderNames.CONTENT_TYPE) returns Some("application/xrds+xml") thenReturns Some("text/plain")
      ws.response.xml returns scala.xml.XML.loadString(readFixture("discovery/xrds/simple-op.xml"))
      ws.response.body returns "is_valid:false\n"

      val openId = new WsOpenIdClient(ws, new WsDiscovery(ws))

      Await.result(openId.verifiedId(setupMockRequest()), dur) must throwA[AUTH_ERROR.type]

      there was one(ws.request).post(any[Params])(any[Writeable[Params]])
    }

    "fail response verification if the response indicates an error" in {
      val ws = new WSMock

      ws.response.status returns OK thenReturns OK
      ws.response.header(HeaderNames.CONTENT_TYPE) returns Some("application/xrds+xml") thenReturns Some("text/plain")
      ws.response.xml returns scala.xml.XML.loadString(readFixture("discovery/xrds/simple-op.xml"))
      ws.response.body returns "is_valid:false\n"

      val openId = new WsOpenIdClient(ws, new WsDiscovery(ws))

      val errorResponse = (openIdResponse - "openid.mode") + ("openid.mode" -> Seq("error"))

      Await.result(openId.verifiedId(setupMockRequest(errorResponse)), dur) must throwA[BAD_RESPONSE.type]
    }

    // OpenID 1.1 compatibility - 14.2.1
    "verify an OpenID 1.1 response that is missing the \"openid.op_endpoint\" parameter" in {
      val ws = createMockWithValidOpDiscoveryAndVerification
      val openId = new WsOpenIdClient(ws, new WsDiscovery(ws))

      val responseQueryString = (openIdResponse - "openid.op_endpoint")

      val userInfo = Await.result(openId.verifiedId(setupMockRequest(responseQueryString)), dur)

      "the claimedId must be present" in {
        userInfo.id must be equalTo claimedId
      }

      "using discovery and direct verification" in {
        got {
          // Use discovery to resolve the endpoint
          one(ws.request).get()
          // Verify the response
          one(ws.request).post(any[Params])(any[Writeable[Params]])
        }
      }
    }
  }

  def createMockWithValidOpDiscoveryAndVerification = {
    val ws = new WSMock
    ws.response.status returns OK thenReturns OK
    ws.response.header(HeaderNames.CONTENT_TYPE) returns Some("application/xrds+xml") thenReturns Some("text/plain")
    ws.response.xml returns scala.xml.XML.loadString(readFixture("discovery/xrds/simple-op.xml"))
    ws.response.body returns "is_valid:true\n" // http://openid.net/specs/openid-authentication-2_0.html#kvform
    ws
  }

  def setupMockRequest(queryString: Params = openIdResponse) = {
    val request = mock[Request[_]]
    request.queryString returns queryString
    request
  }

  def openIdResponse = createDefaultResponse(claimedId, identity, defaultSigned)

}
