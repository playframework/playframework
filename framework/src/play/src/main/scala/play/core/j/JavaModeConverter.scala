package play.core.j

import scala.language.implicitConversions

/**
 * Converter for Java Mode enum from Scala Mode
 */
object JavaModeConverter {
  implicit def asJavaMode(mode: play.api.Mode.Mode): play.Mode = mode match {
    case play.api.Mode.Dev => play.Mode.DEV
    case play.api.Mode.Test => play.Mode.TEST
    case play.api.Mode.Prod => play.Mode.PROD
  }
}
