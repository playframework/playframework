package play.sbt.run

import play.runsupport.Reloader.{ CompileResult, CompileSuccess, Source }
import play.sbt.run.PlayReload.{ compileFailure, getScopedKey, originalSource }
import sbt.Keys.{ Classpath, Streams }
import sbt.{ Incomplete, Output, Result, file }

/**
 * Fix compatibility issues for PlayReload. This is the version compatible with sbt 0.13.
 */
trait PlayReloadCompat {

  def compile(reloadCompile: () => Result[sbt.inc.Analysis], classpath: () => Result[Classpath], streams: () => Option[Streams]): CompileResult = {
    reloadCompile().toEither
      .left.map(compileFailure(streams()))
      .right.map { analysis =>
        classpath().toEither
          .left.map(compileFailure(streams()))
          .right.map { classpath =>
            CompileSuccess(sourceMap(analysis), classpath.files)
          }.fold(identity, identity)
      }.fold(identity, identity)
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
              def line = xsbti.Maybe.just(error._2.toInt)
              def lineContent = ""
              def offset = xsbti.Maybe.nothing[java.lang.Integer]
              def pointer = maybePosition.map(pos => xsbti.Maybe.just((pos - 1).asInstanceOf[java.lang.Integer])).getOrElse(xsbti.Maybe.nothing[java.lang.Integer])
              def pointerSpace = xsbti.Maybe.nothing[String]
              def sourceFile = xsbti.Maybe.just(file(error._1))
              def sourcePath = xsbti.Maybe.just(error._1)
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
