/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.i18n.scalai18n {
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.test._

import play.api._
import play.api.mvc._
import play.api.i18n._

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ScalaI18nSpec extends PlaySpecification with Controller {
  lazy val app = FakeApplication(additionalConfiguration = Map(
    "messages.path" -> "scalaguide/i18n"
  ))

  "A Scala translation" should {
    "escape single quotes" in {
      running(app) {
//#apostrophe-messages
        Messages("info.error") == "You aren't logged in!"
//#apostrophe-messages
      }
    }

    "escape parameter substitution" in {
      running(app) {
//#parameter-escaping
        Messages("example.formatting") == "When using MessageFormat, '{0}' is replaced with the first parameter."
//#parameter-escaping
      }
    }
  }

}

}
