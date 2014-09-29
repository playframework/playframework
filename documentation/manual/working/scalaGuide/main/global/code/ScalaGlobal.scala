/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.global.scalaglobal

import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ScalaGlobalSpec extends Specification {

  "A scala global" should {

    "hooking global to log when start and stop" in {

      //#global-hooking
      import play.api._

      object Global extends GlobalSettings {

        override def onStart(app: Application) {
          Logger.info("Application has started")
        }

        override def onStop(app: Application) {
          Logger.info("Application shutdown...")
        }

      }
      //#global-hooking

      running(FakeApplication(additionalConfiguration = Map("application.secret" -> "pass"), withGlobal = Some(Global))) {
        success
      }
    }
  }

  object SourceDemo {

    def defineGlobal {
      //#global-define
      import play.api._

      object Global extends GlobalSettings {

      }
      //#global-define

    }
  }
}
