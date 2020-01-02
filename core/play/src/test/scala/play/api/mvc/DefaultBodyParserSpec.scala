/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import play.core.test.FakeRequest

import scala.concurrent.Await
import scala.concurrent.duration._

class DefaultBodyParserSpec extends Specification with AfterAll {
  implicit val system = ActorSystem()
  implicit val mat    = Materializer.matFromSystem
  val parsers         = PlayBodyParsers()

  override def afterAll: Unit = {
    system.terminate()
  }

  def parse(request: RequestHeader, byteString: ByteString) = {
    val parser: BodyParser[AnyContent] = parsers.default
    Await.result(parser(request).run(Source.single(byteString)), Duration.Inf)
  }

  "Default Body Parser" should {
    "handle 'Content-Length: 0' header as empty body (containing AnyContentAsEmpty)" in {
      val body = ByteString.empty
      val postRequest =
        FakeRequest("POST", "/")
          .withBody(body)
          .withHeaders("Content-Type" -> "text/plain; charset=utf-8", "Content-Length" -> "0")
      postRequest.hasBody must beFalse
      parse(postRequest, body) must beRight[AnyContent].like {
        case empty: AnyContent => empty must beEqualTo(AnyContentAsEmpty)
      }
    }
    "handle 'Content-Length: 1' header as non-empty body" in {
      val body = ByteString("a")
      val postRequest =
        FakeRequest("POST", "/")
          .withBody(body)
          .withHeaders("Content-Type" -> "text/plain; charset=utf-8", "Content-Length" -> "1")
      postRequest.hasBody must beTrue
      parse(postRequest, body) must beRight[AnyContent].like {
        case text: AnyContentAsText => text must beEqualTo(AnyContentAsText("a"))
      }
    }
    "handle null body without Content-Length and Transfer-Encoding headers as empty body (containing AnyContentAsEmpty)" in {
      val body = ByteString.empty
      val postRequest =
        FakeRequest("POST", "/")
          .withBody(null)
          .withHeaders("Content-Type" -> "text/plain; charset=utf-8")
      (postRequest.body == null) must beTrue
      postRequest.hasBody must beFalse
      parse(postRequest, body) must beRight[AnyContent].like {
        case empty: AnyContent => empty must beEqualTo(AnyContentAsEmpty)
      }
    }
    "handle missing Content-Length and Transfer-Encoding headers as empty body (containing AnyContentAsEmpty)" in {
      val body = ByteString.empty
      val postRequest =
        FakeRequest("POST", "/")
          .withHeaders("Content-Type" -> "text/plain; charset=utf-8")
      postRequest.body must beEqualTo(AnyContentAsEmpty)
      postRequest.hasBody must beFalse
      parse(postRequest, body) must beRight[AnyContent].like {
        case empty: AnyContent => empty must beEqualTo(AnyContentAsEmpty)
      }
    }
  }
}
