/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests.scalatest.oneserverpersuite

import play.api.test._
import org.scalatest._
import org.scalatestplus.play._
import play.api.{Play, Application}

// #scalafunctionaltest-oneserverpersuite
class ExampleSpec extends PlaySpec with OneServerPerSuite {

  // Override app if you need a FakeApplication with other than
  // non-default parameters.
  implicit override lazy val app: FakeApplication =
    FakeApplication(
      additionalConfiguration = Map("ehcacheplugin" -> "disabled")
    )

  "The OneServerPerSuite trait" must {
    "provide a FakeApplication" in {
      app.configuration.getString("ehcacheplugin") mustBe Some("disabled")
    }
    "start the FakeApplication" in {
      Play.maybeApplication mustBe Some(app)
    }
  }
}
// #scalafunctionaltest-oneserverpersuite
