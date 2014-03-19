<<<<<<< HEAD:documentation/manual/scalaGuide/main/tests/code/UserServiceSpec.scala
/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests
=======
package scalaguide.tests.specs
>>>>>>> Organized code samples for test into scalatest and specs subdirectories of test/code. Adjusted links on page such to match and verified it with the validate-docs target.:documentation/manual/scalaGuide/main/tests/code/specs2/UserServiceSpec.scala

import org.specs2.mock._
import org.specs2.mutable._

import scalaguide.tests.models._
import scalaguide.tests.services._

// #scalatest-userservicespec
object UserServiceSpec extends Specification with Mockito {

  "UserService#isAdmin" should {
    "be true when the role is admin" in {
      val userRepository = mock[UserRepository]
      userRepository.roles(any[User]) returns Set(Role("ADMIN"))

      val userService = new UserService(userRepository)
      val actual = userService.isAdmin(User("11", "Steve", "user@example.org"))
      actual must beTrue
    }
  }
}
// #scalatest-userservicespec
