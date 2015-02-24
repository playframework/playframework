/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.i18n

import play.api.test.{ PlaySpecification, WithApplication }
import play.api.mvc.Controller
import play.api.i18n._
import play.api.Mode

object MessagesSpec extends PlaySpecification with Controller {

  sequential

  implicit val lang = Lang("en-US")
  import play.api.i18n.Messages.Implicits.applicationMessages

  "Messages" should {
    "provide default messages" in new WithApplication() {
      val msg = Messages("constraint.email")

      msg must ===("Email")
    }
    "permit default override" in new WithApplication() {
      val msg = Messages("constraint.required")

      msg must ===("Required!")
    }
  }

  "Messages@Java" should {
    import play.i18n._
    import java.util
    val enUS: Lang = new play.i18n.Lang(play.api.i18n.Lang("en-US"))
    "allow translation without parameters" in new WithApplication() {
      val msg = Messages.get(enUS, "constraint.email")

      msg must ===("Email")
    }
    "allow translation with any non-list parameter" in new WithApplication() {
      val msg = Messages.get(enUS, "constraint.min", "Croissant")

      msg must ===("Minimum value: Croissant")
    }
    "allow translation with any list parameter" in new WithApplication() {
      val msg = {
        val list: util.ArrayList[String] = new util.ArrayList[String]()
        list.add("Croissant")
        Messages.get(enUS, "constraint.min", list)
      }

      msg must ===("Minimum value: Croissant")
    }
  }
}

