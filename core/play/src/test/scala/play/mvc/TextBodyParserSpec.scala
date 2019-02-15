/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.charset.{ Charset, StandardCharsets }
import java.util.concurrent.CompletionStage

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.javadsl.Source
import akka.util.ByteString
import org.specs2.matcher.MustMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import play.api.http.{ HttpConfiguration, ParserConfiguration }
import play.http.HttpErrorHandler
import play.libs.F
import play.mvc.Http.RequestBody

class TextBodyParserSpec extends Specification with AfterAll with MustMatchers {
  "Java TextBodyParserSpec" title

  implicit val system = ActorSystem("text-body-parser-spec")
  implicit val materializer = ActorMaterializer()

  def afterAll(): Unit = {
    materializer.shutdown()
    system.terminate()
  }

  val config = ParserConfiguration()
  @inline def req(r: play.api.mvc.Request[Http.RequestBody]) = new Http.RequestImpl(r)

  val httpConfiguration = HttpConfiguration()

  val httpErrorHandler: HttpErrorHandler = new HttpErrorHandler {
    override def onClientError(request: Http.RequestHeader, statusCode: Int, message: String): CompletionStage[Result] = ???
    override def onServerError(request: Http.RequestHeader, exception: Throwable): CompletionStage[Result] = ???
  }

  def tolerantParse(request: Http.Request, byteString: ByteString): Either[Result, String] = {
    val parser: BodyParser[String] = new BodyParser.TolerantText(httpConfiguration, httpErrorHandler)
    val disj: F.Either[Result, String] = parser(request).run(Source.single(byteString), materializer).toCompletableFuture.get
    if (disj.left.isPresent) {
      Left(disj.left.get)
    } else Right(disj.right.get)
  }

  def strictParse(request: Http.Request, byteString: ByteString): Either[Result, String] = {
    val parser: BodyParser[String] = new BodyParser.Text(httpConfiguration, httpErrorHandler)
    val disj: F.Either[Result, String] = parser(request).run(Source.single(byteString), materializer).toCompletableFuture.get
    if (disj.left.isPresent) {
      Left(disj.left.get)
    } else Right(disj.right.get)
  }

  "Text Body Parser" should {
    "parse text" >> {
      "as US-ASCII if not defined" in {
        val body = ByteString("lorem ipsum")
        val postRequest = new Http.RequestBuilder().method("POST").body(new RequestBody(body.toString()), "text/plain").req
        strictParse(req(postRequest), body) must beRight.like {
          case text => text must beEqualTo("lorem ipsum")
        }
      }
      "as UTF-8 if defined" in {
        val body = ByteString("©".getBytes(UTF_8))
        val postRequest = new Http.RequestBuilder().method("POST").body(new RequestBody(body.toString()), "text/plain; charset=utf-8").req
        strictParse(req(postRequest), body) must beRight.like {
          case text => text must beEqualTo("©")
        }
      }
      "as US-ASCII if not defined even if UTF-8 characters are provided" in {
        val body = ByteString("©".getBytes(UTF_8))
        val postRequest = new Http.RequestBuilder().method("POST").body(new RequestBody(body.toString()), "text/plain").req
        strictParse(req(postRequest), body) must beRight.like {
          case text => text must beEqualTo("��")
        }
      }
    }
  }

  "TolerantText Body Parser" should {
    "parse text" >> {

      "as the declared charset if defined" in {
        // http://kunststube.net/encoding/
        val charset = StandardCharsets.UTF_16
        val body = ByteString("エンコーディングは難しくない".getBytes(charset))
        val postRequest = new Http.RequestBuilder().method("POST").bodyText(body.toString(), charset).req
        tolerantParse(req(postRequest), body) must beRight.like {
          case text =>
            text must beEqualTo("エンコーディングは難しくない")
        }
      }

      "as US-ASCII if charset is not explicitly defined" in {
        val body = ByteString("lorem ipsum")
        val postRequest = new Http.RequestBuilder().method("POST").body(new RequestBody(body.toString()), "text/plain").req
        tolerantParse(req(postRequest), body) must beRight.like {
          case text => text must beEqualTo("lorem ipsum")
        }
      }

      "as UTF-8 for undefined if ASCII encoding is insufficient" in {
        // http://kermitproject.org/utf8.html
        val body = ByteString("ᚠᛇᚻ᛫ᛒᛦᚦ᛫ᚠᚱᚩᚠᚢᚱ᛫ᚠᛁᚱᚪ᛫ᚷᛖᚻᚹᛦᛚᚳᚢᛗ")
        val postRequest = new Http.RequestBuilder().method("POST").body(new RequestBody(body.toString()), "text/plain").req
        tolerantParse(req(postRequest), body) must beRight.like {
          case text => text must beEqualTo("ᚠᛇᚻ᛫ᛒᛦᚦ᛫ᚠᚱᚩᚠᚢᚱ᛫ᚠᛁᚱᚪ᛫ᚷᛖᚻᚹᛦᛚᚳᚢᛗ")
        }
      }

      "as ISO-8859-1 for undefined if UTF-8 is insufficient" in {
        val body = ByteString(0xa9) // copyright sign encoded with ISO-8859-1
        val postRequest = new Http.RequestBuilder().method("POST").body(new RequestBody(body.toString()), "text/plain").req
        tolerantParse(req(postRequest), body) must beRight.like {
          case text => text must beEqualTo("©")
        }
      }

      "as UTF-8 even if the guessed encoding is utterly wrong" in {
        // This is not a full solution, so anything where we have a potentially valid encoding is seized on, even
        // when it's not the best one.
        val body = ByteString("エンコーディングは難しくない".getBytes(Charset.forName("Shift-JIS")))
        val postRequest = new Http.RequestBuilder().method("POST").bodyText(body.toString()).req
        tolerantParse(req(postRequest), body) must beRight.like {
          case text =>
            // utter gibberish, but we have no way of knowing the format.
            text must beEqualTo("\u0083G\u0083\u0093\u0083R\u0081[\u0083f\u0083B\u0083\u0093\u0083O\u0082Í\u0093ï\u0082µ\u0082\u00AD\u0082È\u0082¢")
        }
      }
    }
  }
}
