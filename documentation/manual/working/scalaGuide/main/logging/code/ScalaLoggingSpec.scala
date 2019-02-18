/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.logging

import javax.inject.Inject

import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import org.slf4j._
import play.api._
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.mvc._

import play.api.test._
import play.api.test.Helpers._

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
        logger.isDebugEnabled().returns(true)
        logger.isErrorEnabled().returns(true)
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

      there.was(atLeastOne(Logger.logger).isDebugEnabled())
      there.was(atLeastOne(Logger.logger).debug(anyString))
      there.was(atMostOne(Logger.logger).isErrorEnabled())
      there.was(atMostOne(Logger.logger).error(anyString, any[Throwable]))
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

      class AccessLoggingAction @Inject()(parser: BodyParsers.Default)(implicit ec: ExecutionContext)
          extends ActionBuilderImpl(parser) {
        val accessLogger = Logger("access")
        override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
          accessLogger.info(s"method=${request.method} uri=${request.uri} remote-address=${request.remoteAddress}")
          block(request)
        }
      }

      class Application @Inject()(val accessLoggingAction: AccessLoggingAction, cc: ControllerComponents)
          extends AbstractController(cc) {

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
      implicit val system               = ActorSystem()
      implicit val mat                  = ActorMaterializer()
      implicit val ec: ExecutionContext = system.dispatcher
      val controller =
        new Application(new AccessLoggingAction(new BodyParsers.Default()), Helpers.stubControllerComponents())

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

      class AccessLoggingFilter @Inject()(implicit val mat: Materializer) extends Filter {

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
      val loggerName                         = underlyingLogger.getName()
      //#logging-underlying

      loggerName must equalTo("access")
    }
  }

  //#logging-default-marker-context
  val someMarker: org.slf4j.Marker = MarkerFactory.getMarker("SOMEMARKER")
  case object SomeMarkerContext extends play.api.DefaultMarkerContext(someMarker)
  //#logging-default-marker-context

  "MarkerContext" should {
    "return some marker" in {
      import play.api.Logger
      val logger: Logger = Logger("access")

      //#logging-marker-context
      val marker: org.slf4j.Marker = MarkerFactory.getMarker("SOMEMARKER")
      val mc: MarkerContext        = MarkerContext(marker)
      //#logging-marker-context
      mc.marker must beSome.which(_ must be_==(marker))
    }

    "logger.info with explicit marker context" in {
      import play.api.Logger
      val logger: Logger = Logger("access")

      //#logging-log-info-with-explicit-markercontext
      // use a typed marker as input
      logger.info("log message with explicit marker context with case object")(SomeMarkerContext)

      // Use a specified marker.
      val otherMarker: Marker               = MarkerFactory.getMarker("OTHER")
      val otherMarkerContext: MarkerContext = MarkerContext(otherMarker)
      logger.info("log message with explicit marker context")(otherMarkerContext)
      //#logging-log-info-with-explicit-markercontext

      success
    }

    "logger.info with implicit marker context" in {
      import play.api.Logger
      val logger: Logger = Logger("access")

      //#logging-log-info-with-implicit-markercontext
      val marker: Marker             = MarkerFactory.getMarker("SOMEMARKER")
      implicit val mc: MarkerContext = MarkerContext(marker)

      // Use the implicit MarkerContext in logger.info...
      logger.info("log message with implicit marker context")
      //#logging-log-info-with-implicit-markercontext

      mc.marker must beSome.which(_ must be_==(marker))
    }

    "implicitly convert a Marker to a MarkerContext" in {
      import play.api.Logger
      val logger: Logger = Logger("access")

      //#logging-log-info-with-implicit-conversion
      val mc: MarkerContext = MarkerFactory.getMarker("SOMEMARKER")

      // Use the marker that has been implicitly converted to MarkerContext
      logger.info("log message with implicit marker context")(mc)
      //#logging-log-info-with-implicit-conversion

      success
    }

    "implicitly pass marker context in controller" in new WithApplication() with Injecting {
      val controller = inject[ImplicitRequestController]

      val result = controller.asyncIndex()(FakeRequest())
      contentAsString(result) must be_==("testing")
    }
  }

}

//#logging-request-context-trait
trait RequestMarkerContext {

  // Adding 'implicit request' enables implicit conversion chaining
  // See http://docs.scala-lang.org/tutorials/FAQ/chaining-implicits.html
  implicit def requestHeaderToMarkerContext(implicit request: RequestHeader): MarkerContext = {
    import net.logstash.logback.marker.LogstashMarker
    import net.logstash.logback.marker.Markers._

    val requestMarkers: LogstashMarker = append("host", request.host)
      .and(append("path", request.path))

    MarkerContext(requestMarkers)
  }

}
//#logging-request-context-trait

class ImplicitRequestController @Inject()(cc: ControllerComponents)(implicit otherExecutionContext: ExecutionContext)
    extends AbstractController(cc)
    with RequestMarkerContext {
  private val logger = play.api.Logger(getClass)

  //#logging-log-info-with-request-context
  def asyncIndex = Action.async { implicit request =>
    Future {
      methodInOtherExecutionContext() // implicit conversion here
    }(otherExecutionContext)
  }

  def methodInOtherExecutionContext()(implicit mc: MarkerContext): Result = {
    logger.debug("index: ") // same as above
    Ok("testing")
  }
  //#logging-log-info-with-request-context
}

//#logging-log-trace-with-tracer-controller
trait TracerMarker {
  import TracerMarker._

  implicit def requestHeaderToMarkerContext(implicit request: RequestHeader): MarkerContext = {
    val marker = org.slf4j.MarkerFactory.getDetachedMarker("dynamic") // base do-nothing marker...
    if (request.getQueryString("trace").nonEmpty) {
      marker.add(tracerMarker)
    }
    marker
  }
}

object TracerMarker {
  private val tracerMarker = org.slf4j.MarkerFactory.getMarker("TRACER")
}

class TracerBulletController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with TracerMarker {
  private val logger = play.api.Logger("application")

  def index = Action { implicit request: Request[AnyContent] =>
    logger.trace("Only logged if queryString contains trace=true")

    Ok("hello world")
  }
}
//#logging-log-trace-with-tracer-controller
