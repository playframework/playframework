/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.libs

import play.api.test._
import play.api.libs.Crypto
import play.api.{PlayException, Mode}

object CryptoSpec extends PlaySpecification {

  val Secret = "abcdefghijklmnopqrs"

  def fakeApp(m: Mode.Mode, secret: Option[String] = None) = {
    new FakeApplication(
      additionalConfiguration = secret.map("application.secret" -> _).toMap
    ) {
      override val mode = m
    }
  }

  "Crypto" should {
    "secret" should {
      "load a configured secret in prod" in new WithApplication(fakeApp(Mode.Prod, Some(Secret))) {
        Crypto.secret must_== Secret
      }
      "load a configured secret in dev" in new WithApplication(fakeApp(Mode.Dev, Some(Secret))) {
        Crypto.secret must_== Secret
      }
      "throw an exception if secret is changeme in prod" in new WithApplication(fakeApp(Mode.Prod, Some("changeme"))) {
        Crypto.secret must throwA[PlayException]
      }
      "throw an exception if no secret in prod" in new WithApplication(fakeApp(Mode.Prod)) {
        Crypto.secret must throwA[PlayException]
      }
      "throw an exception if secret is blank in prod" in new WithApplication(fakeApp(Mode.Prod, Some("  "))) {
        Crypto.secret must throwA[PlayException]
      }
      "throw an exception if secret is empty in prod" in new WithApplication(fakeApp(Mode.Prod, Some(""))) {
        Crypto.secret must throwA[PlayException]
      }
      "generate a secret if secret is changeme in dev" in new WithApplication(fakeApp(Mode.Dev, Some("changeme"))) {
        Crypto.secret must_!= "changeme"
      }
      "generate a secret if no secret in dev" in new WithApplication(fakeApp(Mode.Dev)) {
        Crypto.secret must_!= ""
      }
      "generate a secret if secret is blank in dev" in new WithApplication(fakeApp(Mode.Dev, Some("  "))) {
        Crypto.secret must_!= "  "
      }
      "generate a secret if secret is empty in dev" in new WithApplication(fakeApp(Mode.Dev, Some(""))) {
        Crypto.secret must_!= ""
      }
    }
  }

}
