/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.play.docs.sbtplugin

import java.util.jar.JarFile

import com.typesafe.play.docs.sbtplugin.PlayDocsValidation.MarkdownReport
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
    val validateDocs = TaskKey[Unit]("validate-docs", "Validates the play docs to ensure they compile and that all links resolve.", KeyRanks.APlusTask)
    val generateMarkdownReport = TaskKey[MarkdownReport]("generate-markdown-report", "Parses all markdown files and generates a report", KeyRanks.CTask)
    val validateExternalLinks = TaskKey[Seq[String]]("validate-external-links", "Validates that all the external links are valid, by checking that they return 200.", KeyRanks.APlusTask)
  }
}

object PlayDocsPlugin extends AutoPlugin {

  import Imports.PlayDocsKeys._

  def autoImport = Imports

  override def trigger = NoTrigger

  override def requires = JvmPlugin

  override def projectSettings = Seq(
    fallbackToJar := true,
    manualPath := baseDirectory.value,
    run <<= docsRunSetting,
    generateMarkdownReport <<= PlayDocsValidation.generateMarkdownReportTask,
    validateDocs <<= PlayDocsValidation.validateDocsTask,
    validateExternalLinks <<= PlayDocsValidation.validateExternalLinksTask,
    libraryDependencies += "com.typesafe.play" %% "play-docs" % play.core.PlayVersion.current
  )

  // Run a documentation server
  val docsRunSetting: Def.Initialize[InputTask[Unit]] = Def.inputTask {
    val args = Def.spaceDelimited().parsed
    val port = args.headOption.map(_.toInt).getOrElse(9000)

    val classpath: Seq[Attributed[File]] = (dependencyClasspath in Test).value

    // Get classloader
    val sbtLoader = this.getClass.getClassLoader
    val classloader = new java.net.URLClassLoader(classpath.map(_.data.toURI.toURL).toArray, null /* important here, don't depend of the sbt classLoader! */) {
      override def loadClass(name: String): Class[_] = {
        if (play.core.classloader.DelegatingClassLoader.isSharedClass(name)) {
          sbtLoader.loadClass(name)
        } else {
          super.loadClass(name)
        }
      }
    }

    val docsJarFile = {
      val f = classpath.map(_.data).filter(_.getName.startsWith("play-docs")).head
      new JarFile(f)
    }
    val buildDocHandler = {
      val docHandlerFactoryClass = classloader.loadClass("play.docs.BuildDocHandlerFactory")
      val fromDirectoryMethod = docHandlerFactoryClass.getMethod("fromDirectoryAndJar", classOf[java.io.File], classOf[JarFile], classOf[String], classOf[Boolean])
      fromDirectoryMethod.invoke(null, manualPath.value, docsJarFile, "play/docs/content", fallbackToJar.value: java.lang.Boolean)
    }

    val clazz = classloader.loadClass("play.docs.DocumentationServer")
    val constructor = clazz.getConstructor(classOf[File], classOf[BuildDocHandler], classOf[java.lang.Integer])
    val server = constructor.newInstance(manualPath.value, buildDocHandler, new java.lang.Integer(port)).asInstanceOf[ServerWithStop]

    println()
    println(Colors.green("Documentation server started, you can now view the docs by going to http://" + server.mainAddress()))
    println()

    waitForKey()

    server.stop()
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
        case 11 => consoleReader.clearScreen(); waitEOF()
        case 10 => println(); waitEOF()
        case _ => waitEOF()
      }

    }
    waitEOF()
    consoleReader.getTerminal.setEchoEnabled(true)
  }

}