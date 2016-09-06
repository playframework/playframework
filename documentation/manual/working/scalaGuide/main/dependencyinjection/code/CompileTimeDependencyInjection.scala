/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.dependencyinjection

import java.io.File

import org.specs2.mutable.Specification

class CompileTimeDependencyInjection extends Specification {

  import play.api._

  val environment = Environment(new File("."), this.getClass.getClassLoader, Mode.Test)

  "compile time dependency injection" should {
    "allow creating an application with the built in components from context" in {
      val context = ApplicationLoader.createContext(environment,
        Map("play.application.loader" -> classOf[basic.MyApplicationLoader].getName)
      )
      val components = new wscomponent.MyComponents(context)
      val application = ApplicationLoader(context).load(context)
      application must beAnInstanceOf[Application]
      components.router.documentation must beEmpty
    }
    "allow using other components" in {
      val context = ApplicationLoader.createContext(environment)
      val components = new wscomponent.MyComponents(context)
      components.application must beAnInstanceOf[Application]
      components.myComponent must beAnInstanceOf[wscomponent.MyComponent]
    }
    "allow declaring a custom router" in {
      val context = ApplicationLoader.createContext(environment)
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

class MyApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    new MyComponents(context).application
  }
}

class MyComponents(context: Context) extends BuiltInComponentsFromContext(context) {
  lazy val router = Router.empty
}
//#basic

//#basicextended
class MyApplicationLoaderWithInitialization extends ApplicationLoader {
  def load(context: Context) = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment)
    }
    new MyComponents(context).application
  }
}
//#basicextended

}

package wscomponent {

import play.api._
import play.api.ApplicationLoader.Context
import play.api.routing.Router

//#wscomponent
import play.api.libs.ws._
import play.api.libs.ws.ahc._

class MyComponents(context: Context) extends BuiltInComponentsFromContext(context) with AhcWSComponents {
  lazy val router = Router.empty

  lazy val myComponent = new MyComponent(wsClient)
}

class MyComponent(ws: WSClient) {
  // ...
}
//#wscomponent

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
import router.Routes

class MyApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    new MyComponents(context).application
  }
}

class MyComponents(context: Context) extends BuiltInComponentsFromContext(context) {

  lazy val router = new Routes(httpErrorHandler, applicationController, barRoutes, assets)

  lazy val barRoutes = new bar.Routes(httpErrorHandler)
  lazy val applicationController = new controllers.Application()
  lazy val assets = new controllers.Assets(httpErrorHandler)
}
//#routers

}

package controllers {

  import play.api.http.HttpErrorHandler
  import play.api.mvc._

  class Application extends Controller {
    def index = Action(Ok)
    def foo = Action(Ok)
  }

  class Assets(errorHandler: HttpErrorHandler) extends _root_.controllers.AssetsBuilder(errorHandler)
}
