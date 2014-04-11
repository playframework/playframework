/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play

import sbt._
import sbt.Keys._

import com.typesafe.sbt.SbtNativePackager.packageArchetype
import com.typesafe.sbt.jse.SbtJsTask
import com.typesafe.sbt.webdriver.SbtWebDriver

/**
 * Base plugin for Play projects. Declares common settings for both Java and Scala based Play projects.
 */
object Play
  extends AutoPlugin
  with PlayExceptions
  with PlayReloader
  with PlayCommands
  with PlayRun
  with play.PlaySettings
  with PlayPositionMapper
  with PlaySourceGenerators {

  override def requires = SbtJsTask && SbtWebDriver

  val autoImport = play.PlayImport


  override def projectSettings =
    packageArchetype.java_server ++
      defaultSettings ++
      intellijCommandSettings ++
      Seq(testListeners += testListener) ++
      Seq(
        scalacOptions ++= Seq("-deprecation", "-unchecked", "-encoding", "utf8"),
        javacOptions in Compile ++= Seq("-encoding", "utf8", "-g")
      )
}

/**
 * The main plugin for Play Java projects. To use this the plugin must be made available to your project
 * via sbt's addPlugins mechanism e.g.:
 * {{{
 *   lazy val root = project.in(file(".")).addPlugins(PlayJava)
 * }}}
 */
object PlayJava extends AutoPlugin {
  override def requires = Play

  import Play._
  import Play.autoImport._
  override def projectSettings =
    eclipseCommandSettings(JAVA) ++
    defaultJavaSettings ++
    Seq(libraryDependencies += javaCore)
}

/**
 * The main plugin for Play Scala projects. To use this the plugin must be made available to your project
 * via sbt's addPlugins mechanism e.g.:
 * {{{
 *   lazy val root = project.in(file(".")).addPlugins(PlayScala)
 * }}}
 */
object PlayScala extends AutoPlugin {
  override def requires = Play

  import Play._
  override def projectSettings =
    eclipseCommandSettings(SCALA) ++
    defaultScalaSettings
}