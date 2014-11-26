/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play

import play.runsupport.PlayWatchService
import play.sbtplugin.run._
import sbt._
import Keys._

/**
 * Declares the default imports for Play plugins.
 */
object PlayImport {

  val Production = config("production")

  def component(id: String) = "com.typesafe.play" %% id % play.core.PlayVersion.current

  val jdbc = component("play-jdbc")

  val anorm = component("anorm")

  val javaCore = component("play-java")

  val javaJdbc = component("play-java-jdbc")

  val javaEbean = component("play-java-ebean")

  val javaJpa = component("play-java-jpa")

  val filters = component("filters-helpers")

  val cache = component("play-cache")

  val json = component("play-json")

  val ws = "com.typesafe.play" %% "play-ws" % play.core.PlayVersion.current

  val javaWs = "com.typesafe.play" %% "play-java-ws" % play.core.PlayVersion.current

  val defaultJavaTemplateImports = Seq(
    "models._",
    "controllers._",

    "java.lang._",
    "java.util._",

    "scala.collection.JavaConversions._",
    "scala.collection.JavaConverters._",

    "play.api.i18n._",
    "play.core.j.PlayMagicForJava._",

    "play.mvc._",
    "play.data._",
    "play.api.data.Field",

    "play.mvc.Http.Context.Implicit._",

    "views.%format%._")

  val defaultScalaTemplateImports = Seq(
    "models._",
    "controllers._",

    "play.api.i18n._",

    "play.api.mvc._",
    "play.api.data._",

    "views.%format%._")

  val defaultTemplateImports = Seq("play.api.templates.PlayMagic._")

  /**
   * Add this to your build.sbt, eg:
   *
   * {{{
   *   emojiLogs
   * }}}
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

  object PlayKeys {
    val standardLayout = SettingKey[Boolean]("play-standard-layout", "Whether to use the standard SBT layout instead of the original Play layout.")

    val playDefaultPort = SettingKey[Int]("play-default-port", "The default port that Play runs on")

    /** Our means of hooking the run task with additional behavior. */
    val playRunHooks = TaskKey[Seq[play.PlayRunHook]]("play-run-hooks", "Hooks to run additional behaviour before/after the run task")

    /** A hook to configure how play blocks on user input while running. */
    val playInteractionMode = SettingKey[play.PlayInteractionMode]("play-interaction-mode", "Hook to configure how Play blocks when running")

    val confDirectory = SettingKey[File]("play-conf", "Where the Play conf directory lives")

    val playOmnidoc = SettingKey[Boolean]("play-omnidoc", "Determines whether to use the aggregated Play documentation")
    val playDocsName = SettingKey[String]("play-docs-name", "Artifact name of the Play documentation")
    val playDocsModule = SettingKey[Option[ModuleID]]("play-docs-module", "Optional Play documentation dependency")
    val playDocsJar = TaskKey[Option[File]]("play-docs-jar", "Optional jar file containing the Play documentation")

    val ebeanEnabled = SettingKey[Boolean]("play-ebean-enabled")

    val ebeanModels = SettingKey[String]("play-ebean-models")

    val playPlugin = SettingKey[Boolean]("play-plugin")

    val devSettings = SettingKey[Seq[(String, String)]]("play-dev-settings")

    val generateSecret = TaskKey[String]("play-generate-secret", "Generate a new application secret", KeyRanks.BTask)
    val updateSecret = TaskKey[File]("play-update-secret", "Update the application conf to generate an application secret", KeyRanks.BTask)

    val assetsPrefix = SettingKey[String]("assets-prefix")
    @deprecated("Aggregating assets is always disabled. See sbt-web support for asset dependencies.", "2.4")
    val playAggregateAssets = SettingKey[Boolean]("play-aggregate-assets", "Determines whether assets from project dependencies are included.")
    val playPackageAssets = TaskKey[File]("play-package-assets")

    val playMonitoredFiles = TaskKey[Seq[String]]("play-monitored-files")
    val playWatchService = SettingKey[PlayWatchService]("play-watch-service", "The watch service Play uses to watch for file changes")
  }
}
