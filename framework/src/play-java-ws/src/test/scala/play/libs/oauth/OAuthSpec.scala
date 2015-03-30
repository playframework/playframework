/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.oauth

import play.api.mvc._
import play.api.test._

import scala.concurrent.Promise
import play.libs.F
import play.libs.oauth.OAuth._
import play.libs.ws.WS
import play.api.libs.oauth.OAuthRequestVerifier

class OAuthSpec extends PlaySpecification {

  sequential

  val javaConsumerKey = new ConsumerKey("someConsumerKey", "someVerySecretConsumerSecret")
  val javaRequestToken = new RequestToken("someRequestToken", "someVerySecretRequestSecret")
  val oauthCalculator = new OAuthCalculator(javaConsumerKey, javaRequestToken)

  val consumerKey = play.api.libs.oauth.ConsumerKey(javaConsumerKey.key, javaConsumerKey.secret)
  val requestToken = play.api.libs.oauth.RequestToken(javaRequestToken.token, javaRequestToken.secret)

  "OAuth" should {
    "sign a simple get request" in {
      val (request, body, hostUrl) = receiveRequest { hostUrl =>
        WS.url(hostUrl + "/foo").sign(oauthCalculator).get()
      }
      OAuthRequestVerifier.verifyRequest(request, body, hostUrl, consumerKey, requestToken)
    }
    "sign a get request with query parameters" in {
      val (request, body, hostUrl) = receiveRequest { hostUrl =>
        WS.url(hostUrl + "/foo").setQueryParameter("param", "paramValue").sign(oauthCalculator).get()
      }
      OAuthRequestVerifier.verifyRequest(request, body, hostUrl, consumerKey, requestToken)
    }
    "sign a post request with a body" in {
      val (request, body, hostUrl) = receiveRequest { hostUrl =>
        WS.url(hostUrl + "/foo").sign(oauthCalculator).setContentType("application/x-www-form-urlencoded")
          .post("param=paramValue")
      }
      OAuthRequestVerifier.verifyRequest(request, body, hostUrl, consumerKey, requestToken)
    }
  }

  def receiveRequest(makeRequest: String => F.Promise[_]): (RequestHeader, Array[Byte], String) = {
    val hostUrl = "http://localhost:" + testServerPort
    val promise = Promise[(RequestHeader, Array[Byte])]()
    val app = FakeApplication(withRoutes = {
      case _ => Action(BodyParsers.parse.raw) { request =>
        promise.success((request, request.body.asBytes().getOrElse(Array.empty[Byte])))
        Results.Ok
      }
    })
    running(TestServer(testServerPort, app)) {
      makeRequest(hostUrl).get(30000l)
    }
    val (request, body) = await(promise.future)
    (request, body, hostUrl)
  }
}
