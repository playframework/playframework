package sbt

import play.api._
import play.core._
import Keys._
import PlayExceptions._

trait PlayReloader {
  this: PlayCommands with PlayPositionMapper =>

  // ----- Reloader

  def newReloader(state: State, playReload: TaskKey[sbt.inc.Analysis], baseLoader: ClassLoader) = {

    val extracted = Project.extract(state)

    new SBTLink {

      lazy val projectPath = extracted.currentProject.base

      lazy val watchFiles = extracted.runTask(watchTransitiveSources, state)._2

      // ----- Internal state used for reloading is kept here

      var currentApplicationClassLoader: Option[ClassLoader] = None

      var reloadNextTime = false
      var currentProducts = Map.empty[java.io.File, Long]
      var currentAnalysis = Option.empty[sbt.inc.Analysis]
      
      // --- USING jnotify to detect file change (TODO: Use Java 7 standard API if available)

      lazy val jnotify = { // This create a fully dynamic version of JNotify that support reloading 

        try {

          var _changed = true

          // --

          var jnotifyJarFile = this.getClass.getClassLoader.asInstanceOf[java.net.URLClassLoader].getURLs
            .map(_.getFile)
            .find(_.contains("/jnotify"))
            .map(new File(_))
            .getOrElse(sys.error("Missing JNotify?"))

          val sbtLoader = this.getClass.getClassLoader.getParent.asInstanceOf[java.net.URLClassLoader]
          val method = classOf[java.net.URLClassLoader].getDeclaredMethod("addURL", classOf[java.net.URL])
          method.setAccessible(true)
          method.invoke(sbtLoader, jnotifyJarFile.toURI.toURL)

          val targetDirectory = extracted.get(target)
          val nativeLibrariesDirectory = new File(targetDirectory, "native_libraries")

          if (!nativeLibrariesDirectory.exists) {
            // Unzip native libraries from the jnotify jar to target/native_libraries
            IO.unzip(jnotifyJarFile, targetDirectory, (name: String) => name.startsWith("native_libraries"))
          }

          val libs = new File(nativeLibrariesDirectory, System.getProperty("sun.arch.data.model") + "bits").getAbsolutePath

          // Hack to set java.library.path
          System.setProperty("java.library.path", {
            Option(System.getProperty("java.library.path")).map { existing =>
              existing + java.io.File.pathSeparator + libs
            }.getOrElse(libs)
          })
          import java.lang.reflect._
          val fieldSysPath = classOf[ClassLoader].getDeclaredField("sys_paths")
          fieldSysPath.setAccessible(true)
          fieldSysPath.set(null, null)

          val jnotifyClass = sbtLoader.loadClass("net.contentobjects.jnotify.JNotify")
          val jnotifyListenerClass = sbtLoader.loadClass("net.contentobjects.jnotify.JNotifyListener")
          val addWatchMethod = jnotifyClass.getMethod("addWatch", classOf[String], classOf[Int], classOf[Boolean], jnotifyListenerClass)
          val removeWatchMethod = jnotifyClass.getMethod("removeWatch", classOf[Int])
          val listener = java.lang.reflect.Proxy.newProxyInstance(sbtLoader, Seq(jnotifyListenerClass).toArray, new java.lang.reflect.InvocationHandler {
            def invoke(proxy: AnyRef, m: java.lang.reflect.Method, args: scala.Array[AnyRef]): AnyRef = {
              _changed = true
              null
            }
          })

          val nativeWatcher = new {
            def addWatch(directoryToWatch: String): Int = {
              addWatchMethod.invoke(null, directoryToWatch, 15: java.lang.Integer, true: java.lang.Boolean, listener).asInstanceOf[Int]
            }
            def removeWatch(id: Int): Unit = removeWatchMethod.invoke(null, id.asInstanceOf[AnyRef])
            def reloaded() { _changed = false }
            def changed() { _changed = true }
            def hasChanged = _changed
          }

          ( /* Try it */ nativeWatcher.removeWatch(0) )

          nativeWatcher

        } catch {
          case e: Throwable => {

            println(play.console.Colors.red(
              """|
                 |Cannot load the JNotify native library (%s)
                 |Play will check file changes for each request, so expect degraded reloading performace.
                 |""".format(e.getMessage).stripMargin
            ))

            new {
              def addWatch(directoryToWatch: String): Int = 0
              def removeWatch(id: Int): Unit = ()
              def reloaded(): Unit = ()
              def changed(): Unit = ()
              def hasChanged = true
            }

          }
        }

        
      }

      val watchChanges: Seq[Int] = extracted.runTask(playMonitoredDirectories, state)._2.map(jnotify.addWatch)

      // --- Utils

      def markdownToHtml(markdown: String, pagePath: String) = {
        import org.pegdown._
        import org.pegdown.ast._

        val link:(String => (String, String)) = _ match {
          case link if link.contains("|") => {
            val parts = link.split('|')
            (parts.tail.head, parts.head)
          }
          case image if image.endsWith(".png") => {
            val link = image match {
              case full if full.startsWith("http://") => full
              case absolute if absolute.startsWith("/") => "resources/manual" + absolute
              case relative => "resources/" + pagePath + "/" + relative
            }
            (link, """<img src="""" + link + """"/>""")
          }
          case link => {
            (link, link)
          }
        }

        val processor = new PegDownProcessor(Extensions.ALL)
        val links = new LinkRenderer {
          override def render(node: WikiLinkNode) = {
            val (href, text) = link(node.getText)
            new LinkRenderer.Rendering(href, text)
          }
        }

        processor.markdownToHtml(markdown, links)
      }

      // ---

      def forceReload() {
        reloadNextTime = true
        jnotify.changed()
      }

      def clean() {
        currentApplicationClassLoader = None
        currentProducts = Map.empty[java.io.File, Long]
        currentAnalysis = None
        watchChanges.foreach(jnotify.removeWatch)
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
              play.templates.MaybeGeneratedSource.unapply(source).map { generatedSource =>
                generatedSource.source.get -> Option(maybeLine).map(l => generatedSource.mapLine(l):java.lang.Integer).orNull
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
        mappedPosition.map { pos => new xsbti.Problem {
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

      private def newClassLoader = {
        val loader = new java.net.URLClassLoader(
          Project.runTask(dependencyClasspath in Runtime, state).map(_._2).get.toEither.right.get.map(_.data.toURI.toURL).toArray, baseLoader) {

          val version = classLoaderVersion.incrementAndGet

          override def getResources(name: String): java.util.Enumeration[java.net.URL] = {
            import scala.collection.JavaConverters._
            new java.util.Vector[java.net.URL](
              super.getResources(name).asScala.toList.distinct.asJava
            ).elements
          }

          override def toString = {
            "ReloadableClassLoader(v" + version + ") {" + {
              getURLs.map(_.toString).mkString(", ")
            } + "}"
          }

        }
        currentApplicationClassLoader = Some(loader)
        loader
      }

      def reload: AnyRef = {

        PlayProject.synchronized {

          if (jnotify.hasChanged) {
            jnotify.reloaded()
            Project.runTask(playReload, state).map(_._2).get.toEither
              .left.map { incomplete =>
                jnotify.changed()
                Incomplete.allExceptions(incomplete).headOption.map {
                  case e: PlayException => e
                  case e: xsbti.CompileFailed => {
                    getProblems(incomplete).headOption.map(CompilationException(_)).getOrElse {
                      UnexpectedException(Some("Compilation failed without reporting any problem!?"), Some(e))
                    }
                  }
                  case e: Exception => UnexpectedException(unexpected = Some(e))
                }.getOrElse {
                  UnexpectedException(Some("Compilation task failed without any exception!?"))
                }
              }
              .right.map { compilationResult =>
                updateAnalysis(compilationResult).map { _ =>
                  newClassLoader
                }
              }.fold(
                oops => oops,
                maybeClassloader => maybeClassloader.getOrElse(null)
              )
          } else {
            null
          }

        }

      }

      def runTask(task: String): AnyRef = {
        val parser = Act.scopedKeyParser(state)
        val Right(sk: ScopedKey[Task[_]]) = complete.DefaultParsers.result(parser, task)
        val result = Project.runTask(sk, state).map(_._2)

        result.flatMap(_.toEither.right.toOption).getOrElse(null).asInstanceOf[AnyRef]
      }

    }

  }
}