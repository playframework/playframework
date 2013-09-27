package play

import sbt.{ Project => SbtProject, _ }
import sbt.Keys._
import Keys._
import play.core.{ SBTLink, SBTDocHandler }
import play.console.Colors
import annotation.tailrec
import scala.collection.JavaConverters._
import java.net.URLClassLoader
import java.util.jar.JarFile

/**
 * Provides mechanisms for running a Play application in SBT
 */
trait PlayRun extends PlayInternalKeys {
  this: PlayReloader =>

  /**
   * Configuration for the Play docs application's dependencies. Used to build a classloader for
   * that application. Hidden so that it isn't exposed when the user application is published.
   */
  val DocsApplication = config("docs") hide

  // For some reason, jline disables echo when it creates a new console reader.
  // When we use the reader, we also enabled echo after using it, so as long as this is lazy, and that holds true,
  // then we won't exit SBT with echo disabled.
  private lazy val consoleReader = new jline.console.ConsoleReader

  private def waitForKey() = {
    def waitEOF() {
      consoleReader.readCharacter() match {
        case 4 => // STOP
        case 11 => consoleReader.clearScreen(); waitEOF()
        case 10 => println(); waitEOF()
        case _ => waitEOF()
      }

    }
    consoleReader.getTerminal.setEchoEnabled(false)
    try {
      waitEOF()
    } finally {
      consoleReader.getTerminal.setEchoEnabled(true)
    }
  }

  private def parsePort(portString: String): Int = {
    try {
      Integer.parseInt(portString)
    } catch {
      case e: NumberFormatException => sys.error("Invalid port argument: " + portString)
    }
  }

  private def filterArgs(args: Seq[String], defaultHttpPort: Int): (Seq[(String, String)], Option[Int], Option[Int]) = {
    val (properties, others) = args.span(_.startsWith("-D"))

    val javaProperties = properties.map(_.drop(2).split('=')).map(a => a(0) -> a(1)).toSeq

    // collect arguments plus config file property if present
    val httpPort = Option(System.getProperty("http.port"))
    val httpsPort = Option(System.getProperty("https.port"))

    //port can be defined as a numeric argument or as disabled, -Dhttp.port argument or a generic sys property
    val maybePort = others.headOption.orElse(javaProperties.toMap.get("http.port")).orElse(httpPort)
    val maybeHttpsPort = javaProperties.toMap.get("https.port").orElse(httpsPort).map(parsePort)
    if (maybePort.exists(_ == "disabled")) (javaProperties, Option.empty[Int], maybeHttpsPort)
    else (javaProperties, (maybePort.map(parsePort)).orElse(Some(defaultHttpPort)), maybeHttpsPort)
  }

  val createURLClassLoader: ClassLoaderCreator = (name, urls, parent) => new java.net.URLClassLoader(urls, parent) {
    override def toString = name + "{" + getURLs.map(_.toString).mkString(", ") + "}"
  }

  val createDelegatedResourcesClassLoader: ClassLoaderCreator = (name, urls, parent) => new java.net.URLClassLoader(urls, parent) {
    require(parent ne null)
    override def getResources(name: String): java.util.Enumeration[java.net.URL] = getParent.getResources(name)
    override def toString = name + "{" + getURLs.map(_.toString).mkString(", ") + "}"
  }

  val playRunSetting: SbtProject.Initialize[InputTask[Unit]] = playRunTask(playRunHooks, playDependencyClasspath, playDependencyClassLoader, playReloaderClasspath, playReloaderClassLoader)

  def playRunTask(
    runHooks: TaskKey[Seq[play.PlayRunHook]],
    dependencyClasspath: TaskKey[Classpath], dependencyClassLoader: TaskKey[ClassLoaderCreator],
    reloaderClasspath: TaskKey[Classpath], reloaderClassLoader: TaskKey[ClassLoaderCreator]): SbtProject.Initialize[InputTask[Unit]] = inputTask { (argsTask: TaskKey[Seq[String]]) =>
    (
      argsTask, state, playCommonClassloader, managedClasspath in DocsApplication,
      dependencyClasspath, dependencyClassLoader, reloaderClassLoader
    ) map { (args, state, commonLoader, docsAppClasspath, appDependencyClasspath, createClassLoader, createReloader) =>
        val extracted = SbtProject.extract(state)

        val (_, hooks) = extracted.runTask(runHooks, state)
        val interaction = extracted.get(playInteractionMode)

        val (properties, httpPort, httpsPort) = filterArgs(args, defaultHttpPort = extracted.get(playDefaultPort))

        require(httpPort.isDefined || httpsPort.isDefined, "You have to specify https.port when http.port is disabled")

        // Set Java properties
        properties.foreach {
          case (key, value) => System.setProperty(key, value)
        }

        println()

        /*
       * We need to do a bit of classloader magic to run the Play application.
       *
       * There are six classloaders:
       *
       * 1. sbtLoader, the classloader of sbt and the Play sbt plugin.
       * 2. commonLoader, a classloader that persists across calls to run.
       *    This classloader is stored inside the
       *    PlayInternalKeys.playCommonClassloader task. This classloader will
       *    load the classes for the H2 database if it finds them in the user's
       *    classpath. This allows H2's in-memory database state to survive across
       *    calls to run.
       * 3. delegatingLoader, a special classloader that overrides class loading
       *    to delegate shared classes for sbt link to the sbtLoader, and accesses
       *    the reloader.currentApplicationClassLoader for resource loading to
       *    make user resources available to dependency classes.
       *    Has the commonLoader as its parent.
       * 4. applicationLoader, contains the application dependencies. Has the
       *    delegatingLoader as its parent. Classes from the commonLoader and
       *    the delegatingLoader are checked for loading first.
       * 5. docsLoader, the classloader for the special play-docs application
       *    that is used to serve documentation when running in development mode.
       *    Has the applicationLoader as its parent for Play dependencies and
       *    delegation to the shared sbt doc link classes.
       * 6. reloader.currentApplicationClassLoader, contains the user classes
       *    and resources. Has applicationLoader as its parent, where the
       *    application dependencies are found, and which will delegate through
       *    to the sbtLoader via the delegatingLoader for the shared link.
       *    Resources are actually loaded by the delegatingLoader, where they
       *    are available to both the reloader and the applicationLoader.
       *    This classloader is recreated on reload. See PlayReloader.
       *
       * Someone working on this code in the future might want to tidy things up
       * by splitting some of the custom logic out of the URLClassLoaders and into
       * their own simpler ClassLoader implementations. The curious cycle between
       * applicationLoader and reloader.currentApplicationClassLoader could also
       * use some attention.
       */

        // Get the URLs for the resources in a classpath
        def urls(cp: Classpath): Array[URL] = cp.map(_.data.toURI.toURL).toArray
        // Support method to merge the output of two calls to ClassLoader.getResources(String) into a single result
        def combineResources(resources1: java.util.Enumeration[URL], resources2: java.util.Enumeration[URL]) =
          new java.util.Vector[java.net.URL]((resources1.asScala ++ resources2.asScala).toSeq.distinct.asJava).elements

        val sbtLoader = this.getClass.getClassLoader

        /**
         * ClassLoader that delegates loading of shared sbt link classes to the
         * sbtLoader. Also accesses the reloader resources to make these available
         * to the applicationLoader, creating a full circle for resource loading.
         */
        lazy val delegatingLoader: ClassLoader = new ClassLoader(commonLoader) {

          private val sbtSharedClasses = Seq(
            classOf[play.core.SBTLink].getName,
            classOf[play.core.SBTDocHandler].getName,
            classOf[play.core.server.ServerWithStop].getName,
            classOf[play.api.UsefulException].getName,
            classOf[play.api.PlayException].getName,
            classOf[play.api.PlayException.InterestingLines].getName,
            classOf[play.api.PlayException.RichDescription].getName,
            classOf[play.api.PlayException.ExceptionSource].getName,
            classOf[play.api.PlayException.ExceptionAttachment].getName)

          override def loadClass(name: String, resolve: Boolean): Class[_] = {
            if (sbtSharedClasses.contains(name)) {
              sbtLoader.loadClass(name)
            } else {
              super.loadClass(name, resolve)
            }
          }

          // -- Delegate resource loading. We have to hack here because the default implementation is already recursive.
          private val findResource = classOf[ClassLoader].getDeclaredMethod("findResource", classOf[String])
          findResource.setAccessible(true)

          override def getResource(name: String): java.net.URL = {
            val resource = reloader.currentApplicationClassLoader.map(findResource.invoke(_, name).asInstanceOf[java.net.URL]).orNull
            if (resource == null) {
              super.getResource(name)
            } else {
              resource
            }
          }

          private val findResources = classOf[ClassLoader].getDeclaredMethod("findResources", classOf[String])
          findResources.setAccessible(true)

          override def getResources(name: String): java.util.Enumeration[java.net.URL] = {
            val resources1 = reloader.currentApplicationClassLoader.map(findResources.invoke(_, name).asInstanceOf[java.util.Enumeration[java.net.URL]]).getOrElse(new java.util.Vector[java.net.URL]().elements)
            val resources2 = super.getResources(name)
            combineResources(resources1, resources2)
          }

          override def toString = {
            "DelegatingClassLoader, using parent: " + (getParent)
          }

        }

        lazy val applicationLoader = createClassLoader("PlayDependencyClassLoader", urls(appDependencyClasspath), delegatingLoader)

        lazy val reloader = newReloader(state, playReload, createReloader, reloaderClasspath, applicationLoader)

        // Now we're about to start, let's call the hooks:
        hooks.run(_.beforeStarted())

        // Get a handler for the documentation. The documentation content lives in play/docs/content
        // within the play-docs JAR.
        val docsLoader = new URLClassLoader(urls(docsAppClasspath), applicationLoader)
        val docsJarFile = {
          val f = docsAppClasspath.map(_.data).filter(_.getName.startsWith("play-docs")).head
          new JarFile(f)
        }
        val sbtDocHandler = {
          val docHandlerFactoryClass = docsLoader.loadClass("play.docs.SBTDocHandlerFactory")
          val factoryMethod = docHandlerFactoryClass.getMethod("fromJar", classOf[JarFile], classOf[String])
          factoryMethod.invoke(null, docsJarFile, "play/docs/content").asInstanceOf[SBTDocHandler]
        }

        val server = {
          val mainClass = applicationLoader.loadClass("play.core.server.NettyServer")
          if (httpPort.isDefined) {
            val mainDev = mainClass.getMethod("mainDevHttpMode", classOf[SBTLink], classOf[SBTDocHandler], classOf[Int])
            mainDev.invoke(null, reloader, sbtDocHandler, httpPort.get: java.lang.Integer).asInstanceOf[play.core.server.ServerWithStop]
          } else {
            val mainDev = mainClass.getMethod("mainDevOnlyHttpsMode", classOf[SBTLink], classOf[SBTDocHandler], classOf[Int])
            mainDev.invoke(null, reloader, sbtDocHandler, httpsPort.get: java.lang.Integer).asInstanceOf[play.core.server.ServerWithStop]
          }
        }

        // Notify hooks
        hooks.run(_.afterStarted(server.mainAddress))

        println()
        println(Colors.green("(Server started, use Ctrl+D to stop and go back to the console...)"))
        println()

        val ContinuousState = AttributeKey[WatchState]("watch state", "Internal: tracks state for continuous execution.")
        def isEOF(c: Int): Boolean = c == 4

        @tailrec def executeContinuously(watched: Watched, s: State, reloader: SBTLink, ws: Option[WatchState] = None): Option[String] = {
          @tailrec def shouldTerminate: Boolean = (System.in.available > 0) && (isEOF(System.in.read()) || shouldTerminate)

          val sourcesFinder = PathFinder { watched watchPaths s }
          val watchState = ws.getOrElse(s get ContinuousState getOrElse WatchState.empty)

          val (triggered, newWatchState, newState) =
            try {
              val (triggered, newWatchState) = SourceModificationWatch.watch(sourcesFinder, watched.pollInterval, watchState)(shouldTerminate)
              (triggered, newWatchState, s)
            } catch {
              case e: Exception =>
                val log = s.log
                log.error("Error occurred obtaining files to watch.  Terminating continuous execution...")
                (false, watchState, s.fail)
            }

          if (triggered) {
            //Then launch compile
            Project.synchronized {
              val start = System.currentTimeMillis
              SbtProject.runTask(compile in Compile, newState).get._2.toEither.right.map { _ =>
                val duration = System.currentTimeMillis - start
                val formatted = duration match {
                  case ms if ms < 1000 => ms + "ms"
                  case s => (s / 1000) + "s"
                }
                println("[" + Colors.green("success") + "] Compiled in " + formatted)
              }
            }

            // Avoid launching too much compilation
            Thread.sleep(Watched.PollDelayMillis)

            // Call back myself
            executeContinuously(watched, newState, reloader, Some(newWatchState))
          } else {
            // Stop
            Some("Okay, i'm done")
          }
        }

        // If we have both Watched.Configuration and Watched.ContinuousState
        // attributes and if Watched.ContinuousState.count is 1 then we assume
        // we're in ~ run mode
        val maybeContinuous = state.get(Watched.Configuration).map { w =>
          state.get(Watched.ContinuousState).map { ws =>
            (ws.count == 1, w, ws)
          }.getOrElse((false, None, None))
        }.getOrElse((false, None, None))

        val newState = maybeContinuous match {
          case (true, w: sbt.Watched, ws) => {
            // ~ run mode
            interaction doWithoutEcho {
              executeContinuously(w, state, reloader, Some(WatchState.empty))
            }

            // Remove state two first commands added by sbt ~
            state.copy(remainingCommands = state.remainingCommands.drop(2)).remove(Watched.ContinuousState)
          }
          case _ => {
            // run mode
            interaction.waitForCancel()
            state
          }
        }

        server.stop()
        docsJarFile.close()
        reloader.clean()

        // Notify hooks
        hooks.run(_.afterStopped())

        // Remove Java properties
        properties.foreach {
          case (key, _) => System.clearProperty(key)
        }

        println()
      }
  }

  val playStartCommand = Command.args("start", "<port>") { (state: State, args: Seq[String]) =>

    val extracted = SbtProject.extract(state)

    val interaction = extracted.get(playInteractionMode)
    // Parse HTTP port argument
    val (properties, httpPort, httpsPort) = filterArgs(args, defaultHttpPort = extracted.get(playDefaultPort))
    require(httpPort.isDefined || httpsPort.isDefined, "You have to specify https.port when http.port is disabled")

    SbtProject.runTask(compile in Compile, state).get._2.toEither match {
      case Left(_) => {
        println()
        println("Cannot start with errors.")
        println()
        state.fail
      }
      case Right(_) => {

        SbtProject.runTask(dependencyClasspath in Runtime, state).get._2.toEither.right.map { dependencies =>
          //trigger a require build if needed
          SbtProject.runTask(buildRequire, state).get._2

          val classpath = dependencies.map(_.data).map(_.getCanonicalPath).reduceLeft(_ + java.io.File.pathSeparator + _)

          import java.lang.{ ProcessBuilder => JProcessBuilder }
          val builder = new JProcessBuilder(Seq(
            "java") ++ (properties ++ System.getProperties.asScala).map { case (key, value) => "-D" + key + "=" + value } ++ Seq("-Dhttp.port=" + httpPort.getOrElse("disabled"), "-cp", classpath, "play.core.server.NettyServer", extracted.currentProject.base.getCanonicalPath): _*)

          new Thread {
            override def run {
              System.exit(Process(builder) !)
            }
          }.start()

          println(Colors.green(
            """|
              |(Starting server. Type Ctrl+D to exit logs, the server will remain in background)
              |""".stripMargin))

          interaction.waitForCancel()

          println()

          state.copy(remainingCommands = Seq.empty)

        }.right.getOrElse {
          println()
          println("Oops, cannot start the server?")
          println()
          state.fail
        }

      }
    }

  }

}
