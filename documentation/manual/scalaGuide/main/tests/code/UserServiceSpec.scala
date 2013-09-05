package scalaguide.tests

import org.specs2.mock._
import org.specs2.mutable._

import models._
import services._

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
