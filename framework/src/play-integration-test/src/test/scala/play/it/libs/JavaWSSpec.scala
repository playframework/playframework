/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.libs

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.{ Charset, StandardCharsets }
import java.util
import java.util.concurrent.{ CompletionStage, TimeUnit }

import akka.NotUsed
import akka.stream.javadsl
import akka.stream.scaladsl.{ FileIO, Sink, Source }
import akka.util.ByteString
import org.specs2.concurrent.{ ExecutionEnv, FutureAwait }
import play.api.http.Port
import play.api.libs.oauth.{ ConsumerKey, RequestToken }
import play.api.libs.streams.Accumulator
import play.api.mvc.{ BodyParser, Result, Results }
import play.api.mvc.Results.Ok
import play.api.test.PlaySpecification
import play.core.server.Server
import play.it.tools.HttpBinApplication
import play.it.{ AkkaHttpIntegrationSpecification, NettyIntegrationSpecification, ServerIntegrationSpecification }
import play.libs.ws.{ WSBodyReadables, WSBodyWritables, WSRequest, WSResponse }
import play.mvc.Http

import scala.concurrent.Future

class NettyJavaWSSpec(val ee: ExecutionEnv) extends JavaWSSpec with NettyIntegrationSpecification

class AkkaHttpJavaWSSpec(val ee: ExecutionEnv) extends JavaWSSpec with AkkaHttpIntegrationSpecification

trait JavaWSSpec extends PlaySpecification with ServerIntegrationSpecification with FutureAwait with WSBodyReadables with WSBodyWritables {

  def ee: ExecutionEnv
  implicit val ec = ee.executionContext

  import play.libs.ws.WSSignatureCalculator

  "Web service client" title

  sequential

  "WSClient@java" should {

    "make GET Requests" in withServer { ws =>
      val request: WSRequest = ws.url("/get")
      val futureResponse: CompletionStage[WSResponse] = request.get()
      val future = futureResponse.toCompletableFuture
      val rep: WSResponse = future.get(10, TimeUnit.SECONDS)

      (rep.getStatus() aka "status" must_== 200) and (rep.asJson().path("origin").textValue must not beNull)
    }

    "make DELETE Requests" in withServer { ws =>
      val request: WSRequest = ws.url("/delete")
      val futureResponse: CompletionStage[WSResponse] = request.execute("DELETE")
      val future = futureResponse.toCompletableFuture
      val rep: WSResponse = future.get(10, TimeUnit.SECONDS)

      (rep.getStatus aka "status" must_== 200) and (rep.asJson().path("origin").textValue must not beNull)
    }

    "use queryString in url" in withServer { ws =>
      val rep = ws.url("/get?foo=bar").get().toCompletableFuture.get(10, TimeUnit.SECONDS)

      (rep.getStatus aka "status" must_== 200) and (
        rep.asJson().path("args").path("foo").textValue() must_== "bar")
    }

    "use user:password in url" in Server.withApplication(app) { implicit port =>
      withClient { ws =>
        val rep = ws.url(s"http://user:password@localhost:$port/basic-auth/user/password").get()
          .toCompletableFuture.get(10, TimeUnit.SECONDS)

        (rep.getStatus aka "status" must_== 200) and (
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

      (empty.asJson.path("args").path("foo").textValue() must_== "") and (
        bar.asJson.path("args").path("foo").textValue() must_== "bar")
    }

    "get a streamed response" in withResult(
      Results.Ok.chunked(Source(List("a", "b", "c")))) { ws =>
        val res = ws.url("/get").stream().toCompletableFuture.get()

        val materializedData = await(res.getBodyAsSource().runWith(foldingSink, app.materializer))

        materializedData.decodeString("utf-8").
          aka("streamed response") must_== "abc"
      }

    "streaming a request body" in withEchoServer { ws =>
      val source = Source(List("a", "b", "c").map(ByteString.apply)).asJava
      val res = ws.url("/post").setMethod("POST").setBody(source).execute()
      val body = res.toCompletableFuture.get().getBody

      body must_== "abc"
    }

    "streaming a request body with manual content length" in withHeaderCheck { ws =>
      val source = akka.stream.javadsl.Source.single(ByteString("abc"))
      val res = ws.url("/post").setMethod("POST").addHeader(CONTENT_LENGTH, "3").setBody(source).execute()
      val body = res.toCompletableFuture.get().getBody

      body must_== s"Content-Length: 3; Transfer-Encoding: -1"
    }

    "sending a simple multipart form body" in withServer { ws =>
      val source = Source.single(new Http.MultipartFormData.DataPart("hello", "world")).asJava[Http.MultipartFormData.Part[javadsl.Source[ByteString, _]], NotUsed]
      val res = ws.url("/post").post(source)
      val body = res.toCompletableFuture.get().asJson()

      body.path("form").path("hello").textValue() must_== "world"
    }

    "sending a multipart form body" in withServer { ws =>
      val file = new File(this.getClass.getResource("/testassets/bar.txt").toURI).toPath
      val dp = new Http.MultipartFormData.DataPart("hello", "world")
      val fp = new Http.MultipartFormData.FilePart("upload", "bar.txt", "text/plain", FileIO.fromPath(file).asJava)
      val source = akka.stream.javadsl.Source.from(util.Arrays.asList(dp, fp))

      val res = ws.url("/post").post(source)
      val body = res.toCompletableFuture.get().asJson()

      body.path("form").path("hello").textValue() must_== "world"
      body.path("file").textValue() must_== "This is a test asset."
    }

    "send a multipart request body via multipartBody()" in withServer { ws =>
      val file = new File(this.getClass.getResource("/testassets/bar.txt").toURI)
      val dp = new Http.MultipartFormData.DataPart("hello", "world")
      val fp = new Http.MultipartFormData.FilePart("upload", "bar.txt", "text/plain", FileIO.fromPath(file.toPath).asJava)
      val source = akka.stream.javadsl.Source.from(util.Arrays.asList(dp, fp))

      val res = ws.url("/post").setBody(multipartBody(source)).setMethod("POST").execute()
      val body = res.toCompletableFuture.get().asJson()

      body.path("form").path("hello").textValue() must_== "world"
      body.path("file").textValue() must_== "This is a test asset."
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

  def app = HttpBinApplication.app

  val foldingSink = Sink.fold[ByteString, ByteString](ByteString.empty)((state, bs) => state ++ bs)

  val isoString = {
    // Converts the String "Hello €" to the ISO Counterparty
    val sourceCharset = StandardCharsets.UTF_8
    val buffer = ByteBuffer.wrap("Hello €".getBytes(sourceCharset))
    val data = sourceCharset.decode(buffer)
    val targetCharset = Charset.forName("Windows-1252")
    new String(targetCharset.encode(data).array(), targetCharset)
  }

  class CustomSigner extends WSSignatureCalculator with play.shaded.ahc.org.asynchttpclient.SignatureCalculator {
    def calculateAndAddSignature(request: play.shaded.ahc.org.asynchttpclient.Request, requestBuilder: play.shaded.ahc.org.asynchttpclient.RequestBuilderBase[_]) = {
      // do nothing
    }
  }

  def withServer[T](block: play.libs.ws.WSClient => T) = {
    Server.withApplication(app) { implicit port =>
      withClient(block)
    }
  }

  def withEchoServer[T](block: play.libs.ws.WSClient => T) = {
    def echo = BodyParser { req =>
      Accumulator.source[ByteString].mapFuture { source =>
        Future.successful(source).map(Right.apply)
      }
    }

    Server.withRouterFromComponents()(components => {
      case _ => components.defaultActionBuilder(echo) { req =>
        Ok.chunked(req.body)
      }
    }) { implicit port =>
      withClient(block)
    }
  }

  def withResult[T](result: Result)(block: play.libs.ws.WSClient => T) = {
    Server.withRouterFromComponents() { components =>
      {
        case _ => components.defaultActionBuilder(result)
      }
    } { implicit port =>
      withClient(block)
    }
  }

  def withClient[T](block: play.libs.ws.WSClient => T)(implicit port: Port): T = {
    val wsClient = play.test.WSTestClient.newClient(port.value)
    try {
      block(wsClient)
    } finally {
      wsClient.close()
    }
  }

  def withHeaderCheck[T](block: play.libs.ws.WSClient => T) = {
    Server.withRouterFromComponents() { components =>
      {
        case _ => components.defaultActionBuilder { req =>
          val contentLength = req.headers.get(CONTENT_LENGTH)
          val transferEncoding = req.headers.get(TRANSFER_ENCODING)
          Ok(s"Content-Length: ${contentLength.getOrElse(-1)}; Transfer-Encoding: ${transferEncoding.getOrElse(-1)}")
        }
      }
    } { implicit port =>
      withClient(block)
    }
  }
}
