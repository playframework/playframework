import play.api.libs.iteratee.Done
import play.api.libs.iteratee.Iteratee

package scalaguide.http.scalaactionscomposition {

  import play.api.test._
  import play.api.test.Helpers._
  import org.specs2.mutable.Specification
  import org.junit.runner.RunWith
  import org.specs2.runner.JUnitRunner
  import play.api.Logger
  import play.api.mvc._
  import scala.concurrent.Future
  import org.specs2.execute.AsResult

  case class User(name: String)
  object User {
    def find(u: String) = Some(User("player"))
  }

  @RunWith(classOf[JUnitRunner])
  class ScalaActionsCompositionSpec extends Specification with Controller {

    "an action composition" should {

      "Basic action composition" in {
        //#basic-logging
        def LoggingAction(f: Request[AnyContent] => Result): Action[AnyContent] = {
          Action { request =>
            Logger.info("Calling action")
            f(request)
          }
        }
        //#basic-logging

        //#basic-logging-index
        def index = LoggingAction { request =>
          Ok("Hello World")
        }
        //#basic-logging-index

        testAction(index)
      }

      "Basic action composition with the parse" in {
        //#basic-logging-parse
        def LoggingAction[A](bp: BodyParser[A])(f: Request[A] => Result): Action[A] = {
          Action(bp) { request =>
            Logger.info("Calling action")
            f(request)
          }
        }
        //#basic-logging-parse

        //#basic-logging-parse-index
        def index = LoggingAction(parse.text) { request =>
          Ok("Hello World")
        }
        //#basic-logging-parse-index

        val request = FakeRequest().withTextBody("hello with the parse")
        testAction(index, request, BAD_REQUEST)
      }

      "Wrapping existing actions" in {

        import full.Logging

        //#actions-wrapping-index
        def index = Logging {
          Action {
            Ok("Hello World")
          }
        }
        //#actions-wrapping-index

        val request = FakeRequest().withTextBody("hello with the parse")
        testAction(index, request)
      }

      "Wrapping existing actions with parse" in {

        import full.Logging

        //#actions-wrapping-index-parse
        def index = Logging {
          Action(parse.text) { request =>
            Ok("Hello World")
          }
        }
        //#actions-wrapping-index-parse

        val request = FakeRequest().withTextBody("hello with the parse")
        testAction(index, request, BAD_REQUEST)
      }

      "Wrapping existing actions without defining the Logging class" in {

        //#actions-def-wrapping
        def Logging[A](action: Action[A]): Action[A] = {
          Action.async(action.parser) { request =>
            Logger.info("Calling action")
            action(request)
          }
        }
        //#actions-def-wrapping

        def index = Logging {
          Action {
            Ok("Hello World")
          }
        }
        val request = FakeRequest().withTextBody("hello with the parse")
        testAction(index, request)
      }

      "Wrapping existing actions addSessionVar" in {

        val index = full.outsideOfSpecs.addSessionVar {
          Action {
            Ok("Hello World")
          }
        }
        val request = FakeRequest().withTextBody("hello with the parse")
        testAction(index, request)
      }

      "Wrapping authenticated essentialaction" in {

        //#authenticated-essentialaction
        def Authenticated(action: User => EssentialAction): EssentialAction = {

          // Let's define a helper function to retrieve a User
          def getUser(request: RequestHeader): Option[User] = {
            request.session.get("user").flatMap(u => User.find(u))
          }

          // Now let's define the new Action
          EssentialAction { request =>
            getUser(request).map(u => action(u)(request)).getOrElse {
              Done(Unauthorized)
            }
          }

        }
        //#authenticated-essentialaction

        //#authenticated-essentialaction-index
        def index = Authenticated { user =>
          Action { request =>
            Ok("Hello " + user.name)
          }
        }
        //#authenticated-essentialaction-index
        def request = FakeRequest().withTextBody("hello with the parse").withSession("user" -> "player")
        testAction(index, request)
      }

      "Wrapping authenticated action param" in {

        //#authenticated-action-param
        def Authenticated(f: (User, Request[AnyContent]) => Result) = {
          Action { request =>
            val result = for {
              id <- request.session.get("user")
              user <- User.find(id)
            } yield f(user, request)
            result getOrElse Unauthorized
          }
        }
        //#authenticated-action-param

        //#authenticated-action-param-index
        def index = Authenticated { (user, request) =>
          Ok("Hello " + user.name)
        }
        //#authenticated-action-param-index
        def request = FakeRequest().withTextBody("hello with the parse").withSession("user" -> "player")
        testAction(index, request)
      }

      "Wrapping authenticated action currying" in {

        //#authenticated-action-currying
        def Authenticated(f: User => Request[AnyContent] => Result) = {
          Action { request =>
            val result = for {
              id <- request.session.get("user")
              user <- User.find(id)
            } yield f(user)(request)
            result getOrElse Unauthorized
          }
        }
        //#authenticated-action-currying

        //#authenticated-action-currying-index
        def index = Authenticated { user =>
          implicit request =>
            Ok("Hello " + user.name)
        }
        //#authenticated-action-currying-index
        def request = FakeRequest().withTextBody("hello with the parse").withSession("user" -> "player")
        testAction(index, request)
      }

      "Wrapping authenticated request" in {

        //#authenticated-request
        class AuthenticatedRequest(
          val user: User, request: Request[AnyContent]) extends WrappedRequest(request)

        def Authenticated(f: AuthenticatedRequest => Result) = {
          Action { request =>
            val result = for {
              id <- request.session.get("user")
              user <- User.find(id)
            } yield f(new AuthenticatedRequest(user, request))
            result getOrElse Unauthorized
          }
        }
        //#authenticated-request

        //#authenticated-request-index
        def index = Authenticated { implicit request =>
          Ok("Hello " + request.user.name)
        }
        //#authenticated-request-index
        def request = FakeRequest().withTextBody("hello with the parse").withSession("user" -> "player")
        testAction(index, request)
      }

      "generic Wrapping authenticated  with body parser" in {

        object Securit extends Controller {
          //#authenticated-request-parser
          case class AuthenticatedRequest[A](
            user: User, private val request: Request[A]) extends WrappedRequest(request)

          def Authenticated[A](p: BodyParser[A])(f: AuthenticatedRequest[A] => Result) = {
            Action(p) { request =>
              val result = for {
                id <- request.session.get("user")
                user <- User.find(id)
              } yield f(AuthenticatedRequest(user, request))
              result getOrElse Unauthorized
            }
          }

          // Overloaded method to use the default body parser
          import play.api.mvc.BodyParsers._
          def Authenticated(f: AuthenticatedRequest[AnyContent] => Result): Action[AnyContent] = {
            Authenticated(parse.anyContent)(f)
          }
          //#authenticated-request-parser
        }
        //#authenticated-request-index
        def index = Securit.Authenticated { implicit request =>
          Ok("Hello " + request.user.name)
        }
        //#authenticated-request-index
        def request = FakeRequest().withTextBody("hello with the parse").withSession("user" -> "player")
        testAction(index, request)
      }

    }

    def testAction[A](action: EssentialAction, request: => Request[A] = FakeRequest(), expectedResponse: Int = OK) = {
      assertAction(action, request, expectedResponse) { result => success }
    }

    def assertAction[A, T: AsResult](action: EssentialAction, request: => Request[A] = FakeRequest(), expectedResponse: Int = OK)(assertions: Future[SimpleResult] => T) = {
      running(FakeApplication(additionalConfiguration = Map("application.secret" -> "pass"))) {
        val result = action(request).run
        status(result) must_== expectedResponse
        assertions(result)
      }
    }

  }
}

package scalaguide.http.scalaactionscomposition.full {

  import play.api.Logger
  import play.api.mvc._
  import scala.concurrent.Future

//#actions-class-wrapping
  case class Logging[A](action: Action[A]) extends Action[A] {

    def apply(request: Request[A]): Future[SimpleResult] = {
      Logger.info("Calling action")
      action(request)
    }

    lazy val parser = action.parser
  }
  //#actions-class-wrapping

  object CodeShow {
    val ??? = null

    //#Source-Code-Action
    trait EssentialAction extends (RequestHeader => Iteratee[Array[Byte], SimpleResult])

    trait Action[A] extends EssentialAction {
      def parser: BodyParser[A]
      def apply(request: Request[A]): Future[SimpleResult]
      def apply(request: RequestHeader): Iteratee[Array[Byte], SimpleResult] = ???
    }
    //#Source-Code-Request

  }

  object outsideOfSpecs {
    //#actions-def-addSessionVar
    import play.api.libs.concurrent.Execution.Implicits._

    def addSessionVar[A](action: Action[A]) = Action.async(action.parser) { request =>
      action(request).map(_.withSession("foo" -> "bar"))
    }
    //#actions-def-addSessionVar
  }
}
 