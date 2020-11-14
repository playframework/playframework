/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.javadsl.Source
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import org.specs2.matcher.MustMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import org.specs2.specification.core.Fragment
import play.api.Environment
import play.api.Mode
import play.api.http.HeaderNames
import play.api.http.Status
import play.api.mvc.PlayBodyParsers
import play.libs.streams.Accumulator
import play.http.DefaultHttpErrorHandler
import play.libs.F

import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._

class MaxLengthBodyParserSpec extends Specification with AfterAll with MustMatchers {
  "Java MaxLengthBodyParserSpec" title

  val Body15   = ByteString("hello" * 3)
  def req      = new Http.RequestBuilder().method("GET").path("/x")
  def reqCLH15 = req.header(HeaderNames.CONTENT_LENGTH, "15")
  def reqCLH16 = req.header(HeaderNames.CONTENT_LENGTH, "16")

  implicit val system       = ActorSystem("java-max-length-body-parser-spec")
  implicit val materializer = Materializer.matFromSystem

  def afterAll(): Unit = {
    materializer.shutdown()
    system.terminate()
  }

  val underlyingParsers = PlayBodyParsers()
  val defaultHttpErrorHandler =
    new DefaultHttpErrorHandler(ConfigFactory.empty(), Environment(null, null, Mode.Prod).asJava, null, null)

  def feed[A](
      accumulator: Accumulator[ByteString, A],
      food: ByteString = Body15,
      ai: AtomicInteger = new AtomicInteger
  ): A = {
    accumulator
      .run(Source.fromIterator[ByteString](() => {
        ai.incrementAndGet()
        food.grouped(3).asJava
      }), materializer)
      .toCompletableFuture
      .get(5, TimeUnit.SECONDS)
  }

  def maxLengthEnforced(result: F.Either[Result, _]) = {
    result.left.asScala.map(_.status) must beSome(Status.REQUEST_ENTITY_TOO_LARGE)
    result.right.asScala must beNone
  }

  val bodyParsers: Seq[(BodyParser[_], Option[String], ByteString)] = Seq(
    // Tuple3: (bodyParser with maxLength of 15, Content-Type header, 15 bytes to feed the parser)
    (new BodyParser.Text(15, defaultHttpErrorHandler), Some("text/plain"), Body15),
    (new BodyParser.TolerantText(15, defaultHttpErrorHandler), None, Body15),
    (new BodyParser.Bytes(15, defaultHttpErrorHandler), None, Body15),
    (
      new BodyParser.Xml(15, defaultHttpErrorHandler, underlyingParsers),
      Some("application/xml"),
      ByteString("<foo>15 b</foo>") // 15 bytes
    ),
    (new BodyParser.TolerantXml(15, defaultHttpErrorHandler), None, ByteString("<foo>15 b</foo>")),                  // 15 bytes
    (new BodyParser.Json(15, defaultHttpErrorHandler), Some("application/json"), ByteString("""{ "x": "15 b" }""")), // 15 bytes
    (new BodyParser.TolerantJson(15, defaultHttpErrorHandler), None, ByteString("""{ "x": "15 b" }""")),             // 15 bytes
    (new BodyParser.FormUrlEncoded(15, defaultHttpErrorHandler), Some("application/x-www-form-urlencoded"), Body15),
    (
      new BodyParser.MultipartFormData(underlyingParsers, 15),
      Some("multipart/form-data; boundary=aabbccddeee"),
      ByteString("--aabbccddeee--") // 15 bytes
    ),
    (
      new DummyDelegatingMultipartFormDataBodyParser(materializer, 102400, 15, false, defaultHttpErrorHandler),
      Some("multipart/form-data; boundary=aabbccddeee"),
      ByteString("--aabbccddeee--") // 15 bytes
    ),
    (new BodyParser.Raw(underlyingParsers, 102400, 15), None, Body15),
    (
      new BodyParser.ToFile(
        underlyingParsers.temporaryFileCreator.create("foo", "bar").path.toFile,
        15,
        defaultHttpErrorHandler,
        materializer
      ),
      None,
      Body15
    ),
    (
      new BodyParser.TemporaryFile(
        15,
        underlyingParsers.temporaryFileCreator.asJava,
        defaultHttpErrorHandler,
        materializer
      ),
      None,
      Body15
    ),
  )

  "Max length body handling" should {
    "not run body parser when existing Content-Length header exceeds maxLength " in {
      Fragment.foreach(bodyParsers) { bodyParser =>
        val (parser, contentType, data) = bodyParser
        parser.toString >> {
          // Let's feed a request, that, via its Content-Length header, pretends to have a body size of 16 bytes,
          // to a body parser that only allows maximum 15 bytes. The actual body we want to parse
          // (with an actual content length of 15 bytes, which would be ok) will never be parsed.
          val ai = new AtomicInteger()
          val result = feed(
            parser
              .apply(
                contentType.map(ct => reqCLH16.header(HeaderNames.CONTENT_TYPE, ct)).getOrElse(reqCLH16).build()
              ),
            food = data,
            ai = ai
          )
          maxLengthEnforced(result)
          ai.get must_== 0 // makes sure no parsing took place at all
        }
      }
    }

    "run body parser when existing Content-Length header does not exceed maxLength " in {
      Fragment.foreach(bodyParsers) { bodyParser =>
        val (parser, contentType, data) = bodyParser
        parser.toString >> {
          // Same like above test, but now the Content-Length header does not exceed maxLength (actually matched the
          // actual body size)
          val ai = new AtomicInteger()
          val result = feed(
            parser
              .apply(
                contentType.map(ct => reqCLH15.header(HeaderNames.CONTENT_TYPE, ct)).getOrElse(reqCLH15).build()
              ),
            food = data,
            ai = ai
          )
          result.left.asScala must beNone
          result.right.asScala must beSome // successfully parsed
          ai.get must_== 1                 // also makes sure parsing took place
        }
      }
    }

    "run body parser when no Content-Length header exists and actual body size does not exceed maxLength" in {
      Fragment.foreach(bodyParsers) { bodyParser =>
        val (parser, contentType, data) = bodyParser
        parser.toString >> {
          val ai = new AtomicInteger()
          val result = feed(
            parser
              .apply(
                contentType.map(ct => req.header(HeaderNames.CONTENT_TYPE, ct)).getOrElse(req).build()
              ),
            food = data,
            ai = ai
          )
          result.left.asScala must beNone
          result.right.asScala must beSome // successfully parsed
          ai.get must_== 1                 // also makes sure parsing took place
        }
      }
    }

    "run body parser when no Content-Length header exists and actual body size exceeds maxLength" in {
      Fragment.foreach(bodyParsers) { bodyParser =>
        val (parser, contentType, data) = bodyParser
        parser.toString >> {
          val ai = new AtomicInteger()
          val result = feed(
            parser
              .apply(
                contentType.map(ct => req.header(HeaderNames.CONTENT_TYPE, ct)).getOrElse(req).build()
              ),
            food = ByteString(" ") ++ data, // prepend space to exceed maxLength by one byte
            ai = ai
          )
          maxLengthEnforced(result) // parser realised body is too large
          ai.get must_== 1          // also makes sure parsing took place
        }
      }
    }
  }
}
