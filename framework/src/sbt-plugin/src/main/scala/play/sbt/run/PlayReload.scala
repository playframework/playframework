/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.sbt.run

import sbt._
import sbt.Keys._
import play.api.PlayException
import play.sbt.PlayExceptions._
import play.runsupport.Reloader.{ CompileFailure, CompileResult }

object PlayReload extends PlayReloadCompat {

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
}
