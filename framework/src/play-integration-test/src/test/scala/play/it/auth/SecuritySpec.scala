package play.it.auth

import play.api.test._
import play.api.mvc.Security.AuthenticatedBuilder
import play.api.mvc.Results

object SecuritySpec extends PlaySpecification {

  "AuthenticatedBuilder" should {
    "block unauthenticated requests" in withApplication {
      status(TestAction { req =>
        Results.Ok(req.user)
      }(FakeRequest())) must_== UNAUTHORIZED
    }
    "allow authenticated requests" in withApplication {
      val result = TestAction { req =>
        Results.Ok(req.user)
      }(FakeRequest().withSession("username" -> "john"))
      status(result) must_== OK
      contentAsString(result) must_== "john"
    }

  }

  val TestAction = AuthenticatedBuilder()

  def withApplication[T](block: => T) =
    running(FakeApplication(additionalConfiguration = Map("application.secret" -> "foobar")))(block)
}
