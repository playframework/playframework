/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it

import org.slf4j.LoggerFactory
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.classic.spi.ILoggingEvent
import scala.collection.mutable.ListBuffer
import ch.qos.logback.classic.{ Logger, LoggerContext, Level }

/**
 * Test utility for testing Play logs
 */
object LogTester {

  def withLogBuffer[T](block: LogBuffer => T) = {
    val ctx = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    val root = ctx.getLogger("ROOT")
    val rootLevel = root.getLevel
    val playLogger = play.api.Logger(this.getClass).asInstanceOf[Logger]
    val playLevel = playLogger.getLevel
    val appender = new LogBuffer()
    appender.start()
    try {
      root.addAppender(appender)
      root.setLevel(Level.ALL)
      playLogger.addAppender(appender)
      playLogger.setLevel(Level.ALL)
      block(appender)
    } finally {
      root.detachAppender(appender)
      root.setLevel(rootLevel)
      playLogger.detachAppender(appender)
      playLogger.setLevel(playLevel)

    }
  }

}

class LogBuffer extends AppenderBase[ILoggingEvent] {
  private val buffer = ListBuffer.empty[ILoggingEvent]

  def append(eventObject: ILoggingEvent) = buffer.synchronized {
    buffer.append(eventObject)
  }

  def find(level: Option[Level] = None,
    logger: Option[String] = None,
    messageContains: Option[String] = None): List[ILoggingEvent] = buffer.synchronized {
    val byLevel = level.fold(buffer) { l => buffer.filter(_.getLevel == l) }
    val byLogger = logger.fold(byLevel) { l => byLevel.filter(_.getLoggerName == l) }
    val byMessageContains = logger.fold(byLogger) { m => byLogger.filter(_.getMessage.contains(m)) }
    byMessageContains.toList
  }
}
