/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.libs

import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.asynchttpclient.{ RequestBuilderBase, SignatureCalculator }
import play.api.libs.oauth._
import play.core.server.Server
import play.it.tools.HttpBinApplication

import play.api.test._
import play.api.http.{ HttpEntity, Port }
import play.api.mvc._
import play.it._
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.libs.iteratee._
import java.io.IOException

import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import akka.util.ByteString

object NettyWSSpec extends WSSpec with NettyIntegrationSpecification

object AkkaHttpWSSpec extends WSSpec with AkkaHttpIntegrationSpecification

trait WSSpec extends PlaySpecification with ServerIntegrationSpecification {

  "Web service client" title

  sequential

  def app = HttpBinApplication.app

  val foldingSink = Sink.fold[ByteString, ByteString](ByteString.empty)((state, bs) => state ++ bs)

  "WS@java" should {

    def withServer[T](block: play.libs.ws.WSClient => T) = {
      Server.withApplication(app) { implicit port =>
        withClient(block)
      }
    }

    def withResult[T](result: Result)(block: play.libs.ws.WSClient => T) = {
      Server.withRouter() {
        case _ => Action(result)
      } { implicit port =>
        withClient(block)
      }
    }

    def withClient[T](block: play.libs.ws.WSClient => T)(implicit port: Port): T = {
      val wsClient = play.libs.ws.WS.newClient(port.value)
      try {
        block(wsClient)
      } finally {
        wsClient.close()
      }
    }

    import play.libs.ws.WSSignatureCalculator

    "make GET Requests" in withServer { ws =>
      val req = ws.url("/get").get
      val rep = req.get(1000) // AWait result

      rep.getStatus aka "status" must_== 200 and (
        rep.asJson.path("origin").textValue must not beNull)
    }

    "use queryString in url" in withServer { ws =>
      val rep = ws.url("/get?foo=bar").get().get(1000)

      rep.getStatus aka "status" must_== 200 and (
        rep.asJson().path("args").path("foo").textValue() must_== "bar")
    }

    "use user:password in url" in Server.withApplication(app) { implicit port =>
      withClient { ws =>
        val rep = ws.url(s"http://user:password@localhost:$port/basic-auth/user/password").get().get(1000)

        rep.getStatus aka "status" must_== 200 and (
          rep.asJson().path("authenticated").booleanValue() must beTrue)
      }
    }

    "reject invalid query string" in withServer { ws =>
      import java.net.MalformedURLException

      ws.url("/get?=&foo").
        aka("invalid request") must throwA[RuntimeException].like {
          case e: RuntimeException =>
            e.getCause must beAnInstanceOf[MalformedURLException]
        }
    }

    "reject invalid user password string" in withServer { ws =>
      import java.net.MalformedURLException

      ws.url("http://@localhost/get").
        aka("invalid request") must throwA[RuntimeException].like {
          case e: RuntimeException =>
            e.getCause must beAnInstanceOf[MalformedURLException]
        }
    }

    "consider query string in JSON conversion" in withServer { ws =>
      val empty = ws.url("/get?foo").get.get(1000)
      val bar = ws.url("/get?foo=bar").get.get(1000)

      empty.asJson.path("args").path("foo").textValue() must_== "" and (
        bar.asJson.path("args").path("foo").textValue() must_== "bar")
    }

    "get a streamed response" in withResult(
      Results.Ok.chunked(Source(List("a", "b", "c")))) { ws =>
        val res = ws.url("/get").stream().toCompletableFuture.get()

        await(res.getBody().runWith(foldingSink, app.materializer)).decodeString("utf-8").
          aka("streamed response") must_== "abc"
      }

    class CustomSigner extends WSSignatureCalculator with org.asynchttpclient.SignatureCalculator {
      def calculateAndAddSignature(request: org.asynchttpclient.Request, requestBuilder: org.asynchttpclient.RequestBuilderBase[_]) = {
        // do nothing
      }
    }

    "not throw an exception while signing requests" in withServer { ws =>
      val key = "12234"
      val secret = "asbcdef"
      val token = "token"
      val tokenSecret = "tokenSecret"
      (ConsumerKey(key, secret), RequestToken(token, tokenSecret))

      val calc: WSSignatureCalculator = new CustomSigner

      ws.url("/").sign(calc).
        aka("signed request") must not(throwA[Exception])
    }
  }

  "WS@scala" should {
    import play.api.libs.ws.WSSignatureCalculator

    implicit val materializer = app.materializer

    def withServer[T](block: play.api.libs.ws.WSClient => T) = {
      Server.withApplication(app) { implicit port =>
        WsTestClient.withClient(block)
      }
    }

    def withResult[T](result: Result)(block: play.api.libs.ws.WSClient => T) = {
      Server.withRouter() {
        case _ => Action(result)
      } { implicit port =>
        WsTestClient.withClient(block)
      }
    }

    "make GET Requests" in withServer { ws =>
      val req = ws.url("/get").get()

      Await.result(req, Duration(1, SECONDS)).status aka "status" must_== 200
    }

    "Get 404 errors" in withServer { ws =>
      val req = ws.url("/post").get()

      Await.result(req, Duration(1, SECONDS)).status aka "status" must_== 404
    }

    "get a streamed response" in withResult(
      Results.Ok.chunked(Source(List("a", "b", "c")))) { ws =>

        val res = ws.url("/get").stream()
        val body = await(res).body

        await(body.runWith(foldingSink)).decodeString("utf-8").
          aka("streamed response") must_== "abc"
      }

    class CustomSigner extends WSSignatureCalculator with SignatureCalculator {
      def calculateAndAddSignature(request: org.asynchttpclient.Request, requestBuilder: RequestBuilderBase[_]) = {
        // do nothing
      }
    }

    "not throw an exception while signing requests" >> {
      val calc = new CustomSigner

      "without query string" in withServer { ws =>
        ws.url("/").sign(calc).get().
          aka("signed request") must not(throwA[NullPointerException])
      }

      "with query string" in withServer { ws =>
        ws.url("/").withQueryString("lorem" -> "ipsum").
          sign(calc) aka "signed request" must not(throwA[Exception])
      }
    }
  }
}
