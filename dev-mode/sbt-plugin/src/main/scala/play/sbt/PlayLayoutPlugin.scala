/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import sbt._
import sbt.Keys._
import play.twirl.sbt.Import.TwirlKeys
import com.typesafe.sbt.web.SbtWeb.autoImport._
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

    resourceDirectory in Compile := baseDirectory.value / "conf",

    scalaSource in Compile := baseDirectory.value / "app",
    scalaSource in Test := baseDirectory.value / "test",

    javaSource in Compile := baseDirectory.value / "app",
    javaSource in Test := baseDirectory.value / "test",

    sourceDirectories in (Compile, TwirlKeys.compileTemplates) := Seq((sourceDirectory in Compile).value),
    sourceDirectories in (Test, TwirlKeys.compileTemplates) := Seq((sourceDirectory in Test).value),

    // sbt-web
    sourceDirectory in Assets := (sourceDirectory in Compile).value / "assets",
    sourceDirectory in TestAssets := (sourceDirectory in Test).value / "assets",
    resourceDirectory in Assets := baseDirectory.value / "public",

    // Native packager
    sourceDirectory in Universal := baseDirectory.value / "dist"
  )

}
