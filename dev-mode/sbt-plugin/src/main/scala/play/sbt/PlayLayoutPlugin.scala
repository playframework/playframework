/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import sbt._
import sbt.Keys._
import play.twirl.sbt.Import.TwirlKeys
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._

/**
 * Play layout plugin to switch to the traditional Play web app layout instead of the standard Maven layout.
 *
 * This is enabled automatically with the PlayWeb plugin (as an AutoPlugin) but not with the PlayService plugin.
 */
object PlayLayoutPlugin extends AutoPlugin {
  override def requires = PlayWeb

  override def trigger = allRequirements

  override def projectSettings = Seq(
    target := baseDirectory.value / "target",
    sourceDirectory in Compile := baseDirectory.value / "app",
    sourceDirectory in Test := baseDirectory.value / "test",
    scalaSource in Compile := baseDirectory.value / "app",
    scalaSource in Test := baseDirectory.value / "test",
    javaSource in Compile := baseDirectory.value / "app",
    javaSource in Test := baseDirectory.value / "test",
    // Native packager
    sourceDirectory in Universal := baseDirectory.value / "dist"
  )
}
