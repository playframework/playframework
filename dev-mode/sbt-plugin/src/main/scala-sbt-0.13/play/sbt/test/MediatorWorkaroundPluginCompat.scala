/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt.test

import sbt.Keys.ivyScala
import sbt.Keys.sbtPlugin
import sbt.AutoPlugin

private[test] trait MediatorWorkaroundPluginCompat extends AutoPlugin {

  override def projectSettings = Seq(
    ivyScala := { ivyScala.value.map { _.copy(overrideScalaVersion = sbtPlugin.value) } }
  )
}
