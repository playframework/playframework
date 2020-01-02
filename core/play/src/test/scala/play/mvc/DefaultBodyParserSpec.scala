/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc

import java.util.Optional
import java.util.concurrent.CompletionStage

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.javadsl.Source
import akka.util.ByteString
import org.specs2.matcher.MustMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import play.api.http.HttpConfiguration
import play.api.http.ParserConfiguration
import play.api.mvc.PlayBodyParsers
import play.http.HttpErrorHandler
import play.libs.F
import play.mvc.Http.RequestBody

import scala.compat.java8.OptionConverters._

class DefaultBodyParserSpec extends Specification with AfterAll with MustMatchers {
  "Java DefaultBodyParserSpec" title

  implicit val system       = ActorSystem("default-body-parser-spec")
  implicit val materializer = Materializer.matFromSystem

  def afterAll(): Unit = {
    materializer.shutdown()
    system.terminate()
  }

  val config                                                 = ParserConfiguration()
  @inline def req(r: play.api.mvc.Request[Http.RequestBody]) = new Http.RequestImpl(r)

  val httpConfiguration = HttpConfiguration()
  val bodyParser        = PlayBodyParsers()

  val httpErrorHandler: HttpErrorHandler = new HttpErrorHandler {
    override def onClientError(request: Http.RequestHeader, statusCode: Int, message: String): CompletionStage[Result] =
      ???
    override def onServerError(request: Http.RequestHeader, exception: Throwable): CompletionStage[Result] = ???
  }

  def parse(request: Http.Request, byteString: ByteString): Either[Result, Object] = {
    val parser: BodyParser[Object] = new BodyParser.Default(httpErrorHandler, httpConfiguration, bodyParser)
    val disj: F.Either[Result, Object] =
      parser(request).run(Source.single(byteString), materializer).toCompletableFuture.get
    if (disj.left.isPresent) {
      Left(disj.left.get)
    } else Right(disj.right.get)
  }

  "Default Body Parser" should {
    "handle 'Content-Length: 0' header as empty body (containing Optional.empty)" in {
      val body = ByteString.empty
      val postRequest =
        new Http.RequestBuilder().method("POST").body(new RequestBody(body.utf8String), "text/plain").req
      postRequest.hasBody must beFalse
      parse(req(postRequest), body) must beRight[Object].like {
        case empty: Optional[_] => empty.asScala must beNone
      }
    }
    "handle 'Content-Length: 1' header as non-empty body" in {
      val body = ByteString("a")
      val postRequest =
        new Http.RequestBuilder().method("POST").body(new RequestBody(body.utf8String), "text/plain").req
      postRequest.hasBody must beTrue
      parse(req(postRequest), body) must beRight[Object].like {
        case text: String => text must beEqualTo("a")
      }
    }
    "handle RequestBody containing null as empty body (containing Optional.empty)" in {
      val body = ByteString.empty
      val postRequest =
        new Http.RequestBuilder().method("POST").body(new RequestBody(null), "text/plain").req
      postRequest.hasBody must beFalse
      parse(req(postRequest), body) must beRight[Object].like {
        case empty: Optional[_] => empty.asScala must beNone
      }
    }
    "handle missing Content-Length and Transfer-Encoding headers as empty body (containing Optional.empty)" in {
      val body = ByteString.empty
      val postRequest =
        new Http.RequestBuilder().method("POST").req
      postRequest.hasBody must beFalse
      parse(req(postRequest), body) must beRight[Object].like {
        case empty: Optional[_] => empty.asScala must beNone
      }
    }
  }
}
