/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
import BuildSettings._
import Dependencies._
import Generators._
import interplay.PlayBuildBase.autoImport._
import sbt.Keys.parallelExecution
import sbt._
import sbt.io.Path._
import org.scalafmt.sbt.ScalafmtPlugin

lazy val RoutesCompilerProject = PlayDevelopmentProject("Routes-Compiler", "dev-mode/routes-compiler")
  .enablePlugins(SbtTwirl)
  .settings(
    libraryDependencies ++= routesCompilerDependencies(scalaVersion.value),
    TwirlKeys.templateFormats := Map("twirl" -> "play.routes.compiler.ScalaFormat")
  )

lazy val SbtRoutesCompilerProject = PlaySbtProject("Sbt-Routes-Compiler", "dev-mode/routes-compiler")
  .enablePlugins(SbtTwirl)
  .settings(
    target := target.value / "sbt-routes-compiler",
    libraryDependencies ++= routesCompilerDependencies(scalaVersion.value),
    TwirlKeys.templateFormats := Map("twirl" -> "play.routes.compiler.ScalaFormat")
  )

lazy val StreamsProject = PlayCrossBuiltProject("Play-Streams", "core/play-streams")
  .settings(libraryDependencies ++= streamsDependencies)

lazy val PlayExceptionsProject = PlayNonCrossBuiltProject("Play-Exceptions", "core/play-exceptions")

lazy val PlayJodaFormsProject = PlayCrossBuiltProject("Play-Joda-Forms", "web/play-joda-forms")
  .settings(
    libraryDependencies ++= joda
  )
  .dependsOn(PlayProject)

lazy val PlayProject = PlayCrossBuiltProject("Play", "core/play")
  .enablePlugins(SbtTwirl)
  .settings(
    libraryDependencies ++= runtime(scalaVersion.value) ++ scalacheckDependencies ++ cookieEncodingDependencies :+
      jimfs % Test,
    (Compile / sourceGenerators) += Def
      .task(
        PlayVersion(
          version.value,
          scalaVersion.value,
          sbtVersion.value,
          Dependencies.akkaVersion,
          Dependencies.akkaHttpVersion,
          (Compile / sourceManaged).value
        )
      )
      .taskValue
  )
  .dependsOn(PlayExceptionsProject, StreamsProject)

lazy val PlayServerProject = PlayCrossBuiltProject("Play-Server", "transport/server/play-server")
  .settings(libraryDependencies ++= playServerDependencies)
  .dependsOn(
    PlayProject,
    PlayGuiceProject % "test"
  )

lazy val PlayNettyServerProject = PlayCrossBuiltProject("Play-Netty-Server", "transport/server/play-netty-server")
  .settings(libraryDependencies ++= netty)
  .dependsOn(PlayServerProject)

lazy val PlayAkkaHttpServerProject =
  PlayCrossBuiltProject("Play-Akka-Http-Server", "transport/server/play-akka-http-server")
    .dependsOn(PlayServerProject, StreamsProject)
    .dependsOn(PlayGuiceProject % "test")
    .settings(
      libraryDependencies ++= specs2Deps.map(_ % "test"),
      libraryDependencies += akkaHttp
    )


lazy val PlayGuiceProject = PlayCrossBuiltProject("Play-Guice", "core/play-guice")
  .settings(libraryDependencies ++= guiceDeps ++ specs2Deps.map(_ % "test"))
  .dependsOn(
    PlayProject % "compile;test->test"
  )

lazy val SbtPluginProject = PlaySbtPluginProject("Sbt-Plugin", "dev-mode/sbt-plugin")
  .enablePlugins(SbtPlugin)
  .settings(
    libraryDependencies ++= sbtDependencies((pluginCrossBuild / sbtVersion).value, scalaVersion.value),
    (Compile / sourceGenerators) += Def.task {
      PlayVersion(
        version.value,
        (PlayProject / scalaVersion).value,
        sbtVersion.value,
        Dependencies.akkaVersion,
        Dependencies.akkaHttpVersion,
        (Compile / sourceManaged).value
      )
    }.taskValue
  )
  .dependsOn(SbtRoutesCompilerProject, PlayExceptionsProject)

lazy val PlayLogback = PlayCrossBuiltProject("Play-Logback", "core/play-logback")
  .settings(
    libraryDependencies += logback,
    (Test / parallelExecution) := false,
    // quieten deprecation warnings in tests
    (Test / scalacOptions) := (Test / scalacOptions).value.diff(Seq("-deprecation"))
  )
  .dependsOn(PlayProject)

// These projects are aggregate by the root project and every
// task (compile, test, publish, etc) executed for the root
// project will also be executed for them:
// https://www.scala-sbt.org/1.x/docs/Multi-Project.html#Aggregation
//
// Keep in mind that specific configurations (like skip in publish) will be respected.
lazy val userProjects = Seq[ProjectReference](
  PlayProject,
  PlayGuiceProject,
  RoutesCompilerProject,
  PlayAkkaHttpServerProject,
  PlayJodaFormsProject,
  PlayNettyServerProject,
  PlayServerProject,
  PlayLogback,
  PlayExceptionsProject,
  StreamsProject
)
lazy val nonUserProjects = Seq[ProjectReference](
  SbtRoutesCompilerProject,
  SbtPluginProject,
)

lazy val PlayFramework = Project("Play-Framework", file("."))
  .enablePlugins(PlayRootProject)
  .settings(
    playCommonSettings,
    scalaVersion := (PlayProject / scalaVersion).value,
    (ThisBuild / playBuildRepoName) := "playframework",
    (Global / concurrentRestrictions) += Tags.limit(Tags.Test, 1),
    libraryDependencies ++= runtime(scalaVersion.value),
    mimaReportBinaryIssues := (()),
    commands += Commands.quickPublish,
    Release.settings
  )
  .aggregate((userProjects ++ nonUserProjects): _*)
