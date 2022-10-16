/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
import java.util.regex.Pattern
import com.jsuereth.sbtpgp.PgpKeys
import com.typesafe.tools.mima.core.ProblemFilters
import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.plugin.MimaKeys._
import com.typesafe.tools.mima.plugin.MimaPlugin
import interplay._
import interplay.PlayBuildBase.autoImport._
import interplay.ScalaVersions._
import sbt._
import sbt.Keys._
import sbt.ScriptedPlugin.autoImport._

import scala.sys.process.stringToProcess
import scala.util.control.NonFatal

object BuildSettings {

  val playVersion = "2.8.16-lila_1.13.1"

  /** File header settings.  */
  private def fileUriRegexFilter(pattern: String): FileFilter = new FileFilter {
    val compiledPattern = Pattern.compile(pattern)
    override def accept(pathname: File): Boolean = {
      val uriString = pathname.toURI.toString
      compiledPattern.matcher(uriString).matches()
    }
  }

  private val VersionPattern = """^(\d+).(\d+).(\d+)(-.*)?""".r

  def evictionSettings: Seq[Setting[_]] = Seq(
    // This avoids a lot of dependency resolution warnings to be showed.
    (update / evictionWarningOptions) := EvictionWarningOptions.default
      .withWarnTransitiveEvictions(false)
      .withWarnDirectEvictions(false)
  )

  val SourcesApplication = config("sources").hide

  /** These settings are used by all projects. */
  def playCommonSettings: Seq[Setting[_]] = Def.settings(
    ivyLoggingLevel := UpdateLogging.DownloadOnly,
    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases"), // sync ScriptedTools.scala
      Resolver.typesafeRepo("releases"),
      Resolver.typesafeIvyRepo("releases"),
      Resolver.sbtPluginRepo("releases"), // weird sbt-pgp/play docs/vegemite issue
    ),
    evictionSettings,
    ivyConfigurations ++= Seq(SourcesApplication),
    javacOptions ++= Seq("-encoding", "UTF-8", "-Xlint:unchecked", "-Xlint:deprecation"),
    (Compile / doc / scalacOptions) := {
      // disable the new scaladoc feature for scala 2.12.0, might be removed in 2.12.0-1 (https://github.com/scala/scala-dev/issues/249)
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v >= 12 => Seq("-no-java-comments")
        case _                       => Seq()
      }
    },
    (Test / fork) := true,
    (Test / parallelExecution) := false,
    (Test / test / testListeners) := Nil,
    (Test / javaOptions) ++= Seq("-XX:MaxMetaspaceSize=384m", "-Xmx512m", "-Xms128m"),
    testOptions ++= Seq(
      Tests.Argument(TestFrameworks.Specs2, "showtimes"),
      Tests.Argument(TestFrameworks.JUnit, "-v")
    ),
    version := playVersion
  )

  // Versions of previous minor releases being checked for binary compatibility
  val mimaPreviousVersion: Option[String] = Some("2.8.0")

  /**
   * These settings are used by all projects that are part of the runtime, as opposed to the development mode of Play.
   */
  def playRuntimeSettings: Seq[Setting[_]] = Def.settings(
    playCommonSettings,
    mimaPreviousArtifacts := mimaPreviousVersion.map { version =>
      val cross = if (crossPaths.value) CrossVersion.binary else CrossVersion.disabled
      (organization.value %% moduleName.value % version).cross(cross)
    }.toSet,
    mimaBinaryIssueFilters ++= Seq(
      //Remove deprecated methods from Http
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#RequestImpl.this"),
      // Remove deprecated methods from HttpRequestHandler
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.http.DefaultHttpRequestHandler.filterHandler"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.http.DefaultHttpRequestHandler.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.http.JavaCompatibleHttpRequestHandler.this"),
      // Refactor params of runEvolutions (ApplicationEvolutions however is private anyway)
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.db.evolutions.ApplicationEvolutions.runEvolutions"),
      // Removed @varargs (which removed the array forwarder method)
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.libs.typedmap.DefaultTypedMap.-"),
      // Add .addAttrs(...) varargs and override methods to Request/RequestHeader and TypedMap's
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.mvc.Http#Request.addAttrs"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.mvc.Http#RequestHeader.addAttrs"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.libs.typedmap.TypedMap.+"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.libs.typedmap.TypedMap.-"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.libs.typedmap.DefaultTypedMap.-"),
      // Remove outdated (internal) method
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.libs.streams.Execution.defaultExecutionContext"),
      // Add allowEmptyFiles config to allow empty file uploads
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.http.ParserConfiguration.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.http.ParserConfiguration.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.http.ParserConfiguration.this"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.api.http.ParserConfiguration.curried"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.api.http.ParserConfiguration.tupled"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.api.http.ParserConfiguration.unapply"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.http.ParserConfiguration$"),
      // Add Result attributes
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Result.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Result.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Result.this"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.api.mvc.Result.unapply")
    ),
    (Compile / unmanagedSourceDirectories) += {
      val suffix = CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((x, y)) => s"$x.$y"
        case None         => scalaBinaryVersion.value
      }
      (Compile / sourceDirectory).value / s"scala-$suffix"
    }
  )

  /** A project that is shared between the sbt runtime and the Play runtime. */
  def PlayNonCrossBuiltProject(name: String, dir: String): Project = {
    Project(name, file(dir))
      .enablePlugins(PlaySbtLibrary)
      .settings(playRuntimeSettings: _*)
      .settings(
        autoScalaLibrary := false,
        crossPaths := false,
        crossScalaVersions := Seq(scala213)
      )
  }

  /** A project that is only used when running in development. */
  def PlayDevelopmentProject(name: String, dir: String): Project = {
    Project(name, file(dir))
      .enablePlugins(PlayLibrary)
      .settings(
        playCommonSettings,
        mimaPreviousArtifacts := Set.empty,
      )
  }

  /** A project that is in the Play runtime. */
  def PlayCrossBuiltProject(name: String, dir: String): Project = {
    Project(name, file(dir))
      .enablePlugins(PlayLibrary, AkkaSnapshotRepositories)
      .settings(playRuntimeSettings: _*)
  }

  def playScriptedSettings: Seq[Setting[_]] = Seq(
    // Don't automatically publish anything.
    // The test-sbt-plugins-* scripts publish before running the scripted tests.
    // When developing the sbt plugins:
    // * run a publishLocal in the root project to get everything
    // * run a publishLocal in the changes projects for fast feedback loops
    scriptedDependencies := (()), // drop Test/compile & publishLocal
    scriptedBufferLog := false,
    scriptedLaunchOpts ++= Seq(
      s"-Dsbt.boot.directory=${file(sys.props("user.home")) / ".sbt" / "boot"}",
      "-Xmx512m",
      "-XX:MaxMetaspaceSize=512m",
      "-XX:HeapDumpPath=/tmp/",
      "-XX:+HeapDumpOnOutOfMemoryError",
    ),
    scripted := scripted.tag(Tags.Test).evaluated,
  )

  def disablePublishing = Def.settings(
    disableNonLocalPublishing,
    // This setting will work for sbt 1, but not 0.13. For 0.13 it only affects
    // `compile` and `update` tasks.
    (publish / skip) := true,
    publishLocal := {},
  )
  def disableNonLocalPublishing = Def.settings(
    // For sbt 0.13 this is what we need to avoid publishing. These settings can
    // be removed when we move to sbt 1.
    PgpKeys.publishSigned := {},
    publish := {},
  )

  /** A project that runs in the sbt runtime. */
  def PlaySbtProject(name: String, dir: String): Project = {
    Project(name, file(dir))
      .enablePlugins(PlaySbtLibrary)
      .settings(
        playCommonSettings,
        mimaPreviousArtifacts := Set.empty,
      )
  }

  /** A project that *is* an sbt plugin. */
  def PlaySbtPluginProject(name: String, dir: String): Project = {
    Project(name, file(dir))
      .enablePlugins(PlaySbtPlugin)
      .settings(
        playCommonSettings,
        playScriptedSettings,
        (Test / fork) := false,
        mimaPreviousArtifacts := Set.empty,
      )
  }
}
