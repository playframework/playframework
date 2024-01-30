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
    sonatypeProfileName := "org.playframework",
    fileHeaderSettings,
    ivyLoggingLevel := UpdateLogging.DownloadOnly,
    resolvers ++= Resolver.sonatypeOssRepos("releases"), // sync ScriptedTools.scala
    evictionSettings,
    ivyConfigurations ++= Seq(DocsApplication, SourcesApplication),
    javacOptions ++= Seq("-encoding", "UTF-8", "-Xlint:unchecked", "-Xlint:deprecation"),
    scalacOptions ++= Seq("-release:11"),
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
  def playRuntimeSettings: Seq[Setting[_]] = Def.settings(
    playCommonSettings,
    mimaPreviousArtifacts := mimaPreviousVersion.map { version =>
      val cross = if (crossPaths.value) CrossVersion.binary else CrossVersion.disabled
      (organization.value %% moduleName.value % version).cross(cross)
    }.toSet,
    mimaBinaryIssueFilters ++= Seq(
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
