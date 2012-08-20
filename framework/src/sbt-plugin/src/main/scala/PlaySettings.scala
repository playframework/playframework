package sbt

import Keys._
import PlayKeys._

trait PlaySettings {
  this: PlayCommands =>

  lazy val defaultJavaSettings = Seq[Setting[_]](

    templatesImport ++= Seq(
      "models._",
      "controllers._",

      "java.lang._",
      "java.util._",

      "scala.collection.JavaConversions._",
      "scala.collection.JavaConverters._",

      "play.api.i18n._",
      "play.api.templates.PlayMagicForJava._",

      "play.mvc._",
      "play.data._",
      "play.api.data.Field",
      "com.avaje.ebean._",

      "play.mvc.Http.Context.Implicit._",

      "views.%format%._"),

    routesImport ++= Seq(
      "play.libs.F"
    ),

    ebeanEnabled := true

  )

  lazy val defaultScalaSettings = Seq[Setting[_]](

    templatesImport ++= Seq(
      "models._",
      "controllers._",

      "play.api.i18n._",

      "play.api.mvc._",
      "play.api.data._",

      "views.%format%._"))

  def closureCompilerSettings(optionCompilerOptions: com.google.javascript.jscomp.CompilerOptions) = Seq[Setting[_]](
    resourceGenerators in Compile <<= JavascriptCompiler(Some(optionCompilerOptions))(Seq(_)),
    resourceGenerators in Compile <+= LessCompiler,
    resourceGenerators in Compile <+= CoffeescriptCompiler
  )

  lazy val defaultSettings = Seq[Setting[_]](

    resolvers ++= Seq(
      "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
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

    distDirectory <<= baseDirectory / "dist",

    libraryDependencies += "play" %% "play" % play.core.PlayVersion.current,

    libraryDependencies += "play" %% "play-test" % play.core.PlayVersion.current % "test",

    parallelExecution in Test := false,

    //TODO: this should be re-enabled once we know more about xsbt/issues/512
    fork in Test := false,

    testOptions in Test += Tests.Setup { loader =>
      loader.loadClass("play.api.Logger").getMethod("init", classOf[java.io.File]).invoke(null, new java.io.File("."))
    },

    testOptions in Test += Tests.Cleanup { loader =>
      loader.loadClass("play.api.Logger").getMethod("shutdown").invoke(null)
    },

    testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "sequential", "true"),

    testOptions in Test += Tests.Argument(TestFrameworks.JUnit, "junitxml", "console"),

    testListeners <<= (target, streams).map((t, s) => Seq(new eu.henkelmann.sbt.JUnitXmlTestsListener(t.getAbsolutePath, s.log))),

    testResultReporter <<= testResultReporterTask,

    testResultReporterReset <<= testResultReporterResetTask,

    sourceGenerators in Compile <+= (confDirectory, sourceManaged in Compile, routesImport) map RouteFiles,

    // Adds config directory's source files to continuous hot reloading 
    watchSources <+= confDirectory map { all => all },

    sourceGenerators in Compile <+= (sourceDirectory in Compile, sourceManaged in Compile, templatesTypes, templatesImport) map ScalaTemplates,

    // Adds app directory's source files to continuous hot reloading
    watchSources <++= baseDirectory map { path => ((path / "app") ** "*").get },

    commands ++= Seq(shCommand, playCommand, playRunCommand, playStartCommand, h2Command, classpathCommand, licenseCommand, computeDependenciesCommand),

    shellPrompt := playPrompt,

    copyResources in Compile <<= (copyResources in Compile, playCopyAssets) map { (r, pr) => r ++ pr },

    mainClass in (Compile, run) := Some(classOf[play.core.server.NettyServer].getName),

    compile in (Compile) <<= PostCompile(scope = Compile),

    dist <<= distTask,

    computeDependencies <<= computeDependenciesTask,

    playVersion := play.core.PlayVersion.current,

    playCommonClassloader <<= playCommonClassloaderTask,

    playCopyAssets <<= playCopyAssetsTask,

    playCompileEverything <<= playCompileEverythingTask,

    playPackageEverything <<= playPackageEverythingTask,

    playReload <<= playReloadTask,

    playStage <<= playStageTask,

    cleanFiles <+= distDirectory,

    ebeanEnabled := false,

    logManager <<= extraLoggers(PlayLogManager.default),

    ivyLoggingLevel := UpdateLogging.DownloadOnly,

    routesImport := Seq.empty[String],

    playMonitoredDirectories <<= playMonitoredDirectoriesTask,

    playDefaultPort := 9000,

    playOnStarted := Nil,

    playOnStopped := Nil,

    // Assets

    playAssetsDirectories := Seq.empty[File],

    playExternalAssets := Seq.empty[(File, File => PathFinder, String)],

    playAssetsDirectories <+= baseDirectory / "public",

    requireSubFolder := "rjs",

    requireNativePath := None,

    buildRequire <<= buildRequireTask,

    buildRequireAndPackage <<= buildRequireAndPackageTask,

    resourceGenerators in Compile <+= LessCompiler,
    resourceGenerators in Compile <+= CoffeescriptCompiler,
    resourceGenerators in Compile <+= JavascriptCompiler(fullCompilerOptions = None),

    lessEntryPoints <<= (sourceDirectory in Compile)(base => ((base / "assets" ** "*.less") --- base / "assets" ** "_*")),
    coffeescriptEntryPoints <<= (sourceDirectory in Compile)(base => base / "assets" ** "*.coffee"),
    javascriptEntryPoints <<= (sourceDirectory in Compile)(base => ((base / "assets" ** "*.js") --- (base / "assets" ** "_*"))),

    lessOptions := Seq.empty[String],
    coffeescriptOptions := Seq.empty[String],
    closureCompilerOptions := Seq.empty[String],

    incrementalAssetsCompilation := false,

    // Templates

    templatesImport := Seq("play.api.templates._", "play.api.templates.PlayMagic._"),

    templatesTypes := {
      case "html" => ("play.api.templates.Html", "play.api.templates.HtmlFormat")
      case "txt" => ("play.api.templates.Txt", "play.api.templates.TxtFormat")
      case "xml" => ("play.api.templates.Xml", "play.api.templates.XmlFormat")
    })

}
