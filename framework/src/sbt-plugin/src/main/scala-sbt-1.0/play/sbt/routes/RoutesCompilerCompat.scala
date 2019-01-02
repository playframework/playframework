/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt.routes

import play.routes.compiler.RoutesCompiler.GeneratedSource
import sbt._
import xsbti.Position
import java.util.Optional

import scala.collection.mutable
import scala.language.implicitConversions

/**
 * Fix compatibility issues for RoutesCompiler. This is the version compatible with sbt 1.0.
 */
private[routes] trait RoutesCompilerCompat {

  val routesPositionMapper: Position => Option[Position] = position => {
    position.sourceFile.asScala.collect {
      case GeneratedSource(generatedSource) => {
        new xsbti.Position {
          override lazy val line: Optional[Integer] = {
            position.line.asScala
              .flatMap(l => generatedSource.mapLine(l.asInstanceOf[Int]))
              .map(l => l.asInstanceOf[java.lang.Integer])
              .asJava
          }
          override lazy val lineContent: String = {
            line.asScala.flatMap { lineNumber =>
              sourceFile.asScala.flatMap { file =>
                IO.read(file).split('\n').lift(lineNumber - 1)
              }
            }.getOrElse("")
          }
          override val offset: Optional[Integer] = Optional.empty[java.lang.Integer]
          override val pointer: Optional[Integer] = Optional.empty[java.lang.Integer]
          override val pointerSpace: Optional[String] = Optional.empty[String]
          override val sourceFile: Optional[File] = Optional.ofNullable(generatedSource.source.get)
          override val sourcePath: Optional[String] = Optional.ofNullable(sourceFile.get.getCanonicalPath)
          override lazy val toString: String = {
            val sb = new mutable.StringBuilder()

            if (sourcePath.isPresent) sb.append(sourcePath.get)
            if (line.isPresent) sb.append(":").append(line.get)
            if (lineContent.nonEmpty) sb.append("\n").append(lineContent)

            sb.toString()
          }
        }
      }
    }
  }

}
