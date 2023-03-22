/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import java.util.regex.Pattern

import scala.sys.process.stringToProcess
import scala.util.control.NonFatal

import sbt._
import sbt.Keys._
import sbt.ScriptedPlugin.autoImport._

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.ProblemFilters
import com.typesafe.tools.mima.plugin.MimaKeys._
import com.typesafe.tools.mima.plugin.MimaPlugin
import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import de.heikoseeberger.sbtheader.CommentBlockCreator
import de.heikoseeberger.sbtheader.CommentStyle
import de.heikoseeberger.sbtheader.FileType
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.HeaderPattern.commentBetween
import de.heikoseeberger.sbtheader.LineCommentCreator
import interplay._
import interplay.Omnidoc.autoImport._
import interplay.PlayBuildBase.autoImport._
import interplay.ScalaVersions._
import xerial.sbt.Sonatype.autoImport.sonatypeProfileName

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

  /** File header settings. */
  private def fileUriRegexFilter(pattern: String): FileFilter = new FileFilter {
    val compiledPattern = Pattern.compile(pattern)
    override def accept(pathname: File): Boolean = {
      val uriString = pathname.toURI.toString
      compiledPattern.matcher(uriString).matches()
    }
  }

  val fileHeaderSettings = Seq(
    (Compile / headerSources / excludeFilter) := HiddenFileFilter ||
      fileUriRegexFilter(".*/cookie/encoding/.*") || fileUriRegexFilter(".*/inject/SourceProvider.java$") ||
      fileUriRegexFilter(".*/libs/reflect/.*"),
    headerLicense := Some(
      HeaderLicense.Custom(
        """Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>""".stripMargin
      )
    ),
    headerMappings ++= Map(
      FileType.xml                 -> CommentStyle.xmlStyleBlockComment,
      FileType.conf                -> CommentStyle.hashLineComment,
      FileType("sbt")              -> HeaderCommentStyle.cppStyleLineComment,
      FileType("routes")           -> HeaderCommentStyle.hashLineComment,
      FileType("", None, "routes") -> HeaderCommentStyle.hashLineComment,
      FileType("default")          -> HeaderCommentStyle.hashLineComment,
      FileType("properties")       -> HeaderCommentStyle.hashLineComment,
      FileType("js")               -> HeaderCommentStyle.cStyleBlockComment,
      FileType("less")             -> HeaderCommentStyle.cStyleBlockComment,
      FileType("md")               -> CommentStyle(new LineCommentCreator("<!---", "-->"), commentBetween("<!---", "*", "-->")),
      FileType("html") -> CommentStyle(
        new CommentBlockCreator("<!--", "  ~", "  -->"),
        commentBetween("<!--", "*", " -->")
      ),
    ),
  )

  private val VersionPattern = """^(\d+).(\d+).(\d+)(-.*)?""".r

  def evictionSettings: Seq[Setting[_]] = Seq(
    // This avoids a lot of dependency resolution warnings to be showed.
    (update / evictionWarningOptions) := EvictionWarningOptions.default
      .withWarnTransitiveEvictions(false)
      .withWarnDirectEvictions(false)
  )

  val DocsApplication    = config("docs").hide
  val SourcesApplication = config("sources").hide

  /** These settings are used by all projects. */
  def playCommonSettings: Seq[Setting[_]] = Def.settings(
    // overwrite Interplay settings to new Sonatype profile
    sonatypeProfileName := "com.typesafe.play",
    fileHeaderSettings,
    ivyLoggingLevel := UpdateLogging.DownloadOnly,
    resolvers ++= Resolver.sonatypeOssRepos("releases"), // sync ScriptedTools.scala
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases"),
      Resolver.typesafeIvyRepo("releases"),
      Resolver.sbtPluginRepo("releases"), // weird sbt-pgp/play docs/vegemite issue
    ),
    evictionSettings,
    ivyConfigurations ++= Seq(DocsApplication, SourcesApplication),
    javacOptions ++= Seq("-encoding", "UTF-8", "-Xlint:unchecked", "-Xlint:deprecation"),
    scalacOptions ++= Seq("-release:11"),
    (Compile / doc / scalacOptions) := {
      // disable the new scaladoc feature for scala 2.12+ (https://github.com/scala/scala-dev/issues/249 and https://github.com/scala/bug/issues/11340)
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v >= 12 => Seq("-no-java-comments")
        case _                       => Seq()
      }
    },
    (Test / fork)                 := true,
    (Test / parallelExecution)    := false,
    (Test / test / testListeners) := Nil,
    (Test / javaOptions) ++= Seq("-XX:MaxMetaspaceSize=384m", "-Xmx512m", "-Xms128m"),
    testOptions ++= Seq(
      Tests.Argument(TestFrameworks.Specs2, "showtimes"),
      Tests.Argument(TestFrameworks.JUnit, "-v")
    ),
    version ~= { v =>
      v +
        sys.props.get("akka.version").map("-akka-" + _).getOrElse("") +
        sys.props.get("akka.http.version").map("-akka-http-" + _).getOrElse("")
    },
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
        .flatMap(cp =>
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
        jar <- (Compile / doc / dependencyClasspath).value.toSet ++ (Test / doc / dependencyClasspath).value
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
      } yield fullyFile -> url)(collection.breakOut(Map.canBuildFrom))
    }
  )

  // Versions of previous minor releases being checked for binary compatibility
  val mimaPreviousVersion: Option[String] = Some("2.8.0")

  /**
   * These settings are used by all projects that are part of the runtime, as opposed to the development mode of Play.
   */
  def playRuntimeSettings: Seq[Setting[_]] = Def.settings(
    playCommonSettings,
    mimaPreviousArtifacts := {
      if (scalaBinaryVersion.value == "3") Set.empty[ModuleID]
      else
        mimaPreviousVersion.map { version =>
          val cross = if (crossPaths.value) CrossVersion.binary else CrossVersion.disabled
          (organization.value %% moduleName.value % version).cross(cross)
        }.toSet
    },
    mimaBinaryIssueFilters ++= Seq(
      // fix typo Commited => Committed https://github.com/playframework/playframework/pull/11608
      ProblemFilters.exclude[MissingClassProblem]("play.api.db.TransactionIsolationLevel$ReadCommited$"),
      ProblemFilters.exclude[MissingFieldProblem]("play.db.TransactionIsolationLevel.ReadCommited"),
      // Refactor constructor to use StandaloneAhcWSClient
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.libs.ws.ahc.AhcWSClientProvider.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.libs.ws.ahc.AhcWSModule#AhcWSClientProvider.this"),
      // Remove deprecated methods from Http
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
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.libs.typedmap.TypedMap.removed"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.libs.typedmap.TypedMap.updated"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.mvc.Http#Request.addAttrs"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.mvc.Http#RequestHeader.addAttrs"),
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
      // Remove deprecated FakeKeyStore
      ProblemFilters.exclude[MissingClassProblem]("play.core.server.ssl.FakeKeyStore$"),
      ProblemFilters.exclude[MissingClassProblem]("play.core.server.ssl.FakeKeyStore"),
      // Limit JSON parsing resources
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.data.FormUtils.fromJson$default$1"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.data.FormUtils.fromJson"), // is private
      // Honour maxMemoryBuffer when binding Json to form
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.data.Form.bindFromRequest"),
      ProblemFilters.exclude[ReversedMissingMethodProblem](
        "play.api.mvc.PlayBodyParsers.play$api$mvc$PlayBodyParsers$_setter_$defaultFormBinding_="
      ),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.mvc.PlayBodyParsers.defaultFormBinding"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.mvc.PlayBodyParsers.formBinding$default$1"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.mvc.PlayBodyParsers.formBinding"),
      // fix types on Json parsing limits
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.data.Form.bind"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.data.Form.bindFromRequest"),
      ProblemFilters.exclude[ReversedMissingMethodProblem](
        "play.api.mvc.BaseControllerHelpers.play$api$mvc$BaseControllerHelpers$_setter_$defaultFormBinding_="
      ),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.mvc.BaseControllerHelpers.defaultFormBinding"),
      // Add UUID PathBindableExtractors
      ProblemFilters.exclude[ReversedMissingMethodProblem](
        "play.api.routing.sird.PathBindableExtractors.play$api$routing$sird$PathBindableExtractors$_setter_$uuid_="
      ),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.routing.sird.PathBindableExtractors.uuid"),
      // Upgrading JJWT
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.http.JWTConfigurationParser.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.http.SecretConfiguration.SHORTEST_SECRET_LENGTH"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.http.SecretConfiguration.SHORT_SECRET_LENGTH"),
      // Removing Jetty ALPN Agent
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.PlayVersion.jettyAlpnAgentVersion"),
      // Remove obsolete CertificateGenerator
      ProblemFilters.exclude[MissingClassProblem]("play.core.server.ssl.CertificateGenerator"),
      ProblemFilters.exclude[MissingClassProblem]("play.core.server.ssl.CertificateGenerator$"),
      // Add SameSite to DiscardingCookie
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.DiscardingCookie.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.DiscardingCookie.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.DiscardingCookie.this"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.api.mvc.DiscardingCookie.curried"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.api.mvc.DiscardingCookie.tupled"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("play.api.mvc.DiscardingCookie.unapply"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.mvc.DiscardingCookie$"),
      // Variable substitution in evolutions scripts
      ProblemFilters
        .exclude[ReversedMissingMethodProblem]("play.api.db.evolutions.EvolutionsDatasourceConfig.substitutionsSuffix"),
      ProblemFilters
        .exclude[ReversedMissingMethodProblem]("play.api.db.evolutions.EvolutionsDatasourceConfig.substitutionsPrefix"),
      ProblemFilters.exclude[ReversedMissingMethodProblem](
        "play.api.db.evolutions.EvolutionsDatasourceConfig.substitutionsMappings"
      ),
      ProblemFilters
        .exclude[ReversedMissingMethodProblem]("play.api.db.evolutions.EvolutionsDatasourceConfig.substitutionsEscape"),
      // Remove routeAndCall(...) methods that depended on StaticRoutesGenerator
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.test.Helpers.routeAndCall"),
      // Remove CrossScala (parent class of play.libs.Scala)
      ProblemFilters.exclude[MissingTypesProblem]("play.libs.Scala"),
      // Renaming clearLang to withoutLang
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.i18n.MessagesApi.withoutLang"),
      // Support for Websockets ping/pong
      ProblemFilters
        .exclude[DirectMissingMethodProblem]("play.core.server.common.WebSocketFlowHandler#RawMessage.copy"),
      ProblemFilters
        .exclude[DirectMissingMethodProblem]("play.core.server.common.WebSocketFlowHandler#RawMessage.this"),
      ProblemFilters.exclude[MissingTypesProblem]("play.core.server.common.WebSocketFlowHandler$RawMessage$"),
      ProblemFilters
        .exclude[DirectMissingMethodProblem]("play.core.server.common.WebSocketFlowHandler#RawMessage.apply"),
      // Add FilePart.transformRefToBytes() method
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.MultipartFormData#FilePart.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.MultipartFormData#FilePart.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.MultipartFormData#FilePart.copy"),
      // Move from play-core to play-configuration
      ProblemFilters.exclude[MissingClassProblem]("play.Environment"),
      ProblemFilters.exclude[MissingClassProblem]("play.Mode"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Environment$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Mode$Dev$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Configuration"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Environment"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Mode$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Mode$Prod$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Configuration$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.ConfigLoader$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.ConfigLoader"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Mode$Test$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.Mode"),
      // Renamed methods (method that were deprecated in 2.8.x back to their original name)
      // Also, for consistency in general drop the get... prefix for methods in Java's RequestHeader/-Builder since
      // all other methods in these classes don't use that
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.core.j.RequestHeaderImpl.cookie"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.mvc.Http#RequestHeader.cookie"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.mvc.Http#RequestHeader.cookie"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.mvc.Http#RequestHeader.headers"),
      // Scala3 compilation
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.libs.ws.WSRequest.addHttpHeaders"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.libs.ws.WSRequest.addQueryStringParameters"),
      // Need to use unapplySeq instead of unapply because of Scala 3 macro not able to handle multiple params for extractor (q, q_s,...) otherwise
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.routing.sird.OptionalQueryStringParameter.unapply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.routing.sird.QueryStringParameterExtractor.unapply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.routing.sird.RequiredQueryStringParameter.unapply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.routing.sird.SeqQueryStringParameter.unapply"),
      ProblemFilters.exclude[ReversedMissingMethodProblem](
        "play.api.routing.sird.QueryStringParameterExtractor.unapplySeq"
      ),
    ),
    (Compile / unmanagedSourceDirectories) += {
      val suffix = CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((x, y)) => s"$x.$y"
        case None         => scalaBinaryVersion.value
      }
      (Compile / sourceDirectory).value / s"scala-$suffix"
    },
    Docs.apiDocsInclude := true
  )

  /**
   * A project that is shared between the sbt runtime and the Play runtime.
   * Such a shared project needs to be a pure Java project, not containing Scala files.
   * That's because sbt 1.x plugins still build with Scala 2.12, but Play libs don't
   */
  def PlayNonCrossBuiltProject(name: String, dir: String): Project = {
    Project(name, file(dir))
      .enablePlugins(PlaySbtLibrary, AutomateHeaderPlugin, MimaPlugin)
      .settings(playRuntimeSettings: _*)
      .settings(omnidocSettings: _*)
      .settings(
        autoScalaLibrary := false,
        crossPaths       := false,
        // The (cross)ScalaVersion version here doesn't actually matter because these projects don't contain
        // Scala files anyway and also are published as Java only artifacts (no _2.1x/_3 suffix)
        // The reason to set crossScalaVersions is so the project gets published when running "+..." (e.g. "+publish[Local]")
        crossScalaVersions := Seq(scalaVersion.value)
      )
  }

  /** A project that is only used when running in development. */
  def PlayDevelopmentProject(name: String, dir: String): Project = {
    Project(name, file(dir))
      .enablePlugins(PlayLibrary, AutomateHeaderPlugin)
      .settings(
        playCommonSettings,
        mimaPreviousArtifacts := Set.empty,
      )
  }

  /** A project that is in the Play runtime. */
  def PlayCrossBuiltProject(name: String, dir: String): Project = {
    Project(name, file(dir))
      .enablePlugins(PlayLibrary, AutomateHeaderPlugin, AkkaSnapshotRepositories, MimaPlugin)
      .settings(playRuntimeSettings: _*)
      .settings(omnidocSettings: _*)
  }

  def omnidocSettings: Seq[Setting[_]] = Def.settings(
    Omnidoc.projectSettings,
    omnidocSnapshotBranch := snapshotBranch,
    omnidocPathPrefix     := ""
  )

  def playScriptedSettings: Seq[Setting[_]] = Seq(
    // Don't automatically publish anything.
    // The test-sbt-plugins-* scripts publish before running the scripted tests.
    // When developing the sbt plugins:
    // * run a publishLocal in the root project to get everything
    // * run a publishLocal in the changes projects for fast feedback loops
    scriptedDependencies := (()), // drop Test/compile & publishLocal
    scriptedBufferLog    := false,
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
    (publish / skip) := true,
    publishLocal     := {},
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
        (Test / fork)         := false,
        mimaPreviousArtifacts := Set.empty,
      )
  }
}
