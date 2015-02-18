/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification

object LoggerSpec extends Specification {

  val loggerConfig = Configuration(ConfigFactory.parseString("""
    logger.root = ERROR
    logger.play = INFO
    logger.application = DEBUG
    logger.org.springframework = INFO

    logger."a.b" = "WARN"
    logger."a.b.c" = "DEBUG"

    logger {
      "a.b.c.d" = "ERROR"
      "a.b.c.d.e" = "DEBUG"
    }

    logger {
      x {
        y = "WARN"
        "y.z" = "INFO"
      }
    }
  """))

  def getLoggerLevel(name: String): String = {
    org.slf4j.LoggerFactory.getLogger(name) match {
      case logger: ch.qos.logback.classic.Logger => Option(logger.getLevel).fold("")(_.toString)
      case _ => ""
    }
  }

  "Logger configuration" should {

    "set logger levels correctly" in {
      Logger.configure(Environment.simple(), loggerConfig)

      getLoggerLevel("root") must_== "ERROR"
      getLoggerLevel("play") must_== "INFO"
      getLoggerLevel("application") must_== "DEBUG"
      getLoggerLevel("org.springframework") must_== "INFO"

      getLoggerLevel("a.b") must_== "WARN"
      getLoggerLevel("a.b.c") must_== "DEBUG"

      getLoggerLevel("a.b.c.d") must_== "ERROR"
      getLoggerLevel("a.b.c.d.e") must_== "DEBUG"

      getLoggerLevel("x") must_== ""
      getLoggerLevel("x.y") must_== "WARN"
      getLoggerLevel("x.y.z") must_== "INFO"
    }

  }

}
