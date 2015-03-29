/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.sbt.routes

import play.core.PlayVersion
import play.routes.compiler.{ RoutesGenerator, RoutesCompilationError }
import play.routes.compiler.RoutesCompiler.GeneratedSource
import sbt._
import sbt.Keys._
import com.typesafe.sbt.web.incremental._
import play.api.PlayException
import sbt.plugins.JvmPlugin
import xsbti.Position

object RoutesKeys {
  val routesFiles = TaskKey[Seq[File]]("play-routes-files", "The routes files to compile")
  val routes = TaskKey[Seq[File]]("play-routes", "Compile the routes files")
  val routesImport = SettingKey[Seq[String]]("play-routes-imports", "Imports for the router")
  val routesGenerator = SettingKey[RoutesGenerator]("play-routes-generator", "The routes generator")
  val generateReverseRouter = SettingKey[Boolean]("play-generate-reverse-router",
    "Whether the reverse router should be generated. Setting to false may reduce compile times if it's not needed.")
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
    namespaceReverseRouter := false,
    routesGenerator := StaticRoutesGenerator,
    sourcePositionMappers += routesPositionMapper
  )

  private val compileRoutesFiles = Def.task[Seq[File]] {
    compileRoutes(routesFiles.value, routesGenerator.value, (target in routes).value, routesImport.value,
      generateReverseRouter.value, namespaceReverseRouter.value, streams.value.cacheDirectory, state.value.log)
  }

  def compileRoutes(files: Seq[File], generator: RoutesGenerator, generatedDir: File, additionalImports: Seq[String],
    reverseRouter: Boolean, namespaceRouter: Boolean, cacheDirectory: File, log: Logger): Seq[File] = {
    val ops = files.map(f => RoutesCompilerOp(f, generator.id, PlayVersion.current, additionalImports, reverseRouter,
      namespaceRouter))
    val (products, errors) = syncIncremental(cacheDirectory, ops) { opsToRun: Seq[RoutesCompilerOp] =>

      val results = opsToRun.map { op =>
        op -> play.routes.compiler.RoutesCompiler.compile(op.file, generator, generatedDir, op.additionalImports, op.reverseRouter,
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

  val routesPositionMapper: Position => Option[Position] = position => {
    position.sourceFile collect {
      case GeneratedSource(generatedSource) => {
        new xsbti.Position {
          lazy val line = {
            position.line.flatMap(l => generatedSource.mapLine(l.asInstanceOf[Int])).map(l => xsbti.Maybe.just(l.asInstanceOf[java.lang.Integer])).getOrElse(xsbti.Maybe.nothing[java.lang.Integer])
          }
          lazy val lineContent = {
            line flatMap { lineNo =>
              sourceFile.flatMap { file =>
                IO.read(file).split('\n').lift(lineNo - 1)
              }
            } getOrElse ""
          }
          val offset = xsbti.Maybe.nothing[java.lang.Integer]
          val pointer = xsbti.Maybe.nothing[java.lang.Integer]
          val pointerSpace = xsbti.Maybe.nothing[String]
          val sourceFile = xsbti.Maybe.just(generatedSource.source.get)
          val sourcePath = xsbti.Maybe.just(sourceFile.get.getCanonicalPath)
        }
      }
    }
  }
}

private case class RoutesCompilerOp(file: File, generatorId: String, playVersion: String,
  additionalImports: Seq[String], reverseRouter: Boolean, namespaceReverseRouter: Boolean)

case class RoutesCompilationException(source: File, message: String, atLine: Option[Int], column: Option[Int]) extends PlayException.ExceptionSource(
  "Compilation error", message) with FeedbackProvidedException {
  def line = atLine.map(_.asInstanceOf[java.lang.Integer]).orNull
  def position = column.map(_.asInstanceOf[java.lang.Integer]).orNull
  def input = IO.read(source)
  def sourceName = source.getAbsolutePath
}
