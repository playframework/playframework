package play.core.server

import scala.language.postfixOps

import play.api._
import play.core._
import play.api.mvc._

import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }
import scala.util.control.NonFatal
import scala.concurrent.Future

trait WebSocketable {
  def getHeader(header: String): String
  def check: Boolean
}

/**
 * provides generic server behaviour for Play applications
 */
trait Server {

  // First delete the default log file for a fresh start (only in Dev Mode)
  try {
    if (mode == Mode.Dev) scalax.file.Path(new java.io.File(applicationProvider.path, "logs/application.log")).delete()
  } catch {
    case NonFatal(_) =>
  }

  // Configure the logger for the first time
  Logger.configure(
    Map("application.home" -> applicationProvider.path.getAbsolutePath),
    mode = mode)

  val bodyParserTimeout = {
    //put in proper config
    1 second
  }

  def mode: Mode.Mode

  def getHandlerFor(request: RequestHeader): Either[Future[SimpleResult], (RequestHeader, Handler, Application)] = {

    import scala.util.control.Exception

    def sendHandler: Try[(RequestHeader, Handler, Application)] = {
      try {
        applicationProvider.get.map { application =>
          application.global.onRequestReceived(request) match {
            case (requestHeader, handler) => (requestHeader, handler, application)
          }
        }
      } catch {
        case e: ThreadDeath => throw e
        case e: VirtualMachineError => throw e
        case e: Throwable => Failure(e)
      }
    }

    def logExceptionAndGetResult(e: Throwable) = {

      Logger.error(
        """
        |
        |! %sInternal server error, for (%s) [%s] ->
        |""".stripMargin.format(e match {
          case p: PlayException => "@" + p.id + " - "
          case _ => ""
        }, request.method, request.uri),
        e)

      DefaultGlobal.onError(request, e)

    }

    Exception
      .allCatch[Option[Future[SimpleResult]]]
      .either(applicationProvider.handleWebCommand(request).map(Future.successful))
      .left.map(logExceptionAndGetResult)
      .right.flatMap(maybeResult => maybeResult.toLeft(())).right.flatMap { _ =>
        sendHandler match {
          case Failure(e) => Left(logExceptionAndGetResult(e))
          case Success(v) => Right(v)
        }
      }

  }

  def applicationProvider: ApplicationProvider

  def stop() {
    Logger.shutdown()
  }

}
