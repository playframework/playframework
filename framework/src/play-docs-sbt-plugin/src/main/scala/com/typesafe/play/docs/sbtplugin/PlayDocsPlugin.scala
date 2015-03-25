/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.play.docs.sbtplugin

import java.util.concurrent.Callable
import java.util.jar.JarFile

import com.typesafe.play.docs.sbtplugin.PlayDocsValidation.{ CodeSamplesReport, MarkdownRefReport }
import play.core.BuildDocHandler
import play.core.server.ServerWithStop
import play.routes.compiler.RoutesCompiler.RoutesCompilerTask
import play.TemplateImports
import play.sbt.Colors
import play.sbt.routes.RoutesCompiler
import play.sbt.routes.RoutesKeys._
import sbt._
import sbt.Keys._
import scala.collection.JavaConverters._
import scala.util.control.NonFatal

object Imports {
  object PlayDocsKeys {
    val fallbackToJar = SettingKey[Boolean]("play-docs-fallback-to-jar", "Whether the docs should fallback to loading things from the jar", KeyRanks.CSetting)
    val manualPath = SettingKey[File]("play-docs-manual-path", "The location of the manual", KeyRanks.CSetting)
    val docsVersion = SettingKey[String]("play-docs-version", "The version of the documentation to fallback to.", KeyRanks.ASetting)
    val docsName = SettingKey[String]("play-docs-name", "The name of the documentation artifact", KeyRanks.BSetting)
    val docsJarFile = TaskKey[Option[File]]("play-docs-jar-file", "Optional play docs jar file", KeyRanks.CTask)
    val docsJarScalaBinaryVersion = SettingKey[String]("play-docs-scala-version", "The binary scala version of the documentation", KeyRanks.BSetting)
    val validateDocs = TaskKey[Unit]("validate-docs", "Validates the play docs to ensure they compile and that all links resolve.", KeyRanks.APlusTask)
    val validateExternalLinks = TaskKey[Seq[String]]("validate-external-links", "Validates that all the external links are valid, by checking that they return 200.", KeyRanks.APlusTask)

    val generateMarkdownRefReport = TaskKey[MarkdownRefReport]("generate-markdown-ref-report", "Parses all markdown files and generates a report of references", KeyRanks.CTask)
    val generateMarkdownCodeSamplesReport = TaskKey[CodeSamplesReport]("generate-markdown-code-samples-report", "Parses all markdown files and generates a report of code samples used", KeyRanks.CTask)
    val generateUpstreamCodeSamplesReport = TaskKey[CodeSamplesReport]("generate-upstream-code-samples-report", "Parses all markdown files from the upstream translation and generates a report of code samples used", KeyRanks.CTask)
    val translationCodeSamplesReportFile = SettingKey[File]("translation-code-samples-report-filename", "The filename of the translation code samples report", KeyRanks.CTask)
    val translationCodeSamplesReport = TaskKey[File]("translation-code-samples-report", "Generates a report on the translation code samples", KeyRanks.CTask)
    val cachedTranslationCodeSamplesReport = TaskKey[File]("cached-translation-code-samples-report", "Generates a report on the translation code samples if not already generated", KeyRanks.CTask)

    val javaManualSourceDirectories = SettingKey[Seq[File]]("java-manual-source-directories")
    val scalaManualSourceDirectories = SettingKey[Seq[File]]("scala-manual-source-directories")
    val javaTwirlSourceManaged = SettingKey[File]("java-routes-source-managed")
    val scalaTwirlSourceManaged = SettingKey[File]("scala-routes-source-managed")

    val evaluateSbtFiles = TaskKey[Unit]("evaluateSbtFiles", "Evaluate all the sbt files in the project")
  }
}

object PlayDocsPlugin extends AutoPlugin {

  import Imports.PlayDocsKeys._

  val autoImport = Imports

  override def trigger = NoTrigger

  override def requires = RoutesCompiler

  override def projectSettings = docsRunSettings ++ docsReportSettings ++ docsTestSettings

  def docsRunSettings = Seq(
    fallbackToJar := true,
    manualPath := baseDirectory.value,
    run <<= docsRunSetting,
    generateMarkdownRefReport <<= PlayDocsValidation.generateMarkdownRefReportTask,
    validateDocs <<= PlayDocsValidation.validateDocsTask,
    validateExternalLinks <<= PlayDocsValidation.validateExternalLinksTask,
    docsVersion := play.core.PlayVersion.current,
    docsName := "play-docs",
    docsJarFile <<= docsJarFileSetting,
    docsJarScalaBinaryVersion <<= scalaBinaryVersion,
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% docsName.value % play.core.PlayVersion.current,
      "com.typesafe.play" % s"${docsName.value}_${docsJarScalaBinaryVersion.value}" % docsVersion.value % "docs" notTransitive ()
    )
  )

  def docsReportSettings = Seq(
    generateMarkdownCodeSamplesReport <<= PlayDocsValidation.generateMarkdownCodeSamplesTask,
    generateUpstreamCodeSamplesReport <<= PlayDocsValidation.generateUpstreamCodeSamplesTask,
    translationCodeSamplesReportFile := target.value / "report.html",
    translationCodeSamplesReport <<= PlayDocsValidation.translationCodeSamplesReportTask,
    cachedTranslationCodeSamplesReport <<= PlayDocsValidation.cachedTranslationCodeSamplesReportTask
  )

  def docsTestSettings = Seq(
    javaManualSourceDirectories := Seq.empty,
    scalaManualSourceDirectories := Seq.empty,
    unmanagedSourceDirectories in Test ++= javaManualSourceDirectories.value ++ scalaManualSourceDirectories.value,
    unmanagedResourceDirectories in Test ++= javaManualSourceDirectories.value ++ scalaManualSourceDirectories.value,

    javaTwirlSourceManaged := target.value / "twirl" / "java",
    scalaTwirlSourceManaged := target.value / "twirl" / "scala",
    managedSourceDirectories in Test ++= Seq(
      javaTwirlSourceManaged.value,
      scalaTwirlSourceManaged.value
    ),

    // Need to ensure that templates in the Java docs get Java imports, and in the Scala docs get Scala imports
    sourceGenerators in Test <+= (javaManualSourceDirectories, javaTwirlSourceManaged, streams) map { (from, to, s) =>
      compileTemplates(from, to, TemplateImports.defaultJavaTemplateImports.asScala, s.log)
    },

    sourceGenerators in Test <+= (scalaManualSourceDirectories, scalaTwirlSourceManaged, streams) map { (from, to, s) =>
      compileTemplates(from, to, TemplateImports.defaultScalaTemplateImports.asScala, s.log)
    },

    routesCompilerTasks in Test := {
      val javaRoutes = (javaManualSourceDirectories.value * "*.routes").get
      val scalaRoutes = (scalaManualSourceDirectories.value * "*.routes").get
      (javaRoutes.map(_ -> Seq("play.libs.F")) ++ scalaRoutes.map(_ -> Nil)).map {
        case (file, imports) => RoutesCompilerTask(file, imports, true, true, true)
      }
    },

    routesGenerator := InjectedRoutesGenerator,

    evaluateSbtFiles := {
      val unit = loadedBuild.value.units(thisProjectRef.value.build)
      val (eval, structure) = Load.defaultLoad(state.value, unit.localBase, state.value.log)
      val sbtFiles = ((unmanagedSourceDirectories in Test).value * "*.sbt").get
      val log = state.value.log
      if (sbtFiles.nonEmpty) {
        log.info("Testing .sbt files...")
      }
      val result = sbtFiles.map { sbtFile =>
        val relativeFile = relativeTo(baseDirectory.value)(sbtFile).getOrElse(sbtFile.getAbsolutePath)
        try {
          EvaluateConfigurations.evaluateConfiguration(eval(), sbtFile, unit.imports)(unit.loader)
          log.info(s"  ${Colors.green("+")} $relativeFile")
          true
        } catch {
          case NonFatal(_) =>
            log.error(s" ${Colors.yellow("x")} $relativeFile")
            false
        }
      }
      if (result.contains(false)) {
        throw new TestsFailedException
      }
    },

    parallelExecution in Test := false,
    (compile in Test) <<= Enhancement.enhanceJavaClasses,
    javacOptions in Test ++= Seq("-g", "-Xlint:deprecation"),
    testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "sequential", "true", "junitxml", "console"),
    testOptions in Test += Tests.Argument(TestFrameworks.JUnit, "-v", "--ignore-runners=org.specs2.runner.JUnitRunner")
  )

  val docsJarFileSetting: Def.Initialize[Task[Option[File]]] = Def.task {
    val jars = update.value.matching(configurationFilter("docs") && artifactFilter(`type` = "jar")).toList
    jars match {
      case Nil =>
        streams.value.log.error("No docs jar was resolved")
        None
      case jar :: Nil =>
        Option(jar)
      case multiple =>
        streams.value.log.error("Multiple docs jars were resolved: " + multiple)
        multiple.headOption
    }
  }

  // Run a documentation server
  val docsRunSetting: Def.Initialize[InputTask[Unit]] = Def.inputTask {
    val args = Def.spaceDelimited().parsed
    val port = args.headOption.map(_.toInt).getOrElse(9000)

    val classpath: Seq[Attributed[File]] = (dependencyClasspath in Test).value

    // Get classloader
    val sbtLoader = this.getClass.getClassLoader
    val classloader = new java.net.URLClassLoader(classpath.map(_.data.toURI.toURL).toArray, null /* important here, don't depend of the sbt classLoader! */ ) {
      override def loadClass(name: String): Class[_] = {
        if (play.core.Build.sharedClasses.contains(name)) {
          sbtLoader.loadClass(name)
        } else {
          super.loadClass(name)
        }
      }
    }

    val maybeDocsJar = docsJarFile.value map { f => new JarFile(f) }

    val docHandlerFactoryClass = classloader.loadClass("play.docs.BuildDocHandlerFactory")
    val buildDocHandler = maybeDocsJar match {
      case Some(docsJar) =>
        val fromDirectoryAndJarMethod = docHandlerFactoryClass.getMethod("fromDirectoryAndJar", classOf[java.io.File], classOf[JarFile], classOf[String], classOf[Boolean])
        fromDirectoryAndJarMethod.invoke(null, manualPath.value, docsJar, "play/docs/content", fallbackToJar.value: java.lang.Boolean)
      case None =>
        val fromDirectoryMethod = docHandlerFactoryClass.getMethod("fromDirectory", classOf[java.io.File])
        fromDirectoryMethod.invoke(null, manualPath.value)
    }

    val clazz = classloader.loadClass("play.docs.DocumentationServer")
    val constructor = clazz.getConstructor(classOf[File], classOf[BuildDocHandler], classOf[Callable[_]],
      classOf[Callable[_]], classOf[java.lang.Integer])

    val translationReport = new Callable[File] {
      def call() = Project.runTask(cachedTranslationCodeSamplesReport, state.value).get._2.toEither.right.get
    }
    val forceTranslationReport = new Callable[File] {
      def call() = Project.runTask(translationCodeSamplesReport, state.value).get._2.toEither.right.get
    }
    val server = constructor.newInstance(manualPath.value, buildDocHandler, translationReport, forceTranslationReport,
      new java.lang.Integer(port)).asInstanceOf[ServerWithStop]

    println()
    println(Colors.green("Documentation server started, you can now view the docs by going to http://" + server.mainAddress()))
    println()

    waitForKey()

    server.stop()
    maybeDocsJar.foreach(_.close())
  }

  private lazy val consoleReader = {
    val cr = new jline.console.ConsoleReader
    // Because jline, whenever you create a new console reader, turns echo off. Stupid thing.
    cr.getTerminal.setEchoEnabled(true)
    cr
  }

  private def waitForKey() = {
    consoleReader.getTerminal.setEchoEnabled(false)
    def waitEOF() {
      consoleReader.readCharacter() match {
        case 4 => // STOP
        case 11 =>
          consoleReader.clearScreen(); waitEOF()
        case 10 =>
          println(); waitEOF()
        case _ => waitEOF()
      }

    }
    waitEOF()
    consoleReader.getTerminal.setEchoEnabled(true)
  }

  val templateFormats = Map("html" -> "play.twirl.api.HtmlFormat")
  val templateFilter = "*.scala.*"
  val templateCodec = scala.io.Codec("UTF-8")

  def compileTemplates(sourceDirectories: Seq[File], target: File, imports: Seq[String], log: Logger) = {
    play.twirl.sbt.TemplateCompiler.compile(sourceDirectories, target, templateFormats, imports, templateFilter, HiddenFileFilter, templateCodec, false, log)
  }
}
