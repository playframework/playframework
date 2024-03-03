/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

object Colors {
  import scala.Console._

  private lazy val isANSISupported: Boolean = {
    sys.props
      .get("sbt.log.noformat")
      .map(_ != "true")
      .orElse {
        sys.props
          .get("os.name")
          .map(_.toLowerCase(java.util.Locale.ENGLISH))
          .filter(_.contains("windows"))
          .map(_ => false)
      }
      .getOrElse(true)
  }

  def red(str: String): String     = if (isANSISupported) RED + str + RESET else str
  def blue(str: String): String    = if (isANSISupported) BLUE + str + RESET else str
  def cyan(str: String): String    = if (isANSISupported) CYAN + str + RESET else str
  def green(str: String): String   = if (isANSISupported) GREEN + str + RESET else str
  def magenta(str: String): String = if (isANSISupported) MAGENTA + str + RESET else str
  def white(str: String): String   = if (isANSISupported) WHITE + str + RESET else str
  def black(str: String): String   = if (isANSISupported) BLACK + str + RESET else str
  def yellow(str: String): String  = if (isANSISupported) YELLOW + str + RESET else str
  def bold(str: String): String    = if (isANSISupported) BOLD + str + RESET else str
}
