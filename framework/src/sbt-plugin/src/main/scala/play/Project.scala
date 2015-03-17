/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play

import sbt._
import sbt.Keys._

import com.typesafe.play.sbt.enhancer.PlayEnhancer
import com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging
import com.typesafe.sbt.jse.SbtJsTask
import com.typesafe.sbt.webdriver.SbtWebDriver
import com.typesafe.sbteclipse.core.EclipsePlugin.EclipseProjectFlavor
import play.twirl.sbt.SbtTwirl
import play.sbtplugin.PlayPositionMapper
import play.sbtplugin.routes.RoutesCompiler

/**
 * Base plugin for Play projects. Declares common settings for both Java and Scala based Play projects.
 */
object Play
    extends AutoPlugin
    with PlayExceptions
    with PlayCommands
    with PlayRun
    with play.PlaySettings
    with PlayPositionMapper {

  override def requires = SbtTwirl && SbtJsTask && SbtWebDriver && RoutesCompiler && JavaServerAppPackaging

  val autoImport = play.PlayImport

  override def projectSettings =
    defaultSettings ++
      intellijCommandSettings ++
      Seq(
        scalacOptions ++= Seq("-deprecation", "-unchecked", "-encoding", "utf8"),
        javacOptions in Compile ++= Seq("-encoding", "utf8", "-g")
      )
}

/**
 * The main plugin for Play Java projects. To use this the plugin must be made available to your project
 * via sbt's enablePlugins mechanism e.g.:
 * {{{
 *   lazy val root = project.in(file(".")).enablePlugins(PlayJava)
 * }}}
 */
object PlayJava extends AutoPlugin {
  override def requires = Play && PlayEnhancer

  import Play._
  import Play.autoImport._
  override def projectSettings =
    eclipseCommandSettings(EclipseProjectFlavor.Java) ++
      defaultJavaSettings ++
      Seq(libraryDependencies += javaCore)
}

/**
 * The main plugin for Play Scala projects. To use this the plugin must be made available to your project
 * via sbt's enablePlugins mechanism e.g.:
 * {{{
 *   lazy val root = project.in(file(".")).enablePlugins(PlayScala)
 * }}}
 */
object PlayScala extends AutoPlugin {
  override def requires = Play

  import Play._
  override def projectSettings =
    eclipseCommandSettings(EclipseProjectFlavor.Scala) ++
      defaultScalaSettings
}

/**
 * This plugin enables the Play akka http server
 */
object PlayNettyServer extends AutoPlugin {
  override def requires = Play
  override def trigger = allRequirements

  override def projectSettings = Seq(
    libraryDependencies ++= {
      if (play.PlayImport.PlayKeys.playPlugin.value) {
        Nil
      } else {
        Seq("com.typesafe.play" %% "play-netty-server" % play.core.PlayVersion.current)
      }
    },
    mainClass in (Compile, run) := Some("play.core.server.NettyServer")
  )
}

/**
 * This plugin enables the Play akka http server
 */
object PlayAkkaHttpServer extends AutoPlugin {
  override def requires = Play

  override def projectSettings = Seq(
    libraryDependencies += "com.typesafe.play" %% "play-akka-http-server-experimental" % play.core.PlayVersion.current,
    mainClass in (Compile, run) := Some("play.core.server.akkahttp.AkkaHttpServer")
  )
}
