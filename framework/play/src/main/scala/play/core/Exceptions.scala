package play.core

import java.io.File
import sbt.IO

case class PlayException(title: String, description: String, cause: Option[Throwable] = None) extends RuntimeException("%s -> %s".format(title, description), cause.orNull) {
    
    val id = "x"
    
}

trait ExceptionSource {
    self: PlayException =>
    
    def line: Option[Int]
    def position: Option[Int]
    def file: Option[File]
    
    def interestingLines(border: Int = 4): Option[(Int,Seq[String],Int)] = {
        for(f <- file; l <- line; val (first,last) = IO.readLines(f).splitAt(l-1); focus <- last.headOption) yield {
            val before = first.takeRight(border)
            val after = last.drop(1).take(border)
            val firstLine = l - before.size
            val errorLine = before.size
            (firstLine, (before :+ focus) ++ after, errorLine)
        }
    }
    
}

trait ExceptionAttachment {
    self: PlayException =>
    
    def subTitle: String
    def content: String
    
}

trait RichDescription {
    self: PlayException =>
    
    def htmlDescription: String
}

case class UnexpectedException(message: Option[String] = None, unexpected: Option[Throwable] = None) extends PlayException(
    "Unexpected exception",
    message.getOrElse {
        unexpected.map(t => "%s: %s".format(t.getClass.getSimpleName, t.getMessage)).getOrElse("")
    },
    unexpected
)

case class RequestParsingException(error: Throwable) extends RuntimeException(error)

case class ExecutionException(target: Throwable, source: Option[(File,Int)]) extends PlayException(
    "Execution exception",
    "%s: %s".format(target.getClass.getSimpleName, target.getMessage),
    Some(target)
) with ExceptionSource {
    def line = source.map(_._2)
    def position = None
    def file = source.map(_._1)
}
