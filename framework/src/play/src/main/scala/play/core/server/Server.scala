package play.core.server

import play.api._
import play.core._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.akka._
import akka.actor._
import akka.actor.Actor._
import akka.routing.Routing._
import akka.routing.CyclicIterator
import akka.config._
import akka.config.Supervision._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._
import play.api.libs.concurrent._

trait WebSocketable {
  def getHeader(header: String): String
  def check: Boolean
}

trait Server {

  // First delete the default log file for a fresh start
  try {
    scalax.file.Path(new java.io.File(applicationProvider.path, "logs/application.log")).delete()
  } catch {
    case _ =>
  }

  // Configure the logger for the first time
  Logger.configure(
    Map("application.home" -> applicationProvider.path.getAbsolutePath))

  def response(webSocketableRequest: WebSocketable)(otheResults: PartialFunction[Result, Unit]) = new Response {

    val websocketErrorResult: PartialFunction[Result, Unit] = { case _ if (webSocketableRequest.check) => handle(Results.BadRequest) }

    val asyncResult: PartialFunction[Result, Unit] = {
      case AsyncResult(p) => p.extend1 {
        case Redeemed(v) => handle(v)
        case Thrown(e) => {
          Logger("play").error("Waiting for a promise, but got an error: " + e.getMessage, e)
          handle(Results.InternalServerError)
        }
      }
    }

    def handle(result: Result) = (asyncResult orElse websocketErrorResult orElse otheResults)(result)
  }

  def newInvoker = { val inv = actorOf[Invoker]; inv.start(); inv }

  val invoker = loadBalancerActor(new CyclicIterator(List.fill(3)(newInvoker))).start()

  def getHandlerFor(request: RequestHeader): Either[Result, (Handler, Application)] = {

    import scala.util.control.Exception

    def sendHandler: Either[Throwable, (Handler, Application)] = {
      try {
        applicationProvider.get.right.map { application =>
          val maybeAction = application.global.onRouteRequest(request)
          (maybeAction.getOrElse(Action(BodyParsers.parse.empty)(_ => application.global.onHandlerNotFound(request))), application)
        }
      } catch {
        case e => Left(e)
      }
    }

    def logExceptionAndGetResult(e: Throwable) = {

      Logger.error(
        """
        |
        |! %sInternal server error, for request [%s] ->
        |""".stripMargin.format(e match {
          case p: PlayException => "@" + p.id + " - "
          case _ => ""
        }, request),
        e)

      DefaultGlobal.onError(request, e)

    }

    Exception
      .allCatch[Option[Result]]
      .either(applicationProvider.handleWebCommand(request))
      .left.map(logExceptionAndGetResult)
      .right.flatMap(maybeResult => maybeResult.toLeft(())).right.flatMap { _ =>
        sendHandler.left.map(logExceptionAndGetResult)
      }

  }

  def invoke[A](request: Request[A], response: Response, action: Action[A], app: Application) = invoker ! HandleAction(request, response, action, app)

  import play.api.libs.concurrent._
  def getBodyParser[A](requestHeaders: RequestHeader, bodyFunction: BodyParser[A]): Promise[Iteratee[Array[Byte], Either[Result, A]]] = {
    (invoker ? (requestHeaders, bodyFunction)).asPromise.map(_.asInstanceOf[Iteratee[Array[Byte], Either[Result, A]]])
  }

  def applicationProvider: ApplicationProvider

}
