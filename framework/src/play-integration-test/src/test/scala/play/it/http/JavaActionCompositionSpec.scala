/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSResponse
import play.api.test.{PlaySpecification, TestServer, WsTestClient}
import play.it.http.ActionCompositionOrderTest.{ActionAnnotation, ControllerAnnotation, WithUsername}
import play.mvc.{Result, Results}

object JavaActionCompositionSpec extends PlaySpecification with WsTestClient {

  def makeRequest[T](controller: MockController, configuration: Map[String, _ <: Any] = Map.empty)(block: WSResponse => T) = {
    implicit val port = testServerPort
    lazy val app: Application = GuiceApplicationBuilder().configure(configuration).routes {
      case _ => JAction(app, controller)
    }.build()

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

  "Java action composition" should {
    "ensure the right request is set when the context is modified down the chain" in makeRequest(new MockController {
      @WithUsername("foo")
      def action = Results.ok(request.username())
    }) { response =>
      response.body must_== "foo"
    }
  }

  "When action composition is configured to invoke request handler action first" should {
    "execute request handler action first and action composition before controller composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.controllerAnnotationsFirst" -> "false",
           "play.http.actionComposition.executeActionCreatorActionFirst" -> "true",
           "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actioncreatoractioncontroller")
    }

    "execute request handler action first and controller composition before action composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.controllerAnnotationsFirst" -> "true",
           "play.http.actionComposition.executeActionCreatorActionFirst" -> "true",
           "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actioncreatorcontrolleraction")
    }

    "execute request handler action first with only controller composition" in makeRequest(new ComposedController {
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.executeActionCreatorActionFirst" -> "true",
           "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actioncreatorcontroller")
    }

    "execute request handler action first with only action composition" in makeRequest(new MockController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.executeActionCreatorActionFirst" -> "true",
           "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actioncreatoraction")
    }
  }

  "When action composition is configured to invoke request handler action last" should {
    "execute request handler action last and action composition before controller composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.controllerAnnotationsFirst" -> "false",
           "play.http.actionComposition.executeActionCreatorActionFirst" -> "false",
           "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actioncontrolleractioncreator")
    }

    "execute request handler action last and controller composition before action composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.controllerAnnotationsFirst" -> "true",
           "play.http.actionComposition.executeActionCreatorActionFirst" -> "false",
           "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("controlleractionactioncreator")
    }

    "execute request handler action last with only controller composition" in makeRequest(new ComposedController {
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.executeActionCreatorActionFirst" -> "false",
           "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("controlleractioncreator")
    }

    "execute request handler action last with only action composition" in makeRequest(new MockController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.executeActionCreatorActionFirst" -> "false",
           "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actionactioncreator")
    }

    "execute request handler action last is the default and controller composition before action composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.controllerAnnotationsFirst" -> "true",
           "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("controlleractionactioncreator")
    }

    "execute request handler action last is the default and action composition before controller composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.controllerAnnotationsFirst" -> "false",
           "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actioncontrolleractioncreator")
    }
  }

  "When request handler is configured without action composition" should {
    "execute request handler action last without action composition" in makeRequest(new MockController {
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.executeActionCreatorActionFirst" -> "false",
           "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actioncreator")
    }

    "execute request handler action first without action composition" in makeRequest(new MockController {
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.executeActionCreatorActionFirst" -> "true",
           "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actioncreator")
    }
  }

}

@ControllerAnnotation
abstract class ComposedController extends MockController {}
