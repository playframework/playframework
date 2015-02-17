/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests.specs2

import play.api.test._

// #scalafunctionaltest-playspecification
object ExamplePlaySpecificationSpec extends PlaySpecification {
  "The specification" should {

    "have access to HeaderNames" in {
      USER_AGENT must be_===("User-Agent")
    }

    "have access to Status" in {
      OK must be_===(200)
    }
  }
}
// #scalafunctionaltest-playspecification
