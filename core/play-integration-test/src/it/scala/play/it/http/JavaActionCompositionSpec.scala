/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
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
import play.mvc.{ EssentialFilter, Result, Results, Security }
import play.mvc.Http._
import play.routing.{ Router => JRouter }

class GuiceJavaActionCompositionSpec extends JavaActionCompositionSpec {

  sequential

  override def makeRequest[T](controller: MockController, configuration: Map[String, AnyRef] = Map.empty, allowHttpContext: Boolean = true)(block: WSResponse => T): T = {
    val oldThreadLocal = Context.current
    try {
      if (!allowHttpContext) {
        Context.current = null
      }
      implicit val port = testServerPort
      lazy val app: Application = GuiceApplicationBuilder().configure(configuration).routes {
        case _ => JAction(app, controller)
      }.build()

      running(TestServer(port, app)) {
        val response = await(wsUrl("/").get())
        block(response)
      }
    } finally {
      if (!allowHttpContext) {
        Context.current = oldThreadLocal
      }
    }
  }
}

class BuiltInComponentsJavaActionCompositionSpec extends JavaActionCompositionSpec {

  sequential

  def context(initialSettings: Map[String, AnyRef]): play.ApplicationLoader.Context = {
    import scala.collection.JavaConverters._
    play.ApplicationLoader.create(play.Environment.simple(), initialSettings.asJava)
  }

  override def makeRequest[T](controller: MockController, configuration: Map[String, AnyRef], allowHttpContext: Boolean = true)(block: (WSResponse) => T): T = {
    val oldThreadLocal = Context.current
    try {
      if (!allowHttpContext) {
        Context.current = null
      }
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
            .addAction(classOf[ActionCompositionOrderTest.FirstAction], new JSupplier[ActionCompositionOrderTest.FirstAction] {
              override def get(): ActionCompositionOrderTest.FirstAction = new ActionCompositionOrderTest.FirstAction()
            })
            .addAction(classOf[ActionCompositionOrderTest.SecondAction], new JSupplier[ActionCompositionOrderTest.SecondAction] {
              override def get(): ActionCompositionOrderTest.SecondAction = new ActionCompositionOrderTest.SecondAction()
            })
            .addAction(classOf[ActionCompositionOrderTest.SomeActionAnnotationAction], new JSupplier[ActionCompositionOrderTest.SomeActionAnnotationAction] {
              override def get(): ActionCompositionOrderTest.SomeActionAnnotationAction = new ActionCompositionOrderTest.SomeActionAnnotationAction()
            })
            .addAction(classOf[ActionCompositionOrderTest.ContextArgsSetAction], new JSupplier[ActionCompositionOrderTest.ContextArgsSetAction] {
              override def get(): ActionCompositionOrderTest.ContextArgsSetAction = new ActionCompositionOrderTest.ContextArgsSetAction()
            })
            .addAction(classOf[ActionCompositionOrderTest.ContextArgsGetAction], new JSupplier[ActionCompositionOrderTest.ContextArgsGetAction] {
              override def get(): ActionCompositionOrderTest.ContextArgsGetAction = new ActionCompositionOrderTest.ContextArgsGetAction()
            })
            .addAction(classOf[ActionCompositionOrderTest.NoopUsingRequestAction], new JSupplier[ActionCompositionOrderTest.NoopUsingRequestAction] {
              override def get(): ActionCompositionOrderTest.NoopUsingRequestAction = new ActionCompositionOrderTest.NoopUsingRequestAction()
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
            .map(c => c.getDeclaredConstructor().newInstance().asInstanceOf[ActionCreator])
            .getOrElse(new DefaultActionCreator)
        }
      }

      running(TestServer(port, components.application().asScala())) {
        val response = await(wsUrl("/").get())
        block(response)
      }
    } finally {
      if (!allowHttpContext) {
        Context.current = oldThreadLocal
      }
    }
  }
}

trait JavaActionCompositionSpec extends PlaySpecification with WsTestClient {

  def makeRequest[T](controller: MockController, configuration: Map[String, AnyRef] = Map.empty, allowHttpContext: Boolean = true)(block: WSResponse => T): T

  "When action composition is configured to invoke controller first" should {
    "execute controller composition before action composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.controllerAnnotationsFirst" -> "true")) { response =>
      response.body must beEqualTo("java.lang.Classcontrollerjava.lang.reflect.Methodaction")
    }

    "execute controller composition when action is not annotated" in makeRequest(new ComposedController {
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.controllerAnnotationsFirst" -> "true")) { response =>
      response.body must beEqualTo("java.lang.Classcontroller")
    }
  }

  "When action composition is configured to invoke action first" should {
    "execute action composition before controller composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.controllerAnnotationsFirst" -> "false")) { response =>
      response.body must beEqualTo("java.lang.reflect.Methodactionjava.lang.Classcontroller")
    }

    "execute action composition when controller is not annotated" in makeRequest(new MockController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map("play.http.actionComposition.controllerAnnotationsFirst" -> "false")) { response =>
      response.body must beEqualTo("java.lang.reflect.Methodaction")
    }

    "execute action composition first is the default" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }) { response =>
      response.body must beEqualTo("java.lang.reflect.Methodactionjava.lang.Classcontroller")
    }
  }

  "Java action composition" should {
    "ensure the right request is set when the context is modified down the chain" in makeRequest(new MockController {
      @WithUsername("foo")
      def action = Results.ok(request.attrs().get(Security.USERNAME))
    }) { response =>
      response.body must_== "foo"
    }
    "ensure context.withRequest in an Action maintains Session" in makeRequest(new MockController {
      @WithUsername("foo")
      def action = {
        session.clear()
        Results.ok(request.attrs().get(Security.USERNAME))
      }
    }) { response =>
      val setCookie = response.headers.get("Set-Cookie").mkString("\n")
      setCookie must contain("PLAY_SESSION=; Max-Age=0")
      response.body must_== "foo"
    }
    "ensure context.withRequest in an Action maintains Flash" in makeRequest(new MockController {
      @WithUsername("foo")
      def action = {
        flash.clear()
        Results.ok(request.attrs().get(Security.USERNAME))
      }
    }) { response =>
      val setCookie = response.headers.get("Set-Cookie").mkString("\n")
      setCookie must contain("PLAY_FLASH=; Max-Age=0")
      response.body must_== "foo"
    }
    "ensure context.withRequest in an Action maintains Response" in makeRequest(new MockController {
      @WithUsername("foo")
      def action = {
        response.setCookie(Cookie.builder("foo", "bar").build())
        Results.ok(request.attrs().get(Security.USERNAME))
      }
    }) { response =>
      val setCookie = response.headers.get("Set-Cookie").mkString("\n")
      setCookie must contain("foo=bar")
      response.body must_== "foo"
    }

    "run a single @Repeatable annotation on a controller type" in makeRequest(new SingleRepeatableOnTypeController()) { response =>
      response.body must beEqualTo("""java.lang.Classaction1
                                     |java.lang.Classaction2""".stripMargin.replaceAll(System.lineSeparator, ""))
    }

    "run a single @Repeatable annotation on a controller action" in makeRequest(new SingleRepeatableOnActionController()) { response =>
      response.body must beEqualTo("""java.lang.reflect.Methodaction1
                                     |java.lang.reflect.Methodaction2""".stripMargin.replaceAll(System.lineSeparator, ""))
    }

    "run multiple @Repeatable annotations on a controller type" in makeRequest(new MultipleRepeatableOnTypeController()) { response =>
      response.body must beEqualTo("""java.lang.Classaction1
                                     |java.lang.Classaction2
                                     |java.lang.Classaction1
                                     |java.lang.Classaction2""".stripMargin.replaceAll(System.lineSeparator, ""))
    }

    "run multiple @Repeatable annotations on a controller action" in makeRequest(new MultipleRepeatableOnActionController()) { response =>
      response.body must beEqualTo("""java.lang.reflect.Methodaction1
                                     |java.lang.reflect.Methodaction2
                                     |java.lang.reflect.Methodaction1
                                     |java.lang.reflect.Methodaction2""".stripMargin.replaceAll(System.lineSeparator, ""))
    }

    "run single @Repeatable annotation on a controller type and a controller action" in makeRequest(new SingleRepeatableOnTypeAndActionController()) { response =>
      response.body must beEqualTo("""java.lang.reflect.Methodaction1
                                     |java.lang.reflect.Methodaction2
                                     |java.lang.Classaction1
                                     |java.lang.Classaction2""".stripMargin.replaceAll(System.lineSeparator, ""))
    }

    "run multiple @Repeatable annotations on a controller type and a controller action" in makeRequest(new MultipleRepeatableOnTypeAndActionController()) { response =>
      response.body must beEqualTo("""java.lang.reflect.Methodaction1
                                     |java.lang.reflect.Methodaction2
                                     |java.lang.reflect.Methodaction1
                                     |java.lang.reflect.Methodaction2
                                     |java.lang.Classaction1
                                     |java.lang.Classaction2
                                     |java.lang.Classaction1
                                     |java.lang.Classaction2""".stripMargin.replaceAll(System.lineSeparator, ""))
    }

    "run @Repeatable action composition annotations backward compatible" in makeRequest(new RepeatableBackwardCompatibilityController()) { response =>
      response.body must beEqualTo("do_NOT_treat_me_as_container_annotation")
    }

    "run @With annotation on a controller type" in makeRequest(new WithOnTypeController()) { response =>
      response.body must beEqualTo("""java.lang.Classaction1
                                     |java.lang.Classaction2""".stripMargin.replaceAll(System.lineSeparator, ""))
    }

    "run @With annotation on a controller action" in makeRequest(new WithOnActionController()) { response =>
      response.body must beEqualTo("""java.lang.reflect.Methodaction1
                                     |java.lang.reflect.Methodaction2""".stripMargin.replaceAll(System.lineSeparator, ""))
    }

    "run @With annotations on a controller type and a controller action" in makeRequest(new WithOnTypeAndActionController()) { response =>
      response.body must beEqualTo("""java.lang.reflect.Methodaction1
                                     |java.lang.reflect.Methodaction2
                                     |java.lang.Classaction1
                                     |java.lang.Classaction2""".stripMargin.replaceAll(System.lineSeparator, ""))
    }

    "make sure ctx.args are preserved when thread-local is disabled and a call(req) was executed in between two call(ctx)" in makeRequest(new PreserveContextArgsController(), Map(
      "play.allowHttpContext" -> "false"
    ), allowHttpContext = false) { response =>
      response.body must beEqualTo("ctx.args were set")
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
      response.body must beEqualTo("actioncreatorjava.lang.reflect.Methodactionjava.lang.Classcontroller")
    }

    "execute request handler action first and controller composition before action composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.controllerAnnotationsFirst" -> "true",
      "play.http.actionComposition.executeActionCreatorActionFirst" -> "true",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actioncreatorjava.lang.Classcontrollerjava.lang.reflect.Methodaction")
    }

    "execute request handler action first with only controller composition" in makeRequest(new ComposedController {
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.executeActionCreatorActionFirst" -> "true",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actioncreatorjava.lang.Classcontroller")
    }

    "execute request handler action first with only action composition" in makeRequest(new MockController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.executeActionCreatorActionFirst" -> "true",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("actioncreatorjava.lang.reflect.Methodaction")
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
      response.body must beEqualTo("java.lang.reflect.Methodactionjava.lang.Classcontrolleractioncreator")
    }

    "execute request handler action last and controller composition before action composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.controllerAnnotationsFirst" -> "true",
      "play.http.actionComposition.executeActionCreatorActionFirst" -> "false",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("java.lang.Classcontrollerjava.lang.reflect.Methodactionactioncreator")
    }

    "execute request handler action last with only controller composition" in makeRequest(new ComposedController {
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.executeActionCreatorActionFirst" -> "false",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("java.lang.Classcontrolleractioncreator")
    }

    "execute request handler action last with only action composition" in makeRequest(new MockController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.executeActionCreatorActionFirst" -> "false",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("java.lang.reflect.Methodactionactioncreator")
    }

    "execute request handler action last is the default and controller composition before action composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.controllerAnnotationsFirst" -> "true",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("java.lang.Classcontrollerjava.lang.reflect.Methodactionactioncreator")
    }

    "execute request handler action last is the default and action composition before controller composition" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action: Result = Results.ok()
    }, Map(
      "play.http.actionComposition.controllerAnnotationsFirst" -> "false",
      "play.http.actionCreator" -> "play.it.http.ActionCompositionActionCreator")) { response =>
      response.body must beEqualTo("java.lang.reflect.Methodactionjava.lang.Classcontrolleractioncreator")
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
