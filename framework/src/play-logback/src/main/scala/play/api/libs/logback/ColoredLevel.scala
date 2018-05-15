/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.logback

import ch.qos.logback.classic._
import ch.qos.logback.classic.pattern._
import ch.qos.logback.classic.spi._

/**
 * A logback converter generating colored, lower-case level names.
 *
 * Used for example as:
 * {{{
 * %coloredLevel %logger{15} - %message%n%xException{5}
 * }}}
 */
class ColoredLevel extends ClassicConverter {

  import play.utils.Colors

  def convert(event: ILoggingEvent): String = {
    event.getLevel match {
      case Level.TRACE => "[" + Colors.blue("trace") + "]"
      case Level.DEBUG => "[" + Colors.cyan("debug") + "]"
      case Level.INFO => "[" + Colors.white("info") + "]"
      case Level.WARN => "[" + Colors.yellow("warn") + "]"
      case Level.ERROR => "[" + Colors.red("error") + "]"
    }
  }

}
