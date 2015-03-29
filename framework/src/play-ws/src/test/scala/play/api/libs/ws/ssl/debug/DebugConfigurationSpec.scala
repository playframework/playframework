/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl.debug

import org.specs2.mutable.Specification
import org.specs2.specification.After
import play.api.libs.ws.ssl.SSLDebugConfig

object DebugConfigurationSpec extends Specification with After {

  def after = {
    System.clearProperty("java.security.debug")
    System.clearProperty("javax.net.debug")
  }

  sequential // global settings, must be sequential

  // Loggers not needed, but useful to doublecheck that the code is doing what it should.
  // ./build test-only play.api.libs.ws.ssl.debug.DebugConfigurationSpec
  val internalDebugLogger = org.slf4j.LoggerFactory.getLogger("play.api.libs.ws.ssl.debug.FixInternalDebugLogging")
  val certpathDebugLogger = org.slf4j.LoggerFactory.getLogger("play.api.libs.ws.ssl.debug.FixCertpathDebugLogging")

  def setLoggerDebug(slf4jLogger: org.slf4j.Logger) {
    val logbackLogger = slf4jLogger.asInstanceOf[ch.qos.logback.classic.Logger]
    logbackLogger.setLevel(ch.qos.logback.classic.Level.DEBUG)
  }

  "configure" should {

    "turn on java.security.debug code" in {
      //setLoggerDebug(certpathDebugLogger)

      Option(System.getProperty("java.security.debug")) must beLike {
        case Some(value) => value must be empty
        case None => ok
      }

      val debugConfig = SSLDebugConfig(certpath = true)
      val config = new DebugConfiguration()
      config.configure(debugConfig)

      System.getProperty("java.security.debug") must contain("certpath")
    }

    "turn off java.security.debug code" in {
      System.setProperty("java.security.debug", "certpath")

      val debugConfig = SSLDebugConfig(certpath = false)
      val config = new DebugConfiguration()
      config.configure(debugConfig)

      System.getProperty("java.security.debug") must not contain ("certpath")
    }

    "turn on javax.ssl.debug code" in {
      //setLoggerDebug(internalDebugLogger)

      Option(System.getProperty("javax.net.debug")) must beLike {
        case Some(value) => value must be empty
        case None => ok
      }

      val debugConfig = SSLDebugConfig(ssl = true)
      val config = new DebugConfiguration()
      config.configure(debugConfig)

      System.getProperty("javax.net.debug") must contain("ssl")
    }

    "turn off javax.ssl.debug code" in {
      System.setProperty("javax.net.debug", "ssl")

      val debugConfig = SSLDebugConfig(ssl = false)
      val config = new DebugConfiguration()
      config.configure(debugConfig)

      System.getProperty("javax.net.debug") must not contain ("ssl")
    }
  }

}
