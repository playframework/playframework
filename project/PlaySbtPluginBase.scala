/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt.*
import sbt.plugins.SbtPlugin
import sbt.Keys.*
import sbt.ScriptedPlugin.autoImport.*

/**
 * Base Plugin for Play sbt plugins.
 *
 * - Publishes the plugin to sonatype
 * - Adds scripted configuration.
 */
object PlaySbtPluginBase extends AutoPlugin {

  override def trigger  = noTrigger
  override def requires = PlaySonatypeBase && PlayBuildBase && PlaySbtBuildBase && SbtPlugin

  override def projectSettings = Seq(
    scriptedLaunchOpts += version.apply { v => s"-Dproject.version=$v" }.value
  )
}
