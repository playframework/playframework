/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play

import play.runsupport.PlayWatchService

import play.api._
import play.core._
import sbt._
import sbt.Keys._
import play.PlayImport._
import PlayKeys._
import play.runsupport.PlayExceptions._

trait PlayReloader {
  this: PlayCommands with PlayPositionMapper =>

  /**
   * An extension of BuildLink to provide internal methods to PlayRun, such as the ability to get the classloader,
   * and the ability to close it (stop watching files).
   */
  trait PlayBuildLink extends BuildLink {
    def close()
    def getClassLoader: Option[ClassLoader]
  }

  /**
   * Create a new reloader
   */
  def newReloader(state: State,
    playReload: TaskKey[sbt.inc.Analysis],
    createClassLoader: ClassLoaderCreator,
    classpathTask: TaskKey[Classpath],
    baseLoader: ClassLoader,
    monitoredFiles: Seq[String],
    playWatchService: PlayWatchService): PlayBuildLink = {

    val extracted = Project.extract(state)

    new PlayBuildLink {

      lazy val projectPath = extracted.currentProject.base

      // The current classloader for the application
      @volatile private var currentApplicationClassLoader: Option[ClassLoader] = None
      // Flag to force a reload on the next request.
      // This is set if a compile error occurs, and also by the forceReload method on BuildLink, which is called for
      // example when evolutions have been applied.
      @volatile private var forceReloadNextTime = false
      // Whether any source files have changed since the last request.
      @volatile private var changed = false
      // The last successful compile results. Used for rendering nice errors.
      @volatile private var currentAnalysis = Option.empty[sbt.inc.Analysis]
      // A watch state for the classpath. Used to determine whether anything on the classpath has changed as a result
      // of compilation, and therefore a new classloader is needed and the app needs to be reloaded.
      @volatile private var watchState: WatchState = WatchState.empty

      // Create the watcher, updates the changed boolean when a file has changed.
      private val watcher = playWatchService.watch(monitoredFiles.map(new File(_)), () => {
        changed = true
      })
      private val classLoaderVersion = new java.util.concurrent.atomic.AtomicInteger(0)

      /**
       * Contrary to its name, this doesn't necessarily reload the app.  It is invoked on every request, and will only
       * trigger a reload of the app if something has changed.
       *
       * Since this communicates across classloaders, it must return only simple objects.
       *
       *
       * @return Either
       * - Throwable - If something went wrong (eg, a compile error).
       * - ClassLoader - If the classloader has changed, and the application should be reloaded.
       * - null - If nothing changed.
       */
      def reload: AnyRef = {
        play.Play.synchronized {
          if (changed || forceReloadNextTime || currentAnalysis.isEmpty
            || currentApplicationClassLoader.isEmpty) {

            val shouldReload = forceReloadNextTime

            changed = false
            forceReloadNextTime = false

            // Run the reload task, which will trigger everything to compile
            Project.runTask(playReload, state).map(_._2).get.toEither
              .left.map(taskFailureHandler)
              .right.map { compilationResult =>

                currentAnalysis = Some(compilationResult)

                // Calculate the classpath
                Project.runTask(classpathTask, state).map(_._2).get.toEither
                  .left.map(taskFailureHandler)
                  .right.map { classpath =>

                    // We only want to reload if the classpath has changed.  Assets don't live on the classpath, so
                    // they won't trigger a reload.
                    // Use the SBT watch service, passing true as the termination to force it to break after one check
                    val (_, newState) = SourceModificationWatch.watch(classpath.files.***, 0, watchState)(true)
                    // SBT has a quiet wait period, if that's set to true, sources were modified
                    val triggered = newState.awaitingQuietPeriod
                    watchState = newState

                    if (triggered || shouldReload || currentApplicationClassLoader.isEmpty) {

                      // Create a new classloader
                      val version = classLoaderVersion.incrementAndGet
                      val name = "ReloadableClassLoader(v" + version + ")"
                      val urls = Path.toURLs(classpath.files)
                      val loader = createClassLoader(name, urls, baseLoader)
                      currentApplicationClassLoader = Some(loader)
                      loader
                    } else {
                      null // null means nothing changed
                    }
                  }.fold(identity, identity)
              }.fold(identity, identity)
          } else {
            null // null means nothing changed
          }
        }
      }

      lazy val settings = {
        import scala.collection.JavaConverters._
        extracted.get(devSettings).toMap.asJava
      }

      def forceReload() {
        forceReloadNextTime = true
      }

      def findSource(className: String, line: java.lang.Integer): Array[java.lang.Object] = {
        val topType = className.split('$').head
        currentAnalysis.flatMap { analysis =>
          analysis.apis.internal.flatMap {
            case (sourceFile, source) =>
              source.api.definitions.find(defined => defined.name == topType).map(_ => {
                sourceFile: java.io.File
              } -> line)
          }.headOption.map {
            case (source, maybeLine) =>
              play.twirl.compiler.MaybeGeneratedSource.unapply(source).map { generatedSource =>
                generatedSource.source.get -> Option(maybeLine).map(l => generatedSource.mapLine(l): java.lang.Integer).orNull
              }.getOrElse(source -> maybeLine)
          }
        }.map {
          case (file, l) =>
            Array[java.lang.Object](file, l)
        }.orNull
      }

      def remapProblemForGeneratedSources(problem: xsbti.Problem) = {
        val mappedPosition = playPositionMapper(problem.position)
        mappedPosition.map { pos =>
          new xsbti.Problem {
            def message = problem.message
            def category = ""
            def position = pos
            def severity = problem.severity
          }
        } getOrElse problem
      }

      private def allProblems(inc: Incomplete): Seq[xsbti.Problem] = {
        allProblems(inc :: Nil)
      }

      private def allProblems(incs: Seq[Incomplete]): Seq[xsbti.Problem] = {
        problems(Incomplete.allExceptions(incs).toSeq)
      }

      private def problems(es: Seq[Throwable]): Seq[xsbti.Problem] = {
        es flatMap {
          case cf: xsbti.CompileFailed => cf.problems
          case _ => Nil
        }
      }

      def getProblems(incomplete: Incomplete): Seq[xsbti.Problem] = {
        (allProblems(incomplete) ++ {
          Incomplete.linearize(incomplete).filter(i => i.node.isDefined && i.node.get.isInstanceOf[ScopedKey[_]]).flatMap { i =>
            val JavacError = """\[error\]\s*(.*[.]java):(\d+):\s*(.*)""".r
            val JavacErrorInfo = """\[error\]\s*([a-z ]+):(.*)""".r
            val JavacErrorPosition = """\[error\](\s*)\^\s*""".r

            Project.runTask(streamsManager, state).map(_._2).get.toEither.right.toOption.map { streamsManager =>
              var first: (Option[(String, String, String)], Option[Int]) = (None, None)
              var parsed: (Option[(String, String, String)], Option[Int]) = (None, None)
              Output.lastLines(i.node.get.asInstanceOf[ScopedKey[_]], streamsManager, None).map(_.replace(scala.Console.RESET, "")).map(_.replace(scala.Console.RED, "")).collect {
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
        }).map(remapProblemForGeneratedSources)
      }

      private def taskFailureHandler(incomplete: Incomplete): Exception = {
        // We force reload next time because compilation failed this time
        forceReloadNextTime = true
        Incomplete.allExceptions(incomplete).headOption.map {
          case e: PlayException => e
          case e: xsbti.CompileFailed =>
            print(s"HRM: ERROR!!!! --> $e")
            getProblems(incomplete)
              .find(_.severity == xsbti.Severity.Error)
              .map(CompilationException)
              .getOrElse(UnexpectedException(Some("The compilation failed without reporting any problem!"), Some(e)))
          case e: Exception => UnexpectedException(unexpected = Some(e))
        }.getOrElse {
          UnexpectedException(Some("The compilation task failed without any exception!"))
        }
      }

      def runTask(task: String): AnyRef = {
        val parser = Act.scopedKeyParser(state)
        val Right(sk) = complete.DefaultParsers.result(parser, task)
        val result = Project.runTask(sk.asInstanceOf[Def.ScopedKey[Task[AnyRef]]], state).map(_._2)

        result.flatMap(_.toEither.right.toOption).orNull
      }

      def close() = {
        currentApplicationClassLoader = None
        currentAnalysis = None
        watcher.stop()
      }

      def isForked(): Boolean = false

      def getClassLoader = currentApplicationClassLoader
    }

  }
}
