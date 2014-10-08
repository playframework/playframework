/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play

import sbt._
import play.runsupport.PlayExceptions.{ TemplateCompilationException, RoutesCompilationException }
import play.api.PlayException

trait PlaySourceGenerators {

  val RouteFiles = (state: State, sourceDirectories: Seq[File], generatedDir: File, additionalImports: Seq[String], reverseRouter: Boolean, reverseRefRouter: Boolean, namespaceReverseRouter: Boolean) => {
    import play.router.RoutesCompiler._

    val javaRoutes = (generatedDir ** "routes.java")
    val scalaRoutes = (generatedDir ** "routes_*.scala")
    (javaRoutes.get ++ scalaRoutes.get).map(GeneratedSource(_)).foreach(_.sync())
    try {
      { (sourceDirectories * "*.routes").get ++ (sourceDirectories * "routes").get }.map { routesFile =>
        compile(routesFile, generatedDir, additionalImports, reverseRouter, reverseRefRouter, namespaceReverseRouter)
      }
    } catch {
      case RoutesCompilationError(source, message, line, column) => {
        throw reportCompilationError(state, RoutesCompilationException(source, message, line, column.map(_ - 1)))
      }
    }

    (scalaRoutes.get ++ javaRoutes.get).map(_.getAbsoluteFile)

  }

  def reportCompilationError(state: State, error: PlayException.ExceptionSource) = {
    val log = state.log
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

object PlaySourceGenerators extends PlaySourceGenerators
