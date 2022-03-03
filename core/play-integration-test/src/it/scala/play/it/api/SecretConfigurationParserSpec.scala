/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.api

import ch.qos.logback.classic.spi.ILoggingEvent
import play.api.http.SecretConfiguration
import play.api.Environment
import play.api.Mode
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
      case scala.util.Failure(_)                => (None, Seq.empty)
    }
  }

  "When parsing SecretConfiguration" should {
    "in DEV mode" should {
      "generate a secret when value is configured to 'changeme'" in {
        val (secret, events) = parseSecret(mode = Mode.Dev)("play.http.secret.key" -> "changeme")
        events.map(_.getFormattedMessage).find(_.contains("Generated dev mode secret")) must beSome
        secret must beSome
        secret must not(beSome("changeme"))
      }

      "generate a secret when no value is configured" in {
        val (secret, events) = parseSecret(mode = Mode.Dev)("play.http.secret.key" -> null)
        events.map(_.getFormattedMessage).find(_.contains("Generated dev mode secret")) must beSome
        secret must beSome
        secret must not(beSome("changeme"))
      }

      "fail when value length is too small than the required bit length by used algorithm" in {
        val (secret, events) = parseSecret(mode = Mode.Dev)("play.http.secret.key" -> ("x" * 31))
        secret must beNone
        events must beEmpty
      }

      "return the value when it is configured respecting the requirements" in {
        val (secret, _) = parseSecret(mode = Mode.Dev)("play.http.secret.key" -> ("x" * 32))
        secret must beSome("x" * 32)
      }
    }

    "in PROD mode" should {
      "fail when value is configured to 'changeme'" in {
        val (secret, events) = parseSecret(mode = Mode.Prod)("play.http.secret.key" -> "changeme")
        secret must beNone
        events must beEmpty
      }

      "fail when value is not configured" in {
        val (secret, events) = parseSecret(mode = Mode.Prod)("play.http.secret.key" -> null)
        secret must beNone
        events must beEmpty
      }

      "fail when value length is too small than the required bit length by used algorithm" in {
        val (secret, events) = parseSecret(mode = Mode.Prod)("play.http.secret.key" -> ("x" * 31))
        secret must beNone
        events must beEmpty
      }

      "return the value when it is configured respecting the requirements" in {
        val (secret, _) = parseSecret(mode = Mode.Prod)("play.http.secret.key" -> ("x" * 32))
        secret must beSome("x" * 32)
      }
    }
  }
}
