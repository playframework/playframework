/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import play.api._
import sbt._

import scala.language.implicitConversions

/**
 * Fix compatibility issues for PlayExceptions. This is the version compatible with sbt 1.0.
 */
object PlayExceptions {
  private def filterAnnoyingErrorMessages(message: String): String = {
    val overloaded = """(?s)overloaded method value (.*) with alternatives:(.*)cannot be applied to(.*)""".r
    message match {
      case overloaded(method, _, signature) =>
        "Overloaded method value [" + method + "] cannot be applied to " + signature
      case msg => msg
    }
  }

  case class UnexpectedException(message: Option[String] = None, unexpected: Option[Throwable] = None)
      extends PlayException(
        "Unexpected exception",
        message.getOrElse {
          unexpected.map(t => "%s: %s".format(t.getClass.getSimpleName, t.getMessage)).getOrElse("")
        },
        unexpected.orNull
      )

  case class CompilationException(problem: xsbti.Problem)
      extends PlayException.ExceptionSource("Compilation error", filterAnnoyingErrorMessages(problem.message)) {
    def line       = problem.position.line.asScala.map(m => m.asInstanceOf[java.lang.Integer]).orNull
    def position   = problem.position.pointer.asScala.map(m => m.asInstanceOf[java.lang.Integer]).orNull
    def input      = problem.position.sourceFile.asScala.map(sf => IO.read(convertSbtVirtualFile(sf))).orNull
    def sourceName = problem.position.sourceFile.asScala.map(convertSbtVirtualFile(_).getAbsolutePath).orNull
    private def convertSbtVirtualFile(sourceFile: File) = {
      val sfPath = sourceFile.getPath
      if (sfPath.startsWith("${")) { // check for ${BASE} or similar (in case it changes)
        // Like: ${BASE}/app/controllers/MyController.scala
        new File(sfPath.substring(sfPath.indexOf("}") + 2)).getAbsoluteFile
      } else {
        // A file outside of the base project folder or using sbt <1.4
        sourceFile
      }
    }
  }
}
