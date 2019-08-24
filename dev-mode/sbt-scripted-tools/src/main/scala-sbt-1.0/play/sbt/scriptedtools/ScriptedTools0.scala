/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt.scriptedtools

import scala.reflect.ClassTag
import scala.reflect.classTag

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.{ LogEvent => Log4JLogEvent, _ }
import org.apache.logging.log4j.core.Filter.Result
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.filter.LevelRangeFilter
import org.apache.logging.log4j.core.layout.PatternLayout

trait ScriptedTools0 {

  def assertNotEmpty[T: ClassTag](o: java.util.Optional[T]): T = {
    if (o.isPresent) o.get()
    else throw new Exception(s"Expected Some[${classTag[T]}]")
  }

  def bufferLoggerMessages = bufferLogger.messages

  // sbt 1.0 defines extraLogs as a SettingKey[ScopedKey[_] => Seq[Appender]]
  // while sbt 0.13 uses SettingKey[ScopedKey[_] => Seq[AbstractLogger]]
  object bufferLogger
      extends AbstractAppender(
        "FakeAppender",
        LevelRangeFilter.createFilter(Level.ERROR, Level.ERROR, Result.NEUTRAL, Result.DENY),
        PatternLayout.createDefaultLayout()
      ) {
    @volatile var messages = List.empty[String]

    override def append(event: Log4JLogEvent): Unit = {
      if (event.getLevel == Level.ERROR) synchronized {
        messages = event.getMessage.getFormattedMessage :: messages
      }
    }
  }

}
