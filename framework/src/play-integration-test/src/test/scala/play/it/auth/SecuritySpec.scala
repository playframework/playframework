package play.it.auth

import play.api.test._
import play.api.mvc.Security.{AuthenticatedRequest, AuthenticatedBuilder}
import play.api.mvc._
import scala.concurrent.Future
import play.api.test.FakeApplication

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

    "allow use as an ActionBuilder" in withApplication {
      val result = Authenticated { req =>
        Results.Ok(s"${req.conn.name}:${req.user.name}")
      }(FakeRequest().withSession("user" -> "Phil"))
      status(result) must_== OK
      contentAsString(result) must_== "fake:Phil"
    }
  }

  val TestAction = AuthenticatedBuilder()

  case class User(name: String)
  def getUserFromRequest(req: RequestHeader) = req.session.get("user") map (User(_))

  class AuthenticatedDbRequest[A](val user: User, val conn: Connection, request: Request[A]) extends WrappedRequest[A](request)

  object Authenticated extends ActionBuilder[AuthenticatedDbRequest] {
    def invokeBlock[A](request: Request[A], block: (AuthenticatedDbRequest[A]) => Future[SimpleResult]) = {
      AuthenticatedBuilder(req => getUserFromRequest(req)).authenticate(request, { authRequest: AuthenticatedRequest[A, User] =>
        DB.withConnection { conn =>
          block(new AuthenticatedDbRequest[A](authRequest.user, conn, request))
        }
      })
    }
  }

  object DB {
    def withConnection[A](block: Connection => A) = {
      block(FakeConnection)
    }
  }
  object FakeConnection extends Connection("fake")
  case class Connection(name: String)

  def withApplication[T](block: => T) =
    running(FakeApplication(additionalConfiguration = Map("application.secret" -> "foobar")))(block)
}
