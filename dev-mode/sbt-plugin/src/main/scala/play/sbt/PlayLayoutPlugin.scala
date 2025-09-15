/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import sbt.*
import sbt.Keys.*

import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.*
import com.typesafe.sbt.web.SbtWeb.autoImport.*
import play.twirl.sbt.Import.TwirlKeys

/**
 * Play layout plugin to switch to the traditional Play web app layout instead of the standard Maven layout.
 *
 * This is enabled automatically with the PlayWeb plugin (as an AutoPlugin) but not with the PlayService plugin.
 */
object PlayLayoutPlugin extends AutoPlugin {
  override def requires = PlayWeb

  override def trigger = allRequirements

  override def projectSettings = Seq(
    target                                                   := baseDirectory.value / "target",
    Compile / sourceDirectory                                := baseDirectory.value / "app",
    Test / sourceDirectory                                   := baseDirectory.value / "test",
    Compile / resourceDirectory                              := baseDirectory.value / "conf",
    Compile / scalaSource                                    := baseDirectory.value / "app",
    Test / scalaSource                                       := baseDirectory.value / "test",
    Compile / javaSource                                     := baseDirectory.value / "app",
    Test / javaSource                                        := baseDirectory.value / "test",
    Compile / TwirlKeys.compileTemplates / sourceDirectories := Seq((Compile / sourceDirectory).value),
    Test / TwirlKeys.compileTemplates / sourceDirectories    := Seq((Test / sourceDirectory).value),
    // sbt-web
    Assets / sourceDirectory     := (Compile / sourceDirectory).value / "assets",
    TestAssets / sourceDirectory := (Test / sourceDirectory).value / "assets",
    Assets / resourceDirectory   := baseDirectory.value / "public",
    // Native packager
    Universal / sourceDirectory := baseDirectory.value / "dist"
  )
}
