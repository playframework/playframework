/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
import sbt.ScriptedPlugin._
import sbt._
import Keys.{ version, _ }
import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.plugin.MimaKeys._
import com.typesafe.tools.mima.plugin.MimaPlugin._
import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._
import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform.autoImport._
import bintray.BintrayPlugin.autoImport._
import interplay._
import interplay.Omnidoc.autoImport._
import interplay.PlayBuildBase.autoImport._
import java.util.regex.Pattern

import scala.util.control.NonFatal

object BuildSettings {

  // Argument for setting size of permgen space or meta space for all forked processes
  val maxMetaspace = s"-XX:MaxMetaspaceSize=384m"

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

  /**
   * File header settings
   */
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
    headerLicense := Some(HeaderLicense.Custom("Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>"))
  )

  private val VersionPattern = """^(\d+).(\d+).(\d+)(-.*)?""".r

  // Versions of previous minor releases being checked for binary compatibility
  val mimaPreviousMinorReleaseVersions: Seq[String] = Seq("2.6.0")
  def mimaPreviousPatchVersions(version: String): Seq[String] = version match {
    case VersionPattern(epoch, major, minor, rest) => (0 until minor.toInt).map(v => s"$epoch.$major.$v")
    case _ => sys.error(s"Cannot find previous versions for $version")
  }
  def mimaPreviousVersions(version: String): Set[String] =
    mimaPreviousMinorReleaseVersions.toSet ++ mimaPreviousPatchVersions(version)

  /**
   * These settings are used by all projects
   */
  def playCommonSettings: Seq[Setting[_]] = {

    fileHeaderSettings ++ Seq(
      scalariformAutoformat := true,
      scalariformPreferences := scalariformPreferences.value
          .setPreference(SpacesAroundMultiImports, true)
          .setPreference(SpaceInsideParentheses, false)
          .setPreference(DanglingCloseParenthesis, Preserve)
          .setPreference(PreserveSpaceBeforeArguments, true)
          .setPreference(DoubleIndentConstructorArguments, true)
    ) ++ Seq(
      homepage := Some(url("https://playframework.com")),
      ivyLoggingLevel := UpdateLogging.DownloadOnly,
      resolvers ++= Seq(
        Resolver.sonatypeRepo("releases"),
        Resolver.typesafeRepo("releases"),
        Resolver.typesafeIvyRepo("releases")
      ),
      javacOptions ++= Seq("-encoding",  "UTF-8", "-Xlint:unchecked", "-Xlint:deprecation"),
      scalacOptions in(Compile, doc) := {
        // disable the new scaladoc feature for scala 2.12.0, might be removed in 2.12.0-1 (https://github.com/scala/scala-dev/issues/249)
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, v)) if v >= 12 => Seq("-no-java-comments")
          case _ => Seq()
        }
      },
      fork in Test := true,
      parallelExecution in Test := false,
      testListeners in (Test,test) := Nil,
      javaOptions in Test ++= Seq(maxMetaspace, "-Xmx512m", "-Xms128m"),
      testOptions += Tests.Argument(TestFrameworks.JUnit, "-v"),
      bintrayPackage := "play-sbt-plugin",
      apiURL := {
        val v = version.value
        if (isSnapshot.value) {
          v match {
            case VersionPattern(epoch, major, _, _) => Some(url(raw"https://www.playframework.com/documentation/$epoch.$major.x/api/scala/index.html"))
            case _ => Some(url("https://www.playframework.com/documentation/latest/api/scala/index.html"))
          }
        } else {
          Some(url(raw"https://www.playframework.com/documentation/$v/api/scala/index.html"))
        }
      },
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
  }

  /**
   * These settings are used by all projects that are part of the runtime, as opposed to development, mode of Play.
   */
  def playRuntimeSettings: Seq[Setting[_]] = playCommonSettings ++ mimaDefaultSettings ++ Seq(
    mimaPreviousArtifacts := {
      // Binary compatibility is tested against these versions
      val previousVersions = mimaPreviousVersions(version.value)
      if (crossPaths.value) {
        previousVersions.map(v => organization.value % s"${moduleName.value}_${scalaBinaryVersion.value}" %  v)
      } else {
        previousVersions.map(v => organization.value % moduleName.value %  v)
      }
    },
    mimaBinaryIssueFilters ++= Seq(
      // Changing return and parameter types from DefaultApplicationLifecycle (implementation) to ApplicationLifecycle (trait)
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.BuiltInComponents.applicationLifecycle"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.BuiltInComponentsFromContext.applicationLifecycle"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.core.server.AkkaHttpServerComponents.applicationLifecycle"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.AkkaHttpServerComponents.applicationLifecycle"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.core.server.AkkaHttpServerComponents.applicationLifecycle"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.ApplicationLoader.createContext$default$5"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.ApplicationLoader#Context.lifecycle"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.ApplicationLoader#Context.copy$default$5"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.core.ObjectMapperComponents.applicationLifecycle"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.core.server.NettyServerComponents.applicationLifecycle"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.http.CookiesConfiguration.serverEncoder"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.http.CookiesConfiguration.serverDecoder"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.http.CookiesConfiguration.clientEncoder"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.http.CookiesConfiguration.clientDecoder"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.ApplicationLoader.createContext"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.ApplicationLoader#Context.apply"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.ApplicationLoader#Context.copy"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.ApplicationLoader#Context.this"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.BuiltInComponents.applicationLifecycle"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.core.ObjectMapperComponents.applicationLifecycle"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.NettyServerComponents.applicationLifecycle"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.core.server.NettyServerComponents.applicationLifecycle"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.common.ServerResultUtils.sessionBaker"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.common.ServerResultUtils.cookieHeaderEncoding"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.common.ServerResultUtils.flashBaker"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.common.ServerResultUtils.this"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.http.HeaderNames.CONTENT_SECURITY_POLICY"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.http.HeaderNames.play$api$http$HeaderNames$_setter_$CONTENT_SECURITY_POLICY_="),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.http.HeaderNames.play$api$http$HeaderNames$_setter_$X_XSS_PROTECTION_="),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.http.HeaderNames.X_XSS_PROTECTION"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.http.HeaderNames.play$api$http$HeaderNames$_setter_$REFERRER_POLICY_="),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.http.HeaderNames.REFERRER_POLICY"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.http.HeaderNames.X_CONTENT_TYPE_OPTIONS"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.http.HeaderNames.play$api$http$HeaderNames$_setter_$X_CONTENT_TYPE_OPTIONS_="),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.http.HeaderNames.X_PERMITTED_CROSS_DOMAIN_POLICIES"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.http.HeaderNames.play$api$http$HeaderNames$_setter_$X_PERMITTED_CROSS_DOMAIN_POLICIES_="),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.http.HeaderNames.X_FRAME_OPTIONS"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.http.HeaderNames.play$api$http$HeaderNames$_setter_$X_FRAME_OPTIONS_="),

      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.http.HeaderNames.X_CONTENT_SECURITY_POLICY_NONCE_HEADER"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.http.HeaderNames.play$api$http$HeaderNames$_setter_$X_CONTENT_SECURITY_POLICY_NONCE_HEADER_="),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.http.HeaderNames.CONTENT_SECURITY_POLICY_REPORT_ONLY"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.http.HeaderNames.play$api$http$HeaderNames$_setter_$CONTENT_SECURITY_POLICY_REPORT_ONLY_="),

      ProblemFilters.exclude[MissingFieldProblem]("play.mvc.Results.TODO"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.mvc.Controller.TODO"),

      ProblemFilters.exclude[MissingTypesProblem]("views.html.defaultpages.devError$"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("views.html.defaultpages.devError.render"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("views.html.defaultpages.devError.apply"),

      ProblemFilters.exclude[MissingTypesProblem]("views.html.defaultpages.badRequest$"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("views.html.defaultpages.badRequest.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("views.html.defaultpages.badRequest.render"),

      ProblemFilters.exclude[MissingTypesProblem]("views.html.defaultpages.todo$"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("views.html.defaultpages.todo.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("views.html.defaultpages.todo.render"),

      ProblemFilters.exclude[MissingTypesProblem]("views.html.defaultpages.devNotFound$"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("views.html.defaultpages.devNotFound.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("views.html.defaultpages.devNotFound.render"),

      ProblemFilters.exclude[MissingTypesProblem]("views.html.defaultpages.error$"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("views.html.defaultpages.error.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("views.html.defaultpages.error.render"),

      ProblemFilters.exclude[MissingTypesProblem]("views.html.helper.jsloader$"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("views.html.helper.jsloader.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("views.html.helper.jsloader.render"),

      ProblemFilters.exclude[MissingTypesProblem]("views.html.defaultpages.notFound$"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("views.html.defaultpages.notFound.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("views.html.defaultpages.notFound.render"),

      ProblemFilters.exclude[MissingTypesProblem]("views.html.defaultpages.unauthorized$"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("views.html.defaultpages.unauthorized.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("views.html.defaultpages.unauthorized.render"),

      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.akkahttp.AkkaModelConversion.this"),

      // Added method to PlayBodyParsers, which is a Play API not meant to be extended by end users.
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.mvc.PlayBodyParsers.byteString"),

      // Refactoring to unify AkkaHttpServer and NettyServer fromRouter methods
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.core.server.NettyServer.fromRouter"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.core.server.AkkaHttpServer.fromRouter"),

      // Moved play[private] out of from companion object to allow it to access member variables
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.TestServer.start"),

      // Added component so configuration would work properly
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.cache.ehcache.EhCacheComponents.actorSystem"),

      // Changed this private[play] type to a Lock to allow explicit locking
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.test.PlayRunners.mutex"),

      // Deprecate ApplicationProvider.handleWebCommands and pass BuildLink through ApplicationLoader.Context
      ProblemFilters.exclude[FinalClassProblem]("play.api.OptionalSourceMapper"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.ApplicationLoader$Context$"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.ApplicationLoader#Context.copy"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.ApplicationLoader#Context.copy$default$4"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.ApplicationLoader#Context.copy$default$3"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.ApplicationLoader#Context.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.AkkaHttpServerComponents.sourceMapper"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.AkkaHttpServerComponents.webCommands"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.BuiltInComponents.play$api$BuiltInComponents$$defaultWebCommands"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.BuiltInComponents.play$api$BuiltInComponents$_setter_$play$api$BuiltInComponents$$defaultWebCommands_="),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.ApplicationLoader#Context.copy$default$2"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.ApplicationLoader#Context.copy$default$5"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.NettyServerComponents.sourceMapper"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.NettyServerComponents.webCommands"),

      // Add compressionLevel to GzipFilter
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.gzip.GzipFilter.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.gzip.GzipFilterConfig.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.gzip.GzipFilterConfig.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.gzip.GzipFilterConfig.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.libs.streams.GzipFlow.gzip"),

      // Pass a default server header to netty
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.netty.NettyModelConversion.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.netty.PlayRequestHandler.this"),

      // Made InlineCache.cache private and changed the type (class is private[play])
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.utils.InlineCache.cache"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.utils.InlineCache.cache_="),
      ProblemFilters.exclude[FinalMethodProblem]("play.api.inject.guice.FakeRoutes.handlerFor"),
      ProblemFilters.exclude[FinalMethodProblem]("play.core.routing.GeneratedRouter.handlerFor"),
      ProblemFilters.exclude[FinalMethodProblem]("play.api.routing.SimpleRouterImpl.handlerFor"),

      // Added xForwardedForProto handling to RedirectHttpsFilter
      ProblemFilters.exclude[MissingTypesProblem]("play.filters.https.RedirectHttpsConfiguration$"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.https.RedirectHttpsConfiguration.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.https.RedirectHttpsConfiguration.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.https.RedirectHttpsConfiguration.this"),

      // invokeWithContextOpt is unnecessary since JavaGlobalSettingsAdapter has been removed in Play 2.6
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.JavaHelpers.invokeWithContextOpt"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.JavaAction.invokeWithContextOpt"),

      // Remove BoneCP
      ProblemFilters.exclude[MissingClassProblem]("play.api.db.BoneConnectionPool"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.db.BoneConnectionPool$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.db.BoneCPComponents"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.db.BoneCPModule"),
      ProblemFilters.exclude[MissingClassProblem]("play.db.BoneCPComponents"),

      // Remove deprecated methods
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.Application.configuration"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.Application.getFile"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.Application.resource"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.Application.resourceAsStream"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.ApplicationLoader#Context.create"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.ApplicationLoader#Context.initialConfiguration"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.ApplicationLoader#Context.underlying"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.ApplicationLoader#Context.withConfiguration"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.Environment.underlying"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.cache.DefaultSyncCacheApi.getOrElse"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.RequestHeaderImpl._underlyingHeader"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.RequestHeaderImpl.getHeader"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.RequestHeaderImpl.headers"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.RequestHeaderImpl.tags"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.RequestImpl._underlyingRequest"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.RequestImpl.username"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.RequestImpl.withUsername"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.data.DynamicForm.data"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.data.DynamicForm.reject"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.data.Form#Field.valueOr"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.data.Form.data"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.data.Form.discardErrors"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.data.Form.reject"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.data.format.Formatters.parse"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.http.HandlerForRequest.getRequest"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.http.HttpFilters.filters"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.i18n.MessagesApi.scalaApi"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.libs.Files#DelegateTemporaryFile.file"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.libs.Files#TemporaryFile.file"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.libs.concurrent.Futures.delayed"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.libs.concurrent.HttpExecution.defaultContext"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.libs.crypto.CSRFTokenSigner.constantTimeEquals"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.libs.crypto.DefaultCSRFTokenSigner.constantTimeEquals"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Cookie.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Request._underlyingRequest"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Request.username"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Request.withUsername"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#RequestBuilder.header"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#RequestBuilder.headers"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#RequestBuilder.tag"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#RequestBuilder.tags"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#RequestBuilder.username"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#RequestHeader._underlyingHeader"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#RequestHeader.getHeader"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#RequestHeader.headers"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#RequestHeader.tags"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Response.setContentType"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.mvc.Http#Response.setCookie"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.routing.RoutingDsl.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.server.ApplicationProvider.getApplication"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.test.Helpers.route"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.test.Helpers.routeAndCall"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.DefaultApplication.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.data.DynamicForm.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.data.Form.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.http.DefaultHttpErrorHandler.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.inject.guice.GuiceApplicationBuilder.load"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.inject.guice.GuiceApplicationBuilder.loadConfig"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.inject.guice.GuiceBuilder.configure"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.libs.concurrent.Futures.timeout"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Http#CookieBuilder.withMaxAge"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Http#RequestBuilder.header"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Http#RequestBuilder.headers"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Result.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Results.badRequest"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Results.created"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Results.forbidden"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Results.internalServerError"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Results.notFound"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Results.ok"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Results.paymentRequired"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Results.status"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.Results.unauthorized"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.mvc.StatusHeader.sendJson"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.routing.RoutingDsl.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.server.Server.forRouter"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.test.Helpers.route"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.test.Helpers.routeAndCall"),
      ProblemFilters.exclude[MissingClassProblem]("play.Configuration"),
      ProblemFilters.exclude[MissingClassProblem]("play.Play"),
      ProblemFilters.exclude[MissingClassProblem]("play.cache.CacheApi"),
      ProblemFilters.exclude[MissingClassProblem]("play.inject.ConfigurationProvider"),
      ProblemFilters.exclude[MissingClassProblem]("play.libs.Classpath"),
      ProblemFilters.exclude[MissingClassProblem]("play.libs.ReflectionsCache"),
      ProblemFilters.exclude[MissingClassProblem]("play.libs.ReflectionsCache$"),
      ProblemFilters.exclude[MissingClassProblem]("play.libs.concurrent.Timeout"),
      ProblemFilters.exclude[MissingClassProblem]("play.libs.ws.WS"),
      ProblemFilters.exclude[MissingClassProblem]("play.routing.Router$Tags"),
      ProblemFilters.exclude[MissingClassProblem]("play.routing.RoutingDslProvider"),
      ProblemFilters.exclude[MissingTypesProblem]("play.cache.DefaultSyncCacheApi"),

      // Removed request tags
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.RequestHeader.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.RequestHeader.copy$default$11"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.RequestHeaderImpl.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.RequestHeaderImpl.copy$default$11"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.RequestHeaderImpl.tags"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.RequestHeaderImpl.withTag"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.RequestHeader.tags"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.RequestHeader.withTag"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.RequestImpl.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.RequestImpl.copy$default$11"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.RequestImpl.tags"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.RequestImpl.withTag"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.request.RequestAttrKey.Tags"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.WrappedRequest.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.WrappedRequest.copy$default$11"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.WrappedRequest.tags"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.mvc.WrappedRequest.withTag"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.FakeRequest.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.FakeRequest.copy$default$11"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.FakeRequest.copyFakeRequest"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.FakeRequest.copyFakeRequest$default$11"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.FakeRequestFactory.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.FakeRequestFactory.apply$default$11"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.FakeRequest.tags"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.FakeRequest.withTag"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.j.RequestHeaderImpl.withTag"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.cors.CORSFilter.RequestTag"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RequestHeader.copy$default$10"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RequestHeader.copy$default$2"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RequestHeader.copy$default$6"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RequestHeader.copy$default$7"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RequestHeader.copy$default$8"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RequestHeader.copy$default$9"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RequestHeaderImpl.copy$default$10"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RequestHeaderImpl.copy$default$2"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RequestHeaderImpl.copy$default$6"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RequestHeaderImpl.copy$default$7"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RequestHeaderImpl.copy$default$8"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RequestHeaderImpl.copy$default$9"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RequestImpl.copy$default$10"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RequestImpl.copy$default$2"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RequestImpl.copy$default$6"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RequestImpl.copy$default$7"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RequestImpl.copy$default$8"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RequestImpl.copy$default$9"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.WrappedRequest.copy$default$10"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.WrappedRequest.copy$default$2"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.WrappedRequest.copy$default$6"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.WrappedRequest.copy$default$7"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.WrappedRequest.copy$default$8"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.WrappedRequest.copy$default$9"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.test.FakeRequest.copy$default$10"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.test.FakeRequest.copy$default$2"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.test.FakeRequest.copy$default$6"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.test.FakeRequest.copy$default$7"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.test.FakeRequest.copy$default$8"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.test.FakeRequest.copy$default$9"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.test.FakeRequest.copyFakeRequest$default$10"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.test.FakeRequest.copyFakeRequest$default$2"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.test.FakeRequest.copyFakeRequest$default$6"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.test.FakeRequest.copyFakeRequest$default$7"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.test.FakeRequest.copyFakeRequest$default$8"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.test.FakeRequest.copyFakeRequest$default$9"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.test.FakeRequestFactory.apply$default$10"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.test.FakeRequestFactory.apply$default$8"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.test.FakeRequestFactory.apply$default$9"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.mvc.RequestTaggingHandler"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.routing.Router$Tags$"),
      ProblemFilters.exclude[MissingClassProblem]("play.routing.Router$Tags"),

      // Upgrade Guice from 4.1.0 to 4.2.0 which uses java.util.function.Function instead of com.google.common.base.Function now
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.test.TestBrowser.waitUntil"),

      // "Renamed" methods in Java form api
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.data.Form#Field.value"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.data.Form#Field.name"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.data.Form.error"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.data.Form.globalError"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.data.Form.errors"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.data.DynamicForm.error"),

      // Remove CacheApi
      ProblemFilters.exclude[MissingClassProblem]("play.api.cache.CacheApi"),
      ProblemFilters.exclude[MissingTypesProblem]("play.api.cache.DefaultSyncCacheApi"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.cache.DefaultSyncCacheApi.getOrElse"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.cache.DefaultSyncCacheApi.getOrElse$default$2"),

      // Remove Server trait's deprecated getHandler method
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.Server.getHandlerFor"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.NettyServer.getHandlerFor"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.AkkaHttpServer.getHandlerFor"),

      // Make Akka's Coordinated Shutdown take over the shutdown process
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.components.AkkaComponents.coordinatedShutdown"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.Application.coordinatedShutdown"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.libs.concurrent.ActorSystemProvider.this"),

      // Change signature of Play.privateMaybeApplication to return a Try[Application]
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.Play.privateMaybeApplication"),

      // Update Play WS to version 2.0.0
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.libs.ws.WSRequest.getRequestTimeoutDuration"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.libs.ws.WSRequest.getRequestTimeout"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.libs.ws.WSRequest.getFollowRedirects"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.libs.ws.WSRequest.getPassword"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.libs.ws.WSRequest.getCalculator"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.libs.ws.WSRequest.getUsername"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.libs.ws.WSRequest.getScheme"),
      // Updates Play WS to version 2.0.0 has impact on AHC implementation
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.libs.ws.ahc.AhcWSRequest.getRequestTimeoutDuration"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.libs.ws.ahc.AhcWSRequest.asCookie"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.libs.ws.ahc.AhcWSRequest.getPassword"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.libs.ws.ahc.AhcWSRequest.getUsername"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.libs.ws.ahc.AhcWSRequest.getScheme"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.libs.ws.ahc.AhcWSRequest.getRequestTimeout"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.libs.ws.ahc.AhcWSRequest.getCalculator"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.libs.ws.ahc.AhcWSRequest.getContentType"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.libs.ws.ahc.AhcWSRequest.getFollowRedirects"),

      // PlayConfig is private[play]
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.ConfigLoader.seqPlayConfigLoader"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.ConfigLoader.playConfigLoader"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.PlayConfig$"),
      ProblemFilters.exclude[MissingClassProblem]("play.api.PlayConfig"),

      // Add play.Application environment() method
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.Application.environment"),

      // Added getOptional to Java (async)cacheApi
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.cache.AsyncCacheApi.getOptional"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.cache.SyncCacheApi.getOptional"),

      // Remove DefaultDBApi.connect method
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.db.DefaultDBApi.connect$default$1"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.db.DefaultDBApi.connect"),

      // Make all BodyParser maxLength args Long
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.DefaultPlayBodyParsers.text"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.DefaultPlayBodyParsers.xml"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.DefaultPlayBodyParsers.tolerantJson"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.DefaultPlayBodyParsers.formUrlEncoded"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.DefaultPlayBodyParsers.tolerantFormUrlEncoded"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.DefaultPlayBodyParsers.json"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.DefaultPlayBodyParsers.urlFormEncoded"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.DefaultPlayBodyParsers.tolerantXml"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.PlayBodyParsers.text"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.PlayBodyParsers.xml"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.PlayBodyParsers.formUrlEncoded"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.PlayBodyParsers.tolerantJson"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.PlayBodyParsers.tolerantFormUrlEncoded"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.PlayBodyParsers.json"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.PlayBodyParsers.urlFormEncoded"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.PlayBodyParsers.tolerantXml"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.core.parsers.Multipart#BodyPartParser.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.core.parsers.Multipart.partParser"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.core.parsers.Multipart.multipartParser"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.DefaultPlayBodyParsers.raw"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.DefaultPlayBodyParsers.DefaultMaxTextLength"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.DefaultPlayBodyParsers.raw$default$1"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.PlayBodyParsers.raw"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.PlayBodyParsers.DefaultMaxTextLength"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.PlayBodyParsers.raw$default$1"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RawBuffer.memoryThreshold"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.RawBuffer.copy"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.mvc.RawBuffer.copy$default$1"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.RawBuffer.this"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.mvc.RawBuffer.apply"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.http.ParserConfiguration.apply$default$1"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.http.ParserConfiguration.apply"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.http.ParserConfiguration.<init>$default$1"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.http.ParserConfiguration.maxMemoryBuffer"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.http.ParserConfiguration.copy"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.http.ParserConfiguration.copy$default$1"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.api.http.ParserConfiguration.this"),

      // Add configuration to set max header value length
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.http.Status.REQUEST_HEADER_FIELDS_TOO_LARGE"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.http.Status.play$api$http$Status$_setter_$REQUEST_HEADER_FIELDS_TOO_LARGE_="),

      // https://github.com/playframework/playframework/issues/8534
      // Removed StopHook from ActorSystemProvider.start methods return values
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.libs.concurrent.ActorSystemProvider.start"),
      // Removed private[play] class CloseableLazy
      ProblemFilters.exclude[MissingClassProblem]("play.core.ClosableLazy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.libs.concurrent.ActorSystemProvider.lazyStart"),

      // Merge Lagom changes to KeyStore generation
      // https://github.com/playframework/playframework/pull/8574
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.ssl.FakeKeyStore.DnName"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.ssl.FakeKeyStore.createSelfSignedCertificate"),

      // Simplify ReloadableServer interface
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.core.server.Server.mainAddress"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.ReloadableServer.mainAddress"),

      // Add route modifier whitelist / blacklist to AllowedHostsFilter
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.hosts.AllowedHostsConfig.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.hosts.AllowedHostsConfig.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.hosts.AllowedHostsConfig.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.filters.hosts.AllowedHostsConfig.apply"),

      // Add ValidationPayload to Java isValid/validate methods
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.data.FormFactory.this"),

      // Remove JPA class + add more withTransaction(...) methods
      ProblemFilters.exclude[MissingClassProblem]("play.db.jpa.JPA"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.db.jpa.JPAApi.withTransaction")
  ),
    unmanagedSourceDirectories in Compile += {
      (sourceDirectory in Compile).value / s"scala-${scalaBinaryVersion.value}"
    },
    // Argument for setting size of permgen space or meta space for all forked processes
    Docs.apiDocsInclude := true
  ) ++ Seq(
    // TODO: Remove when updating to Scala 2.13.0-M4
    // Interplay 2.0.3 adds Scala 2.13.0-M4 to crossScalaVersions, but we don't want
    // that right because some dependencies don't have a build to M4 yet. As soon as
    // we decide that we could release to M4, than we can remove this setting and use
    // the one provided by interplay.
    //
    // See also:
    // 1. the root project at build.sbt file.
    // 2. RoutesCompilerProject project
    crossScalaVersions := Seq(ScalaVersions.scala211, ScalaVersions.scala212, "2.13.0-M3")
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
        .enablePlugins(PlaySbtLibrary, AutomateHeaderPlugin)
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
        .enablePlugins(PlayLibrary, AutomateHeaderPlugin)
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
        .enablePlugins(PlayLibrary, AutomateHeaderPlugin, AkkaSnapshotRepositories)
        .settings(playRuntimeSettings: _*)
        .settings(omnidocSettings: _*)
        .settings(
          // Need to add this after updating to Scala 2.11.12
          scalacOptions += "-target:jvm-1.8"
        )
  }

  def omnidocSettings: Seq[Setting[_]] = Omnidoc.projectSettings ++ Seq(
    omnidocSnapshotBranch := snapshotBranch,
    omnidocPathPrefix := "framework/"
  )

  def playScriptedSettings: Seq[Setting[_]] = Seq(
    ScriptedPlugin.scripted := ScriptedPlugin.scripted.tag(Tags.Test).evaluated,
    scriptedLaunchOpts ++= Seq(
      "-Xmx768m",
      maxMetaspace,
      "-Dscala.version=" + sys.props.get("scripted.scala.version").orElse(sys.props.get("scala.version")).getOrElse("2.12.6")
    )
  )

  def playFullScriptedSettings: Seq[Setting[_]] = ScriptedPlugin.scriptedSettings ++ Seq(
    ScriptedPlugin.scriptedLaunchOpts += s"-Dproject.version=${version.value}"
  ) ++ playScriptedSettings

  /**
   * A project that runs in the SBT runtime
   */
  def PlaySbtProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
        .enablePlugins(PlaySbtLibrary, AutomateHeaderPlugin)
        .settings(playCommonSettings: _*)
  }

  /**
   * A project that *is* an SBT plugin
   */
  def PlaySbtPluginProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
        .enablePlugins(PlaySbtPlugin, AutomateHeaderPlugin)
        .settings(playCommonSettings: _*)
        .settings(playScriptedSettings: _*)
        .settings(
          fork in Test := false
        )
  }

}
