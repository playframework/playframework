/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

import sbt.ScriptedPlugin._
import sbt._
import Keys._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.{
  previousArtifact, binaryIssueFilters, reportBinaryIssues
}
import com.typesafe.tools.mima.core._

import com.typesafe.sbt.SbtScalariform.scalariformSettings

import play.twirl.sbt.SbtTwirl
import play.twirl.sbt.Import.TwirlKeys

import interplay.Omnidoc
import interplay.Omnidoc.Import.OmnidocKeys
import sbtdoge.CrossPerProjectPlugin

import scala.util.control.NonFatal

object BuildSettings {

  // Binary compatibility is tested against this version
  val previousVersion = "2.4.0"

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

    organization := "com.typesafe.play",
    homepage := Some(url("https://playframework.com")),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),

    ivyLoggingLevel := UpdateLogging.DownloadOnly,
    resolvers ++= ResolverSettings.playResolvers,
    resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases", // specs2 depends on scalaz-stream

    javacOptions ++= Seq("-encoding", "UTF-8", "-Xlint:-options", "-J-Xmx512m"),

    scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature"),

    fork in Test := true,
    parallelExecution in Test := false,
    testListeners in (Test,test) := Nil,
    javaOptions in Test += maxMetaspace,
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")
  )

  /**
   * These settings are used by all projects that are part of the runtime, as opposed to development, mode of Play.
   */
  def playRuntimeSettings: Seq[Setting[_]] = playCommonSettings ++ mimaDefaultSettings ++ Seq(
    previousArtifact := {
      if (crossPaths.value) {
        Some(organization.value % s"${moduleName.value}_${scalaBinaryVersion.value}" % previousVersion)
      } else {
        Some(organization.value % moduleName.value % previousVersion)
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
      .settings(playRuntimeSettings: _*)
      .settings(PublishSettings.publishSettings: _*)
      .settings(omnidocSettings: _*)
      .settings(javaVersionSettings("1.6"): _*)
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
      .settings(playCommonSettings: _*)
      .settings(PublishSettings.publishSettings: _*)
      .settings(crossBuildSettings: _*)
      .settings(javaVersionSettings("1.6"): _*)
  }

  /**
   * A project that is in the Play runtime
   */
  def PlayCrossBuiltProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .settings(playRuntimeSettings: _*)
      .settings(PublishSettings.publishSettings: _*)
      .settings(crossBuildSettings: _*)
      .settings(omnidocSettings: _*)
      .settings(javaVersionSettings("1.8"): _*)
  }

  def crossBuildSettings: Seq[Setting[_]] = Seq(
    crossScalaVersions := Seq("2.10.4", "2.11.5"),
    scalaVersion := "2.10.4"
  )

  def omnidocSettings: Seq[Setting[_]] = Omnidoc.projectSettings ++ Seq(
    OmnidocKeys.githubRepo := "playframework/playframework",
    OmnidocKeys.snapshotBranch := snapshotBranch,
    OmnidocKeys.tagPrefix := "",
    OmnidocKeys.pathPrefix := "framework/"
  )

  def playSbtCommonSettings: Seq[Setting[_]] = playCommonSettings ++ scalariformSettings ++ Seq(
    scalaVersion := "2.10.4",
    sbtVersion in GlobalScope := "0.13.5"
  )

  def playScriptedSettings = ScriptedPlugin.scriptedSettings ++ Seq(
    ScriptedPlugin.scripted <<= ScriptedPlugin.scripted.tag(Tags.Test),
    scriptedLaunchOpts ++= Seq(
      "-Xmx768m",
      maxMetaspace,
      "-Dproject.version=" + version.value,
      "-Dscala.version=" + sys.props.get("scripted.scala.version").getOrElse((scalaVersion in PlayBuild.PlayProject).value)
    )
  )

  /**
   * A project that runs in the SBT runtime
   */
  def PlaySbtProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .settings(playSbtCommonSettings: _*)
      .settings(PublishSettings.publishSettings: _*)
  }

  /**
   * A project that *is* an SBT plugin
   */
  def PlaySbtPluginProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .settings(playSbtCommonSettings: _*)
      .settings(PublishSettings.sbtPluginPublishSettings: _*)
      .settings(playScriptedSettings: _*)
      .settings(
        sbtPlugin := true
      )
  }

  /**
   * Adds a set of Scala 2.11 modules to the build.
   *
   * Only adds if Scala version is >= 2.11.
   */
  def addScalaModules(modules: ModuleID*): Setting[_] = {
    libraryDependencies := {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, scalaMajor)) if scalaMajor >= 11 =>
          libraryDependencies.value ++ modules
        case _ =>
          libraryDependencies.value
      }
    }
  }

  val scalaParserCombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1"
}

object PlayBuild extends Build {

  import Dependencies._
  import BuildSettings._
  import Generators._
  import Tasks._

  lazy val BuildLinkProject = PlayNonCrossBuiltProject("Build-Link", "build-link")
    .settings(libraryDependencies ++= link)
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

  lazy val RoutesCompilerProject = PlaySbtProject("Routes-Compiler", "routes-compiler")
    .enablePlugins(SbtTwirl)
    .settings(
      libraryDependencies ++= routesCompilerDependencies,
      TwirlKeys.templateFormats := Map("twirl" -> "play.routes.compiler.ScalaFormat")
    )

  lazy val IterateesProject = PlayCrossBuiltProject("Play-Iteratees", "iteratees")
    .settings(libraryDependencies ++= iterateesDependencies)

  lazy val StreamsProject = PlayCrossBuiltProject("Play-Streams-Experimental", "play-streams")
    .settings(libraryDependencies ++= streamsDependencies)
    .dependsOn(IterateesProject)

  lazy val FunctionalProject = PlayCrossBuiltProject("Play-Functional", "play-functional")

  lazy val DataCommonsProject = PlayCrossBuiltProject("Play-DataCommons", "play-datacommons")

  lazy val JsonProject = PlayCrossBuiltProject("Play-Json", "play-json")
    .settings(libraryDependencies ++= jsonDependencies(scalaVersion.value))
    .dependsOn(IterateesProject, FunctionalProject, DataCommonsProject)

  lazy val PlayExceptionsProject = PlayNonCrossBuiltProject("Play-Exceptions", "play-exceptions")

  lazy val PlayNettyUtilsProject = PlayNonCrossBuiltProject("Play-Netty-Utils", "play-netty-utils")
    .settings(javacOptions in (Compile,doc) += "-Xdoclint:none")

  lazy val PlayProject = PlayCrossBuiltProject("Play", "play")
    .enablePlugins(SbtTwirl)
    .settings(
      addScalaModules(scalaParserCombinators),
      libraryDependencies ++= runtime(scalaVersion.value) ++ scalacheckDependencies,

      sourceGenerators in Compile <+= (version, scalaVersion, sbtVersion, sourceManaged in Compile) map PlayVersion,

      sourceDirectories in (Compile, TwirlKeys.compileTemplates) := (unmanagedSourceDirectories in Compile).value,
      TwirlKeys.templateImports += "play.api.templates.PlayMagic._",
      mappings in (Compile, packageSrc) <++= scalaTemplateSourceMappings,
      Docs.apiDocsIncludeManaged := true
    ).settings(Docs.playdocSettings: _*)
     .dependsOn(
      BuildLinkProject,
      IterateesProject % "test->test;compile->compile",
      JsonProject,
      PlayNettyUtilsProject)

  lazy val PlayServerProject = PlayCrossBuiltProject("Play-Server", "play-server")
    .settings(libraryDependencies ++= playServerDependencies)
    .dependsOn(
      PlayProject,
      IterateesProject % "test->test;compile->compile"
    )

  lazy val PlayNettyServerProject = PlayCrossBuiltProject("Play-Netty-Server", "play-netty-server")
    .settings(libraryDependencies ++= netty)
    .dependsOn(PlayServerProject)

  import ScriptedPlugin._

  lazy val PlayAkkaHttpServerProject = PlayCrossBuiltProject("Play-Akka-Http-Server-Experimental", "play-akka-http-server")
    .settings(libraryDependencies ++= akkaHttp)
     // Include scripted tests here as well as in the SBT Plugin, because we
     // don't want the SBT Plugin to have a dependency on an experimental module.
    .settings(playScriptedSettings: _*)
    .dependsOn(PlayServerProject, StreamsProject)
    .dependsOn(PlaySpecs2Project % "test", PlayWsProject % "test")

  lazy val PlayJdbcProject = PlayCrossBuiltProject("Play-JDBC", "play-jdbc")
    .settings(libraryDependencies ++= jdbcDeps)
    .dependsOn(PlayProject).dependsOn(PlaySpecs2Project % "test")

  lazy val PlayJavaJdbcProject = PlayCrossBuiltProject("Play-Java-JDBC", "play-java-jdbc").settings(libraryDependencies ++= javaJdbcDeps)
    .dependsOn(PlayJdbcProject, PlayJavaProject)
    .dependsOn(PlaySpecs2Project % "test")

  lazy val PlayJpaProject = PlayCrossBuiltProject("Play-Java-JPA", "play-java-jpa")
    .settings(libraryDependencies ++= jpaDeps)
    .dependsOn(PlayJavaJdbcProject % "compile;test->test")

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
    ).dependsOn(RoutesCompilerProject, SbtRunSupportProject)

  val ProtocolCompile = Tags.Tag("protocol-compile")

  lazy val ForkRunProtocolProject = PlayDevelopmentProject("Fork-Run-Protocol", "fork-run-protocol")
    .settings(
      libraryDependencies ++= forkRunProtocolDependencies(scalaBinaryVersion.value),
      compile in Compile <<= (compile in Compile) tag ProtocolCompile,
      doc in Compile <<= (doc in Compile) tag ProtocolCompile)
    .dependsOn(RunSupportProject)

  // extra fork-run-protocol project that is only compiled against sbt scala version
  lazy val SbtForkRunProtocolProject = PlaySbtProject("SBT-Fork-Run-Protocol", "fork-run-protocol")
    .settings(
      target := target.value / "sbt-fork-run-protocol",
      libraryDependencies ++= forkRunProtocolDependencies(scalaBinaryVersion.value),
      compile in Compile <<= (compile in Compile) tag ProtocolCompile,
      doc in Compile <<= (doc in Compile) tag ProtocolCompile)
    .dependsOn(SbtRunSupportProject)

  lazy val ForkRunProject = PlayDevelopmentProject("Fork-Run", "fork-run")
    .settings(libraryDependencies ++= forkRunDependencies(scalaBinaryVersion.value))
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
        val () = (publishLocal in RoutesCompilerProject).value
      })
    .dependsOn(SbtForkRunProtocolProject, SbtPluginProject)

  lazy val PlayWsProject = PlayCrossBuiltProject("Play-WS", "play-ws")
    .settings(
      libraryDependencies ++= playWsDeps,
      parallelExecution in Test := false,
      // quieten deprecation warnings in tests
      scalacOptions in Test := (scalacOptions in Test).value diff Seq("-deprecation")
    ).dependsOn(PlayProject)
    .dependsOn(PlaySpecs2Project % "test")

  lazy val PlayWsJavaProject = PlayCrossBuiltProject("Play-Java-WS", "play-java-ws")
      .settings(
        libraryDependencies ++= playWsDeps,
        parallelExecution in Test := false
      ).dependsOn(PlayProject)
    .dependsOn(PlayWsProject % "test->test;compile->compile", PlayJavaProject)

  lazy val PlayFiltersHelpersProject = PlayCrossBuiltProject("Filters-Helpers", "play-filters-helpers")
    .settings(
      parallelExecution in Test := false
    ).dependsOn(PlayProject, PlaySpecs2Project % "test", PlayJavaProject % "test", PlayWsProject % "test")

  // This project is just for testing Play, not really a public artifact
  lazy val PlayIntegrationTestProject = PlayCrossBuiltProject("Play-Integration-Test", "play-integration-test")
    .settings(
      parallelExecution in Test := false,
      previousArtifact := None
    )
    .dependsOn(PlayProject % "test->test", PlayWsProject, PlayWsJavaProject, PlaySpecs2Project)
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
    PlayAkkaHttpServerProject,
    PlayCacheProject,
    PlayJdbcProject,
    PlayJavaProject,
    PlayJavaJdbcProject,
    PlayJpaProject,
    PlayNettyUtilsProject,
    PlayNettyServerProject,
    PlayServerProject,
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
    .settings(playCommonSettings: _*)
    .settings(PublishSettings.dontPublishSettings: _*)
    .settings(
      concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
      concurrentRestrictions in Global += Tags.limit(ProtocolCompile, 1),
      libraryDependencies ++= (runtime(scalaVersion.value) ++ jdbcDeps),
      Docs.apiDocsInclude := false,
      Docs.apiDocsIncludeManaged := false,
      reportBinaryIssues := (),
      commands += Commands.quickPublish
    ).settings(Release.settings: _*)
    .aggregate(publishedProjects: _*)
    .enablePlugins(CrossPerProjectPlugin)
}
