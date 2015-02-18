/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests.scalatest.oneapppertest

import play.api.test._
import org.scalatest._
import org.scalatestplus.play._
import play.api.{Play, Application}

// #scalafunctionaltest-oneapppertest
class ExampleSpec extends PlaySpec with OneAppPerTest {

  // Override app if you need a FakeApplication with other than
  // default parameters.
  implicit override def newAppForTest(td: TestData): FakeApplication =
    FakeApplication(
      additionalConfiguration = Map("ehcacheplugin" -> "disabled")
    )

  "The OneAppPerTest trait" must {
    "provide a new FakeApplication for each test" in {
      app.configuration.getString("ehcacheplugin") mustBe Some("disabled")
    }
    "start the FakeApplication" in {
      Play.maybeApplication mustBe Some(app)
    }
  }
}
// #scalafunctionaltest-oneapppertest
