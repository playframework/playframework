<<<<<<< HEAD:documentation/manual/scalaGuide/main/tests/code/ExamplePlaySpecificationSpec.scala
/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests
=======
package scalaguide.tests.specs
>>>>>>> Organized code samples for test into scalatest and specs subdirectories of test/code. Adjusted links on page such to match and verified it with the validate-docs target.:documentation/manual/scalaGuide/main/tests/code/specs2/ExamplePlaySpecificationSpec.scala

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
