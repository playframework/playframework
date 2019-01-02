/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification
import play.core.test._

class ApplicationSpec extends Specification {

  "Scala Application" should {

    "honors Environment mode" in {
      "Mode.Test" in withApplication(Environment.simple(mode = Mode.Test)) { application =>
        application.isTest must beTrue
      }
      "Mode.Dev" in withApplication(Environment.simple(mode = Mode.Dev)) { application =>
        application.isDev must beTrue
      }
      "Mode.Prod" in withApplication(Environment.simple(mode = Mode.Prod)) { application =>
        application.isProd must beTrue
      }
    }

    "when converting to Java application" should {

      "preserve environment" in {
        "test mode" in withApplication(Environment.simple(mode = Mode.Test)) { application =>
          val javaApplication = application.asJava
          javaApplication.isTest must beTrue
          javaApplication.environment().mode() must beEqualTo(play.Mode.TEST)
        }
        "dev mode" in withApplication(Environment.simple(mode = Mode.Dev)) { application =>
          val javaApplication = application.asJava
          javaApplication.isDev must beTrue
          javaApplication.environment().mode() must beEqualTo(play.Mode.DEV)
        }
        "prod mode" in withApplication(Environment.simple(mode = Mode.Prod)) { application =>
          val javaApplication = application.asJava
          javaApplication.isProd must beTrue
          javaApplication.environment().mode() must beEqualTo(play.Mode.PROD)
        }
      }

      "preserve configuration" in withApplicationAndConfig(
        Environment.simple(),
        ConfigFactory.parseString("test.config = 10")
      ) { application =>
          val javaApplication = application.asJava
          javaApplication.config().getInt("test.config") must beEqualTo(10)
        }
    }
  }
}
