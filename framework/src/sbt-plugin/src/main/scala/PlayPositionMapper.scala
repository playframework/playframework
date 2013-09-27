package play

import sbt._
import xsbti.Position

/**
 * Maps positions from compile errors on generated sources to positions in the original sources
 */
trait PlayPositionMapper {
  val templatePositionMapper: Position => Option[Position] = position => {
    position.sourceFile collect {
      case play.templates.MaybeGeneratedSource(generatedSource) => {
        new xsbti.Position {
          lazy val line = {
            position.line.map(l => generatedSource.mapLine(l.asInstanceOf[Int])).map(l => xsbti.Maybe.just(l.asInstanceOf[java.lang.Integer])).getOrElse(xsbti.Maybe.nothing[java.lang.Integer])
          }
          lazy val lineContent = {
            line flatMap { lineNo =>
              sourceFile.flatMap { file =>
                IO.read(file).split('\n').lift(lineNo - 1)
              }
            } getOrElse ""
          }
          val offset = xsbti.Maybe.nothing[java.lang.Integer]
          lazy val pointer = {
            position.offset.map { offset =>
              generatedSource.mapPosition(offset.asInstanceOf[Int]) - IO.read(generatedSource.source.get).split('\n').take(position.line.map(l => generatedSource.mapLine(l.asInstanceOf[Int])).get - 1).mkString("\n").size - 1
            }.map { p =>
              xsbti.Maybe.just(p.asInstanceOf[java.lang.Integer])
            }.getOrElse(xsbti.Maybe.nothing[java.lang.Integer])
          }
          lazy val pointerSpace = {
            pointer map { p =>
              ((lineContent: Seq[Char]).take(p).map { case '\t' => '\t'; case x => ' ' }).mkString
            } map { ps => xsbti.Maybe.just(ps) } getOrElse xsbti.Maybe.nothing[String]
          }
          val sourceFile = xsbti.Maybe.just(generatedSource.source.get)
          val sourcePath = xsbti.Maybe.just(sourceFile.get.getCanonicalPath)
        }
      }
    }
  }

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
  val playPositionMappers = Seq(templatePositionMapper, routesPositionMapper)
  /** Single function for mapping positions. For use with reloader and now */
  val playPositionMapper = playPositionMappers.reduceLeft { (m1, m2) =>
    pos: xsbti.Position => m1(pos).orElse(m2(pos))
  }
}
