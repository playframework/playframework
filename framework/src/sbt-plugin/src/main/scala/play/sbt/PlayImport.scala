/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.sbt

import sbt.Keys._
import sbt._

import play.runsupport.FileWatchService

/**
 * Declares the default imports for Play plugins.
 */
object PlayImport {

  val Production = config("production")

  def component(id: String) = "com.typesafe.play" %% id % play.core.PlayVersion.current

  def movedExternal(msg: String): ModuleID = {
    System.err.println(msg)
    class ComponentExternalisedException extends RuntimeException(msg) with FeedbackProvidedException
    throw new ComponentExternalisedException
  }

  val evolutions = component("play-jdbc-evolutions")

  val jdbc = component("play-jdbc")

  def anorm = movedExternal(
    """Anorm has been moved to an external module.
      |See https://playframework.com/documentation/2.4.x/Migration24 for details.""".stripMargin)

  val javaCore = component("play-java")

  val javaJdbc = component("play-java-jdbc")

  def javaEbean = movedExternal(
    """Play ebean module has been replaced with an external Play ebean plugin.
      |See https://playframework.com/documentation/2.4.x/Migration24 for details.""".stripMargin)

  val javaJpa = component("play-java-jpa")

  val filters = component("filters-helpers")

  val cache = component("play-cache")

  val json = component("play-json")

  val ws = component("play-ws")

  val javaWs = component("play-java-ws")

  val specs2 = component("play-specs2")

  /**
   * Add this to your build.sbt, eg:
   *
   * {{{
   *   emojiLogs
   * }}}
   */
  lazy val emojiLogs = logManager ~= { lm =>
    new LogManager {
      def apply(data: Settings[Scope], state: State, task: Def.ScopedKey[_], writer: java.io.PrintWriter) = {
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

  object PlayKeys {
    val playDefaultPort = SettingKey[Int]("playDefaultPort", "The default port that Play runs on")
    val playDefaultAddress = SettingKey[String]("playDefaultAddress", "The default address that Play runs on")

    /** Our means of hooking the run task with additional behavior. */
    val playRunHooks = TaskKey[Seq[PlayRunHook]]("playRunHooks", "Hooks to run additional behaviour before/after the run task")

    /** A hook to configure how play blocks on user input while running. */
    val playInteractionMode = SettingKey[PlayInteractionMode]("playInteractionMode", "Hook to configure how Play blocks when running")

    val externalizeResources = SettingKey[Boolean]("playExternalizeResources", "Whether resources should be externalized into the conf directory when Play is packaged as a distribution.")
    val playExternalizedResources = TaskKey[Seq[(File, String)]]("playExternalizedResources", "The resources to externalize")
    val playJarSansExternalized = TaskKey[File]("playJarSansExternalized", "Creates a jar file that has all the externalized resources excluded")

    val playOmnidoc = SettingKey[Boolean]("playOmnidoc", "Determines whether to use the aggregated Play documentation")
    val playDocsName = SettingKey[String]("playDocsName", "Artifact name of the Play documentation")
    val playDocsModule = SettingKey[Option[ModuleID]]("playDocsModule", "Optional Play documentation dependency")
    val playDocsJar = TaskKey[Option[File]]("playDocsJar", "Optional jar file containing the Play documentation")

    val playPlugin = SettingKey[Boolean]("playPlugin")

    val devSettings = SettingKey[Seq[(String, String)]]("playDevSettings")

    val generateSecret = TaskKey[String]("playGenerateSecret", "Generate a new application secret", KeyRanks.BTask)
    val updateSecret = TaskKey[File]("playUpdateSecret", "Update the application conf to generate an application secret", KeyRanks.BTask)

    val assetsPrefix = SettingKey[String]("assetsPrefix")
    val playPackageAssets = TaskKey[File]("playPackageAssets")

    val playMonitoredFiles = TaskKey[Seq[File]]("playMonitoredFiles")
    val fileWatchService = SettingKey[FileWatchService]("fileWatchService", "The watch service Play uses to watch for file changes")
  }
}
