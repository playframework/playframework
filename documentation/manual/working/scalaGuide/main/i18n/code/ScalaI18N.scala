/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.i18n.scalai18n {
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.test._

import play.api._
import play.api.mvc._
import play.api.i18n._

@RunWith(classOf[JUnitRunner])
class ScalaI18nSpec extends PlaySpecification with Controller {

//#i18n-support
  import javax.inject.Inject
  import play.api.i18n.I18nSupport
  class MyController @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {
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
  val messagesApi = new DefaultMessagesApiProvider(Environment.simple(), conf, new DefaultLangsProvider(conf).get).get

  new MyController(messagesApi)

}

}
