/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play

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

  val defaultJavaTemplatesImport = Seq(
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

  val defaultScalaTemplatesImport = Seq(
    "models._",
    "controllers._",

    "play.api.i18n._",

    "play.api.mvc._",
    "play.api.data._",

    "views.%format%._")

  val defaultTemplatesImport = Seq("play.api.templates._", "play.api.templates.PlayMagic._")

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
    val playVersion = SettingKey[String]("play-version")

    val playDefaultPort = SettingKey[Int]("play-default-port")

    val requireJs = SettingKey[Seq[String]]("play-require-js")

    val requireJsFolder = SettingKey[String]("play-require-js-folder")

    val requireJsShim = SettingKey[String]("play-require-js-shim")

    val requireNativePath = SettingKey[Option[String]]("play-require-native-path")

    /** Our means of hooking the run task with additional behavior. */
    val playRunHooks = TaskKey[Seq[play.PlayRunHook]]("play-run-hooks")

    /** A hook to configure how play blocks on user input while running. */
    val playInteractionMode = SettingKey[play.PlayInteractionMode]("play-interaction-mode")

    val confDirectory = SettingKey[File]("play-conf")

    val templatesImport = SettingKey[Seq[String]]("play-templates-imports")

    val routesImport = SettingKey[Seq[String]]("play-routes-imports")

    val generateReverseRouter = SettingKey[Boolean]("play-generate-reverse-router",
      "Whether the reverse router should be generated. Setting to false may reduce compile times if it's not needed.")

    val generateRefReverseRouter = SettingKey[Boolean]("play-generate-ref-reverse-router",
      "Whether the ref reverse router should be generated along with reverse router. Setting to false will make it easy to export routes to other projects and improve compile time.")

    val namespaceReverseRouter = SettingKey[Boolean]("play-namespace-reverse-router",
      "Whether the reverse router should be namespaced. Useful if you have many routers that use the same actions.")

    val ebeanEnabled = SettingKey[Boolean]("play-ebean-enabled")

    val templatesTypes = SettingKey[Map[String, String]]("play-templates-formats")

    val closureCompilerOptions = SettingKey[Seq[String]]("play-closure-compiler-options")

    val lessOptions = SettingKey[Seq[String]]("play-less-options")

    val coffeescriptOptions = SettingKey[Seq[String]]("play-coffeescript-options")

    val lessEntryPoints = SettingKey[PathFinder]("play-less-entry-points")

    val coffeescriptEntryPoints = SettingKey[PathFinder]("play-coffeescript-entry-points")

    val javascriptEntryPoints = SettingKey[PathFinder]("play-javascript-entry-points")

    val playPlugin = SettingKey[Boolean]("play-plugin")

    val devSettings = SettingKey[Seq[(String, String)]]("play-dev-settings")

    val generateSecret = TaskKey[String]("play-generate-secret", "Generate a new application secret", KeyRanks.BTask)
    val updateSecret = TaskKey[File]("play-update-secret", "Update the application conf to generate an application secret", KeyRanks.BTask)

  }
}
