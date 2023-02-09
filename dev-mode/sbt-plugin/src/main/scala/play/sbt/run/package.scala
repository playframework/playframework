/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import sbt._

import play.dev.filewatch.LoggerProxy

package object run {
  import scala.language.implicitConversions

  implicit def toLoggerProxy(in: Logger): LoggerProxy = new LoggerProxy {
    def verbose(message: => String): Unit = in.verbose(message)
    def debug(message: => String): Unit   = in.debug(message)
    def info(message: => String): Unit    = in.info(message)
    def warn(message: => String): Unit    = in.warn(message)
    def error(message: => String): Unit   = in.error(message)
    def trace(t: => Throwable): Unit      = in.trace(t)
    def success(message: => String): Unit = in.success(message)
  }

  val PlayRun = sbt.PlayRun
}
