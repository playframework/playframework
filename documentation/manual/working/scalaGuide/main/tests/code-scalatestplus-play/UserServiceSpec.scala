/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests.scalatest

import org.scalatest._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play._

import org.mockito.Mockito._
import org.mockito.Matchers._

import scalaguide.tests.models._
import scalaguide.tests.services._

// #scalatest-userservicespec
class UserServiceSpec extends PlaySpec with MockitoSugar {

  "UserService#isAdmin" should {
    "be true when the role is admin" in {
      val userRepository = mock[UserRepository]
      when(userRepository.roles(any[User])) thenReturn Set(Role("ADMIN"))

      val userService = new UserService(userRepository)

      val actual = userService.isAdmin(User("11", "Steve", "user@example.org"))
      actual mustBe true
    }
  }
}
// #scalatest-userservicespec
