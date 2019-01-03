/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.api

import ch.qos.logback.classic.spi.ILoggingEvent
import play.api.http.SecretConfiguration
import play.api.{ Environment, Mode }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.PlaySpecification
import play.it.LogTester

import scala.util.Try

class SecretConfigurationParserSpec extends PlaySpecification {

  sequential

  def parseSecret(mode: Mode)(extraConfig: (String, String)*): (Option[String], Seq[ILoggingEvent]) = {
    Try {
      val app = GuiceApplicationBuilder(environment = Environment.simple(mode = mode))
        .configure(extraConfig: _*)
        .build()
      val (secret, events) = LogTester.recordLogEvents {
        app.httpConfiguration.secret.secret
      }
      (secret, events)
    } match {
      case scala.util.Success((secret, events)) => (Option(secret), events)
      case scala.util.Failure(_) => (None, Seq.empty)
    }
  }

  "When parsing SecretConfiguration" should {
    "in DEV mode" should {
      "return 'changeme' when it is configured to it" in {
        val (secret, events) = parseSecret(mode = Mode.Dev)("play.http.secret.key" -> "changeme")
        events.map(_.getFormattedMessage).find(_.contains("Generated dev mode secret")) must beSome
        secret must beSome
      }

      "generate a secret when no value is configured" in {
        val (secret, events) = parseSecret(mode = Mode.Dev)("play.http.secret.key" -> null)
        events.map(_.getFormattedMessage).find(_.contains("Generated dev mode secret")) must beSome
        secret must beSome
      }

      "log an warning when secret length is smaller than SHORTEST_SECRET_LENGTH chars" in {
        val (secret, events) = parseSecret(mode = Mode.Dev)("play.http.secret.key" -> ("x" * (SecretConfiguration.SHORTEST_SECRET_LENGTH - 1)))
        events.map(_.getFormattedMessage).find(_.contains("The application secret is too short and does not have the recommended amount of entropy")) must beSome
        secret must beSome
      }

      "log a warning when secret length is smaller then SHORT_SECRET_LENGTH chars" in {
        val (secret, events) = parseSecret(mode = Mode.Dev)("play.http.secret.key" -> ("x" * (SecretConfiguration.SHORT_SECRET_LENGTH - 1)))
        events.map(_.getFormattedMessage).find(_.contains("Your secret key is very short, and may be vulnerable to dictionary attacks.  Your application may not be secure")) must beSome
        secret must beSome
      }

      "return the value without warnings when it is configured respecting the requirements" in {
        val (secret, events) = parseSecret(mode = Mode.Dev)("play.http.secret.key" -> ("x" * SecretConfiguration.SHORT_SECRET_LENGTH))
        events.map(_.getFormattedMessage).find(_.contains("Your secret key is very short, and may be vulnerable to dictionary attacks.  Your application may not be secure")) must beNone
        secret must beSome
      }
    }

    "in PROD mode" should {
      "fail when value is configured to 'changeme'" in {
        val (secret, _) = parseSecret(mode = Mode.Prod)("play.http.secret.key" -> "changeme")
        secret must beNone
      }

      "fail when value is not configured" in {
        val (secret, _) = parseSecret(mode = Mode.Prod)("play.http.secret.key" -> null)
        secret must beNone
      }

      "fail when value length is smaller than SHORTEST_SECRET_LENGTH chars" in {
        val (secret, _) = parseSecret(mode = Mode.Prod)("play.http.secret.key" -> "x" * (SecretConfiguration.SHORTEST_SECRET_LENGTH - 1))
        secret must beNone
      }

      "log a warning when value length is smaller than SHORT_SECRET_LENGTH chars" in {
        val (secret, events) = parseSecret(mode = Mode.Prod)("play.http.secret.key" -> "x" * (SecretConfiguration.SHORT_SECRET_LENGTH - 1))
        events.map(_.getFormattedMessage).find(_.contains("Your secret key is very short, and may be vulnerable to dictionary attacks.  Your application may not be secure")) must beSome
        secret must beSome
      }

      "return the value without warnings when it is configured respecting the requirements" in {
        val (secret, events) = parseSecret(mode = Mode.Prod)("play.http.secret.key" -> "12345678901234567890")
        events.map(_.getFormattedMessage).find(_.contains("Your secret key is very short, and may be vulnerable to dictionary attacks.  Your application may not be secure")) must beNone
        secret must beSome("12345678901234567890")
      }
    }
  }
}
