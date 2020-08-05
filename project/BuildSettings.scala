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
import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import de.heikoseeberger.sbtheader.FileType
import de.heikoseeberger.sbtheader.CommentStyle
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._
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

  val playVersion = "2.8.2-lila_0.1"

  /** File header settings.  */
  private def fileUriRegexFilter(pattern: String): FileFilter = new FileFilter {
    val compiledPattern = Pattern.compile(pattern)
    override def accept(pathname: File): Boolean = {
      val uriString = pathname.toURI.toString
      compiledPattern.matcher(uriString).matches()
    }
  }

  val fileHeaderSettings = Seq(
    excludeFilter in (Compile, headerSources) := HiddenFileFilter ||
      fileUriRegexFilter(".*/cookie/encoding/.*") || fileUriRegexFilter(".*/inject/SourceProvider.java$") ||
      fileUriRegexFilter(".*/libs/reflect/.*"),
    headerLicense := Some(HeaderLicense.Custom("Copyright (C) Lightbend Inc. <https://www.lightbend.com>")),
    headerMappings ++= Map(
      FileType.xml  -> CommentStyle.xmlStyleBlockComment,
      FileType.conf -> CommentStyle.hashLineComment
    )
  )

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
    fileHeaderSettings,
    homepage := Some(url("https://playframework.com")),
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
    scalacOptions in (Compile, doc) := {
      // disable the new scaladoc feature for scala 2.12.0, might be removed in 2.12.0-1 (https://github.com/scala/scala-dev/issues/249)
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v >= 12 => Seq("-no-java-comments")
        case _                       => Seq()
      }
    },
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
    version := playVersion
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
      // Add withExtraServerConfiguration() to append server config to endpoints
      ProblemFilters
        .exclude[ReversedMissingMethodProblem]("play.api.test.ServerEndpointRecipe.withExtraServerConfiguration"),
      // Support custom name of play_evolutions(_lock) table via metaTable config
      ProblemFilters
        .exclude[DirectMissingMethodProblem]("play.api.db.evolutions.DefaultEvolutionsDatasourceConfig.apply"),
      ProblemFilters
        .exclude[DirectMissingMethodProblem]("play.api.db.evolutions.DefaultEvolutionsDatasourceConfig.copy"),
      ProblemFilters
        .exclude[DirectMissingMethodProblem]("play.api.db.evolutions.DefaultEvolutionsDatasourceConfig.this"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem](
        "play.api.db.evolutions.DefaultEvolutionsDatasourceConfig.copy$default$3"
      ),
      ProblemFilters
        .exclude[IncompatibleSignatureProblem]("play.api.db.evolutions.DefaultEvolutionsDatasourceConfig.curried"),
      ProblemFilters
        .exclude[IncompatibleSignatureProblem]("play.api.db.evolutions.DefaultEvolutionsDatasourceConfig.tupled"),
      ProblemFilters
        .exclude[IncompatibleSignatureProblem]("play.api.db.evolutions.DefaultEvolutionsDatasourceConfig.unapply"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.db.evolutions.DefaultEvolutionsDatasourceConfig$"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.db.evolutions.DefaultEvolutionsApi.applyFor"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.db.evolutions.EvolutionsApi.applyFor"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.db.evolutions.EvolutionsApi.evolve"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.db.evolutions.EvolutionsApi.resetScripts"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.db.evolutions.EvolutionsApi.resolve"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.db.evolutions.EvolutionsApi.scripts"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.db.evolutions.Evolutions.applyEvolutions"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.db.evolutions.Evolutions.cleanupEvolutions"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.db.evolutions.Evolutions.withEvolutions"),
      ProblemFilters
        .exclude[ReversedMissingMethodProblem]("play.api.db.evolutions.EvolutionsDatasourceConfig.metaTable"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.db.evolutions.OfflineEvolutions.applyScript"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.db.evolutions.OfflineEvolutions.resolve"),
      // Add Result attributes
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Result.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Result.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Result.this"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.api.mvc.Result.unapply"),
      // Config which sets Caffeine's internal executor, also switched to trampoline where useful
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.cache.caffeine.CacheManagerProvider.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.cache.caffeine.CaffeineCacheApi.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.cache.caffeine.CaffeineCacheManager.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.cache.caffeine.CaffeineParser.from"),
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
      .enablePlugins(PlaySbtLibrary, AutomateHeaderPlugin)
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
      .enablePlugins(PlayLibrary, AutomateHeaderPlugin)
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
      .enablePlugins(PlayLibrary, AutomateHeaderPlugin, AkkaSnapshotRepositories)
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
      .enablePlugins(PlaySbtLibrary, AutomateHeaderPlugin)
      .settings(
        playCommonSettings,
        mimaPreviousArtifacts := Set.empty,
      )
  }

  /** A project that *is* an sbt plugin. */
  def PlaySbtPluginProject(name: String, dir: String): Project = {
    Project(name, file(dir))
      .enablePlugins(PlaySbtPlugin, AutomateHeaderPlugin)
      .settings(
        playCommonSettings,
        playScriptedSettings,
        fork in Test := false,
        mimaPreviousArtifacts := Set.empty,
      )
  }
}
