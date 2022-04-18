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
    Compile / sourceDirectory := baseDirectory.value / "app",
    Test / sourceDirectory := baseDirectory.value / "test",
    Compile / scalaSource := baseDirectory.value / "app",
    Test / scalaSource := baseDirectory.value / "test",
    Compile / javaSource := baseDirectory.value / "app",
    Test / javaSource := baseDirectory.value / "test",
    // Native packager
    Universal / sourceDirectory := baseDirectory.value / "dist"
  )
}
