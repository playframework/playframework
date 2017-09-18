/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.sbt

import java.util.Optional

import play.api._
import sbt._

/**
 * Fix compatibility issues for RoutesCompiler. This is the version compatible with sbt 1.0.
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
    def line = toScala(problem.position.line).map(m => m.asInstanceOf[java.lang.Integer]).orNull
    def position = toScala(problem.position.pointer).map(m => m.asInstanceOf[java.lang.Integer]).orNull
    def input = toScala(problem.position.sourceFile).map(IO.read(_)).orNull
    def sourceName = toScala(problem.position.sourceFile).map(_.getAbsolutePath).orNull
  }

  private def toScala[T](o: Optional[T]): Option[T] = {
    if (o.isPresent) Option(o.get())
    else None
  }

}
