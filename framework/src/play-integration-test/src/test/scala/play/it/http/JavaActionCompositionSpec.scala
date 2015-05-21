package play.it.http

import play.api.Application
import play.api.libs.ws.WSResponse
import play.api.test.{ WsTestClient, TestServer, FakeApplication, PlaySpecification }
import play.it.http.ActionCompositionOrderTest.{ ActionAnnotation, ControllerAnnotation }
import play.mvc.{ Results, Result }

object JavaActionCompositionSpec extends PlaySpecification with WsTestClient {

  def makeRequest[T](controller: MockController, configuration: Map[String, _ <: Any] = Map.empty)(block: WSResponse => T) = {
    implicit val port = testServerPort
    lazy val app: Application = FakeApplication(
      withRoutes = {
        case _ => JAction(app, controller)
      },
      additionalConfiguration = configuration
    )

    running(TestServer(port, app)) {
      val response = await(wsUrl("/").get())
      block(response)
    }
  }

  "When action composition is configured to invoke controller first" should {
    "execute controller composition before action composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.controllerAnnotationsFirst" -> "true")) { response =>
      response.body must beEqualTo("controlleraction")
    }

    "execute controller composition when action is not annotated" in makeRequest(new ComposedController {
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.controllerAnnotationsFirst" -> "true")) { response =>
      response.body must beEqualTo("controller")
    }
  }

  "When action composition is configured to invoke action first" should {
    "execute action composition before controller composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.controllerAnnotationsFirst" -> "false")) { response =>
      response.body must beEqualTo("actioncontroller")
    }

    "execute action composition when controller is not annotated" in makeRequest(new MockController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.controllerAnnotationsFirst" -> "false")) { response =>
      response.body must beEqualTo("action")
    }

    "execute action composition first is the default" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }) { response =>
      response.body must beEqualTo("actioncontroller")
    }
  }

}

@ControllerAnnotation
abstract class ComposedController extends MockController {}