/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.sbt

import scala.collection.JavaConverters._
import scala.language.postfixOps

import sbt._
import sbt.Keys._

import play.TemplateImports
import play.runsupport.FileWatchService
import play.sbt.PlayImport.PlayKeys._
import play.sbt.PlayInternalKeys._
import play.sbt.routes.RoutesKeys
import play.sbt.run._
import play.sbt.run.PlayRun.DocsApplication
import play.twirl.sbt.Import.TwirlKeys

import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import WebKeys._

object PlaySettings {

  lazy val defaultJavaSettings = Seq[Setting[_]](

    TwirlKeys.templateImports ++= TemplateImports.defaultJavaTemplateImports.asScala,

    RoutesKeys.routesImport ++= Seq(
      "play.libs.F"
    )
  )

  lazy val defaultScalaSettings = Seq[Setting[_]](
    TwirlKeys.templateImports ++= TemplateImports.defaultScalaTemplateImports.asScala
  )

  /** Ask SBT to manage the classpath for the given configuration. */
  def manageClasspath(config: Configuration) = managedClasspath in config <<= (classpathTypes in config, update) map { (ct, report) =>
    Classpaths.managedJars(config, ct, report)
  }

  lazy val defaultSettings = Seq[Setting[_]](

    playPlugin := false,

    resolvers ++= Seq(
      "Typesafe Releases Repository" at "https://repo.typesafe.com/typesafe/releases/"
    ),

    externalizeResources := true,

    javacOptions in (Compile, doc) := List("-encoding", "utf8"),

    libraryDependencies <+= (playPlugin) {
      isPlugin =>
        if (isPlugin) {
          "com.typesafe.play" %% "play" % play.core.PlayVersion.current % "provided"
        } else {
          "com.typesafe.play" %% "play-server" % play.core.PlayVersion.current
        }
    },
    libraryDependencies += "com.typesafe.play" %% "play-test" % play.core.PlayVersion.current % "test",

    ivyConfigurations += DocsApplication,
    playOmnidoc := !play.core.PlayVersion.current.endsWith("-SNAPSHOT"),
    playDocsName := { if (playOmnidoc.value) "play-omnidoc" else "play-docs" },
    playDocsModule := Some("com.typesafe.play" %% playDocsName.value % play.core.PlayVersion.current % DocsApplication.name),
    libraryDependencies ++= playDocsModule.value.toSeq,
    manageClasspath(DocsApplication),
    playDocsJar := (managedClasspath in DocsApplication).value.files.filter(_.getName.startsWith(playDocsName.value)).headOption,

    parallelExecution in Test := false,

    fork in Test := true,

    testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "sequential", "true", "junitxml", "console"),

    testOptions in Test += Tests.Argument(TestFrameworks.JUnit, "--ignore-runners=org.specs2.runner.JUnitRunner"),

    // Adds app directory's source files to continuous hot reloading
    watchSources <++= (sourceDirectory in Compile, sourceDirectory in Assets) map { (sources, assets) =>
      (sources ** "*" --- assets ** "*").get
    },

    commands ++= {
      import PlayCommands._
      import PlayRun._
      Seq(playStartCommand, playStopCommand, h2Command, classpathCommand, licenseCommand, computeDependenciesCommand)
    },

    // THE `in Compile` IS IMPORTANT!
    Keys.run in Compile <<= PlayRun.playDefaultRunTask,

    PlayInternalKeys.playStop := {
      playInteractionMode.value match {
        case nonBlocking: PlayNonBlockingInteractionMode =>
          nonBlocking.stop()
        case _ => throw new RuntimeException("Play interaction mode must be non blocking to stop it")
      }
    },

    shellPrompt := PlayCommands.playPrompt,

    computeDependencies <<= PlayCommands.computeDependenciesTask,

    // all dependencies from outside the project (all dependency jars)
    playDependencyClasspath <<= externalDependencyClasspath in Runtime,

    // all user classes, in this project and any other subprojects that it depends on
    playReloaderClasspath <<= Classpaths.concatDistinct(exportedProducts in Runtime, internalDependencyClasspath in Runtime),

    // filter out asset directories from the classpath (supports sbt-web 1.0 and 1.1)
    playReloaderClasspath ~= { _.filter(_.get(WebKeys.webModulesLib.key).isEmpty) },

    playCommonClassloader <<= PlayCommands.playCommonClassloaderTask,

    playDependencyClassLoader := PlayRun.createURLClassLoader,

    playReloaderClassLoader := PlayRun.createDelegatedResourcesClassLoader,

    playCompileEverything <<= PlayCommands.playCompileEverythingTask,

    playReload <<= PlayCommands.playReloadTask,

    ivyLoggingLevel := UpdateLogging.DownloadOnly,

    RoutesKeys.routesImport ++= Seq("controllers.Assets.Asset"),

    sources in (Compile, RoutesKeys.routes) ++= {
      val dirs = (unmanagedResourceDirectories in Compile).value
      (dirs * "routes").get ++ (dirs * "*.routes").get
    },

    playMonitoredFiles <<= PlayCommands.playMonitoredFilesTask,

    fileWatchService := FileWatchService.defaultWatchService(target.value, pollInterval.value, sLog.value),

    playDefaultPort := 9000,
    playDefaultAddress := "0.0.0.0",

    // Default hooks

    playRunHooks := Nil,

    playInteractionMode := PlayConsoleInteractionMode,

    // sbt-web
    jsFilter in Assets := new PatternFilter("""[^_].*\.js""".r.pattern),

    WebKeys.stagingDirectory := WebKeys.stagingDirectory.value / "public",

    playAssetsWithCompilation := {
      val ignore = ((assets in Assets)?).value
      (compile in Compile).value
    },

    // Assets for run mode
    PlayRun.playPrefixAndAssetsSetting,
    PlayRun.playAllAssetsSetting,
    PlayRun.playAssetsClassLoaderSetting,
    assetsPrefix := "public/",

    // Assets for distribution
    WebKeys.packagePrefix in Assets := assetsPrefix.value,
    playPackageAssets := (packageBin in Assets).value,
    scriptClasspathOrdering += {
      val (id, art) = (projectID.value, (artifact in (Assets, packageBin)).value)
      val jarName = JavaAppPackaging.makeJarName(id.organization, id.name, id.revision, art.name, Some("assets"))
      playPackageAssets.value -> ("lib/" + jarName)
    },

    // Assets for testing
    public in TestAssets := (public in TestAssets).value / assetsPrefix.value,
    fullClasspath in Test += Attributed.blank((assets in TestAssets).value.getParentFile),

    // Settings

    devSettings := Nil,

    // Native packaging

    sourceDirectory in Universal <<= baseDirectory(_ / "dist"),

    mainClass in Compile <<= mainClass in (Compile, Keys.run),

    // Support for externalising resources
    mappings in Universal ++= {
      if (externalizeResources.value) {
        val rdirs = (unmanagedResourceDirectories in Compile).value
        val resourceMappings = ((unmanagedResources in Compile).value --- rdirs) pair (relativeTo(rdirs) | flat)
        resourceMappings.map {
          case (resource, path) => resource -> ("conf/" + path)
        }
      } else Nil
    },
    scriptClasspath := {
      if (externalizeResources.value) {
        "../conf" +: scriptClasspath.value
      } else scriptClasspath.value
    },

    mappings in Universal ++= {
      val docDirectory = (doc in Compile).value
      val docDirectoryLen = docDirectory.getCanonicalPath.length
      val pathFinder = docDirectory ** "*"
      pathFinder.get map {
        docFile: File =>
          docFile -> ("share/doc/api/" + docFile.getCanonicalPath.substring(docDirectoryLen))
      }
    },

    mappings in Universal ++= {
      val pathFinder = baseDirectory.value * "README*"
      pathFinder.get map {
        readmeFile: File =>
          readmeFile -> readmeFile.getName
      }
    },

    // Adds the Play application directory to the command line args passed to Play
    bashScriptExtraDefines += "addJava \"-Duser.dir=$(cd \"${app_home}/..\"; pwd -P)\"\n",

    generateSecret <<= ApplicationSecretGenerator.generateSecretTask,
    updateSecret <<= ApplicationSecretGenerator.updateSecretTask

  )

}
