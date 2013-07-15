package play.api

import org.slf4j.{ LoggerFactory, Logger => Slf4jLogger }
import scala.util.control.NonFatal

/**
 * Typical logger interface.
 */
trait LoggerLike {

  /**
   * The underlying SLF4J Logger.
   */
  val logger: Slf4jLogger

  /**
   * The underlying SLF4J Logger.
   */
  lazy val underlyingLogger = logger

  /**
   * `true` if the logger instance is enabled for the `TRACE` level.
   */
  def isTraceEnabled = logger.isTraceEnabled

  /**
   * `true` if the logger instance is enabled for the `DEBUG` level.
   */
  def isDebugEnabled = logger.isDebugEnabled

  /**
   * `true` if the logger instance is enabled for the `INFO` level.
   */
  def isInfoEnabled = logger.isInfoEnabled

  /**
   * `true` if the logger instance is enabled for the `WARN` level.
   */
  def isWarnEnabled = logger.isWarnEnabled

  /**
   * `true` if the logger instance is enabled for the `ERROR` level.
   */
  def isErrorEnabled = logger.isErrorEnabled

  /**
   * Logs a message with the `TRACE` level.
   *
   * @param message the message to log
   */
  def trace(message: => String) {
    if (logger.isTraceEnabled) logger.trace(message)
  }

  /**
   * Logs a message with the `TRACE` level.
   *
   * @param message the message to log
   * @param error the associated exception
   */
  def trace(message: => String, error: => Throwable) {
    if (logger.isTraceEnabled) logger.trace(message, error)
  }

  /**
   * Logs a message with the `DEBUG` level.
   *
   * @param message the message to log
   */
  def debug(message: => String) {
    if (logger.isDebugEnabled) logger.debug(message)
  }

  /**
   * Logs a message with the `DEBUG` level.
   *
   * @param message the message to log
   * @param error the associated exception
   */
  def debug(message: => String, error: => Throwable) {
    if (logger.isDebugEnabled) logger.debug(message, error)
  }

  /**
   * Logs a message with the `INFO` level.
   *
   * @param message the message to log
   */
  def info(message: => String) {
    if (logger.isInfoEnabled) logger.info(message)
  }

  /**
   * Logs a message with the `INFO` level.
   *
   * @param message the message to log
   * @param error the associated exception
   */
  def info(message: => String, error: => Throwable) {
    if (logger.isInfoEnabled) logger.info(message, error)
  }

  /**
   * Logs a message with the `WARN` level.
   *
   * @param message the message to log
   */
  def warn(message: => String) {
    if (logger.isWarnEnabled) logger.warn(message)
  }

  /**
   * Logs a message with the `WARN` level.
   *
   * @param message the message to log
   * @param error the associated exception
   */
  def warn(message: => String, error: => Throwable) {
    if (logger.isWarnEnabled) logger.warn(message, error)
  }

  /**
   * Logs a message with the `ERROR` level.
   *
   * @param message the message to log
   */
  def error(message: => String) {
    if (logger.isErrorEnabled) logger.error(message)
  }

  /**
   * Logs a message with the `ERROR` level.
   *
   * @param message the message to log
   * @param error the associated exception
   */
  def error(message: => String, error: => Throwable) {
    if (logger.isErrorEnabled) logger.error(message, error)
  }

}

/**
 * A Play logger.
 *
 * @param logger the underlying SL4FJ logger
 */
class Logger(val logger: Slf4jLogger) extends LoggerLike

/**
 * High-level API for logging operations.
 *
 * For example, logging with the default application logger:
 * {{{
 * Logger.info("Hello!")
 * }}}
 *
 * Logging with a custom logger:
 * {{{
 * Logger("my.logger").info("Hello!")
 * }}}
 *
 */
object Logger extends LoggerLike {

  /**
   * Initialize the Logger in a brut way.
   */
  def init(home: java.io.File) {
    Logger.configure(
      Map("application.home" -> home.getAbsolutePath),
      Map.empty,
      Mode.Test)
  }

  /**
   * The 'application' logger.
   */
  val logger = LoggerFactory.getLogger("application")

  /**
   * Obtains a logger instance.
   *
   * @param name the name of the logger
   * @return a logger
   */
  def apply(name: String): Logger = new Logger(LoggerFactory.getLogger(name))

  /**
   * Obtains a logger instance.
   *
   * @param clazz a class whose name will be used as logger name
   * @return a logger
   */
  def apply[T](clazz: Class[T]): Logger = new Logger(LoggerFactory.getLogger(clazz))

  /**
   * Reconfigures the underlying logback infrastructure.
   *
   * @param properties these properties will be added to the logger context (for example `application.home`)
   * @param levels the log levels
   * @param mode the application mode
   * @see http://logback.qos.ch/
   */
  def configure(properties: Map[String, String] = Map.empty, levels: Map[String, ch.qos.logback.classic.Level] = Map.empty, mode: Mode.Value) {

    // Redirect JUL -> SL4FJ
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

      try {
        val ctx = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
        val configurator = new JoranConfigurator
        configurator.setContext(ctx)
        ctx.reset()
        properties.foreach {
          case (name, value) => ctx.putProperty(name, value)
        }

        try {
          val configResource =
            Option(System.getProperty("logger.resource"))
              .map(s => if (s.startsWith("/")) s.drop(1) else s)
              .map(r => Option(this.getClass.getClassLoader.getResource(r))
                .getOrElse(new java.net.URL("file:///" + System.getProperty("logger.resource")))
              ).orElse {
                Option(System.getProperty("logger.file")).map(new java.io.File(_).toURI.toURL)
              }.orElse {
                Option(System.getProperty("logger.url")).map(new java.net.URL(_))
              }.orElse {
                Option(this.getClass.getClassLoader.getResource("application-logger.xml"))
                  .orElse(Option(this.getClass.getClassLoader.getResource("logger.xml")))
              }

          configResource.foreach { url => configurator.doConfigure(url) }
        } catch {
          case NonFatal(e) => e.printStackTrace()
        }

        levels.foreach {
          case (logger, level) => ctx.getLogger(logger).setLevel(level)
        }
        StatusPrinter.printIfErrorsOccured(ctx)
      } catch {
        case NonFatal(_) =>
      }

    }

  }

  /**
   * Shutdown the logger infrastructure.
   */
  def shutdown() {
    import ch.qos.logback.classic._

    val ctx = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    ctx.stop()
  }

  import ch.qos.logback.classic._
  import ch.qos.logback.classic.spi._
  import ch.qos.logback.classic.pattern._

  /**
   * A logback converter generating colored, lower-case level names.
   *
   * Used for example as:
   * {{{
   * %coloredLevel %logger{15} - %message%n%xException{5}
   * }}}
   */
  class ColoredLevel extends ClassicConverter {

    import play.utils.Colors

    def convert(event: ILoggingEvent): String = {
      event.getLevel match {
        case Level.TRACE => "[" + Colors.blue("trace") + "]"
        case Level.DEBUG => "[" + Colors.cyan("debug") + "]"
        case Level.INFO => "[" + Colors.white("info") + "]"
        case Level.WARN => "[" + Colors.yellow("warn") + "]"
        case Level.ERROR => "[" + Colors.red("error") + "]"
      }
    }

  }

}

