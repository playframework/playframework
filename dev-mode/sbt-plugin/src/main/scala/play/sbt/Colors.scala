/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

object Colors {
  import play.runsupport.{ Colors => RunColors }

  lazy val isANSISupported = RunColors.isANSISupported

  def red(str: String): String     = RunColors.red(str)
  def blue(str: String): String    = RunColors.blue(str)
  def cyan(str: String): String    = RunColors.cyan(str)
  def green(str: String): String   = RunColors.green(str)
  def magenta(str: String): String = RunColors.magenta(str)
  def white(str: String): String   = RunColors.white(str)
  def black(str: String): String   = RunColors.black(str)
  def yellow(str: String): String  = RunColors.yellow(str)
  def bold(str: String): String    = RunColors.bold(str)
}
