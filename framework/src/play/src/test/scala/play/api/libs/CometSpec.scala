/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ ActorMaterializer, Materializer }
import akka.util.{ ByteString, Timeout }
import org.specs2.mutable._
import play.api.PlayCoreTestApplication
import play.api.http.ContentTypes
import play.api.libs.json.{ JsString, JsValue }
import play.api.mvc._
import play.core.test.FakeRequest

import scala.concurrent.{ Await, Future }

class CometSpec extends Specification {

  class MockController(val materializer: Materializer, action: ActionBuilder[Request, AnyContent]) extends ControllerHelpers {

    val Action = action

    //#comet-string
    def cometString = action {
      implicit val m = materializer
      def stringSource: Source[String, _] = Source(List("kiki", "foo", "bar"))
      Ok.chunked(stringSource via Comet.string("parent.cometMessage")).as(ContentTypes.HTML)
    }
    //#comet-string

    //#comet-json
    def cometJson = action {
      implicit val m = materializer
      def stringSource: Source[JsValue, _] = Source(List(JsString("jsonString")))
      Ok.chunked(stringSource via Comet.json("parent.cometMessage")).as(ContentTypes.HTML)
    }
    //#comet-json
  }

  def newTestApplication(): play.api.Application = new PlayCoreTestApplication() {
    override lazy val actorSystem = ActorSystem()
    override lazy val materializer = ActorMaterializer()(actorSystem)
  }

  "play comet" should {

    "work with string" in {
      val app = newTestApplication()
      try {
        implicit val m = app.materializer
        val controller = new MockController(m, ActionBuilder.ignoringBody)
        val result = controller.cometString.apply(FakeRequest())
        contentAsString(result) must contain("<html><body><script>parent.cometMessage('kiki');</script><script>parent.cometMessage('foo');</script><script>parent.cometMessage('bar');</script>")
      } finally {
        app.stop()
      }
    }

    "work with json" in {
      val app = newTestApplication()
      try {
        implicit val m = app.materializer
        val controller = new MockController(m, ActionBuilder.ignoringBody)
        val result = controller.cometJson.apply(FakeRequest())
        contentAsString(result) must contain("<html><body><script>parent.cometMessage(\"jsonString\");</script>")
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
