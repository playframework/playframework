/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.sbt.routes

import play.routes.compiler.RoutesCompiler.GeneratedSource
import sbt._
import xsbti.Position
import java.util.Optional

import scala.language.implicitConversions

/**
 * Fix compatibility issues for RoutesCompiler. This is the version compatible with sbt 1.0.
 */
trait RoutesCompilerCompat {

  private def toScala[T](o: Optional[T]): Option[T] = {
    if (o.isPresent) Option(o.get())
    else None
  }

  private def toJava[T](o: Option[T]): Optional[T] = o match {
    case Some(v) => Optional.ofNullable(v)
    case None => Optional.empty[T]
  }

  val routesPositionMapper: Position => Option[Position] = position => {
    toScala(position.sourceFile).collect {
      case GeneratedSource(generatedSource) => {
        new xsbti.Position {
          override lazy val line: Optional[Integer] = {
            val r = toScala(position.line)
              .flatMap(l => generatedSource.mapLine(l.asInstanceOf[Int]))
              .map(l => l.asInstanceOf[java.lang.Integer])
            toJava(r)
          }
          override lazy val lineContent: String = {
            toScala(line).flatMap { lineNumber =>
              toScala(sourceFile).flatMap { file =>
                IO.read(file).split('\n').lift(lineNumber - 1)
              }
            }.getOrElse("")
          }
          override val offset: Optional[Integer] = Optional.empty[java.lang.Integer]
          override val pointer: Optional[Integer] = Optional.empty[java.lang.Integer]
          override val pointerSpace: Optional[String] = Optional.empty[String]
          override val sourceFile: Optional[File] = Optional.ofNullable(generatedSource.source.get)
          override val sourcePath: Optional[String] = Optional.ofNullable(sourceFile.get.getCanonicalPath)
        }
      }
    }
  }

}
