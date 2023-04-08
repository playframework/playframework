/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.tests.specs2

import org.specs2.execute.AsResult
import org.specs2.execute.Result
import play.api.test._

class WithDbDataSpec extends PlaySpecification {
  // #scalafunctionaltest-withdbdata
  abstract class WithDbData extends WithApplication {
    override def wrap[T: AsResult](t: => T): Result = super.wrap {
      setupData()
      t
    }

    def setupData(): Unit = {
      // setup data
    }
  }

  "Computer model" should {
    "be retrieved by id" in new WithDbData {
      override def running() = {
        // your test code
      }
    }
    "be retrieved by email" in new WithDbData {
      override def running() = {
        // your test code
      }
    }
  }
  // #scalafunctionaltest-withdbdata
}
