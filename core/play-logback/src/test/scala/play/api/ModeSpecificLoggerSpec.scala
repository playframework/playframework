/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import org.specs2.mutable.Specification
import play.api.libs.logback.LogbackCapturingAppender

class ModeSpecificLoggerSpec extends Specification {

  sequential

  case class ModeLoggerTest(mode: Mode*) {
    private val logger = Logger(getClass).forMode(mode: _*)

    logger.info("This is info")
    logger.debug("This is debug")
  }

  "Mode-specific logger" should {
    "not log in the wrong mode" in withLoggerMode(Mode.Test) {
      val appender = LogbackCapturingAppender[ModeLoggerTest]
      ModeLoggerTest(Mode.Dev)
      appender.events.map(_.getMessage) must beEmpty
    }
    "log in the correct mode" in withLoggerMode(Mode.Dev) {
      val appender = LogbackCapturingAppender[ModeLoggerTest]
      ModeLoggerTest(Mode.Dev)
      appender.events.map(_.getMessage) must_== Seq("This is info", "This is debug")
    }
    "log in the correct mode if multiple modes" in withLoggerMode(Mode.Dev) {
      val appender = LogbackCapturingAppender[ModeLoggerTest]
      ModeLoggerTest(Mode.Dev, Mode.Test)
      appender.events.map(_.getMessage) must_== Seq("This is info", "This is debug")
    }
  }

  private def withLoggerMode[T](mode: Mode)(block: => T): T = {
    Logger.setApplicationMode(mode)
    val result = block
    Logger.unsetApplicationMode()
    LogbackCapturingAppender.detachAll()
    result
  }
}
