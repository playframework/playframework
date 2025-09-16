/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import scala.jdk.CollectionConverters.*

import sbt.*
import sbt.internal.inc.Analysis
import sbt.Keys.*
import sbt.Path.*

import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.*
import com.typesafe.sbt.packager.Keys.*
import com.typesafe.sbt.web.SbtWeb.autoImport.*
import com.typesafe.sbt.web.SbtWeb.autoImport.WebKeys.*
import play.core.PlayVersion
import play.dev.filewatch.FileWatchService
import play.sbt.routes.RoutesCompiler.autoImport.*
import play.sbt.routes.RoutesKeys
import play.sbt.run.toLoggerProxy
import play.sbt.run.PlayRun
import play.sbt.PlayImport.PlayKeys.*
import play.sbt.PlayInternalKeys.*
import play.sbt.PluginCompat.*
import play.twirl.sbt.Import.TwirlKeys.*
import play.TemplateImports
import xsbti.FileConverter

object PlaySettings {
  lazy val minimalJavaSettings = Seq[Setting[?]](
    templateImports ++= TemplateImports.minimalJavaTemplateImports.asScala.toSeq,
    routesImport ++= Seq("play.libs.F")
  )

  lazy val defaultJavaSettings = Seq[Setting[?]](
    templateImports ++= TemplateImports.defaultJavaTemplateImports.asScala.toSeq,
    routesImport ++= Seq("play.libs.F")
  )

  lazy val defaultScalaSettings = Seq[Setting[?]](
    templateImports ++= TemplateImports.defaultScalaTemplateImports.asScala.toSeq
  )

  lazy val serviceGlobalSettings: Seq[Setting[?]] = Seq(
  )

  // Settings for a Play service (not a web project)
  lazy val serviceSettings: Seq[Setting[?]] = Def.settings(
    onLoadMessage := {
      val javaVersion            = sys.props("java.specification.version")
      val unsupportedJavaWarning =
        s"Java version is ${sys.props("java.specification.version")}. Play supports only Java 17, 21 and 25."
      val exclamationMarksWarning = "!".repeat(unsupportedJavaWarning.length + 4)
      """|  __              __
         |  \ \     ____   / /____ _ __  __
         |   \ \   / __ \ / // __ `// / / /
         |   / /  / /_/ // // /_/ // /_/ /
         |  /_/  / .___//_/ \__,_/ \__, /
         |      /_/               /____/
         |""".stripMargin.linesIterator.map(Colors.green).mkString("\n") +
        s"""|
            |
            |Version ${play.core.PlayVersion.current} running Java ${System.getProperty("java.version")}
            |
            |${Colors.bold(
             "Play is run entirely by the community. Please consider contributing and/or donating:"
           )}
            |https://www.playframework.com/sponsors
            |
            |""".stripMargin +
        (if (javaVersion != "17" && javaVersion != "21" && javaVersion != "25")
           s"""${Colors.red(exclamationMarksWarning)}
              |  ${Colors.red(unsupportedJavaWarning)}
              |${Colors.red(exclamationMarksWarning)}
              |
              |""".stripMargin
         else "")
    },
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-encoding", "utf8"),
    Compile / javacOptions ++= Seq("-encoding", "utf8", "-g"),
    playPlugin                   := false,
    generateAssetsJar            := true,
    externalizeResources         := true,
    externalizeResourcesExcludes := Nil,
    includeDocumentationInBinary := true,
    Compile / doc / javacOptions := List("-encoding", "utf8"),
    libraryDependencies += {
      if (playPlugin.value)
        "org.playframework" %% "play" % PlayVersion.current % "provided"
      else
        "org.playframework" %% "play-server" % PlayVersion.current
    },
    libraryDependencies += "org.playframework" %% "play-test" % PlayVersion.current % "test",
    Test / parallelExecution                   := false,
    Test / fork                                := true,
    Test / testOptions += Tests.Argument(TestFrameworks.Specs2, "sequential", "true", "junitxml", "console"),
    Test / testOptions += Tests.Argument(TestFrameworks.JUnit, "--ignore-runners=org.specs2.runner.JUnitRunner"),
    // Adds app directory's source files to continuous hot reloading
    watchSources ++= {
      ((Compile / sourceDirectory).value ** "*" --- (Assets / sourceDirectory).value ** "*").get()
    },
    commands ++= {
      import PlayCommands.*
      import PlayRun.*
      Seq(playStartCommand, playRunProdCommand, playTestProdCommand, playStopProdCommand, h2Command)
    },
    // Assets classloader (used by PlayRun.playDefaultRunTask)
    PlayInternalKeys.playAllAssets := Seq.empty,
    PlayRun.playAssetsClassLoaderSetting,
    // THE `in Compile` IS IMPORTANT!
    Compile / Keys.run             := PlayRun.playDefaultRunTask.evaluated,
    Compile / Keys.run / mainClass := Some("play.core.server.DevServerStart"),
    Compile / Keys.bgRun           := PlayRun.playDefaultBgRunTask.evaluated,
    PlayInternalKeys.playStop      := {
      playInteractionMode.value match {
        case x: PlayNonBlockingInteractionMode => x.stop()
        case _                                 => sys.error("Play interaction mode must be non blocking to stop it")
      }
    },
    shellPrompt := PlayCommands.playPrompt,
    // all dependencies from outside the project (all dependency jars)
    playDependencyClasspath := (Runtime / externalDependencyClasspath).value,
    // all user classes, in this project and any other subprojects that it depends on
    playReloaderClasspath := Classpaths
      .concatDistinct(Runtime / exportedProducts, Runtime / internalDependencyClasspath)
      .value,
    // filter out asset directories from the classpath (supports sbt-web 1.0 and 1.1)
    playReloaderClasspath ~= { _.filter(_.get(WebKeys.webModulesLib.key).isEmpty) },
    playCommonClassloader      := PlayCommands.playCommonClassloaderTask.value,
    playCompileEverything      := PlayCommands.playCompileEverythingTask.value.asInstanceOf[Seq[Analysis]],
    playReload                 := PlayCommands.playReloadTask.value,
    ivyLoggingLevel            := UpdateLogging.DownloadOnly,
    playMonitoredFiles         := PlayCommands.playMonitoredFilesTask.value,
    playMonitoredFilesExcludes := Seq.empty,
    fileWatchService           := {
      FileWatchService.detect(pollInterval.value.toMillis.toInt, sLog.value)
    },
    playDefaultPort    := 9000,
    playDefaultAddress := "0.0.0.0",
    // Default hooks
    playRunHooks        := Nil,
    playInteractionMode := PlayConsoleInteractionMode,
    // Settings
    devSettings := Nil,
    // Native packaging
    Compile / mainClass := Some("play.core.server.ProdServerStart"),
    // Support for externalising resources
    Universal / mappings ++= {
      val resourceMappings = (Compile / playExternalizedResources).value
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
          val jar                 = (Runtime / packageBin).value
          val jarSansExternalized = (Runtime / playJarSansExternalized).value
          oldValue.map {
            case (packageBinJar, _) if jar == packageBinJar =>
              val id      = projectID.value
              val art     = (playJarSansExternalized / (Compile / artifact)).value
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
    Universal / mappings ++= Def.taskDyn {
      implicit val fc: FileConverter = fileConverter.value
      // the documentation will only be included if includeDocumentation is true (see: https://www.scala-sbt.org/1.x/docs/Tasks.html#Dynamic+Computations+with)
      if (includeDocumentationInBinary.value) {
        Def.task {
          val docDirectory    = (Compile / doc).value
          val docDirectoryLen = docDirectory.getCanonicalPath.length
          val pathFinder      = docDirectory ** "*"
          pathFinder.get().map { (docFile: File) =>
            toFileRef(docFile) -> ("share/doc/api/" + docFile.getCanonicalPath.substring(docDirectoryLen))
          }
        }
      } else {
        Def.task {
          Seq[(FileRef, String)]()
        }
      }
    }.value,
    Universal / mappings ++= {
      val pathFinder                 = baseDirectory.value * "README*"
      implicit val fc: FileConverter = fileConverter.value
      pathFinder.get().map { (readmeFile: File) => toFileRef(readmeFile) -> readmeFile.getName }
    },
    // Adds the Play application directory to the command line args passed to Play
    bashScriptExtraDefines += "addJava \"-Duser.dir=$(realpath \"$(cd \"${app_home}/..\"; pwd -P)\"  $(is_cygwin && echo \"fix\"))\"\n",
    generateSecret := ApplicationSecretGenerator.generateSecretTask.value,
    updateSecret   := ApplicationSecretGenerator.updateSecretTask.value,
    // by default, compile any routes files in the root named "routes" or "*.routes"
    Compile / RoutesKeys.routes / sources ++= {
      val dirs = (Compile / unmanagedResourceDirectories).value
      (dirs * "routes").get() ++ (dirs * "*.routes").get()
    },
    inConfig(Compile)(externalizedSettings),
    // Avoid duplicated asset files in dist packages, see https://github.com/playframework/playframework/issues/5765
    Assets / WebKeys.addExportedMappingsToPackageBinMappings     := false,
    TestAssets / WebKeys.addExportedMappingsToPackageBinMappings := false,
  )

  /**
   * All default settings for a Play project with the Full (web) profile and the PlayLayout. Normally these are
   * enabled by the PlayWeb and PlayService plugin and will be added separately.
   */
  @deprecated("Use serviceSettings for a Play app or service, and add webSettings for a web app", "2.7.0")
  lazy val defaultSettings = serviceSettings ++ webSettings

  lazy val webSettings = Seq[Setting[?]](
    constructorAnnotations += "@jakarta.inject.Inject()",
    playMonitoredFiles ++= (Compile / compileTemplates / sourceDirectories).value,
    routesImport ++= Seq("controllers.Assets.Asset"),
    // The default packageOptions get set here (and do set the main class by default which we want to avoid):
    // https://github.com/sbt/sbt/blob/v1.10.0/main/src/main/scala/sbt/Defaults.scala#L1725-L1740
    Compile / packageBin / packageOptions := (Compile / packageBin / packageOptions).value.filter {
      case _: MainClass => false // The jar(s) should not contain a main class
      case _            => true
    },
    // sbt-web
    Assets / jsFilter         := new PatternFilter("""[^_].*\.js""".r.pattern),
    WebKeys.stagingDirectory  := WebKeys.stagingDirectory.value / "public",
    playAssetsWithCompilation := (Compile / compile).value.asInstanceOf[Analysis],
    playAssetsWithCompilation := playAssetsWithCompilation.dependsOn((Assets / assets).?).value,
    // Assets for run mode
    PlayRun.playPrefixAndAssetsSetting,
    PlayRun.playAllAssetsSetting,
    assetsPrefix := "public/",
    // Assets for distribution
    Assets / WebKeys.packagePrefix := assetsPrefix.value,
    // The ...-assets.jar should contain the same META-INF/MANIFEST.MF file like the main app jar
    Assets / packageBin / packageOptions := (Runtime / packageBin / packageOptions).value,
    playPackageAssets                    := (Assets / packageBin).value,
    scriptClasspathOrdering              := Def.taskDyn {
      val oldValue = scriptClasspathOrdering.value
      // only create a assets-jar if the task is active
      // this actually disables calling playPackageAssets, which in turn would call packageBin in Assets
      // without these calls no assets jar will be created
      if (generateAssetsJar.value) {
        Def.task {
          val (id, art) = (projectID.value, (Assets / packageBin / artifact).value)
          val jarName   = JavaAppPackaging.makeJarName(id.organization, id.name, id.revision, art.name, Some("assets"))
          oldValue :+ playPackageAssets.value -> ("lib/" + jarName)
        }
      } else {
        Def.task(oldValue)
      }
    }.value,
    // Assets for testing
    TestAssets / public := (TestAssets / public).value / assetsPrefix.value,
    Test / fullClasspath += Def.taskDyn {
      implicit val fc: FileConverter = fileConverter.value
      Def.task(Attributed.blank(toFileRef((TestAssets / assets).value.getParentFile)))
    }.value
  )

  /**
   * Settings for creating a jar that excludes externalized resources
   */
  private def externalizedSettings: Seq[Setting[?]] = Def.settings(
    Defaults.packageTaskSettings(playJarSansExternalized, playJarSansExternalized / mappings),
    playExternalizedResources := {
      val rdirs = unmanagedResourceDirectories.value
      (unmanagedResources.value --- rdirs --- externalizeResourcesExcludes.value)
        .pair(relativeTo(rdirs) | flat)
    },
    playJarSansExternalized / mappings := {
      implicit val fc: FileConverter = fileConverter.value
      // packageBin mappings have all the copied resources from the classes directory
      // so we need to get the copied resources, and map the source files to the destination files,
      // so we can then exclude the destination files
      val packageBinMappings = (packageBin / mappings).value
      val externalized       = playExternalizedResources.value.map(_._1).toSet
      val copied             = copyResources.value
      val toExclude          = copied.collect {
        case (source, dest) if externalized(toFileRef(source)) => toFileRef(dest)
      }.toSet
      packageBinMappings.filterNot {
        case (file, _) => toExclude(file)
      }
    },
    playJarSansExternalized / artifactClassifier := Option("sans-externalized"),
    // The ...-sans-externalized.jar should contain the same META-INF/MANIFEST.MF file like the main app jar
    playJarSansExternalized / packageOptions := (Runtime / packageBin / packageOptions).value,
  )
}
