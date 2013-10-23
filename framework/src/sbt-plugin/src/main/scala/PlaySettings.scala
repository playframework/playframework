/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play

import sbt.{ Project => SbtProject, _ }
import sbt.Keys._
import Keys._
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import java.io.{ Writer, PrintWriter }
import play.console.Colors

trait Settings {
  this: PlayCommands with PlayPositionMapper with PlayRun with PlaySourceGenerators =>

  lazy val defaultJavaSettings = Seq[Setting[_]](

    templatesImport ++= defaultJavaTemplatesImport,

    routesImport ++= Seq(
      "play.libs.F"
    ),

    ebeanEnabled := true

  )

  lazy val defaultScalaSettings = Seq[Setting[_]](
    templatesImport ++= defaultScalaTemplatesImport
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

  lazy val defaultSettings = Seq[Setting[_]](

    scalaVersion := play.core.PlayVersion.scalaVersion,

    playPlugin := false,

    resolvers ++= Seq(
      "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/"
    ),

    target <<= baseDirectory / "target",

    sourceDirectory in Compile <<= baseDirectory / "app",
    sourceDirectory in Test <<= baseDirectory / "test",

    confDirectory <<= baseDirectory / "conf",

    resourceDirectory in Compile <<= baseDirectory / "conf",

    scalaSource in Compile <<= baseDirectory / "app",
    scalaSource in Test <<= baseDirectory / "test",

    javaSource in Compile <<= baseDirectory / "app",
    javaSource in Test <<= baseDirectory / "test",

    javacOptions in (Compile, doc) := List("-encoding", "utf8"),

    libraryDependencies <+= (playPlugin) { isPlugin =>
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
    testFrameworks ~= { tf => tf.filter(_ != TestFrameworks.Specs2).:+(TestFrameworks.Specs2) },

    testListeners <<= (target, streams).map((t, s) => Seq(new eu.henkelmann.sbt.JUnitXmlTestsListener(t.getAbsolutePath, s.log))),

    testResultReporter <<= testResultReporterTask,

    testResultReporterReset <<= testResultReporterResetTask,

    generateReverseRouter := true,

    namespaceReverseRouter := false,

    sourceGenerators in Compile <+= (state, confDirectory, sourceManaged in Compile, routesImport, generateReverseRouter, namespaceReverseRouter) map { (s, cd, sm, ri, grr, nrr) =>
      RouteFiles(s, Seq(cd), sm, ri, grr, nrr)
    },

    // Adds config directory's source files to continuous hot reloading
    watchSources <+= confDirectory map { all => all },

    sourceGenerators in Compile <+= (state, unmanagedSourceDirectories in Compile, sourceManaged in Compile, templatesTypes, templatesImport) map ScalaTemplates,

    // Adds app directory's source files to continuous hot reloading
    watchSources <++= baseDirectory map { path => ((path / "app") ** "*" --- (path / "app/assets") ** "*").get },

    commands ++= Seq(shCommand, playCommand, playStartCommand, h2Command, classpathCommand, licenseCommand, computeDependenciesCommand),

    // THE `in Compile` IS IMPORTANT!
    run in Compile <<= playRunSetting,

    shellPrompt := playPrompt,

    copyResources in Compile <<= (copyResources in Compile, playCopyAssets) map { (r, pr) => r ++ pr },

    mainClass in (Compile, run) := Some("play.core.server.NettyServer"),

    compile in (Compile) <<= PostCompile(scope = Compile),

    compile in Test <<= PostCompile(Test),

    computeDependencies <<= computeDependenciesTask,

    playVersion := play.core.PlayVersion.current,

    // all dependencies from outside the project (all dependency jars)
    playDependencyClasspath <<= externalDependencyClasspath in Runtime,

    // all user classes, in this project and any other subprojects that it depends on
    playReloaderClasspath <<= Classpaths.concatDistinct(exportedProducts in Runtime, internalDependencyClasspath in Runtime),

    playCommonClassloader <<= playCommonClassloaderTask,

    playDependencyClassLoader := createURLClassLoader,

    playReloaderClassLoader := createDelegatedResourcesClassLoader,

    playCopyAssets <<= playCopyAssetsTask,

    playCompileEverything <<= playCompileEverythingTask,

    playReload <<= playReloadTask,

    sourcePositionMappers += playPositionMapper,

    ivyLoggingLevel := UpdateLogging.DownloadOnly,

    routesImport := Seq.empty[String],

    playMonitoredFiles <<= playMonitoredFilesTask,

    playDefaultPort := 9000,

    // Default hooks

    playOnStarted := Nil,

    playOnStopped := Nil,

    playRunHooks := Nil,

    playRunHooks <++= playOnStarted map { funcs =>
      funcs map play.PlayRunHook.makeRunHookFromOnStarted
    },

    playRunHooks <++= playOnStopped map { funcs =>
      funcs map play.PlayRunHook.makeRunHookFromOnStopped
    },

    playInteractionMode := play.PlayConsoleInteractionMode,

    // Assets

    playAssetsDirectories := Seq.empty[File],

    playExternalAssets := Seq.empty[(File, File => PathFinder, String)],

    playAssetsDirectories <+= baseDirectory / "public",

    requireJs := Nil,

    requireJsFolder := "",

    requireJsShim := "",

    requireNativePath := None,

    buildRequire <<= buildRequireTask,

    packageBin in Compile <<= (packageBin in Compile).dependsOn(buildRequire),

    resourceGenerators in Compile <+= LessCompiler,
    resourceGenerators in Compile <+= CoffeescriptCompiler,
    resourceGenerators in Compile <+= JavascriptCompiler(fullCompilerOptions = None),

    lessEntryPoints <<= (sourceDirectory in Compile)(base => ((base / "assets" ** "*.less") --- base / "assets" ** "_*")),
    coffeescriptEntryPoints <<= (sourceDirectory in Compile)(base => base / "assets" ** "*.coffee"),
    javascriptEntryPoints <<= (sourceDirectory in Compile)(base => ((base / "assets" ** "*.js") --- (base / "assets" ** "_*"))),

    lessOptions := Seq.empty[String],
    coffeescriptOptions := Seq.empty[String],
    closureCompilerOptions := Seq.empty[String],

    // Settings

    devSettings := Nil,

    // Templates

    templatesImport := defaultTemplatesImport,

    templatesTypes := Map(
      "html" -> "play.api.templates.HtmlFormat",
      "txt" -> "play.api.templates.TxtFormat",
      "xml" -> "play.api.templates.XmlFormat",
      "js" -> "play.api.templates.JavaScriptFormat"
    ),

    // Native packaging

    sourceDirectory in Universal <<= baseDirectory(_ / "dist"),

    mainClass in Compile := Some("play.core.server.NettyServer"),

    mappings in Universal <++= (confDirectory) map {
      confDirectory: File =>
        val confDirectoryLen = confDirectory.getCanonicalPath.length
        val pathFinder = confDirectory ** ("*" -- "routes")
        pathFinder.get map {
          confFile: File =>
            confFile -> ("conf/" + confFile.getCanonicalPath.substring(confDirectoryLen))
        }
    },

    mappings in Universal <++= (doc in Compile) map {
      docDirectory: File =>
        val docDirectoryLen = docDirectory.getCanonicalPath.length
        val pathFinder = docDirectory ** "*"
        pathFinder.get map {
          docFile: File =>
            docFile -> ("share/doc/api/" + docFile.getCanonicalPath.substring(docDirectoryLen))
        }
    },

    mappings in Universal <++= (baseDirectory) map {
      baseDirectory: File =>
        val pathFinder = baseDirectory * "README*"
        pathFinder.get map {
          readmeFile: File =>
            readmeFile -> readmeFile.getName
        }
    },

    // Adds the Play application directory to the command line args passed to Play
    bashScriptExtraDefines += "addJava \"-Duser.dir=$(cd \"${app_home}/..\"; pwd -P)\"\n"

  )

  /**
   * Add this to your build.sbt, eg:
   *
   * {{{
   *   play.Project.emojiLogs
   * }}}
   *
   * Note that this setting is not supported and may break or be removed or changed at any time.
   */
  lazy val emojiLogs = logManager ~= { lm =>
    new LogManager {
      def apply(data: sbt.Settings[Scope], state: State, task: Def.ScopedKey[_], writer: java.io.PrintWriter) = {
        val l = lm.apply(data, state, task, writer)
        val FailuresErrors = "(?s).*(\\d+) failures?, (\\d+) errors?.*".r
        new Logger {
          def filter(s: String) = {
            val filtered = s.replace("\033[32m+\033[0m", "\u2705 ")
              .replace("\033[33mx\033[0m", "\u274C ")
              .replace("\033[31m!\033[0m", "\uD83D\uDCA5 ")
            filtered match {
              case FailuresErrors("0", "0") => filtered + " \uD83D\uDE04"
              case FailuresErrors(_, _) => filtered + " \uD83D\uDE22"
              case _ => filtered
            }
          }
          def log(level: Level.Value, message: => String) = l.log(level, filter(message))
          def success(message: => String) = l.success(message)
          def trace(t: => Throwable) = l.trace(t)

          override def ansiCodesSupported = l.ansiCodesSupported
        }
      }
    }
  }
}
