/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt.run

import java.util.Optional

import scala.util.control.NonFatal

import sbt._
import sbt.Keys._
import sbt.internal.Output

import play.api.PlayException
import play.runsupport.Reloader.CompileFailure
import play.runsupport.Reloader.CompileResult
import play.runsupport.Reloader.CompileSuccess
import play.runsupport.Reloader.Source
import play.sbt.PlayExceptions.CompilationException
import play.sbt.PlayExceptions.UnexpectedException
import java.net.URI
import java.nio.file.Paths

import xsbti.Position
import xsbti.Problem
import xsbti.Severity

/**
 * Fix compatibility issues for PlayReload. This is the version compatible with sbt 1.0.
 */
object PlayReload {
  def originalSource(file: File): Option[File] = {
    play.twirl.compiler.MaybeGeneratedSource.unapply(file).flatMap(_.source)
  }

  def compileFailure(streams: Option[Streams], state: State, scope: Scope)(incomplete: Incomplete): CompileResult = {
    CompileFailure(taskFailureHandler(incomplete, streams, state, scope))
  }

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
        .map { path =>
          convertSbtVirtualFile(path)
        }

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
      mappers.foldRight({ p: Position =>
        toAbsoluteSource(p) // Fallback if sourcePositionMappers is empty
      }) {
        (mapper, previousPosition) =>
          { p: Position =>
            // To each mapper we pass the position with the absolute source
            mapper(toAbsoluteSource(p)).getOrElse(previousPosition(p))
          }
      }

    Incomplete
      .allExceptions(incomplete)
      .headOption
      .map {
        case e: PlayException => e
        case e: xsbti.CompileFailed =>
          getProblems(incomplete, streams)
            .find(_.severity == xsbti.Severity.Error)
            .map(problem =>
              // Starting with sbt 1.4.1, when a compilation error occurs, the Position of a Problem (which is contained within the Incomplete) will no longer refer
              // to the mapped source file (e.g. until sbt 1.4.0 the Position would refer to "conf/routes" when a compilation error actually happened
              // in "target/scala-2.13/routes/main/router/Routes.scala").
              // That's caused by https://github.com/sbt/zinc/pull/931: The file causing the compilation error is not transformed via the sourcePositionMappers config
              // anymore before adding it to "allProblems", the field that eventually gets used by the Incomplete. (The transformation still takes place to show
              // the mapped source file in the logs) Play however needs to know the mapped source file to display it in it's error pages for a nice dev experience.
              // So the solution is that Play itself will try to transform the source file to the mapped file by running it "through" sourcePositionMappers:
              Project
                .runTask(sourcePositionMappers in scope, state)
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
      reloadCompile: () => Result[sbt.internal.inc.Analysis],
      classpath: () => Result[Classpath],
      streams: () => Option[Streams],
      state: State,
      scope: Scope
  ): CompileResult = {
    val compileResult: Either[Incomplete, CompileSuccess] = for {
      analysis  <- reloadCompile().toEither.right
      classpath <- classpath().toEither.right
    } yield CompileSuccess(sourceMap(analysis), classpath.files)
    compileResult.left.map(compileFailure(streams(), state, scope)).merge
  }

  object JFile {
    class FileOption(val anyValue: Any) extends AnyVal {
      def isEmpty: Boolean  = !anyValue.isInstanceOf[java.io.File]
      def get: java.io.File = anyValue.asInstanceOf[java.io.File]
    }
    def unapply(any: Any): FileOption = new FileOption(any)
  }

  object VirtualFile {
    class VirtualFileOption(val anyValue: Any) extends AnyVal {
      def isEmpty: Boolean =
        anyValue.getClass.getSimpleName != "BasicVirtualFileRef" && anyValue.getClass.getSimpleName != "MappedVirtualFile"
      def get: Any = anyValue
    }
    def unapply(any: Any): VirtualFileOption = new VirtualFileOption(any)
  }

  def sourceMap(analysis: sbt.internal.inc.Analysis): Map[String, Source] = {
    analysis.relations.classes.reverseMap
      .mapValues { files =>
        files.head
          .asInstanceOf[Any] match { // This is typically a set containing a single file, so we can use head here.
          case JFile(file) => Source(file, originalSource(file)) // sbt < 1.4

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
            Source(path.toFile, originalSource(path.toFile))

          case anyOther =>
            throw new RuntimeException(
              s"Can't handle class ${anyOther.getClass.getName} used for sourceMap"
            )
        }
      }
  }

  def getProblems(incomplete: Incomplete, streams: Option[Streams]): Seq[xsbti.Problem] = {
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
                  parsed._1.foreach { o =>
                    parsed = Some(
                      (
                        parsed._1.get._1,
                        parsed._1.get._2,
                        parsed._1.get._3 + " [" + key.trim + ": " + message.trim + "]"
                      )
                    ) -> None
                  }
                case JavacErrorPosition(pos) =>
                  parsed = parsed._1 -> Some(pos.size)
                  if (first == ((None, None))) {
                    first = parsed
                  }
              }
            first
          }
          .collect {
            case (Some(error), maybePosition) =>
              new xsbti.Problem {
                def message  = error._3
                def category = ""
                def position = new xsbti.Position {
                  def line        = java.util.Optional.ofNullable(error._2.toInt)
                  def lineContent = ""
                  def offset      = java.util.Optional.empty[java.lang.Integer]
                  def pointer =
                    maybePosition
                      .map(pos => java.util.Optional.ofNullable((pos - 1).asInstanceOf[java.lang.Integer]))
                      .getOrElse(java.util.Optional.empty[java.lang.Integer])
                  def pointerSpace = java.util.Optional.empty[String]
                  def sourceFile   = java.util.Optional.ofNullable(file(error._1))
                  def sourcePath   = java.util.Optional.ofNullable(error._1)
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
    es.flatMap {
      case cf: xsbti.CompileFailed => cf.problems
      case _                       => Nil
    }
  }
}
