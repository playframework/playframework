/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.runsupport

trait LoggerProxy {
  def verbose(message: => String): Unit
  def debug(message: => String): Unit
  def info(message: => String): Unit
  def warn(message: => String): Unit
  def error(message: => String): Unit
  def trace(t: => Throwable): Unit
  def success(message: => String): Unit
}
