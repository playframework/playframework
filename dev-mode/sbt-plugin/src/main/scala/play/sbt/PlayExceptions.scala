/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import sbt._
import xsbti.Problem
import play.api._

object PlayExceptions {
  private def filterAnnoyingErrorMessages(message: String): String = {
    val overloaded = """(?s)overloaded method value (.*) with alternatives:(.*)cannot be applied to(.*)""".r
    message match {
      case overloaded(method, _, signature) => s"Overloaded method value [$method] cannot be applied to $signature"
      case _                                => message
    }
  }

  case class UnexpectedException(message: Option[String] = None, unexpected: Option[Throwable] = None)
      extends PlayException(
        "Unexpected exception",
        message.getOrElse(unexpected.fold("")(t => s"${t.getClass.getSimpleName}: ${t.getMessage}")),
        unexpected.orNull
      )

  case class CompilationException(problem: Problem)
      extends PlayException.ExceptionSource("Compilation error", filterAnnoyingErrorMessages(problem.message)) {
    def line       = problem.position.line.asScala.orNull
    def position   = problem.position.pointer.asScala.orNull
    def input      = problem.position.sourceFile.asScala.map(IO.read(_)).orNull
    def sourceName = problem.position.sourceFile.asScala.map(_.getAbsolutePath).orNull
  }
}
