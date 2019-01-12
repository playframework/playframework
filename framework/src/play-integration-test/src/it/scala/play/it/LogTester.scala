/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import org.slf4j.LoggerFactory

import scala.collection.immutable
import scala.collection.mutable.ArrayBuffer

/**
 * Test utility for testing Play logs
 */
object LogTester {

  /** Record log events and return them for analysis. */
  def recordLogEvents[T](block: => T): (T, immutable.Seq[ILoggingEvent]) = {

    /** Collects all log events that occur */
    class RecordingAppender extends AppenderBase[ILoggingEvent] {
      private val eventBuffer = ArrayBuffer[ILoggingEvent]()

      override def append(e: ILoggingEvent): Unit = synchronized {
        eventBuffer += e
      }

      def events: immutable.Seq[ILoggingEvent] = synchronized {
        eventBuffer.to[immutable.Seq]
      }
    }

    // Get the Logback root logger and attach a RecordingAppender
    val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
    val appender = new RecordingAppender()
    appender.setContext(rootLogger.getLoggerContext)
    appender.start()
    rootLogger.addAppender(appender)
    val result: T = block
    rootLogger.detachAppender(appender)
    appender.stop()
    (result, appender.events)
  }

}