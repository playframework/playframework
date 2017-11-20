/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http

import java.nio.file.{ Path, Files => JFiles }

import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.specs2.mutable.BeforeAfter
import play.api._
import play.api.http.{ HttpEntity, _ }
import play.api.libs.EventSource
import play.api.mvc._
import play.api.routing.Router
import play.api.test._
import play.core.server.common.ServerResultException
import play.it.test._

import scala.concurrent.Future
import scala.util.Try

/**
 * Partial migration of the tests from ScalaResultsHandlingSpec to use endpoints.
 * This allows testing of HTTP/2.
 *
 * When the migration is finished this class should be renamed.
 */
class ScalaResultsHandlingWithEndpointsSpec extends PlaySpecification
    with EndpointIntegrationSpecification
    with OkHttpEndpointSupport with WSEndpointSupport with BasicClientEndpointSupport {

  class WithTempFile extends BeforeAfter {
    protected var tempFile: Path = _
    override def before: Any = tempFile = JFiles.createTempFile("play-integration-test", "")
    override def after: Any = JFiles.delete(tempFile)
  }

  "scala result handling (2)" should {

    "add Date header" in serveOk("Hello world")
      .useOkHttp.request("/").forEndpoints { response =>
        response.header(DATE) must not beNull
      }

    "work with non-standard HTTP response codes" in serveResult(Result(ResponseHeader(498), HttpEntity.NoEntity))
      .useOkHttp.request("/").forEndpoints { response =>
        response.code must_== 498
        response.body.bytes must beEmpty
      }

    "add Content-Length for strict results" in serveOk("Hello world")
      .useOkHttp.request("/").forEndpoints { response =>
        response.header(CONTENT_LENGTH) must_== "11"
        response.body.string must_== "Hello world"
      }

    "support sending an empty streamed entity with a known size zero" in serveResult(
      Results.Ok.sendEntity(HttpEntity.Streamed(Source.empty[ByteString], Some(0), None))
    ).useOkHttp.request("/").forEndpoints { response =>
        response.code must_== 200
        response.header(CONTENT_LENGTH) must (equalTo("0") or beNull)
      }

    "support sending an empty file" in {
      serveComponents {
        val context = ApplicationLoader.createContext(
          environment = Environment.simple(),
          initialSettings = Map[String, AnyRef](Play.GlobalAppConfigKey -> java.lang.Boolean.FALSE)
        )
        new BuiltInComponentsFromContext(context) with NoHttpFiltersComponents {
          val emptyPath = JFiles.createTempFile("empty", ".txt")
          override lazy val router: Router = Router.from {
            case _ => defaultActionBuilder {
              implicit val mimeTypes: FileMimeTypes = new DefaultFileMimeTypes(FileMimeTypesConfiguration())
              Results.Ok.sendPath(emptyPath)
            }
          }
          applicationLifecycle.addStopHook { () =>
            Future.fromTry(Try(JFiles.delete(emptyPath)))
          }
        }
      }.useOkHttp.request("/").forEndpoints {
        response =>
          response.code must_== 200
          response.header(CONTENT_LENGTH) must_== "0"
      }
    }

    "support sending an empty file 2" in new WithTempFile {
      serveAction { components: BuiltInComponents =>
        components.defaultActionBuilder {
          implicit val fileMimeTypes = new FileMimeTypes {
            override def forFileName(name: String): Option[String] = Some("text/plain")
          }
          implicit val ec = components.executionContext
          Results.Ok.sendPath(tempFile)
        }
      }.useOkHttp.request("/").forEndpoints {
        response =>
          response.code must_== 200
          response.header(CONTENT_LENGTH) must_== "0"
      }
    }

    "not add a content length header when none is supplied" in serveResult(
      Results.Ok.sendEntity(HttpEntity.Streamed(Source(List("abc", "def", "ghi")).map(ByteString.apply), None, None))
    )
      .useOkHttp.request("/")
      // Rich 8-20 Nov 2017: Have seen intermittent failures for Akka HTTP/1.1 over SSL.
      // The failure is a read timeout in the Sun SSL stack on the OkHttp client side.
      // Other types of endpoint worked OK.
      .skipped(r => r.isAkkaHttp && r.isEncrypted)
      .forEndpoints { response =>
        response.header(CONTENT_LENGTH) must beNull
        response.header(TRANSFER_ENCODING) must beNull
        response.body.string must_== "abcdefghi"
      }

    "support responses with custom Content-Types" in serveResult(
      Results.Ok.sendEntity(HttpEntity.Strict(ByteString(0xff.toByte), Some("schmitch/foo; bar=bax")))
    ).useOkHttp.request("/").forEndpoints { response: okhttp3.Response =>
        response.header(CONTENT_TYPE) must_== "schmitch/foo; bar=bax"
        response.header(CONTENT_LENGTH) must_== "1"
        response.header(TRANSFER_ENCODING) must beNull
        ByteString(response.body.bytes) must_== ByteString(0xff.toByte)
      }

    "support multipart/mixed responses" in {
      // Example taken from https://www.w3.org/Protocols/rfc1341/7_2_Multipart.html
      val contentType = "multipart/mixed; boundary=\"simple boundary\""
      val body: String =
        """|This is the preamble.  It is to be ignored, though it
                       |is a handy place for mail composers to include an
                       |explanatory note to non-MIME compliant readers.
                       |--simple boundary
                       |
                       |This is implicitly typed plain ASCII text.
                       |It does NOT end with a linebreak.
                       |--simple boundary
                       |Content-type: text/plain; charset=us-ascii
                       |
                       |This is explicitly typed plain ASCII text.
                       |It DOES end with a linebreak.
                       |
                       |--simple boundary--
                       |This is the epilogue.  It is also to be ignored.""".stripMargin
      serveResult(
        Results.Ok.sendEntity(HttpEntity.Strict(ByteString(body), Some(contentType)))
      ).useOkHttp.request("/").forEndpoints { response =>
          response.header(CONTENT_TYPE) must_== contentType
          response.header(CONTENT_LENGTH) must_== body.length.toString
          response.header(TRANSFER_ENCODING) must beNull
          response.body.string must_== body
        }
    }

    "chunk results for chunked streaming strategy" in serveResult(
      Results.Ok.chunked(Source(List("a", "b", "c")))
    ).useOkHttp.request("/").skipped(_.supportsHttp2).forEndpoints { response =>
        response.code must_== 200
        response.header(TRANSFER_ENCODING) must_== "chunked"
        response.header(CONTENT_LENGTH) must beNull
        response.body.string must_== "abc"
      }

    "chunk results for event source strategy" in serveResult(
      Results.Ok.chunked(Source(List("a", "b")) via EventSource.flow).as("text/event-stream")
    ).useOkHttp.request("/").skipped(_.supportsHttp2).forEndpoints { response =>
        response.code must_== 200
        response.header(CONTENT_TYPE).toLowerCase(java.util.Locale.ENGLISH) must_== "text/event-stream"
        response.header(TRANSFER_ENCODING) must_== "chunked"
        response.header(CONTENT_LENGTH) must beNull
        response.body.string must_== "data: a\n\ndata: b\n\n"
      }

    // Rich: Nov 21 2017: The 'ignore cookies' test gives read timeouts
    // on both Netty and Akka when `serveOk` is replaced with
    // an action that returns the cookie value. I've left it
    // as a simple Ok response for now.
    "ignore invalid cookies in request header" in serveOk
      .useBasicClient.forEndpoints { basicEndpoint: BasicClientEndpoint =>
        val response = basicEndpoint.request("GET", "/", "HTTP/1.1", headers = Map("Cookie" -> "£"))
        response.status must_== 200
        response.body must beLeft
      }

    "reject HTTP 1.0 requests for chunked results" in serveComponents
      .withResult(Results.Ok.chunked(Source(List("a", "b", "c"))))
      .withErrorHandler(_ => new HttpErrorHandler {
        override def onClientError(request: RequestHeader, statusCode: Int, message: String = ""): Future[Result] = ???
        override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
          request.path must_== "/"
          exception must beLike {
            case e: ServerResultException =>
              // Check original result
              e.result.header.status must_== 200
          }
          Future.successful(Results.Status(500))
        }
      })
      .useBasicClient.forEndpoints { basicEndpoint: BasicClientEndpoint =>
        val response = basicEndpoint.request("GET", "/", "HTTP/1.0")
        response.status must_== 505
      }

    "return a 500 error on response with null header" in serveResult(Results.Ok("some body").withHeaders("X-Null" -> null))
      .useOkHttp.request("/").forEndpoints { response =>
        response.code must_== 500
      }

    "return a 400 error on Header value contains a prohibited character" in serveOk
      .useBasicClient.forEndpoints { basicEndpoint: BasicClientEndpoint =>
        basicEndpoint.request("GET", "/", "HTTP/1.1", Map("aaa" -> "bbb\fccc")).status must_== 400
      }

    "return a 400 error on Header value contains a prohibited character" in serveOk
      .useBasicClient.forEndpoints { basicEndpoint: BasicClientEndpoint =>
        basicEndpoint.request("GET", "/", "HTTP/1.1", Map("ddd" -> "eee\u000bfff")).status must_== 400
      }

    "support UTF-8 encoded filenames in Content-Disposition headers" in new WithTempFile {
      serveResult { components: BuiltInComponents =>
        import components.executionContext
        implicit val mimeTypes: FileMimeTypes = new DefaultFileMimeTypes(FileMimeTypesConfiguration())
        Results.Ok.sendFile(
          tempFile.toFile,
          fileName = _ => "测 试.tmp"
        )
      }.useBasicClient.request("/").forEndpoints { response: BasicResponse =>
        response.status must_== 200
        response.body must beLeft("")
        response.headers.get(CONTENT_DISPOSITION) must beSome(s"""inline; filename="? ?.tmp"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp""")
      }
    }
  }
}
