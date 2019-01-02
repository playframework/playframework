/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt.routes

import play.routes.compiler.RoutesCompiler.GeneratedSource
import sbt._
import xsbti.{ Maybe, Position }

import scala.language.implicitConversions

/**
 * Fix compatibility issues for RoutesCompiler. This is the version compatible with sbt 0.13.
 */
private[routes] trait RoutesCompilerCompat {

  val routesPositionMapper: Position => Option[Position] = position => {
    position.sourceFile collect {
      case GeneratedSource(generatedSource) => {
        new xsbti.Position {
          override lazy val line: Maybe[Integer] = {
            position.line
              .flatMap(l => generatedSource.mapLine(l.asInstanceOf[Int]))
              .map(l => Maybe.just(l.asInstanceOf[java.lang.Integer]))
              .getOrElse(Maybe.nothing[java.lang.Integer])
          }
          override lazy val lineContent: String = {
            line flatMap { lineNo =>
              sourceFile.flatMap { file =>
                IO.read(file).split('\n').lift(lineNo - 1)
              }
            } getOrElse ""
          }
          override val offset: Maybe[Integer] = Maybe.nothing[java.lang.Integer]
          override val pointer: Maybe[Integer] = Maybe.nothing[java.lang.Integer]
          override val pointerSpace: Maybe[String] = Maybe.nothing[String]
          override val sourceFile: Maybe[File] = Maybe.just(generatedSource.source.get)
          override val sourcePath: Maybe[String] = Maybe.just(sourceFile.get.getCanonicalPath)
        }
      }
    }
  }

}
