/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.oauth

import play.api.Application
import play.api.libs.ws.WS
import play.api.mvc._
import play.api.test._

import scala.concurrent.{ Future, Promise }

class OAuthSpec extends PlaySpecification {

  sequential

  val consumerKey = ConsumerKey("someConsumerKey", "someVerySecretConsumerSecret")
  val requestToken = RequestToken("someRequestToken", "someVerySecretRequestSecret")
  val oauthCalculator = OAuthCalculator(consumerKey, requestToken)

  "OAuth" should {

    "sign a simple get request" in {
      val (request, body, hostUrl) = receiveRequest { implicit app =>
        hostUrl =>
          WS.url(hostUrl + "/foo").sign(oauthCalculator).get()
      }
      OAuthRequestVerifier.verifyRequest(request, body, hostUrl, consumerKey, requestToken)
    }

    "sign a get request with query parameters" in {
      val (request, body, hostUrl) = receiveRequest { implicit app =>
        hostUrl =>
          WS.url(hostUrl + "/foo").withQueryString("param" -> "paramValue").sign(oauthCalculator).get()
      }
      OAuthRequestVerifier.verifyRequest(request, body, hostUrl, consumerKey, requestToken)
    }

    "sign a post request with a body" in {
      val (request, body, hostUrl) = receiveRequest { implicit app =>
        hostUrl =>
          WS.url(hostUrl + "/foo").sign(oauthCalculator).post(Map("param" -> Seq("paramValue")))
      }
      OAuthRequestVerifier.verifyRequest(request, body, hostUrl, consumerKey, requestToken)
    }
  }

  def receiveRequest(makeRequest: Application => String => Future[_]): (RequestHeader, Array[Byte], String) = {
    val hostUrl = "http://localhost:" + testServerPort
    val promise = Promise[(RequestHeader, Array[Byte])]()
    val app = FakeApplication(withRoutes = {
      case _ => Action(BodyParsers.parse.raw) { request =>
        promise.success((request, request.body.asBytes().getOrElse(Array.empty[Byte])))
        Results.Ok
      }
    })
    running(TestServer(testServerPort, app)) {
      await(makeRequest(app)(hostUrl))
    }
    val (request, body) = await(promise.future)
    (request, body, hostUrl)
  }
}

