/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import scala.language.implicitConversions

/**
 * Converter for Java Mode enum from Scala Mode
 */
object JavaModeConverter {
  implicit def asJavaMode(mode: play.api.Mode): play.Mode = mode.asJava
  implicit def asScalaMode(mode: play.Mode): play.api.Mode = mode.asScala()
}
