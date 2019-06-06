/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.dependencyinjection

import java.io.File

import org.specs2.mutable.Specification
import _root_.controllers.AssetsMetadata

class CompileTimeDependencyInjection extends Specification {

  import play.api._

  val environment = Environment(new File("."), this.getClass.getClassLoader, Mode.Test)

  "compile time dependency injection" should {
    "allow creating an application with the built in components from context" in {
      val context = ApplicationLoader.createContext(
        environment,
        Map("play.application.loader" -> classOf[basic.MyApplicationLoader].getName)
      )
      val components  = new messages.MyComponents(context)
      val application = ApplicationLoader(context).load(context)
      application must beAnInstanceOf[Application]
      components.router.documentation must beEmpty
    }
    "allow using other components" in {
      val context    = ApplicationLoader.createContext(environment)
      val components = new messages.MyComponents(context)
      components.application must beAnInstanceOf[Application]
      components.myComponent must beAnInstanceOf[messages.MyComponent]
    }
    "allow declaring a custom router" in {
      val context    = ApplicationLoader.createContext(environment)
      val components = new routers.MyComponents(context)
      components.application must beAnInstanceOf[Application]
      components.router must beAnInstanceOf[scalaguide.dependencyinjection.Routes]
    }
  }

}

package basic {

//#basic
  import play.api._
  import play.api.ApplicationLoader.Context
  import play.api.routing.Router
  import play.filters.HttpFiltersComponents

  class MyApplicationLoader extends ApplicationLoader {
    def load(context: Context) = {
      new MyComponents(context).application
    }
  }

  class MyComponents(context: Context) extends BuiltInComponentsFromContext(context) with HttpFiltersComponents {
    lazy val router = Router.empty
  }
//#basic

//#basicextended
  class MyApplicationLoaderWithInitialization extends ApplicationLoader {
    def load(context: Context) = {
      LoggerConfigurator(context.environment.classLoader).foreach {
        _.configure(context.environment, context.initialConfiguration, Map.empty)
      }
      new MyComponents(context).application
    }
  }
//#basicextended

}

package messages {

  import play.api._
  import play.api.ApplicationLoader.Context
  import play.api.routing.Router
  import play.filters.HttpFiltersComponents

//#messages
  import play.api.i18n._

  class MyComponents(context: Context)
      extends BuiltInComponentsFromContext(context)
      with I18nComponents
      with HttpFiltersComponents {
    lazy val router = Router.empty

    lazy val myComponent = new MyComponent(messagesApi)
  }

  class MyComponent(messages: MessagesApi) {
    // ...
  }
//#messages

}

package routers {

  import scalaguide.dependencyinjection.controllers
  import scalaguide.dependencyinjection.bar

  object router {
    type Routes = scalaguide.dependencyinjection.Routes
  }

//#routers
  import play.api._
  import play.api.ApplicationLoader.Context
  import play.filters.HttpFiltersComponents
  import router.Routes

  class MyApplicationLoader extends ApplicationLoader {
    def load(context: Context) = {
      new MyComponents(context).application
    }
  }

  class MyComponents(context: Context)
      extends BuiltInComponentsFromContext(context)
      with HttpFiltersComponents
      with controllers.AssetsComponents {
    lazy val barRoutes             = new bar.Routes(httpErrorHandler)
    lazy val applicationController = new controllers.Application(controllerComponents)

    lazy val router = new Routes(httpErrorHandler, applicationController, barRoutes, assets)
  }
//#routers

}

package controllers {

  import javax.inject.Inject

  import play.api.http.HttpErrorHandler
  import play.api.mvc._

  class Application @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
    def index = Action(Ok)
    def foo   = Action(Ok)
  }

  trait AssetsComponents extends _root_.controllers.AssetsComponents {
    override lazy val assets = new controllers.Assets(httpErrorHandler, assetsMetadata)
  }

  class Assets(errorHandler: HttpErrorHandler, assetsMetadata: AssetsMetadata)
      extends _root_.controllers.Assets(errorHandler, assetsMetadata)
}
