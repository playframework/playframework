/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.libs

import org.specs2.matcher.MatchResult
import play.api.http.HeaderNames
import play.api.libs.ws.{ WSBodyReadables, WSBodyWritables }
import play.api.libs.oauth._
import play.api.test.PlaySpecification
import play.it.{ AkkaHttpIntegrationSpecification, NettyIntegrationSpecification, ServerIntegrationSpecification }

class NettyScalaWSSpec extends ScalaWSSpec with NettyIntegrationSpecification

class AkkaHttpScalaWSSpec extends ScalaWSSpec with AkkaHttpIntegrationSpecification

trait ScalaWSSpec extends PlaySpecification with ServerIntegrationSpecification with WSBodyWritables with WSBodyReadables {
  import java.io.File
  import java.nio.ByteBuffer
  import java.nio.charset.{ Charset, StandardCharsets }

  import akka.stream.scaladsl.{ FileIO, Sink, Source }
  import akka.util.ByteString
  import play.api.libs.json.JsString
  import play.api.libs.streams.Accumulator
  import play.api.libs.ws._
  import play.api.mvc.Results.Ok
  import play.api.mvc._
  import play.api.test._
  import play.core.server.Server
  import play.it.tools.HttpBinApplication
  import play.shaded.ahc.org.asynchttpclient.{ RequestBuilderBase, SignatureCalculator }

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
  import scala.concurrent.{ Await, Future }

  "Web service client" title

  sequential

  "play.api.libs.ws.WSClient" should {

    "make GET Requests" in withServer { ws =>
      val req = ws.url("/get").get()

      Await.result(req, Duration(1, SECONDS)).status aka "status" must_== 200
    }

    "Get 404 errors" in withServer { ws =>
      val req = ws.url("/post").get()

      Await.result(req, Duration(1, SECONDS)).status aka "status" must_== 404
    }

    "get a streamed response" in withResult(Results.Ok.chunked(Source(List("a", "b", "c")))) { ws =>
      val res: Future[WSResponse] = ws.url("/get").stream()
      val body: Source[ByteString, _] = await(res).bodyAsSource

      val result: MatchResult[Any] = await(body.runWith(foldingSink)).utf8String.
        aka("streamed response") must_== "abc"
      result
    }

    "streaming a request body" in withEchoServer { ws =>
      val source = Source(List("a", "b", "c").map(ByteString.apply))
      val res = ws.url("/post").withMethod("POST").withBody(source).execute()
      val body = await(res).body

      body must_== "abc"
    }

    "streaming a request body with manual content length" in withHeaderCheck { ws =>
      val source = Source.single(ByteString("abc"))
      val res = ws.url("/post").withMethod("POST").addHttpHeaders(CONTENT_LENGTH -> "3").withBody(source).execute()
      val body = await(res).body

      body must_== s"Content-Length: 3; Transfer-Encoding: -1"
    }

    "send a multipart request body" in withServer { ws =>
      val file = new File(this.getClass.getResource("/testassets/foo.txt").toURI).toPath
      val dp = MultipartFormData.DataPart("hello", "world")
      val fp = MultipartFormData.FilePart("upload", "foo.txt", None, FileIO.fromPath(file))
      val source: Source[MultipartFormData.Part[Source[ByteString, _]], _] = Source(List(dp, fp))
      val res = ws.url("/post").post(source)
      val jsonBody = await(res).json

      (jsonBody \ "form" \ "hello").toOption must beSome(JsString("world"))
      (jsonBody \ "file").toOption must beSome(JsString("This is a test asset."))
    }

    "send a multipart request body via withBody" in withServer { ws =>
      val file = new File(this.getClass.getResource("/testassets/foo.txt").toURI)
      val dp = MultipartFormData.DataPart("hello", "world")
      val fp = MultipartFormData.FilePart("upload", "foo.txt", None, FileIO.fromPath(file.toPath))
      val source = Source(List(dp, fp))
      val res = ws.url("/post").withBody(source).withMethod("POST").execute()
      val body = await(res).json

      (body \ "form" \ "hello").toOption must beSome(JsString("world"))
      (body \ "file").toOption must beSome(JsString("This is a test asset."))
    }

    "not throw an exception while signing requests" >> {
      val calc = new CustomSigner

      "without query string" in withServer { ws =>
        ws.url("/").sign(calc).get().
          aka("signed request") must not(throwA[NullPointerException])
      }

      "with query string" in withServer { ws =>
        ws.url("/").withQueryStringParameters("lorem" -> "ipsum").
          sign(calc) aka "signed request" must not(throwA[Exception])
      }
    }

    "preserve the case of an Authorization header" >> {

      def withAuthorizationCheck[T](block: play.api.libs.ws.WSClient => T) = {
        Server.withRouterFromComponents() { c =>
          {
            case _ => c.defaultActionBuilder { req: Request[AnyContent] =>
              Results.Ok(req.headers.keys.filter(_.equalsIgnoreCase("authorization")).mkString)
            }
          }
        } { implicit port =>
          WsTestClient.withClient(block)
        }
      }

      "when signing with the OAuthCalculator" in {
        val oauthCalc = {
          val consumerKey = ConsumerKey("key", "secret")
          val requestToken = RequestToken("token", "secret")
          OAuthCalculator(consumerKey, requestToken)
        }
        "expect title-case header with signed request" in withAuthorizationCheck { ws =>
          val body = await(ws.url("/").sign(oauthCalc).execute()).body
          body must beEqualTo("Authorization").ignoreCase
        }
      }

      // Attempt to replicate https://github.com/playframework/playframework/issues/7735
      "when signing with a custom calculator" in {
        val customCalc = new WSSignatureCalculator with SignatureCalculator {
          def calculateAndAddSignature(request: play.shaded.ahc.org.asynchttpclient.Request, requestBuilder: RequestBuilderBase[_]) = {
            requestBuilder.addHeader(HeaderNames.AUTHORIZATION, "some value")
          }
        }
        "expect title-case header with signed request" in withAuthorizationCheck { ws =>
          val body = await(ws.url("/").sign(customCalc).execute()).body
          body must_== ("Authorization")
        }
      }

      // Attempt to replicate https://github.com/playframework/playframework/issues/7735
      "when sending an explicit header" in {
        "preserve a title-case 'Authorization' header" in withAuthorizationCheck { ws =>
          val body = await(ws.url("/").withHttpHeaders("Authorization" -> "some value").execute()).body
          body must_== ("Authorization")
        }
        "preserve a lower-case 'authorization' header" in withAuthorizationCheck { ws =>
          val body = await(ws.url("/").withHttpHeaders("authorization" -> "some value").execute()).body
          body must_== ("authorization")
        }
      }

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
  implicit val materializer = app.materializer

  def withServer[T](block: play.api.libs.ws.WSClient => T) = {
    Server.withApplication(app) { implicit port =>
      WsTestClient.withClient(block)
    }
  }

  def withEchoServer[T](block: play.api.libs.ws.WSClient => T) = {
    def echo = BodyParser { req =>
      Accumulator.source[ByteString].mapFuture { source =>
        Future.successful(source).map(Right.apply)
      }
    }

    Server.withRouterFromComponents() { components =>
      {
        case _ => components.defaultActionBuilder(echo) { req: Request[Source[ByteString, _]] =>
          Ok.chunked(req.body)
        }
      }
    } { implicit port =>
      WsTestClient.withClient(block)
    }
  }

  def withResult[T](result: Result)(block: play.api.libs.ws.WSClient => T): T = {
    Server.withRouterFromComponents() { c =>
      {
        case _ => c.defaultActionBuilder(result)
      }
    } { implicit port =>
      WsTestClient.withClient(block)
    }
  }

  def withHeaderCheck[T](block: play.api.libs.ws.WSClient => T) = {
    Server.withRouterFromComponents() { c =>
      {
        case _ => c.defaultActionBuilder { req: Request[AnyContent] =>

          val contentLength = req.headers.get(CONTENT_LENGTH)
          val transferEncoding = req.headers.get(TRANSFER_ENCODING)
          Ok(s"Content-Length: ${contentLength.getOrElse(-1)}; Transfer-Encoding: ${transferEncoding.getOrElse(-1)}")

        }
      }
    } { implicit port =>
      WsTestClient.withClient(block)
    }
  }

  class CustomSigner extends WSSignatureCalculator with SignatureCalculator {
    def calculateAndAddSignature(request: play.shaded.ahc.org.asynchttpclient.Request, requestBuilder: RequestBuilderBase[_]) = {
      // do nothing
    }
  }

}
