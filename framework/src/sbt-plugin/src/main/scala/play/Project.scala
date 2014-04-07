/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play

import sbt.{ Project => _, _ }
import sbt.Keys._

import com.typesafe.sbt.SbtNativePackager.packageArchetype
import com.typesafe.sbt.web.SbtWeb
import com.typesafe.sbt.jse.{SbtJsEngine, SbtJsTask}
import com.typesafe.sbt.webdriver.SbtWebDriver

object Project extends PlayExceptions with play.Keys with PlayReloader with PlayCommands
    with PlayRun with play.Settings with PlayPositionMapper with PlaySourceGenerators {

  private lazy val sbtWebSettings =
    SbtWeb.projectSettings ++
      SbtJsEngine.projectSettings ++
      SbtJsTask.projectSettings ++
      SbtWebDriver.projectSettings

  private lazy val commonSettings: Seq[Setting[_]] =
    packageArchetype.java_server ++
      defaultSettings ++
      intellijCommandSettings ++
      Seq(testListeners += testListener) ++
      Seq(
        scalacOptions ++= Seq("-deprecation", "-unchecked", "-encoding", "utf8"),
        javacOptions in Compile ++= Seq("-encoding", "utf8", "-g")
      )

  private[play] lazy val javaSettings: Seq[Setting[_]] =
    commonSettings ++
      eclipseCommandSettings(JAVA) ++
      defaultJavaSettings ++
      Seq(libraryDependencies += javaCore)

  @deprecated("Use the PlayJava plugin instead i.e. project...addPlugins(PlayJava).", "2.3")
  lazy val playJavaSettings: Seq[Setting[_]] = sbtWebSettings ++ javaSettings

  private[play] lazy val scalaSettings: Seq[Setting[_]] =
    commonSettings ++
      eclipseCommandSettings(SCALA) ++
      defaultScalaSettings

  @deprecated("Use the PlayScala plugin instead i.e. project...addPlugins(PlayScala).", "2.3")
  lazy val playScalaSettings: Seq[Setting[_]] = sbtWebSettings ++ scalaSettings

  @deprecated("Use the PlayJava or PlayScala plugins instead e.g. project...addPlugins(PlayJava).", "2.3")
  def apply(name: String, applicationVersion: String = "1.0", dependencies: Seq[ModuleID] = Nil, path: File = file("."), settings: => Seq[Setting[_]] = Seq()): sbt.Project = {
    lazy val playSettings = if (dependencies.contains(javaCore)) playJavaSettings else playScalaSettings

    lazy val projectSettings: Seq[Setting[_]] = Seq(
      version := applicationVersion,
      libraryDependencies ++= dependencies
    )

    sbt.Project(name, path)
      .settings(playSettings: _*)
      .settings(projectSettings: _*)
      .settings(settings: _*)
  }
}

/**
 * Declares the default imports for Play plugins.
 */
object PlayImport {

  import play.Keys

  object PlayKeys extends Keys
}

/**
 * Base plugin for Play projects. Declares common settings for both Java and Scala based Play projects.
 */
object Play
  extends AutoPlugin
  with PlayExceptions
  with PlayReloader
  with PlayCommands
  with PlayRun
  with play.Settings
  with PlayPositionMapper
  with PlaySourceGenerators {

  override def requires = SbtJsTask && SbtWebDriver
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

  override def projectSettings = Project.javaSettings

  val autoImport = PlayImport
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

  override def projectSettings = Project.scalaSettings

  val autoImport = PlayImport
}