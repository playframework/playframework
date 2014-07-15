/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.system

import play.api.test._
import play.it.{LogBuffer, LogTester}
import ch.qos.logback.classic.Level
import org.specs2.execute.Result
import play.api.Play

class MigrationHelperSpec extends PlaySpecification {

  sequential

  def logMessage(logs: LogBuffer) = {
    logs.find(level = Some(Level.WARN), logger = Some("play"), messageContains = Some("session.maxAge")).headOption
  }

  def assertWasLogged(logs: LogBuffer): Result = {
    if (logMessage(logs).isDefined) {
      ok
    } else {
      ko("Could not find log message in \n" + logs.find().mkString("\n"))
    }
  }

  def assertWasNotLogged(logs: LogBuffer): Result = {
    logMessage(logs) match {
      case Some(msg) => ko("Expected no log message but found " + msg)
      case None => ok
    }
  }

  def runTest[T](config: (String, String)*)(block: LogBuffer => T) = {
    val app = FakeApplication(additionalConfiguration = Map(config:_*))
    try {
      LogTester.withLogBuffer { logs =>
        Play.start(app)
        block(logs)
      }
    } finally {
      Play.stop(app)
    }

  }

  "MigrationHelper" should {
    "warn when session timeout is too low raw value" in runTest("session.maxAge" -> "3600") { logs =>
      assertWasLogged(logs)
    }
    "not warn when session timeout is not set" in runTest() { logs =>
      assertWasNotLogged(logs)
    }
    "not warn when session timeout has units" in runTest("session.maxAge" -> "3600s") { logs =>
      assertWasNotLogged(logs)
    }
    "not warn when session timeout is significantly large" in runTest("session.maxAge" -> "36000000") { logs =>
      assertWasNotLogged(logs)
    }
    "not warn when migration helper is disabled" in runTest("session.maxAge" -> "3600", "play.migrationhelper" -> "disabled") { logs =>
      assertWasNotLogged(logs)
    }
  }

}
