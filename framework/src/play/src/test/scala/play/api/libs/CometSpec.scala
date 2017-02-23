/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs

import akka.stream.Materializer
import akka.stream.scaladsl._
import akka.util.{ ByteString, Timeout }
import org.specs2.mutable._
import play.api.http.ContentTypes
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.{ JsString, JsValue }
import play.api.mvc.{ Action, Controller, Result, Results }
import play.core.test.FakeRequest

import scala.concurrent.{ Await, Future }

class CometSpec extends Specification {

  class MockController(val materializer: Materializer) extends Controller {

    //#comet-string
    def cometString = Action {
      implicit val m = materializer
      def stringSource: Source[String, _] = Source(List("kiki", "foo", "bar"))
      Ok.chunked(stringSource via Comet.string("parent.cometMessage")).as(ContentTypes.HTML)
    }
    //#comet-string

    //#comet-json
    def cometJson = Action {
      implicit val m = materializer
      def stringSource: Source[JsValue, _] = Source(List(JsString("jsonString")))
      Ok.chunked(stringSource via Comet.json("parent.cometMessage")).as(ContentTypes.HTML)
    }
    //#comet-json
  }

  "play comet" should {

    "work with enumerator" in {
      val result = Results.Ok.chunked(Enumerator("foo", "bar", "baz") &> Comet("callback.method"))
      result.body.contentType must beSome(ContentTypes.HTML)
    }

    "work with string" in {
      val app = new GuiceApplicationBuilder().build()
      try {
        implicit val m = app.materializer
        val controller = new MockController(m)
        val result = controller.cometString.apply(FakeRequest())
        contentAsString(result) must contain("<html><body><script type=\"text/javascript\">parent.cometMessage('kiki');</script><script type=\"text/javascript\">parent.cometMessage('foo');</script><script type=\"text/javascript\">parent.cometMessage('bar');</script>")
      } finally {
        app.stop()
      }
    }

    "work with json" in {
      val app = new GuiceApplicationBuilder().build()
      try {
        implicit val m = app.materializer
        val controller = new MockController(m)
        val result = controller.cometJson.apply(FakeRequest())
        contentAsString(result) must contain("<html><body><script type=\"text/javascript\">parent.cometMessage(\"jsonString\");</script>")
      } finally {
        app.stop()
      }
    }

  }

  //---------------------------------------------------------------------------
  // Can't use play.api.test.ResultsExtractor here as it is not imported
  // So, copy the methods necessary to extract string.

  import scala.concurrent.duration._

  implicit def timeout: Timeout = 20.seconds

  def charset(of: Future[Result]): Option[String] = {
    Await.result(of, timeout.duration).body.contentType match {
      case Some(s) if s.contains("charset=") => Some(s.split("; *charset=").drop(1).mkString.trim)
      case _ => None
    }
  }

  /**
   * Extracts the content as String.
   */
  def contentAsString(of: Future[Result])(implicit mat: Materializer): String =
    contentAsBytes(of).decodeString(charset(of).getOrElse("utf-8"))

  /**
   * Extracts the content as bytes.
   */
  def contentAsBytes(of: Future[Result])(implicit mat: Materializer): ByteString = {
    val result = Await.result(of, timeout.duration)
    Await.result(result.body.consumeData, timeout.duration)
  }

}
