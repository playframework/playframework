/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.i18n

// #inject-langs
import javax.inject.Inject

import play.api.i18n.Lang
import play.api.i18n.Langs
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents

class ScalaI18nService @Inject()(langs: Langs) {
  val availableLangs: Seq[Lang] = langs.availables
}
// #inject-langs

import play.api.i18n.Messages
import play.api.i18n.MessagesImpl
import play.api.i18n.MessagesProvider

class ScalaLangsOperations @Inject()(langs: Langs, messagesApi: play.api.i18n.MessagesApi) {
  val lang = langs.availables.head

  // #lang-to-locale
  val locale: java.util.Locale = lang.toLocale
  // #lang-to-locale

  // #using-messages-impl
  val messages: Messages = MessagesImpl(lang, messagesApi)
  val title: String      = messages("home.title")
  // #using-messages-impl

  {
    // #using-implicit-messages-provider
    implicit val messagesProvider: MessagesProvider = {
      MessagesImpl(lang, messagesApi)
    }
    // uses implicit messages
    val title2 = Messages("home.title")
    // #using-implicit-messages-provider
  }

  {
    // #custom-message-provider
    implicit val customMessagesProvider: MessagesProvider = new MessagesProvider {
      // resolve messages at runtime
      // ###replace:   override def messages: Messages = { ... }
      override def messages: Messages = ???
    }
    // uses implicit messages
    val title3: String = Messages("home.title")
    // #custom-message-provider
  }
}

// #inject-messages-api
import play.api.i18n.MessagesApi

class MyService @Inject()(langs: Langs, messagesApi: MessagesApi) {
  val lang: Lang = langs.availables.head

  val title: String = messagesApi("home.title")(lang)
}
// #inject-messages-api

// #use-implicit-lang
class MyOtherService @Inject()(langs: Langs, messagesApi: MessagesApi) {
  implicit val lang: Lang = langs.availables.head

  lazy val title: String = messagesApi("home.title")
}
// #use-implicit-lang

import play.api.i18n.I18nSupport

class MyController @Inject()(val controllerComponents: ControllerComponents) extends BaseController with I18nSupport {

  // #lang-cookies
  def homePageInFrench = Action {
    Redirect("/user/home").withLang(Lang("fr"))
  }

  def homePageWithDefaultLang = Action {
    Redirect("/user/home").clearingLang
  }
  // #lang-cookies

  // #http-supported-langs
  def index = Action { request =>
    Ok("Languages: " + request.acceptLanguages.map(_.code).mkString(", "))
  }
  // #http-supported-langs
}

// #using-lang-implicits-trait
import play.api.i18n.LangImplicits

class MyClass @Inject()(val messagesApi: MessagesApi) extends LangImplicits {
  def convertToMessage: Unit = {
    implicit val lang      = Lang("en")
    val messages: Messages = lang2Messages // implicit conversion
  }
}
// #using-lang-implicits-trait
