/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt.test

import sbt.Keys.{ ivyScala, sbtPlugin }
import sbt.AutoPlugin

private[test] trait MediatorWorkaroundPluginCompat extends AutoPlugin {

  override def projectSettings = Seq(
    ivyScala := { ivyScala.value map { _.copy(overrideScalaVersion = sbtPlugin.value) } }
  )
}
