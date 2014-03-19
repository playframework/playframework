<<<<<<< HEAD:documentation/manual/scalaGuide/main/tests/code/WithDbDataSpec.scala
/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests
=======
package scalaguide.tests.specs
>>>>>>> Organized code samples for test into scalatest and specs subdirectories of test/code. Adjusted links on page such to match and verified it with the validate-docs target.:documentation/manual/scalaGuide/main/tests/code/specs2/WithDbDataSpec.scala

import play.api.test._
import play.api.test.Helpers._

import org.specs2.execute.{Result, AsResult}

/**
 *
 */
class WithDbDataSpec extends PlaySpecification {

  // #scalafunctionaltest-withdbdata
  abstract class WithDbData extends WithApplication {
    override def around[T: AsResult](t: => T): Result = super.around {
      setupData()
      t
    }

    def setupData() {
      // setup data
    }
  }

  "Computer model" should {

    "be retrieved by id" in new WithDbData {
      // your test code
    }
    "be retrieved by email" in new WithDbData {
      // your test code
    }
  }
  // #scalafunctionaltest-withdbdata
}
