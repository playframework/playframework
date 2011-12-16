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

      "play.api.i18n.Messages",

      "play.mvc._",
      "play.data._",
      "com.avaje.ebean._",

      "play.mvc.Http.Context.Implicit._",

      "views.%format%._"))

  lazy val defaultScalaSettings = Seq[Setting[_]](

    templatesImport ++= Seq(
      "models._",
      "controllers._",

      "play.api.i18n.Messages",

      "play.api.mvc._",
      "play.api.data._",

      "views.%format%._"))

  lazy val defaultSettings = Seq[Setting[_]](

    resolvers ++= Seq(
      Resolver.url("Play Repository", url("http://download.playframework.org/ivy-releases/"))(Resolver.ivyStylePatterns),
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"),

    target <<= baseDirectory / "target",

    sourceDirectory in Compile <<= baseDirectory / "app",
    sourceDirectory in Test <<= baseDirectory / "test",

    confDirectory <<= baseDirectory / "conf",

    scalaSource in Compile <<= baseDirectory / "app",
    scalaSource in Test <<= baseDirectory / "test",

    javaSource in Compile <<= baseDirectory / "app",
    javaSource in Test <<= baseDirectory / "test",

    distDirectory <<= baseDirectory / "dist",

    libraryDependencies += "play" %% "play" % play.core.PlayVersion.current,

    libraryDependencies ++= Seq("org.specs2" %% "specs2" % "1.6.1" % "test",
      "com.novocode" % "junit-interface" % "0.7" % "test",
      "fr.javafreelance.fluentlenium" % "fluentlenium" % "0.5.3" % "test"),

    sourceGenerators in Compile <+= (confDirectory, sourceManaged in Compile) map RouteFiles,

    sourceGenerators in Compile <+= (sourceDirectory in Compile, sourceManaged in Compile, templatesTypes, templatesImport) map ScalaTemplates,

    commands ++= Seq(playCommand, playRunCommand, playStartCommand, playHelpCommand, h2Command, classpathCommand, licenseCommand, computeDependenciesCommand),

    shellPrompt := playPrompt,

    copyResources in Compile <<= (copyResources in Compile, playCopyResources) map { (r, pr) => r ++ pr },

    mainClass in (Compile, run) := Some(classOf[play.core.server.NettyServer].getName),

    compile in (Compile) <<= PostCompile,

    dist <<= distTask,

    testResultReporter <<= testResultReporterTask,

    testResultReporterReset <<= testResultReporterResetTask,

    computeDependencies <<= computeDependenciesTask,

    playCommonClassloader <<= playCommonClassloaderTask,

    playCopyResources <<= playCopyResourcesTask,

    playCompileEverything <<= playCompileEverythingTask,

    playPackageEverything <<= playPackageEverythingTask,

    playReload <<= playReloadTask,

    playStage <<= playStageTask,

    cleanFiles <+= distDirectory,

    resourceGenerators in Compile <+= LessCompiler,

    resourceGenerators in Compile <+= CoffeescriptCompiler,

    resourceGenerators in Compile <+= JavascriptCompiler,

    minify := false,

    playResourceDirectories := Seq.empty[File],

    playResourceDirectories <+= baseDirectory / "conf",

    playResourceDirectories <+= baseDirectory / "public",

    playResourceDirectories <+= baseDirectory / "META-INF",

    templatesImport := Seq("play.api.templates._", "play.api.templates.PlayMagic._"),

    templatesTypes := {
      case "html" => ("play.api.templates.Html", "play.api.templates.HtmlFormat")
      case "txt" => ("play.api.templates.Txt", "play.api.templates.TxtFormat")
      case "xml" => ("play.api.templates.Xml", "play.api.templates.XmlFormat")
    })

}
