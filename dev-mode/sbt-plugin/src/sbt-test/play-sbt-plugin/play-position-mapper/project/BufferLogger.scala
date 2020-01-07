/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.{ LogEvent => Log4JLogEvent, _ }
import org.apache.logging.log4j.core.Filter.Result
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.filter.LevelRangeFilter
import org.apache.logging.log4j.core.layout.PatternLayout

object BufferLogger extends AbstractAppender(
  "FakeAppender",
  LevelRangeFilter.createFilter(Level.ERROR, Level.ERROR, Result.NEUTRAL, Result.DENY),
  PatternLayout.createDefaultLayout()
) {
  @volatile var messages = List.empty[String]

  def append(event: Log4JLogEvent): Unit = {
    if (event.getLevel == Level.ERROR) {
      synchronized {
        messages = event.getMessage.getFormattedMessage :: messages
      }
    }
  }
}
