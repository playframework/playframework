/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt.test

import sbt.Keys.{ scalaModuleInfo, sbtPlugin }
import sbt.AutoPlugin

private[test] trait MediatorWorkaroundPluginCompat extends AutoPlugin {

  override def projectSettings = Seq(
    scalaModuleInfo := { scalaModuleInfo.value map { _.withOverrideScalaVersion(sbtPlugin.value) } }
  )
}
