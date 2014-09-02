/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
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
      ApplicationLoader(context).load(context) must beAnInstanceOf[Application]
    }
    "allow using other components" in {
      val context = ApplicationLoader.createContext(environment)
      val components = new messages.MyComponents(context)
      components.application must beAnInstanceOf[Application]
      components.myComponent must beAnInstanceOf[messages.MyComponent]
    }
  }

}

package basic {

//#basic
import play.api._
import play.api.ApplicationLoader.Context
import play.core.Router

class MyApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    new MyComponents(context).application
  }
}

class MyComponents(context: Context) extends BuiltInComponentsFromContext(context) {
  lazy val routes = Router.Null
}
//#basic

}

package messages {

import play.api._
import play.api.ApplicationLoader.Context
import play.core.Router

//#messages
import play.api.i18n._

class MyComponents(context: Context) extends BuiltInComponentsFromContext(context)
                                     with I18nComponents {
  lazy val routes = Router.Null

  lazy val myComponent = new MyComponent(messagesApi)
}

class MyComponent(messages: MessagesApi) {
  // ...
}
//#messages

}