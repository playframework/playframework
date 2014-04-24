/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play

import sbt._
import xsbti.Position

/**
 * Maps positions from compile errors on generated sources to positions in the original sources
 */
trait PlayPositionMapper {
  val routesPositionMapper: Position => Option[Position] = position => {
    position.sourceFile collect {
      case play.router.RoutesCompiler.MaybeGeneratedSource(generatedSource) => {
        new xsbti.Position {
          lazy val line = {
            position.line.flatMap(l => generatedSource.mapLine(l.asInstanceOf[Int])).map(l => xsbti.Maybe.just(l.asInstanceOf[java.lang.Integer])).getOrElse(xsbti.Maybe.nothing[java.lang.Integer])
          }
          lazy val lineContent = {
            line flatMap { lineNo =>
              sourceFile.flatMap { file =>
                IO.read(file).split('\n').lift(lineNo - 1)
              }
            } getOrElse ""
          }
          val offset = xsbti.Maybe.nothing[java.lang.Integer]
          val pointer = xsbti.Maybe.nothing[java.lang.Integer]
          val pointerSpace = xsbti.Maybe.nothing[String]
          val sourceFile = xsbti.Maybe.just(new File(generatedSource.source.get.path))
          val sourcePath = xsbti.Maybe.just(sourceFile.get.getCanonicalPath)
        }
      }
    }
  }

  /** Sequence of position mappers. For using with SBT 0.13 sourcePositionMappers feature */
  val playPositionMappers = Seq(routesPositionMapper)
  /** Single function for mapping positions. For use with reloader and now */
  val playPositionMapper = routesPositionMapper
}
