/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */

import BuildSettings._
import Dependencies._
import Generators._
import com.typesafe.tools.mima.plugin.MimaKeys.{mimaPreviousArtifacts, mimaReportBinaryIssues}
import interplay.PlayBuildBase.autoImport._
import sbt.Keys.parallelExecution
import sbt.ScriptedPlugin._
import sbt._

lazy val BuildLinkProject = PlayNonCrossBuiltProject("Build-Link", "build-link")
    .dependsOn(PlayExceptionsProject)

lazy val RunSupportProject = PlayDevelopmentProject("Run-Support", "run-support")
    .settings(libraryDependencies ++= runSupportDependencies(sbtVersion.value, scalaVersion.value))
    .dependsOn(BuildLinkProject)

// extra run-support project that is only compiled against sbt scala version
lazy val SbtRunSupportProject = PlaySbtProject("SBT-Run-Support", "run-support")
    .settings(
      target := target.value / "sbt-run-support",
      libraryDependencies ++= runSupportDependencies(sbtVersion.value, scalaVersion.value)
    ).dependsOn(BuildLinkProject)

lazy val RoutesCompilerProject = PlayDevelopmentProject("Routes-Compiler", "routes-compiler")
    .enablePlugins(SbtTwirl)
    .settings(
      libraryDependencies ++= routesCompilerDependencies(scalaVersion.value),
      TwirlKeys.templateFormats := Map("twirl" -> "play.routes.compiler.ScalaFormat")
    )

lazy val SbtRoutesCompilerProject = PlaySbtProject("SBT-Routes-Compiler", "routes-compiler")
    .enablePlugins(SbtTwirl)
    .settings(
      target := target.value / "sbt-routes-compiler",
      libraryDependencies ++= routesCompilerDependencies(scalaVersion.value),
      TwirlKeys.templateFormats := Map("twirl" -> "play.routes.compiler.ScalaFormat")
    )

lazy val StreamsProject = PlayCrossBuiltProject("Play-Streams", "play-streams")
    .settings(libraryDependencies ++= streamsDependencies)

lazy val PlayExceptionsProject = PlayNonCrossBuiltProject("Play-Exceptions", "play-exceptions")

lazy val PlayNettyUtilsProject = PlayNonCrossBuiltProject("Play-Netty-Utils", "play-netty-utils")
    .settings(
      javacOptions in(Compile, doc) += "-Xdoclint:none",
      libraryDependencies ++= nettyUtilsDependencies
    )

lazy val PlayProject = PlayCrossBuiltProject("Play", "play")
    .enablePlugins(SbtTwirl)
    .settings(
      libraryDependencies ++= runtime(scalaVersion.value) ++ scalacheckDependencies,

      sourceGenerators in Compile += Def.task(PlayVersion(version.value, scalaVersion.value, sbtVersion.value, (sourceManaged in Compile).value)).taskValue,

      sourceDirectories in(Compile, TwirlKeys.compileTemplates) := (unmanagedSourceDirectories in Compile).value,
      TwirlKeys.templateImports += "play.api.templates.PlayMagic._",
      mappings in(Compile, packageSrc) ++= {
        // Add both the templates, useful for end users to read, and the Scala sources that they get compiled to,
        // so omnidoc can compile and produce scaladocs for them.
        val twirlSources = (sources in(Compile, TwirlKeys.compileTemplates)).value pair
            relativeTo((sourceDirectories in(Compile, TwirlKeys.compileTemplates)).value)

        val twirlTarget = (target in(Compile, TwirlKeys.compileTemplates)).value
        // The pair with errorIfNone being false both creates the mappings, and filters non twirl outputs out of
        // managed sources
        val twirlCompiledSources = (managedSources in Compile).value.pair(relativeTo(twirlTarget), errorIfNone = false)

        twirlSources ++ twirlCompiledSources
      },
      Docs.apiDocsIncludeManaged := true
    ).settings(Docs.playdocSettings: _*)
    .dependsOn(
      BuildLinkProject,
      PlayNettyUtilsProject,
      StreamsProject
    )

lazy val PlayServerProject = PlayCrossBuiltProject("Play-Server", "play-server")
    .settings(libraryDependencies ++= playServerDependencies)
    .dependsOn(
      PlayProject,
      PlayGuiceProject % "test"
    )

lazy val PlayNettyServerProject = PlayCrossBuiltProject("Play-Netty-Server", "play-netty-server")
    .settings(libraryDependencies ++= netty)
    .dependsOn(PlayServerProject)

lazy val PlayAkkaHttpServerProject = PlayCrossBuiltProject("Play-Akka-Http-Server", "play-akka-http-server")
    .settings(libraryDependencies ++= akkaHttp)
    // Include scripted tests here as well as in the SBT Plugin, because we
    // don't want the SBT Plugin to have a dependency on an experimental module.
    //.settings(ScriptedPlugin.scriptedSettings ++ playScriptedSettings)
    .dependsOn(PlayServerProject, StreamsProject)
    .dependsOn(PlaySpecs2Project % "test", PlayAhcWsProject % "test")

lazy val PlayJdbcApiProject = PlayCrossBuiltProject("Play-JDBC-Api", "play-jdbc-api")
    .dependsOn(PlayProject)

lazy val PlayJdbcProject: Project = PlayCrossBuiltProject("Play-JDBC", "play-jdbc")
    .settings(libraryDependencies ++= jdbcDeps)
    .dependsOn(PlayJdbcApiProject)
    .dependsOn(PlaySpecs2Project % "test")

lazy val PlayJdbcEvolutionsProject = PlayCrossBuiltProject("Play-JDBC-Evolutions", "play-jdbc-evolutions")
    .settings(libraryDependencies += derbyDatabase % Test)
    .dependsOn(PlayJdbcApiProject)
    .dependsOn(PlaySpecs2Project % "test")
    .dependsOn(PlayJdbcProject % "test->test")
    .dependsOn(PlayJavaJdbcProject % "test")

lazy val PlayJavaJdbcProject = PlayCrossBuiltProject("Play-Java-JDBC", "play-java-jdbc")
    .dependsOn(PlayJdbcProject % "compile->compile;test->test", PlayJavaProject)
    .dependsOn(PlaySpecs2Project % "test", PlayGuiceProject % "test")

lazy val PlayJpaProject = PlayCrossBuiltProject("Play-Java-JPA", "play-java-jpa")
    .settings(libraryDependencies ++= jpaDeps)
    .dependsOn(PlayJavaJdbcProject % "compile->compile;test->test")
    .dependsOn(PlayJdbcEvolutionsProject % "test")
    .dependsOn(PlaySpecs2Project % "test")

lazy val PlayTestProject = PlayCrossBuiltProject("Play-Test", "play-test")
    .settings(
      libraryDependencies ++= testDependencies ++ Seq(h2database % "test"),
      parallelExecution in Test := false
    ).dependsOn(
  PlayGuiceProject,
  PlayNettyServerProject
)

lazy val PlaySpecs2Project = PlayCrossBuiltProject("Play-Specs2", "play-specs2")
    .settings(
      libraryDependencies ++= specsBuild,
      parallelExecution in Test := false
    ).dependsOn(PlayTestProject)

lazy val PlayJavaProject = PlayCrossBuiltProject("Play-Java", "play-java")
    .settings(libraryDependencies ++= javaDeps ++ javaTestDeps)
    .dependsOn(
      PlayProject % "compile;test->test",
      PlayTestProject % "test",
      PlaySpecs2Project % "test",
      PlayGuiceProject % "test"
    )

lazy val PlayJavaFormsProject = PlayCrossBuiltProject("Play-Java-Forms", "play-java-forms")
    .settings(libraryDependencies ++= javaDeps ++ javaFormsDeps ++ javaTestDeps)
    .dependsOn(
      PlayJavaProject % "compile;test->test"
    )

lazy val PlayDocsProject = PlayCrossBuiltProject("Play-Docs", "play-docs")
    .settings(Docs.settings: _*)
    .settings(
      libraryDependencies ++= playDocsDependencies
    ).dependsOn(PlayNettyServerProject)

lazy val PlayGuiceProject = PlayCrossBuiltProject("Play-Guice", "play-guice")
    .settings(libraryDependencies ++= guiceDeps ++ specsBuild.map(_ % "test"))
    .dependsOn(
      PlayProject % "compile;test->test"
    )

lazy val SbtPluginProject = PlaySbtPluginProject("SBT-Plugin", "sbt-plugin")
    .settings(
      libraryDependencies ++= sbtDependencies(sbtVersion.value, scalaVersion.value),
      sourceGenerators in Compile += Def.task(PlayVersion(version.value, (scalaVersion in PlayProject).value, sbtVersion.value, (sourceManaged in Compile).value)).taskValue,

      // This only publishes the sbt plugin projects on each scripted run.
      // The runtests script does a full publish before running tests.
      // When developing the sbt plugins, run a publishLocal in the root project first.
      scriptedDependencies := {
        val () = publishLocal.value
        val () = (publishLocal in RoutesCompilerProject).value
      }
    ).dependsOn(SbtRoutesCompilerProject, SbtRunSupportProject)

lazy val PlayLogback = PlayCrossBuiltProject("Play-Logback", "play-logback")
    .settings(
      libraryDependencies += logback,
      parallelExecution in Test := false,
      // quieten deprecation warnings in tests
      scalacOptions in Test := (scalacOptions in Test).value diff Seq("-deprecation")
    ).dependsOn(PlayProject)

lazy val PlayWsProject = PlayCrossBuiltProject("Play-WS", "play-ws")
    .settings(
      libraryDependencies ++= playWsDeps,
      parallelExecution in Test := false,
      // quieten deprecation warnings in tests
      scalacOptions in Test := (scalacOptions in Test).value diff Seq("-deprecation")
  ).dependsOn(PlayProject)
  .dependsOn(PlayTestProject % "test")

lazy val PlayAhcWsProject = PlayCrossBuiltProject("Play-AHC-WS", "play-ahc-ws")
  .settings(
    libraryDependencies ++= playAhcWsDeps,
    parallelExecution in Test := false,
    // quieten deprecation warnings in tests
    scalacOptions in Test := (scalacOptions in Test).value diff Seq("-deprecation")
  ).dependsOn(PlayWsProject, PlayJavaProject)
  .dependsOn(PlaySpecs2Project % "test")
  .dependsOn(PlayTestProject % "compile->compile; test->test")

lazy val PlayOpenIdProject = PlayCrossBuiltProject("Play-OpenID", "play-openid")
  .settings(
    parallelExecution in Test := false,
    // quieten deprecation warnings in tests
    scalacOptions in Test := (scalacOptions in Test).value diff Seq("-deprecation")
  ).dependsOn(PlayAhcWsProject)
  .dependsOn(PlaySpecs2Project % "test")

lazy val PlayFiltersHelpersProject = PlayCrossBuiltProject("Filters-Helpers", "play-filters-helpers")
    .settings(
      parallelExecution in Test := false
    ).dependsOn(PlayProject, PlayJavaProject % "test", PlaySpecs2Project % "test", PlayAhcWsProject % "test")

// This project is just for testing Play, not really a public artifact
lazy val PlayIntegrationTestProject = PlayCrossBuiltProject("Play-Integration-Test", "play-integration-test")
    .settings(
      libraryDependencies += h2database % Test,
      parallelExecution in Test := false,
      mimaPreviousArtifacts := Set.empty
    )
    .dependsOn(PlayProject % "test->test", PlayLogback % "test->test", PlayAhcWsProject % "test->test", PlaySpecs2Project)
    .dependsOn(PlayFiltersHelpersProject)
    .dependsOn(PlayJavaProject)
    .dependsOn(PlayJavaFormsProject)
    .dependsOn(PlayAkkaHttpServerProject)

// This project is just for microbenchmarking Play, not really a public artifact
lazy val PlayMicrobenchmarkProject = PlayCrossBuiltProject("Play-Microbenchmark", "play-microbenchmark")
    .enablePlugins(JmhPlugin)
    .settings(
      parallelExecution in Test := false,
      mimaPreviousArtifacts := Set.empty
    )
    .dependsOn(PlayProject % "test->test", PlayLogback % "test->test", PlayAhcWsProject, PlaySpecs2Project)
    .dependsOn(PlayFiltersHelpersProject)
    .dependsOn(PlayJavaProject)
    .dependsOn(PlayAkkaHttpServerProject)

lazy val PlayCacheProject = PlayCrossBuiltProject("Play-Cache", "play-cache")
    .settings(
      libraryDependencies ++= playCacheDeps,
      parallelExecution in Test := false
    ).dependsOn(PlayProject)
    .dependsOn(PlaySpecs2Project % "test")

lazy val PlayDocsSbtPlugin = PlaySbtPluginProject("Play-Docs-SBT-Plugin", "play-docs-sbt-plugin")
    .enablePlugins(SbtTwirl)
    .settings(
      libraryDependencies ++= playDocsSbtPluginDependencies
    ).dependsOn(SbtPluginProject)

lazy val publishedProjects = Seq[ProjectReference](
  PlayProject,
  PlayGuiceProject,
  BuildLinkProject,
  RoutesCompilerProject,
  SbtRoutesCompilerProject,
  PlayAkkaHttpServerProject,
  PlayCacheProject,
  PlayJdbcApiProject,
  PlayJdbcProject,
  PlayJdbcEvolutionsProject,
  PlayJavaProject,
  PlayJavaFormsProject,
  PlayJavaJdbcProject,
  PlayJpaProject,
  PlayNettyUtilsProject,
  PlayNettyServerProject,
  PlayServerProject,
  PlayLogback,
  PlayWsProject,
  PlayAhcWsProject,
  PlayOpenIdProject,
  SbtRunSupportProject,
  RunSupportProject,
  SbtPluginProject,
  PlaySpecs2Project,
  PlayTestProject,
  PlayExceptionsProject,
  PlayDocsProject,
  PlayFiltersHelpersProject,
  PlayIntegrationTestProject,
  PlayMicrobenchmarkProject,
  PlayDocsSbtPlugin,
  StreamsProject
)

lazy val PlayFramework = Project("Play-Framework", file("."))
    .enablePlugins(PlayRootProject)
    .enablePlugins(CrossPerProjectPlugin)
    .settings(playCommonSettings: _*)
    .settings(
      scalaVersion := (scalaVersion in PlayProject).value,
      playBuildRepoName in ThisBuild := "playframework",
      concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
      libraryDependencies ++= (runtime(scalaVersion.value) ++ jdbcDeps),
      Docs.apiDocsInclude := false,
      Docs.apiDocsIncludeManaged := false,
      mimaReportBinaryIssues := (),
      commands += Commands.quickPublish
    ).settings(Release.settings: _*)
    .aggregate(publishedProjects: _*)
