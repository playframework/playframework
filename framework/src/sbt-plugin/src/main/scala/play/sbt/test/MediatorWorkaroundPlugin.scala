/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt.test

import sbt._

/**
 * This tracks https://github.com/sbt/sbt/issues/2786 and it is intended to
 * be used ONLY internally for test purposes.
 */
object MediatorWorkaroundPlugin extends MediatorWorkaroundPluginCompat {
  override def requires = plugins.JvmPlugin
  override def trigger = noTrigger
}
