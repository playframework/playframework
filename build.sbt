// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

import sbt._
import sbt.io.Path._
import sbt.Keys.parallelExecution

import pl.project13.scala.sbt.JmhPlugin.generateJmhSourcesAndResources
import BuildSettings._
import Dependencies._
import Generators._
import VersionHelper._

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
(ThisBuild / dynverVTagPrefix) := false

// Sanity-check: assert that version comes from a tag (e.g. not a too-shallow clone)
// https://github.com/dwijnand/sbt-dynver/#sanity-checking-the-version
Global / onLoad := (Global / onLoad).value.andThen { s =>
  dynverAssertTagVersion.value
  s
}

// Makes sure to increase the version and remove the distance
ThisBuild / version := dynverGitDescribeOutput.value
  .mkVersion(out => versionFmt(out, dynverSonatypeSnapshots.value), fallbackVersion(dynverCurrentDate.value))
ThisBuild / dynver := {
  val d = new java.util.Date
  sbtdynver.DynVer
    .getGitDescribeOutput(d)
    .mkVersion(out => versionFmt(out, dynverSonatypeSnapshots.value), fallbackVersion(d))
}

lazy val PlayBuildLinkProject = PlayNonCrossBuiltProject("Play-Build-Link", "dev-mode/play-build-link")
  .dependsOn(PlayExceptionsProject)

// play-run-support project is only compiled against sbt scala version
lazy val PlayRunSupportProject = PlayNonCrossBuiltProject("Play-Run-Support", "dev-mode/play-run-support")
  .settings(
    target := target.value / "play-run-support",
    libraryDependencies ++= runSupportDeps
  )
  .dependsOn(PlayBuildLinkProject)

// This project is actually not needed by Play, we (cross) publish it for the community only:
// https://github.com/playframework/playframework/issues/5290
lazy val PlayRoutesCompilerProject = PlayDevelopmentProject("Play-Routes-Compiler", "dev-mode/play-routes-compiler")
  .enablePlugins(SbtTwirl)
  .settings(
    scalacOptions -= "-Xsource:3", // Since this is actually a sbt plugin and the codebase is 2.12, this flag causes problems when cross compiling
    libraryDependencies ++= routesCompilerDependencies(scalaVersion.value),
    TwirlKeys.templateFormats := Map("twirl" -> "play.routes.compiler.ScalaFormat")
  )

lazy val SbtRoutesCompilerProject = PlaySbtProject("Sbt-Routes-Compiler", "dev-mode/play-routes-compiler")
  .enablePlugins(SbtTwirl)
  .settings(
    target := target.value / "sbt-routes-compiler",
    libraryDependencies ++= routesCompilerDependencies(scalaVersion.value),
    TwirlKeys.templateFormats := Map("twirl" -> "play.routes.compiler.ScalaFormat")
  )

lazy val PlayStreamsProject = PlayCrossBuiltProject("Play-Streams", "core/play-streams")
  .settings(libraryDependencies ++= streamsDependencies)

lazy val PlayExceptionsProject = PlayNonCrossBuiltProject("Play-Exceptions", "core/play-exceptions")

lazy val PlayBillOfMaterials = PlayCrossBuiltProject("Play-Bom", "dev-mode/play-bill-of-materials")
  .enablePlugins(BillOfMaterialsPlugin)
  .settings(
    bomIncludeProjects    := userProjects,
    mimaPreviousArtifacts := Set.empty
  )

lazy val PlayJodaFormsProject = PlayCrossBuiltProject("Play-Joda-Forms", "web/play-joda-forms")
  .settings(
    libraryDependencies ++= joda
  )
  .dependsOn(PlayProject, PlaySpecs2Project % "test")

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
          Dependencies.pekkoVersion,
          Dependencies.pekkoHttpVersion,
          (Compile / sourceManaged).value
        )
      )
      .taskValue,
    (Compile / TwirlKeys.compileTemplates / sourceDirectories) := (Compile / unmanagedSourceDirectories).value,
    TwirlKeys.templateImports += "play.api.templates.PlayMagic._",
    (Compile / packageSrc / mappings) ++= {
      // Add both the templates, useful for end users to read, and the Scala sources that they get compiled to,
      // so omnidoc can compile and produce scaladocs for them.
      val twirlSources = (Compile / TwirlKeys.compileTemplates / sources).value
        .pair(relativeTo((Compile / TwirlKeys.compileTemplates / sourceDirectories).value))

      val twirlTarget = (Compile / TwirlKeys.compileTemplates / target).value
      // The pair with errorIfNone being false both creates the mappings, and filters non twirl outputs out of
      // managed sources
      val twirlCompiledSources = (Compile / managedSources).value.pair(relativeTo(twirlTarget), errorIfNone = false)

      twirlSources ++ twirlCompiledSources
    },
    Docs.apiDocsIncludeManaged := true
  )
  .settings(Docs.playdocSettings: _*)
  .dependsOn(
    PlayBuildLinkProject,
    PlayStreamsProject,
    PlayConfiguration
  )

lazy val PlayServerProject = PlayCrossBuiltProject("Play-Server", "transport/server/play-server")
  .settings(libraryDependencies ++= playServerDependencies)
  .dependsOn(
    PlayProject,
    PlayGuiceProject % "test"
  )

lazy val PlayNettyServerProject = PlayCrossBuiltProject("Play-Netty-Server", "transport/server/play-netty-server")
  .settings(libraryDependencies ++= netty)
  .dependsOn(PlayServerProject)

lazy val PlayPekkoHttpServerProject =
  PlayCrossBuiltProject("Play-Pekko-Http-Server", "transport/server/play-pekko-http-server")
    .dependsOn(PlayServerProject, PlayStreamsProject)
    .dependsOn(PlayGuiceProject % "test")
    .settings(
      libraryDependencies ++= specs2Deps.map(_ % "test"),
      libraryDependencies += pekkoHttp
    )

lazy val PlayPekkoHttp2SupportProject =
  PlayCrossBuiltProject("Play-Pekko-Http2-Support", "transport/server/play-pekko-http2-support")
    .dependsOn(PlayPekkoHttpServerProject)

lazy val PlayClusterSharding = PlayCrossBuiltProject("Play-Cluster-Sharding", "cluster/play-cluster-sharding")
  .settings(libraryDependencies ++= clusterDependencies)
  .dependsOn(PlayProject)

lazy val PlayJavaClusterSharding =
  PlayCrossBuiltProject("Play-Java-Cluster-Sharding", "cluster/play-java-cluster-sharding")
    .settings(libraryDependencies ++= clusterDependencies)
    .dependsOn(PlayProject)

lazy val PlayJdbcApiProject = PlayCrossBuiltProject("Play-JDBC-Api", "persistence/play-jdbc-api")
  .settings(
    libraryDependencies += javaxInject,
  )

lazy val PlayJdbcProject: Project = PlayCrossBuiltProject("Play-JDBC", "persistence/play-jdbc")
  .settings(libraryDependencies ++= jdbcDeps)
  .dependsOn(PlayJdbcApiProject, PlayProject)
  .dependsOn(PlaySpecs2Project % "test")

lazy val PlayJdbcEvolutionsProject = PlayCrossBuiltProject("Play-JDBC-Evolutions", "persistence/play-jdbc-evolutions")
  .settings(libraryDependencies ++= derbyDatabase.map(_ % Test))
  .dependsOn(PlayJdbcApiProject, PlayProject)
  .dependsOn(PlaySpecs2Project % "test")
  .dependsOn(PlayJdbcProject % "test->test")
  .dependsOn(PlayJavaJdbcProject % "test")

lazy val PlayJavaJdbcProject = PlayCrossBuiltProject("Play-Java-JDBC", "persistence/play-java-jdbc")
  .settings(libraryDependencies += assertj % Test)
  .dependsOn(PlayJdbcProject % "compile->compile;test->test", PlayJavaProject)
  .dependsOn(PlaySpecs2Project % "test", PlayGuiceProject % "test")

lazy val PlayJpaProject = PlayCrossBuiltProject("Play-Java-JPA", "persistence/play-java-jpa")
  .settings(libraryDependencies ++= jpaDeps)
  .dependsOn(PlayJavaJdbcProject % "compile->compile;test->test")
  .dependsOn(PlayJdbcEvolutionsProject % "test")
  .dependsOn(PlaySpecs2Project % "test")

lazy val PlayTestProject = PlayCrossBuiltProject("Play-Test", "testkit/play-test")
  .settings(
    libraryDependencies ++= testDependencies ++ Seq(h2database, assertj).map(_ % "test"),
    (Test / parallelExecution) := false
  )
  .dependsOn(
    PlayGuiceProject,
    PlayServerProject,
    // We still need a server provider when running Play-Test tests.
    // Since Pekko HTTP is the default, we should use it here.
    PlayPekkoHttpServerProject % "test"
  )

lazy val PlaySpecs2Project = PlayCrossBuiltProject("Play-Specs2", "testkit/play-specs2")
  .settings(
    libraryDependencies ++= specs2Deps :+ (if (ScalaArtifacts.isScala3(scalaVersion.value)) { mockitoAll }
                                           else { specs2Mock }),
    (Test / parallelExecution) := false
  )
  .dependsOn(PlayTestProject)

lazy val PlayJavaProject = PlayCrossBuiltProject("Play-Java", "core/play-java")
  .settings(libraryDependencies ++= javaDeps ++ javaTestDeps)
  .dependsOn(
    PlayProject       % "compile;test->test",
    PlayTestProject   % "test",
    PlaySpecs2Project % "test",
    PlayGuiceProject  % "test"
  )

lazy val PlayJavaFormsProject = PlayCrossBuiltProject("Play-Java-Forms", "web/play-java-forms")
  .settings(
    libraryDependencies ++= javaDeps ++ javaFormsDeps ++ javaTestDeps
  )
  .dependsOn(
    PlayJavaProject % "compile;test->test"
  )

lazy val PlayDocsProject = PlayCrossBuiltProject("Play-Docs", "dev-mode/play-docs")
  .settings(Docs.settings: _*)
  .settings(
    libraryDependencies ++= playDocsDependencies
  )
  .dependsOn(PlayNettyServerProject)

lazy val PlayGuiceProject = PlayCrossBuiltProject("Play-Guice", "core/play-guice")
  .settings(libraryDependencies ++= guiceDeps ++ (specs2Deps ++ Seq(assertj)).map(_ % "test"))
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
        Dependencies.pekkoVersion,
        Dependencies.pekkoHttpVersion,
        (Compile / sourceManaged).value
      )
    }.taskValue,
    (Compile / headerSources) ++= (sbtTestDirectory.value ** ("*.scala" || "*.java" || "*.sbt")).get,
  )
  .dependsOn(SbtRoutesCompilerProject, PlayRunSupportProject)

lazy val SbtScriptedToolsProject = PlaySbtPluginProject("Sbt-Scripted-Tools", "dev-mode/sbt-scripted-tools")
  .enablePlugins(SbtPlugin)
  .dependsOn(SbtPluginProject)

lazy val PlayLogback = PlayCrossBuiltProject("Play-Logback", "core/play-logback")
  .settings(
    libraryDependencies += logback,
    (Test / parallelExecution) := false,
    // quieten deprecation warnings in tests
    (Test / scalacOptions) := (Test / scalacOptions).value.diff(Seq("-deprecation"))
  )
  .dependsOn(PlayProject)
  .dependsOn(PlaySpecs2Project % "test")

lazy val PlayConfiguration = PlayCrossBuiltProject("Play-Configuration", "core/play-configuration")
  .settings(
    libraryDependencies ++= Seq(typesafeConfig, slf4jApi) ++ specs2Deps.map(_ % Test),
    (Test / parallelExecution) := false,
    // quieten deprecation warnings in tests
    (Test / scalacOptions) := (Test / scalacOptions).value.diff(Seq("-deprecation"))
  )
  .dependsOn(PlayExceptionsProject)

lazy val PlayWsProject = PlayCrossBuiltProject("Play-WS", "transport/client/play-ws")
  .settings(
    libraryDependencies ++= playWsDeps,
    (Test / parallelExecution) := false,
    // quieten deprecation warnings in tests
    (Test / scalacOptions) := (Test / scalacOptions).value.diff(Seq("-deprecation"))
  )
  .dependsOn(PlayProject)
  .dependsOn(PlayTestProject % "test")

lazy val PlayAhcWsProject = PlayCrossBuiltProject("Play-AHC-WS", "transport/client/play-ahc-ws")
  .settings(
    libraryDependencies ++= playAhcWsDeps,
    (Test / parallelExecution) := false,
    // quieten deprecation warnings in tests
    (Test / scalacOptions) := (Test / scalacOptions).value.diff(Seq("-deprecation"))
  )
  .dependsOn(PlayWsProject, PlayCaffeineCacheProject % "test")
  .dependsOn(PlaySpecs2Project % "test")
  .dependsOn(PlayTestProject % "test->test")
  .dependsOn(PlayPekkoHttpServerProject % "test") // Because we need a server provider when running the tests

lazy val PlayOpenIdProject = PlayCrossBuiltProject("Play-OpenID", "web/play-openid")
  .settings(
    (Test / parallelExecution) := false,
    // quieten deprecation warnings in tests
    (Test / scalacOptions) := (Test / scalacOptions).value.diff(Seq("-deprecation"))
  )
  .dependsOn(PlayAhcWsProject)
  .dependsOn(PlaySpecs2Project % "test")

lazy val PlayFiltersHelpersProject = PlayCrossBuiltProject("Play-Filters-Helpers", "web/play-filters-helpers")
  .settings(
    libraryDependencies ++= playFilterDeps,
    (Test / parallelExecution) := false
  )
  .dependsOn(
    PlayProject,
    PlayTestProject            % "test",
    PlayJavaProject            % "test",
    PlaySpecs2Project          % "test",
    PlayAhcWsProject           % "test",
    PlayPekkoHttpServerProject % "test" // Because we need a server provider when running the tests
  )

lazy val PlayIntegrationTestProject = PlayCrossBuiltProject("Play-Integration-Test", "core/play-integration-test")
// This project is just for testing Play, not really a public artifact
  .settings(disablePublishing)
  .settings(
    libraryDependencies += okHttp % Test,
    (Test / parallelExecution)   := false,
    mimaPreviousArtifacts        := Set.empty,
    (Test / fork)                := true,
    (Test / javaOptions) += "-Dfile.encoding=UTF8",
  )
  .dependsOn(
    PlayProject       % "test->test",
    PlayLogback       % "test->test",
    PlayAhcWsProject  % "test->test",
    PlayServerProject % "test->test",
    PlaySpecs2Project
  )
  .dependsOn(PlayFiltersHelpersProject)
  .dependsOn(PlayJavaProject)
  .dependsOn(PlayJavaFormsProject)
  .dependsOn(PlayPekkoHttpServerProject)
  .dependsOn(PlayPekkoHttp2SupportProject)
  .dependsOn(PlayNettyServerProject)

// NOTE: this project depends on JMH, which is GPLv2.
lazy val PlayMicrobenchmarkProject = PlayCrossBuiltProject("Play-Microbenchmark", "core/play-microbenchmark")
  .enablePlugins(JmhPlugin)
  // This project is just for microbenchmarking Play. Not published.
  .settings(disablePublishing)
  .settings(
    // Change settings so that IntelliJ can handle dependencies
    // from JMH to the integration tests. We can't use "compile->test"
    // when we depend on the integration test project, we have to use
    // "test->test" so that IntelliJ can handle it. This means that
    // we need to put our JMH sources into src/test so they can pick
    // up the integration test files.
    // See: https://github.com/ktoso/sbt-jmh/pull/73#issue-163891528
    (Jmh / classDirectory)                 := (Test / classDirectory).value,
    (Jmh / dependencyClasspath)            := (Test / dependencyClasspath).value,
    (Jmh / generateJmhSourcesAndResources) := (Jmh / generateJmhSourcesAndResources).dependsOn(Test / compile).value,
    (Jmh / run / mainClass)                := Some("org.openjdk.jmh.Main"),
    (Test / parallelExecution)             := false,
    mimaPreviousArtifacts                  := Set.empty
  )
  .dependsOn(
    PlayProject                % "test->test",
    PlayLogback                % "test->test",
    PlayIntegrationTestProject % "test->test",
    PlayAhcWsProject,
    PlaySpecs2Project,
    PlayFiltersHelpersProject,
    PlayJavaProject,
    PlayNettyServerProject,
    PlayPekkoHttpServerProject,
    PlayPekkoHttp2SupportProject
  )

lazy val PlayCacheProject = PlayCrossBuiltProject("Play-Cache", "cache/play-cache")
  .settings(
    libraryDependencies ++= playCacheDeps
  )
  .dependsOn(
    PlayProject,
    PlaySpecs2Project % "test"
  )

lazy val PlayEhcacheProject = PlayCrossBuiltProject("Play-Ehcache", "cache/play-ehcache")
  .settings(
    libraryDependencies ++= playEhcacheDeps
  )
  .dependsOn(
    PlayProject,
    PlayCacheProject,
    PlaySpecs2Project % "test"
  )

lazy val PlayCaffeineCacheProject = PlayCrossBuiltProject("Play-Caffeine-Cache", "cache/play-caffeine-cache")
  .settings(
    libraryDependencies ++= playCaffeineDeps
  )
  .dependsOn(
    PlayProject,
    PlayCacheProject,
    PlaySpecs2Project % "test"
  )

// JSR 107 cache bindings (note this does not depend on ehcache)
lazy val PlayJCacheProject = PlayCrossBuiltProject("Play-JCache", "cache/play-jcache")
  .settings(
    libraryDependencies ++= jcacheApi
  )
  .dependsOn(
    PlayProject,
    PlayCaffeineCacheProject % "test", // provide a cachemanager implementation
    PlaySpecs2Project        % "test"
  )

lazy val PlayDocsSbtPlugin = PlaySbtPluginProject("Play-Docs-Sbt-Plugin", "dev-mode/play-docs-sbt-plugin")
  .enablePlugins(SbtPlugin)
  .enablePlugins(SbtTwirl)
  .settings(
    libraryDependencies ++= playDocsSbtPluginDependencies,
    scriptedDependencies := (()), // drop Test/compile & publishLocal being called on aggregating root scripted
  )
  .dependsOn(SbtPluginProject)

// These projects are aggregate by the root project and every
// task (compile, test, publish, etc) executed for the root
// project will also be executed for them:
// https://www.scala-sbt.org/1.x/docs/Multi-Project.html#Aggregation
//
// Keep in mind that specific configurations (like skip in publish) will be respected.
lazy val userProjects = Seq[ProjectReference](
  PlayProject,
  PlayGuiceProject,
  PlayBuildLinkProject,
  PlayRoutesCompilerProject,
  PlayPekkoHttpServerProject,
  PlayPekkoHttp2SupportProject,
  PlayCacheProject,
  PlayEhcacheProject,
  PlayCaffeineCacheProject,
  PlayJCacheProject,
  PlayJdbcApiProject,
  PlayJdbcProject,
  PlayJdbcEvolutionsProject,
  PlayJavaProject,
  PlayJavaFormsProject,
  PlayJodaFormsProject,
  PlayJavaJdbcProject,
  PlayJpaProject,
  PlayNettyServerProject,
  PlayServerProject,
  PlayLogback,
  PlayConfiguration,
  PlayWsProject,
  PlayAhcWsProject,
  PlayOpenIdProject,
  PlaySpecs2Project,
  PlayTestProject,
  PlayExceptionsProject,
  PlayFiltersHelpersProject,
  PlayStreamsProject,
  PlayClusterSharding,
  PlayJavaClusterSharding
)
lazy val nonUserProjects = Seq[ProjectReference](
  PlayMicrobenchmarkProject,
  PlayDocsProject,
  PlayIntegrationTestProject,
  PlayDocsSbtPlugin,
  PlayRunSupportProject,
  SbtRoutesCompilerProject,
  SbtPluginProject,
  SbtScriptedToolsProject,
  PlayBillOfMaterials
)

lazy val PlayFramework = Project("Play-Framework", file("."))
  .enablePlugins(PlayRootProject)
  .settings(
    playCommonSettings,
    scalaVersion                    := (PlayProject / scalaVersion).value,
    crossScalaVersions              := Nil,
    (ThisBuild / playBuildRepoName) := "playframework",
    (Global / concurrentRestrictions) += Tags.limit(Tags.Test, 1),
    libraryDependencies ++= (runtime(scalaVersion.value) ++ jdbcDeps),
    Docs.apiDocsInclude        := false,
    Docs.apiDocsIncludeManaged := false,
    mimaReportBinaryIssues     := (()),
    commands += Commands.quickPublish,
    publish / skip := true,
    (Compile / headerSources) ++=
      ((baseDirectory.value ** ("*.default" || "*.properties" || "*.md" || "*.sbt" || "*.routes" || "routes" || "*.js" || "*.less"))
        --- (baseDirectory.value ** "jquery*js")
        --- (baseDirectory.value ** "target" ** "*")
        --- (baseDirectory.value / "documentation" ** "*")).get ++
        (baseDirectory.value / "web" / "play-openid" ** "*.html" --- (baseDirectory.value ** "target" ** "*")).get ++
        (baseDirectory.value / "project" ** "*.scala" --- (baseDirectory.value ** "target" ** "*")).get
  )
  .aggregate((userProjects ++ nonUserProjects): _*)

//val _ = sys.props += ("sbt_validateCode" -> List(
//  "+checkPekkoModuleVersions", // TODO: See https://github.com/playframework/playframework/issues/11986
//).mkString(";"))
