/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.crypto

import org.specs2.mutable.Specification
import play.api.{ PlayException, Configuration, Environment, Mode }

/**
 *
 */
class CryptoConfigParserSpec extends Specification {

  "Crypto config parser" should {
    "parse the secret" in {
      val Secret = "abcdefghijklmnopqrs"

      def parseSecret(mode: Mode.Mode, secret: Option[String] = None) = {
        new CryptoConfigParser(Environment.simple(mode = mode),
          Configuration.reference ++ Configuration.from(
            secret.map("play.crypto.secret" -> _).toMap +
              ("play.crypto.aes.transformation" -> "AES")
          )).get.secret
      }

      "load a configured secret in prod" in {
        parseSecret(Mode.Prod, Some(Secret)) must_== Secret
      }
      "load a configured secret in dev" in {
        parseSecret(Mode.Dev, Some(Secret)) must_== Secret
      }
      "throw an exception if secret is changeme in prod" in {
        parseSecret(Mode.Prod, Some("changeme")) must throwA[PlayException]
      }
      "throw an exception if no secret in prod" in {
        parseSecret(Mode.Prod) must throwA[PlayException]
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
        parseSecret(Mode.Dev, Some("changeme")) must_== parseSecret(Mode.Dev)
      }
    }
  }
}
