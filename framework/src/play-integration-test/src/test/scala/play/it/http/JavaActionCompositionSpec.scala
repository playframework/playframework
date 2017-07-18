/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSResponse
import play.api.routing.Router
import play.api.test.{ PlaySpecification, TestServer, WsTestClient }
import play.core.j.MappedJavaHandlerComponents
import play.http.{ ActionCreator, DefaultActionCreator }
import play.it.http.ActionCompositionOrderTest.{ ActionAnnotation, ControllerAnnotation, WithUsername }
import play.mvc.{ EssentialFilter, Result, Results }
import play.mvc.Http.Cookie
import play.routing.{ Router => JRouter }

class GuiceJavaActionCompositionSpec extends JavaActionCompositionSpec {
  override def makeRequest[T](controller: MockController, configuration: Map[String, AnyRef] = Map.empty)(block: WSResponse => T): T = {
    implicit val port = testServerPort
    lazy val app: Application = GuiceApplicationBuilder().configure(configuration).routes {
      case _ => JAction(app, controller)
    }.build()

    running(TestServer(port, app)) {
      val response = await(wsUrl("/").get())
      block(response)
    }
  }
}

class BuiltInComponentsJavaActionCompositionSpec extends JavaActionCompositionSpec {

  def context(initialSettings: Map[String, AnyRef]): play.ApplicationLoader.Context = {
    import scala.collection.JavaConverters._
    play.ApplicationLoader.create(play.Environment.simple(), initialSettings.asJava)
  }

  override def makeRequest[T](controller: MockController, configuration: Map[String, AnyRef])(block: (WSResponse) => T): T = {
    implicit val port = testServerPort
    val components = new play.BuiltInComponentsFromContext(context(configuration)) {

      override def javaHandlerComponents(): MappedJavaHandlerComponents = {
        import java.util.function.{ Supplier => JSupplier }
        super.javaHandlerComponents()
          .addAction(classOf[ActionCompositionOrderTest.ActionComposition], new JSupplier[ActionCompositionOrderTest.ActionComposition] {
            override def get(): ActionCompositionOrderTest.ActionComposition = new ActionCompositionOrderTest.ActionComposition()
          })
          .addAction(classOf[ActionCompositionOrderTest.ControllerComposition], new JSupplier[ActionCompositionOrderTest.ControllerComposition] {
            override def get(): ActionCompositionOrderTest.ControllerComposition = new ActionCompositionOrderTest.ControllerComposition()
          })
          .addAction(classOf[ActionCompositionOrderTest.WithUsernameAction], new JSupplier[ActionCompositionOrderTest.WithUsernameAction] {
            override def get(): ActionCompositionOrderTest.WithUsernameAction = new ActionCompositionOrderTest.WithUsernameAction()
          })
      }

      override def router(): JRouter = {
        Router.from {
          case _ => JAction(application().asScala(), controller, javaHandlerComponents())
        }.asJava
      }

      override def httpFilters(): java.util.List[EssentialFilter] = java.util.Collections.emptyList()

      override def actionCreator(): ActionCreator = {
        configuration.get[Option[String]]("play.http.actionCreator")
          .map(Class.forName)
          .map(c => c.newInstance().asInstanceOf[ActionCreator])
          .getOrElse(new DefaultActionCreator)
      }
    }

    running(TestServer(port, components.application().asScala())) {
      val response = await(wsUrl("/").get())
      block(response)
    }
  }
}

trait JavaActionCompositionSpec extends PlaySpecification with WsTestClient {

  def makeRequest[T](controller: MockController, configuration: Map[String, AnyRef] = Map.empty)(block: WSResponse => T): T

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
    "ensure context.withRequest in an Action maintains Session" in makeRequest(new MockController {
      @WithUsername("foo")
      def action = {
        session.clear()
        Results.ok(request.username())
      }
    }) { response =>
      val setCookie = response.headers.get("Set-Cookie").mkString("\n")
      setCookie must contain("PLAY_SESSION=; Max-Age=-86400")
      response.body must_== "foo"
    }
    "ensure context.withRequest in an Action maintains Flash" in makeRequest(new MockController {
      @WithUsername("foo")
      def action = {
        flash.clear()
        Results.ok(request.username())
      }
    }) { response =>
      val setCookie = response.headers.get("Set-Cookie").mkString("\n")
      setCookie must contain("PLAY_FLASH=; Max-Age=-86400")
      response.body must_== "foo"
    }
    "ensure context.withRequest in an Action maintains Response" in makeRequest(new MockController {
      @WithUsername("foo")
      def action = {
        response.setCookie(Cookie.builder("foo", "bar").build())
        Results.ok(request.username())
      }
    }) { response =>
      val setCookie = response.headers.get("Set-Cookie").mkString("\n")
      setCookie must contain("foo=bar")
      response.body must_== "foo"
    }
  }

  "When action composition is configured to invoke request handler action first" should {
    "execute request handler action first and action composition before controller composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.controllerAnnotationsFirst" -> "false",
      "play.http.actionComposition.executeActionCreatorActionFirst" -> "true",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actioncreatoractioncontroller")
    }

    "execute request handler action first and controller composition before action composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.controllerAnnotationsFirst" -> "true",
      "play.http.actionComposition.executeActionCreatorActionFirst" -> "true",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actioncreatorcontrolleraction")
    }

    "execute request handler action first with only controller composition" in makeRequest(new ComposedController {
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.executeActionCreatorActionFirst" -> "true",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actioncreatorcontroller")
    }

    "execute request handler action first with only action composition" in makeRequest(new MockController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.executeActionCreatorActionFirst" -> "true",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actioncreatoraction")
    }
  }

  "When action composition is configured to invoke request handler action last" should {
    "execute request handler action last and action composition before controller composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.controllerAnnotationsFirst" -> "false",
      "play.http.actionComposition.executeActionCreatorActionFirst" -> "false",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actioncontrolleractioncreator")
    }

    "execute request handler action last and controller composition before action composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.controllerAnnotationsFirst" -> "true",
      "play.http.actionComposition.executeActionCreatorActionFirst" -> "false",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("controlleractionactioncreator")
    }

    "execute request handler action last with only controller composition" in makeRequest(new ComposedController {
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.executeActionCreatorActionFirst" -> "false",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("controlleractioncreator")
    }

    "execute request handler action last with only action composition" in makeRequest(new MockController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.executeActionCreatorActionFirst" -> "false",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actionactioncreator")
    }

    "execute request handler action last is the default and controller composition before action composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.controllerAnnotationsFirst" -> "true",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("controlleractionactioncreator")
    }

    "execute request handler action last is the default and action composition before controller composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.controllerAnnotationsFirst" -> "false",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actioncontrolleractioncreator")
    }
  }

  "When request handler is configured without action composition" should {
    "execute request handler action last without action composition" in makeRequest(new MockController {
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.executeActionCreatorActionFirst" -> "false",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actioncreator")
    }

    "execute request handler action first without action composition" in makeRequest(new MockController {
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.executeActionCreatorActionFirst" -> "true",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actioncreator")
    }
  }

}

@ControllerAnnotation
abstract class ComposedController extends MockController {}
