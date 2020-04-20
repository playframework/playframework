/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import org.specs2.specification.core.Fragment
import play.api.data.Form
import play.api.data.Forms.of
import play.api.data.format.Formats.stringFormat
import play.api.http.HeaderNames
import play.api.http.Status
import play.api.libs.streams.Accumulator
import play.core.test.FakeRequest

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Failure
import scala.util.Try

/**
 * All tests relating to max length handling
 */
class MaxLengthBodyParserSpec extends Specification with AfterAll {
  val MaxLength10 = 10
  val MaxLength15 = 15
  val MaxLength20 = 20
  val Body15      = ByteString("hello" * 3)
  val req         = FakeRequest("GET", "/x")
  val reqCLH15    = req.withHeaders((HeaderNames.CONTENT_LENGTH, "15"))
  val reqCLH16    = req.withHeaders((HeaderNames.CONTENT_LENGTH, "16"))

  implicit val system = ActorSystem()
  implicit val mat    = Materializer.matFromSystem

  import system.dispatcher
  val parse = PlayBodyParsers()

  override def afterAll: Unit = {
    system.terminate()
  }

  def bodyParser: (Accumulator[ByteString, Either[Result, ByteString]], Future[Unit]) = {
    val bodyParsed = Promise[Unit]()
    val parser = Accumulator(
      Sink
        .seq[ByteString]
        .mapMaterializedValue(future =>
          future.transform({ bytes =>
            bodyParsed.success(())
            Right(bytes.fold(ByteString.empty)(_ ++ _))
          }, { t =>
            bodyParsed.failure(t)
            t
          })
        )
    )
    (parser, bodyParsed.future)
  }

  def feed[A](
      accumulator: Accumulator[ByteString, A],
      food: ByteString = Body15,
      ai: AtomicInteger = new AtomicInteger
  ): A = {
    Await.result(accumulator.run(Source.fromIterator(() => {
      ai.incrementAndGet()
      food.grouped(3)
    })), 5.seconds)
  }

  def assertDidNotParse(parsed: Future[Unit]) = {
    Await.ready(parsed, 5.seconds)
    parsed.value must beSome[Try[Unit]].like {
      case Failure(t: BodyParsers.MaxLengthLimitAttained) => ok
    }
  }

  def enforceMaxLengthEnforced(result: Either[Result, _]) = {
    result must beLeft[Result].which { inner =>
      inner.header.status must_== Status.REQUEST_ENTITY_TOO_LARGE
    }
  }

  def maxLengthParserEnforced(
      result: Either[Result, Either[MaxSizeExceeded, ByteString]],
      maxLength: Int = MaxLength10
  ) = {
    result must beRight[Either[MaxSizeExceeded, ByteString]].which { inner =>
      inner must beLeft(MaxSizeExceeded(maxLength))
    }
  }

  "Max length body handling" should {
    "be exceeded when using the default max length handling" in {
      val (parser, parsed) = bodyParser
      val result           = feed(parse.enforceMaxLength(req, MaxLength10, parser))
      enforceMaxLengthEnforced(result)
      assertDidNotParse(parsed)
    }

    "be exceeded when using the maxLength body parser" in {
      val (parser, parsed) = bodyParser
      val result           = feed(parse.maxLength(MaxLength10, BodyParser(req => parser)).apply(req))
      maxLengthParserEnforced(result)
      assertDidNotParse(parsed)
    }

    "be exceeded when using the maxLength body parser and an equal enforceMaxLength" in {
      val (parser, parsed) = bodyParser
      val result = feed(
        parse.maxLength(MaxLength10, BodyParser(req => parse.enforceMaxLength(req, MaxLength10, parser))).apply(req)
      )
      maxLengthParserEnforced(result)
      assertDidNotParse(parsed)
    }

    "be exceeded when using the maxLength body parser and a longer enforceMaxLength" in {
      val (parser, parsed) = bodyParser
      val result = feed(
        parse.maxLength(MaxLength10, BodyParser(req => parse.enforceMaxLength(req, MaxLength20, parser))).apply(req)
      )
      maxLengthParserEnforced(result)
      assertDidNotParse(parsed)
    }

    "be exceeded when using enforceMaxLength and a longer maxLength body parser" in {
      val (parser, parsed) = bodyParser
      val result = feed(
        parse.maxLength(MaxLength20, BodyParser(req => parse.enforceMaxLength(req, MaxLength10, parser))).apply(req)
      )
      enforceMaxLengthEnforced(result)
      assertDidNotParse(parsed)
    }

    "not be exceeded when nothing is exceeded" in {
      val (parser, parsed) = bodyParser
      val result = feed(
        parse.maxLength(MaxLength20, BodyParser(req => parse.enforceMaxLength(req, MaxLength20, parser))).apply(req)
      )
      result must beRight.which { inner =>
        inner must beRight(Body15)
      }
      Await.result(parsed, 5.seconds) must_== (())
    }

    val bodyParsers = Seq(
      // Tuple3: (bodyParser with maxLength of 15, Content-Type header, 15 bytes to feed the parser)
      (parse.text(maxLength = MaxLength15), Some("text/plain"), Body15),
      (parse.tolerantText(maxLength = MaxLength15), None, Body15),
      (parse.byteString(maxLength = MaxLength15), None, Body15),
      (parse.xml(maxLength = MaxLength15), Some("application/xml"), ByteString("<foo>15 b</foo>")),       // 15 bytes
      (parse.tolerantXml(maxLength = MaxLength15), None, ByteString("<foo>15 b</foo>")),                  // 15 bytes
      (parse.json(maxLength = MaxLength15), Some("application/json"), ByteString("""{ "x": "15 b" }""")), // 15 bytes
      (parse.tolerantJson(maxLength = MaxLength15), None, ByteString("""{ "x": "15 b" }""")),             // 15 bytes
      (
        parse.form(Form("myfield" -> of[String](stringFormat)), maxLength = Some(MaxLength15)),
        Some("application/x-www-form-urlencoded"),
        ByteString("myfield=15bytes") // 15 bytes
      ),
      (parse.formUrlEncoded(maxLength = MaxLength15), Some("application/x-www-form-urlencoded"), Body15),
      (parse.tolerantFormUrlEncoded(maxLength = MaxLength15), None, Body15),
      (
        parse.multipartFormData(maxLength = MaxLength15),
        Some("multipart/form-data; boundary=aabbccddeee"),
        ByteString("--aabbccddeee--") // 15 bytes
      ),
      (parse.raw(maxLength = MaxLength15), None, Body15),
      (
        parse.file(parse.temporaryFileCreator.create("requestBody", "asTemporaryFile"), maxLength = MaxLength15),
        None,
        Body15
      ),
      (parse.temporaryFile(maxLength = MaxLength15), None, Body15),
    )
    // maxLength body parser needs special treatment because it uses Either[MaxSizeExceeded,...] instead of Either[Result,...]
    val maxLengthParser = parse.maxLength(MaxLength15, BodyParser(req => bodyParser._1)) // bodyParser._1 does not matter really

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
              .apply(contentType.map(ct => reqCLH16.withHeaders((HeaderNames.CONTENT_TYPE, ct))).getOrElse(reqCLH16)),
            food = data,
            ai = ai
          )
          enforceMaxLengthEnforced(result)
          ai.get must_== 0 // makes sure no parsing took place at all
        }
      }

      // special treatment for maxLength
      maxLengthParser.toString() in {
        val ai     = new AtomicInteger()
        val result = feed(maxLengthParser.apply(reqCLH16), food = Body15, ai = ai)
        maxLengthParserEnforced(result, MaxLength15)
        ai.get must_== 0 // makes sure no parsing took place at all
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
              .apply(contentType.map(ct => reqCLH15.withHeaders((HeaderNames.CONTENT_TYPE, ct))).getOrElse(reqCLH15)),
            food = data,
            ai = ai
          )
          result must beRight // successfully parsed
          ai.get must_== 1    // also makes sure parsing took place
        }
      }

      // special treatment for maxLength
      maxLengthParser.toString() in {
        val ai     = new AtomicInteger()
        val result = feed(maxLengthParser.apply(reqCLH15), food = Body15, ai = ai)
        result must beRight.which { inner =>
          inner must beRight(Body15)
        }
        ai.get must_== 1 // makes sure parsing took place
      }
    }

    "run body parser when no Content-Length header exists and actual body size does not exceed maxLength" in {
      Fragment.foreach(bodyParsers) { bodyParser =>
        val (parser, contentType, data) = bodyParser
        parser.toString >> {
          val ai = new AtomicInteger()
          val result = feed(
            parser
              .apply(contentType.map(ct => req.withHeaders((HeaderNames.CONTENT_TYPE, ct))).getOrElse(req)),
            food = data,
            ai = ai
          )
          result must beRight // successfully parsed
          ai.get must_== 1    // also makes sure parsing took place
        }
      }

      // special treatment for maxLength
      maxLengthParser.toString() in {
        val ai     = new AtomicInteger()
        val result = feed(maxLengthParser.apply(req), food = Body15, ai = ai)
        result must beRight.which { inner =>
          inner must beRight(Body15)
        }
        ai.get must_== 1 // makes sure parsing took place
      }
    }

    "run body parser when no Content-Length header exists and actual body size exceeds maxLength" in {
      Fragment.foreach(bodyParsers) { bodyParser =>
        val (parser, contentType, data) = bodyParser
        parser.toString >> {
          val ai = new AtomicInteger()
          val result = feed(
            parser
              .apply(contentType.map(ct => req.withHeaders((HeaderNames.CONTENT_TYPE, ct))).getOrElse(req)),
            food = ByteString(" ") ++ data, // prepend space to exceed maxLength by one byte
            ai = ai
          )
          enforceMaxLengthEnforced(result) // parser realised body is too large
          ai.get must_== 1                 // also makes sure parsing took place
        }
      }

      // special treatment for maxLength
      maxLengthParser.toString() in {
        val ai     = new AtomicInteger()
        val result = feed(maxLengthParser.apply(req), food = ByteString(" ") ++ Body15, ai = ai) // prepend space to exceed maxLength by one byte
        maxLengthParserEnforced(result, MaxLength15) // parser realised body is too large
        ai.get must_== 1                             // makes sure parsing took place
      }
    }
  }
}
