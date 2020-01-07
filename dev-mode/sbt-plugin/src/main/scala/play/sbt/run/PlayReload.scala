/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt.run

import java.util.Optional

import sbt._
import sbt.Keys._
import sbt.internal.Output
import sbt.internal.inc.Analysis
import sbt.util.InterfaceUtil.o2jo

import play.api.PlayException
import play.runsupport.Reloader.CompileFailure
import play.runsupport.Reloader.CompileResult
import play.runsupport.Reloader.CompileSuccess
import play.runsupport.Reloader.Source
import play.sbt.PlayExceptions.CompilationException
import play.sbt.PlayExceptions.UnexpectedException
import play.twirl.compiler.MaybeGeneratedSource

import xsbti.CompileFailed
import xsbti.Position
import xsbti.Problem
import xsbti.Severity

object PlayReload {
  def taskFailureHandler(incomplete: Incomplete, streams: Option[Streams]): PlayException = {
    Incomplete
      .allExceptions(incomplete)
      .headOption
      .map {
        case e: PlayException => e
        case e: CompileFailed =>
          getProblems(incomplete, streams)
            .find(_.severity == Severity.Error)
            .map(CompilationException)
            .getOrElse(UnexpectedException(Some("The compilation failed without reporting any problem!"), Some(e)))
        case e: Exception => UnexpectedException(unexpected = Some(e))
      }
      .getOrElse {
        UnexpectedException(Some("The compilation task failed without any exception!"))
      }
  }

  def getScopedKey(incomplete: Incomplete): Option[ScopedKey[_]] = incomplete.node.flatMap {
    case key: ScopedKey[_] => Option(key)
    case task: Task[_]     => task.info.attributes.get(taskDefinitionKey)
  }

  def compile(
      reloadCompile: () => Result[Analysis],
      classpath: () => Result[Classpath],
      streams: () => Option[Streams]
  ): CompileResult = {
    val compileResult: Either[Incomplete, CompileSuccess] = for {
      analysis  <- reloadCompile().toEither.right
      classpath <- classpath().toEither.right
    } yield CompileSuccess(sourceMap(analysis), classpath.files)
    compileResult.left.map(inc => CompileFailure(taskFailureHandler(inc, streams()))).merge
  }

  def sourceMap(analysis: Analysis): Map[String, Source] = {
    analysis.relations.classes.reverseMap.flatMap {
      case (name, files) =>
        files.headOption match {
          case None       => Map.empty[String, Source]
          case Some(file) => Map(name -> Source(file, MaybeGeneratedSource.unapply(file).flatMap(_.source)))
        }
    }
  }

  def getProblems(incomplete: Incomplete, streams: Option[Streams]): Seq[Problem] = {
    allProblems(incomplete) ++ {
      Incomplete.linearize(incomplete).flatMap(getScopedKey).flatMap { scopedKey =>
        val JavacError         = """\[error\]\s*(.*[.]java):(\d+):\s*(.*)""".r
        val JavacErrorInfo     = """\[error\]\s*([a-z ]+):(.*)""".r
        val JavacErrorPosition = """\[error\](\s*)\^\s*""".r

        streams
          .map { streamsManager =>
            var first: (Option[(String, String, String)], Option[Int])  = (None, None)
            var parsed: (Option[(String, String, String)], Option[Int]) = (None, None)
            Output
              .lastLines(scopedKey, streamsManager, None)
              .map(_.replace(scala.Console.RESET, ""))
              .map(_.replace(scala.Console.RED, ""))
              .collect {
                case JavacError(file, line, message) => parsed = Some((file, line, message)) -> None
                case JavacErrorInfo(key, message) =>
                  parsed._1.foreach {
                    case (file, line, message1) =>
                      parsed = Some((file, line, s"$message1 [${key.trim}: ${message.trim}]")) -> None
                  }
                case JavacErrorPosition(pos) =>
                  parsed = parsed._1 -> Some(pos.length)
                  if (first == ((None, None))) {
                    first = parsed
                  }
              }
            first
          }
          .collect {
            case (Some((fileName, lineNo, msg)), pos) =>
              new ProblemImpl(msg, new PositionImpl(fileName, lineNo.toInt, pos))
          }
      }
    }
  }

  def allProblems(inc: Incomplete): Seq[Problem] = allProblems(inc :: Nil)

  def allProblems(incs: Seq[Incomplete]): Seq[Problem] = problems(Incomplete.allExceptions(incs).toSeq)

  def problems(es: Seq[Throwable]): Seq[Problem] = {
    es.flatMap {
      case cf: CompileFailed => cf.problems
      case _                 => Nil
    }
  }

  private class PositionImpl(fileName: String, lineNo: Int, pos: Option[Int]) extends Position {
    def line         = Optional.ofNullable(lineNo)
    def lineContent  = ""
    def offset       = Optional.empty[Integer]
    def pointer      = o2jo(pos.map(_ - 1))
    def pointerSpace = Optional.empty[String]
    def sourcePath   = Optional.ofNullable(fileName)
    def sourceFile   = Optional.ofNullable(file(fileName))
  }

  private class ProblemImpl(msg: String, pos: Position) extends Problem {
    def category = ""
    def severity = Severity.Error
    def message  = msg
    def position = pos
  }
}
