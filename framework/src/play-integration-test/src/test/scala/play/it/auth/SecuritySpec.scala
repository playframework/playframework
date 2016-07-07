/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.auth

import play.api.Application
import play.api.mvc.Security.{ AuthenticatedBuilder, AuthenticatedRequest }
import play.api.mvc._
import play.api.test._

import scala.concurrent.Future

class SecuritySpec extends PlaySpecification {

  "AuthenticatedBuilder" should {
    "block unauthenticated requests" in withApplication { implicit app =>
      status(TestAction(app) { req =>
        Results.Ok(req.user)
      }(FakeRequest())) must_== UNAUTHORIZED
    }
    "allow authenticated requests" in withApplication { implicit app =>
      val result = TestAction(app) { req =>
        Results.Ok(req.user)
      }(FakeRequest().withSession("username" -> "john"))
      status(result) must_== OK
      contentAsString(result) must_== "john"
    }

    "allow use as an ActionBuilder" in withApplication { implicit app =>
      val result = Authenticated(app) { req =>
        Results.Ok(s"${req.conn.name}:${req.user.name}")
      }(FakeRequest().withSession("user" -> "Phil"))
      status(result) must_== OK
      contentAsString(result) must_== "fake:Phil"
    }
  }

  def TestAction(implicit app: Application) =
    AuthenticatedBuilder(app.injector.instanceOf[BodyParsers.Default])(app.materializer.executionContext)

  case class User(name: String)
  def getUserFromRequest(req: RequestHeader) = req.session.get("user") map (User(_))

  class AuthenticatedDbRequest[A](val user: User, val conn: Connection, request: Request[A]) extends WrappedRequest[A](request)

  def Authenticated(implicit app: Application) = new ActionBuilder[AuthenticatedDbRequest, AnyContent] {
    lazy val executionContext = app.materializer.executionContext
    lazy val parser = app.injector.instanceOf[PlayBodyParsers].default
    def invokeBlock[A](request: Request[A], block: (AuthenticatedDbRequest[A]) => Future[Result]) = {
      val builder = AuthenticatedBuilder(req => getUserFromRequest(req), app.injector.instanceOf[BodyParsers.Default])(app.materializer.executionContext)
      builder.authenticate(request, { authRequest: AuthenticatedRequest[A, User] =>
        fakedb.withConnection { conn =>
          block(new AuthenticatedDbRequest[A](authRequest.user, conn, request))
        }
      })
    }
  }

  object fakedb {
    def withConnection[A](block: Connection => A) = {
      block(FakeConnection)
    }
  }
  object FakeConnection extends Connection("fake")
  case class Connection(name: String)

  def withApplication[T](block: Application => T) = {
    running(_.configure("play.crypto.secret" -> "foobar"))(block)
  }
}
