/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt.run

import sbt._
import sbt.Keys._

import play.api.PlayException
import play.runsupport.Reloader.{ CompileFailure, CompileResult, CompileSuccess, Source }
import play.sbt.PlayExceptions.{ CompilationException, UnexpectedException }

/**
 * Fix compatibility issues for PlayReload. This is the version compatible with sbt 0.13.
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

  def compile(reloadCompile: () => Result[sbt.inc.Analysis], classpath: () => Result[Classpath], streams: () => Option[Streams]): CompileResult = {
    val compileResult: Either[Incomplete, CompileSuccess] = for {
      analysis <- reloadCompile().toEither.right
      classpath <- classpath().toEither.right
    } yield CompileSuccess(sourceMap(analysis), classpath.files)
    compileResult.left.map(compileFailure(streams())).merge
  }

  def sourceMap(analysis: sbt.inc.Analysis): Map[String, Source] = {
    analysis.apis.internal.foldLeft(Map.empty[String, Source]) {
      case (sourceMap, (file, source)) => sourceMap ++ {
        source.api.definitions map { d => d.name -> Source(file, originalSource(file)) }
      }
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
              parsed = parsed._1 -> Some(pos.length)
              if (first == ((None, None))) {
                first = parsed
              }
          }
          first
        }.collect {
          case (Some(error), maybePosition) => new xsbti.Problem {
            override def message: String = error._3
            override def category: String = ""
            override def position: xsbti.Position = new xsbti.Position {
              override def line: xsbti.Maybe[java.lang.Integer] = xsbti.Maybe.just(error._2.toInt)
              override def lineContent: String = ""
              override def offset: xsbti.Maybe[java.lang.Integer] = xsbti.Maybe.nothing[java.lang.Integer]
              override def pointer: xsbti.Maybe[java.lang.Integer] = maybePosition.map(pos => xsbti.Maybe.just((pos - 1).asInstanceOf[java.lang.Integer])).getOrElse(xsbti.Maybe.nothing[java.lang.Integer])
              override def pointerSpace: xsbti.Maybe[String] = xsbti.Maybe.nothing[String]
              override def sourceFile: xsbti.Maybe[java.io.File] = xsbti.Maybe.just(file(error._1))
              override def sourcePath: xsbti.Maybe[String] = xsbti.Maybe.just(error._1)
            }
            override def severity: xsbti.Severity = xsbti.Severity.Error
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
