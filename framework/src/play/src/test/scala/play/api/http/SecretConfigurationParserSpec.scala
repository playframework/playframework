/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

import org.specs2.mutable.Specification
import play.api.{ Configuration, Environment, Mode, PlayException }

class ActualKeySecretConfigurationParserSpec extends SecretConfigurationParserSpec {
  override def secretKey: String = "play.http.secret.key"
}

class DeprecatedKeySecretConfigurationParserSpec extends SecretConfigurationParserSpec {
  override def secretKey: String = "play.crypto.secret"

  override def parseSecret(mode: Mode, secret: Option[String] = None) = {
    HttpConfiguration.fromConfiguration(
      Configuration.reference ++ Configuration.from(
        secret.map(secretKey -> _).toMap ++ Map(
          "play.http.secret.key" -> null
        )
      ),
      Environment.simple(mode = mode)
    ).secret.secret
  }

}

trait SecretConfigurationParserSpec extends Specification {

  def secretKey: String

  val Secret = "abcdefghijklmnopqrs"

  def parseSecret(mode: Mode, secret: Option[String] = None): String = {
    HttpConfiguration.fromConfiguration(
      Configuration.reference ++ Configuration.from(
        secret.map(secretKey -> _).toMap
      ),
      Environment.simple(mode = mode)
    ).secret.secret
  }

  "Secret config parser" should {
    "parse the secret" in {

      "load a configured secret in prod" in {
        parseSecret(Mode.Prod, Some(Secret)) must_== Secret
      }
      "load a configured secret in dev" in {
        parseSecret(Mode.Dev, Some(Secret)) must_== Secret
      }
      "throw an exception if secret is too short in prod" in {
        parseSecret(Mode.Prod, Some("12345678")) must throwA[PlayException]
      }
      "throw an exception if secret is changeme in prod" in {
        parseSecret(Mode.Prod, Some("changeme")) must throwA[PlayException]
      }
      "throw an exception if no secret in prod" in {
        parseSecret(Mode.Prod, Some(null)) must throwA[PlayException]
      }
      "throw an exception if secret is blank in prod" in {
        parseSecret(Mode.Prod, Some("  ")) must throwA[PlayException]
      }
      "throw an exception if secret is empty in prod" in {
        parseSecret(Mode.Prod, Some("")) must throwA[PlayException]
      }
      "generate a secret if secret is changeme in dev" in {
        parseSecret(Mode.Dev, Some("changeme")) must_!= "changeme"
      }
      "generate a secret if no secret in dev" in {
        parseSecret(Mode.Dev) must_!= ""
      }
      "generate a secret if secret is blank in dev" in {
        parseSecret(Mode.Dev, Some("  ")) must_!= "  "
      }
      "generate a secret if secret is empty in dev" in {
        parseSecret(Mode.Dev, Some("")) must_!= ""
      }
      "generate a stable secret in dev" in {
        parseSecret(Mode.Dev, Some("changeme")) must_!= "changeme"
      }
    }
  }
}

