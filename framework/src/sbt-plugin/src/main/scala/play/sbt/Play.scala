/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import com.typesafe.sbt.jse.SbtJsTask
import com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging
import play.sbt.PlayImport.PlayKeys
import play.sbt.routes.RoutesCompiler
import play.twirl.sbt.SbtTwirl
import sbt.Keys._
import sbt._

/**
 * Base plugin for all Play services (web apps or microservices).
 *
 * Declares common settings for both Java and Scala based Play projects.
 */
object PlayService extends AutoPlugin {

  override def requires = JavaServerAppPackaging

  val autoImport = PlayImport

  override def projectSettings = PlaySettings.serviceSettings
}

@deprecated("Use PlayWeb instead for a web project.", "2.7.0")
object Play extends AutoPlugin {
  override def requires = JavaServerAppPackaging && SbtTwirl && SbtJsTask && RoutesCompiler
  val autoImport = PlayImport
  override def projectSettings = PlaySettings.defaultSettings
}

/**
 * Base plugin for Play web projects.
 *
 * Declares common settings for both Java and Scala based web projects, as well as sbt-web and assets settings.
 */
object PlayWeb extends AutoPlugin {
  override def requires = PlayService && SbtTwirl && SbtJsTask && RoutesCompiler
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
  override def projectSettings =
    PlaySettings.minimalJavaSettings ++
      Seq(libraryDependencies += PlayImport.javaCore)
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
  override def projectSettings =
    PlaySettings.defaultJavaSettings ++
      Seq(libraryDependencies += PlayImport.javaForms)
}

/**
 * The main plugin for Play Scala projects. To use this the plugin must be made available to your project
 * via sbt's enablePlugins mechanism e.g.:
 * {{{
 *   lazy val root = project.in(file(".")).enablePlugins(PlayScala)
 * }}}
 */
object PlayScala extends AutoPlugin {
  override def requires = PlayWeb
  override def projectSettings =
    PlaySettings.defaultScalaSettings
}

/**
 * This plugin enables the Play netty http server
 */
object PlayNettyServer extends AutoPlugin {
  override def requires = PlayService

  override def projectSettings = Seq(
    libraryDependencies ++= {
      if (PlayKeys.playPlugin.value) {
        Nil
      } else {
        Seq(PlayImport.nettyServer)
      }
    }
  )
}

/**
 * This plugin enables the Play akka http server
 */
object PlayAkkaHttpServer extends AutoPlugin {
  override def requires = PlayService
  override def trigger = allRequirements

  override def projectSettings = Seq(
    libraryDependencies += PlayImport.akkaHttpServer
  )
}

object PlayAkkaHttp2Support extends AutoPlugin {
  import com.lightbend.sbt.javaagent.JavaAgent

  override def requires = PlayAkkaHttpServer && JavaAgent

  import JavaAgent.JavaAgentKeys._

  override def projectSettings = Seq(
    libraryDependencies += "com.typesafe.play" %% "play-akka-http2-support" % play.core.PlayVersion.current,
    javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % play.core.PlayVersion.jettyAlpnAgentVersion % "compile;test"
  )
}
