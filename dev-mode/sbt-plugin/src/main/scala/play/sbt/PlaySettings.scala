/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import scala.language.postfixOps

import scala.collection.JavaConverters._

import sbt._
import sbt.Keys._

import play.TemplateImports
import play.core.PlayVersion
import play.dev.filewatch.FileWatchService
import play.sbt.PlayImport.PlayKeys._
import play.sbt.PlayInternalKeys._
import play.sbt.routes.RoutesKeys
import play.sbt.routes.RoutesCompiler.autoImport._
import play.sbt.run.PlayRun
import play.sbt.run.PlayRun.DocsApplication
import play.sbt.run.toLoggerProxy
import play.twirl.sbt.Import.TwirlKeys._

import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web.SbtWeb.autoImport.WebKeys._

object PlaySettings extends PlaySettingsCompat {
  lazy val minimalJavaSettings = Seq[Setting[_]](
    templateImports ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v >= 13 =>
          TemplateImports.minimalJavaTemplateImports.asScala :+ "scala.jdk.CollectionConverters._"
        case Some((2, v)) if v <= 12 =>
          TemplateImports.minimalJavaTemplateImports.asScala :+ "scala.collection.JavaConverters._"
        case _ => TemplateImports.minimalJavaTemplateImports.asScala :+ "scala.collection.JavaConverters._"
      }
    },
    routesImport ++= Seq("play.libs.F")
  )

  lazy val defaultJavaSettings = Seq[Setting[_]](
    templateImports ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v >= 13 =>
          TemplateImports.defaultJavaTemplateImports.asScala :+ "scala.jdk.CollectionConverters._"
        case Some((2, v)) if v <= 12 =>
          TemplateImports.defaultJavaTemplateImports.asScala :+ "scala.collection.JavaConverters._"
        case _ => TemplateImports.defaultJavaTemplateImports.asScala :+ "scala.collection.JavaConverters._"
      }
    },
    routesImport ++= Seq("play.libs.F")
  )

  lazy val defaultScalaSettings = Seq[Setting[_]](
    templateImports ++= TemplateImports.defaultScalaTemplateImports.asScala
  )

  /** Ask sbt to manage the classpath for the given configuration. */
  def manageClasspath(config: Configuration) = managedClasspath in config := {
    Classpaths.managedJars(config, (classpathTypes in config).value, update.value)
  }

  lazy val serviceGlobalSettings: Seq[Setting[_]] = Seq(
    playOmnidoc := false
  )

  // Settings for a Play service (not a web project)
  lazy val serviceSettings = Seq[Setting[_]](
    onLoadMessage := {
      val javaVersion = sys.props("java.specification.version")
      """|  __              __
         |  \ \     ____   / /____ _ __  __
         |   \ \   / __ \ / // __ `// / / /
         |   / /  / /_/ // // /_/ // /_/ /
         |  /_/  / .___//_/ \__,_/ \__, /
         |      /_/               /____/
         |""".stripMargin.linesIterator.map(Colors.green(_)).mkString("\n") +
        s"""|
            |
            |Version ${play.core.PlayVersion.current} running Java ${System.getProperty("java.version")}
            |
            |${Colors.bold(
             "Play is run entirely by the community. Please consider contributing and/or donating:"
           )}
            |https://www.playframework.com/sponsors
            |
            |
            |${Colors.blue(Colors.bold("Play 2.9 and 3.0 released! Play 2.8 will be end-of-life (EOL) at May 31st, 2024."))}
            |Please upgrade, see https://www.playframework.com/documentation/2.9.x/Highlights29
            |
            |
            |""".stripMargin +
        (if (javaVersion == "17")
           s"""Running Play on Java ${sys.props("java.specification.version")} is experimental. Tweaks are necessary:
              |https://github.com/playframework/playframework/releases/2.8.15
              |
              |""".stripMargin
         else if (javaVersion != "1.8" && javaVersion != "11")
           s"""!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
              |  Java version is ${sys
               .props("java.specification.version")}. Play supports only 8, 11 and, experimentally, 17.
              |!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
              |
              |""".stripMargin
         else "")
    },
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-encoding", "utf8"),
    javacOptions in Compile ++= Seq("-encoding", "utf8", "-g"),
    playPlugin                     := false,
    generateAssetsJar              := true,
    externalizeResources           := true,
    externalizeResourcesExcludes   := Nil,
    includeDocumentationInBinary   := true,
    javacOptions in (Compile, doc) := List("-encoding", "utf8"),
    libraryDependencies += {
      if (playPlugin.value)
        "com.typesafe.play" %% "play" % PlayVersion.current % "provided"
      else
        "com.typesafe.play" %% "play-server" % PlayVersion.current
    },
    libraryDependencies += "com.typesafe.play" %% "play-test" % PlayVersion.current % "test",
    ivyConfigurations += DocsApplication,
    playDocsName   := { if (playOmnidoc.value) "play-omnidoc" else "play-docs" },
    playDocsModule := Some("com.typesafe.play" %% playDocsName.value % PlayVersion.current % DocsApplication.name),
    libraryDependencies ++= playDocsModule.value.toSeq,
    manageClasspath(DocsApplication),
    playDocsJar               := (managedClasspath in DocsApplication).value.files.find(_.getName.startsWith(playDocsName.value)),
    parallelExecution in Test := false,
    fork in Test              := true,
    testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "sequential", "true", "junitxml", "console"),
    testOptions in Test += Tests.Argument(TestFrameworks.JUnit, "--ignore-runners=org.specs2.runner.JUnitRunner"),
    // Adds app directory's source files to continuous hot reloading
    watchSources ++= {
      ((sourceDirectory in Compile).value ** "*" --- (sourceDirectory in Assets).value ** "*").get
    },
    commands ++= {
      import PlayCommands._
      import PlayRun._
      Seq(playStartCommand, playRunProdCommand, playTestProdCommand, playStopProdCommand, h2Command)
    },
    // Assets classloader (used by PlayRun.playDefaultRunTask)
    PlayInternalKeys.playAllAssets := Seq.empty,
    PlayRun.playAssetsClassLoaderSetting,
    // THE `in Compile` IS IMPORTANT!
    Keys.run in Compile              := PlayRun.playDefaultRunTask.evaluated,
    mainClass in (Compile, Keys.run) := Some("play.core.server.DevServerStart"),
    PlayInternalKeys.playStop := {
      playInteractionMode.value match {
        case x: PlayNonBlockingInteractionMode => x.stop()
        case _                                 => sys.error("Play interaction mode must be non blocking to stop it")
      }
    },
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
    playCompileEverything := getPlayCompileEverything(PlayCommands.playCompileEverythingTask.value),
    playReload            := PlayCommands.playReloadTask.value,
    ivyLoggingLevel       := UpdateLogging.DownloadOnly,
    playMonitoredFiles    := PlayCommands.playMonitoredFilesTask.value,
    fileWatchService := FileWatchService
      .defaultWatchService(target.value, getPoolInterval(pollInterval.value).toMillis.toInt, sLog.value),
    playDefaultPort    := 9000,
    playDefaultAddress := "0.0.0.0",
    // Default hooks
    playRunHooks        := Nil,
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
    mappings in Universal ++= Def.taskDyn {
      // the documentation will only be included if includeDocumentation is true (see: http://www.scala-sbt.org/1.0/docs/Tasks.html#Dynamic+Computations+with)
      if (includeDocumentationInBinary.value) {
        Def.task {
          val docDirectory    = (doc in Compile).value
          val docDirectoryLen = docDirectory.getCanonicalPath.length
          val pathFinder      = docDirectory ** "*"
          pathFinder.get.map { docFile: File =>
            docFile -> ("share/doc/api/" + docFile.getCanonicalPath.substring(docDirectoryLen))
          }
        }
      } else {
        Def.task {
          Seq[(sbt.File, String)]()
        }
      }
    }.value,
    mappings in Universal ++= {
      val pathFinder = baseDirectory.value * "README*"
      pathFinder.get.map { readmeFile: File => readmeFile -> readmeFile.getName }
    },
    // Adds the Play application directory to the command line args passed to Play
    bashScriptExtraDefines += "addJava \"-Duser.dir=$(realpath \"$(cd \"${app_home}/..\"; pwd -P)\"  $(is_cygwin && echo \"fix\"))\"\n",
    generateSecret := ApplicationSecretGenerator.generateSecretTask.value,
    updateSecret   := ApplicationSecretGenerator.updateSecretTask.value,
    // by default, compile any routes files in the root named "routes" or "*.routes"
    sources in (Compile, RoutesKeys.routes) ++= {
      val dirs = (unmanagedResourceDirectories in Compile).value
      (dirs * "routes").get ++ (dirs * "*.routes").get
    }
  ) ++ inConfig(Compile)(externalizedSettings)

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
    jsFilter in Assets        := new PatternFilter("""[^_].*\.js""".r.pattern),
    WebKeys.stagingDirectory  := WebKeys.stagingDirectory.value / "public",
    playAssetsWithCompilation := getPlayAssetsWithCompilation((compile in Compile).value),
    playAssetsWithCompilation := playAssetsWithCompilation.dependsOn((assets in Assets).?).value,
    // Assets for run mode
    PlayRun.playPrefixAndAssetsSetting,
    PlayRun.playAllAssetsSetting,
    assetsPrefix := "public/",
    // Assets for distribution
    WebKeys.packagePrefix in Assets := assetsPrefix.value,
    playPackageAssets               := (packageBin in Assets).value,
    scriptClasspathOrdering := Def.taskDyn {
      val oldValue = scriptClasspathOrdering.value
      // only create a assets-jar if the task is active
      // this actually disables calling playPackageAssets, which in turn would call packageBin in Assets
      // without these calls no assets jar will be created
      if (generateAssetsJar.value) {
        Def.task {
          val (id, art) = (projectID.value, (artifact in (Assets, packageBin)).value)
          val jarName   = JavaAppPackaging.makeJarName(id.organization, id.name, id.revision, art.name, Some("assets"))
          oldValue :+ playPackageAssets.value -> ("lib/" + jarName)
        }
      } else {
        Def.task(oldValue)
      }
    }.value,
    // Assets for testing
    public in TestAssets := (public in TestAssets).value / assetsPrefix.value,
    fullClasspath in Test += Attributed.blank((assets in TestAssets).value.getParentFile)
  )

  /**
   * Settings for creating a jar that excludes externalized resources
   */
  private def externalizedSettings: Seq[Setting[_]] =
    Defaults.packageTaskSettings(playJarSansExternalized, mappings in playJarSansExternalized) ++ Seq(
      playExternalizedResources := getPlayExternalizedResources(
        unmanagedResourceDirectories.value,
        unmanagedResources.value,
        externalizeResourcesExcludes.value
      ),
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
      artifactClassifier in playJarSansExternalized := Option("sans-externalized")
    )
}
