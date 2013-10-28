/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.i18n

import play.api.test.{PlaySpecification, WithApplication, FakeApplication}
import play.api.mvc.Controller
import play.api.i18n._
import play.api.Mode


object MessagesSpec extends PlaySpecification with Controller {

  "Messages" should {
    val app = FakeApplication()
    "provide default messages" in new WithApplication(app) {
      val msg = Messages("constraint.email")(Lang("en-US"))

      msg must ===("Email")
    }
    "permit default override" in new WithApplication(app) {
      val msg = Messages("constraint.required")(Lang("en-US"))

      msg must ===("Required!")
    }
  }
}


