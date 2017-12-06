/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
import sbt.ScriptedPlugin._
import sbt._
import Keys.{version, _}

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.plugin.MimaKeys._
import com.typesafe.tools.mima.plugin.MimaPlugin._

import de.heikoseeberger.sbtheader.HeaderKey._
import de.heikoseeberger.sbtheader.HeaderPattern

import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform.autoImport._

import bintray.BintrayPlugin.autoImport._
import interplay._
import interplay.Omnidoc.autoImport._
import interplay.PlayBuildBase.autoImport._

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
  val fileHeaderSettings = Seq(
    excludes := Seq("*/netty/utils/*", "*/inject/SourceProvider.java"),
    headers := Map(
      "scala" -> (HeaderPattern.cStyleBlockComment,
        """|/*
           | * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
           | */
           |""".stripMargin),
      "java"  -> (HeaderPattern.cStyleBlockComment,
        """|/*
           | * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
           | */
           |""".stripMargin)
    )
  )

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
      scalacOptions in(Compile, doc) := {
        // disable the new scaladoc feature for scala 2.12.0, might be removed in 2.12.0-1 (https://github.com/scala/scala-dev/issues/249)
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, 12)) => Seq("-no-java-comments")
          case _ => Seq()
        }
      },
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
    mimaPreviousArtifacts := {
      // Binary compatibility is tested against these versions
      val invalidVersions = Seq("2.6.4")
      val previousVersions = {
        val VersionPattern = """^(\d+).(\d+).(\d+)(-.*)?""".r
        version.value match {
          case VersionPattern(epoch, major, minor, rest) => (0 until minor.toInt).map(v => s"$epoch.$major.$v")
          case _ => sys.error(s"Cannot find previous versions for ${version.value}")
        }
      }.toSet -- invalidVersions
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

      // private
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.core.server.akkahttp.AkkaModelConversion.this"),

      // Added method to PlayBodyParsers, which is a Play API not meant to be extended by end users.
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.mvc.PlayBodyParsers.byteString"),

      // Moved play[private] out of from companion object to allow it to access member variables
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.api.test.TestServer.start"),

      // Added component so configuration would work properly
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.api.cache.ehcache.EhCacheComponents.actorSystem"),

      // Changed this private[play] type to a Lock to allow explicit locking
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.api.test.PlayRunners.mutex")
    ),
    unmanagedSourceDirectories in Compile += {
      (sourceDirectory in Compile).value / s"scala-${scalaBinaryVersion.value}"
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
    ScriptedPlugin.scripted := ScriptedPlugin.scripted.tag(Tags.Test).evaluated,
    scriptedLaunchOpts ++= Seq(
      "-Xmx768m",
      maxMetaspace,
      "-Dscala.version=" + sys.props.get("scripted.scala.version").orElse(sys.props.get("scala.version")).getOrElse("2.12.4")
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
        .settings(
          fork in Test := false
        )
  }

}
