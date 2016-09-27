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

import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform.scalariformSettings
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

import pl.project13.scala.sbt.JmhPlugin
import play.twirl.sbt.SbtTwirl
import play.twirl.sbt.Import.TwirlKeys

import sbtdoge.CrossPerProjectPlugin

import bintray.BintrayPlugin.autoImport._

import interplay._
import interplay.Omnidoc.autoImport._
import interplay.PlayBuildBase.autoImport._

import scala.util.control.NonFatal


object BuildSettings {

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
  def playCommonSettings: Seq[Setting[_]] = {

    scalariformSettings ++ Seq(
      ScalariformKeys.preferences := ScalariformKeys.preferences.value
        .setPreference(SpacesAroundMultiImports, true)
        .setPreference(SpaceInsideParentheses, false)
        .setPreference(DanglingCloseParenthesis, Preserve)
        .setPreference(PreserveSpaceBeforeArguments, true)
        .setPreference(DoubleIndentClassDeclaration, true)
    ) ++ Seq(
      homepage := Some(url("https://playframework.com")),
      ivyLoggingLevel := UpdateLogging.DownloadOnly,
      resolvers ++= Seq(
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
      apiMappings += scalaInstance.value.libraryJar -> url(raw"""http://scala-lang.org/files/archive/api/${scalaInstance.value.actualVersion}/index.html"""),
      apiMappings += {
        // Maps JDK 1.8 jar into apidoc.
        val rtJar: String = System.getProperty("sun.boot.class.path").split(java.io.File.pathSeparator).collectFirst {
          case str: String if str.endsWith(java.io.File.separator + "rt.jar") => str
        }.get // fail hard if not found
        file(rtJar) -> url(Docs.javaApiUrl)
      },
      apiMappings ++= {
        // Finds appropriate scala apidoc from dependencies when autoAPIMappings are insufficient.
        // See the following:
        //
        // http://stackoverflow.com/questions/19786841/can-i-use-sbts-apimappings-setting-for-managed-dependencies/20919304#20919304
        // http://www.scala-sbt.org/release/docs/Howto-Scaladoc.html#Enable+manual+linking+to+the+external+Scaladoc+of+managed+dependencies
        // https://github.com/ThoughtWorksInc/sbt-api-mappings/blob/master/src/main/scala/com/thoughtworks/sbtApiMappings/ApiMappings.scala#L34

        val ScalaLibraryRegex = """^.*[/\\]scala-library-([\d\.]+)\.jar$""".r
        val JavaxInjectRegex = """^.*[/\\]java.inject-([\d\.]+)\.jar$""".r

        val IvyRegex = """^.*[/\\]([\.\-_\w]+)[/\\]([\.\-_\w]+)[/\\](?:jars|bundles)[/\\]([\.\-_\w]+)\.jar$""".r

        (for {
          jar <- (dependencyClasspath in Compile in doc).value.toSet ++ (dependencyClasspath in Test in doc).value
          fullyFile = jar.data
          urlOption = fullyFile.getCanonicalPath match {
            case ScalaLibraryRegex(v) =>
              Some(url(raw"""http://scala-lang.org/files/archive/api/$v/index.html"""))

            case JavaxInjectRegex(v) =>
              // the jar file doesn't match up with $apiName-
              Some(url(Docs.javaxInjectUrl))

            case re@IvyRegex(apiOrganization, apiName, jarBaseFile) if jarBaseFile.startsWith(s"$apiName-") =>
              val apiVersion = jarBaseFile.substring(apiName.length + 1, jarBaseFile.length)
              apiOrganization match {
                case "com.typesafe.akka" =>
                  Some(url(raw"http://doc.akka.io/api/akka/$apiVersion/"))

                case default =>
                  val link = Docs.artifactToJavadoc(apiOrganization, apiName, apiVersion, jarBaseFile)
                  Some(url(link))
              }

            case other =>
              None

          }
          url <- urlOption
        } yield (fullyFile -> url))(collection.breakOut(Map.canBuildFrom))
      }
    )
  }

  /**
   * These settings are used by all projects that are part of the runtime, as opposed to development, mode of Play.
   */
  def playRuntimeSettings: Seq[Setting[_]] = playCommonSettings ++ mimaDefaultSettings ++ Seq(
    previousArtifacts := {
      // Binary compatibility is tested against these versions
      val previousVersions = {
        val VersionPattern = """^(\d+).(\d+).(\d+)(-.*)?""".r
        version.value match {
          case VersionPattern(epoch, major, minor, rest) => (0 until minor.toInt).map(v => s"$epoch.$major.$v")
          case _ => sys.error(s"Cannot find previous versions for ${version.value}")
        }
      }.toSet
      if (crossPaths.value) {
        previousVersions.map(v => organization.value % s"${moduleName.value}_${scalaBinaryVersion.value}" %  v)
      } else {
        previousVersions.map(v => organization.value % moduleName.value %  v)
      }
    },
    // Argument for setting size of permgen space or meta space for all forked processes
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

  lazy val StreamsProject = PlayCrossBuiltProject("Play-Streams", "play-streams")
    .settings(libraryDependencies ++= streamsDependencies)

  lazy val FunctionalProject = PlayCrossBuiltProject("Play-Functional", "play-functional")

  lazy val DataCommonsProject = PlayCrossBuiltProject("Play-DataCommons", "play-datacommons")

  lazy val JsonProject = PlayCrossBuiltProject("Play-Json", "play-json")
    .settings(libraryDependencies ++= jsonDependencies(scalaVersion.value))
    .dependsOn(FunctionalProject, DataCommonsProject)

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
      Docs.apiDocsIncludeManaged := true
    ).settings(Docs.playdocSettings: _*)
     .dependsOn(
      BuildLinkProject,
      JsonProject,
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
      sourceGenerators in Compile <+= (version, scalaVersion in PlayProject, sbtVersion, sourceManaged in Compile) map PlayVersion,
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
    ).dependsOn(PlayProject, PlayJavaProject, PlaySpecs2Project % "test", PlayWsProject % "test")

  // This project is just for testing Play, not really a public artifact
  lazy val PlayIntegrationTestProject = PlayCrossBuiltProject("Play-Integration-Test", "play-integration-test")
    .settings(
      libraryDependencies += h2database % Test,
      parallelExecution in Test := false,
      previousArtifacts := Set.empty
    )
    .dependsOn(PlayProject % "test->test", PlayLogback % "test->test", PlayWsProject, PlayWsJavaProject, PlaySpecs2Project)
    .dependsOn(PlayFiltersHelpersProject)
    .dependsOn(PlayJavaProject)
    .dependsOn(PlayAkkaHttpServerProject)

  // This project is just for microbenchmarking Play, not really a public artifact
  lazy val PlayMicrobenchmarkProject = PlayCrossBuiltProject("Play-Microbenchmark", "play-microbenchmark")
    .enablePlugins(JmhPlugin)
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
    PlayGuiceProject,
    BuildLinkProject,
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
