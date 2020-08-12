/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
import java.util.regex.Pattern

import bintray.BintrayPlugin.autoImport._
import com.jsuereth.sbtpgp.PgpKeys
import com.typesafe.tools.mima.core.ProblemFilters
import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.plugin.MimaKeys._
import com.typesafe.tools.mima.plugin.MimaPlugin._
import interplay._
import interplay.PlayBuildBase.autoImport._
import interplay.ScalaVersions._
import sbt._
import sbt.Keys._
import sbt.ScriptedPlugin.autoImport._
import sbtwhitesource.WhiteSourcePlugin.autoImport._

import scala.sys.process.stringToProcess
import scala.util.control.NonFatal

object BuildSettings {

  val playVersion = "2.8.2-lila_0.6"

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
    evictionWarningOptions in update := EvictionWarningOptions.default
      .withWarnTransitiveEvictions(false)
      .withWarnDirectEvictions(false)
  )

  // We are not automatically promoting artifacts to Sonatype and
  // Bintray so that we can have more control of the release process
  // and do something if somethings fails (for example, if publishing
  // a artifact times out).
  def playPublishingPromotionSettings: Seq[Setting[_]] = Seq(
    playBuildPromoteBintray := false,
    playBuildPromoteSonatype := false
  )

  val SourcesApplication = config("sources").hide


  /** These settings are used by all projects. */
  def playCommonSettings: Seq[Setting[_]] = Def.settings(
    ivyLoggingLevel := UpdateLogging.DownloadOnly,
    resolvers ++= Seq(
      // using this variant due to sbt#5405
      "sonatype-service-local-releases"
        .at("https://oss.sonatype.org/service/local/repositories/releases/content/"), // sync ScriptedTools.scala
      Resolver.typesafeRepo("releases"),
      Resolver.typesafeIvyRepo("releases"),
      Resolver.sbtPluginRepo("releases"), // weird sbt-pgp/play docs/vegemite issue
    ),
    evictionSettings,
    ivyConfigurations ++= Seq(SourcesApplication),
    javacOptions ++= Seq("-encoding", "UTF-8", "-Xlint:unchecked", "-Xlint:deprecation"),
    fork in Test := true,
    parallelExecution in Test := false,
    testListeners in (Test, test) := Nil,
    javaOptions in Test ++= Seq("-XX:MaxMetaspaceSize=384m", "-Xmx512m", "-Xms128m"),
    testOptions ++= Seq(
      Tests.Argument(TestFrameworks.Specs2, "showtimes"),
      Tests.Argument(TestFrameworks.JUnit, "-v")
    ),
    bintrayPackage := "play-sbt-plugin",
    playPublishingPromotionSettings,
    version := playVersion,
    sources in (Compile,doc) := Seq.empty
  )

  // Versions of previous minor releases being checked for binary compatibility
  val mimaPreviousVersion: Option[String] = Some("2.8.0")

  /**
   * These settings are used by all projects that are part of the runtime, as opposed to the development mode of Play.
   */
  def playRuntimeSettings: Seq[Setting[_]] = Def.settings(
    playCommonSettings,
    mimaDefaultSettings,
    mimaPreviousArtifacts := mimaPreviousVersion.map { version =>
      val cross = if (crossPaths.value) CrossVersion.binary else CrossVersion.disabled
      (organization.value %% moduleName.value % version).cross(cross)
    }.toSet,
    mimaBinaryIssueFilters ++= Seq(
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
    unmanagedSourceDirectories in Compile += {
      val suffix = CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((x, y)) => s"$x.$y"
        case None         => scalaBinaryVersion.value
      }
      (sourceDirectory in Compile).value / s"scala-$suffix"
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
        crossScalaVersions := Seq(scala212)
      )
  }

  /** A project that is only used when running in development. */
  def PlayDevelopmentProject(name: String, dir: String): Project = {
    Project(name, file(dir))
      .enablePlugins(PlayLibrary)
      .settings(
        playCommonSettings,
        (javacOptions in compile) ~= (_.map {
          case "1.8" => "1.6"
          case other => other
        }),
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
      s"-Dscala.version=$scala212",
    ),
    scripted := scripted.tag(Tags.Test).evaluated,
  )

  def disablePublishing = Def.settings(
    disableNonLocalPublishing,
    // This setting will work for sbt 1, but not 0.13. For 0.13 it only affects
    // `compile` and `update` tasks.
    skip in publish := true,
    publishLocal := {},
  )
  def disableNonLocalPublishing = Def.settings(
    // For sbt 0.13 this is what we need to avoid publishing. These settings can
    // be removed when we move to sbt 1.
    PgpKeys.publishSigned := {},
    publish := {},
    // We also don't need to track dependencies for unpublished projects
    // so we need to disable WhiteSource plugin.
    whitesourceIgnore := true
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
        fork in Test := false,
        mimaPreviousArtifacts := Set.empty,
      )
  }
}
