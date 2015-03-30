/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.advanced.dependencyinjection

import java.io.File

import org.specs2.mutable.Specification

object CompileTimeDependencyInjection extends Specification {

  import play.api._

  val environment = Environment(new File("."), CompileTimeDependencyInjection.getClass.getClassLoader, Mode.Test)

  "compile time dependency injection" should {
    "allow creating an application with the built in components from context" in {
      val context = ApplicationLoader.createContext(environment,
        Map("play.application.loader" -> classOf[basic.MyApplicationLoader].getName)
      )
      val application = ApplicationLoader(context).load(context)
      application must beAnInstanceOf[Application]
      application.routes.documentation must beEmpty
    }
    "allow using other components" in {
      val context = ApplicationLoader.createContext(environment)
      val components = new messages.MyComponents(context)
      components.application must beAnInstanceOf[Application]
      components.myComponent must beAnInstanceOf[messages.MyComponent]
    }
    "allow declaring a custom router" in {
      val context = ApplicationLoader.createContext(environment)
      val components = new routers.MyComponents(context)
      components.application must beAnInstanceOf[Application]
      components.router must beAnInstanceOf[scalaguide.advanced.dependencyinjection.Routes]
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

}

package messages {

import play.api._
import play.api.ApplicationLoader.Context
import play.api.routing.Router

//#messages
import play.api.i18n._

class MyComponents(context: Context) extends BuiltInComponentsFromContext(context)
                                     with I18nComponents {
  lazy val router = Router.empty

  lazy val myComponent = new MyComponent(messagesApi)
}

class MyComponent(messages: MessagesApi) {
  // ...
}
//#messages

}

package routers {

import scalaguide.advanced.dependencyinjection.controllers
import scalaguide.advanced.dependencyinjection.bar

object router {
  type Routes = scalaguide.advanced.dependencyinjection.Routes
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
