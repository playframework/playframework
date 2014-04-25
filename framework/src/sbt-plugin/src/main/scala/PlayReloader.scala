/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play

import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader
import scala.util.control.NonFatal

import play.api._
import play.core._
import play.core.watcher._
import play.PlayImport._
import PlayKeys._
import PlayExceptions._
import sbt._
import sbt.Keys._

trait PlayReloader {
  this: PlayCommands with PlayPositionMapper =>

  // ----- Reloader

  def newReloader(state: State, playReload: TaskKey[sbt.inc.Analysis], createClassLoader: ClassLoaderCreator, classpathTask: TaskKey[Classpath], baseLoader: ClassLoader) = {

    val extracted = Project.extract(state)

    new BuildLink {

      lazy val projectPath = extracted.currentProject.base

      lazy val watchFiles = extracted.runTask(watchTransitiveSources, state)._2

      // ----- Internal state used for reloading is kept here

      var currentApplicationClassLoader: Option[ClassLoader] = None

      var reloadNextTime = false
      var currentProducts = Map.empty[java.io.File, Long]
      var currentAnalysis = Option.empty[sbt.inc.Analysis]

      lazy val watcher = {

        try {

          var jnotifyJarFile: File = this.getClass.getClassLoader.asInstanceOf[URLClassLoader].getURLs
            .map(_.getFile)
            .find(_.contains("/jnotify"))
            .map(new File(_))
            .getOrElse(sys.error("Missing JNotify?"))

          val buildLoader: URLClassLoader = this.getClass.getClassLoader.getParent.asInstanceOf[URLClassLoader]
          val method: Method = classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL])
          method.setAccessible(true)
          method.invoke(buildLoader, jnotifyJarFile.toURI.toURL)

          val targetDirectory = extracted.get(target)
          val nativeLibrariesDirectory = new File(targetDirectory, "native_libraries")

          if (!nativeLibrariesDirectory.exists) {
            // Unzip native libraries from the jnotify jar to target/native_libraries
            IO.unzip(jnotifyJarFile, targetDirectory, (name: String) => name.startsWith("native_libraries"))
          }

          val nativeWatcher: FileSystemWatcher = new FileSystemWatcherFactory(nativeLibrariesDirectory).createWatcher()

          ( /* Try it */ nativeWatcher.removeWatch(0))

          nativeWatcher
        } catch {
          case NonFatal(e) => {

            println(play.sbtplugin.Colors.red(
              """|
                 |Cannot load the JNotify native library (%s)
                 |Play will check file changes for each request, so expect degraded reloading performace.
                 |""".format(e.getMessage).stripMargin
            ))

            FileSystemWatcherFactory.createFakeWatcher()
          }
        }

      }

      val (monitoredFiles, monitoredDirs) = {
        val all = extracted.runTask(playMonitoredFiles, state)._2.map(f => new File(f))
        (all.filter(!_.isDirectory), all.filter(_.isDirectory))
      }

      def calculateTimestamps = monitoredFiles.map(f => f.getAbsolutePath -> f.lastModified).toMap

      var fileTimestamps = calculateTimestamps

      def hasChangedFiles: Boolean = monitoredFiles.exists { f =>
        val fileChanged = fileTimestamps.get(f.getAbsolutePath).map { timestamp =>
          f.lastModified != timestamp
        }.getOrElse {
          state.log.debug("Did not find expected timestamp of file: " + f.getAbsolutePath + " in timestamps. Marking it as changed...")
          true
        }
        if (fileChanged) {
          fileTimestamps = calculateTimestamps //recalulating all, one _or more_ files has changed
        }
        fileChanged
      }

      val watchChanges: Seq[Int] = monitoredDirs.map(f => watcher.addWatch(f.getAbsolutePath))

      lazy val settings = {
        import scala.collection.JavaConverters._
        extracted.get(devSettings).toMap.asJava
      }

      // ---

      def forceReload() {
        reloadNextTime = true
        watcher.changed()
      }

      def clean() {
        currentApplicationClassLoader = None
        currentProducts = Map.empty[java.io.File, Long]
        currentAnalysis = None
        watchChanges.foreach(watcher.removeWatch)
      }

      def updateAnalysis(newAnalysis: sbt.inc.Analysis) = {
        val classFiles = newAnalysis.stamps.allProducts ++ watchFiles
        val newProducts = classFiles.map { classFile =>
          classFile -> classFile.lastModified
        }.toMap
        val updated = if (newProducts != currentProducts || reloadNextTime) {
          Some(newProducts)
        } else {
          None
        }
        updated.foreach(currentProducts = _)
        currentAnalysis = Some(newAnalysis)

        reloadNextTime = false

        updated
      }

      def findSource(className: String, line: java.lang.Integer): Array[java.lang.Object] = {
        val topType = className.split('$').head
        currentAnalysis.flatMap { analysis =>
          analysis.apis.internal.flatMap {
            case (sourceFile, source) => {
              source.api.definitions.find(defined => defined.name == topType).map(_ => {
                sourceFile: java.io.File
              } -> line)
            }
          }.headOption.map {
            case (source, maybeLine) => {
              play.twirl.compiler.MaybeGeneratedSource.unapply(source).map { generatedSource =>
                generatedSource.source.get -> Option(maybeLine).map(l => generatedSource.mapLine(l): java.lang.Integer).orNull
              }.getOrElse(source -> maybeLine)
            }
          }
        }.map {
          case (file, line) => {
            Array[java.lang.Object](file, line)
          }
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
              Output.lastLines(i.node.get.asInstanceOf[ScopedKey[_]], streamsManager).map(_.replace(scala.Console.RESET, "")).map(_.replace(scala.Console.RED, "")).collect {
                case JavacError(file, line, message) => parsed = Some((file, line, message)) -> None
                case JavacErrorInfo(key, message) => parsed._1.foreach { o =>
                  parsed = Some((parsed._1.get._1, parsed._1.get._2, parsed._1.get._3 + " [" + key.trim + ": " + message.trim + "]")) -> None
                }
                case JavacErrorPosition(pos) => {
                  parsed = parsed._1 -> Some(pos.size)
                  if (first == (None, None)) {
                    first = parsed
                  }
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

      private val classLoaderVersion = new java.util.concurrent.atomic.AtomicInteger(0)

      private def taskFailureHandler(incomplete: Incomplete): Exception = {
        watcher.changed()
        Incomplete.allExceptions(incomplete).headOption.map {
          case e: PlayException => e
          case e: xsbti.CompileFailed => {
            getProblems(incomplete)
              .find(_.severity == xsbti.Severity.Error)
              .map(CompilationException)
              .getOrElse(UnexpectedException(Some("The compilation failed without reporting any problem!"), Some(e)))
          }
          case e: Exception => UnexpectedException(unexpected = Some(e))
        }.getOrElse {
          UnexpectedException(Some("The compilation task failed without any exception!"))
        }
      }

      private def newClassLoader: Either[Exception, ClassLoader] = {
        val version = classLoaderVersion.incrementAndGet
        val name = "ReloadableClassLoader(v" + version + ")"
        Project.runTask(classpathTask, state).map(_._2).get.toEither
          .left.map(taskFailureHandler)
          .right.map {
            classpath =>
              val urls = Path.toURLs(classpath.files)
              val loader = createClassLoader(name, urls, baseLoader)
              currentApplicationClassLoader = Some(loader)
              loader
          }
      }

      def reload: AnyRef = {
        play.Play.synchronized {
          if (watcher.hasChanged || hasChangedFiles) {
            watcher.reloaded()
            Project.runTask(playReload, state).map(_._2).get.toEither
              .left.map(taskFailureHandler)
              .right.map {
                compilationResult =>
                  updateAnalysis(compilationResult)
                  newClassLoader.fold(identity, identity)
              }.fold(identity, identity)
          } else {
            null
          }
        }
      }

      def runTask(task: String): AnyRef = {
        val parser = Act.scopedKeyParser(state)
        val Right(sk) = complete.DefaultParsers.result(parser, task)
        val result = Project.runTask(sk.asInstanceOf[Def.ScopedKey[Task[AnyRef]]], state).map(_._2)

        result.flatMap(_.toEither.right.toOption).getOrElse(null)
      }

    }

  }
}
