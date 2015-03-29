/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.sbt

import play.api._
import sbt._

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
    def line = problem.position.line.map(m => m.asInstanceOf[java.lang.Integer]).orNull
    def position = problem.position.pointer.map(m => m.asInstanceOf[java.lang.Integer]).orNull
    def input = problem.position.sourceFile.map(IO.read(_)).orNull
    def sourceName = problem.position.sourceFile.map(_.getAbsolutePath).orNull
  }

}
