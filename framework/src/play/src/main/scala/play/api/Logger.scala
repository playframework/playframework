/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import org.slf4j.{ Logger => Slf4jLogger, LoggerFactory, Marker }

import scala.collection.mutable
import scala.language.implicitConversions
import scala.collection.JavaConverters._

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

  @inline def enabled: Boolean = true

  /**
   * `true` if the logger instance is enabled for the `TRACE` level.
   */
  def isTraceEnabled(implicit mc: MarkerContext): Boolean = enabled && (mc.marker match {
    case None =>
      logger.isTraceEnabled
    case Some(marker) =>
      logger.isTraceEnabled(marker)
  })

  /**
   * `true` if the logger instance is enabled for the `DEBUG` level.
   */
  def isDebugEnabled(implicit mc: MarkerContext): Boolean = enabled && (mc.marker match {
    case None =>
      logger.isDebugEnabled
    case Some(marker) =>
      logger.isDebugEnabled(marker)
  })

  /**
   * `true` if the logger instance is enabled for the `INFO` level.
   */
  def isInfoEnabled(implicit mc: MarkerContext): Boolean = enabled && (mc.marker match {
    case None =>
      logger.isInfoEnabled
    case Some(marker) =>
      logger.isInfoEnabled(marker)
  })

  /**
   * `true` if the logger instance is enabled for the `WARN` level.
   */
  def isWarnEnabled(implicit mc: MarkerContext): Boolean = enabled && (mc.marker match {
    case None =>
      logger.isWarnEnabled()
    case Some(marker) =>
      logger.isWarnEnabled(marker)
  })

  /**
   * `true` if the logger instance is enabled for the `ERROR` level.
   */
  def isErrorEnabled(implicit mc: MarkerContext): Boolean = enabled && (mc.marker match {
    case None =>
      logger.isErrorEnabled()
    case Some(marker) =>
      logger.isErrorEnabled(marker)
  })

  /**
   * Logs a message with the `TRACE` level.
   *
   * @param message the message to log
   * @param mc the implicit marker context, if defined.
   */
  def trace(message: => String)(implicit mc: MarkerContext): Unit = {
    if (isTraceEnabled) {
      mc.marker match {
        case None => logger.trace(message)
        case Some(marker) => logger.trace(marker, message)
      }
    }
  }

  /**
   * Logs a message with the `TRACE` level.
   *
   * @param message the message to log
   * @param error the associated exception
   * @param mc the implicit marker context, if defined.
   */
  def trace(message: => String, error: => Throwable)(implicit mc: MarkerContext): Unit = {
    if (isTraceEnabled) {
      mc.marker match {
        case None => logger.trace(message, error)
        case Some(marker) => logger.trace(marker, message, error)
      }
    }
  }

  /**
   * Logs a message with the `DEBUG` level.
   *
   * @param message the message to log
   * @param mc the implicit marker context, if defined.
   */
  def debug(message: => String)(implicit mc: MarkerContext): Unit = {
    if (isDebugEnabled) {
      mc.marker match {
        case None => logger.debug(message)
        case Some(marker) => logger.debug(marker, message)
      }
    }
  }

  /**
   * Logs a message with the `DEBUG` level.
   *
   * @param message the message to log
   * @param error the associated exception
   * @param mc the implicit marker context, if defined.
   */
  def debug(message: => String, error: => Throwable)(implicit mc: MarkerContext): Unit = {
    if (isDebugEnabled) {
      mc.marker match {
        case None => logger.debug(message, error)
        case Some(marker) => logger.debug(marker, message, error)
      }
    }
  }

  /**
   * Logs a message with the `INFO` level.
   *
   * @param message the message to log
   * @param mc the implicit marker context, if defined.
   */
  def info(message: => String)(implicit mc: MarkerContext): Unit = {
    if (isInfoEnabled) {
      mc.marker match {
        case None => logger.info(message)
        case Some(marker) => logger.info(marker, message)
      }
    }
  }

  /**
   * Logs a message with the `INFO` level.
   *
   * @param message the message to log
   * @param error the associated exception
   * @param mc the implicit marker context, if defined.
   */
  def info(message: => String, error: => Throwable)(implicit mc: MarkerContext): Unit = {
    if (isInfoEnabled) {
      mc.marker match {
        case None => logger.info(message, error)
        case Some(marker) => logger.info(marker, message, error)
      }
    }
  }

  /**
   * Logs a message with the `WARN` level.
   *
   * @param message the message to log
   * @param mc the implicit marker context, if defined.
   */
  def warn(message: => String)(implicit mc: MarkerContext): Unit = {
    if (isWarnEnabled) {
      mc.marker match {
        case None => logger.warn(message)
        case Some(marker) => logger.warn(marker, message)
      }
    }
  }

  /**
   * Logs a message with the `WARN` level.
   *
   * @param message the message to log
   * @param error the associated exception
   * @param mc the implicit marker context, if defined.
   */
  def warn(message: => String, error: => Throwable)(implicit mc: MarkerContext): Unit = {
    if (isWarnEnabled) {
      mc.marker match {
        case None => logger.warn(message, error)
        case Some(marker) => logger.warn(marker, message, error)
      }
    }
  }

  /**
   * Logs a message with the `ERROR` level.
   *
   * @param message the message to log
   * @param mc the implicit marker context, if defined.
   */
  def error(message: => String)(implicit mc: MarkerContext): Unit = {
    if (isErrorEnabled) {
      mc.marker match {
        case None => logger.error(message)
        case Some(marker) => logger.error(marker, message)
      }
    }
  }

  /**
   * Logs a message with the `ERROR` level.
   *
   * @param message the message to log
   * @param error the associated exception
   * @param mc the implicit marker context, if defined.
   */
  def error(message: => String, error: => Throwable)(implicit mc: MarkerContext): Unit = {
    if (isErrorEnabled) {
      mc.marker match {
        case None => logger.error(message, error)
        case Some(marker) => logger.error(marker, message, error)
      }
    }
  }

}

/**
 * A trait that can mixed into a class or trait to add a `logger` named based on the class name.
 */
trait Logging {
  protected val logger: Logger = Logger(getClass)
}

/**
 * A Play logger.
 *
 * @param logger the underlying SL4FJ logger
 */
class Logger private (val logger: Slf4jLogger, isEnabled: => Boolean) extends LoggerLike {

  def this(logger: Slf4jLogger) = this(logger, true)

  @inline override def enabled = isEnabled

  /**
   * Get a logger that only works when the application is in the given mode(s).
   *
   * If the global application mode has not been set (by calling Logger.setApplicationMode), this has no effect.
   */
  def forMode(mode: Mode*): Logger = {
    modeLoggerCache.getOrElseUpdate(mode, new Logger(logger, Logger.applicationMode.forall(mode.contains)))
  }

  private[this] val modeLoggerCache: mutable.Map[Seq[Mode], Logger] =
    new ConcurrentHashMap[Seq[Mode], Logger]().asScala
}

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
 */
object Logger extends Logger(LoggerFactory.getLogger("application")) { // TODO: After Play 2.7 this should simply become: object Logger {

  private[this] val log: Slf4jLogger = LoggerFactory.getLogger(getClass)

  private[this] var _mode: Option[Mode] = None
  private[this] val _appsRunning: AtomicInteger = new AtomicInteger(0)

  /**
   * The global application mode currently being used by the logging API.
   */
  def applicationMode: Option[Mode] = _mode

  /**
   * Set the global application mode used for logging. Used when the Play application starts.
   */
  def setApplicationMode(mode: Mode): Unit = {
    val appsRunning = _appsRunning.incrementAndGet()
    applicationMode foreach { currentMode =>
      if (currentMode != mode) {
        log.warn(s"Setting logging mode to $mode when it was previously set to $currentMode")
        log.warn(s"There are currently $appsRunning applications running.")
      }
    }
    _mode = Some(mode)
  }

  /**
   * Unset the global application mode. Used when the application shuts down.
   *
   * If multiple applications are running
   */
  def unsetApplicationMode(): Unit = {
    val appsRunning = _appsRunning.decrementAndGet()
    if (appsRunning == 0) {
      _mode = None
    } else if (appsRunning < 0) {
      log.warn("Cannot unset application mode because none was previously set")
      _mode = None
      _appsRunning.incrementAndGet()
    }
  }

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
  def apply(clazz: Class[_]): Logger = new Logger(LoggerFactory.getLogger(clazz.getName.stripSuffix("$")))

  // ### Deprecate inherited methods from Logger and LoggerLike

  @deprecated("Create an instance of via Logger(...) and use the same-named method. Or use SLF4J directly.", "2.7.0")
  override def enabled: Boolean = super.enabled

  @deprecated("Create an instance of via Logger(...) and use the same-named method. Or use SLF4J directly.", "2.7.0")
  override def forMode(mode: Mode*): Logger = super.forMode(mode: _*)

  @deprecated("Create an instance of via Logger(...) and use the same-named method. Or use SLF4J directly.", "2.7.0")
  override def isTraceEnabled(implicit mc: MarkerContext): Boolean = super.isTraceEnabled

  @deprecated("Create an instance of via Logger(...) and use the same-named method. Or use SLF4J directly.", "2.7.0")
  override def isDebugEnabled(implicit mc: MarkerContext): Boolean = super.isDebugEnabled

  @deprecated("Create an instance of via Logger(...) and use the same-named method. Or use SLF4J directly.", "2.7.0")
  override def isInfoEnabled(implicit mc: MarkerContext): Boolean = super.isInfoEnabled

  @deprecated("Create an instance of via Logger(...) and use the same-named method. Or use SLF4J directly.", "2.7.0")
  override def isWarnEnabled(implicit mc: MarkerContext): Boolean = super.isWarnEnabled

  @deprecated("Create an instance of via Logger(...) and use the same-named method. Or use SLF4J directly.", "2.7.0")
  override def isErrorEnabled(implicit mc: MarkerContext): Boolean = super.isErrorEnabled

  @deprecated("Create an instance of via Logger(...) and use the same-named method. Or use SLF4J directly.", "2.7.0")
  override def trace(message: => String)(implicit mc: MarkerContext): Unit = super.trace(message)

  @deprecated("Create an instance of via Logger(...) and use the same-named method. Or use SLF4J directly.", "2.7.0")
  override def trace(message: => String, error: => Throwable)(implicit mc: MarkerContext): Unit = super.trace(message, error)

  @deprecated("Create an instance of via Logger(...) and use the same-named method. Or use SLF4J directly.", "2.7.0")
  override def debug(message: => String)(implicit mc: MarkerContext): Unit = super.debug(message)

  @deprecated("Create an instance of via Logger(...) and use the same-named method. Or use SLF4J directly.", "2.7.0")
  override def debug(message: => String, error: => Throwable)(implicit mc: MarkerContext): Unit = super.debug(message, error)

  @deprecated("Create an instance of via Logger(...) and use the same-named method. Or use SLF4J directly.", "2.7.0")
  override def info(message: => String)(implicit mc: MarkerContext): Unit = super.info(message)

  @deprecated("Create an instance of via Logger(...) and use the same-named method. Or use SLF4J directly.", "2.7.0")
  override def info(message: => String, error: => Throwable)(implicit mc: MarkerContext): Unit = super.info(message, error)

  @deprecated("Create an instance of via Logger(...) and use the same-named method. Or use SLF4J directly.", "2.7.0")
  override def warn(message: => String)(implicit mc: MarkerContext): Unit = super.warn(message)

  @deprecated("Create an instance of via Logger(...) and use the same-named method. Or use SLF4J directly.", "2.7.0")
  override def warn(message: => String, error: => Throwable)(implicit mc: MarkerContext): Unit = super.warn(message, error)

  @deprecated("Create an instance of via Logger(...) and use the same-named method. Or use SLF4J directly.", "2.7.0")
  override def error(message: => String)(implicit mc: MarkerContext): Unit = super.error(message)

  @deprecated("Create an instance of via Logger(...) and use the same-named method. Or use SLF4J directly.", "2.7.0")
  override def error(message: => String, error: => Throwable)(implicit mc: MarkerContext): Unit = super.error(message, error)
}

/**
 * A MarkerContext trait, to provide easy access to org.slf4j.Marker in Logger API.  This is usually accessed
 * with a marker through an implicit conversion from a Marker.
 *
 * {{{
 *   implicit val markerContext: MarkerContext = org.slf4j.MarkerFactory.getMarker("EXAMPLEMARKER")
 *   log.error("This message will be logged with the EXAMPLEMARKER marker")
 * }}}
 *
 */
trait MarkerContext {
  /**
   * @return an SLF4J marker, if one has been defined.
   */
  def marker: Option[Marker]
}

object MarkerContext extends LowPriorityMarkerContextImplicits {

  /**
   * Provides an instance of a MarkerContext from a Marker.  The explicit form is useful when
   * you want to explicitly tag a log message with a particular Marker and you already have a
   * Marker in implicit scope.
   *
   * {{{
   *   implicit val implicitContext: MarkerContext = ...
   *   val explicitContext: MarkerContext = MarkerContext(MarkerFactory.getMarker("EXPLICITMARKER"))
   *
   *   // do not use the implicit MarkerContext
   *   log.error("This message is logged with EXPLICITMARKER")(explicitContext)
   * }}}
   *
   * @param marker the marker to wrap in DefaultMarkerContext
   * @return an instance of DefaultMarkerContext.
   */
  def apply(marker: Marker): MarkerContext = {
    new DefaultMarkerContext(marker)
  }
}

trait LowPriorityMarkerContextImplicits {

  /**
   * A MarkerContext that returns None.  This is used as the "default" marker context if
   * no implicit MarkerContext is found in local scope (meaning there is nothing defined
   * through import or "implicit val").
   */
  implicit val NoMarker = MarkerContext(null)

  /**
   * Enables conversion from a marker to a MarkerContext:
   *
   * {{{
   *  val mc: MarkerContext = MarkerFactory.getMarker("SOMEMARKER")
   * }}}
   *
   * @param marker the SLF4J marker to convert
   * @return the result of `MarkerContext.apply(marker)`
   */
  implicit def markerToMarkerContext(marker: Marker): MarkerContext = {
    MarkerContext(marker)
  }
}

/**
 * A default marker context.  This is used by `MarkerContext.apply`, but can also be used to provide
 * explicit typing for markers.  For example, to define a SecurityContext marker, you can define a case
 * object extending DefaultMarkerContext:
 *
 * {{{
 * case object SecurityMarkerContext extends DefaultMarkerContext(MarkerFactory.getMarker("SECURITY"))
 * }}}
 *
 * @param someMarker a marker used in the `marker` method.
 */
class DefaultMarkerContext(someMarker: Marker) extends MarkerContext {
  def marker: Option[Marker] = Option(someMarker)
}

object MarkerContexts {

  case object SecurityMarkerContext extends DefaultMarkerContext(org.slf4j.MarkerFactory.getMarker("SECURITY"))

}
