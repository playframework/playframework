/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.oauth

import java.util.concurrent.CompletionStage

import akka.util.ByteString
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test._

import scala.concurrent.Promise
import play.libs.oauth.OAuth._
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
      val (request, body, hostUrl) = receiveRequest { (client, hostUrl) =>
        client.url(hostUrl + "/foo").sign(oauthCalculator).get()
      }
      OAuthRequestVerifier.verifyRequest(request, body, hostUrl, consumerKey, requestToken)
    }

    "sign a get request with query parameters" in {
      val (request, body, hostUrl) = receiveRequest { (client, hostUrl) =>
        client.url(hostUrl + "/foo").setQueryParameter("param", "paramValue").sign(oauthCalculator).get()
      }
      OAuthRequestVerifier.verifyRequest(request, body, hostUrl, consumerKey, requestToken)
    }

    "sign a post request with a body" in {
      val (request, body, hostUrl) = receiveRequest { (client, hostUrl) =>
        client.url(hostUrl + "/foo").sign(oauthCalculator).setContentType("application/x-www-form-urlencoded")
          .post("param=paramValue")
      }
      OAuthRequestVerifier.verifyRequest(request, body, hostUrl, consumerKey, requestToken)
    }
  }

  def receiveRequest(makeRequest: (play.libs.ws.WSClient, String) => CompletionStage[_]): (RequestHeader, ByteString, String) = {
    val hostUrl = "http://localhost:" + testServerPort
    val promise = Promise[(RequestHeader, ByteString)]()
    val app = GuiceApplicationBuilder().routes {
      case _ => Action(BodyParsers.parse.raw) { request =>
        promise.success((request, request.body.asBytes().getOrElse(ByteString.empty)))
        Results.Ok
      }
    }.build()
    running(TestServer(testServerPort, app)) {
      val client = app.injector.instanceOf(classOf[play.libs.ws.WSClient])
      makeRequest(client, hostUrl).toCompletableFuture.get()
    }
    val (request, body) = await(promise.future)
    (request, body, hostUrl)
  }
}
