/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt.scriptedtools

import scala.reflect.{ ClassTag, classTag }

import sbt._

trait ScriptedTools0 {

  def assertNotEmpty[T: ClassTag](m: xsbti.Maybe[T]): T = {
    if (m.isEmpty) throw new Exception(s"Expected Some[${classTag[T]}]")
    else m.get()
  }

  def bufferLoggerMessages = bufferLogger.messages

  // sbt 1.0 defines extraLogs as a SettingKey[ScopedKey[_] => Seq[Appender]]
  // while sbt 0.13 uses SettingKey[ScopedKey[_] => Seq[AbstractLogger]]
  object bufferLogger extends AbstractLogger {
    @volatile var messages                                     = List.empty[String]
    def getLevel                                               = Level.Error
    def setLevel(newLevel: Level.Value)                        = ()
    def setTrace(flag: Int)                                    = ()
    def getTrace                                               = 0
    def successEnabled                                         = false
    def setSuccessEnabled(flag: Boolean)                       = ()
    def control(event: ControlEvent.Value, message: => String) = ()
    def logAll(events: Seq[LogEvent])                          = events.foreach(log)
    def trace(t: => Throwable)                                 = ()
    def success(message: => String)                            = ()
    def log(level: Level.Value, message: => String) = {
      if (level == Level.Error) synchronized {
        messages = message :: messages
      }
    }
  }

}
