/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import scala.collection.JavaConverters._

import sbt._
import sbt.Keys._
import sbt.Path._
import sbt.internal.inc.Analysis

import play.core.PlayVersion
import play.dev.filewatch.FileWatchService
import play.sbt.PlayImport.PlayKeys._
import play.sbt.PlayInternalKeys._
import play.sbt.routes.RoutesKeys
import play.sbt.routes.RoutesCompiler.autoImport._
import play.sbt.run.toLoggerProxy
import play.twirl.sbt.Import.TwirlKeys._

import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web.SbtWeb.autoImport.WebKeys._

object PlaySettings {
  lazy val minimalJavaSettings = Seq[Setting[_]](
    routesImport ++= Seq("play.libs.F")
  )

  lazy val defaultJavaSettings = Seq[Setting[_]](
    routesImport ++= Seq("play.libs.F")
  )

  lazy val defaultScalaSettings = Seq[Setting[_]]()

  lazy val serviceGlobalSettings: Seq[Setting[_]] = Seq()

  // Settings for a Play service (not a web project)
  lazy val serviceSettings: Seq[Setting[_]] = Def.settings(
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-encoding", "utf8"),
    javacOptions in Compile ++= Seq("-encoding", "utf8", "-g"),
    playPlugin := false,
    externalizeResources := true,
    externalizeResourcesExcludes := Nil,
    javacOptions in (Compile, doc) := List("-encoding", "utf8"),
    libraryDependencies += {
      if (playPlugin.value)
        "com.typesafe.play" %% "play" % PlayVersion.current % "provided"
      else
        "com.typesafe.play" %% "play-server" % PlayVersion.current
    },
    fork in Test := true,
    // Adds app directory's source files to continuous hot reloading
    watchSources ++= {
      ((sourceDirectory in Compile).value ** "*" --- (sourceDirectory in Assets).value ** "*").get
    },
    // Assets classloader (used by PlayRun.playDefaultRunTask)
    PlayInternalKeys.playAllAssets := Seq.empty,
    shellPrompt := PlayCommands.playPrompt,
    // all dependencies from outside the project (all dependency jars)
    playDependencyClasspath := (externalDependencyClasspath in Runtime).value,
    // all user classes, in this project and any other subprojects that it depends on
    playReloaderClasspath := Classpaths
      .concatDistinct(exportedProducts in Runtime, internalDependencyClasspath in Runtime)
      .value,
    // filter out asset directories from the classpath (supports sbt-web 1.0 and 1.1)
    playReloaderClasspath ~= { _.filter(_.get(WebKeys.webModulesLib.key).isEmpty) },
    playCommonClassloader := PlayCommands.playCommonClassloaderTask.value,
    playCompileEverything := PlayCommands.playCompileEverythingTask.value.asInstanceOf[Seq[Analysis]],
    playReload := PlayCommands.playReloadTask.value,
    ivyLoggingLevel := UpdateLogging.DownloadOnly,
    playMonitoredFiles := PlayCommands.playMonitoredFilesTask.value,
    fileWatchService := {
      FileWatchService.defaultWatchService(target.value, pollInterval.value.toMillis.toInt, sLog.value)
    },
    playDefaultPort := 9000,
    playDefaultAddress := "0.0.0.0",
    playInteractionMode := PlayConsoleInteractionMode,
    // Settings
    devSettings := Nil,
    // Native packaging
    mainClass in Compile := Some("play.core.server.ProdServerStart"),
    // Support for externalising resources
    mappings in Universal ++= {
      val resourceMappings = (playExternalizedResources in Compile).value
      if (externalizeResources.value) {
        resourceMappings.map {
          case (resource, path) => resource -> ("conf/" + path)
        }
      } else Nil
    },
    scriptClasspath := {
      val scriptClasspathValue = scriptClasspath.value
      if (externalizeResources.value) {
        "../conf/" +: scriptClasspathValue
      } else scriptClasspathValue
    },
    // taskDyn ensures we only build the sans externalised jar if we need to
    scriptClasspathOrdering := Def.taskDyn {
      val oldValue = scriptClasspathOrdering.value
      if (externalizeResources.value) {
        Def.task {
          // Filter out the regular jar
          val jar                 = (packageBin in Runtime).value
          val jarSansExternalized = (playJarSansExternalized in Runtime).value
          oldValue.map {
            case (packageBinJar, _) if jar == packageBinJar =>
              val id  = projectID.value
              val art = (artifact in Compile in playJarSansExternalized).value
              val jarName =
                JavaAppPackaging.makeJarName(id.organization, id.name, id.revision, art.name, art.classifier)
              jarSansExternalized -> ("lib/" + jarName)
            case other => other
          }
        }
      } else {
        Def.task(oldValue)
      }
    }.value,
    // Adds the Play application directory to the command line args passed to Play
    bashScriptExtraDefines += "addJava \"-Duser.dir=$(realpath \"$(cd \"${app_home}/..\"; pwd -P)\"  $(is_cygwin && echo \"fix\"))\"\n",
    // by default, compile any routes files in the root named "routes" or "*.routes"
    sources in (Compile, RoutesKeys.routes) ++= {
      val dirs = (unmanagedResourceDirectories in Compile).value
      (dirs * "routes").get ++ (dirs * "*.routes").get
    },
    inConfig(Compile)(externalizedSettings),
  )

  /**
   * All default settings for a Play project with the Full (web) profile and the PlayLayout. Normally these are
   * enabled by the PlayWeb and PlayService plugin and will be added separately.
   */
  @deprecated("Use serviceSettings for a Play app or service, and add webSettings for a web app", "2.7.0")
  lazy val defaultSettings = serviceSettings ++ webSettings

  lazy val webSettings = Seq[Setting[_]](
    constructorAnnotations += "@javax.inject.Inject()",
    playMonitoredFiles ++= (sourceDirectories in (Compile, compileTemplates)).value,
    routesImport ++= Seq("controllers.Assets.Asset"),
    // sbt-web
    jsFilter in Assets := new PatternFilter("""[^_].*\.js""".r.pattern),
    WebKeys.stagingDirectory := WebKeys.stagingDirectory.value / "public",
    playAssetsWithCompilation := (compile in Compile).value.asInstanceOf[Analysis],
    playAssetsWithCompilation := playAssetsWithCompilation.dependsOn((assets in Assets).?).value,
    assetsPrefix := "public/",
    // Assets for distribution
    WebKeys.packagePrefix in Assets := assetsPrefix.value,
    scriptClasspathOrdering := Def.taskDyn {
      val oldValue = scriptClasspathOrdering.value
      Def.task(oldValue)
    }.value,
    // Assets for testing
    public in TestAssets := (public in TestAssets).value / assetsPrefix.value,
    fullClasspath in Test += Attributed.blank((assets in TestAssets).value.getParentFile)
  )

  /**
   * Settings for creating a jar that excludes externalized resources
   */
  private def externalizedSettings: Seq[Setting[_]] = Def.settings(
    Defaults.packageTaskSettings(playJarSansExternalized, mappings in playJarSansExternalized),
    playExternalizedResources := {
      val rdirs = unmanagedResourceDirectories.value
      (unmanagedResources.value --- rdirs --- externalizeResourcesExcludes.value)
        .pair(relativeTo(rdirs) | flat)
    },
    mappings in playJarSansExternalized := {
      // packageBin mappings have all the copied resources from the classes directory
      // so we need to get the copied resources, and map the source files to the destination files,
      // so we can then exclude the destination files
      val packageBinMappings = (mappings in packageBin).value
      val externalized       = playExternalizedResources.value.map(_._1).toSet
      val copied             = copyResources.value
      val toExclude = copied.collect {
        case (source, dest) if externalized(source) => dest
      }.toSet
      packageBinMappings.filterNot {
        case (file, _) => toExclude(file)
      }
    },
    artifactClassifier in playJarSansExternalized := Option("sans-externalized"),
  )
}
