/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.i18n.scalai18n {
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.test._

import play.api._
import play.api.mvc._
import play.api.i18n.{DefaultLangs, DefaultMessagesApi, Messages, MessagesApi}

@RunWith(classOf[JUnitRunner])
class ScalaI18nSpec extends PlaySpecification with Controller {

//#i18n-support
  import play.api.i18n.I18nSupport
  class MyController(val messagesApi: MessagesApi) extends Controller with I18nSupport {
    // ...
//#i18n-support

    "A Scala translation" should {
      "escape single quotes" in {
//#apostrophe-messages
        Messages("info.error") == "You aren't logged in!"
//#apostrophe-messages
      }

      "escape parameter substitution" in {
//#parameter-escaping
        Messages("example.formatting") == "When using MessageFormat, '{0}' is replaced with the first parameter."
//#parameter-escaping
      }
    }
  }

  val conf = Configuration.reference ++ Configuration.from(Map("play.i18n.path" -> "scalaguide/i18n"))
  val messagesApi = new DefaultMessagesApi(Environment.simple(), conf, new DefaultLangs(conf))

  new MyController(messagesApi)

}

}
