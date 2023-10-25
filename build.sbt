/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
import BuildSettings._
import Dependencies._
import Generators._
import com.lightbend.sbt.javaagent.JavaAgent.JavaAgentKeys.javaAgents
import com.lightbend.sbt.javaagent.JavaAgent.JavaAgentKeys.resolvedJavaAgents
import interplay.PlayBuildBase.autoImport._
import pl.project13.scala.sbt.JmhPlugin.generateJmhSourcesAndResources
import sbt.Keys.parallelExecution
import sbt._
import sbt.io.Path._
import org.scalafmt.sbt.ScalafmtPlugin
import VersionHelper._

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
dynverVTagPrefix in ThisBuild := false

// We are publishing snapshots to Sonatype
(ThisBuild / dynverSonatypeSnapshots) := true

// Sanity-check: assert that version comes from a tag (e.g. not a too-shallow clone)
// https://github.com/dwijnand/sbt-dynver/#sanity-checking-the-version
Global / onLoad := (Global / onLoad).value.andThen { s =>
  val v = version.value
  if (dynverGitDescribeOutput.value.hasNoTags)
    throw new MessageOnlyException(
      s"Failed to derive version from git tags. Maybe run `git fetch --unshallow`? Version: $v"
    )
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
  .settings(libraryDependencies ++= streamsDependencies(scalaVersion.value))

lazy val PlayExceptionsProject = PlayNonCrossBuiltProject("Play-Exceptions", "core/play-exceptions")

lazy val billOfMaterials = PlayCrossBuiltProject("bill-of-materials", "dev-mode/bill-of-materials")
  .enablePlugins(BillOfMaterialsPlugin)
  .disablePlugins(MimaPlugin)
  .settings(
    name                  := "play-bom",
    bomIncludeProjects    := userProjects,
    pomExtra              := pomExtra.value :+ bomDependenciesListing.value,
    publishTo             := sonatypePublishToBundle.value,
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
    // Among other things, scala-xml is used for body parsers. Play never explicitly depended on scala-xml, but it was pulled in by SbtTwirl (via twirl-api).
    // In scala-xml 1.3.1+ an issue was fixed which could have caused body parsers to overflow the stack. Therefore we finally make Play depend on scala-xml.
    // In theory we could have made twirl 1.5.x depend on that scala-xml version, however in practise this causes problems, since twirl is also published for scala-js 0.6.x,
    // but scala-xml 1.3.1 is not published for scala-js 0.6.x anymore (1.3.0 still was). To avoid any bad side effects (and since publishing twirl 1.5.x is a bit of a pain), we keep twirl 1.5.1
    // untouched. Users who want to use twirl standalone should just upgrade to 1.6.0, which depends on latest scala-xml 2.x (which includes the fix as well).
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.2.0",
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
    Docs.apiDocsIncludeManaged := true
  )
  .settings(Docs.playdocSettings: _*)
  .dependsOn(
    BuildLinkProject,
    StreamsProject
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

lazy val PlayClusterSharding = PlayCrossBuiltProject("Play-Cluster-Sharding", "cluster/play-cluster-sharding")
  .settings(libraryDependencies ++= clusterDependencies)
  .dependsOn(PlayProject)

lazy val PlayJavaClusterSharding =
  PlayCrossBuiltProject("Play-Java-Cluster-Sharding", "cluster/play-java-cluster-sharding")
    .settings(libraryDependencies ++= clusterDependencies)
    .dependsOn(PlayProject)

lazy val PlayJdbcApiProject = PlayCrossBuiltProject("Play-JDBC-Api", "persistence/play-jdbc-api")
  .dependsOn(PlayProject)

lazy val PlayJdbcProject: Project = PlayCrossBuiltProject("Play-JDBC", "persistence/play-jdbc")
  .settings(libraryDependencies ++= jdbcDeps)
  .dependsOn(PlayJdbcApiProject)
  .dependsOn(PlaySpecs2Project % "test")

lazy val PlayJdbcEvolutionsProject = PlayCrossBuiltProject("Play-JDBC-Evolutions", "persistence/play-jdbc-evolutions")
  .settings(libraryDependencies += derbyDatabase % Test)
  .dependsOn(PlayJdbcApiProject)
  .dependsOn(PlaySpecs2Project % "test")
  .dependsOn(PlayJdbcProject % "test->test")
  .dependsOn(PlayJavaJdbcProject % "test")

lazy val PlayJavaJdbcProject = PlayCrossBuiltProject("Play-Java-JDBC", "persistence/play-java-jdbc")
  .dependsOn(PlayJdbcProject % "compile->compile;test->test", PlayJavaProject)
  .dependsOn(PlaySpecs2Project % "test", PlayGuiceProject % "test")

lazy val PlayJpaProject = PlayCrossBuiltProject("Play-Java-JPA", "persistence/play-java-jpa")
  .settings(libraryDependencies ++= jpaDeps)
  .dependsOn(PlayJavaJdbcProject % "compile->compile;test->test")
  .dependsOn(PlayJdbcEvolutionsProject % "test")
  .dependsOn(PlaySpecs2Project % "test")

lazy val PlayTestProject = PlayCrossBuiltProject("Play-Test", "testkit/play-test")
  .settings(
    libraryDependencies ++= testDependencies ++ Seq(h2database % "test"),
    parallelExecution in Test := false
  )
  .dependsOn(
    PlayGuiceProject,
    PlayServerProject,
    // We still need a server provider when running Play-Test tests.
    // Since Akka HTTP is the default, we should use it here.
    PlayAkkaHttpServerProject % "test"
  )

lazy val PlaySpecs2Project = PlayCrossBuiltProject("Play-Specs2", "testkit/play-specs2")
  .settings(
    libraryDependencies ++= specs2Deps,
    parallelExecution in Test := false
  )
  .dependsOn(PlayTestProject)

lazy val PlayJavaProject = PlayCrossBuiltProject("Play-Java", "core/play-java")
  .settings(libraryDependencies ++= javaDeps(scalaVersion.value) ++ javaTestDeps)
  .dependsOn(
    PlayProject       % "compile;test->test",
    PlayTestProject   % "test",
    PlaySpecs2Project % "test",
    PlayGuiceProject  % "test"
  )

lazy val PlayJavaFormsProject = PlayCrossBuiltProject("Play-Java-Forms", "web/play-java-forms")
  .settings(
    libraryDependencies ++= javaDeps(scalaVersion.value) ++ javaFormsDeps ++ javaTestDeps
  )
  .dependsOn(
    PlayJavaProject % "compile;test->test"
  )

lazy val PlayDocsProject = PlayCrossBuiltProject("Play-Docs", "dev-mode/play-docs")
  .settings(Docs.settings: _*)
  .settings(
    libraryDependencies ++= playDocsDependencies
  )
  .dependsOn(PlayAkkaHttpServerProject)

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
  )
  .dependsOn(SbtRoutesCompilerProject, RunSupportProject)

lazy val SbtScriptedToolsProject = PlaySbtPluginProject("Sbt-Scripted-Tools", "dev-mode/sbt-scripted-tools")
  .enablePlugins(SbtPlugin)
  .dependsOn(SbtPluginProject)
  .settings(disableNonLocalPublishing)

lazy val PlayLogback = PlayCrossBuiltProject("Play-Logback", "core/play-logback")
  .settings(
    libraryDependencies += logback,
    parallelExecution in Test := false,
    // quieten deprecation warnings in tests
    scalacOptions in Test := (scalacOptions in Test).value.diff(Seq("-deprecation"))
  )
  .dependsOn(PlayProject)
  .dependsOn(PlaySpecs2Project % "test")

lazy val PlayWsProject = PlayCrossBuiltProject("Play-WS", "transport/client/play-ws")
  .settings(
    libraryDependencies ++= playWsDeps,
    parallelExecution in Test := false,
    // quieten deprecation warnings in tests
    scalacOptions in Test := (scalacOptions in Test).value.diff(Seq("-deprecation"))
  )
  .dependsOn(PlayProject)
  .dependsOn(PlayTestProject % "test")

lazy val PlayAhcWsProject = PlayCrossBuiltProject("Play-AHC-WS", "transport/client/play-ahc-ws")
  .settings(
    libraryDependencies ++= playAhcWsDeps,
    parallelExecution in Test := false,
    // quieten deprecation warnings in tests
    scalacOptions in Test := (scalacOptions in Test).value.diff(Seq("-deprecation"))
  )
  .dependsOn(PlayWsProject, PlayCaffeineCacheProject % "test")
  .dependsOn(PlaySpecs2Project % "test")
  .dependsOn(PlayTestProject % "test->test")
  .dependsOn(PlayAkkaHttpServerProject % "test") // Because we need a server provider when running the tests

lazy val PlayOpenIdProject = PlayCrossBuiltProject("Play-OpenID", "web/play-openid")
  .settings(
    parallelExecution in Test := false,
    // quieten deprecation warnings in tests
    scalacOptions in Test := (scalacOptions in Test).value.diff(Seq("-deprecation"))
  )
  .dependsOn(PlayAhcWsProject)
  .dependsOn(PlaySpecs2Project % "test")

lazy val PlayFiltersHelpersProject = PlayCrossBuiltProject("Filters-Helpers", "web/play-filters-helpers")
  .settings(
    libraryDependencies ++= playFilterDeps,
    parallelExecution in Test := false
  )
  .dependsOn(
    PlayProject,
    PlayTestProject           % "test",
    PlayJavaProject           % "test",
    PlaySpecs2Project         % "test",
    PlayAhcWsProject          % "test",
    PlayAkkaHttpServerProject % "test" // Because we need a server provider when running the tests
  )

lazy val PlayIntegrationTestProject = PlayCrossBuiltProject("Play-Integration-Test", "core/play-integration-test")
  .enablePlugins(JavaAgent)
  // This project is just for testing Play, not really a public artifact
  .settings(disablePublishing)
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    inConfig(IntegrationTest)(ScalafmtPlugin.scalafmtConfigSettings),
    inConfig(IntegrationTest)(JavaFormatterPlugin.toBeScopedSettings),
    libraryDependencies += okHttp         % IntegrationTest,
    parallelExecution in IntegrationTest := false,
    mimaPreviousArtifacts                := Set.empty,
    fork in IntegrationTest              := true,
    javaOptions in IntegrationTest += "-Dfile.encoding=UTF8",
    javaAgents += jettyAlpnAgent % IntegrationTest,
    javaOptions in IntegrationTest ++= {
      val javaAgents = (resolvedJavaAgents in IntegrationTest).value
      assert(javaAgents.length == 1, s"multiple java agents: $javaAgents")
      val resolvedJavaAgent = javaAgents.head
      val jettyAgentPath    = resolvedJavaAgent.artifact.absString
      Seq(
        s"-Djetty.anlp.agent.jar=$jettyAgentPath",
        "-javaagent:" + jettyAgentPath + resolvedJavaAgent.agent.arguments
      )
    }
  )
  .dependsOn(
    PlayProject       % "it->test",
    PlayLogback       % "it->test",
    PlayAhcWsProject  % "it->test",
    PlayServerProject % "it->test",
    PlaySpecs2Project
  )
  .dependsOn(PlayFiltersHelpersProject)
  .dependsOn(PlayJavaProject)
  .dependsOn(PlayJavaFormsProject)
  .dependsOn(PlayAkkaHttpServerProject)
  .dependsOn(PlayAkkaHttp2SupportProject)
  .dependsOn(PlayNettyServerProject)

// NOTE: this project depends on JMH, which is GPLv2.
lazy val PlayMicrobenchmarkProject = PlayCrossBuiltProject("Play-Microbenchmark", "core/play-microbenchmark")
  .enablePlugins(JmhPlugin, JavaAgent)
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
    classDirectory in Jmh                 := (classDirectory in Test).value,
    dependencyClasspath in Jmh            := (dependencyClasspath in Test).value,
    generateJmhSourcesAndResources in Jmh := (generateJmhSourcesAndResources in Jmh).dependsOn(compile in Test).value,
    // Add the Jetty ALPN agent to the list of agents. This will cause the JAR to
    // be downloaded and available. We need to tell JMH to use this agent when it
    // forks its benchmark processes. We use a custom runner to read a system
    // property and add the agent JAR to JMH's forked process JVM arguments.
    javaAgents += jettyAlpnAgent,
    javaOptions in (Jmh, run) += {
      val javaAgents = (resolvedJavaAgents in Jmh).value
      assert(javaAgents.length == 1)
      val jettyAgentPath = javaAgents.head.artifact.absString
      s"-Djetty.anlp.agent.jar=$jettyAgentPath"
    },
    mainClass in (Jmh, run)   := Some("play.microbenchmark.PlayJmhRunner"),
    parallelExecution in Test := false,
    mimaPreviousArtifacts     := Set.empty
  )
  .dependsOn(
    PlayProject                % "test->test",
    PlayLogback                % "test->test",
    PlayIntegrationTestProject % "test->it",
    PlayAhcWsProject,
    PlaySpecs2Project,
    PlayFiltersHelpersProject,
    PlayJavaProject,
    PlayNettyServerProject,
    PlayAkkaHttpServerProject,
    PlayAkkaHttp2SupportProject
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
  BuildLinkProject,
  RoutesCompilerProject,
  PlayAkkaHttpServerProject,
  PlayAkkaHttp2SupportProject,
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
  PlayWsProject,
  PlayAhcWsProject,
  PlayOpenIdProject,
  PlaySpecs2Project,
  PlayTestProject,
  PlayExceptionsProject,
  PlayFiltersHelpersProject,
  StreamsProject,
  PlayClusterSharding,
  PlayJavaClusterSharding
)
lazy val nonUserProjects = Seq[ProjectReference](
  PlayMicrobenchmarkProject,
  PlayDocsProject,
  PlayIntegrationTestProject,
  PlayDocsSbtPlugin,
  RunSupportProject,
  SbtRoutesCompilerProject,
  SbtPluginProject,
  SbtScriptedToolsProject,
  billOfMaterials
)

lazy val PlayFramework = Project("Play-Framework", file("."))
  .enablePlugins(PlayRootProject)
  .enablePlugins(PlayWhitesourcePlugin)
  .settings(
    playCommonSettings,
    scalaVersion                   := (scalaVersion in PlayProject).value,
    playBuildRepoName in ThisBuild := "playframework",
    concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
    libraryDependencies ++= (runtime(scalaVersion.value) ++ jdbcDeps),
    Docs.apiDocsInclude        := false,
    Docs.apiDocsIncludeManaged := false,
    mimaReportBinaryIssues     := (()),
    commands += Commands.quickPublish,
    Release.settings
  )
  .aggregate((userProjects ++ nonUserProjects): _*)
