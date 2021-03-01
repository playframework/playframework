/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

import org.specs2.mutable.Specification
import play.api.Configuration
import play.api.Environment
import play.api.Mode
import play.api.PlayException

class SecretConfigurationParserSpec extends Specification {
  def secretKey: String            = "play.http.secret.key"
  def flashCookieAlgorithm: String = "play.http.flash.jwt.signatureAlgorithm"

  val Secret32Bytes = "abcdefghijklmnopqrstuvwxyz123456" // => 256 bits, required for HS256 (the default algorithm)
  val Secret31Bytes = "abcdefghijklmnopqrstuvwxyz12345"  // => 248 bits, too short for HS256

  def parseSecret(mode: Mode, secret: Option[String] = None, flashJWTAlgorithm: Option[String] = None): String = {
    HttpConfiguration
      .fromConfiguration(
        Configuration
          .from(secret.map(secretKey -> _).toMap ++ flashJWTAlgorithm.map(flashCookieAlgorithm -> _).toMap)
          .withFallback(Configuration.reference),
        Environment.simple(mode = mode)
      )
      .secret
      .secret
  }

  "Secret config parser" should {
    "parse the secret" in {
      "load a configured secret in prod" in {
        parseSecret(Mode.Prod, Some(Secret32Bytes)) must_== Secret32Bytes
      }
      "load a configured secret in dev" in {
        parseSecret(Mode.Dev, Some(Secret32Bytes)) must_== Secret32Bytes
      }
      "throw an exception if secret is too short in prod" in {
        parseSecret(Mode.Prod, Some(Secret31Bytes)) must throwA[PlayException].like {
          case e =>
            e.getMessage must beEqualTo(
              """Configuration error[
                |The application secret is too short and does not have the recommended amount of entropy for algorithm HS256 defined at play.http.session.jwt.signatureAlgorithm.
                |Current application secret bits: 248, minimal required bits for algorithm HS256: 256.
                |To set the application secret, please read https://playframework.com/documentation/latest/ApplicationSecret
                |]""".stripMargin
            )
        }
      }
      "throw an exception if secret is too short in dev" in {
        parseSecret(Mode.Dev, Some(Secret31Bytes)) must throwA[PlayException].like {
          case e =>
            e.getMessage must beEqualTo(
              """Configuration error[
                |The application secret is too short and does not have the recommended amount of entropy for algorithm HS256 defined at play.http.session.jwt.signatureAlgorithm.
                |Current application secret bits: 248, minimal required bits for algorithm HS256: 256.
                |To set the application secret, please read https://playframework.com/documentation/latest/ApplicationSecret
                |]""".stripMargin
            )
        }
      }
      "throw an exception if secret is ok for session cookie but too short for flash cookie in prod" in {
        parseSecret(Mode.Prod, Some(Secret32Bytes), flashJWTAlgorithm = Some("HS512")) must throwA[PlayException].like {
          case e =>
            e.getMessage must beEqualTo(
              """Configuration error[
                |The application secret is too short and does not have the recommended amount of entropy for algorithm HS512 defined at play.http.flash.jwt.signatureAlgorithm.
                |Current application secret bits: 256, minimal required bits for algorithm HS512: 512.
                |To set the application secret, please read https://playframework.com/documentation/latest/ApplicationSecret
                |]""".stripMargin
            )
        }
      }
      "throw an exception if secret is ok for session cookie but too short for flash cookie in dev" in {
        parseSecret(Mode.Dev, Some(Secret32Bytes), flashJWTAlgorithm = Some("HS512")) must throwA[PlayException].like {
          case e =>
            e.getMessage must beEqualTo(
              """Configuration error[
                |The application secret is too short and does not have the recommended amount of entropy for algorithm HS512 defined at play.http.flash.jwt.signatureAlgorithm.
                |Current application secret bits: 256, minimal required bits for algorithm HS512: 512.
                |To set the application secret, please read https://playframework.com/documentation/latest/ApplicationSecret
                |]""".stripMargin
            )
        }
      }
      "throw an exception if secret is changeme in prod" in {
        parseSecret(Mode.Prod, Some("changeme")) must throwA[PlayException].like {
          case e =>
            e.getMessage must beEqualTo(
              """Configuration error[
                |The application secret has not been set, and we are in prod mode. Your application is not secure.
                |To set the application secret, please read https://playframework.com/documentation/latest/ApplicationSecret
                |]""".stripMargin
            )
        }
      }
      "throw an exception if no secret in prod" in {
        parseSecret(Mode.Prod, Some(null)) must throwA[PlayException].like {
          case e =>
            e.getMessage must beEqualTo(
              """Configuration error[
                |The application secret has not been set, and we are in prod mode. Your application is not secure.
                |To set the application secret, please read https://playframework.com/documentation/latest/ApplicationSecret
                |]""".stripMargin
            )
        }
      }
      "throw an exception if secret is blank in prod" in {
        parseSecret(Mode.Prod, Some("  ")) must throwA[PlayException].like {
          case e =>
            e.getMessage must beEqualTo(
              """Configuration error[
                |The application secret has not been set, and we are in prod mode. Your application is not secure.
                |To set the application secret, please read https://playframework.com/documentation/latest/ApplicationSecret
                |]""".stripMargin
            )
        }
      }
      "throw an exception if secret is empty in prod" in {
        parseSecret(Mode.Prod, Some("")) must throwA[PlayException].like {
          case e =>
            e.getMessage must beEqualTo(
              """Configuration error[
                |The application secret has not been set, and we are in prod mode. Your application is not secure.
                |To set the application secret, please read https://playframework.com/documentation/latest/ApplicationSecret
                |]""".stripMargin
            )
        }
      }
      "generate a secret if secret is changeme in dev" in {
        parseSecret(Mode.Dev, Some("changeme")) must_!= "changeme"
        parseSecret(Mode.Dev, Some("changeme")) must_!= "a test secret which is more than 32 bytes"
      }
      "generate a secret if no secret in dev" in {
        parseSecret(Mode.Dev, Some(null)) must_!= ""
        parseSecret(Mode.Dev, Some(null)) must_!= "a test secret which is more than 32 bytes"
      }
      "generate a secret if secret is blank in dev" in {
        parseSecret(Mode.Dev, Some("  ")) must_!= "  "
        parseSecret(Mode.Dev, Some("  ")) must_!= "a test secret which is more than 32 bytes"
      }
      "generate a secret if secret is empty in dev" in {
        parseSecret(Mode.Dev, Some("")) must_!= ""
        parseSecret(Mode.Dev, Some("")) must_!= "a test secret which is more than 32 bytes"
      }
      "generate a stable secret in dev" in {
        parseSecret(Mode.Dev, Some("changeme")) must_!= "changeme"
        parseSecret(Mode.Dev, Some("changeme")) must_!= "a test secret which is more than 32 bytes"
      }
    }
  }
}
