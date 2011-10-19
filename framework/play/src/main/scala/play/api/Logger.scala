package play.api

import org.slf4j.{ LoggerFactory, Logger => Slf4jLogger }

trait LoggerLike {

  val logger: Slf4jLogger

  lazy val isTraceEnabled = logger.isTraceEnabled
  lazy val isDebugEnabled = logger.isDebugEnabled
  lazy val isInfoEnabled = logger.isInfoEnabled
  lazy val isWarnEnabled = logger.isWarnEnabled
  lazy val isErrorEnabled = logger.isWarnEnabled

  def trace(message: String) { logger.trace(message) }
  def trace(message: String, error: Throwable) { logger.trace(message, error) }

  def debug(message: String) { logger.debug(message) }
  def debug(message: String, error: Throwable) { logger.debug(message, error) }

  def info(message: String) { logger.info(message) }
  def info(message: String, error: Throwable) { logger.info(message, error) }

  def warn(message: String) { logger.warn(message) }
  def warn(message: String, error: Throwable) { logger.warn(message, error) }

  def error(message: String) { logger.error(message) }
  def error(message: String, error: Throwable) { logger.error(message, error) }

}

class Logger(val logger: Slf4jLogger) extends LoggerLike

object Logger extends LoggerLike {

  val logger = LoggerFactory.getLogger("application")

  def apply(name: String) = new Logger(LoggerFactory.getLogger(name))
  def apply[T](clazz: Class[T]) = new Logger(LoggerFactory.getLogger(clazz))

  def configure(configFile: java.net.URL, properties: Map[String, String] = Map.empty) = {

    // Redirect JUL -> SL4J
    {
      import org.slf4j.bridge._
      import java.util.logging._

      Option(java.util.logging.Logger.getLogger("")).map { root =>
        root.setLevel(Level.FINEST)
        root.getHandlers.foreach(root.removeHandler(_))
      }

      SLF4JBridgeHandler.install()
    }

    // Configure logback
    {
      import org.slf4j._

      import ch.qos.logback.classic.joran._
      import ch.qos.logback.core.util._
      import ch.qos.logback.classic._

      val ctx = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
      try {
        val configurator = new JoranConfigurator
        configurator.setContext(ctx)
        ctx.reset
        properties.map {
          case (name, value) => ctx.putProperty(name, value)
        }
        configurator.doConfigure(configFile)
      } catch {
        case _ =>
      }

      StatusPrinter.printInCaseOfErrorsOrWarnings(ctx)
    }

  }

}

import ch.qos.logback.classic._
import ch.qos.logback.classic.spi._
import ch.qos.logback.classic.pattern._

class ColoredLevel extends ClassicConverter {

  def convert(event: ILoggingEvent) = {
    event.getLevel match {
      case Level.TRACE => "[\u001b[0;35mtrace\u001b[m]"
      case Level.DEBUG => "[\u001b[0;36mdebug\u001b[m]"
      case Level.INFO => "[\u001b[0;37minfo\u001b[m]"
      case Level.WARN => "[\u001b[0;33mwarn\u001b[m]"
      case Level.ERROR => "[\u001b[0;31merror\u001b[m]"
    }
  }

}