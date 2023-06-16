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
 * This plugin enables the Play pekko http server
 */
object PlayPekkoHttpServer extends AutoPlugin {
  override def requires = PlayService
  override def trigger  = allRequirements

  private val pekkoDeps =
    Seq("pekko-actor", "pekko-actor-typed", "pekko-slf4j", "pekko-serialization-jackson", "pekko-stream")
  private val scala2Deps = Map(
    "org.apache.pekko"             -> (PlayVersion.pekkoVersion, pekkoDeps),
    "com.typesafe"                 -> (PlayVersion.sslConfigCoreVersion, Seq("ssl-config-core")),
    "com.fasterxml.jackson.module" -> (PlayVersion.jacksonVersion, Seq("jackson-module-scala"))
  )

  override def projectSettings = Def.settings(
    libraryDependencies += PlayImport.pekkoHttpServer,
    PlaySettings.pekkoHttpSettings,
    excludeDependencies ++=
      (if (scalaBinaryVersion.value == "3") {
         if (PlayKeys.pekkoHttpScala3Artifacts.value) {
           // The user upgraded to pekko-http 10.5+, which provides Scala 3 artifacts. We need to exclude the pekko-http Scala 2.13 artifacts that Play
           // depends on per default (since pekko-http 10.2.x did not yet have Scala 3 artifacts), see Dependencies.scala (".cross(CrossVersion.for3Use2_13)")
           Seq(ExclusionRule("org.apache.pekko", "pekko-http-core_2.13"))
         } else {
           // The user project switched to Scala 3, but did not upgrade pekko-http beyond 10.2.x, which does not ship Scala 3 artifacts,
           // therefore we need to make some dependencies keep using Scala 2 artifacts to make them work well with the pekko-http 10.2.x Scala 2 artifacts
           scala2Deps.flatMap(e => e._2._2.map(_ + "_3").map(ExclusionRule(e._1, _))).toSeq
         }
       } else {
         Seq.empty
       }),
    libraryDependencies ++=
      (if (scalaBinaryVersion.value == "3" && !PlayKeys.pekkoHttpScala3Artifacts.value) {
         // see comment above
         scala2Deps.flatMap(e => e._2._2.map(e._1 %% _ % e._2._1).map(_.cross(CrossVersion.for3Use2_13))).toSeq
       } else {
         Seq.empty
       }),
    onLoadMessage := onLoadMessage.value + (if (
                                              scalaBinaryVersion.value == "3" && !PlayKeys.akkaHttpScala3Artifacts.value
                                            ) {
                                              s"""
                                                 |${Colors.blue(Colors.bold("Using Scala 3 and the Akka HTTP server backend, but without native Scala 3 artifacts:"))}
                                                 |https://www.playframework.com/documentation/latest/Scala3Migration#Using-Scala-3-with-Akka-HTTP-10.5-or-newer
                                                 |
                                                 |""".stripMargin
                                            } else {
                                              ""
                                            }),
  )

}

object PlayPekkoHttp2Support extends AutoPlugin {
  override def requires = PlayPekkoHttpServer
  override def projectSettings = Def.settings(
    libraryDependencies += "com.typesafe.play" %% "play-pekko-http2-support" % PlayVersion.current,
    excludeDependencies ++=
      (if (scalaBinaryVersion.value == "3" && PlayKeys.pekkoHttpScala3Artifacts.value) {
         // The user upgraded to pekko-http 10.5+, which provides Scala 3 artifacts. We need to exclude the pekko-http Scala 2.13 artifacts that Play
         // depends on per default (since pekko-http 10.2.x did not yet have Scala 3 artifacts), see Dependencies.scala (".cross(CrossVersion.for3Use2_13)")
         Seq(ExclusionRule("org.apache.pekko", "pekko-http2-support_2.13"))
       } else {
         Seq.empty
       }),
  )
}
