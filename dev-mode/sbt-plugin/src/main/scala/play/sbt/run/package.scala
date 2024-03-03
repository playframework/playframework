/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import java.util.function.Supplier

import sbt._

import play.dev.filewatch.LoggerProxy

package object run {
  import scala.language.implicitConversions

  implicit def toLoggerProxy(in: Logger): LoggerProxy = new LoggerProxy {
    def verbose(message: Supplier[String]): Unit = in.verbose(message.get())
    def debug(message: Supplier[String]): Unit   = in.debug(message)
    def info(message: Supplier[String]): Unit    = in.info(message)
    def warn(message: Supplier[String]): Unit    = in.warn(message)
    def error(message: Supplier[String]): Unit   = in.error(message)
    def trace(t: Supplier[Throwable]): Unit      = in.trace(t)
    def success(message: Supplier[String]): Unit = in.success(message.get())
  }

  val PlayRun = sbt.PlayRun
}
