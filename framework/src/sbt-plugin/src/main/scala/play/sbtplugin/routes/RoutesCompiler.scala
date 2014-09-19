/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.sbtplugin.routes

import play.routes.compiler.{ RoutesGenerator, RoutesCompilationError }
import sbt._
import sbt.Keys._
import play.PlayExceptions.RoutesCompilationException
import com.typesafe.sbt.web.incremental._
import play.api.PlayException
import sbt.plugins.JvmPlugin

object RoutesKeys {
  val routesFiles = TaskKey[Seq[File]]("play-routes-files", "The routes files to compile")
  val routes = TaskKey[Seq[File]]("play-routes", "Compile the routes files")
  val routesImport = SettingKey[Seq[String]]("play-routes-imports", "Imports for the router")
  val routesGenerator = SettingKey[RoutesGenerator]("play-routes-generator", "The routes generator")
  val generateReverseRouter = SettingKey[Boolean]("play-generate-reverse-router",
    "Whether the reverse router should be generated. Setting to false may reduce compile times if it's not needed.")
  val generateRefReverseRouter = SettingKey[Boolean]("play-generate-ref-reverse-router",
    "Whether the ref reverse router should be generated along with reverse router. Setting to false will make it easy to export routes to other projects and improve compile time.")
  val namespaceReverseRouter = SettingKey[Boolean]("play-namespace-reverse-router",
    "Whether the reverse router should be namespaced. Useful if you have many routers that use the same actions.")

  val InjectedRoutesGenerator = play.routes.compiler.InjectedRoutesGenerator
  val StaticRoutesGenerator = play.routes.compiler.StaticRoutesGenerator
}

object RoutesCompiler extends AutoPlugin {
  import RoutesKeys._

  override def trigger = noTrigger

  override def requires = JvmPlugin

  val autoImport = RoutesKeys

  override def projectSettings =
    defaultSettings ++
      inConfig(Compile)(routesSettings) ++
      inConfig(Test)(routesSettings)

  def routesSettings = Seq(
    routesFiles := Nil,

    watchSources in Defaults.ConfigGlobal <++= routesFiles,

    target in routes := crossTarget.value / "routes" / Defaults.nameForSrc(configuration.value.name),

    routes <<= compileRoutesFiles,

    sourceGenerators <+= routes,
    managedSourceDirectories <+= target in routes
  )

  def defaultSettings = Seq(
    routesImport := Nil,
    generateReverseRouter := true,
    generateRefReverseRouter := true,
    namespaceReverseRouter := false,
    routesGenerator := StaticRoutesGenerator
  )

  private val compileRoutesFiles = Def.task[Seq[File]] {
    compileRoutes(routesFiles.value, routesGenerator.value, (target in routes).value, routesImport.value,
      generateReverseRouter.value, generateRefReverseRouter.value, namespaceReverseRouter.value,
      streams.value.cacheDirectory, state.value.log)
  }

  def compileRoutes(files: Seq[File], generator: RoutesGenerator, generatedDir: File, additionalImports: Seq[String],
    reverseRouter: Boolean, reverseRefRouter: Boolean, namespaceRouter: Boolean, cacheDirectory: File,
    log: Logger): Seq[File] = {
    val ops = files.map(f => RoutesCompilerOp(f, generator.id, additionalImports, reverseRouter, reverseRefRouter,
      namespaceRouter))
    val (products, errors) = syncIncremental(cacheDirectory, ops) { opsToRun: Seq[RoutesCompilerOp] =>

      val results = opsToRun.map { op =>
        op -> play.routes.compiler.RoutesCompiler.compile(op.file, generator, generatedDir, op.additionalImports, op.reverseRouter, op.reverseRefRouter,
          op.namespaceReverseRouter)
      }
      val opResults = results.map {
        case (op, Right(inputs)) => op -> OpSuccess(Set(op.file), inputs.toSet)
        case (op, Left(_)) => op -> OpFailure
      }.toMap
      val errors = results.collect {
        case (_, Left(e)) => e
      }.flatten
      (opResults, errors)
    }

    if (errors.nonEmpty) {
      val exceptions = errors.map {
        case RoutesCompilationError(source, message, line, column) =>
          reportCompilationError(log, RoutesCompilationException(source, message, line, column.map(_ - 1)))
      }
      throw exceptions.head
    }

    products.to[Seq]
  }

  private def reportCompilationError(log: Logger, error: PlayException.ExceptionSource) = {
    // log the source file and line number with the error message
    log.error(Option(error.sourceName).getOrElse("") + Option(error.line).map(":" + _).getOrElse("") + ": " + error.getMessage)
    Option(error.interestingLines(0)).map(_.focus).flatMap(_.headOption) map { line =>
      // log the line
      log.error(line)
      Option(error.position).map { pos =>
        // print a carat under the offending character
        val spaces = (line: Seq[Char]).take(pos).map {
          case '\t' => '\t'
          case x => ' '
        }
        log.error(spaces.mkString + "^")
      }
    }
    error
  }
}

private case class RoutesCompilerOp(file: File, generatorId: String, additionalImports: Seq[String], reverseRouter: Boolean,
  reverseRefRouter: Boolean, namespaceReverseRouter: Boolean)
