/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.docs.sbtplugin

import java.io.Closeable
import java.util.concurrent.Callable

import com.typesafe.play.docs.sbtplugin.PlayDocsValidation.{ ValidationConfig, CodeSamplesReport, MarkdownRefReport }
import play.core.BuildDocHandler
import play.core.PlayVersion
import play.core.server.ReloadableServer
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
    val manualPath = SettingKey[File]("playDocsManualPath", "The location of the manual", KeyRanks.CSetting)
    val docsVersion = SettingKey[String]("playDocsVersion", "The version of the documentation to fallback to.", KeyRanks.ASetting)
    val docsName = SettingKey[String]("playDocsName", "The name of the documentation artifact", KeyRanks.BSetting)
    val docsJarFile = TaskKey[Option[File]]("playDocsJarFile", "Optional play docs jar file", KeyRanks.CTask)
    val resources = TaskKey[Seq[PlayDocsResource]]("playDocsResources", "Resource files to add to the file repository for running docs and validation", KeyRanks.CTask)
    val docsJarScalaBinaryVersion = SettingKey[String]("playDocsScalaVersion", "The binary scala version of the documentation", KeyRanks.BSetting)
    val validateDocs = TaskKey[Unit]("validateDocs", "Validates the play docs to ensure they compile and that all links resolve.", KeyRanks.APlusTask)
    val validateExternalLinks = TaskKey[Seq[String]]("validateExternalLinks", "Validates that all the external links are valid, by checking that they return 200.", KeyRanks.APlusTask)

    val generateMarkdownRefReport = TaskKey[MarkdownRefReport]("generateMarkdownRefReport", "Parses all markdown files and generates a report of references", KeyRanks.CTask)
    val generateMarkdownCodeSamplesReport = TaskKey[CodeSamplesReport]("generateMarkdownCodeSamplesReport", "Parses all markdown files and generates a report of code samples used", KeyRanks.CTask)
    val generateUpstreamCodeSamplesReport = TaskKey[CodeSamplesReport]("generateUpstreamCodeSamplesReport", "Parses all markdown files from the upstream translation and generates a report of code samples used", KeyRanks.CTask)
    val translationCodeSamplesReportFile = SettingKey[File]("translationCodeSamplesReportFilename", "The filename of the translation code samples report", KeyRanks.CTask)
    val translationCodeSamplesReport = TaskKey[File]("translationCodeSamplesReport", "Generates a report on the translation code samples", KeyRanks.CTask)
    val cachedTranslationCodeSamplesReport = TaskKey[File]("cached-translation-code-samples-report", "Generates a report on the translation code samples if not already generated", KeyRanks.CTask)
    val playDocsValidationConfig = settingKey[ValidationConfig]("Configuration for docs validation")

    val javaManualSourceDirectories = SettingKey[Seq[File]]("javaManualSourceDirectories")
    val scalaManualSourceDirectories = SettingKey[Seq[File]]("scalaManualSourceDirectories")
    val commonManualSourceDirectories = SettingKey[Seq[File]]("commonManualSourceDirectories")
    val migrationManualSources = SettingKey[Seq[File]]("migrationManualSources")
    val javaTwirlSourceManaged = SettingKey[File]("javaRoutesSourceManaged")
    val scalaTwirlSourceManaged = SettingKey[File]("scalaRoutesSourceManaged")

    val evaluateSbtFiles = TaskKey[Unit]("evaluateSbtFiles", "Evaluate all the sbt files in the project")
  }

  sealed trait PlayDocsResource {
    def file: File
  }
  case class PlayDocsDirectoryResource(file: File) extends PlayDocsResource
  case class PlayDocsJarFileResource(file: File, base: Option[String]) extends PlayDocsResource

}

/**
 * This plugin is used by all Play modules that themselves have compiled and tested markdown documentation, for example,
 * anorm, play-ebean, scalatestplus-play, etc. It's also used by translators translating the Play docs.  And of course,
 * it's used by the main Play documentation.
 *
 * Any changes to this plugin need to be made in consideration of the downstream projects that depend on it.
 */
object PlayDocsPlugin extends AutoPlugin with PlayDocsPluginCompat {

  import Imports._
  import Imports.PlayDocsKeys._

  val autoImport = Imports

  override def trigger = NoTrigger

  override def requires = RoutesCompiler

  override def projectSettings = docsRunSettings ++ docsReportSettings ++ docsTestSettings

  def docsRunSettings = Seq(
    playDocsValidationConfig := ValidationConfig(),
    manualPath := baseDirectory.value,
    run := docsRunSetting.evaluated,
    generateMarkdownRefReport := PlayDocsValidation.generateMarkdownRefReportTask.value,
    validateDocs := PlayDocsValidation.validateDocsTask.value,
    validateExternalLinks := PlayDocsValidation.validateExternalLinksTask.value,
    docsVersion := PlayVersion.current,
    docsName := "play-docs",
    docsJarFile := docsJarFileSetting.value,
    PlayDocsKeys.resources := Seq(PlayDocsDirectoryResource(manualPath.value)) ++
      docsJarFile.value.map(jar => PlayDocsJarFileResource(jar, Some("play/docs/content"))).toSeq,
    docsJarScalaBinaryVersion := scalaBinaryVersion.value,
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% docsName.value % PlayVersion.current,
      "com.typesafe.play" % s"${docsName.value}_${docsJarScalaBinaryVersion.value}" % docsVersion.value % "docs" notTransitive ()
    )
  )

  def docsReportSettings = Seq(
    generateMarkdownCodeSamplesReport := PlayDocsValidation.generateMarkdownCodeSamplesTask.value,
    generateUpstreamCodeSamplesReport := PlayDocsValidation.generateUpstreamCodeSamplesTask.value,
    translationCodeSamplesReportFile := target.value / "report.html",
    translationCodeSamplesReport := PlayDocsValidation.translationCodeSamplesReportTask.value,
    cachedTranslationCodeSamplesReport := PlayDocsValidation.cachedTranslationCodeSamplesReportTask.value
  )

  def docsTestSettings = Seq(
    migrationManualSources := Nil,
    javaManualSourceDirectories := Nil,
    scalaManualSourceDirectories := Nil,
    commonManualSourceDirectories := Nil,
    unmanagedSourceDirectories in Test ++= javaManualSourceDirectories.value ++ scalaManualSourceDirectories.value ++
      commonManualSourceDirectories.value ++ migrationManualSources.value,
    unmanagedResourceDirectories in Test ++= javaManualSourceDirectories.value ++ scalaManualSourceDirectories.value ++
      commonManualSourceDirectories.value ++ migrationManualSources.value,

    javaTwirlSourceManaged := target.value / "twirl" / "java",
    scalaTwirlSourceManaged := target.value / "twirl" / "scala",
    managedSourceDirectories in Test ++= Seq(
      javaTwirlSourceManaged.value,
      scalaTwirlSourceManaged.value
    ),

    // Need to ensure that templates in the Java docs get Java imports, and in the Scala docs get Scala imports
    sourceGenerators in Test += Def.task {
      compileTemplates(javaManualSourceDirectories.value, javaTwirlSourceManaged.value, TemplateImports.defaultJavaTemplateImports.asScala, streams.value.log)
    }.taskValue,

    sourceGenerators in Test += Def.task {
      compileTemplates(scalaManualSourceDirectories.value, scalaTwirlSourceManaged.value, TemplateImports.defaultScalaTemplateImports.asScala, streams.value.log)
    }.taskValue,

    routesCompilerTasks in Test := {
      val javaRoutes = (javaManualSourceDirectories.value * "*.routes").get
      val scalaRoutes = (scalaManualSourceDirectories.value * "*.routes").get
      val commonRoutes = (commonManualSourceDirectories.value * "*.routes").get
      (javaRoutes.map(_ -> Seq("play.libs.F")) ++ scalaRoutes.map(_ -> Nil) ++ commonRoutes.map(_ -> Nil)).map {
        case (file, imports) => RoutesCompilerTask(file, imports, true, true, true)
      }
    },

    routesGenerator := InjectedRoutesGenerator,

    evaluateSbtFiles := {
      val unit = loadedBuild.value.units(thisProjectRef.value.build)
      val (eval, structure) = defaultLoad(state.value, unit.localBase)
      val sbtFiles = ((unmanagedSourceDirectories in Test).value * "*.sbt").get
      val log = state.value.log
      if (sbtFiles.nonEmpty) {
        log.info("Testing .sbt files...")
      }

      val baseDir = baseDirectory.value
      val result = sbtFiles.map { sbtFile =>
        val relativeFile = sbt.Path.relativeTo(baseDir)(sbtFile).getOrElse(sbtFile.getAbsolutePath)
        try {
          evaluateConfigurations(sbtFile, unit.imports, unit.loader, eval)
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

    val allResources = PlayDocsKeys.resources.value

    val docHandlerFactoryClass = classloader.loadClass("play.docs.BuildDocHandlerFactory")
    val fromResourcesMethod = docHandlerFactoryClass.getMethod("fromResources", classOf[Array[java.io.File]], classOf[Array[String]])

    val files = allResources.map(_.file).toArray[File]
    val baseDirs = allResources.map {
      case PlayDocsJarFileResource(_, base) => base.orNull
      case PlayDocsDirectoryResource(_) => null
    }.toArray[String]

    val buildDocHandler = fromResourcesMethod.invoke(null, files, baseDirs)

    val clazz = classloader.loadClass("play.docs.DocServerStart")
    val constructor = clazz.getConstructor()
    val startMethod = clazz.getMethod("start", classOf[File], classOf[BuildDocHandler], classOf[Callable[_]],
      classOf[Callable[_]], classOf[java.lang.Integer])

    val translationReport = new Callable[File] {
      def call() = Project.runTask(cachedTranslationCodeSamplesReport, state.value).get._2.toEither.right.get
    }
    val forceTranslationReport = new Callable[File] {
      def call() = Project.runTask(translationCodeSamplesReport, state.value).get._2.toEither.right.get
    }
    val docServerStart = constructor.newInstance()
    val server: ReloadableServer = startMethod.invoke(docServerStart, manualPath.value, buildDocHandler, translationReport, forceTranslationReport,
      java.lang.Integer.valueOf(port)).asInstanceOf[ReloadableServer]

    println()
    println(Colors.green("Documentation server started, you can now view the docs by going to http://" + server.mainAddress()))
    println()

    waitForKey()

    server.stop()
    buildDocHandler.asInstanceOf[Closeable].close()
  }

  private lazy val consoleReader = {
    val cr = new jline.console.ConsoleReader
    // Because jline, whenever you create a new console reader, turns echo off. Stupid thing.
    cr.getTerminal.setEchoEnabled(true)
    cr
  }

  private def waitForKey() = {
    consoleReader.getTerminal.setEchoEnabled(false)
    def waitEOF(): Unit = {
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
    play.twirl.sbt.TemplateCompiler.compile(
      sourceDirectories = sourceDirectories,
      targetDirectory = target,
      templateFormats = templateFormats,
      templateImports = imports,
      constructorAnnotations = Nil,
      includeFilter = templateFilter,
      excludeFilter = HiddenFileFilter,
      codec = templateCodec,
      log = log
    )
  }
}
