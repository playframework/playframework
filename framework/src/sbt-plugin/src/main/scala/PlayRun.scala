package sbt

import sbt.Keys._
import sbt.PlayKeys._
import play.core.{ SBTLink, SBTDocLink }
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
   * Configuration for dependencies common to all Play applications. Used to build a classloader
   * with classes common to the user's application and the Play docs application. Hidden so that
   * it isn't exposed when the user application is published.
   */
  val SharedApplication = config("shared") hide

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

  val playRunSetting: Project.Initialize[InputTask[Unit]] = inputTask { (argsTask: TaskKey[Seq[String]]) =>
    (
      argsTask, state, playCommonClassloader, managedClasspath in SharedApplication,
      dependencyClasspath in Runtime, managedClasspath in DocsApplication
    ) map { (args, state, commonLoader, sharedAppClasspath, userAppClasspath, docsAppClasspath) =>
        val extracted = Project.extract(state)

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
       * We begin with two classloaders and three classpaths.
       *
       * 1. sbtClassLoader, the classloader of SBT and the Play SBT plugin.
       * 2. commonClassloader, a classloader that persists across calls to run.
       *    This classloader is stored inside the
       *    PlayInternalKeys.playCommonClassloader task. This classloader will
       *    load the classes for the H2 database if it finds them in the user's
       *    classpath. This allows H2's in-memory database state to survive across
       *    calls to run.
       * 3. sharedAppClasspath, the classpath for Play and its dependencies.
       * 4. userAppClasspath, the classpath for the current Play application.
       *    This contains the user app's classes and dependencies. It will also
       *    include Play, so it contain a superset of the classes in
       *    sharedAppClasspath.
       * 5. docsAppClasspath, the classpath for the special play-docs
       *    application that is used to serve documentation when running in
       *    development mode. Again this is a superset of sharedAppClasspath.
       *
       * We construct separate classloaders for the user app
       * (applicationLoader) and for the docs app (docsLoader). They need to be
       * isolated from each other because they might have conflicting
       * dependencies. They both delegate to a classloader that includes the
       * dependencies that are common to both (sharedApplicationLoader).
       *
       * The code is quite confusing, but here's an educated guess at the
       * ordering of class loading for the user app classloader
       * (applicationLoader). Don't trust this too much, but it might be
       * helpful as a start for understanding the code.
       *
       * 1. classes on userAppClasspath that are in directories (not JARs) these
       *    are the classes that will be reloaded if the application code
       *    changes
       * 2. classes persisted across runs in the commonLoader
       * 3. a few whitelisted classes shared between SBT and Play in the sbtClassLoader
       * 4. classes common to Play apps on the sharedAppClasspath
       * 5. classes on the userAppClasspath that are in JARs
       *
       * Someone working on this code in the future might want to tidy things up
       * by splitting some of the custom logic out of the URLClassLoaders and into
       * their own simpler ClassLoader implementations. The curious cycle between
       * applicationLoader and reloader.currentApplicationClassLoader could also
       * use some attention.
       */

        // Get the URLs for the resources in a classpath
        def urls(cp: Keys.Classpath): Array[URL] = cp.map(_.data.toURI.toURL).toArray
        // Support method to merge the output of two calls to ClassLoader.getResources(String) into a single result
        def combineResources(resources1: java.util.Enumeration[URL], resources2: java.util.Enumeration[URL]) =
          new java.util.Vector[java.net.URL]((resources1.asScala ++ resources2.asScala).toSeq.distinct.asJava).elements

        val sbtLoader = this.getClass.getClassLoader

        /**
         * The ClassLoader holding the common Play classes. Loads classes in this order:
         * - classes persisted across runs in the commonLoader
         * - a few whitelisted classes shared between SBT and Play in the sbtClassLoader
         * - classes common to Play apps on the sharedAppClasspath
         */
        val sharedApplicationLoader: ClassLoader = new URLClassLoader(urls(sharedAppClasspath), commonLoader) {

          private val sbtSharedClasses = Seq(
            classOf[play.core.SBTLink].getName,
            classOf[play.core.SBTDocLink].getName,
            classOf[play.core.server.ServerWithStop].getName,
            classOf[play.api.UsefulException].getName,
            classOf[play.api.PlayException].getName,
            classOf[play.api.PlayException.InterestingLines].getName,
            classOf[play.api.PlayException.RichDescription].getName,
            classOf[play.api.PlayException.ExceptionSource].getName,
            classOf[play.api.PlayException.ExceptionAttachment].getName)

          override def findClass(name: String): Class[_] = {
            if (sbtSharedClasses.contains(name)) sbtLoader.loadClass(name) else super.findClass(name)
          }

          override def toString = {
            "SBT/Play shared ClassLoader, with: " + (getURLs.toSeq) + ", using parent: " + (getParent)
          }

        }

        /**
         * The ClassLoader holding the user app's classes. Loads:
         * - classes on userAppClasspath that are in directories (not JARs) these
         *   are the classes that will be reloaded if the application code
         *   changes
         * - classes in sharedApplicationLoader
         * - classes on the userAppClasspath that are in JARs
         *
         * Note that this class delegates to the reloader.currentApplicationClassLoader, but
         * currentApplicationClassLoader is a URLClassLoader that delegates back to this class,
         * forming a cycle. Somehow it seems to work...
         */
        lazy val applicationLoader: ClassLoader = new URLClassLoader(urls(userAppClasspath).filter(_.toString.endsWith(".jar")), sharedApplicationLoader) {
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
            "Play application reloadable ClassLoader, with: " + (getURLs.toSeq) + ", using parent: " + (getParent)
          }

        }
        lazy val reloader = newReloader(state, playReload, applicationLoader)

        // Get a handler for the documentation. The documentation content lives in play/docs/content
        // within the play-docs JAR.
        val docsLoader = new URLClassLoader(urls(docsAppClasspath), sharedApplicationLoader)
        val docsJarFile = {
          val f = docsAppClasspath.map(_.data).filter(_.getName.startsWith("play-docs")).head
          new JarFile(f)
        }
        val sbtDocLink = {
          val docLinkFactoryClass = docsLoader.loadClass("play.docs.SBTDocLinkFactory")
          val factoryMethod = docLinkFactoryClass.getMethod("fromJar", classOf[JarFile], classOf[String])
          factoryMethod.invoke(null, docsJarFile, "play/docs/content").asInstanceOf[SBTDocLink]
        }

        val server = {
          val mainClass = sharedApplicationLoader.loadClass("play.core.server.NettyServer")
          if (httpPort.isDefined) {
            val mainDev = mainClass.getMethod("mainDevHttpMode", classOf[SBTLink], classOf[SBTDocLink], classOf[Int])
            mainDev.invoke(null, reloader, sbtDocLink, httpPort.get: java.lang.Integer).asInstanceOf[play.core.server.ServerWithStop]
          } else {
            val mainDev = mainClass.getMethod("mainDevOnlyHttpsMode", classOf[SBTLink], classOf[SBTDocLink], classOf[Int])
            mainDev.invoke(null, reloader, sbtDocLink, httpsPort.get: java.lang.Integer).asInstanceOf[play.core.server.ServerWithStop]
          }
        }

        // Notify hooks
        extracted.get(playOnStarted).foreach(_(server.mainAddress))

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
            play.Project.synchronized {
              val start = System.currentTimeMillis
              Project.runTask(compile in Compile, newState).get._2.toEither.right.map { _ =>
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
            consoleReader.getTerminal.setEchoEnabled(false)
            try {
              executeContinuously(w, state, reloader, Some(WatchState.empty))
            } finally {
              consoleReader.getTerminal.setEchoEnabled(true)
            }

            // Remove state two first commands added by sbt ~
            state.copy(remainingCommands = state.remainingCommands.drop(2)).remove(Watched.ContinuousState)
          }
          case _ => {
            // run mode
            waitForKey()
            state
          }
        }

        server.stop()
        docsJarFile.close()
        reloader.clean()

        // Notify hooks
        extracted.get(playOnStopped).foreach(_())

        // Remove Java properties
        properties.foreach {
          case (key, _) => System.clearProperty(key)
        }

        println()
      }
  }

  val playStartCommand = Command.args("start", "<port>") { (state: State, args: Seq[String]) =>

    val extracted = Project.extract(state)

    // Parse HTTP port argument
    val (properties, httpPort, httpsPort) = filterArgs(args, defaultHttpPort = extracted.get(playDefaultPort))
    require(httpPort.isDefined || httpsPort.isDefined, "You have to specify https.port when http.port is disabled")

    Project.runTask(compile in Compile, state).get._2.toEither match {
      case Left(_) => {
        println()
        println("Cannot start with errors.")
        println()
        state.fail
      }
      case Right(_) => {

        Project.runTask(dependencyClasspath in Runtime, state).get._2.toEither.right.map { dependencies =>
          //trigger a require build if needed
          Project.runTask(buildRequire, state).get._2

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

          waitForKey()

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
