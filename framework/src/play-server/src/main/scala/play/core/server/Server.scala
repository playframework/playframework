/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server

import play.api.http.DefaultHttpErrorHandler

import scala.language.postfixOps

import play.api._
import play.core._
import play.api.mvc._

import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }
import scala.concurrent.Future

trait WebSocketable {
  def getHeader(header: String): String
  def check: Boolean
}

/**
 * provides generic server behaviour for Play applications
 */
trait Server {

  val bodyParserTimeout = {
    //put in proper config
    1 second
  }

  def mode: Mode.Mode

  def getHandlerFor(request: RequestHeader): Either[Future[Result], (RequestHeader, Handler, Application)] = {

    import scala.util.control.Exception

    def sendHandler: Try[(RequestHeader, Handler, Application)] = {
      try {
        applicationProvider.get.map { application =>
          application.requestHandler.handlerForRequest(request) match {
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
      DefaultHttpErrorHandler.onServerError(request, e)
    }

    Exception
      .allCatch[Option[Future[Result]]]
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
