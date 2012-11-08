# About SBT Settings

## About sbt settings

The sbt build script defines settings for your project. You can also define your own custom settings for your project, as described in the [[sbt documentation | https://github.com/harrah/xsbt/wiki]].  In particular, it helps to be familiar with the [[settings | https://github.com/harrah/xsbt/wiki/Getting-Started-More-About-Settings]] in sbt.

To set a basic setting, use the `:=` operator:

```scala
val main = PlayProject(appName, appVersion, appDependencies).settings(
  confDirectory := "myConfFolder"     
)
```

## Default settings for Java applications

Play defines a default set of settings suitable for Java-based applications. To enable them add the `defaultJavaSettings` set of settings to your application definition:

```scala
val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA)
```

These default settings mostly define the default imports for generated templates. For example, it imports `java.lang.*`, so types like `Long` are the Java ones by default instead of the Scala ones. It also imports `java.util.*` so the default collection library will be the Java one.

## Default settings for Scala applications

Play defines a default set of settings suitable for Scala-based applications. To enable them add the `defaultScalaSettings` set of settings to your application definition:

```scala
val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA)
```

These default settings define the default imports for generated templates (such as internationalized messages, and core APIs).

## Play project settings with their default value

When you define your sbt project using `PlayProject` instead of `Project`, you will get a default set of settings. Here is the default configuration:

```scala
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

    testOptions in Test += Tests.Setup { loader =>
      loader.loadClass("play.api.Logger").getMethod("init", classOf[java.io.File]).invoke(null, new java.io.File("."))
    },

    testOptions in Test += Tests.Cleanup { loader =>
      loader.loadClass("play.api.Logger").getMethod("shutdown").invoke(null)
    },

    testOptions in Test += Tests.Argument("sequential", "true"),

    testOptions in Test += Tests.Argument("junitxml", "console"),

    testListeners <<= (target, streams).map((t, s) => Seq(new eu.henkelmann.sbt.JUnitXmlTestsListener(t.getAbsolutePath, s.log))),

    testResultReporter <<= testResultReporterTask,

    testResultReporterReset <<= testResultReporterResetTask,

    sourceGenerators in Compile <+= (confDirectory, sourceManaged in Compile, routesImport) map RouteFiles,

    // Adds config/routes to continious triggers
    watchSources <+= confDirectory map { _ / "routes" },

    sourceGenerators in Compile <+= (sourceDirectory in Compile, sourceManaged in Compile, templatesTypes, templatesImport) map ScalaTemplates,

    // Adds views template to continious triggers
    watchSources <++= baseDirectory map { path => ((path / "app") ** "*.scala.*").get },

    commands ++= Seq(shCommand, playCommand, playRunCommand, playStartCommand, h2Command, classpathCommand, licenseCommand, computeDependenciesCommand),

    shellPrompt := playPrompt,

    copyResources in Compile <<= (copyResources in Compile, playCopyAssets) map { (r, pr) => r ++ pr },

    mainClass in (Compile, run) := Some(classOf[play.core.server.NettyServer].getName),

    compile in (Compile) <<= PostCompile,

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

    playIntellij <<= playIntellijTask,

    playHash <<= playHashTask,

    // Assets

    playAssetsDirectories := Seq.empty[File],

    playAssetsDirectories <+= baseDirectory / "public",

    resourceGenerators in Compile <+= LessCompiler,
    resourceGenerators in Compile <+= CoffeescriptCompiler,
    resourceGenerators in Compile <+= JavascriptCompiler,

    lessEntryPoints <<= (sourceDirectory in Compile)(base => ((base / "assets" ** "*.less") --- base / "assets" ** "_*")),
    coffeescriptEntryPoints <<= (sourceDirectory in Compile)(base => base / "assets" ** "*.coffee"),
    javascriptEntryPoints <<= (sourceDirectory in Compile)(base => ((base / "assets" ** "*.js") --- (base / "assets" ** "_*"))),

    lessOptions := Seq.empty[String],
    coffeescriptOptions := Seq.empty[String],
    closureCompilerOptions := Seq.empty[String],

    // Templates

    templatesImport := Seq("play.api.templates._", "play.api.templates.PlayMagic._"),

    templatesTypes := {
      case "html" => ("play.api.templates.Html", "play.api.templates.HtmlFormat")
      case "txt" => ("play.api.templates.Txt", "play.api.templates.TxtFormat")
      case "xml" => ("play.api.templates.Xml", "play.api.templates.XmlFormat")
    })

```

> **Next:** [[Managing library dependencies | SBTDependencies]]