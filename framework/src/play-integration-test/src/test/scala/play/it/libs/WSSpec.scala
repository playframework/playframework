/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.libs

import java.io.File
import java.util
import java.util.concurrent.TimeUnit

import akka.stream.scaladsl.FileIO
import akka.util.ByteString
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import org.asynchttpclient.{ RequestBuilderBase, SignatureCalculator }
import play.api.http.Port
import play.api.libs.json.JsString
import play.api.libs.oauth._
import play.api.mvc._
import play.api.test._
import play.core.server.Server
import play.it._
import play.it.tools.HttpBinApplication
import play.api.mvc.Results.Ok
import play.api.libs.streams.Accumulator
import play.api.libs.ws.StreamedBody
import play.libs.ws.WSResponse
import play.mvc.Http

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future

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

    def withEchoServer[T](block: play.libs.ws.WSClient => T) = {
      def echo = BodyParser { req =>
        import play.api.libs.concurrent.Execution.Implicits.defaultContext
        Accumulator.source[ByteString].mapFuture { source =>
          Future.successful(source).map(Right.apply)
        }
      }

      Server.withRouter() {
        case _ => Action(echo) { req =>
          Ok.chunked(req.body)
        }
      } { implicit port =>
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

    def withHeaderCheck[T](block: play.libs.ws.WSClient => T) = {
      Server.withRouter() {
        case _ => Action { req =>
          val contentLength = req.headers.get(CONTENT_LENGTH)
          val transferEncoding = req.headers.get(TRANSFER_ENCODING)
          Ok(s"Content-Length: ${contentLength.getOrElse(-1)}; Transfer-Encoding: ${transferEncoding.getOrElse(-1)}")
        }
      } { implicit port =>
        withClient(block)
      }
    }

    import play.libs.ws.WSSignatureCalculator

    "make GET Requests" in withServer { ws =>
      val req = ws.url("/get").get
      val rep = req.toCompletableFuture.get(10, TimeUnit.SECONDS) // AWait result

      rep.getStatus aka "status" must_== 200 and (
        rep.asJson.path("origin").textValue must not beNull)
    }

    "use queryString in url" in withServer { ws =>
      val rep = ws.url("/get?foo=bar").get().toCompletableFuture.get(10, TimeUnit.SECONDS)

      rep.getStatus aka "status" must_== 200 and (
        rep.asJson().path("args").path("foo").textValue() must_== "bar")
    }

    "use user:password in url" in Server.withApplication(app) { implicit port =>
      withClient { ws =>
        val rep = ws.url(s"http://user:password@localhost:$port/basic-auth/user/password").get()
          .toCompletableFuture.get(10, TimeUnit.SECONDS)

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
      val empty = ws.url("/get?foo").get.toCompletableFuture.get(10, TimeUnit.SECONDS)
      val bar = ws.url("/get?foo=bar").get.toCompletableFuture.get(10, TimeUnit.SECONDS)

      empty.asJson.path("args").path("foo").textValue() must_== "" and (
        bar.asJson.path("args").path("foo").textValue() must_== "bar")
    }

    "get a streamed response" in withResult(
      Results.Ok.chunked(Source(List("a", "b", "c")))) { ws =>
        val res = ws.url("/get").stream().toCompletableFuture.get()

        await(res.getBody().runWith(foldingSink, app.materializer)).decodeString("utf-8").
          aka("streamed response") must_== "abc"
      }

    "streaming a request body" in withEchoServer { ws =>
      val source = Source(List("a", "b", "c").map(ByteString.apply)).asJava
      val res = ws.url("/post").setMethod("POST").setBody(source).execute()
      val body = res.toCompletableFuture.get().getBody

      body must_== "abc"
    }

    "streaming a request body with manual content length" in withHeaderCheck { ws =>
      val source = Source.single(ByteString("abc")).asJava
      val res = ws.url("/post").setMethod("POST").setHeader(CONTENT_LENGTH, "3").setBody(source).execute()
      val body = res.toCompletableFuture.get().getBody

      body must_== s"Content-Length: 3; Transfer-Encoding: -1"
    }

    "sending a simple multipart form body" in withServer { ws =>
      val source = Source.single(new Http.MultipartFormData.DataPart("hello", "world")).asJava
      val res = ws.url("/post").post(source)
      val body = res.toCompletableFuture.get().asJson()

      body.path("form").path("hello").textValue() must_== "world"
    }

    "sending a multipart form body" in withServer { ws =>
      val file = new File(this.getClass.getResource("/testassets/bar.txt").toURI)
      val dp = new Http.MultipartFormData.DataPart("hello", "world")
      val fp = new Http.MultipartFormData.FilePart("upload", "bar.txt", "text/plain", FileIO.fromFile(file).asJava)
      val source = akka.stream.javadsl.Source.from(util.Arrays.asList(dp, fp))

      val res = ws.url("/post").post(source)
      val body = res.toCompletableFuture.get().asJson()

      body.path("form").path("hello").textValue() must_== "world"
      body.path("file").textValue() must_== "This is a test asset."
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
    import play.api.libs.ws.StreamedBody

    implicit val materializer = app.materializer

    val foldingSink = Sink.fold[ByteString, ByteString](ByteString.empty)((state, bs) => state ++ bs)

    def withServer[T](block: play.api.libs.ws.WSClient => T) = {
      Server.withApplication(app) { implicit port =>
        WsTestClient.withClient(block)
      }
    }

    def withEchoServer[T](block: play.api.libs.ws.WSClient => T) = {
      def echo = BodyParser { req =>
        import play.api.libs.concurrent.Execution.Implicits.defaultContext
        Accumulator.source[ByteString].mapFuture { source =>
          Future.successful(source).map(Right.apply)
        }
      }

      Server.withRouter() {
        case _ => Action(echo) { req =>
          Ok.chunked(req.body)
        }
      } { implicit port =>
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

    def withHeaderCheck[T](block: play.api.libs.ws.WSClient => T) = {
      Server.withRouter() {
        case _ => Action { req =>

          val contentLength = req.headers.get(CONTENT_LENGTH)
          val transferEncoding = req.headers.get(TRANSFER_ENCODING)
          Ok(s"Content-Length: ${contentLength.getOrElse(-1)}; Transfer-Encoding: ${transferEncoding.getOrElse(-1)}")

        }
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

    "streaming a request body" in withEchoServer { ws =>
      val source = Source(List("a", "b", "c").map(ByteString.apply))
      val res = ws.url("/post").withMethod("POST").withBody(StreamedBody(source)).execute()
      val body = await(res).body

      body must_== "abc"
    }

    "streaming a request body with manual content length" in withHeaderCheck { ws =>
      val source = Source.single(ByteString("abc"))
      val res = ws.url("/post").withMethod("POST").withHeaders(CONTENT_LENGTH -> "3").withBody(StreamedBody(source)).execute()
      val body = await(res).body

      body must_== s"Content-Length: 3; Transfer-Encoding: -1"
    }

    "send a multipart request body" in withServer { ws =>
      val file = new File(this.getClass.getResource("/testassets/foo.txt").toURI)
      val dp = MultipartFormData.DataPart("hello", "world")
      val fp = MultipartFormData.FilePart("upload", "foo.txt", None, FileIO.fromFile(file))
      val source = Source(List(dp, fp))
      val res = ws.url("/post").post(source)
      val body = await(res).json

      (body \ "form" \ "hello").toOption must beSome(JsString("world"))
      (body \ "file").toOption must beSome(JsString("This is a test asset."))
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
