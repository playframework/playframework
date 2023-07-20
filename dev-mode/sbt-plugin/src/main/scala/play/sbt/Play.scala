/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import sbt._
import sbt.Keys._

import com.typesafe.sbt.jse.SbtJsTask
import com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging
import play.core.PlayVersion
import play.sbt.routes.RoutesCompiler
import play.sbt.PlayImport.PlayKeys
import play.twirl.sbt.SbtTwirl

/**
 * Base plugin for all Play services (web apps or micro-services).
 *
 * Declares common settings for both Java and Scala based Play projects.
 */
object PlayService extends AutoPlugin {
  override def requires = JavaServerAppPackaging
  val autoImport        = PlayImport

  override def globalSettings  = PlaySettings.serviceGlobalSettings
  override def projectSettings = PlaySettings.serviceSettings
}

@deprecated("Use PlayWeb instead for a web project.", "2.7.0")
object Play extends AutoPlugin {
  override def requires        = JavaServerAppPackaging && SbtTwirl && SbtJsTask && RoutesCompiler
  val autoImport               = PlayImport
  override def projectSettings = PlaySettings.defaultSettings
}

/**
 * Base plugin for Play web projects.
 *
 * Declares common settings for both Java and Scala based web projects, as well as sbt-web and assets settings.
 */
object PlayWeb extends AutoPlugin {
  override def requires        = PlayService && SbtTwirl && SbtJsTask && RoutesCompiler
  override def projectSettings = PlaySettings.webSettings
}

/**
 * The main plugin for minimal Play Java projects that do not include Forms.
 *
 * To use this the plugin must be made available to your project
 * via sbt's enablePlugins mechanism e.g.:
 *
 * {{{
 *   lazy val root = project.in(file(".")).enablePlugins(PlayMinimalJava)
 * }}}
 */
object PlayMinimalJava extends AutoPlugin {
  override def requires = PlayWeb
  override def projectSettings = Def.settings(
    PlaySettings.minimalJavaSettings,
    libraryDependencies += PlayImport.javaCore
  )
}

/**
 * The main plugin for Play Java projects.
 *
 * To use this the plugin must be made available to your project
 * via sbt's enablePlugins mechanism e.g.:
 *
 * {{{
 *   lazy val root = project.in(file(".")).enablePlugins(PlayJava)
 * }}}
 */
object PlayJava extends AutoPlugin {
  override def requires = PlayWeb
  override def projectSettings = Def.settings(
    PlaySettings.defaultJavaSettings,
    libraryDependencies += PlayImport.javaForms
  )
}

/**
 * The main plugin for Play Scala projects. To use this the plugin must be made available to your project
 * via sbt's enablePlugins mechanism e.g.:
 * {{{
 *   lazy val root = project.in(file(".")).enablePlugins(PlayScala)
 * }}}
 */
object PlayScala extends AutoPlugin {
  override def requires        = PlayWeb
  override def projectSettings = PlaySettings.defaultScalaSettings
}

/**
 * This plugin enables the Play netty http server
 */
object PlayNettyServer extends AutoPlugin {
  override def requires = PlayService
  override def projectSettings = Seq(
    libraryDependencies ++= (if (PlayKeys.playPlugin.value) Nil else Seq(PlayImport.nettyServer))
  )
}

/**
 * This plugin enables the Play akka http server
 */
object PlayAkkaHttpServer extends AutoPlugin {
  override def requires = PlayService
  override def trigger  = allRequirements

  private val akkaDeps =
    Seq("akka-actor", "akka-actor-typed", "akka-slf4j", "akka-serialization-jackson", "akka-stream")
  private val scala2Deps = Map(
    "com.typesafe.akka"            -> (PlayVersion.akkaVersion, akkaDeps),
    "com.typesafe"                 -> (PlayVersion.sslConfigCoreVersion, Seq("ssl-config-core")),
    "com.fasterxml.jackson.module" -> (PlayVersion.jacksonVersion, Seq("jackson-module-scala"))
  )

  private def usingAkka26Scala3(scalaVersion: String, deps: Seq[ModuleID]): Boolean = {
    scalaVersion == "3" &&
    deps.exists(m => m.organization == "com.typesafe.akka" && m.name == "akka-actor" && m.revision.startsWith("2.6."))
  }

  override def projectSettings = Seq(
    libraryDependencies += PlayImport.akkaHttpServer,
    excludeDependencies ++=
      (if (usingAkka26Scala3(scalaBinaryVersion.value, libraryDependencies.value)) {
         scala2Deps.flatMap(e => e._2._2.map(_ + "_3").map(ExclusionRule(e._1, _))).toSeq
       } else {
         Seq.empty
       }),
    libraryDependencies ++=
      (if (usingAkka26Scala3(scalaBinaryVersion.value, libraryDependencies.value)) {
         scala2Deps.flatMap(e => e._2._2.map(e._1 %% _ % e._2._1).map(_.cross(CrossVersion.for3Use2_13))).toSeq
       } else {
         Seq.empty
       }),
  )

}

object PlayAkkaHttp2Support extends AutoPlugin {
  override def requires = PlayAkkaHttpServer
  override def projectSettings = Seq(
    libraryDependencies += "com.typesafe.play" %% "play-akka-http2-support" % PlayVersion.current,
  )
}
