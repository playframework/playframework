/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt._
import sbt.plugins.SbtPlugin
import sbt.Keys._
import sbt.ScriptedPlugin.autoImport._

/**
 * Base Plugin for Play sbt plugins.
 *
 * - Adds scripted configuration.
 */
object PlaySbtPluginBase extends AutoPlugin {

  override def trigger  = noTrigger
  override def requires = PlayBuildBase && PlaySbtBuildBase && SbtPlugin

  override def projectSettings = Seq(
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" =>
          SbtVersions.sbt1
        case _ =>
          SbtVersions.sbt2
      }
    },
    scriptedLaunchOpts += version.apply { v => s"-Dproject.version=$v" }.value
  )
}
