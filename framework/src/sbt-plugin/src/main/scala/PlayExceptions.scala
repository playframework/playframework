package sbt

import play.api._
import play.core._

trait PlayExceptions {

  def filterAnnoyingErrorMessages(message: String): String = {
    val overloaded = """(?s)overloaded method value (.*) with alternatives:(.*)cannot be applied to(.*)""".r
    message match {
      case overloaded(method, _, signature) => "Overloaded method value [" + method + "] cannot be applied to " + signature
      case msg => msg
    }
  }

  case class CompilationException(problem: xsbti.Problem) extends PlayException(
    "Compilation error", filterAnnoyingErrorMessages(problem.message)) with PlayException.ExceptionSource {
    def line = problem.position.line.map(m => m.asInstanceOf[Int])
    def position = problem.position.pointer.map(m => m.asInstanceOf[Int])
    def input = problem.position.sourceFile.map(scalax.file.Path(_))
    def sourceName = problem.position.sourceFile.map(_.getAbsolutePath)
    override def toString =  "in " + sourceName.getOrElse("")+" - "+ super.toString()
  }

  case class TemplateCompilationException(source: File, message: String, atLine: Int, column: Int) extends PlayException(
    "Compilation error", message) with PlayException.ExceptionSource {
    def line = Some(atLine)
    def position = Some(column)
    def input = Some(scalax.file.Path(source))
    def sourceName = Some(source.getAbsolutePath)
    override def toString =  "in " + source.getAbsolutePath+" - "+ super.toString()
  }

  case class RoutesCompilationException(source: File, message: String, atLine: Option[Int], column: Option[Int]) extends PlayException(
    "Compilation error", message) with PlayException.ExceptionSource {
    def line = atLine
    def position = column
    def input = Some(scalax.file.Path(source))
    def sourceName = Some(source.getAbsolutePath)
    override def toString =  "in " + source.getAbsolutePath+" - "+ super.toString()
  }

  case class AssetCompilationException(source: Option[File], message: String, atLine: Int, atColumn: Int) extends PlayException(
    "Compilation error", message) with PlayException.ExceptionSource {
    def line = Some(atLine)
    def position = Some(atColumn)
    def input = source.filter(_.exists()).map(scalax.file.Path(_))
    def sourceName = source.map(_.getAbsolutePath)
    override def toString =  "in " + sourceName.getOrElse("")+" - "+ super.toString()

  }

}
object PlayExceptions extends PlayExceptions
