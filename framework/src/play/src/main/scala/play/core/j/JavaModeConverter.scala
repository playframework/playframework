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
  implicit def asScalaMode(mode: play.Mode): play.api.Mode.Mode = mode match {
    case play.Mode.DEV => play.api.Mode.Dev
    case play.Mode.TEST => play.api.Mode.Test
    case play.Mode.PROD => play.api.Mode.Prod
  }
}
