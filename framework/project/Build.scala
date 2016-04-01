/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt.ScriptedPlugin._
import sbt._
import Keys._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.{
  previousArtifacts, binaryIssueFilters, reportBinaryIssues
}
import com.typesafe.tools.mima.core._

import com.typesafe.sbt.SbtScalariform.scalariformSettings

import play.twirl.sbt.SbtTwirl
import play.twirl.sbt.Import.TwirlKeys

import sbtdoge.CrossPerProjectPlugin

import bintray.BintrayPlugin.autoImport._

import interplay._
import interplay.Omnidoc.autoImport._
import interplay.PlayBuildBase.autoImport._

import scala.util.control.NonFatal

object BuildSettings {
  // Binary compatibility is tested against this version
  val previousVersion = "2.5.0"

  // Argument for setting size of permgen space or meta space for all forked processes
  val maxMetaspace = s"-XX:MaxMetaspaceSize=384m"

  val snapshotBranch = {
    try {
      val branch = "git rev-parse --abbrev-ref HEAD".!!.trim
      if (branch == "HEAD") {
        // not on a branch, get the hash
        "git rev-parse HEAD".!!.trim
      } else branch
    } catch {
      case NonFatal(_) => "unknown"
    }
  }

  /**
   * These settings are used by all projects
   */
  def playCommonSettings: Seq[Setting[_]] = scalariformSettings ++ Seq(
    homepage := Some(url("https://playframework.com")),
    ivyLoggingLevel := UpdateLogging.DownloadOnly,
    resolvers ++= Seq(
      "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases",
      Resolver.typesafeRepo("releases"),
      Resolver.typesafeIvyRepo("releases")
    ),
    fork in Test := true,
    parallelExecution in Test := false,
    testListeners in (Test,test) := Nil,
    javaOptions in Test ++= Seq(maxMetaspace, "-Xmx512m", "-Xms128m"),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v"),
    bintrayPackage := "play-sbt-plugin",
    autoAPIMappings := true,
    apiMappings += scalaInstance.value.libraryJar -> url( raw"""http://scala-lang.org/files/archive/api/${scalaInstance.value.actualVersion}/index.html""")
  )

  /**
   * These settings are used by all projects that are part of the runtime, as opposed to development, mode of Play.
   */
  def playRuntimeSettings: Seq[Setting[_]] = playCommonSettings ++ mimaDefaultSettings ++ Seq(
    previousArtifacts := {
      if (crossPaths.value) {
        Set(organization.value % s"${moduleName.value}_${scalaBinaryVersion.value}" % previousVersion)
      } else {
        Set(organization.value % moduleName.value % previousVersion)
      }
    },
    Docs.apiDocsInclude := true
  )

  def javaVersionSettings(version: String): Seq[Setting[_]] = Seq(
    javacOptions ++= Seq("-source", version, "-target", version),
    javacOptions in doc := Seq("-source", version)
  )

  /**
   * A project that is shared between the SBT runtime and the Play runtime
   */
  def PlayNonCrossBuiltProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .enablePlugins(PlaySbtLibrary)
      .settings(playRuntimeSettings: _*)
      .settings(omnidocSettings: _*)
      .settings(
        autoScalaLibrary := false,
        crossPaths := false
      )
  }

  /**
   * A project that is only used when running in development.
   */
  def PlayDevelopmentProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .enablePlugins(PlayLibrary)
      .settings(playCommonSettings: _*)
      .settings(
        (javacOptions in compile) ~= (_.map {
          case "1.8" => "1.6"
          case other => other
        })
      )
  }

  /**
   * A project that is in the Play runtime
   */
  def PlayCrossBuiltProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .enablePlugins(PlayLibrary)
      .settings(playRuntimeSettings: _*)
      .settings(omnidocSettings: _*)
  }

  def omnidocSettings: Seq[Setting[_]] = Omnidoc.projectSettings ++ Seq(
    omnidocSnapshotBranch := snapshotBranch,
    omnidocPathPrefix := "framework/"
  )

  def playScriptedSettings: Seq[Setting[_]] = Seq(
    ScriptedPlugin.scripted <<= ScriptedPlugin.scripted.tag(Tags.Test),
    scriptedLaunchOpts ++= Seq(
      "-Xmx768m",
      maxMetaspace,
      "-Dscala.version=" + sys.props.get("scripted.scala.version").getOrElse((scalaVersion in PlayBuild.PlayProject).value)
    )
  )

  def playFullScriptedSettings: Seq[Setting[_]] = ScriptedPlugin.scriptedSettings ++ Seq(
    ScriptedPlugin.scriptedLaunchOpts <+= version apply { v => s"-Dproject.version=$v" }
  ) ++ playScriptedSettings

  /**
   * A project that runs in the SBT runtime
   */
  def PlaySbtProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .enablePlugins(PlaySbtLibrary)
      .settings(playCommonSettings: _*)
  }

  /**
   * A project that *is* an SBT plugin
   */
  def PlaySbtPluginProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .enablePlugins(PlaySbtPlugin)
      .settings(playCommonSettings: _*)
      .settings(playScriptedSettings: _*)
  }

}

object PlayBuild extends Build {

  import Dependencies._
  import BuildSettings._
  import Generators._

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

  lazy val IterateesProject = PlayCrossBuiltProject("Play-Iteratees", "iteratees")
    .settings(libraryDependencies ++= iterateesDependencies)

  lazy val StreamsProject = PlayCrossBuiltProject("Play-Streams", "play-streams")
    .settings(
      libraryDependencies ++= streamsDependencies,
      binaryIssueFilters := Seq(
        ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.libs.streams.SinkAccumulator.this")
      )
    )
    .dependsOn(IterateesProject)

  lazy val FunctionalProject = PlayCrossBuiltProject("Play-Functional", "play-functional")

  lazy val DataCommonsProject = PlayCrossBuiltProject("Play-DataCommons", "play-datacommons")

  lazy val JsonProject = PlayCrossBuiltProject("Play-Json", "play-json")
    .settings(libraryDependencies ++= jsonDependencies(scalaVersion.value))
    .dependsOn(IterateesProject, FunctionalProject, DataCommonsProject)

  lazy val PlayExceptionsProject = PlayNonCrossBuiltProject("Play-Exceptions", "play-exceptions")

  lazy val PlayNettyUtilsProject = PlayNonCrossBuiltProject("Play-Netty-Utils", "play-netty-utils")
    .settings(
      javacOptions in (Compile,doc) += "-Xdoclint:none",
      libraryDependencies ++= nettyUtilsDependencies
    )

  lazy val PlayProject = PlayCrossBuiltProject("Play", "play")
    .enablePlugins(SbtTwirl)
    .settings(
      libraryDependencies ++= runtime(scalaVersion.value) ++ scalacheckDependencies,

      sourceGenerators in Compile <+= (version, scalaVersion, sbtVersion, sourceManaged in Compile) map PlayVersion,

      sourceDirectories in (Compile, TwirlKeys.compileTemplates) := (unmanagedSourceDirectories in Compile).value,
      TwirlKeys.templateImports += "play.api.templates.PlayMagic._",
      mappings in (Compile, packageSrc) ++= {
        // Add both the templates, useful for end users to read, and the Scala sources that they get compiled to,
        // so omnidoc can compile and produce scaladocs for them.
        val twirlSources = (sources in (Compile, TwirlKeys.compileTemplates)).value pair
          relativeTo((sourceDirectories in (Compile, TwirlKeys.compileTemplates)).value)

        val twirlTarget = (target in (Compile, TwirlKeys.compileTemplates)).value
        // The pair with errorIfNone being false both creates the mappings, and filters non twirl outputs out of
        // managed sources
        val twirlCompiledSources = (managedSources in Compile).value.pair(relativeTo(twirlTarget), errorIfNone = false)

        twirlSources ++ twirlCompiledSources
      },
      Docs.apiDocsIncludeManaged := true,
      binaryIssueFilters := Seq(
        ProblemFilters.exclude[MissingMethodProblem]("play.core.parsers.Multipart.partParser"),
        ProblemFilters.exclude[MissingMethodProblem]("play.api.BuiltInComponents.crypto"),
        ProblemFilters.exclude[MissingMethodProblem]("play.api.BuiltInComponents.aesCrypter")
      )
    ).settings(Docs.playdocSettings: _*)
     .dependsOn(
      BuildLinkProject,
      IterateesProject % "test->test;compile->compile",
      JsonProject,
      PlayNettyUtilsProject,
      StreamsProject
    )

  lazy val PlayServerProject = PlayCrossBuiltProject("Play-Server", "play-server")
    .settings(libraryDependencies ++= playServerDependencies)
    .dependsOn(
      PlayProject,
      IterateesProject % "test->test;compile->compile"
    )

  lazy val PlayNettyServerProject = PlayCrossBuiltProject("Play-Netty-Server", "play-netty-server")
    .settings(libraryDependencies ++= netty)
    .dependsOn(PlayServerProject)

  lazy val PlayAkkaHttpServerProject = PlayCrossBuiltProject("Play-Akka-Http-Server-Experimental", "play-akka-http-server")
    .settings(libraryDependencies ++= akkaHttp)
     // Include scripted tests here as well as in the SBT Plugin, because we
     // don't want the SBT Plugin to have a dependency on an experimental module.
    .settings(playFullScriptedSettings: _*)
    .dependsOn(PlayServerProject, StreamsProject)
    .dependsOn(PlaySpecs2Project % "test", PlayWsProject % "test")

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
    .dependsOn(PlayJdbcProject % "test")
    .dependsOn(PlayJavaJdbcProject % "test")

  lazy val PlayJavaJdbcProject = PlayCrossBuiltProject("Play-Java-JDBC", "play-java-jdbc")
    .settings(libraryDependencies ++= javaJdbcDeps)
    .dependsOn(PlayJdbcProject, PlayJavaProject)
    .dependsOn(PlaySpecs2Project % "test")

  lazy val PlayJpaProject = PlayCrossBuiltProject("Play-Java-JPA", "play-java-jpa")
    .settings(libraryDependencies ++= jpaDeps)
    .dependsOn(PlayJavaJdbcProject)
    .dependsOn(PlayJdbcEvolutionsProject % "test")
    .dependsOn(PlaySpecs2Project % "test")

  lazy val PlayTestProject = PlayCrossBuiltProject("Play-Test", "play-test")
    .settings(
      libraryDependencies ++= testDependencies,
      parallelExecution in Test := false
    ).dependsOn(PlayNettyServerProject)

  lazy val PlaySpecs2Project = PlayCrossBuiltProject("Play-Specs2", "play-specs2")
    .settings(
      libraryDependencies ++= specsBuild,
      parallelExecution in Test := false
    ).dependsOn(PlayTestProject)

  lazy val PlayJavaProject = PlayCrossBuiltProject("Play-Java", "play-java")
    .settings(libraryDependencies ++= javaDeps ++ javaTestDeps)
    .dependsOn(PlayProject % "compile;test->test")
    .dependsOn(PlayTestProject % "test")
    .dependsOn(PlaySpecs2Project % "test")

  lazy val PlayDocsProject = PlayCrossBuiltProject("Play-Docs", "play-docs")
    .settings(Docs.settings: _*)
    .settings(
      libraryDependencies ++= playDocsDependencies
    ).dependsOn(PlayNettyServerProject)

  lazy val SbtPluginProject = PlaySbtPluginProject("SBT-Plugin", "sbt-plugin")
    .settings(
      libraryDependencies ++= sbtDependencies(sbtVersion.value, scalaVersion.value),
      sourceGenerators in Compile <+= (version, scalaVersion in PlayProject, sbtVersion, sourceManaged in Compile) map PlayVersion,
      // This only publishes the sbt plugin projects on each scripted run.
      // The runtests script does a full publish before running tests.
      // When developing the sbt plugins, run a publishLocal in the root project first.
      scriptedDependencies := {
        val () = publishLocal.value
        val () = (publishLocal in RoutesCompilerProject).value
      }
    ).dependsOn(SbtRoutesCompilerProject, SbtRunSupportProject)

  lazy val ForkRunProtocolProject = PlayDevelopmentProject("Fork-Run-Protocol", "fork-run-protocol")
    .settings(
      libraryDependencies ++= forkRunProtocolDependencies(scalaBinaryVersion.value)
    ).dependsOn(RunSupportProject)

  // extra fork-run-protocol project that is only compiled against sbt scala version
  lazy val SbtForkRunProtocolProject = PlaySbtProject("SBT-Fork-Run-Protocol", "fork-run-protocol")
    .settings(
      target := target.value / "sbt-fork-run-protocol",
      libraryDependencies ++= forkRunProtocolDependencies(scalaBinaryVersion.value)
    ).dependsOn(SbtRunSupportProject)

  lazy val ForkRunProject = PlayDevelopmentProject("Fork-Run", "fork-run")
    .settings(
      libraryDependencies ++= forkRunDependencies(scalaBinaryVersion.value),
      // Needed to get the jnotify dependency
      resolvers += Classpaths.sbtPluginReleases
    )
    .dependsOn(ForkRunProtocolProject)

  lazy val SbtForkRunPluginProject = PlaySbtPluginProject("SBT-Fork-Run-Plugin", "sbt-fork-run-plugin")
    .settings(
      libraryDependencies ++= sbtForkRunPluginDependencies(sbtVersion.value, scalaVersion.value),
      // This only publishes the sbt plugin projects on each scripted run.
      // The runtests script does a full publish before running tests.
      // When developing the sbt plugins, run a publishLocal in the root project first.
      scriptedDependencies := {
        val () = publishLocal.value
        val () = (publishLocal in SbtPluginProject).value
        val () = (publishLocal in SbtRoutesCompilerProject).value
      })
    .dependsOn(SbtForkRunProtocolProject, SbtPluginProject)

  lazy val PlayLogback = PlayCrossBuiltProject("Play-Logback", "play-logback")
    .settings(
      libraryDependencies ++= logback,
      parallelExecution in Test := false,
      // quieten deprecation warnings in tests
      scalacOptions in Test := (scalacOptions in Test).value diff Seq("-deprecation")
    ).dependsOn(PlayProject)

  lazy val PlayWsProject = PlayCrossBuiltProject("Play-WS", "play-ws")
    .settings(
      libraryDependencies ++= playWsDeps,
      parallelExecution in Test := false,
      // quieten deprecation warnings in tests
      scalacOptions in Test := (scalacOptions in Test).value diff Seq("-deprecation"),
      binaryIssueFilters := Seq(
        ProblemFilters.exclude[MissingMethodProblem]("play.api.libs.ws.WSRequest.put"),
        ProblemFilters.exclude[MissingMethodProblem]("play.api.libs.ws.WSRequest.post"),
        ProblemFilters.exclude[MissingMethodProblem]("play.api.libs.ws.WSRequest.patch"),
        ProblemFilters.exclude[MissingMethodProblem]("play.api.libs.ws.WSRequest.withMultipartBody")
      )
    ).dependsOn(PlayProject)
    .dependsOn(PlaySpecs2Project % "test")

  lazy val PlayWsJavaProject = PlayCrossBuiltProject("Play-Java-WS", "play-java-ws")
      .settings(
        libraryDependencies ++= playWsDeps,
        parallelExecution in Test := false,
        binaryIssueFilters := Seq(
          ProblemFilters.exclude[MissingMethodProblem]("play.libs.ws.WSRequest.put"),
          ProblemFilters.exclude[MissingMethodProblem]("play.libs.ws.WSRequest.post"),
          ProblemFilters.exclude[MissingMethodProblem]("play.libs.ws.WSRequest.patch"),
          ProblemFilters.exclude[MissingMethodProblem]("play.libs.ws.WSRequest.withMultipartBody")
        )
      ).dependsOn(PlayProject)
    .dependsOn(PlayWsProject % "test->test;compile->compile", PlayJavaProject)

  lazy val PlayFiltersHelpersProject = PlayCrossBuiltProject("Filters-Helpers", "play-filters-helpers")
    .settings(
      parallelExecution in Test := false,
      binaryIssueFilters := Seq(
        // We needed to change this since the method names did not line up with BuiltInComponents
        ProblemFilters.exclude[MissingMethodProblem]("play.filters.csrf.CSRFComponents.tokenSigner"),
        ProblemFilters.exclude[MissingMethodProblem]("play.filters.csrf.CSRFComponents.csrfTokenSigner")
      )
    ).dependsOn(PlayProject, PlayJavaProject, PlaySpecs2Project % "test", PlayWsProject % "test")

  // This project is just for testing Play, not really a public artifact
  lazy val PlayIntegrationTestProject = PlayCrossBuiltProject("Play-Integration-Test", "play-integration-test")
    .settings(
      parallelExecution in Test := false,
      previousArtifacts := Set.empty
    )
    .dependsOn(PlayProject % "test->test", PlayLogback % "test->test", PlayWsProject, PlayWsJavaProject, PlaySpecs2Project)
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
    BuildLinkProject,
    IterateesProject,
    FunctionalProject,
    DataCommonsProject,
    JsonProject,
    RoutesCompilerProject,
    SbtRoutesCompilerProject,
    PlayAkkaHttpServerProject,
    PlayCacheProject,
    PlayJdbcApiProject,
    PlayJdbcProject,
    PlayJdbcEvolutionsProject,
    PlayJavaProject,
    PlayJavaJdbcProject,
    PlayJpaProject,
    PlayNettyUtilsProject,
    PlayNettyServerProject,
    PlayServerProject,
    PlayLogback,
    PlayWsProject,
    PlayWsJavaProject,
    SbtRunSupportProject,
    RunSupportProject,
    SbtPluginProject,
    ForkRunProtocolProject,
    SbtForkRunProtocolProject,
    ForkRunProject,
    SbtForkRunPluginProject,
    PlaySpecs2Project,
    PlayTestProject,
    PlayExceptionsProject,
    PlayDocsProject,
    PlayFiltersHelpersProject,
    PlayIntegrationTestProject,
    PlayDocsSbtPlugin,
    StreamsProject
  )

  lazy val PlayFramework = Project(
    "Play-Framework",
    file("."))
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
      reportBinaryIssues := (),
      commands += Commands.quickPublish
    ).settings(Release.settings: _*)
    .aggregate(publishedProjects: _*)

}
