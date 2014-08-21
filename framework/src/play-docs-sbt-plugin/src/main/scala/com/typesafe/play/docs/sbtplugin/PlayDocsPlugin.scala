/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.play.docs.sbtplugin

import java.util.concurrent.Callable
import java.util.jar.JarFile

import com.typesafe.play.docs.sbtplugin.PlayDocsValidation.{ CodeSamplesReport, MarkdownRefReport }
import play.core.BuildDocHandler
import play.core.server.ServerWithStop
import play.sbtplugin.Colors
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object Imports {
  object PlayDocsKeys {
    val fallbackToJar = SettingKey[Boolean]("play-docs-fallback-to-jar", "Whether the docs should fallback to loading things from the jar", KeyRanks.CSetting)
    val manualPath = SettingKey[File]("play-docs-manual-path", "The location of the manual", KeyRanks.CSetting)
    val docsVersion = SettingKey[String]("play-docs-version", "The version of the documentation to fallback to.", KeyRanks.ASetting)
    val docsJarFile = TaskKey[File]("play-docs-jar-file", "The play docs jar file", KeyRanks.CTask)
    val docsJarScalaBinaryVersion = SettingKey[String]("play-docs-scala-version", "The binary scala version of the documentation", KeyRanks.BSetting)
    val validateDocs = TaskKey[Unit]("validate-docs", "Validates the play docs to ensure they compile and that all links resolve.", KeyRanks.APlusTask)
    val validateExternalLinks = TaskKey[Seq[String]]("validate-external-links", "Validates that all the external links are valid, by checking that they return 200.", KeyRanks.APlusTask)

    val generateMarkdownRefReport = TaskKey[MarkdownRefReport]("generate-markdown-ref-report", "Parses all markdown files and generates a report of references", KeyRanks.CTask)
    val generateMarkdownCodeSamplesReport = TaskKey[CodeSamplesReport]("generate-markdown-code-samples-report", "Parses all markdown files and generates a report of code samples used", KeyRanks.CTask)
    val generateUpstreamCodeSamplesReport = TaskKey[CodeSamplesReport]("generate-upstream-code-samples-report", "Parses all markdown files from the upstream translation and generates a report of code samples used", KeyRanks.CTask)
    val translationCodeSamplesReportFile = SettingKey[File]("translation-code-samples-report-filename", "The filename of the translation code samples report", KeyRanks.CTask)
    val translationCodeSamplesReport = TaskKey[File]("translation-code-samples-report", "Generates a report on the translation code samples", KeyRanks.CTask)
    val cachedTranslationCodeSamplesReport = TaskKey[File]("cached-translation-code-samples-report", "Generates a report on the translation code samples if not already generated", KeyRanks.CTask)
  }
}

object PlayDocsPlugin extends AutoPlugin {

  import Imports.PlayDocsKeys._

  val autoImport = Imports

  override def trigger = NoTrigger

  override def requires = JvmPlugin

  override def projectSettings = Seq(
    fallbackToJar := true,
    manualPath := baseDirectory.value,
    run <<= docsRunSetting,
    generateMarkdownRefReport <<= PlayDocsValidation.generateMarkdownRefReportTask,
    validateDocs <<= PlayDocsValidation.validateDocsTask,
    validateExternalLinks <<= PlayDocsValidation.validateExternalLinksTask,
    docsVersion := play.core.PlayVersion.current,
    docsJarFile <<= docsJarFileSetting,
    docsJarScalaBinaryVersion <<= scalaBinaryVersion,
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-docs" % play.core.PlayVersion.current,
      "com.typesafe.play" % s"play-docs_${docsJarScalaBinaryVersion.value}" % docsVersion.value % "docs" notTransitive ()
    ),

    generateMarkdownCodeSamplesReport <<= PlayDocsValidation.generateMarkdownCodeSamplesTask,
    generateUpstreamCodeSamplesReport <<= PlayDocsValidation.generateUpstreamCodeSamplesTask,
    translationCodeSamplesReportFile := target.value / "report.html",
    translationCodeSamplesReport <<= PlayDocsValidation.translationCodeSamplesReportTask,
    cachedTranslationCodeSamplesReport <<= PlayDocsValidation.cachedTranslationCodeSamplesReportTask
  )

  val docsJarFileSetting: Def.Initialize[Task[File]] = Def.task {
    val jars = update.value.matching(configurationFilter("docs") && artifactFilter(`type` = "jar")).toList
    jars match {
      case Nil => throw new RuntimeException("No docs jar was resolved")
      case jar :: Nil => jar
      case multiple => throw new RuntimeException("Multiple docs jars were resolved: " + multiple)
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
        if (play.core.classloader.DelegatingClassLoader.isSharedClass(name)) {
          sbtLoader.loadClass(name)
        } else {
          super.loadClass(name)
        }
      }
    }

    val docsJarFile = new JarFile(docsJarFileSetting.value)

    val buildDocHandler = {
      val docHandlerFactoryClass = classloader.loadClass("play.docs.BuildDocHandlerFactory")
      val fromDirectoryMethod = docHandlerFactoryClass.getMethod("fromDirectoryAndJar", classOf[java.io.File], classOf[JarFile], classOf[String], classOf[Boolean])
      fromDirectoryMethod.invoke(null, manualPath.value, docsJarFile, "play/docs/content", fallbackToJar.value: java.lang.Boolean)
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
    docsJarFile.close()
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

}