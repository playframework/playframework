package play

import sbt._
import PlayExceptions.{ TemplateCompilationException, RoutesCompilationException }
import play.api.PlayException

trait PlaySourceGenerators {

  val RouteFiles = (state: State, sourceDirectories: Seq[File], generatedDir: File, additionalImports: Seq[String], reverseRouter: Boolean, namespaceReverseRouter: Boolean) => {
    import play.router.RoutesCompiler._

    val javaRoutes = (generatedDir ** "routes.java")
    val scalaRoutes = (generatedDir ** "routes_*.scala")
    (javaRoutes.get ++ scalaRoutes.get).map(GeneratedSource(_)).foreach(_.sync())
    try {
      { (sourceDirectories * "*.routes").get ++ (sourceDirectories * "routes").get }.map { routesFile =>
        compile(routesFile, generatedDir, additionalImports, reverseRouter, namespaceReverseRouter)
      }
    } catch {
      case RoutesCompilationError(source, message, line, column) => {
        throw reportCompilationError(state, RoutesCompilationException(source, message, line, column.map(_ - 1)))
      }
      case e: Throwable => throw e
    }

    (scalaRoutes.get ++ javaRoutes.get).map(_.getAbsoluteFile)

  }

  val ScalaTemplates = (state: State, sourceDirectories: Seq[File], generatedDir: File, templateTypes: Map[String, String], additionalImports: Seq[String]) => {
    import play.templates._

    val templateExt: PartialFunction[File, (File, String, String)] = {
      case p if templateTypes.contains(p.name.split('.').last) =>
        val extension = p.name.split('.').last
        val exts = templateTypes(extension)
        (p, extension, exts)
    }
    (generatedDir ** "*.template.scala").get.map(GeneratedSource(_)).foreach(_.sync())
    try {

      sourceDirectories.foreach { sourceDirectory =>
        (sourceDirectory ** "*.scala.*").get.collect(templateExt).foreach {
          case (template, extension, format) =>
            ScalaTemplateCompiler.compile(
              template,
              sourceDirectory,
              generatedDir,
              format,
              additionalImports.map("import " + _.replace("%format%", extension)).mkString("\n"))
        }
      }
    } catch {
      case TemplateCompilationError(source, message, line, column) => {
        throw reportCompilationError(state, TemplateCompilationException(source, message, line, column - 1))
      }
    }

    (generatedDir ** "*.template.scala").get.map(_.getAbsoluteFile)
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
