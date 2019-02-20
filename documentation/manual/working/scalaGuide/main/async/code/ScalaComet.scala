/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.async.scalacomet

//#comet-imports
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import play.api.http.ContentTypes
import play.api.libs.Comet
import play.api.libs.json._
import play.api.mvc._
//#comet-imports

import play.api.test._

class MockController(val controllerComponents: ControllerComponents)(implicit materializer: Materializer)
    extends BaseController {

  //#comet-string
  def cometString = Action {
    implicit val m                      = materializer
    def stringSource: Source[String, _] = Source(List("kiki", "foo", "bar"))
    Ok.chunked(stringSource.via(Comet.string("parent.cometMessage"))).as(ContentTypes.HTML)
  }
  //#comet-string

  //#comet-json
  def cometJson = Action {
    implicit val m                     = materializer
    def jsonSource: Source[JsValue, _] = Source(List(JsString("jsonString")))
    Ok.chunked(jsonSource.via(Comet.json("parent.cometMessage"))).as(ContentTypes.HTML)
  }
  //#comet-json
}

class ScalaCometSpec extends PlaySpecification {

  "play comet" should {

    "work with string" in new WithApplication() with Injecting {
      try {
        val controllerComponents = inject[ControllerComponents]
        val controller           = new MockController(controllerComponents)
        val result               = controller.cometString.apply(FakeRequest())
        contentAsString(result) must contain(
          "<html><body><script type=\"text/javascript\">parent.cometMessage('kiki');</script><script type=\"text/javascript\">parent.cometMessage('foo');</script><script type=\"text/javascript\">parent.cometMessage('bar');</script>"
        )
      } finally {
        app.stop()
      }
    }

    "work with json" in new WithApplication() with Injecting {
      try {
        val controllerComponents = inject[ControllerComponents]
        val controller           = new MockController(controllerComponents)
        val result               = controller.cometJson.apply(FakeRequest())
        contentAsString(result) must contain(
          "<html><body><script type=\"text/javascript\">parent.cometMessage(\"jsonString\");</script>"
        )
      } finally {
        app.stop()
      }
    }

  }

}
