/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.sbt.routes

import play.core.PlayVersion
import play.routes.compiler.{ RoutesGenerator, RoutesCompilationError }
import play.routes.compiler.RoutesCompiler.{ RoutesCompilerTask, GeneratedSource }
import sbt._
import sbt.Keys._
import com.typesafe.sbt.web.incremental._
import play.api.PlayException
import sbt.plugins.JvmPlugin
import xsbti.Position

import scala.language.implicitConversions

object RoutesKeys {
  val routesCompilerTasks = TaskKey[Seq[RoutesCompilerTask]]("playRoutesTasks", "The routes files to compile")
  val routes = TaskKey[Seq[File]]("playRoutes", "Compile the routes files")
  val routesImport = SettingKey[Seq[String]]("playRoutesImports", "Imports for the router")
  val routesGenerator = SettingKey[RoutesGenerator]("playRoutesGenerator", "The routes generator")
  val generateReverseRouter = SettingKey[Boolean]("playGenerateReverseRouter",
    "Whether the reverse router should be generated. Setting to false may reduce compile times if it's not needed.")
  val namespaceReverseRouter = SettingKey[Boolean]("playNamespaceReverseRouter",
    "Whether the reverse router should be namespaced. Useful if you have many routers that use the same actions.")

  /**
   * This class is used to avoid infinite recursions when configuring aggregateReverseRoutes, since it makes the
   * ProjectReference a thunk.
   */
  class LazyProjectReference(ref: => ProjectReference) {
    def project: ProjectReference = ref
  }

  object LazyProjectReference {
    implicit def fromProjectReference(ref: => ProjectReference): LazyProjectReference = new LazyProjectReference(ref)
    implicit def fromProject(project: => Project): LazyProjectReference = new LazyProjectReference(project)
  }

  val aggregateReverseRoutes = SettingKey[Seq[LazyProjectReference]]("playAggregateReverseRoutes",
    "A list of projects that reverse routes should be aggregated from.")

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
    sources in routes := Nil,

    routesCompilerTasks <<= Def.taskDyn {

      // Aggregate all the routes file tasks that we want to compile the reverse routers for.
      aggregateReverseRoutes.value.map { agg =>
        routesCompilerTasks in (agg.project, configuration.value)
      }.join.map { aggTasks: Seq[Seq[RoutesCompilerTask]] =>

        // Aggregated tasks need to have forwards router compilation disabled and reverse router compilation enabled.
        val reverseRouterTasks = aggTasks.flatten.map { task =>
          task.copy(forwardsRouter = false, reverseRouter = true)
        }

        // Find the routes compile tasks for this project
        val thisProjectTasks = (sources in routes).value.map { file =>
          RoutesCompilerTask(file, routesImport.value, forwardsRouter = true,
            reverseRouter = generateReverseRouter.value, namespaceReverseRouter = namespaceReverseRouter.value)
        }

        thisProjectTasks ++ reverseRouterTasks
      }
    },

    watchSources in Defaults.ConfigGlobal <++= sources in routes,

    target in routes := crossTarget.value / "routes" / Defaults.nameForSrc(configuration.value.name),

    routes <<= compileRoutesFiles,

    sourceGenerators <+= routes,
    managedSourceDirectories <+= target in routes
  )

  def defaultSettings = Seq(
    routesImport := Nil,
    aggregateReverseRoutes := Nil,

    // Generate reverse router defaults to true if this project is not aggregated by any of the projects it depends on
    // aggregateReverseRoutes projects.  Otherwise, it will be false, since another project will be generating the
    // reverse router for it.
    generateReverseRouter <<= Def.settingDyn {
      val projectRef = thisProjectRef.value
      val dependencies = buildDependencies.value.classpathTransitiveRefs(projectRef)

      // Go through each dependency of this project
      dependencies.map { dep =>

        // Get the aggregated reverse routes projects for the dependency, if defined
        Def.optional(aggregateReverseRoutes in dep)(_.map(_.map(_.project)).getOrElse(Nil))

      }.join.apply { aggregated: Seq[Seq[ProjectReference]] =>
        val localProject = LocalProject(projectRef.project)
        // Return false if this project is aggregated by one of our dependencies
        !aggregated.flatten.contains(localProject)
      }
    },

    namespaceReverseRouter := false,
    routesGenerator := StaticRoutesGenerator,
    sourcePositionMappers += routesPositionMapper
  )

  private val compileRoutesFiles = Def.task[Seq[File]] {
    compileRoutes(routesCompilerTasks.value, routesGenerator.value, (target in routes).value, streams.value.cacheDirectory,
      state.value.log)
  }

  def compileRoutes(tasks: Seq[RoutesCompilerTask], generator: RoutesGenerator, generatedDir: File,
    cacheDirectory: File, log: Logger): Seq[File] = {
    val ops = tasks.map(task => RoutesCompilerOp(task, generator.id, PlayVersion.current))
    val (products, errors) = syncIncremental(cacheDirectory, ops) { opsToRun: Seq[RoutesCompilerOp] =>

      val results = opsToRun.map { op =>
        op -> play.routes.compiler.RoutesCompiler.compile(op.task, generator, generatedDir)
      }
      val opResults = results.map {
        case (op, Right(inputs)) => op -> OpSuccess(Set(op.task.file), inputs.toSet)
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

private case class RoutesCompilerOp(task: RoutesCompilerTask, generatorId: String, playVersion: String)

case class RoutesCompilationException(source: File, message: String, atLine: Option[Int], column: Option[Int]) extends PlayException.ExceptionSource(
  "Compilation error", message) with FeedbackProvidedException {
  def line = atLine.map(_.asInstanceOf[java.lang.Integer]).orNull
  def position = column.map(_.asInstanceOf[java.lang.Integer]).orNull
  def input = IO.read(source)
  def sourceName = source.getAbsolutePath
}
