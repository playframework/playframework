/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play

import play.runsupport.PlayWatchService
import play.sbtplugin.run._
import sbt._
import sbt.Keys._
import play.PlayImport._
import PlayKeys._
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import play.sbtplugin.ApplicationSecretGenerator
import com.typesafe.sbt.web.SbtWeb.autoImport._
import WebKeys._
import scala.language.postfixOps
import play.twirl.sbt.Import.TwirlKeys

trait PlaySettings {
  this: PlayCommands with PlayPositionMapper with PlayRun with PlaySourceGenerators =>

  lazy val defaultJavaSettings = Seq[Setting[_]](

    TwirlKeys.templateImports ++= defaultJavaTemplateImports,

    routesImport ++= Seq(
      "play.libs.F"
    ),

    ebeanEnabled := true

  )

  lazy val defaultScalaSettings = Seq[Setting[_]](
    TwirlKeys.templateImports ++= defaultScalaTemplateImports
  )

  def closureCompilerSettings(optionCompilerOptions: com.google.javascript.jscomp.CompilerOptions) = Seq[Setting[_]](
    resourceGenerators in Compile <<= JavascriptCompiler(Some(optionCompilerOptions))(Seq(_)),
    resourceGenerators in Compile <+= LessCompiler,
    resourceGenerators in Compile <+= CoffeescriptCompiler
  )

  /** Ask SBT to manage the classpath for the given configuration. */
  def manageClasspath(config: Configuration) = managedClasspath in config <<= (classpathTypes in config, update) map { (ct, report) =>
    Classpaths.managedJars(config, ct, report)
  }

  lazy val defaultSettings = Defaults.packageTaskSettings(playPackageAssets, playPackageAssetsMappings) ++ Seq[Setting[_]](

    playPlugin := false,

    resolvers ++= Seq(
      "Typesafe Releases Repository" at "https://repo.typesafe.com/typesafe/releases/"
    ),

    target <<= baseDirectory(_ / "target"),

    sourceDirectory in Compile <<= baseDirectory(_ / "app"),
    sourceDirectory in Test <<= baseDirectory(_ / "test"),

    confDirectory <<= baseDirectory(_ / "conf"),

    resourceDirectory in Compile <<= baseDirectory(_ / "conf"),

    scalaSource in Compile <<= baseDirectory(_ / "app"),
    scalaSource in Test <<= baseDirectory(_ / "test"),

    javaSource in Compile <<= baseDirectory(_ / "app"),
    javaSource in Test <<= baseDirectory(_ / "test"),

    sourceDirectories in (Compile, TwirlKeys.compileTemplates) := Seq((sourceDirectory in Compile).value),
    sourceDirectories in (Test, TwirlKeys.compileTemplates) := Seq((sourceDirectory in Test).value),

    javacOptions in (Compile, doc) := List("-encoding", "utf8"),

    libraryDependencies <+= (playPlugin) {
      isPlugin =>
        val d = "com.typesafe.play" %% "play" % play.core.PlayVersion.current
        if (isPlugin)
          d % "provided"
        else
          d
    },
    libraryDependencies += "com.typesafe.play" %% "play-test" % play.core.PlayVersion.current % "test",

    ivyConfigurations += DocsApplication,
    libraryDependencies += "com.typesafe.play" %% "play-docs" % play.core.PlayVersion.current % DocsApplication.name,
    manageClasspath(DocsApplication),

    parallelExecution in Test := false,

    fork in Test := true,

    testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "sequential", "true", "junitxml", "console"),

    testOptions in Test += Tests.Argument(TestFrameworks.JUnit, "--ignore-runners=org.specs2.runner.JUnitRunner"),

    // Make sure Specs2 is at the end of the list of test frameworks, so that it gets priority over
    // JUnit. This is a hack/workaround to prevent Specs2 tests with @RunsWith annotations being
    // picked up by JUnit. We don't want JUnit to run the tests since JUnit ignores the Specs2
    // runnner, which means the tests run but their results are ignored by SBT.
    testFrameworks ~= {
      tf => tf.filter(_ != TestFrameworks.Specs2).:+(TestFrameworks.Specs2)
    },

    testResultReporter <<= testResultReporterTask,

    testResultReporterReset <<= testResultReporterResetTask,

    generateReverseRouter := true,

    generateRefReverseRouter := true,

    namespaceReverseRouter := false,

    sourceGenerators in Compile <+= (state, confDirectory, sourceManaged in Compile, routesImport, generateReverseRouter, generateRefReverseRouter, namespaceReverseRouter) map {
      (s, cd, sm, ri, grr, grrr, nrr) => RouteFiles(s, Seq(cd), sm, ri, grr, grrr, nrr)
    },

    // Adds config directory's source files to continuous hot reloading
    watchSources <+= confDirectory map {
      all => all
    },

    // Adds app directory's source files to continuous hot reloading
    watchSources <++= baseDirectory map {
      path => ((path / "app") ** "*" --- (path / "app/assets") ** "*").get
    },

    commands ++= Seq(shCommand, playStartCommand, h2Command, classpathCommand, licenseCommand, computeDependenciesCommand),

    // THE `in Compile` IS IMPORTANT!
    run in Compile <<= playDefaultRunTask,

    playStop := {
      playInteractionMode.value match {
        case nonBlocking: PlayNonBlockingInteractionMode =>
          nonBlocking.stop()
        case _ => throw new RuntimeException("Play interaction mode must be non blocking to stop it")
      }
    },

    shellPrompt := playPrompt,

    mainClass in (Compile, run) := Some("play.core.server.NettyServer"),

    compile in Compile <<= PostCompile(scope = Compile),

    compile in Test <<= PostCompile(Test),

    computeDependencies <<= computeDependenciesTask,

    playVersion := play.core.PlayVersion.current,

    // all dependencies from outside the project (all dependency jars)
    playDependencyClasspath <<= externalDependencyClasspath in Runtime,

    // all user classes, in this project and any other subprojects that it depends on
    playReloaderClasspath <<= Classpaths.concatDistinct(exportedProducts in Runtime, internalDependencyClasspath in Runtime),

    // filter out asset directories from the classpath (supports sbt-web 1.0 and 1.1)
    playReloaderClasspath ~= { _.filter(_.get(WebKeys.webModulesLib.key).isEmpty) },

    playCommonClassloader <<= playCommonClassloaderTask,

    playDependencyClassLoader := createURLClassLoader,

    playReloaderClassLoader := createDelegatedResourcesClassLoader,

    playCompileEverything <<= playCompileEverythingTask,

    playReload <<= playReloadTask,

    sourcePositionMappers += playPositionMapper,

    ivyLoggingLevel := UpdateLogging.DownloadOnly,

    routesImport := Seq("controllers.Assets.Asset"),

    playMonitoredFiles <<= playMonitoredFilesTask,

    playWatchService := PlayWatchService.default(target.value, pollInterval.value, sLog.value),

    playDefaultPort := 9000,

    // Default hooks

    playRunHooks := Nil,

    playInteractionMode := play.PlayConsoleInteractionMode,

    // Assets

    requireJs := Nil,

    requireJsFolder := "",

    requireJsShim := "",

    requireNativePath := None,

    buildRequire <<= buildRequireTask,

    packageBin in Compile <<= (packageBin in Compile).dependsOn(buildRequire),

    lessEntryPoints <<= (sourceDirectory in Compile)(base => ((base / "assets" ** "*.less") --- base / "assets" ** "_*")),
    coffeescriptEntryPoints <<= (sourceDirectory in Compile)(base => base / "assets" ** "*.coffee"),
    javascriptEntryPoints <<= (sourceDirectory in Compile)(base => ((base / "assets" ** "*.js") --- (base / "assets" ** "_*"))),

    lessOptions := Seq.empty[String],
    coffeescriptOptions := Seq.empty[String],
    closureCompilerOptions := Seq.empty[String],

    // sbt-web
    sourceDirectory in Assets := (sourceDirectory in Compile).value / "assets",
    sourceDirectory in TestAssets := (sourceDirectory in Test).value / "assets",

    jsFilter in Assets := new PatternFilter("""[^_].*\.js""".r.pattern),
    resourceDirectory in Assets := baseDirectory.value / "public",

    WebKeys.stagingDirectory := WebKeys.stagingDirectory.value / "public",

    playAssetsWithCompilation := {
      val ignore = ((assets in Assets)?).value
      (compile in Compile).value
    },

    // Assets
    playAggregateAssets := true,

    // Assets for run mode
    playPrefixAndAssetsSetting,
    playAllAssetsSetting,
    playAssetsClassLoaderSetting,
    assetsPrefix := "public/",

    // Assets for distribution
    playPrefixAndPipelineSetting,
    playPackageAssetsMappingsSetting,
    artifactClassifier in playPackageAssets := Some("assets"),
    Keys.artifactName in playPackageAssets := { (_, mid, art) =>
      val classifier = art.classifier match {
        case None => ""
        case Some(c) => "-" + c
      }
      art.name + "-" + mid.revision + classifier + "." + art.extension
    },
    artifactPath in playPackageAssets := {
      val sv = ScalaVersion((scalaVersion in Keys.artifactName).value, (scalaBinaryVersion in Keys.artifactName).value)
      target.value / (Keys.artifactName in playPackageAssets).value(sv, projectID.value, (artifact in playPackageAssets).value)
    },
    mappings in Universal += (playPackageAssets.value -> ("lib/" + organization.value + "." + playPackageAssets.value.getName)),
    scriptClasspath += (organization.value + "." + playPackageAssets.value.getName),

    // Assets for testing
    public in TestAssets := (public in TestAssets).value / assetsPrefix.value,
    fullClasspath in Test ++= {
      val testAssetDirs = {
        if (playAggregateAssets.value)
          ((assets in TestAssets) ?).all(ScopeFilter(inDependencies(ThisProject))).value.flatten
        else
          Seq((assets in TestAssets).value)
      }
      testAssetDirs.map(dir => Attributed.blank(dir.getParentFile))
    },

    // Settings

    devSettings := Nil,

    // Templates

    TwirlKeys.templateImports ++= defaultTemplateImports,

    // Native packaging

    sourceDirectory in Universal <<= baseDirectory(_ / "dist"),

    mainClass in Compile := Some("play.core.server.NettyServer"),

    mappings in Universal ++= {
      val confDirectoryLen = confDirectory.value.getCanonicalPath.length
      val pathFinder = confDirectory.value ** ("*" -- "routes")
      pathFinder.get map {
        confFile: File =>
          confFile -> ("conf/" + confFile.getCanonicalPath.substring(confDirectoryLen))
      }
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
