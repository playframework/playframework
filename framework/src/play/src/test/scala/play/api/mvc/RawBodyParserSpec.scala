/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import akka.util.ByteString
import akka.stream.scaladsl.Source
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import java.io.IOException

import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll

import play.api.http.ParserConfiguration
import play.core.test.FakeRequest

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object RawBodyParserSpec extends Specification with AfterAll {

  implicit val system = ActorSystem("content-types-spec")
  implicit val materializer = ActorMaterializer()(system)

  def afterAll(): Unit = {
    materializer.shutdown()
    system.terminate()
  }

  val config = ParserConfiguration()
  val bodyParser = new BodyParsers {}

  def parse(body: ByteString, memoryThreshold: Int = config.maxMemoryBuffer, maxLength: Long = config.maxDiskBuffer)(parser: BodyParser[RawBuffer] = bodyParser.parse.raw(memoryThreshold, maxLength)): Either[Result, RawBuffer] = {
    val request = FakeRequest(method = "GET", "/x")

    Await.result(parser(request).run(Source.single(body)), Duration.Inf)
  }

  "Raw Body Parser" should {
    "parse a simple body" >> {
      val body = ByteString("lorem ipsum")

      "successfully" in {
        parse(body)() must beRight.like {
          case rawBuffer => rawBuffer.asBytes() must beSome.like {
            case outBytes => outBytes mustEqual body
          }
        }
      }

      "using a future" in {
        import scala.concurrent.ExecutionContext.Implicits.global

        parse(body)(bodyParser.parse.flatten(Future.successful(bodyParser.parse.raw()))) must beRight.like {
          case rawBuffer => rawBuffer.asBytes() must beSome.like {
            case outBytes =>
              outBytes mustEqual body
          }
        }
      }
    }

    "close the raw buffer after parsing the body" in {
      val body = ByteString("lorem ipsum")
      parse(body, memoryThreshold = 1)() must beRight.like {
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
