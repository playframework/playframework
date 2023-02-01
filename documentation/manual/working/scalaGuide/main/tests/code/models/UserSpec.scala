/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.tests
// #basic-spec
package models

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UserSpec extends Specification {
  "User" should {
    "have a name" in {
      val user = User(id = "user-id", name = "Player", email = "user@email.com")
      user.name must beEqualTo("Player")
    }
  }
}
// #basic-spec

class AnotherSpec extends Specification {
  "Some example" in {
    // #assertion-example
    "Hello world" must endWith("world")
    // #assertion-example
  }
}
