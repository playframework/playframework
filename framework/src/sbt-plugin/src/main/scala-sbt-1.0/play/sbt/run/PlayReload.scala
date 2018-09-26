/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt.run

import sbt._
import sbt.Keys._
import sbt.internal.Output

import play.api.PlayException
import play.runsupport.Reloader.{ CompileFailure, CompileResult, CompileSuccess, Source }
import play.sbt.PlayExceptions.{ CompilationException, UnexpectedException }

/**
 * Fix compatibility issues for PlayReload. This is the version compatible with sbt 1.0.
 */
object PlayReload {

  def originalSource(file: File): Option[File] = {
    play.twirl.compiler.MaybeGeneratedSource.unapply(file).flatMap(_.source)
  }

  def compileFailure(streams: Option[Streams])(incomplete: Incomplete): CompileResult = {
    CompileFailure(taskFailureHandler(incomplete, streams))
  }

  def taskFailureHandler(incomplete: Incomplete, streams: Option[Streams]): PlayException = {
    Incomplete.allExceptions(incomplete).headOption.map {
      case e: PlayException => e
      case e: xsbti.CompileFailed =>
        getProblems(incomplete, streams)
          .find(_.severity == xsbti.Severity.Error)
          .map(CompilationException)
          .getOrElse(UnexpectedException(Some("The compilation failed without reporting any problem!"), Some(e)))
      case e: Exception => UnexpectedException(unexpected = Some(e))
    }.getOrElse {
      UnexpectedException(Some("The compilation task failed without any exception!"))
    }
  }

  def getScopedKey(incomplete: Incomplete): Option[ScopedKey[_]] = incomplete.node flatMap {
    case key: ScopedKey[_] => Option(key)
    case task: Task[_] => task.info.attributes get taskDefinitionKey
  }

  def compile(reloadCompile: () => Result[sbt.internal.inc.Analysis], classpath: () => Result[Classpath], streams: () => Option[Streams]): CompileResult = {
    val compileResult: Either[Incomplete, CompileSuccess] = for {
      analysis <- reloadCompile().toEither.right
      classpath <- classpath().toEither.right
    } yield CompileSuccess(sourceMap(analysis), classpath.files)
    compileResult.left.map(compileFailure(streams())).merge
  }

  def sourceMap(analysis: sbt.internal.inc.Analysis): Map[String, Source] = {
    analysis
      .relations
      .classes
      .reverseMap
      .mapValues { files =>
        val file = files.head // This is typically a set containing a single file, so we can use head here.
        Source(file, originalSource(file))
      }
  }

  def getProblems(incomplete: Incomplete, streams: Option[Streams]): Seq[xsbti.Problem] = {
    allProblems(incomplete) ++ {
      Incomplete.linearize(incomplete).flatMap(getScopedKey).flatMap { scopedKey =>
        val JavacError = """\[error\]\s*(.*[.]java):(\d+):\s*(.*)""".r
        val JavacErrorInfo = """\[error\]\s*([a-z ]+):(.*)""".r
        val JavacErrorPosition = """\[error\](\s*)\^\s*""".r

        streams.map { streamsManager =>
          var first: (Option[(String, String, String)], Option[Int]) = (None, None)
          var parsed: (Option[(String, String, String)], Option[Int]) = (None, None)
          Output.lastLines(scopedKey, streamsManager, None).map(_.replace(scala.Console.RESET, "")).map(_.replace(scala.Console.RED, "")).collect {
            case JavacError(file, line, message) => parsed = Some((file, line, message)) -> None
            case JavacErrorInfo(key, message) => parsed._1.foreach { o =>
              parsed = Some((parsed._1.get._1, parsed._1.get._2, parsed._1.get._3 + " [" + key.trim + ": " + message.trim + "]")) -> None
            }
            case JavacErrorPosition(pos) =>
              parsed = parsed._1 -> Some(pos.size)
              if (first == ((None, None))) {
                first = parsed
              }
          }
          first
        }.collect {
          case (Some(error), maybePosition) => new xsbti.Problem {
            def message = error._3
            def category = ""
            def position = new xsbti.Position {
              def line = java.util.Optional.ofNullable(error._2.toInt)
              def lineContent = ""
              def offset = java.util.Optional.empty[java.lang.Integer]
              def pointer = maybePosition.map(pos => java.util.Optional.ofNullable((pos - 1).asInstanceOf[java.lang.Integer])).getOrElse(java.util.Optional.empty[java.lang.Integer])
              def pointerSpace = java.util.Optional.empty[String]
              def sourceFile = java.util.Optional.ofNullable(file(error._1))
              def sourcePath = java.util.Optional.ofNullable(error._1)
            }
            def severity = xsbti.Severity.Error
          }
        }

      }
    }
  }

  def allProblems(inc: Incomplete): Seq[xsbti.Problem] = {
    allProblems(inc :: Nil)
  }

  def allProblems(incs: Seq[Incomplete]): Seq[xsbti.Problem] = {
    problems(Incomplete.allExceptions(incs).toSeq)
  }

  def problems(es: Seq[Throwable]): Seq[xsbti.Problem] = {
    es flatMap {
      case cf: xsbti.CompileFailed => cf.problems
      case _ => Nil
    }
  }

}
