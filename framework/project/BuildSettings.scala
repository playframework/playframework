/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
import java.util.regex.Pattern

import bintray.BintrayPlugin.autoImport._
import com.typesafe.sbt.SbtScalariform.autoImport._
import com.typesafe.sbt.pgp.PgpKeys
import com.typesafe.tools.mima.core.{ProblemFilters, _}
import com.typesafe.tools.mima.plugin.MimaKeys._
import com.typesafe.tools.mima.plugin.MimaPlugin._
import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._
import interplay.Omnidoc.autoImport._
import interplay.PlayBuildBase.autoImport._
import interplay._
import sbt.Keys.{version, _}
import sbt.ScriptedPlugin._
import sbt.{Resolver, _}
import scalariform.formatter.preferences._

import scala.util.control.NonFatal

import sbtwhitesource.WhiteSourcePlugin.autoImport._

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
    headerLicense := Some(HeaderLicense.Custom("Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>"))
  )

  private val VersionPattern = """^(\d+).(\d+).(\d+)(-.*)?""".r

  // Versions of previous minor releases being checked for binary compatibility
  val mimaPreviousMinorReleaseVersions: Seq[String] = Nil // Seq("2.7.0")
  def mimaPreviousPatchVersions(version: String): Seq[String] = version match {
    case VersionPattern(epoch, major, minor, rest) => (0 until minor.toInt).map(v => s"$epoch.$major.$v")
    case _ => sys.error(s"Cannot find previous versions for $version")
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

  /**
   * These settings are used by all projects
   */
  def playCommonSettings: Seq[Setting[_]] = evictionSettings ++ playPublishingPromotionSettings ++ {

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
      testOptions ++= Seq(
        Tests.Argument(TestFrameworks.Specs2, "showtimes"),
        Tests.Argument(TestFrameworks.JUnit, "-v")
      ),
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
      apiMappings ++= {
        // Maps JDK 1.8 jar into apidoc.
        val rtJar = System.getProperty("sun.boot.class.path").split(java.io.File.pathSeparator).collectFirst {
          case str: String if str.endsWith(java.io.File.separator + "rt.jar") => str
        }
        rtJar match {
          case None => Map.empty
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
    mimaBinaryIssueFilters ++= Seq(),
    unmanagedSourceDirectories in Compile += {
      (sourceDirectory in Compile).value / s"scala-${scalaBinaryVersion.value}"
    },
    // Argument for setting size of permgen space or meta space for all forked processes
    Docs.apiDocsInclude := true
  ) ++ Seq(
    // TODO: Re-add ScalaVersions.scala213
    // Interplay 2.0.4 adds Scala 2.13.0-M5 to crossScalaVersions, but we don't want
    // that right because some dependencies don't have a build for M5 yet. As soon as
    // we decide that we could release to M5, than we can re-add scala213 to it
    //
    // See also:
    // 1. the root project at build.sbt file.
    // 2. RoutesCompilerProject project
    crossScalaVersions := Seq(ScalaVersions.scala211, ScalaVersions.scala212)
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
      "-Dscala.version=" + sys.props.get("scripted.scala.version").orElse(sys.props.get("scala.version")).getOrElse("2.12.8")
    )
  )

  def playFullScriptedSettings: Seq[Setting[_]] = ScriptedPlugin.scriptedSettings ++ Seq(
    ScriptedPlugin.scriptedLaunchOpts += s"-Dproject.version=${version.value}"
  ) ++ playScriptedSettings

  def disablePublishing = Seq[Setting[_]](
    // This setting will work for sbt 1, but not 0.13. For 0.13 it only affects
    // `compile` and `update` tasks.
    skip in publish := true,

    // For sbt 0.13 this is what we need to avoid publishing. These settings can
    // be removed when we move to sbt 1.
    PgpKeys.publishSigned := {},
    publish := {},
    publishLocal := {},

    // We also don't need to track dependencies for unpublished projects
    // so we need to disable WhiteSource plugin.
    whitesourceIgnore := true
  )

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
