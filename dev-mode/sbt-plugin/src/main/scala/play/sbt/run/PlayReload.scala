/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt.run

import java.net.URI
import java.nio.file.Paths
import java.util.Optional

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

import sbt._
import sbt.internal.inc.Analysis
import sbt.internal.Output
import sbt.util.InterfaceUtil.o2jo
import sbt.Keys._

import play.api.PlayException
import play.runsupport.CompileResult
import play.runsupport.CompileResult.CompileFailure
import play.runsupport.CompileResult.CompileSuccess
import play.runsupport.Source
import play.sbt.PlayExceptions.CompilationException
import play.sbt.PlayExceptions.UnexpectedException
import play.twirl.compiler.MaybeGeneratedSource
import xsbti.CompileFailed
import xsbti.Position
import xsbti.Problem
import xsbti.Severity

object PlayReload {
  def taskFailureHandler(
      incomplete: Incomplete,
      streams: Option[Streams],
      state: State,
      scope: Scope
  ): PlayException = {

    def convertSbtVirtualFile(sourcePath: String) =
      new File(if (sourcePath.startsWith("${")) { // check for ${BASE} or similar (in case it changes)
        // Like: ${BASE}/app/controllers/MyController.scala
        sourcePath.substring(sourcePath.indexOf("}") + 2)
      } else {
        // A file outside of the base project folder or using sbt <1.4
        sourcePath
      }).getAbsoluteFile

    // Stolen from https://github.com/sbt/sbt/blob/v1.4.8/main/src/main/scala/sbt/Defaults.scala#L466-L515
    // Slightly modified because fileConverter settings do not exist pre sbt 1.4 yet
    def toAbsoluteSource(pos: Position): Position = {
      val newFile: Option[File] = pos
        .sourcePath()
        .asScala
        .map { path => convertSbtVirtualFile(path) }

      newFile
        .map { file =>
          new Position {
            override def line(): Optional[Integer]        = pos.line()
            override def lineContent(): String            = pos.lineContent()
            override def offset(): Optional[Integer]      = pos.offset()
            override def pointer(): Optional[Integer]     = pos.pointer()
            override def pointerSpace(): Optional[String] = pos.pointerSpace()
            override def sourcePath(): Optional[String]   = Optional.of(file.getAbsolutePath)
            override def sourceFile(): Optional[File]     = Optional.of(file)
            override def startOffset(): Optional[Integer] = pos.startOffset()
            override def endOffset(): Optional[Integer]   = pos.endOffset()
            override def startLine(): Optional[Integer]   = pos.startLine()
            override def startColumn(): Optional[Integer] = pos.startColumn()
            override def endLine(): Optional[Integer]     = pos.endLine()
            override def endColumn(): Optional[Integer]   = pos.endColumn()
          }
        }
        .getOrElse(pos)
    }

    // Stolen from https://github.com/sbt/sbt/blob/v1.4.8/main/src/main/scala/sbt/Defaults.scala#L2299-L2316
    // Slightly modified because reportAbsolutePath and fileConverter settings do not exist pre sbt 1.4 yet
    def foldMappers(mappers: Seq[Position => Option[Position]]) =
      mappers.foldRight { (p: Position) => toAbsoluteSource(p) } { // Fallback if sourcePositionMappers is empty
        (mapper, previousPosition) =>
          { (p: Position) =>
            // To each mapper we pass the position with the absolute source
            mapper(toAbsoluteSource(p)).getOrElse(previousPosition(p))
          }
      }

    Incomplete
      .allExceptions(incomplete)
      .headOption
      .map {
        case e: PlayException => e
        case e: CompileFailed =>
          getProblems(incomplete, streams)
            .find(_.severity == Severity.Error)
            .map(problem =>
              // Starting with sbt 1.4.1, when a compilation error occurs, the Position of a Problem (which is contained within the Incomplete) will no longer refer
              // to the mapped source file (e.g. until sbt 1.4.0 the Position would refer to "conf/routes" when a compilation error actually happened
              // in "target/scala-2.13/routes/main/router/Routes.scala").
              // That's caused by https://github.com/sbt/zinc/pull/931: The file causing the compilation error is not transformed via the sourcePositionMappers config
              // anymore before adding it to "allProblems", the field that eventually gets used by the Incomplete. (The transformation still takes place to show
              // the mapped source file in the logs) Play however needs to know the mapped source file to display it in it's error pages for a nice dev experience.
              // So the solution is that Play itself will try to transform the source file to the mapped file by running it "through" sourcePositionMappers:
              Project
                .runTask(scope / sourcePositionMappers, state)
                .flatMap(_._2.toEither.toOption)
                .map(mappers =>
                  new Problem {
                    override def category(): String           = problem.category()
                    override def severity(): Severity         = problem.severity()
                    override def message(): String            = problem.message()
                    override def position(): Position         = foldMappers(mappers)(problem.position())
                    override def rendered(): Optional[String] = problem.rendered()
                  }
                )
                .getOrElse(problem)
            )
            .map(CompilationException)
            .getOrElse(UnexpectedException(Some("The compilation failed without reporting any problem!"), Some(e)))
        case NonFatal(e) => UnexpectedException(unexpected = Some(e))
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
      streams: () => Option[Streams],
      state: State,
      scope: Scope
  ): CompileResult = {
    val compileResult: Either[Incomplete, CompileSuccess] = for {
      analysis  <- reloadCompile().toEither.right
      classpath <- classpath().toEither.right
    } yield new CompileSuccess(sourceMap(analysis).asJava, classpath.files.asJava)
    compileResult.left.map(inc => new CompileFailure(taskFailureHandler(inc, streams(), state, scope))).merge
  }

  object JFile {
    class FileOption(val anyOpt: Option[Any]) extends AnyVal {
      def isEmpty: Boolean  = !anyOpt.exists(_.isInstanceOf[java.io.File])
      def get: java.io.File = anyOpt.get.asInstanceOf[java.io.File]
    }
    def unapply(any: Option[Any]): FileOption = new FileOption(any)
  }

  object VirtualFile {
    def unapply(value: Option[Any]): Option[Any] =
      value.filter { vf =>
        val name = vf.getClass.getSimpleName
        name == "BasicVirtualFileRef" || name == "MappedVirtualFile"
      }
  }

  def sourceMap(analysis: Analysis): Map[String, Source] = {
    analysis.relations.classes.reverseMap.flatMap {
      case (name, files) =>
        files.headOption match { // This is typically a set containing a single file, so we can use head here.
          case None => Map.empty[String, Source]

          case JFile(file) => // sbt < 1.4
            Map(name -> new Source(file, MaybeGeneratedSource.unapply(file).flatMap(_.source).orNull))

          case VirtualFile(vf) => // sbt 1.4+ virtual file, see #10486
            val names = vf.getClass.getMethod("names").invoke(vf).asInstanceOf[Array[String]]
            val path =
              if (names.head.startsWith("${")) { // check for ${BASE} or similar (in case it changes)
                // It's an relative path, skip the first element (which usually is "${BASE}")
                Paths.get(names.drop(1).head, names.drop(2): _*)
              } else {
                // It's an absolute path, sbt uses them e.g. for subprojects located outside of the base project
                val id = vf.getClass.getMethod("id").invoke(vf).asInstanceOf[String]
                // In Windows the sbt virtual file id does not start with a slash, but absolute paths in Java URIs need that
                val extraSlash = if (id.startsWith("/")) "" else "/"
                val prefix     = "file://" + extraSlash
                // The URI will be like file:///home/user/project/SomeClass.scala (Linux/Mac) or file:///C:/Users/user/project/SomeClass.scala (Windows)
                Paths.get(URI.create(s"$prefix$id"));
              }
            Map(name -> new Source(path.toFile, MaybeGeneratedSource.unapply(path.toFile).flatMap(_.source).orNull))

          case anyOther =>
            throw new RuntimeException(
              s"Can't handle class ${anyOther.getClass.getName} used for sourceMap"
            )
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
