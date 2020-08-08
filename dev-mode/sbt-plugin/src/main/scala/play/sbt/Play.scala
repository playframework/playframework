/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import sbt._
import sbt.Keys._
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

/**
 * Base plugin for Play web projects.
 *
 * Declares common settings for both Java and Scala based web projects, as well as sbt-web and assets settings.
 */
object PlayWeb extends AutoPlugin {
  override def requires        = PlayService && SbtTwirl && RoutesCompiler
  override def projectSettings = PlaySettings.webSettings
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
  override def requires        = PlayService
  override def trigger         = allRequirements
  override def projectSettings = Seq(libraryDependencies += PlayImport.akkaHttpServer)
}
