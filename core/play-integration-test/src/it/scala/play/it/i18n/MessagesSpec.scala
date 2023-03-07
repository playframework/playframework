/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.i18n

import controllers.Execution
import play.api.i18n._
import play.api.mvc.ActionBuilder
import play.api.mvc.ControllerHelpers
import play.api.test.PlaySpecification
import play.api.test.WithApplication

class MessagesSpec extends PlaySpecification with ControllerHelpers {
  sequential

  implicit val lang: Lang = Lang("en-US")

  lazy val Action = new ActionBuilder.IgnoringBody()(Execution.trampoline)

  "Messages" should {
    "provide default messages" in new WithApplication(_.requireExplicitBindings()) {
      val messagesApi     = app.injector.instanceOf[MessagesApi]
      val javaMessagesApi = app.injector.instanceOf[play.i18n.MessagesApi]

      val msg     = messagesApi("constraint.email")
      val javaMsg = javaMessagesApi.get(new play.i18n.Lang(lang), "constraint.email")

      msg must ===("Email")
      msg must ===(javaMsg)
    }
    "permit default override" in new WithApplication(_.requireExplicitBindings()) {
      val messagesApi = app.injector.instanceOf[MessagesApi]
      val msg         = messagesApi("constraint.required")

      msg must ===("Required!")
    }
  }

  "Messages@Java" should {
    import play.i18n._
    import java.util
    val enUS: Lang = new play.i18n.Lang(play.api.i18n.Lang("en-US"))
    "allow translation without parameters" in new WithApplication() {
      val messagesApi = app.injector.instanceOf[MessagesApi]
      val msg         = messagesApi.get(enUS, "constraint.email")

      msg must ===("Email")
    }
    "allow translation with any non-list parameter" in new WithApplication() {
      val messagesApi = app.injector.instanceOf[MessagesApi]
      val msg         = messagesApi.get(enUS, "constraint.min", "Croissant")

      msg must ===("Minimum value: Croissant")
    }
    "allow translation with any list parameter" in new WithApplication() {
      val messagesApi = app.injector.instanceOf[MessagesApi]

      val msg = {
        val list: util.ArrayList[String] = new util.ArrayList[String]()
        list.add("Croissant")
        messagesApi.get(enUS, "constraint.min", list)
      }

      msg must ===("Minimum value: Croissant")
    }
  }
}
