/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8

import scala.concurrent.duration._
import scala.concurrent.Await

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.ByteString
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import play.core.test.FakeRequest

/**
 */
class TextBodyParserSpec extends Specification with AfterAll {
  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: Materializer   = Materializer.matFromSystem
  val parse                        = PlayBodyParsers()

  override def afterAll(): Unit = {
    system.terminate()
  }

  def tolerantParse(request: RequestHeader, byteString: ByteString) = {
    val parser: BodyParser[String] = parse.tolerantText
    Await.result(parser(request).run(Source.single(byteString)), Duration.Inf)
  }

  def strictParse(request: RequestHeader, byteString: ByteString) = {
    val parser: BodyParser[String] = parse.text
    Await.result(parser(request).run(Source.single(byteString)), Duration.Inf)
  }

  "Text Body Parser" should {
    "parse text" >> {
      "as UTF-8 if defined" in {
        val body        = ByteString("©".getBytes(UTF_8))
        val postRequest =
          FakeRequest("POST", "/").withBody(body).withHeaders("Content-Type" -> "text/plain; charset=utf-8")
        strictParse(postRequest, body) must beRight.like {
          case text => text must beEqualTo("©")
        }
      }

      "as the declared charset if defined" in {
        // http://kunststube.net/encoding/
        val charset     = StandardCharsets.UTF_16
        val body        = ByteString("エンコーディングは難しくない".getBytes(charset))
        val postRequest =
          FakeRequest("POST", "/").withBody(body).withHeaders("Content-Type" -> "text/plain; charset=utf-16")
        strictParse(postRequest, body) must beRight.like {
          case text =>
            text must beEqualTo("エンコーディングは難しくない")
        }
      }

      "as US-ASCII if not defined" in {
        val body        = ByteString("lorem ipsum")
        val postRequest = FakeRequest("POST", "/").withBody(body).withHeaders("Content-Type" -> "text/plain")
        strictParse(postRequest, body) must beRight.like {
          case text => text must beEqualTo("lorem ipsum")
        }
      }

      "as US-ASCII if not defined even if UTF-8 characters are provided" in {
        val body        = ByteString("©".getBytes(UTF_8))
        val postRequest = FakeRequest("POST", "/").withBody(body).withHeaders("Content-Type" -> "text/plain")
        strictParse(postRequest, body) must beRight.like {
          case text => text must beEqualTo("��")
        }
      }
    }
  }

  "TolerantText Body Parser" should {
    "parse text" >> {
      "as the declared charset if defined" in {
        // http://kunststube.net/encoding/
        val charset     = StandardCharsets.UTF_16
        val body        = ByteString("エンコーディングは難しくない".getBytes(charset))
        val postRequest =
          FakeRequest("POST", "/").withBody(body).withHeaders("Content-Type" -> "text/plain; charset=utf-16")
        tolerantParse(postRequest, body) must beRight.like {
          case text =>
            text must beEqualTo("エンコーディングは難しくない")
        }
      }

      "as US-ASCII if charset is not explicitly defined" in {
        val body        = ByteString("lorem ipsum")
        val postRequest = FakeRequest("POST", "/").withBody(body).withHeaders("Content-Type" -> "text/plain")
        tolerantParse(postRequest, body) must beRight.like {
          case text => text must beEqualTo("lorem ipsum")
        }
      }

      "as UTF-8 for undefined if ASCII encoding is insufficient" in {
        // http://kermitproject.org/utf8.html
        val body        = ByteString("ᚠᛇᚻ᛫ᛒᛦᚦ᛫ᚠᚱᚩᚠᚢᚱ᛫ᚠᛁᚱᚪ᛫ᚷᛖᚻᚹᛦᛚᚳᚢᛗ")
        val postRequest = FakeRequest("POST", "/").withBody(body).withHeaders("Content-Type" -> "text/plain")
        tolerantParse(postRequest, body) must beRight.like {
          case text => text must beEqualTo("ᚠᛇᚻ᛫ᛒᛦᚦ᛫ᚠᚱᚩᚠᚢᚱ᛫ᚠᛁᚱᚪ᛫ᚷᛖᚻᚹᛦᛚᚳᚢᛗ")
        }
      }

      "as ISO-8859-1 for undefined if UTF-8 is insufficient" in {
        val body        = ByteString(0xa9) // copyright sign encoded with ISO-8859-1
        val postRequest = FakeRequest("POST", "/").withBody(body).withHeaders("Content-Type" -> "text/plain")
        tolerantParse(postRequest, body) must beRight.like {
          case text => text must beEqualTo("©")
        }
      }

      "as UTF-8 for undefined even if US-ASCII could parse a prefix" in {
        val body        = ByteString("Oekraïene") // 'Oekra' can be decoded by US-ASCII
        val postRequest = FakeRequest("POST", "/").withBody(body).withHeaders("Content-Type" -> "text/plain")
        tolerantParse(postRequest, body) must beRight.like {
          case text => text must beEqualTo("Oekraïene")
        }
      }

      "as UTF-8 even if the guessed encoding is utterly wrong" in {
        // This is not a full solution, so anything where we have a potentially valid encoding is seized on, even
        // when it's not the best one.
        val body        = ByteString("エンコーディングは難しくない".getBytes(Charset.forName("Shift-JIS")))
        val postRequest = FakeRequest("POST", "/").withBody(body).withHeaders("Content-Type" -> "text/plain")
        tolerantParse(postRequest, body) must beRight.like {
          case text =>
            // utter gibberish, but we have no way of knowing the format.
            text must beEqualTo(
              "\u0083G\u0083\u0093\u0083R\u0081[\u0083f\u0083B\u0083\u0093\u0083O\u0082Í\u0093ï\u0082µ\u0082\u00AD\u0082È\u0082¢"
            )
        }
      }
    }
  }
}
