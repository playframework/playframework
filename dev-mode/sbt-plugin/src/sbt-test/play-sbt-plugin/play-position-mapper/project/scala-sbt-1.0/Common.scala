/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt._
import sbt.Keys._
import play.sbt.PlayScala
import play.sbt.test.MediatorWorkaroundPlugin

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.{ LogEvent => Log4JLogEvent, _ }
import org.apache.logging.log4j.core.Filter.Result
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.filter.LevelRangeFilter
import org.apache.logging.log4j.core.layout.PatternLayout

object Common {

  // sbt 1.0 defines extraLogs as a SettingKey[ScopedKey[_] => Seq[Appender]]
  // while sbt 0.13 uses SettingKey[ScopedKey[_] => Seq[AbstractLogger]]
  val bufferLogger = new AbstractAppender("FakeAppender", LevelRangeFilter.createFilter(Level.ERROR, Level.ERROR, Result.NEUTRAL, Result.DENY), PatternLayout.createDefaultLayout()) {

    @volatile var messages = List.empty[String]

    override def append(event: Log4JLogEvent): Unit = {
      if (event.getLevel == Level.ERROR) synchronized {
        messages = event.getMessage.getFormattedMessage :: messages
      }
    }
  }

  import complete.DefaultParsers._

  def simpleParser(state: State) = Space ~> any.+.map(_.mkString(""))

  def checkLogContains(msg: String): Task[Boolean] = task {
    if (!bufferLogger.messages.exists(_.contains(msg))) {
      sys.error("Did not find log message:\n    '" + msg + "'\nin output:\n" + bufferLogger.messages.reverse.mkString("    ", "\n    ", ""))
    }
    true
  }

}
