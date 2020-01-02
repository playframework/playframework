/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSResponse
import play.api.routing.Router
import play.api.test.PlaySpecification
import play.api.test.TestServer
import play.api.test.WsTestClient
import play.core.j.MappedJavaHandlerComponents
import play.http.ActionCreator
import play.http.DefaultActionCreator
import play.it.http.ActionCompositionOrderTest.ActionAnnotation
import play.it.http.ActionCompositionOrderTest.ControllerAnnotation
import play.it.http.ActionCompositionOrderTest.SingletonActionAnnotation
import play.it.http.ActionCompositionOrderTest.WithUsername
import play.mvc.EssentialFilter
import play.mvc.Result
import play.mvc.Results
import play.mvc.Security
import play.mvc.Http._
import play.routing.{ Router => JRouter }

class GuiceJavaActionCompositionSpec extends JavaActionCompositionSpec {
  override def makeRequest[T](
      controller: MockController,
      configuration: Map[String, AnyRef] = Map.empty
  )(block: WSResponse => T): T = {
    implicit val port = testServerPort
    lazy val app: Application = GuiceApplicationBuilder()
      .configure(configuration)
      .routes {
        case _ => JAction(app, controller)
      }
      .build()

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

  override def makeRequest[T](
      controller: MockController,
      configuration: Map[String, AnyRef]
  )(block: (WSResponse) => T): T = {
    implicit val port = testServerPort
    val components = new play.BuiltInComponentsFromContext(context(configuration)) {
      override def javaHandlerComponents(): MappedJavaHandlerComponents = {
        super
          .javaHandlerComponents()
          .addAction(
            classOf[ActionCompositionOrderTest.ActionComposition],
            () => new ActionCompositionOrderTest.ActionComposition(),
          )
          .addAction(
            classOf[ActionCompositionOrderTest.ControllerComposition],
            () => new ActionCompositionOrderTest.ControllerComposition(),
          )
          .addAction(
            classOf[ActionCompositionOrderTest.WithUsernameAction],
            () => new ActionCompositionOrderTest.WithUsernameAction(),
          )
          .addAction(
            classOf[ActionCompositionOrderTest.FirstAction],
            () => new ActionCompositionOrderTest.FirstAction(),
          )
          .addAction(
            classOf[ActionCompositionOrderTest.SecondAction],
            () => new ActionCompositionOrderTest.SecondAction(),
          )
          .addAction(
            classOf[ActionCompositionOrderTest.SomeActionAnnotationAction],
            () => new ActionCompositionOrderTest.SomeActionAnnotationAction(),
          )
      }

      override def router(): JRouter = {
        Router.from {
          case _ => JAction(application().asScala(), controller, javaHandlerComponents())
        }.asJava
      }

      override def httpFilters(): java.util.List[EssentialFilter] = java.util.Collections.emptyList()

      override def actionCreator(): ActionCreator = {
        configuration
          .get[Option[String]]("play.http.actionCreator")
          .map(Class.forName)
          .map(c => c.getDeclaredConstructor().newInstance().asInstanceOf[ActionCreator])
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
  def makeRequest[T](
      controller: MockController,
      configuration: Map[String, AnyRef] = Map.empty
  )(block: WSResponse => T): T

  "When action composition is configured to invoke controller first" should {
    "execute controller composition before action composition" in makeRequest(
      new ComposedController {
        @ActionAnnotation
        override def action(request: Request): Result = Results.ok()
      },
      Map("play.http.actionComposition.controllerAnnotationsFirst" -> "true")
    ) { response =>
      response.body must beEqualTo("java.lang.Classcontrollerjava.lang.reflect.Methodaction")
    }

    "execute controller composition when action is not annotated" in makeRequest(new ComposedController {
      override def action(request: Request): Result = Results.ok()
    }, Map("play.http.actionComposition.controllerAnnotationsFirst" -> "true")) { response =>
      response.body must beEqualTo("java.lang.Classcontroller")
    }
  }

  "When action composition is configured to invoke action first" should {
    "execute action composition before controller composition" in makeRequest(
      new ComposedController {
        @ActionAnnotation
        override def action(request: Request): Result = Results.ok()
      },
      Map("play.http.actionComposition.controllerAnnotationsFirst" -> "false")
    ) { response =>
      response.body must beEqualTo("java.lang.reflect.Methodactionjava.lang.Classcontroller")
    }

    "execute action composition when controller is not annotated" in makeRequest(
      new MockController {
        @ActionAnnotation
        override def action(request: Request): Result = Results.ok()
      },
      Map("play.http.actionComposition.controllerAnnotationsFirst" -> "false")
    ) { response =>
      response.body must beEqualTo("java.lang.reflect.Methodaction")
    }

    "execute action composition first is the default" in makeRequest(new ComposedController {
      @ActionAnnotation
      override def action(request: Request): Result = Results.ok()
    }) { response =>
      response.body must beEqualTo("java.lang.reflect.Methodactionjava.lang.Classcontroller")
    }
  }

  "Java action composition" should {
    "ensure the right request attributes are set when an attribute is added down the chain" in makeRequest(
      new MockController {
        @WithUsername("foo")
        def action(request: Request) = Results.ok(request.attrs().get(Security.USERNAME))
      }
    ) { response =>
      response.body must_== "foo"
    }
    "ensure withNewSession maintains Session" in makeRequest(new MockController {
      @WithUsername("foo")
      def action(request: Request) = {
        Results.ok(request.attrs().get(Security.USERNAME)).withNewSession()
      }
    }) { response =>
      val setCookie = response.headers.get("Set-Cookie").mkString("\n")
      setCookie must contain("PLAY_SESSION=; Max-Age=0")
      response.body must_== "foo"
    }
    "ensure withNewFlash maintains Flash" in makeRequest(new MockController {
      @WithUsername("foo")
      def action(request: Request) = {
        Results.ok(request.attrs().get(Security.USERNAME)).withNewFlash()
      }
    }) { response =>
      val setCookie = response.headers.get("Set-Cookie").mkString("\n")
      setCookie must contain("PLAY_FLASH=; Max-Age=0")
      response.body must_== "foo"
    }
    "ensure withCookies maintains custom cookies" in makeRequest(new MockController {
      @WithUsername("foo")
      def action(request: Request) = {
        Results.ok(request.attrs().get(Security.USERNAME)).withCookies(Cookie.builder("foo", "bar").build())
      }
    }) { response =>
      val setCookie = response.headers.get("Set-Cookie").mkString("\n")
      setCookie must contain("foo=bar")
      response.body must_== "foo"
    }

    "run a single @Repeatable annotation on a controller type" in makeRequest(new SingleRepeatableOnTypeController()) {
      response =>
        response.body must beEqualTo("""java.lang.Classaction1
                                       |java.lang.Classaction2""".stripMargin.replaceAll(System.lineSeparator, ""))
    }

    "run a single @Repeatable annotation on a controller action" in makeRequest(
      new SingleRepeatableOnActionController()
    ) { response =>
      response.body must beEqualTo(
        """java.lang.reflect.Methodaction1
          |java.lang.reflect.Methodaction2""".stripMargin
          .replaceAll(System.lineSeparator, "")
      )
    }

    "run multiple @Repeatable annotations on a controller type" in makeRequest(new MultipleRepeatableOnTypeController()) {
      response =>
        response.body must beEqualTo("""java.lang.Classaction1
                                       |java.lang.Classaction2
                                       |java.lang.Classaction1
                                       |java.lang.Classaction2""".stripMargin.replaceAll(System.lineSeparator, ""))
    }

    "run multiple @Repeatable annotations on a controller action" in makeRequest(
      new MultipleRepeatableOnActionController()
    ) { response =>
      response.body must beEqualTo(
        """java.lang.reflect.Methodaction1
          |java.lang.reflect.Methodaction2
          |java.lang.reflect.Methodaction1
          |java.lang.reflect.Methodaction2""".stripMargin
          .replaceAll(System.lineSeparator, "")
      )
    }

    "run single @Repeatable annotation on a controller type and a controller action" in makeRequest(
      new SingleRepeatableOnTypeAndActionController()
    ) { response =>
      response.body must beEqualTo("""java.lang.reflect.Methodaction1
                                     |java.lang.reflect.Methodaction2
                                     |java.lang.Classaction1
                                     |java.lang.Classaction2""".stripMargin.replaceAll(System.lineSeparator, ""))
    }

    "run multiple @Repeatable annotations on a controller type and a controller action" in makeRequest(
      new MultipleRepeatableOnTypeAndActionController()
    ) { response =>
      response.body must beEqualTo("""java.lang.reflect.Methodaction1
                                     |java.lang.reflect.Methodaction2
                                     |java.lang.reflect.Methodaction1
                                     |java.lang.reflect.Methodaction2
                                     |java.lang.Classaction1
                                     |java.lang.Classaction2
                                     |java.lang.Classaction1
                                     |java.lang.Classaction2""".stripMargin.replaceAll(System.lineSeparator, ""))
    }

    "run @Repeatable action composition annotations backward compatible" in makeRequest(
      new RepeatableBackwardCompatibilityController()
    ) { response =>
      response.body must beEqualTo("do_NOT_treat_me_as_container_annotation")
    }

    "run @With annotation on a controller type" in makeRequest(new WithOnTypeController()) { response =>
      response.body must beEqualTo("""java.lang.Classaction1
                                     |java.lang.Classaction2""".stripMargin.replaceAll(System.lineSeparator, ""))
    }

    "run @With annotation on a controller action" in makeRequest(new WithOnActionController()) { response =>
      response.body must beEqualTo(
        """java.lang.reflect.Methodaction1
          |java.lang.reflect.Methodaction2""".stripMargin
          .replaceAll(System.lineSeparator, "")
      )
    }

    "run @With annotations on a controller type and a controller action" in makeRequest(
      new WithOnTypeAndActionController()
    ) { response =>
      response.body must beEqualTo("""java.lang.reflect.Methodaction1
                                     |java.lang.reflect.Methodaction2
                                     |java.lang.Classaction1
                                     |java.lang.Classaction2""".stripMargin.replaceAll(System.lineSeparator, ""))
    }

    "abort the request when action class is annotated with @javax.inject.Singleton" in makeRequest(new MockController {
      @SingletonActionAnnotation
      override def action(request: Request): Result = Results.ok()
    }) { response =>
      response.status must_== 500
      response.body must contain(
        "RuntimeException: Singleton action instances are not allowed! Remove the @javax.inject.Singleton annotation from the action class play.it.http.ActionCompositionOrderTest$SingletonActionAnnotationAction"
      )
    }
  }

  "When action composition is configured to invoke request handler action first" should {
    "execute request handler action first and action composition before controller composition" in makeRequest(
      new ComposedController {
        @ActionAnnotation
        override def action(request: Request): Result = Results.ok()
      },
      Map(
        "play.http.actionComposition.controllerAnnotationsFirst"      -> "false",
        "play.http.actionComposition.executeActionCreatorActionFirst" -> "true",
        "play.http.actionCreator"                                     -> "play.it.http.ActionCompositionActionCreator"
      )
    ) { response =>
      response.body must beEqualTo("actioncreatorjava.lang.reflect.Methodactionjava.lang.Classcontroller")
    }

    "execute request handler action first and controller composition before action composition" in makeRequest(
      new ComposedController {
        @ActionAnnotation
        override def action(request: Request): Result = Results.ok()
      },
      Map(
        "play.http.actionComposition.controllerAnnotationsFirst"      -> "true",
        "play.http.actionComposition.executeActionCreatorActionFirst" -> "true",
        "play.http.actionCreator"                                     -> "play.it.http.ActionCompositionActionCreator"
      )
    ) { response =>
      response.body must beEqualTo("actioncreatorjava.lang.Classcontrollerjava.lang.reflect.Methodaction")
    }

    "execute request handler action first with only controller composition" in makeRequest(
      new ComposedController {
        override def action(request: Request): Result = Results.ok()
      },
      Map(
        "play.http.actionComposition.executeActionCreatorActionFirst" -> "true",
        "play.http.actionCreator"                                     -> "play.it.http.ActionCompositionActionCreator"
      )
    ) { response =>
      response.body must beEqualTo("actioncreatorjava.lang.Classcontroller")
    }

    "execute request handler action first with only action composition" in makeRequest(
      new MockController {
        @ActionAnnotation
        override def action(request: Request): Result = Results.ok()
      },
      Map(
        "play.http.actionComposition.executeActionCreatorActionFirst" -> "true",
        "play.http.actionCreator"                                     -> "play.it.http.ActionCompositionActionCreator"
      )
    ) { response =>
      response.body must beEqualTo("actioncreatorjava.lang.reflect.Methodaction")
    }
  }

  "When action composition is configured to invoke request handler action last" should {
    "execute request handler action last and action composition before controller composition" in makeRequest(
      new ComposedController {
        @ActionAnnotation
        override def action(request: Request): Result = Results.ok()
      },
      Map(
        "play.http.actionComposition.controllerAnnotationsFirst"      -> "false",
        "play.http.actionComposition.executeActionCreatorActionFirst" -> "false",
        "play.http.actionCreator"                                     -> "play.it.http.ActionCompositionActionCreator"
      )
    ) { response =>
      response.body must beEqualTo("java.lang.reflect.Methodactionjava.lang.Classcontrolleractioncreator")
    }

    "execute request handler action last and controller composition before action composition" in makeRequest(
      new ComposedController {
        @ActionAnnotation
        override def action(request: Request): Result = Results.ok()
      },
      Map(
        "play.http.actionComposition.controllerAnnotationsFirst"      -> "true",
        "play.http.actionComposition.executeActionCreatorActionFirst" -> "false",
        "play.http.actionCreator"                                     -> "play.it.http.ActionCompositionActionCreator"
      )
    ) { response =>
      response.body must beEqualTo("java.lang.Classcontrollerjava.lang.reflect.Methodactionactioncreator")
    }

    "execute request handler action last with only controller composition" in makeRequest(
      new ComposedController {
        override def action(request: Request): Result = Results.ok()
      },
      Map(
        "play.http.actionComposition.executeActionCreatorActionFirst" -> "false",
        "play.http.actionCreator"                                     -> "play.it.http.ActionCompositionActionCreator"
      )
    ) { response =>
      response.body must beEqualTo("java.lang.Classcontrolleractioncreator")
    }

    "execute request handler action last with only action composition" in makeRequest(
      new MockController {
        @ActionAnnotation
        override def action(request: Request): Result = Results.ok()
      },
      Map(
        "play.http.actionComposition.executeActionCreatorActionFirst" -> "false",
        "play.http.actionCreator"                                     -> "play.it.http.ActionCompositionActionCreator"
      )
    ) { response =>
      response.body must beEqualTo("java.lang.reflect.Methodactionactioncreator")
    }

    "execute request handler action last is the default and controller composition before action composition" in makeRequest(
      new ComposedController {
        @ActionAnnotation
        override def action(request: Request): Result = Results.ok()
      },
      Map(
        "play.http.actionComposition.controllerAnnotationsFirst" -> "true",
        "play.http.actionCreator"                                -> "play.it.http.ActionCompositionActionCreator"
      )
    ) { response =>
      response.body must beEqualTo("java.lang.Classcontrollerjava.lang.reflect.Methodactionactioncreator")
    }

    "execute request handler action last is the default and action composition before controller composition" in makeRequest(
      new ComposedController {
        @ActionAnnotation
        override def action(request: Request): Result = Results.ok()
      },
      Map(
        "play.http.actionComposition.controllerAnnotationsFirst" -> "false",
        "play.http.actionCreator"                                -> "play.it.http.ActionCompositionActionCreator"
      )
    ) { response =>
      response.body must beEqualTo("java.lang.reflect.Methodactionjava.lang.Classcontrolleractioncreator")
    }
  }

  "When request handler is configured without action composition" should {
    "execute request handler action last without action composition" in makeRequest(
      new MockController {
        override def action(request: Request): Result = Results.ok()
      },
      Map(
        "play.http.actionComposition.executeActionCreatorActionFirst" -> "false",
        "play.http.actionCreator"                                     -> "play.it.http.ActionCompositionActionCreator"
      )
    ) { response =>
      response.body must beEqualTo("actioncreator")
    }

    "execute request handler action first without action composition" in makeRequest(
      new MockController {
        override def action(request: Request): Result = Results.ok()
      },
      Map(
        "play.http.actionComposition.executeActionCreatorActionFirst" -> "true",
        "play.http.actionCreator"                                     -> "play.it.http.ActionCompositionActionCreator"
      )
    ) { response =>
      response.body must beEqualTo("actioncreator")
    }
  }
}

@ControllerAnnotation
abstract class ComposedController extends MockController
