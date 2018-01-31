/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.ByteString
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import play.api.http.{ DefaultHttpErrorHandler, ParserConfiguration, Status }
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.libs.streams.Accumulator
import play.api.{ Configuration, Environment }
import play.core.test.FakeRequest

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future, Promise }
import scala.util.{ Failure, Try }

/**
 * All tests relating to max length handling
 */
class MaxLengthBodyParserSpec extends Specification with AfterAll {

  val MaxLength10 = 10
  val MaxLength20 = 20
  val Body15 = ByteString("hello" * 3)
  val req = FakeRequest("GET", "/x")

  implicit val system = ActorSystem()
  import system.dispatcher
  implicit val mat = ActorMaterializer()
  val parse = PlayBodyParsers()

  override def afterAll: Unit = {
    system.terminate()
  }

  def bodyParser: (Accumulator[ByteString, Either[Result, ByteString]], Future[Unit]) = {
    val bodyParsed = Promise[Unit]()
    val parser = Accumulator(Sink.seq[ByteString].mapMaterializedValue(future =>
      future.transform({ bytes =>
        bodyParsed.success(())
        Right(bytes.fold(ByteString.empty)(_ ++ _))
      }, { t =>
        bodyParsed.failure(t)
        t
      })
    ))
    (parser, bodyParsed.future)
  }

  def feed[A](accumulator: Accumulator[ByteString, A]): A = {
    Await.result(accumulator.run(Source.fromIterator(() => Body15.grouped(3))), 5.seconds)
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

  def maxLengthParserEnforced(result: Either[Result, Either[MaxSizeExceeded, ByteString]]) = {
    result must beRight[Either[MaxSizeExceeded, ByteString]].which { inner =>
      inner must beLeft(MaxSizeExceeded(MaxLength10))
    }
  }

  "Max length body handling" should {

    "be exceeded when using the default max length handling" in {
      val (parser, parsed) = bodyParser
      val result = feed(parse.enforceMaxLength(req, MaxLength10, parser))
      enforceMaxLengthEnforced(result)
      assertDidNotParse(parsed)
    }

    "be exceeded when using the maxLength body parser" in {
      val (parser, parsed) = bodyParser
      val result = feed(parse.maxLength(MaxLength10, BodyParser(req => parser)).apply(req))
      maxLengthParserEnforced(result)
      assertDidNotParse(parsed)
    }

    "be exceeded when using the maxLength body parser and an equal enforceMaxLength" in {
      val (parser, parsed) = bodyParser
      val result = feed(parse.maxLength(MaxLength10, BodyParser(req => parse.enforceMaxLength(req, MaxLength10, parser))).apply(req))
      maxLengthParserEnforced(result)
      assertDidNotParse(parsed)
    }

    "be exceeded when using the maxLength body parser and a longer enforceMaxLength" in {
      val (parser, parsed) = bodyParser
      val result = feed(parse.maxLength(MaxLength10, BodyParser(req => parse.enforceMaxLength(req, MaxLength20, parser))).apply(req))
      maxLengthParserEnforced(result)
      assertDidNotParse(parsed)
    }

    "be exceeded when using enforceMaxLength and a longer maxLength body parser" in {
      val (parser, parsed) = bodyParser
      val result = feed(parse.maxLength(MaxLength20, BodyParser(req => parse.enforceMaxLength(req, MaxLength10, parser))).apply(req))
      enforceMaxLengthEnforced(result)
      assertDidNotParse(parsed)
    }

    "not be exceeded when nothing is exceeded" in {
      val (parser, parsed) = bodyParser
      val result = feed(parse.maxLength(MaxLength20, BodyParser(req => parse.enforceMaxLength(req, MaxLength20, parser))).apply(req))
      result must beRight.which { inner =>
        inner must beRight(Body15)
      }
      Await.result(parsed, 5.seconds) must_== (())
    }

  }

}
