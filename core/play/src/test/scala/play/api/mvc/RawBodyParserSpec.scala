/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import java.io.IOException

import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.Future

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.ByteString
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import play.api.http.ParserConfiguration
import play.core.test.FakeRequest

class RawBodyParserSpec extends Specification with AfterAll {
  implicit val system: ActorSystem        = ActorSystem("raw-body-parser-spec")
  implicit val materializer: Materializer = Materializer.matFromSystem

  def afterAll(): Unit = {
    materializer.shutdown()
    system.terminate()
  }

  val config = ParserConfiguration()
  val parse  = PlayBodyParsers()

  def parse(body: ByteString, memoryThreshold: Long = config.maxMemoryBuffer, maxLength: Long = config.maxDiskBuffer)(
      parser: BodyParser[RawBuffer] = parse.raw(memoryThreshold, maxLength)
  ): Either[Result, RawBuffer] = {
    val request = FakeRequest(method = "GET", "/x")

    Await.result(parser(request).run(Source.single(body)), Duration.Inf)
  }

  "Raw Body Parser" should {
    "parse a strict body" >> {
      val body = ByteString("lorem ipsum")
      // Feed a strict element rather than a singleton source, strict element triggers
      // fast path with zero materialization.
      Await.result(parse.raw.apply(FakeRequest()).run(body), Duration.Inf) must beRight[RawBuffer].like {
        case rawBuffer =>
          rawBuffer.asBytes() must beSome[ByteString].like {
            case outBytes => outBytes mustEqual body
          }
      }
    }

    "parse a simple body" >> {
      val body = ByteString("lorem ipsum")

      "successfully" in {
        parse(body)() must beRight[RawBuffer].like {
          case rawBuffer =>
            rawBuffer.asBytes() must beSome[ByteString].like {
              case outBytes => outBytes mustEqual body
            }
        }
      }

      "using a future" in {
        import scala.concurrent.ExecutionContext.Implicits.global

        parse(body)(parse.flatten(Future.successful(parse.raw()))) must beRight[RawBuffer].like {
          case rawBuffer =>
            rawBuffer.asBytes() must beSome[ByteString].like {
              case outBytes =>
                outBytes mustEqual body
            }
        }
      }
    }

    "close the raw buffer after parsing the body" in {
      val body = ByteString("lorem ipsum")
      parse(body, memoryThreshold = 1)() must beRight[RawBuffer].like {
        case rawBuffer =>
          rawBuffer.push(ByteString("This fails because the stream was closed!")) must throwA[IOException]
      }
    }

    "fail to parse longer than allowed body" in {
      val msg = ByteString("lorem ipsum")
      parse(msg, maxLength = 1)() must beLeft
    }
  }
}
