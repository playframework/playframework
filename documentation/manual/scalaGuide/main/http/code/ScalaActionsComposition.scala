
package scalaguide.http.scalaactionscomposition {

  import play.api.test._
  import play.api.test.Helpers._
  import org.specs2.mutable.Specification
  import org.junit.runner.RunWith
  import org.specs2.runner.JUnitRunner
  import play.api.Logger
  import play.api.mvc.Controller
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
        import play.api.mvc._

        object LoggingAction extends ActionBuilder[Request] {
          def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[SimpleResult]) = {
            Logger.info("Calling action")
            block(request)
          }
        }
        //#basic-logging

        //#basic-logging-index
        def index = LoggingAction {
          Ok("Hello World")
        }
        //#basic-logging-index

        testAction(index)

        //#basic-logging-parse
        def submit = LoggingAction(parse.text) { request =>
          Ok("Got a body " + request.body.length + " bytes long")
        }
        //#basic-logging-parse

        val request = FakeRequest().withTextBody("hello with the parse")
        testAction(index, request)
      }

      "Wrapping existing actions" in {

        //#actions-class-wrapping
        import play.api.mvc._

        case class Logging[A](action: Action[A]) extends Action[A] {

          def apply(request: Request[A]): Future[SimpleResult] = {
            Logger.info("Calling action")
            action(request)
          }

          lazy val parser = action.parser
        }
        //#actions-class-wrapping

        //#actions-wrapping-builder
        object LoggingAction extends ActionBuilder[Request] {
          def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[SimpleResult]) = {
            block(request)
          }
          override def composeAction[A](action: Action[A]) = new Logging(action)
        }
        //#actions-wrapping-builder

        {
          //#actions-wrapping-index
          def index = LoggingAction {
            Ok("Hello World")
          }
          //#actions-wrapping-index

          testAction(index)
        }

        {
          //#actions-wrapping-direct
          def index = Logging {
            Action {
              Ok("Hello World")
            }
          }
          //#actions-wrapping-direct

          testAction(index)
        }
      }

      "Wrapping existing actions without defining the Logging class" in {

        //#actions-def-wrapping
        import play.api.mvc._

        def logging[A](action: Action[A])= Action.async(action.parser) { request =>
          Logger.info("Calling action")
          action(request)
        }
        //#actions-def-wrapping

        val request = FakeRequest().withTextBody("hello with the parse")
        testAction(logging {
          Action {
            Ok("Hello World")
          }
        }, request)
      }

      "allow modifying the request object" in {
        //#modify-request
        import play.api.mvc._

        def xForwardedFor[A](action: Action[A]) = Action.async(action.parser) { request =>
          val newRequest = request.headers.get("X-Forwarded-For").map { xff =>
            new WrappedRequest[A](request) {
              override def remoteAddress = xff
            }
          } getOrElse request
          action(newRequest)
        }
        //#modify-request

        testAction(xForwardedFor(Action(Ok)))
      }

      "allow blocking the request" in {
        //#block-request
        import play.api.mvc._

        def onlyHttps[A](action: Action[A]) = Action.async(action.parser) { request =>
          request.headers.get("X-Forwarded-Proto").collect {
            case "https" => action(request)
          } getOrElse {
            Future.successful(Forbidden("Only HTTPS requests allowed"))
          }
        }
        //#block-request

        testAction(action = onlyHttps(Action(Ok)), expectedResponse = FORBIDDEN)
      }

      "allow modifying the result" in {
        implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

        //#modify-result
        import play.api.mvc._
        import play.api.libs.concurrent.Execution.Implicits._

        def addUaHeader[A](action: Action[A]) = Action.async(action.parser) { request =>
          action(request).map(_.withHeaders("X-UA-Compatible" -> "Chrome=1"))
        }
        //#modify-result

        assertAction(addUaHeader(Action(Ok))) { result =>
          header("X-UA-Compatible", result) must beSome("Chrome=1")
        }
      }

      "allow action builders with different request types" in {
        case class Item(id: String) {
          def addTag(tag: String) = ()
        }
        object ItemDao {
          def findById(id: String) = Some(Item(id))
        }

        //#request-with-item
        import play.api.mvc._

        class RequestWithItem[A](val item: Item, request: Request[A]) extends WrappedRequest[A](request)
        //#request-with-item

        //#item-action-builder
        def ItemAction(itemId: String) = new ActionBuilder[RequestWithItem] {
          def invokeBlock[A](request: Request[A], block: (RequestWithItem[A]) => Future[SimpleResult]) = {
            ItemDao.findById(itemId).map { item =>
              block(new RequestWithItem(item, request))
            } getOrElse {
              Future.successful(NotFound)
            }
          }
        }
        //#item-action-builder

        //#item-action-use
        def tagItem(itemId: String, tag: String) = ItemAction(itemId) { request =>
          request.item.addTag(tag)
          Ok
        }
        //#item-action-use

        testAction(tagItem("foo", "bar"))
      }

      "allow writing an authentication action builder" in {

        //#authenticated-action-builder
        import play.api.mvc._

        class AuthenticatedRequest[A](val username: String, request: Request[A]) extends WrappedRequest[A](request)

        object Authenticated extends ActionBuilder[AuthenticatedRequest] {
          def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[SimpleResult]) = {
            request.session.get("username").map { username =>
              block(new AuthenticatedRequest(username, request))
            } getOrElse {
              Future.successful(Forbidden)
            }
          }
        }

        def currentUser = Authenticated { request =>
          Ok("The current user is " + request.username)
        }
        //#authenticated-action-builder

        testAction(action = currentUser, expectedResponse = FORBIDDEN)
      }
    }

    import play.api.mvc._
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

 