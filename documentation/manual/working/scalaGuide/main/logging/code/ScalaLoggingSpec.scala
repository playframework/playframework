/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.logging

import play.api.http._
import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import scala.concurrent.ExecutionContext

@RunWith(classOf[JUnitRunner])
class ScalaLoggingSpec extends Specification with Mockito {

  private def riskyCalculation: Int = {
    10 / scala.util.Random.nextInt(2)
  }

  "The default Logger" should {
    "properly log" in {

      object Logger extends play.api.LoggerLike {
        // Mock underlying logger implementation
        val logger = mock[org.slf4j.Logger].smart
        logger.isDebugEnabled() returns true
        logger.isErrorEnabled() returns true
      }

      //#logging-default-logger
      // Log some debug info
      Logger.debug("Attempting risky calculation.")

      try {
        val result = riskyCalculation

        // Log result if successful
        Logger.debug(s"Result=$result")
      } catch {
        case t: Throwable => {
          // Log error with message and Throwable.
          Logger.error("Exception with riskyCalculation", t)
        }
      }
      //#logging-default-logger

      there was atLeastOne(Logger.logger).isDebugEnabled()
      there was atLeastOne(Logger.logger).debug(anyString)
      there was atMostOne(Logger.logger).isErrorEnabled()
      there was atMostOne(Logger.logger).error(anyString, any[Throwable])
    }

  }

  "Creating a Logger" should {
    "return a new Logger with specified name" in {
      //#logging-import
      import play.api.Logger
      //#logging-import

      //#logging-create-logger-name
      val accessLogger: Logger = Logger("access")
      //#logging-create-logger-name

      accessLogger.underlyingLogger.getName must equalTo("access")
    }

    "return a new Logger with class name" in {
      import play.api.Logger

      //#logging-create-logger-class
      val logger: Logger = Logger(this.getClass())
      //#logging-create-logger-class

      logger.underlyingLogger.getName must equalTo("scalaguide.logging.ScalaLoggingSpec")
    }

    "allow for using multiple loggers" in {

//      object Logger extends LoggerLike {
//        // Mock underlying logger implementation
//        val logger = mock[org.slf4j.Logger].smart
//
//        def apply[T](clazz: Class[T]): play.api.Logger = new play.api.Logger(mock[org.slf4j.Logger].smart)
//        def apply[T](name: String): play.api.Logger = new play.api.Logger(mock[org.slf4j.Logger].smart)
//      }

      //#logging-pattern-mix
      import scala.concurrent.Future
      import play.api.Logger
      import play.api.mvc._
      import javax.inject.Inject

      class AccessLoggingAction @Inject() (parser: BodyParsers.Default)(implicit ec: ExecutionContext) extends ActionBuilderImpl(parser) {
        val accessLogger = Logger("access")
        override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
          accessLogger.info(s"method=${request.method} uri=${request.uri} remote-address=${request.remoteAddress}")
          block(request)
        }
      }

      class Application @Inject() (val accessLoggingAction: AccessLoggingAction) extends Controller {

        val logger = Logger(this.getClass())

        def index = accessLoggingAction {
          try {
            val result = riskyCalculation
            Ok(s"Result=$result")
          } catch {
            case t: Throwable => {
              logger.error("Exception with riskyCalculation", t)
              InternalServerError("Error in calculation: " + t.getMessage())
            }
          }
        }
      }
      //#logging-pattern-mix

      import akka.actor._
      import akka.stream.ActorMaterializer
      import play.api.http._
      import akka.stream.Materializer
      implicit val system = ActorSystem();
      implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
      val eh: HttpErrorHandler =
        new DefaultHttpErrorHandler(play.api.Environment.simple(), play.api.Configuration.empty)
      val controller = new Application(new AccessLoggingAction(
        new BodyParsers.Default(ParserConfiguration(), eh, ActorMaterializer())))

      controller.accessLoggingAction.accessLogger.underlyingLogger.getName must equalTo("access")
      controller.logger.underlyingLogger.getName must contain("Application")
    }

    "allow for use in filters" in {
      //#logging-pattern-filter
      import javax.inject.Inject
      import akka.stream.Materializer
      import scala.concurrent.ExecutionContext.Implicits.global
      import scala.concurrent.Future
      import play.api.Logger
      import play.api.mvc._
      import play.api._

      class AccessLoggingFilter @Inject() (implicit val mat: Materializer) extends Filter {

        val accessLogger = Logger("access")

        def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
          val resultFuture = next(request)

          resultFuture.foreach(result => {
            val msg = s"method=${request.method} uri=${request.uri} remote-address=${request.remoteAddress}" +
              s" status=${result.header.status}";
            accessLogger.info(msg)
          })

          resultFuture
        }
      }
      //#logging-pattern-filter

      ok
    }

  }

  "Underlying logger" should {
    "return logger name" in {
      import play.api.Logger

      val logger: Logger = Logger("access")

      //#logging-underlying
      val underlyingLogger: org.slf4j.Logger = logger.underlyingLogger
      val loggerName = underlyingLogger.getName()
      //#logging-underlying

      loggerName must equalTo("access")
    }
  }

}
