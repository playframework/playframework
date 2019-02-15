/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import java.util.Optional

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
      case overloaded(method, _, signature) => "Overloaded method value [" + method + "] cannot be applied to " + signature
      case msg => msg
    }
  }

  case class UnexpectedException(message: Option[String] = None, unexpected: Option[Throwable] = None) extends PlayException(
    "Unexpected exception",
    message.getOrElse {
      unexpected.map(t => "%s: %s".format(t.getClass.getSimpleName, t.getMessage)).getOrElse("")
    },
    unexpected.orNull
  )

  case class CompilationException(problem: xsbti.Problem) extends PlayException.ExceptionSource(
    "Compilation error", filterAnnoyingErrorMessages(problem.message)) {
    def line = problem.position.line.asScala.map(m => m.asInstanceOf[java.lang.Integer]).orNull
    def position = problem.position.pointer.asScala.map(m => m.asInstanceOf[java.lang.Integer]).orNull
    def input = problem.position.sourceFile.asScala.map(IO.read(_)).orNull
    def sourceName = problem.position.sourceFile.asScala.map(_.getAbsolutePath).orNull
  }

}
