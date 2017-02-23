/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.sbt

import sbt._
import sbt.Keys._

import com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging
import com.typesafe.sbt.jse.SbtJsTask
import play.twirl.sbt.SbtTwirl

import play.sbt.routes.RoutesCompiler
import play.sbt.PlayImport.PlayKeys

/**
 * Base plugin for Play projects. Declares common settings for both Java and Scala based Play projects.
 */
object Play extends AutoPlugin {

  override def requires = SbtTwirl && SbtJsTask && RoutesCompiler && JavaServerAppPackaging

  val autoImport = PlayImport

  override def projectSettings =
    PlaySettings.defaultSettings ++
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
  override def requires = Play
  override def projectSettings =
    PlaySettings.defaultJavaSettings ++
      Seq(libraryDependencies += PlayImport.javaCore)
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
  override def projectSettings =
    PlaySettings.defaultScalaSettings
}

/**
 * This plugin enables the Play netty http server
 */
object PlayNettyServer extends AutoPlugin {
  override def requires = Play
  override def trigger = allRequirements

  override def projectSettings = Seq(
    libraryDependencies ++= {
      if (PlayKeys.playPlugin.value) {
        Nil
      } else {
        Seq("com.typesafe.play" %% "play-netty-server" % play.core.PlayVersion.current)
      }
    }
  )
}

/**
 * This plugin enables the Play akka http server
 */
object PlayAkkaHttpServer extends AutoPlugin {
  override def requires = Play

  override def projectSettings = Seq(
    libraryDependencies += "com.typesafe.play" %% "play-akka-http-server-experimental" % play.core.PlayVersion.current
  )
}
