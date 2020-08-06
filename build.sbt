/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
import BuildSettings._
import Dependencies._
import Generators._
import com.lightbend.sbt.javaagent.JavaAgent.JavaAgentKeys.javaAgents
import com.lightbend.sbt.javaagent.JavaAgent.JavaAgentKeys.resolvedJavaAgents
import interplay.PlayBuildBase.autoImport._
import sbt.Keys.parallelExecution
import sbt._
import sbt.io.Path._
import org.scalafmt.sbt.ScalafmtPlugin

lazy val BuildLinkProject = PlayNonCrossBuiltProject("Build-Link", "dev-mode/build-link")
  .dependsOn(PlayExceptionsProject)

// run-support project is only compiled against sbt scala version
lazy val RunSupportProject = PlaySbtProject("Run-Support", "dev-mode/run-support")
  .settings(
    target := target.value / "run-support",
    libraryDependencies ++= runSupportDependencies((sbtVersion in pluginCrossBuild).value)
  )
  .dependsOn(BuildLinkProject)

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
    unmanagedSourceDirectories in Compile ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v >= 13 => (sourceDirectory in Compile).value / s"java-scala-2.13+" :: Nil
        case Some((2, v)) if v <= 12 => (sourceDirectory in Compile).value / s"java-scala-2.13-" :: Nil
        case _                       => Nil
      }
    },
    sourceGenerators in Compile += Def
      .task(
        PlayVersion(
          version.value,
          scalaVersion.value,
          sbtVersion.value,
          jettyAlpnAgent.revision,
          Dependencies.akkaVersion,
          Dependencies.akkaHttpVersion,
          (sourceManaged in Compile).value
        )
      )
      .taskValue,
    sourceDirectories in (Compile, TwirlKeys.compileTemplates) := (unmanagedSourceDirectories in Compile).value,
    TwirlKeys.templateImports += "play.api.templates.PlayMagic._",
    mappings in (Compile, packageSrc) ++= {
      // Add both the templates, useful for end users to read, and the Scala sources that they get compiled to,
      // so omnidoc can compile and produce scaladocs for them.
      val twirlSources = (sources in (Compile, TwirlKeys.compileTemplates)).value
        .pair(relativeTo((sourceDirectories in (Compile, TwirlKeys.compileTemplates)).value))

      val twirlTarget = (target in (Compile, TwirlKeys.compileTemplates)).value
      // The pair with errorIfNone being false both creates the mappings, and filters non twirl outputs out of
      // managed sources
      val twirlCompiledSources = (managedSources in Compile).value.pair(relativeTo(twirlTarget), errorIfNone = false)

      twirlSources ++ twirlCompiledSources
    },
  )
  .dependsOn(StreamsProject)

lazy val PlayServerProject = PlayCrossBuiltProject("Play-Server", "transport/server/play-server")
  .settings(libraryDependencies ++= playServerDependencies)
  .dependsOn(
    PlayProject,
    PlayGuiceProject % "test"
  )

lazy val PlayNettyServerProject = PlayCrossBuiltProject("Play-Netty-Server", "transport/server/play-netty-server")
  .settings(libraryDependencies ++= netty)
  .dependsOn(PlayServerProject)

import AkkaDependency._
lazy val PlayAkkaHttpServerProject =
  PlayCrossBuiltProject("Play-Akka-Http-Server", "transport/server/play-akka-http-server")
    .dependsOn(PlayServerProject, StreamsProject)
    .dependsOn(PlayGuiceProject % "test")
    .settings(
      libraryDependencies ++= specs2Deps.map(_ % "test")
    )
    .addAkkaModuleDependency("akka-http-core")

lazy val PlayAkkaHttp2SupportProject =
  PlayCrossBuiltProject("Play-Akka-Http2-Support", "transport/server/play-akka-http2-support")
    .dependsOn(PlayAkkaHttpServerProject)
    .addAkkaModuleDependency("akka-http2-support")

lazy val PlayGuiceProject = PlayCrossBuiltProject("Play-Guice", "core/play-guice")
  .settings(libraryDependencies ++= guiceDeps ++ specs2Deps.map(_ % "test"))
  .dependsOn(
    PlayProject % "compile;test->test"
  )

lazy val SbtPluginProject = PlaySbtPluginProject("Sbt-Plugin", "dev-mode/sbt-plugin")
  .enablePlugins(SbtPlugin)
  .settings(
    libraryDependencies ++= sbtDependencies((sbtVersion in pluginCrossBuild).value, scalaVersion.value),
    sourceGenerators in Compile += Def.task {
      PlayVersion(
        version.value,
        (scalaVersion in PlayProject).value,
        sbtVersion.value,
        jettyAlpnAgent.revision,
        Dependencies.akkaVersion,
        Dependencies.akkaHttpVersion,
        (sourceManaged in Compile).value
      )
    }.taskValue,
    headerSources in Compile ++= (sbtTestDirectory.value ** ("*.scala" || "*.java")).get,
  )
  .dependsOn(SbtRoutesCompilerProject)

lazy val PlayLogback = PlayCrossBuiltProject("Play-Logback", "core/play-logback")
  .settings(
    libraryDependencies += logback,
    parallelExecution in Test := false,
    // quieten deprecation warnings in tests
    scalacOptions in Test := (scalacOptions in Test).value.diff(Seq("-deprecation"))
  )
  .dependsOn(PlayProject)

lazy val PlayWsProject = PlayCrossBuiltProject("Play-WS", "transport/client/play-ws")
  .settings(
    libraryDependencies ++= playWsDeps,
    parallelExecution in Test := false,
    // quieten deprecation warnings in tests
    scalacOptions in Test := (scalacOptions in Test).value.diff(Seq("-deprecation"))
  )
  .dependsOn(PlayProject)

lazy val PlayAhcWsProject = PlayCrossBuiltProject("Play-AHC-WS", "transport/client/play-ahc-ws")
  .settings(
    libraryDependencies ++= playAhcWsDeps,
    parallelExecution in Test := false,
    // quieten deprecation warnings in tests
    scalacOptions in Test := (scalacOptions in Test).value.diff(Seq("-deprecation"))
  )
  .dependsOn(PlayWsProject)
  .dependsOn(PlayAkkaHttpServerProject % "test") // Because we need a server provider when running the tests

lazy val PlayFiltersHelpersProject = PlayCrossBuiltProject("Filters-Helpers", "web/play-filters-helpers")
  .settings(
    libraryDependencies ++= playFilterDeps,
    parallelExecution in Test := false
  )
  .dependsOn(
    PlayProject,
    PlayAhcWsProject          % "test",
    PlayAkkaHttpServerProject % "test" // Because we need a server provider when running the tests
  )

// These projects are aggregate by the root project and every
// task (compile, test, publish, etc) executed for the root
// project will also be executed for them:
// https://www.scala-sbt.org/1.x/docs/Multi-Project.html#Aggregation
//
// Keep in mind that specific configurations (like skip in publish) will be respected.
lazy val aggregatedProjects = Seq[ProjectReference](
  PlayProject,
  PlayGuiceProject,
  RoutesCompilerProject,
  SbtRoutesCompilerProject,
  PlayAkkaHttpServerProject,
  PlayAkkaHttp2SupportProject,
  PlayJodaFormsProject,
  PlayNettyServerProject,
  PlayServerProject,
  PlayLogback,
  PlayWsProject,
  PlayAhcWsProject,
  SbtPluginProject,
  PlayExceptionsProject,
  PlayFiltersHelpersProject,
  StreamsProject,
)

lazy val PlayFramework = Project("Play-Framework", file("."))
  .enablePlugins(PlayRootProject)
  .enablePlugins(PlayWhitesourcePlugin)
  .settings(
    playCommonSettings,
    scalaVersion := (scalaVersion in PlayProject).value,
    playBuildRepoName in ThisBuild := "playframework",
    concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
    libraryDependencies ++= runtime(scalaVersion.value),
    mimaReportBinaryIssues := (()),
    commands += Commands.quickPublish,
    Release.settings
  )
  .aggregate(aggregatedProjects: _*)
