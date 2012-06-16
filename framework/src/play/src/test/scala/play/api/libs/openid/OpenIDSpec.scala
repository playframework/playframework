package play.api.libs.openid

import org.specs2.mutable.Specification
import scala.Predef._
import org.specs2.mock.Mockito
import org.mockito._
import play.api.mvc.Request
import play.api.http.{ContentTypeOf, Writeable, HeaderNames}
import play.api.libs.openid.Errors.{BAD_RESPONSE, AUTH_ERROR}
import scala.Some

object OpenIDSpec extends Specification with Mockito {

  val claimedId = "http://example.com/openid?id=C123"
  val identity = "http://example.com/openid?id=C123&id"
  val defaultSigned = "op_endpoint,claimed_id,identity,return_to,response_nonce,assoc_handle"

  "OpenID" should {
    "initiate discovery" in {
      val ws = new WSMock
      val openId = new OpenIDClient(ws.url)
      openId.redirectURL("http://example.com", "http://foo.bar.com/openid")
      there was one(ws.request).get()
    }

    "verify the response" in {
      val ws = new WSMock

      ws.response.header(HeaderNames.CONTENT_TYPE) returns Some("text/plain")
      ws.response.body returns "is_valid:true\n" // http://openid.net/specs/openid-authentication-2_0.html#kvform

      val openId = new OpenIDClient(ws.url)

      val responseQueryString = openIdResponse
      val userInfo = openId.verifiedId(setupMockRequest(responseQueryString)).value.get

      "the claimedId must be present" in {
        userInfo.id must be equalTo claimedId
      }

      val argument = ArgumentCaptor.forClass(classOf[Params])
      "direct verification using a POST request was used" in {
        there was one(ws.request).post(argument.capture())(any[Writeable[Params]], any[ContentTypeOf[Params]])

        val verificationQuery = argument.getValue

        "openid.mode was set to check_authentication" in {
          verificationQuery.get("openid.mode") must beSome(Seq("check_authentication"))
        }

        "every query parameter apart from openid.mode is used in the verification request" in {
          (verificationQuery - "openid.mode") forall { case (key,value) => responseQueryString.get(key) == Some(value) } must beTrue
        }
      }
    }

    "fail response verification if direct verification fails" in {
      val ws = new WSMock

      ws.response.header(HeaderNames.CONTENT_TYPE) returns Some("text/plain")
      ws.response.body returns "is_valid:false\n"

      val openId = new OpenIDClient(ws.url)

      openId.verifiedId(setupMockRequest()).value.get must throwA[AUTH_ERROR.type]

      there was one(ws.request).post(any[Params])(any[Writeable[Params]], any[ContentTypeOf[Params]])
    }

    "fail response verification if the response indicates an error" in {
      val ws = new WSMock

      ws.response.header(HeaderNames.CONTENT_TYPE) returns Some("text/plain")
      ws.response.body returns "is_valid:false\n"

      val openId = new OpenIDClient(ws.url)

      val errorResponse = (openIdResponse - "openid.mode") + ("openid.mode" -> Seq("error"))

      openId.verifiedId(setupMockRequest(errorResponse)).value.get must throwA[BAD_RESPONSE.type]
    }

    // OpenID 1.1 compatibility - 14.2.1
    "verify an OpenID 1.1 response that is missing the \"openid.op_endpoint\" parameter" in {
      val ws = new WSMock

      ws.response.header(HeaderNames.CONTENT_TYPE) returns Some("text/plain")
      ws.response.body returns "is_valid:true\n" // http://openid.net/specs/openid-authentication-2_0.html#kvform

      val openId = new OpenIDClient(ws.url)

      val responseQueryString = (openIdResponse - "openid.op_endpoint")


      val userInfo = openId.verifiedId(setupMockRequest(responseQueryString)).value.get

      "the claimedId must be present" in {
        userInfo.id must be equalTo claimedId
      }

      "using discovery and direct verification" in {
        got {
          // Use discovery to resolve the endpoint
          one(ws.request).get()
          // Verify the response
          one(ws.request).post(any[Params])(any[Writeable[Params]], any[ContentTypeOf[Params]])
        }
      }
    }
  }

  def setupMockRequest(queryString:Params = openIdResponse) = {
    val request = mock[Request[_]]
    request.queryString returns queryString
    request
  }

  def openIdResponse = createDefaultResponse(claimedId, identity, defaultSigned)

}
