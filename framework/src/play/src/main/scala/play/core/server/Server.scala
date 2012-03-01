package play.core.server

import play.api._
import play.core._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._
import play.api.libs.concurrent._

import akka.actor._
import akka.actor.Actor._
import akka.routing._
import akka.config._
import akka.pattern.Patterns.ask
import akka.util.duration._
import akka.util.Timeout

trait WebSocketable {
  def getHeader(header: String): String
  def check: Boolean
}

/**
 * provides generic server behaviour for Play applications
 */
trait Server {

  // First delete the default log file for a fresh start
  try {
    scalax.file.Path(new java.io.File(applicationProvider.path, "logs/application.log")).delete()
  } catch {
    case _ =>
  }

  // Configure the logger for the first time
  Logger.configure(
    Map("application.home" -> applicationProvider.path.getAbsolutePath),
    mode = mode)

  // Start the main Invoker
  Invoker.init(applicationProvider)

  // Get a reference on the Action invoker
  val invoker = Invoker.actionInvoker

  val bodyParserTimeout = {
    Configuration(Invoker.system.settings.config).getMilliseconds("akka.actor.retrieveBodyParserTimeout").map(_ milliseconds).getOrElse(1 second)
  }

  def mode: Mode.Mode

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

  def invoke[A](request: Request[A], response: Response, action: Action[A], app: Application) = {
    invoker ! Invoker.HandleAction(request, response, action, app)
  }

  def getBodyParser[A](requestHeaders: RequestHeader, bodyFunction: BodyParser[A]): Promise[Iteratee[Array[Byte], Either[Result, A]]] = {
    val future = ask(invoker, Invoker.GetBodyParser(requestHeaders, bodyFunction), bodyParserTimeout)
    future.asPromise.map(_.asInstanceOf[Iteratee[Array[Byte], Either[Result, A]]])
  }

  def applicationProvider: ApplicationProvider

  def stop() {
    Invoker.system.shutdown()
    Logger.shutdown()
  }

}
