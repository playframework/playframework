/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api

import org.slf4j.{ LoggerFactory, Marker, Logger => Slf4jLogger }

import scala.language.implicitConversions

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
  def isTraceEnabled(implicit mc: MarkerContext): Boolean = mc.marker match {
    case None =>
      logger.isTraceEnabled
    case Some(marker) =>
      logger.isTraceEnabled(marker)
  }

  /**
   * `true` if the logger instance is enabled for the `DEBUG` level.
   */
  def isDebugEnabled(implicit mc: MarkerContext): Boolean = mc.marker match {
    case None =>
      logger.isDebugEnabled
    case Some(marker) =>
      logger.isDebugEnabled(marker)
  }

  /**
   * `true` if the logger instance is enabled for the `INFO` level.
   */
  def isInfoEnabled(implicit mc: MarkerContext): Boolean = mc.marker match {
    case None =>
      logger.isInfoEnabled
    case Some(marker) =>
      logger.isInfoEnabled(marker)
  }

  /**
   * `true` if the logger instance is enabled for the `WARN` level.
   */
  def isWarnEnabled(implicit mc: MarkerContext): Boolean = mc.marker match {
    case None =>
      logger.isWarnEnabled()
    case Some(marker) =>
      logger.isWarnEnabled(marker)
  }

  /**
   * `true` if the logger instance is enabled for the `ERROR` level.
   */
  def isErrorEnabled(implicit mc: MarkerContext): Boolean = mc.marker match {
    case None =>
      logger.isErrorEnabled()
    case Some(marker) =>
      logger.isErrorEnabled(marker)
  }

  /**
   * Logs a message with the `TRACE` level.
   *
   * @param message the message to log
   * @param mc the implicit marker context, if defined.
   */
  def trace(message: => String)(implicit mc: MarkerContext): Unit = {
    mc.marker match {
      case None =>
        if (logger.isTraceEnabled()) logger.trace(message)
      case Some(marker) =>
        if (logger.isTraceEnabled(marker)) logger.trace(marker, message)
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
    mc.marker match {
      case None =>
        if (logger.isTraceEnabled()) logger.trace(message, error)
      case Some(marker) =>
        if (logger.isTraceEnabled(marker)) logger.trace(marker, message, error)
    }
  }

  /**
   * Logs a message with the `DEBUG` level.
   *
   * @param message the message to log
   * @param mc the implicit marker context, if defined.
   */
  def debug(message: => String)(implicit mc: MarkerContext): Unit = {
    mc.marker match {
      case None =>
        if (logger.isDebugEnabled()) logger.debug(message)
      case Some(marker) =>
        if (logger.isDebugEnabled(marker)) logger.debug(marker, message)
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
    mc.marker match {
      case None =>
        if (logger.isDebugEnabled()) logger.debug(message, error)
      case Some(marker) =>
        if (logger.isDebugEnabled(marker)) logger.debug(marker, message, error)
    }
  }

  /**
   * Logs a message with the `INFO` level.
   *
   * @param message the message to log
   * @param mc the implicit marker context, if defined.
   */
  def info(message: => String)(implicit mc: MarkerContext): Unit = {
    mc.marker match {
      case None =>
        if (logger.isInfoEnabled) logger.info(message)
      case Some(marker) =>
        if (logger.isInfoEnabled(marker)) logger.info(marker, message)
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
    mc.marker match {
      case None =>
        if (logger.isInfoEnabled) logger.info(message, error)
      case Some(marker) =>
        if (logger.isInfoEnabled(marker)) logger.info(marker, message, error)
    }
  }

  /**
   * Logs a message with the `WARN` level.
   *
   * @param message the message to log
   * @param mc the implicit marker context, if defined.
   */
  def warn(message: => String)(implicit mc: MarkerContext): Unit = {
    mc.marker match {
      case None =>
        if (logger.isWarnEnabled) logger.warn(message)
      case Some(marker) =>
        if (logger.isWarnEnabled(marker)) logger.warn(marker, message)
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
    if (logger.isWarnEnabled) logger.warn(message, error)
    mc.marker match {
      case None =>
        if (logger.isWarnEnabled) logger.warn(message, error)
      case Some(marker) =>
        if (logger.isWarnEnabled(marker)) logger.warn(marker, message, error)
    }
  }

  /**
   * Logs a message with the `ERROR` level.
   *
   * @param message the message to log
   * @param mc the implicit marker context, if defined.
   */
  def error(message: => String)(implicit mc: MarkerContext): Unit = {
    mc.marker match {
      case None =>
        if (logger.isErrorEnabled()) logger.error(message)
      case Some(marker) =>
        if (logger.isErrorEnabled(marker)) logger.error(marker, message)
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
    mc.marker match {
      case None =>
        if (logger.isErrorEnabled()) logger.error(message, error)
      case Some(marker) =>
        if (logger.isErrorEnabled(marker)) logger.error(marker, message, error)
    }
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
 */
object Logger extends LoggerLike {

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
  def apply[T](clazz: Class[T]): Logger = new Logger(LoggerFactory.getLogger(clazz.getName.stripSuffix("$")))

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
 * object extending DefaultMarkerContext
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
