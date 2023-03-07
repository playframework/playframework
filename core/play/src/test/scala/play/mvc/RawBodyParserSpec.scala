/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc

import java.io.IOException

import scala.concurrent.Future
import scala.language.postfixOps

import akka.actor.ActorSystem
import akka.stream.javadsl.Source
import akka.stream.Materializer
import akka.util.ByteString
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import play.api.http.ParserConfiguration
import play.api.mvc.PlayBodyParsers
import play.api.mvc.RawBuffer
import play.core.j.JavaParsers
import play.core.test.FakeRequest

class RawBodyParserSpec extends Specification with AfterAll {
  "Java RawBodyParserSpec" title

  implicit val system: ActorSystem        = ActorSystem("raw-body-parser-spec")
  implicit val materializer: Materializer = Materializer.matFromSystem
  val parsers                             = PlayBodyParsers()

  def afterAll(): Unit = {
    materializer.shutdown()
    system.terminate()
  }

  val config                                     = ParserConfiguration()
  @inline def req[T](r: play.api.mvc.Request[T]) = new Http.RequestImpl(r.withBody(null)) {}

  def javaParser(p: play.api.mvc.BodyParser[RawBuffer]): BodyParser[RawBuffer] =
    new BodyParser.DelegatingBodyParser[RawBuffer, RawBuffer](p, java.util.function.Function.identity[RawBuffer]) {}

  def parse[B](
      body: ByteString,
      memoryThreshold: Long = config.maxMemoryBuffer,
      maxLength: Long = config.maxDiskBuffer
  )(
      javaParser: B => BodyParser[RawBuffer],
      parserInit: B = parsers.raw(memoryThreshold, maxLength)
  ): Either[Result, RawBuffer] = {
    val request = req(FakeRequest(method = "GET", "/x"))
    val parser  = javaParser(parserInit)

    val disj = parser(request).run(Source.single(body), materializer).toCompletableFuture.get

    if (disj.left.isPresent) {
      Left(disj.left.get)
    } else Right(disj.right.get)
  }

  "Raw Body Parser" should {
    "parse a simple body" >> {
      val body = ByteString("lorem ipsum")

      "successfully" in {
        parse(body)(javaParser _) must beRight[RawBuffer].like {
          case rawBuffer =>
            rawBuffer.asBytes() must beSome[ByteString].like {
              case outBytes => outBytes mustEqual body
            }
        }
      }

      "using a future" in {
        import scala.concurrent.ExecutionContext.Implicits.global
        val stage           = new java.util.concurrent.CompletableFuture[play.mvc.BodyParser[RawBuffer]]()
        implicit val system = ActorSystem()

        Future {
          val scalaParser = PlayBodyParsers().raw
          val javaParser = new BodyParser.DelegatingBodyParser[RawBuffer, RawBuffer](
            scalaParser,
            java.util.function.Function.identity[RawBuffer]
          ) {}

          stage.complete(javaParser)
        }

        parse[BodyParser[RawBuffer]](body)(identity, JavaParsers.flatten(stage, materializer)) must beRight[RawBuffer]
          .like {
            case rawBuffer =>
              rawBuffer.asBytes() must beSome[ByteString].like {
                case outBytes => outBytes mustEqual body
              }
          }
      }

      "close the raw buffer after parsing the body" in {
        val body = ByteString("lorem ipsum")
        parse(body, memoryThreshold = 1)(javaParser _) must beRight[RawBuffer].like {
          case rawBuffer =>
            rawBuffer.push(ByteString("This fails because the stream was closed!")) must throwA[IOException]
        }
      }

      "fail to parse longer than allowed body" in {
        val msg = ByteString("lorem ipsum")
        parse(msg, maxLength = 1)(javaParser _) must beLeft
      }
    }
  }
}
