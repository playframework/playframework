package play

import sbt._
import play.api._

trait PlayExceptions {

  def filterAnnoyingErrorMessages(message: String): String = {
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
    def input = problem.position.sourceFile.map(scalax.file.Path(_).string).orNull
    def sourceName = problem.position.sourceFile.map(_.getAbsolutePath).orNull
  }

  case class TemplateCompilationException(source: File, message: String, atLine: Int, column: Int) extends PlayException.ExceptionSource(
    "Compilation error", message) with FeedbackProvidedException {
    def line = atLine
    def position = column
    def input = scalax.file.Path(source).string
    def sourceName = source.getAbsolutePath
  }

  case class RoutesCompilationException(source: File, message: String, atLine: Option[Int], column: Option[Int]) extends PlayException.ExceptionSource(
    "Compilation error", message) with FeedbackProvidedException {
    def line = atLine.map(_.asInstanceOf[java.lang.Integer]).orNull
    def position = column.map(_.asInstanceOf[java.lang.Integer]).orNull
    def input = scalax.file.Path(source).string
    def sourceName = source.getAbsolutePath
  }

  case class AssetCompilationException(source: Option[File], message: String, atLine: Option[Int], column: Option[Int]) extends PlayException.ExceptionSource(
    "Compilation error", message) with FeedbackProvidedException {
    def line = atLine.map(_.asInstanceOf[java.lang.Integer]).orNull
    def position = column.map(_.asInstanceOf[java.lang.Integer]).orNull
    def input = source.filter(_.exists()).map(scalax.file.Path(_).string).orNull
    def sourceName = source.map(_.getAbsolutePath).orNull
  }

}

object PlayExceptions extends PlayExceptions
