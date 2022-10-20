/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.tests.specs2

import play.api.test._

// #scalafunctionaltest-playspecification
class ExamplePlaySpecificationSpec extends PlaySpecification {
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
