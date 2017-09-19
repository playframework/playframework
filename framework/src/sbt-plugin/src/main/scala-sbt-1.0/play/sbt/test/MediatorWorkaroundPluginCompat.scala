/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.sbt.test

import sbt.Keys.{ scalaModuleInfo, sbtPlugin }
import sbt.AutoPlugin

trait MediatorWorkaroundPluginCompat extends AutoPlugin {

  override def projectSettings = Seq(
    scalaModuleInfo := { scalaModuleInfo.value map {_.copy(overrideScalaVersion = sbtPlugin.value)} }
  )
}
