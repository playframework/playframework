/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.advanced.extending

import org.specs2.mutable.Specification
import play.api._
import play.api.i18n._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.mvc.Http.RequestHeader

class MyMessagesApi extends MessagesApi {
  override def messages: Map[String, Map[String, String]]                                  = ???
  override def preferred(candidates: Seq[Lang]): Messages                                  = ???
  override def preferred(request: mvc.RequestHeader): Messages                             = ???
  override def preferred(request: RequestHeader): Messages                                 = ???
  override def langCookieHttpOnly: Boolean                                                 = ???
  override def clearLang(result: Result): Result                                           = ???
  override def langCookieSecure: Boolean                                                   = ???
  override def langCookieName: String                                                      = ???
  override def setLang(result: Result, lang: Lang): Result                                 = ???
  override def apply(key: String, args: Any*)(implicit lang: Lang): String                 = ???
  override def apply(keys: Seq[String], args: Any*)(implicit lang: Lang): String           = ???
  override def isDefinedAt(key: String)(implicit lang: Lang): Boolean                      = ???
  override def translate(key: String, args: Seq[Any])(implicit lang: Lang): Option[String] = ???
}

class MyMessagesApiProvider extends javax.inject.Provider[MyMessagesApi] {
  override def get(): MyMessagesApi = new MyMessagesApi
}

// #module-definition
class MyCode {
  // add functionality here
}

class MyModule extends play.api.inject.Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(bind[MyCode].toInstance(new MyCode))
  }
}
// #module-definition

// #builtin-module-definition
class MyI18nModule extends play.api.inject.Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[Langs].toProvider[DefaultLangsProvider],
      bind[MessagesApi].toProvider[MyMessagesApiProvider]
    )
  }
}
// #builtin-module-definition

class ScalaExtendingPlay extends Specification {

  "Extending Play" should {

    "adds a module" in {
      // #module-bindings
      val application = new GuiceApplicationBuilder()
        .bindings(new MyModule)
        .build()
      val myCode = application.injector.instanceOf(classOf[MyCode])
      myCode must beAnInstanceOf[MyCode]
      // #module-bindings
    }

    "overrides a built-in module" in {
      // #builtin-module-overrides
      val application = new GuiceApplicationBuilder()
        .overrides(new MyI18nModule)
        .build()
      // #builtin-module-overrides
      val messageApi = application.injector.instanceOf(classOf[MessagesApi])
      messageApi must beAnInstanceOf[MyMessagesApi]
    }

  }

}
