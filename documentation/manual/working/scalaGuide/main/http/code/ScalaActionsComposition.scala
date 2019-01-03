/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.http.scalaactionscomposition {

import javax.inject.Inject

import akka.actor._
import akka.stream.ActorMaterializer
import play.api.test._
import play.api.test.Helpers._
import play.api.mvc._
import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.Logger
import play.api.Logging

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import org.specs2.execute.AsResult

case class User(name: String)
object User {
  def find(u: String) = Some(User("player"))
}

@RunWith(classOf[JUnitRunner])
class ScalaActionsCompositionSpec extends Specification with ControllerHelpers {

  "an action composition" should {

    implicit val system = ActorSystem()
    implicit val mat = ActorMaterializer()
    implicit val ec: ExecutionContext = system.dispatcher
    val parse = PlayBodyParsers()
    val defaultParser = new BodyParsers.Default(parse)
    val actionBuilder = new DefaultActionBuilderImpl(new BodyParsers.Default(parse))

    "Basic action composition" in {
      //#basic-logging
      import play.api.mvc._

      class LoggingAction @Inject() (parser: BodyParsers.Default)(implicit ec: ExecutionContext)
          extends ActionBuilderImpl(parser) with Logging {
        override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
          logger.info("Calling action")
          block(request)
        }
      }
      //#basic-logging

      val loggingAction = new LoggingAction(defaultParser)

      //#basic-logging-index
      class MyController @Inject()(loggingAction: LoggingAction,
                                   cc:ControllerComponents)
        extends AbstractController(cc) {
        def index = loggingAction {
          Ok("Hello World")
        }
      }
      //#basic-logging-index

      testAction(new MyController(loggingAction, Helpers.stubControllerComponents()).index)

      //#basic-logging-parse
      def submit = loggingAction(parse.text) { request =>
        Ok("Got a body " + request.body.length + " bytes long")
      }
      //#basic-logging-parse

      val request = FakeRequest().withTextBody("hello with the parse")
      testAction(new MyController(loggingAction, Helpers.stubControllerComponents()).index, request)
    }

    "Wrapping existing actions" in {

      //#actions-class-wrapping
      import play.api.mvc._

      case class Logging[A](action: Action[A]) extends Action[A] with play.api.Logging {

        def apply(request: Request[A]): Future[Result] = {
          logger.info("Calling action")
          action(request)
        }

        override def parser = action.parser
        override def executionContext = action.executionContext
      }
      //#actions-class-wrapping

      //#actions-wrapping-builder
      class LoggingAction @Inject() (parser: BodyParsers.Default)(implicit ec: ExecutionContext) extends ActionBuilderImpl(parser) {
        override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
          block(request)
        }
        override def composeAction[A](action: Action[A]) = new Logging(action)
      }
      //#actions-wrapping-builder

      val loggingAction = new LoggingAction(defaultParser)

      {
        //#actions-wrapping-index
        def index = loggingAction {
          Ok("Hello World")
        }
        //#actions-wrapping-index

        testAction(index)
      }

      {
        val Action = actionBuilder
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

      //###skip:2
      val logger = Logger(getClass)
      val Action = actionBuilder
      def logging[A](action: Action[A]) = Action.async(action.parser) { request =>
        logger.info("Calling action")
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
      import play.api.mvc.request.RemoteConnection

      //###skip:1
      val Action = actionBuilder
      def xForwardedFor[A](action: Action[A]) = Action.async(action.parser) { request =>
        val newRequest = request.headers.get("X-Forwarded-For") match {
          case None => request
          case Some(xff) =>
            val xffConnection = RemoteConnection(xff, request.connection.secure, None)
            request.withConnection(xffConnection)
        }
        action(newRequest)
      }
      //#modify-request

      testAction(xForwardedFor(Action(Ok)))
    }

    "allow blocking the request" in {
      //#block-request
      import play.api.mvc._
      //###insert: import play.api.mvc.Results._

      //###skip:1
      val Action = actionBuilder
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
      //#modify-result
      import play.api.mvc._

      //###skip:1
      val Action = actionBuilder
      def addUaHeader[A](action: Action[A]) = Action.async(action.parser) { request =>
        action(request).map(_.withHeaders("X-UA-Compatible" -> "Chrome=1"))
      }
      //#modify-result

      assertAction(addUaHeader(Action(Ok))) { result =>
        header("X-UA-Compatible", result) must beSome("Chrome=1")
      }
    }

    "allow action builders with different request types" in {

      //#authenticated-action-builder
      import play.api.mvc._

      class UserRequest[A](val username: Option[String], request: Request[A]) extends WrappedRequest[A](request)

      class UserAction @Inject()(val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext)
        extends ActionBuilder[UserRequest, AnyContent] with ActionTransformer[Request, UserRequest] {
        def transform[A](request: Request[A]) = Future.successful {
          new UserRequest(request.session.get("username"), request)
        }
      }
      //#authenticated-action-builder
      val userAction = new UserAction(defaultParser)

      def currentUser = userAction { request =>
        Ok("The current user is " + request.username.getOrElse("anonymous"))
      }

      testAction(currentUser)

      case class Item(id: String) {
        def addTag(tag: String) = ()
        def accessibleByUser(user: Option[String]) = user.isDefined
      }
      object ItemDao {
        def findById(id: String) = Some(Item(id))
      }

      //#request-with-item
      import play.api.mvc._

      class ItemRequest[A](val item: Item, request: UserRequest[A]) extends WrappedRequest[A](request) {
        def username = request.username
      }
      //#request-with-item

      //#item-action-builder
      def ItemAction(itemId: String)(implicit ec: ExecutionContext) = new ActionRefiner[UserRequest, ItemRequest] {
        def executionContext = ec
        def refine[A](input: UserRequest[A]) = Future.successful {
          ItemDao.findById(itemId)
            .map(new ItemRequest(_, input))
            .toRight(NotFound)
        }
      }
      //#item-action-builder

      //#permission-check-action
      def PermissionCheckAction(implicit ec: ExecutionContext) = new ActionFilter[ItemRequest] {
        def executionContext = ec
        def filter[A](input: ItemRequest[A]) = Future.successful {
          if (!input.item.accessibleByUser(input.username))
            Some(Forbidden)
          else
            None
        }
      }
      //#permission-check-action

      //#item-action-use
      def tagItem(itemId: String, tag: String)(implicit ec: ExecutionContext) =
        (userAction andThen ItemAction(itemId) andThen PermissionCheckAction) { request =>
          request.item.addTag(tag)
          Ok("User " + request.username + " tagged " + request.item.id)
        }
      //#item-action-use

      testAction(tagItem("foo", "bar"), expectedResponse = FORBIDDEN)
    }

  }

  import play.api.mvc._
  def testAction[A](action: EssentialAction, request: => Request[A] = FakeRequest(), expectedResponse: Int = OK) = {
    assertAction(action, request, expectedResponse) { result => success }
  }

  def assertAction[A, T: AsResult](action: EssentialAction, request: => Request[A] = FakeRequest(), expectedResponse: Int = OK)(assertions: Future[Result] => T) = {
    running() { app =>
      implicit val mat = ActorMaterializer()(app.actorSystem)
      val result = action(request).run()
      status(result) must_== expectedResponse
      assertions(result)
    }
  }

}

}
