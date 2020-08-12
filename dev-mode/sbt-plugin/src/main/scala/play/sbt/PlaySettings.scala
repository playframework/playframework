/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import scala.collection.JavaConverters._

import sbt._
import sbt.Keys._
import sbt.Path._
import sbt.internal.inc.Analysis

import play.core.PlayVersion
import play.dev.filewatch.FileWatchService
import play.sbt.PlayImport.PlayKeys._
import play.sbt.PlayInternalKeys._
import play.sbt.routes.RoutesKeys
import play.sbt.routes.RoutesCompiler.autoImport._
import play.sbt.run.toLoggerProxy
import play.twirl.sbt.Import.TwirlKeys._

import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.Keys._

object PlaySettings {

  lazy val defaultScalaSettings = Seq[Setting[_]]()

  lazy val serviceGlobalSettings: Seq[Setting[_]] = Seq()

  // Settings for a Play service (not a web project)
  lazy val serviceSettings: Seq[Setting[_]] = Def.settings(
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-encoding", "utf8"),
    javacOptions in Compile ++= Seq("-encoding", "utf8", "-g"),
    playPlugin := false,
    javacOptions in (Compile, doc) := List("-encoding", "utf8"),
    libraryDependencies += {
      if (playPlugin.value)
        "com.typesafe.play" %% "play" % PlayVersion.current % "provided"
      else
        "com.typesafe.play" %% "play-server" % PlayVersion.current
    },
    fork in Test := true,
    shellPrompt := PlayCommands.playPrompt,
    // all dependencies from outside the project (all dependency jars)
    playDependencyClasspath := (externalDependencyClasspath in Runtime).value,
    playCommonClassloader := PlayCommands.playCommonClassloaderTask.value,
    playCompileEverything := PlayCommands.playCompileEverythingTask.value.asInstanceOf[Seq[Analysis]],
    ivyLoggingLevel := UpdateLogging.DownloadOnly,
    playDefaultPort := 9000,
    playDefaultAddress := "0.0.0.0",
    // Settings
    devSettings := Nil,
    // Native packaging
    mainClass in Compile := Some("play.core.server.ProdServerStart"),
    // Adds the Play application directory to the command line args passed to Play
    bashScriptExtraDefines += "addJava \"-Duser.dir=$(realpath \"$(cd \"${app_home}/..\"; pwd -P)\"  $(is_cygwin && echo \"fix\"))\"\n",
    // by default, compile any routes files in the root named "routes" or "*.routes"
    sources in (Compile, RoutesKeys.routes) ++= {
      val dirs = (unmanagedResourceDirectories in Compile).value
      (dirs * "routes").get ++ (dirs * "*.routes").get
    }
  )

  lazy val webSettings = Seq[Setting[_]](
    routesImport ++= Seq("controllers.Assets.Asset")
  )
}
