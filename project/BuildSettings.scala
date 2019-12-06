/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
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
import interplay.Omnidoc.autoImport._
import interplay.PlayBuildBase.autoImport._
import interplay.ScalaVersions._
import sbt._
import sbt.Keys._
import sbt.ScriptedPlugin.autoImport._
import sbtwhitesource.WhiteSourcePlugin.autoImport._

import scala.sys.process.stringToProcess
import scala.util.control.NonFatal

object BuildSettings {
  val snapshotBranch: String = {
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
    headerLicense := Some(HeaderLicense.Custom("Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>")),
    headerMappings ++= Map(
      FileType.xml  -> CommentStyle.xmlStyleBlockComment,
      FileType.conf -> CommentStyle.hashLineComment
    )
  )

  private val VersionPattern = """^(\d+).(\d+).(\d+)(-.*)?""".r

  // Versions of previous minor releases being checked for binary compatibility
  val mimaPreviousMinorReleaseVersions: Seq[String] = Seq("2.7.0")
  def mimaPreviousPatchVersions(version: String): Seq[String] = version match {
    case VersionPattern(epoch, major, minor, rest) => (0 until minor.toInt).map(v => s"$epoch.$major.$v")
    case _                                         => sys.error(s"Cannot find previous versions for $version")
  }
  def mimaPreviousVersions(version: String): Set[String] =
    mimaPreviousMinorReleaseVersions.toSet ++ mimaPreviousPatchVersions(version)

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

  val DocsApplication    = config("docs").hide
  val SourcesApplication = config("sources").hide

  /** These settings are used by all projects. */
  def playCommonSettings: Seq[Setting[_]] = Def.settings(
    fileHeaderSettings,
    homepage := Some(url("https://playframework.com")),
    ivyLoggingLevel := UpdateLogging.DownloadOnly,
    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases"),
      Resolver.typesafeRepo("releases"),
      Resolver.typesafeIvyRepo("releases"),
      Resolver.sbtPluginRepo("releases"), // weird sbt-pgp/play docs/vegemite issue
    ),
    evictionSettings,
    ivyConfigurations ++= Seq(DocsApplication, SourcesApplication),
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
    apiURL := {
      val v = version.value
      if (isSnapshot.value) {
        v match {
          case VersionPattern(epoch, major, _, _) =>
            Some(url(raw"https://www.playframework.com/documentation/$epoch.$major.x/api/scala/index.html"))
          case _ => Some(url("https://www.playframework.com/documentation/latest/api/scala/index.html"))
        }
      } else {
        Some(url(raw"https://www.playframework.com/documentation/$v/api/scala/index.html"))
      }
    },
    autoAPIMappings := true,
    apiMappings ++= {
      val scalaInstance = Keys.scalaInstance.value
      scalaInstance.libraryJars.map { libraryJar =>
        libraryJar -> url(
          raw"""http://scala-lang.org/files/archive/api/${scalaInstance.actualVersion}/index.html"""
        )
      }.toMap
    },
    apiMappings ++= {
      // Maps JDK 1.8 jar into apidoc.
      val rtJar = sys.props
        .get("sun.boot.class.path")
        .flatMap(
          cp =>
            cp.split(java.io.File.pathSeparator).collectFirst {
              case str if str.endsWith(java.io.File.separator + "rt.jar") => str
            }
        )
      rtJar match {
        case None        => Map.empty
        case Some(rtJar) => Map(file(rtJar) -> url(Docs.javaApiUrl))
      }
    },
    apiMappings ++= {
      // Finds appropriate scala apidoc from dependencies when autoAPIMappings are insufficient.
      // See the following:
      //
      // http://stackoverflow.com/questions/19786841/can-i-use-sbts-apimappings-setting-for-managed-dependencies/20919304#20919304
      // http://www.scala-sbt.org/release/docs/Howto-Scaladoc.html#Enable+manual+linking+to+the+external+Scaladoc+of+managed+dependencies
      // https://github.com/ThoughtWorksInc/sbt-api-mappings/blob/master/src/main/scala/com/thoughtworks/sbtApiMappings/ApiMappings.scala#L34

      val ScalaLibraryRegex = """^.*[/\\]scala-library-([\d\.]+)\.jar$""".r
      val JavaxInjectRegex  = """^.*[/\\]java.inject-([\d\.]+)\.jar$""".r

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

          case re @ IvyRegex(apiOrganization, apiName, jarBaseFile) if jarBaseFile.startsWith(s"$apiName-") =>
            val apiVersion = jarBaseFile.substring(apiName.length + 1, jarBaseFile.length)
            apiOrganization match {
              case "com.typesafe.akka" =>
                Some(url(raw"https://doc.akka.io/api/akka/$apiVersion/"))

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

  /**
   * These settings are used by all projects that are part of the runtime, as opposed to the development mode of Play.
   */
  def playRuntimeSettings: Seq[Setting[_]] = Def.settings(
    playCommonSettings,
    mimaDefaultSettings,
    mimaPreviousArtifacts := {
      // Binary compatibility is tested against these versions
      val previousVersions = mimaPreviousVersions(version.value)
      val cross            = if (crossPaths.value) CrossVersion.binary else CrossVersion.disabled
      previousVersions.map(v => (organization.value %% moduleName.value % v).cross(cross))
    },
    mimaPreviousArtifacts := {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v >= 13 => Set.empty // No release of Play 2.7 using Scala 2.13, yet
        case _                       => mimaPreviousArtifacts.value
      }
    },
    mimaBinaryIssueFilters ++= Seq(
      // Ignore signature problems on constructors
      ProblemFilters.exclude[IncompatibleSignatureProblem]("*.this"),
      // Scala 2.11 removed
      ProblemFilters.exclude[MissingClassProblem]("play.core.j.AbstractFilter"),
      ProblemFilters.exclude[MissingClassProblem]("play.core.j.JavaImplicitConversions"),
      ProblemFilters.exclude[MissingTypesProblem]("play.core.j.PlayMagicForJava$"),
      // Remove deprecated
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.data.validation.Constraints#ValidationPayload.getArgs"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.data.validation.Constraints#ValidationPayload.this"),
      // Remove deprecated
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.libs.typedmap.TypedMap.underlying"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.libs.concurrent.Execution"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.libs.concurrent.Execution$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.libs.concurrent.Execution$Implicits$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.libs.concurrent.Timeout"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.libs.concurrent.Timeout$"),
      // Remove deprecated
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.data.DynamicForm.bind"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.data.DynamicForm.bindFromRequest"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.data.Form.allErrors"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.data.Form.bind"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.data.Form.bindFromRequest"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.data.Form#Field.getName"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.data.Form#Field.getValue"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.data.Form.getError"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.data.Form.getGlobalError"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.libs.openid.DefaultOpenIdClient.verifiedId"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.libs.openid.OpenIdClient.verifiedId"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.RangeResults.ofFile"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.RangeResults.ofPath"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.RangeResults.ofSource"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.RangeResults.ofStream"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.test.Helpers.httpContext"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.test.Helpers.invokeWithContext"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.data.DynamicForm.bindFromRequest"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.data.Form.bindFromRequest"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.RangeResults.ofFile"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.RangeResults.ofPath"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.RangeResults.ofStream"),
      // Remove deprecated
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Play.current"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Play.maybeApplication"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Play.unsafeApplication"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.db.evolutions.Evolutions.applyFor"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.db.evolutions.Evolutions.applyFor$default$*"),
      // Remove deprecated
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.routing.JavaScriptReverseRouter.create"),
      // Renamed methods back to original name
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.mvc.Http#Cookies.get"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.mvc.Result.cookie"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.mvc.Http#Cookies.get"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.cache.DefaultAsyncCacheApi.getOptional"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.cache.DefaultSyncCacheApi.getOptional"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.cache.SyncCacheApiAdapter.getOptional"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.cache.DefaultSyncCacheApi.get"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.cache.DefaultAsyncCacheApi.get"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.cache.SyncCacheApiAdapter.get"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.cache.SyncCacheApi.get"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.cache.AsyncCacheApi.get"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.cache.SyncCacheApi.get"),
      ProblemFilters.exclude[MissingTypesProblem]("play.cache.caffeine.NamedCaffeineCache"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.cache.caffeine.NamedCaffeineCache.asMap"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.cache.caffeine.NamedCaffeineCache.get"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.cache.caffeine.NamedCaffeineCache.put"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.cache.caffeine.NamedCaffeineCache.getIfPresent"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.cache.caffeine.NamedCaffeineCache.stats"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.cache.caffeine.NamedCaffeineCache.invalidate"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.cache.caffeine.NamedCaffeineCache.invalidateAll"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.cache.caffeine.NamedCaffeineCache.invalidateAll"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.cache.caffeine.NamedCaffeineCache.policy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.cache.caffeine.NamedCaffeineCache.cleanUp"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.cache.caffeine.NamedCaffeineCache.estimatedSize"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.cache.caffeine.NamedCaffeineCache.putAll"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.cache.caffeine.NamedCaffeineCache.getAllPresent"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.cache.caffeine.NamedCaffeineCache.this"),
      ProblemFilters.exclude[MissingClassProblem]("play.cache.caffeine.CaffeineDefaultExpiry"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem](
        "play.api.libs.Files#DefaultTemporaryFileCreator#DefaultTemporaryFile.atomicMoveWithFallback"
      ),
      ProblemFilters.exclude[IncompatibleResultTypeProblem](
        "play.api.libs.Files#DefaultTemporaryFileCreator#DefaultTemporaryFile.moveTo"
      ),
      ProblemFilters.exclude[IncompatibleResultTypeProblem](
        "play.api.libs.Files#SingletonTemporaryFileCreator#SingletonTemporaryFile.atomicMoveWithFallback"
      ),
      ProblemFilters.exclude[IncompatibleResultTypeProblem](
        "play.api.libs.Files#SingletonTemporaryFileCreator#SingletonTemporaryFile.moveTo"
      ),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.libs.Files#TemporaryFile.atomicMoveWithFallback"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.libs.Files#TemporaryFile.moveTo"),
      ProblemFilters
        .exclude[IncompatibleResultTypeProblem]("play.libs.Files#DelegateTemporaryFile.atomicMoveWithFallback"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.libs.Files#DelegateTemporaryFile.moveTo"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.libs.Files#TemporaryFile.atomicMoveWithFallback"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.libs.Files#TemporaryFile.moveTo"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.libs.Files#TemporaryFile.atomicMoveWithFallback"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.libs.Files#TemporaryFile.moveTo"),
      // Add fileName param (with default value) to Scala's sendResource(...) method
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Results#Status.sendResource"),
      // Removed internally-used subclass
      ProblemFilters.exclude[MissingClassProblem]("org.jdbcdslog.LogSqlDataSource"),
      // play.api.Logger$ no longer extends play.api.Logger
      ProblemFilters.exclude[MissingTypesProblem]("play.api.Logger$"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Logger.debug"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Logger.enabled"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Logger.error"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Logger.forMode"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Logger.info"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Logger.isDebugEnabled"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Logger.isErrorEnabled"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Logger.isInfoEnabled"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Logger.isTraceEnabled"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Logger.isWarnEnabled"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Logger.trace"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Logger.warn"),
      // Dropped an internal method
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.asScalaList"),
      // Add queryString method to RequestHeader interface
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.mvc.Http#RequestHeader.queryString"),
      // Add getCookie method to RequestHeader interface
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.mvc.Http#RequestHeader.getCookie"),
      // Removed deprecated method Database.toScala()
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.db.Database.toScala"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.db.DefaultDatabase.toScala"),
      // No longer extends AssetsBuilder
      ProblemFilters.exclude[MissingTypesProblem]("controllers.Assets$"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("controllers.Assets.at"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("controllers.Assets.at"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("controllers.Assets.versioned"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("controllers.Assets.versioned"),
      // TODO: document this exclude
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.JavaParsers.parse"),
      // TODO: document this exclude
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#MultipartFormData#FilePart.getFile"),
      // Switch one of these from returning scala.collection.Seq to scala.collection.immutable.Seq (I think)
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("views.html.helper.options.apply"),
      // No longer extends CookieBaker
      ProblemFilters.exclude[MissingTypesProblem]("play.api.mvc.Flash$"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Flash.COOKIE_NAME"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Flash.config"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Flash.cookieSigner"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Flash.decode"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Flash.decodeCookieToMap"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Flash.decodeFromCookie"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Flash.deserialize"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Flash.discard"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Flash.domain"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Flash.encode"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Flash.encodeAsCookie"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Flash.httpOnly"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Flash.isSigned"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Flash.maxAge"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Flash.path"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Flash.sameSite"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Flash.secure"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Flash.serialize"),
      // No longer extends CookieBaker
      ProblemFilters.exclude[MissingTypesProblem]("play.api.mvc.Session$"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Session.COOKIE_NAME"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Session.config"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Session.decode"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Session.decodeCookieToMap"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Session.decodeFromCookie"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Session.deserialize"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Session.discard"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Session.domain"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Session.encode"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Session.encodeAsCookie"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Session.httpOnly"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Session.isSigned"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Session.jwtCodec"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Session.maxAge"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Session.path"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Session.sameSite"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Session.secure"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Session.serialize"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Session.signedCodec"),
      // Remove deprecated
      ProblemFilters.exclude[MissingClassProblem]("play.api.http.LazyHttpErrorHandler"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.http.LazyHttpErrorHandler$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.libs.Crypto"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.libs.Crypto$"),
      // Removed deprecated BodyParsers.urlFormEncoded method
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.DefaultPlayBodyParsers.urlFormEncoded"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.PlayBodyParsers.urlFormEncoded"),
      // Remove deprecated
      ProblemFilters.exclude[MissingClassProblem]("play.api.mvc.Action$"),
      // These return Seq[Any] instead of Seq[String] #9385
      ProblemFilters.exclude[IncompatibleSignatureProblem]("views.html.helper.FieldElements.infos"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("views.html.helper.FieldElements.errors"),
      // Removed deprecated TOO_MANY_REQUEST field
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.http.Status.TOO_MANY_REQUEST"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.AbstractController.TOO_MANY_REQUEST"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.AbstractController.TooManyRequest"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.ControllerHelpers.TOO_MANY_REQUEST"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.ControllerHelpers.TooManyRequest"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.MessagesAbstractController.TOO_MANY_REQUEST"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.MessagesAbstractController.TooManyRequest"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Results.TooManyRequest"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.Helpers.TOO_MANY_REQUEST"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("controllers.AssetsBuilder.TOO_MANY_REQUEST"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("controllers.AssetsBuilder.TooManyRequest"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("controllers.Default.TOO_MANY_REQUEST"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("controllers.Default.TooManyRequest"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("controllers.ExternalAssets.TOO_MANY_REQUEST"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("controllers.ExternalAssets.TooManyRequest"),
      // Removed deprecated methods that depend on Java's Http.Context
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Controller.changeLang"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Controller.clearLang"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Controller.ctx"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Controller.flash"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Controller.lang"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Controller.request"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Controller.response"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Controller.session"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Controller.TODO"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Security#Authenticator.getUsername"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Security#Authenticator.onUnauthorized"),
      // No static forwarders for non-public overloads
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.libs.concurrent.ActorSystemProvider.start"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.libs.concurrent.ActorSystemProvider.start"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Action.async"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Action.invokeBlock"),
      // Removed Java's JPAApi thread-local
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.db.jpa.DefaultJPAApi.em"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.db.jpa.DefaultJPAApi#JPAApiProvider.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.db.jpa.DefaultJPAApi.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.db.jpa.JPAApi.em"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.db.jpa.DefaultJPAApi.withTransaction"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.db.jpa.JPAApi.withTransaction"),
      ProblemFilters.exclude[MissingClassProblem]("play.db.jpa.JPAEntityManagerContext"),
      ProblemFilters.exclude[MissingClassProblem]("play.db.jpa.Transactional"),
      ProblemFilters.exclude[MissingClassProblem]("play.db.jpa.TransactionalAction"),
      // Removed deprecated methods PathPatternMatcher.routeAsync and PathPatternMatcher.routeTo
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.routing.RoutingDsl#PathPatternMatcher.routeAsync"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.routing.RoutingDsl#PathPatternMatcher.routeTo"),
      // Tweaked generic signature - false positive
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.test.Helpers.fakeApplication"),
      // Remove constructor from private class
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.routing.RouterBuilderHelper.this"),
      // Remove Http.Context and Http.Response
      ProblemFilters.exclude[DirectAbstractMethodProblem]("play.mvc.Action.call"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.HttpExecutionContext.httpContext_="),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.HttpExecutionContext.httpContext"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.HttpExecutionContext.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.JavaAction.createJavaContext"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.JavaAction.createResult"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.JavaAction.invokeWithContext"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.JavaAction.withContext"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.JavaHelpers.createJavaContext"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.JavaHelpers.createResult"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.JavaHelpers.invokeWithContext"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.JavaHelpers.withContext"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.PlayMagicForJava.implicitJavaLang"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.PlayMagicForJava.implicitJavaMessages"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.PlayMagicForJava.javaRequest2ScalaRequest"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.PlayMagicForJava.requestHeader"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Action.call"),
      ProblemFilters.exclude[MissingClassProblem]("play.mvc.Http$Context"),
      ProblemFilters.exclude[MissingClassProblem]("play.mvc.Http$Context$Implicit"),
      ProblemFilters.exclude[MissingClassProblem]("play.mvc.Http$Response"),
      ProblemFilters.exclude[MissingClassProblem]("play.mvc.Http$WrappedContext"),
      ProblemFilters.exclude[ReversedAbstractMethodProblem]("play.mvc.Action.call"),
      // Made these two utility and constants classes final
      ProblemFilters.exclude[FinalClassProblem]("play.libs.XML$Constants"),
      ProblemFilters.exclude[FinalClassProblem]("play.libs.XML"),
      // Make Java's Session and Flash immutable
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.clear"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.compute"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.computeIfAbsent"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.computeIfPresent"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.containsKey"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.containsValue"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.entrySet"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.forEach"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.getOrDefault"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.isEmpty"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.keySet"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.merge"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.put"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.putAll"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.putIfAbsent"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.remove"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.replace"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.replaceAll"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.size"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Flash.values"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.clear"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.compute"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.computeIfAbsent"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.computeIfPresent"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.containsKey"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.containsValue"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.entrySet"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.forEach"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.getOrDefault"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.isEmpty"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.keySet"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.merge"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.put"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.putAll"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.putIfAbsent"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.remove"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.replace"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.replaceAll"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.size"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Session.values"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Http#Flash.get"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Http#Flash.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Http#Session.get"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Http#Session.this"),
      ProblemFilters.exclude[MissingFieldProblem]("play.mvc.Http#Flash.isDirty"),
      ProblemFilters.exclude[MissingFieldProblem]("play.mvc.Http#Session.isDirty"),
      ProblemFilters.exclude[MissingTypesProblem]("play.mvc.Http$Flash"),
      ProblemFilters.exclude[MissingTypesProblem]("play.mvc.Http$Session"),
      ProblemFilters.exclude[InaccessibleMethodProblem]("java.lang.Object.clone"),
      // Taught Scala.asScala to covariantly widen seq element type
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.libs.Scala.asScala"),
      // Replaced raw type usages
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.mvc.BodyParser#Of.value"),
      // Add configuration for max-age of language-cookie
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.i18n.DefaultMessagesApi.this"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.i18n.MessagesApi.langCookieMaxAge"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.Helpers.stubMessagesApi"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.StubMessagesFactory.stubMessagesApi"),
      // Use Akka Jackson ObjectMapper
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.core.ObjectMapperComponents.actorSystem"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.ObjectMapperProvider.this"),
      // Add configuration for temporary file directory
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.libs.Files#DefaultTemporaryFileCreator.this"),
      // Return type of filename function parameter changed from String to Option[String]
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.api.mvc.Results#Status.sendFile"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.api.mvc.Results#Status.sendFile$default$3"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.api.mvc.Results#Status.sendPath"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.api.mvc.Results#Status.sendPath$default$3"),
      // Add contentType param (which defaults to None) to Results.chunked(...) like Results.streamed(...) already has
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Results#Status.chunked"),
      // Netty's request handler needs maxContentLength to check if request size exceeds allowed configured value
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.netty.PlayRequestHandler.this"),
      // Removing NoMaterializer
      ProblemFilters.exclude[MissingClassProblem]("play.api.test.NoMaterializer$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.test.NoMaterializer"),
      // Fix "memory leak" in DelegatingMultipartFormDataBodyParser
      ProblemFilters
        .exclude[IncompatibleSignatureProblem]("play.mvc.BodyParser#DelegatingMultipartFormDataBodyParser.apply"),
      // Update mima to 0.6.0
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Security#Authenticator.getUsername"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Security#Authenticator.onUnauthorized"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Action.call"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.db.jpa.JPAApi.withTransaction"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.db.jpa.JPAApi.withTransaction"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.db.jpa.JPAApi.withTransaction"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.db.jpa.DefaultJPAApi.withTransaction"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.db.jpa.DefaultJPAApi.withTransaction"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.db.jpa.DefaultJPAApi.withTransaction"),
      // Add treshold to GzipFilter
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.gzip.GzipFilterConfig.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.gzip.GzipFilterConfig.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.gzip.GzipFilterConfig.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.gzip.GzipFilter.this"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.gzip.GzipFilterConfig.apply$default$3"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.gzip.GzipFilterConfig.apply$default$4"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.gzip.GzipFilterConfig.copy$default$3"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.gzip.GzipFilterConfig.copy$default$4"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.gzip.GzipFilterConfig.<init>$default$3"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.gzip.GzipFilterConfig.<init>$default$4"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.gzip.GzipFilter.<init>$default$3"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.gzip.GzipFilter.<init>$default$4"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.filters.gzip.GzipFilterConfig.unapply"),
      // Remove deprecated Messages implicits
      ProblemFilters.exclude[MissingClassProblem]("play.api.i18n.Messages$Implicits$"),
      // Remove deprecated internal API
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.AkkaHttpServer.executeAction"),
      // Remove deprecated security methods
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Security.Authenticated"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Security.username"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Security#AuthenticatedBuilder.apply"),
      // Remove unneeded implicit materializer
      ProblemFilters
        .exclude[DirectMissingMethodProblem]("play.core.server.netty.NettyModelConversion.convertRequestBody"),
      // Remove deprecated application methods
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.DefaultApplication.getFile"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.DefaultApplication.getExistingFile"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.DefaultApplication.resource"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.DefaultApplication.resourceAsStream"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Application.getFile"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Application.getExistingFile"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Application.resource"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Application.resourceAsStream"),
      // Remove deprecated ApplicationProvider
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.ApplicationProvider.current"),
      // Remove deprecated sqlDate
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.data.Forms.sqlDate"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.data.Forms.sqlDate$default$2"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.data.format.Formats.sqlDateFormat"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.data.format.Formats.sqlDateFormat$default$2"),
      // Remove deprecated Default singleton object
      ProblemFilters.exclude[MissingClassProblem]("controllers.Default$"),
      // Remove deprecated AkkaHttpServer methods
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.AkkaHttpServer.executeAction"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.AkkaHttpServer.this"),
      // Remove deprecated Execution methods
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.Execution.internalContext"),
      // Remove deprecated BodyParsers trait
      ProblemFilters.exclude[MissingClassProblem]("play.api.mvc.BodyParsers"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.mvc.Controller"),
      ProblemFilters.exclude[IncompatibleTemplateDefProblem]("play.api.mvc.BodyParsers"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.mvc.BodyParsers$"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.BodyParsers.parse"),
      // Remove deprecated Controller class
      ProblemFilters.exclude[MissingClassProblem]("play.api.mvc.Controller"),
      // Remove deprecated Configuration methods
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getInt"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getString"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getBoolean"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getMilliseconds"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getNanoseconds"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getBytes"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getConfig"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getDouble"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getLong"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getNumber"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getString$default$2"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getBooleanList"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getBooleanSeq"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getBytesList"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getBytesSeq"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getConfigList"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getConfigSeq"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getDoubleList"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getDoubleSeq"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getIntList"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getIntSeq"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getList"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getLongList"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getLongSeq"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getMillisecondsList"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getMillisecondsSeq"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getNanosecondsList"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getNanosecondsSeq"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getNumberList"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getNumberSeq"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getObjectList"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getObjectSeq"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getStringList"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getStringSeq"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Configuration.getObject"),
      // More deprecated removals
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.libs.typedmap.TypedKey.underlying"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.TestServer.port"),
      // Remove package private
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.AkkaHttpServer.runAction"),
      // Add SSLContext to SSLEngineProvider
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.server.SSLEngineProvider.sslContext"),
      ProblemFilters
        .exclude[DirectMissingMethodProblem]("play.core.server.ssl.DefaultSSLEngineProvider.createSSLContext"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.ssl.noCATrustManager.nullArray"),
      // Move ServerEndpoints definition to Server implementation
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.ServerEndpointRecipe.AkkaHttp11Encrypted"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.ServerEndpointRecipe.AkkaHttp11Plaintext"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.ServerEndpointRecipe.AkkaHttp20Encrypted"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.ServerEndpointRecipe.AkkaHttp20Plaintext"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.ServerEndpointRecipe.AllRecipes"),
      ProblemFilters
        .exclude[DirectMissingMethodProblem]("play.api.test.ServerEndpointRecipe.AllRecipesIncludingExperimental"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.ServerEndpointRecipe.Netty11Encrypted"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.ServerEndpointRecipe.Netty11Plaintext"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.ServerEndpoint.expectedHttpVersions"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.ServerEndpoint.expectedServerAttr"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.ServerEndpoints.andThen"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.ServerEndpoints.compose"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.core.server.ServerEndpoint.apply"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.core.server.ServerEndpoint.copy"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.core.server.ServerEndpoint.copy$default$7"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.core.server.ServerEndpoint.ssl"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.core.server.ServerEndpoint.unapply"),
      ProblemFilters.exclude[MissingClassProblem]("play.core.server.ServerEndpoint$ClientSsl"),
      ProblemFilters.exclude[MissingClassProblem]("play.core.server.ServerEndpoint$ClientSsl$"),
      ProblemFilters.exclude[MissingTypesProblem]("play.core.server.ServerEndpoints$"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.http.HttpProtocol.HTTP_2_0"),
      ProblemFilters.exclude[ReversedMissingMethodProblem](
        "play.api.http.HttpProtocol.play$api$http$HttpProtocol$_setter_$HTTP_2_0_="
      ),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.core.server.Server.serverEndpoints"),
      // Move from play-core to play-configuration and play-utils
      ProblemFilters.exclude[MissingClassProblem]("play.Environment"),
      ProblemFilters.exclude[MissingClassProblem]("play.Mode"),
      ProblemFilters.exclude[MissingClassProblem]("play.libs.F$Tuple3"),
      ProblemFilters.exclude[MissingClassProblem]("play.libs.F$Tuple"),
      ProblemFilters.exclude[MissingClassProblem]("play.libs.F$Tuple5"),
      ProblemFilters.exclude[MissingClassProblem]("play.libs.F$Function4"),
      ProblemFilters.exclude[MissingClassProblem]("play.libs.Scala"),
      ProblemFilters.exclude[MissingClassProblem]("play.libs.F$Either"),
      ProblemFilters.exclude[MissingClassProblem]("play.libs.F$LazySupplier"),
      ProblemFilters.exclude[MissingClassProblem]("play.libs.F$Function3"),
      ProblemFilters.exclude[MissingClassProblem]("play.libs.F$Tuple4"),
      ProblemFilters.exclude[MissingClassProblem]("play.libs.F"),
      ProblemFilters.exclude[MissingClassProblem]("play.libs.F$PromiseTimeoutException"),
      ProblemFilters.exclude[MissingClassProblem]("play.utils.Conversions$"),
      ProblemFilters.exclude[MissingClassProblem]("play.utils.PlayIO$"),
      ProblemFilters.exclude[MissingClassProblem]("play.utils.Conversions"),
      ProblemFilters.exclude[MissingClassProblem]("play.utils.PlayIO"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Environment$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Mode$Dev$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Configuration"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Environment"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Mode$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.DefaultMarkerContext"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Logging"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Mode$Prod$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Configuration$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.ConfigLoader$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.MarkerContexts"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.LoggerLike"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Logger"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.ConfigLoader"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.MarkerContexts$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Mode$Test$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.MarkerContext"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.MarkerContexts$SecurityMarkerContext$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Mode"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.LowPriorityMarkerContextImplicits"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Logger$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.MarkerContext$"),
    ),
    unmanagedSourceDirectories in Compile += {
      val suffix = CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((x, y)) => s"$x.$y"
        case None         => scalaBinaryVersion.value
      }
      (sourceDirectory in Compile).value / s"scala-$suffix"
    },
    // Argument for setting size of permgen space or meta space for all forked processes
    Docs.apiDocsInclude := true
  )

  /** A project that is shared between the sbt runtime and the Play runtime. */
  def PlayNonCrossBuiltProject(name: String, dir: String): Project = {
    Project(name, file(dir))
      .enablePlugins(PlaySbtLibrary, AutomateHeaderPlugin)
      .settings(playRuntimeSettings: _*)
      .settings(omnidocSettings: _*)
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
      .settings(omnidocSettings: _*)
      .settings(
        scalacOptions += "-target:jvm-1.8"
      )
  }

  def omnidocSettings: Seq[Setting[_]] = Def.settings(
    Omnidoc.projectSettings,
    omnidocSnapshotBranch := snapshotBranch,
    omnidocPathPrefix := ""
  )

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
