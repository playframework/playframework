/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db.evolutions

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{ Level, Logger => LogbackLogger }
import ch.qos.logback.core.AppenderBase
import org.slf4j.{ Logger => Slf4jLogger, LoggerFactory }
import scala.reflect.ClassTag

import scala.collection.mutable

class LogbackCapturingAppender private (slf4jLogger: Slf4jLogger) extends AppenderBase[ILoggingEvent] {

  private val _logger: LogbackLogger = {
    val logger = slf4jLogger.asInstanceOf[LogbackLogger]
    logger.setLevel(Level.ALL)
    logger.addAppender(this)
    logger
  }

  private val _events: mutable.ArrayBuffer[ILoggingEvent] = new mutable.ArrayBuffer

  /**
   * Start the appender
   */
  start()

  /**
   * Returns the list of all captured logging events
   */
  def events: Seq[ILoggingEvent] = _events.toSeq

  protected def append(event: ILoggingEvent): Unit = synchronized {
    _events += event
  }

  private def detach(): Unit = {
    _logger.detachAppender(this)
    _events.clear()
  }
}

object LogbackCapturingAppender {
  private[this] val _appenders: mutable.ArrayBuffer[LogbackCapturingAppender] = new mutable.ArrayBuffer

  def apply[T](implicit ct: ClassTag[T]): LogbackCapturingAppender =
    attachForLogger(LoggerFactory.getLogger(ct.runtimeClass))

  /**
   * Get a capturing appender for the given logger
   */
  def attachForLogger(playLogger: play.api.Logger): LogbackCapturingAppender = attachForLogger(playLogger.logger)

  /**
   * Get a capturing appender for the given logger
   */
  def attachForLogger(slf4jLogger: Slf4jLogger): LogbackCapturingAppender = {
    val appender = new LogbackCapturingAppender(slf4jLogger)
    _appenders += appender
    appender
  }

  /**
   * Detach all the appenders we attached
   */
  def detachAll(): Unit = {
    _appenders foreach (_.detach())
    _appenders.clear()
  }
}
