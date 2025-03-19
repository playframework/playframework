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
import nl.gn0s1s.pekko.versioncheck.PekkoVersionCheckPlugin.autoImport._
import xerial.sbt.Sonatype.autoImport.sonatypeProfileName
import Omnidoc.autoImport.omnidocPathPrefix
import Omnidoc.autoImport.omnidocSnapshotBranch
import PlayBuildBase.autoImport.PlayLibrary
import PlayBuildBase.autoImport.PlaySbtLibrary
import PlayBuildBase.autoImport.PlaySbtPlugin

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

  def evictionSettings: Seq[Setting[?]] = Seq(
    // This avoids a lot of dependency resolution warnings to be showed.
    (update / evictionWarningOptions) := EvictionWarningOptions.default
      .withWarnTransitiveEvictions(false)
      .withWarnDirectEvictions(false)
  )

  val DocsApplication    = config("docs").hide
  val SourcesApplication = config("sources").hide

  /** These settings are used by all projects. */
  def playCommonSettings: Seq[Setting[?]] = Def.settings(
    sonatypeProfileName := "org.playframework",
    fileHeaderSettings,
    ivyLoggingLevel := UpdateLogging.DownloadOnly,
    resolvers ++= Resolver.sonatypeOssRepos("releases"), // sync ScriptedTools.scala
    evictionSettings,
    ivyConfigurations ++= Seq(DocsApplication, SourcesApplication),
    javacOptions ++= Seq("-encoding", "UTF-8", "-Xlint:unchecked", "-Xlint:deprecation"),
    scalacOptions ++= Seq("-release:17"),
    (Compile / doc / scalacOptions) := {
      // disable the new scaladoc feature for scala 2.12+ (https://github.com/scala/scala-dev/issues/249 and https://github.com/scala/bug/issues/11340)
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v >= 12 => Seq("-no-java-comments")
        case _                       => Seq() // Not available/needed in Scala 3 according to https://github.com/lampepfl/dotty/issues/11907
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
        sys.props.get("pekko.version").map("-pekko-" + _).getOrElse("") +
        sys.props.get("pekko.http.version").map("-pekko-http-" + _).getOrElse("")
    },
    pekkoVersionCheckFailBuildOnNonMatchingVersions := true,
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
      // Finds appropriate scala apidoc from dependencies when autoAPIMappings are insufficient.
      // See the following:
      //
      // http://stackoverflow.com/questions/19786841/can-i-use-sbts-apimappings-setting-for-managed-dependencies/20919304#20919304
      // http://www.scala-sbt.org/release/docs/Howto-Scaladoc.html#Enable+manual+linking+to+the+external+Scaladoc+of+managed+dependencies
      // https://github.com/ThoughtWorksInc/sbt-api-mappings/blob/master/src/main/scala/com/thoughtworks/sbtApiMappings/ApiMappings.scala#L34

      val ScalaLibraryRegex  = """^.*[/\\]scala-library-([\d\.]+)\.jar$""".r
      val JakartaInjectRegex = """^.*[/\\]jakarta.inject-api-([\d\.]+)\.jar$""".r

      val IvyRegex = """^.*[/\\]([\.\-_\w]+)[/\\]([\.\-_\w]+)[/\\](?:jars|bundles)[/\\]([\.\-_\w]+)\.jar$""".r

      (for {
        jar <- (Compile / doc / dependencyClasspath).value.toSet ++ (Test / doc / dependencyClasspath).value
        fullyFile = jar.data
        urlOption = fullyFile.getCanonicalPath match {
          case ScalaLibraryRegex(v) =>
            Some(url(raw"""http://scala-lang.org/files/archive/api/$v/index.html"""))

          case JakartaInjectRegex(v) =>
            // the jar file doesn't match up with $apiName-
            Some(url(Docs.jakartaInjectUrl))

          case re @ IvyRegex(apiOrganization, apiName, jarBaseFile) if jarBaseFile.startsWith(s"$apiName-") =>
            val apiVersion = jarBaseFile.substring(apiName.length + 1, jarBaseFile.length)
            apiOrganization match {
              case "org.apache.pekko" =>
                Some(url(raw"https://pekko.apache.org/api/pekko/$apiVersion/"))

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
  val mimaPreviousVersion: Option[String] = Some("3.0.0")

  /**
   * These settings are used by all projects that are part of the runtime, as opposed to the development mode of Play.
   */
  def playRuntimeSettings: Seq[Setting[?]] = Def.settings(
    playCommonSettings,
    mimaPreviousArtifacts := mimaPreviousVersion.map { version =>
      val cross = if (crossPaths.value) CrossVersion.binary else CrossVersion.disabled
      (organization.value %% moduleName.value % version).cross(cross)
    }.toSet,
    mimaBinaryIssueFilters ++= Seq(
      // Upgrade spring and hibernate-validator dependencies, switches to jakarta.validation namespace
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.data.DynamicForm.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.data.Form.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.data.FormFactory.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem](
        "play.data.validation.Constraints#PlayConstraintValidator.reportValidationStatus"
      ),
      ProblemFilters.exclude[IncompatibleMethTypeProblem](
        "play.data.validation.Constraints#PlayConstraintValidatorWithPayload.isValid"
      ),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.data.validation.Constraints#ValidateValidator.isValid"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem](
        "play.data.validation.Constraints#ValidateValidatorWithPayload.isValid"
      ),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.data.validation.Constraints#Validator.isValid"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem](
        "play.data.validation.Constraints#ValidatorWithPayload.isValid"
      ),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.data.validation.Constraints.displayableConstraint"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem](
        "play.data.validation.DefaultConstraintValidatorFactory.releaseInstance"
      ),
      ProblemFilters.exclude[IncompatibleMethTypeProblem](
        "play.data.validation.MappedConstraintValidatorFactory.addConstraintValidator"
      ),
      ProblemFilters.exclude[IncompatibleMethTypeProblem](
        "play.data.validation.MappedConstraintValidatorFactory.releaseInstance"
      ),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.data.validation.ValidatorFactoryProvider.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.data.validation.ValidatorProvider.this"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem](
        "play.data.validation.DefaultConstraintValidatorFactory.getInstance"
      ),
      ProblemFilters.exclude[IncompatibleResultTypeProblem](
        "play.data.validation.MappedConstraintValidatorFactory.getInstance"
      ),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.data.validation.ValidatorFactoryProvider.get"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.data.validation.ValidatorProvider.get"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem](
        "play.data.validation.ValidatorsComponents.constraintValidatorFactory"
      ),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.data.validation.ValidatorsComponents.validator"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem](
        "play.data.validation.ValidatorsComponents.validatorFactory"
      ),
      ProblemFilters.exclude[InheritedNewAbstractMethodProblem](
        "play.data.validation.Constraints#PlayConstraintValidator.isValid"
      ),
      ProblemFilters.exclude[MissingTypesProblem]("play.data.validation.Constraints$EmailValidator"),
      ProblemFilters.exclude[MissingTypesProblem]("play.data.validation.Constraints$MaxLengthValidator"),
      ProblemFilters.exclude[MissingTypesProblem]("play.data.validation.Constraints$MaxValidator"),
      ProblemFilters.exclude[MissingTypesProblem]("play.data.validation.Constraints$MinLengthValidator"),
      ProblemFilters.exclude[MissingTypesProblem]("play.data.validation.Constraints$MinValidator"),
      ProblemFilters.exclude[MissingTypesProblem]("play.data.validation.Constraints$PatternValidator"),
      ProblemFilters.exclude[MissingTypesProblem]("play.data.validation.Constraints$PlayConstraintValidator"),
      ProblemFilters.exclude[MissingTypesProblem](
        "play.data.validation.Constraints$PlayConstraintValidatorWithPayload"
      ),
      ProblemFilters.exclude[MissingTypesProblem]("play.data.validation.Constraints$RequiredValidator"),
      ProblemFilters.exclude[MissingTypesProblem]("play.data.validation.Constraints$ValidatePayloadWithValidator"),
      ProblemFilters.exclude[MissingTypesProblem]("play.data.validation.Constraints$ValidateValidator"),
      ProblemFilters.exclude[MissingTypesProblem]("play.data.validation.Constraints$ValidateValidatorWithPayload"),
      ProblemFilters.exclude[MissingTypesProblem]("play.data.validation.Constraints$ValidateWithValidator"),
      ProblemFilters.exclude[MissingTypesProblem]("play.data.validation.DefaultConstraintValidatorFactory"),
      ProblemFilters.exclude[MissingTypesProblem]("play.data.validation.MappedConstraintValidatorFactory"),
      ProblemFilters.exclude[ReversedMissingMethodProblem](
        "play.data.validation.Constraints#PlayConstraintValidatorWithPayload.isValid"
      ),
      // Inject (CSRF)errorHandler in CSRFCheck
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.csrf.CSRFCheck.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.csrf.CSRFCheck.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.csrf.CSRFCheck.this"),
      ProblemFilters.exclude[MissingTypesProblem]("play.filters.csrf.CSRFCheck$"),
      // Remove code deprecated in previous Play releases
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.libs.crypto.CSRFTokenSigner.constantTimeEquals"),
      ProblemFilters.exclude[DirectMissingMethodProblem](
        "play.api.libs.crypto.DefaultCSRFTokenSigner.constantTimeEquals"
      ),
      ProblemFilters.exclude[MissingClassProblem]("play.api.libs.crypto.CSRFTokenSigner$"),
      // Add routeModifierExcluded to RedirectHttpsConfiguration to implement route modifier black/whitelist
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.https.RedirectHttpsConfiguration.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.https.RedirectHttpsConfiguration.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.https.RedirectHttpsConfiguration.this"),
      ProblemFilters.exclude[MissingTypesProblem]("play.filters.https.RedirectHttpsConfiguration$"),
      // Switch to Jakarta DI
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.http.DefaultHttpErrorHandler.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.http.DefaultHttpRequestHandler.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.http.JavaCompatibleHttpRequestHandler.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.inject.BindingKey.to"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.inject.guice.FakeRouterProvider.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.inject.ProviderTarget.apply"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.inject.ProviderTarget.copy"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.inject.ProviderTarget.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.filters.csrf.CSRFFilter.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.http.DefaultHttpErrorHandler.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.inject.BindingKey.to"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.inject.ProviderTarget.this"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.inject.ProviderTarget._1"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.inject.ProviderTarget.copy$default$1"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.inject.ProviderTarget.provider"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.libs.concurrent.Pekko.providerOf"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.inject.ProviderTarget.getProvider"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.libs.Pekko.providerOf"),
      ProblemFilters.exclude[MissingTypesProblem]("controllers.AssetsConfigurationProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("controllers.AssetsFinderProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("controllers.AssetsMetadataProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.cache.caffeine.CacheManagerProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.cache.ehcache.CacheManagerProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.cluster.sharding.typed.ClusterShardingProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.db.DBApiProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.db.evolutions.ApplicationEvolutionsProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.db.evolutions.DefaultEvolutionsConfigParser"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.db.NamedDatabaseProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.http.DefaultFileMimeTypesProvider"),
      ProblemFilters.exclude[MissingTypesProblem](
        "play.api.http.HttpConfiguration$ActionCompositionConfigurationProvider"
      ),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.http.HttpConfiguration$CookiesConfigurationProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.http.HttpConfiguration$FileMimeTypesConfigurationProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.http.HttpConfiguration$FlashConfigurationProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.http.HttpConfiguration$HttpConfigurationProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.http.HttpConfiguration$ParserConfigurationProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.http.HttpConfiguration$SecretConfigurationProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.http.HttpConfiguration$SessionConfigurationProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.i18n.DefaultLangsProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.i18n.DefaultMessagesApiProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.inject.ConfigProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.inject.ConfigurationProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.inject.EnvironmentProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.inject.guice.AdditionalRouterProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.inject.guice.FakeRouterProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.inject.guice.GuiceInjectorWithClassLoaderProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.inject.RoutesProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.libs.concurrent.ActorRefProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.libs.concurrent.ActorSystemProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.libs.concurrent.ClassicActorSystemProviderProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.libs.concurrent.CoordinatedShutdownProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.libs.concurrent.ExecutionContextProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.libs.concurrent.MaterializerProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.libs.concurrent.PekkoSchedulerProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.libs.crypto.CookieSignerProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.libs.crypto.CSRFTokenSignerProvider"),
      ProblemFilters.exclude[MissingTypesProblem](
        "play.api.libs.Files$TemporaryFileReaperConfiguration$TemporaryFileReaperConfigurationProvider"
      ),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.libs.Files$TemporaryFileReaperConfigurationProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.libs.jcache.DefaultCacheManagerProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.libs.ws.ahc.AhcWSClientProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.libs.ws.ahc.AsyncHttpClientProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.libs.ws.ahc.OptionalAhcHttpCacheProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.libs.ws.ahc.StandaloneWSClientProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.OptionalSourceMapperProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.cluster.sharding.typed.ClusterShardingProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.core.FileMimeTypesProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.core.ObjectMapperProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.data.validation.ValidatorFactoryProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.data.validation.ValidatorProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.db.DBModule$NamedDatabaseProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.db.jpa.DefaultJPAApi$JPAApiProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.db.jpa.DefaultJPAConfig$JPAConfigProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.filters.cors.CORSConfigProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.filters.cors.CORSFilterProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.filters.csp.CSPConfigProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.filters.csrf.CSRF$TokenProviderProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.filters.csrf.CSRFConfigProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.filters.gzip.GzipFilterConfigProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.filters.headers.SecurityHeadersConfigProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.filters.hosts.AllowedHostsConfigProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.filters.https.RedirectHttpsConfigurationProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.filters.ip.IPFilterConfigProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.inject.NamedImpl"),
      ProblemFilters.exclude[MissingTypesProblem]("play.libs.ws.ahc.AhcWSModule$AhcWSClientProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.libs.ws.ahc.AhcWSModule$StandaloneWSClientProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.routing.JavaRoutingDslProvider"),
      // Remove global application
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Application.globalApplicationEnabled"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.DefaultApplication.globalApplicationEnabled"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Play.GlobalAppConfigKey"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Play.privateMaybeApplication"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.Play.routesCompilerMaybeApplication"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.http.HttpConfiguration.current"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.inject.guice.GuiceApplicationBuilder.globalApp"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.DefaultTestServerFactory.optionalGlobalLock"),
      // Rename runSynchronized to maybeRunSynchronized (which is and was private[play] anyway...)
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.Helpers.runSynchronized"),
      // Add setSameSite setter to Cookie interface
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.core.cookie.encoding.Cookie.setSameSite"),
      // Add Partitioned attribute to cookies
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.http.FlashConfiguration.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.http.FlashConfiguration.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.http.FlashConfiguration.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.http.SessionConfiguration.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.http.SessionConfiguration.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.http.SessionConfiguration.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.i18n.DefaultMessagesApi.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Cookie.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Cookie.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.Cookie.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.DiscardingCookie.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.DiscardingCookie.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.DiscardingCookie.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.Helpers.stubMessagesApi"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.StubMessagesFactory.stubMessagesApi"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.csrf.CSRFConfig.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.csrf.CSRFConfig.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.csrf.CSRFConfig.this"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.http.FlashConfiguration.apply$default$7"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.http.FlashConfiguration.copy$default$7"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.http.FlashConfiguration.<init>$default$7"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.http.SessionConfiguration.apply$default$8"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.http.SessionConfiguration.copy$default$8"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.http.SessionConfiguration.<init>$default$8"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.i18n.DefaultMessagesApi.<init>$default$7"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.i18n.DefaultMessagesApi.<init>$default$8"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.apply$default$11"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.apply$default$12"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.apply$default$13"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.apply$default$6"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.apply$default$7"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.apply$default$8"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.apply$default$9"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.copy$default$11"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.copy$default$12"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.copy$default$13"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.copy$default$6"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.copy$default$7"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.copy$default$8"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.copy$default$9"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.<init>$default$11"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.<init>$default$12"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.<init>$default$13"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.<init>$default$6"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.<init>$default$7"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.<init>$default$8"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig.<init>$default$9"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.http.FlashConfiguration$"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.http.SessionConfiguration$"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.mvc.DiscardingCookie$"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.i18n.MessagesApi.langCookiePartitioned"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.core.cookie.encoding.Cookie.isPartitioned"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.core.cookie.encoding.Cookie.setPartitioned"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.http.FlashConfiguration._7"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.http.SessionConfiguration._8"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig._11"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig._12"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig._13"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig._6"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig._7"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig._8"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.filters.csrf.CSRFConfig._9"),
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
      .enablePlugins(PlayLibrary, AutomateHeaderPlugin, PekkoSnapshotRepositories, MimaPlugin)
      .settings(playRuntimeSettings: _*)
      .settings(omnidocSettings: _*)
  }

  def omnidocSettings: Seq[Setting[?]] = Def.settings(
    Omnidoc.projectSettings,
    omnidocSnapshotBranch := snapshotBranch,
    omnidocPathPrefix     := ""
  )

  def playScriptedSettings: Seq[Setting[?]] = Seq(
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
