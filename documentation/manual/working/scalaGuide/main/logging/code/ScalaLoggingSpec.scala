/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.logging

import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.data.validation.ValidationError
import play.api.LoggerLike

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
      
      trait AccessLogging {
        
        val accessLogger = Logger("access")
  
        object AccessLoggingAction extends ActionBuilder[Request] {
    
          def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
            accessLogger.info(s"method=${request.method} uri=${request.uri} remote-address=${request.remoteAddress}")
            block(request)
          }
        }
      }
      
      object Application extends Controller with AccessLogging {
        
        val logger = Logger(this.getClass())
        
        def index = AccessLoggingAction {
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
      
      Application.accessLogger.underlyingLogger.getName must equalTo("access")
      Application.logger.underlyingLogger.getName must contain("Application")
    }
    
    "allow for use in filters" in {
      //#logging-pattern-filter
      import scala.concurrent.ExecutionContext.Implicits.global
      import scala.concurrent.Future
      import play.api.Logger
      import play.api.mvc._
      import play.api._
      
      object AccessLoggingFilter extends Filter {
        
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

      object Global extends WithFilters(AccessLoggingFilter) {
        
        override def onStart(app: Application) {
          Logger.info("Application has started")
        }

        override def onStop(app: Application) {
          Logger.info("Application has stopped")
        }
      }
      //#logging-pattern-filter
      
      AccessLoggingFilter.accessLogger.underlyingLogger.getName must equalTo("access")
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
