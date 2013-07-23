package sbt

import sbt.Keys._
import sbt.PlayKeys._
import play.core.SBTLink
import play.console.Colors
import annotation.tailrec
import scala.collection.JavaConverters._

/**
 * Provides mechanisms for running a Play application in SBT
 */
trait PlayRun extends PlayInternalKeys {
  this: PlayReloader =>

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
    (argsTask, state) map { (args, state) =>
      val extracted = Project.extract(state)

      val (properties, httpPort, httpsPort) = filterArgs(args, defaultHttpPort = extracted.get(playDefaultPort))

      require(httpPort.isDefined || httpsPort.isDefined, "You have to specify https.port when http.port is disabled")

      // Set Java properties
      properties.foreach {
        case (key, value) => System.setProperty(key, value)
      }

      println()

      val sbtLoader = this.getClass.getClassLoader
      def commonLoaderEither = Project.runTask(playCommonClassloader, state).get._2.toEither
      val commonLoader = commonLoaderEither.right.toOption.getOrElse {
        state.log.warn("Some of the dependencies were not recompiled properly, so classloader is not available")
        throw commonLoaderEither.left.get
      }
      val maybeNewState = Project.runTask(dependencyClasspath in Compile, state).get._2.toEither.right.map { dependencies =>

        // All jar dependencies. They will not been reloaded and must be part of this top classloader
        val classpath = dependencies.map(_.data.toURI.toURL).filter(_.toString.endsWith(".jar")).toArray

        /**
         * Create a temporary classloader to run the application.
         * This classloader share the minimal set of interface needed for
         * communication between SBT and Play.
         * It also uses the same Scala classLoader as SBT allowing to share any
         * values coming from the Scala library between both.
         */
        lazy val applicationLoader: ClassLoader = new java.net.URLClassLoader(classpath, commonLoader) {

          val sharedClasses = Seq(
            classOf[play.core.SBTLink].getName,
            classOf[play.core.server.ServerWithStop].getName,
            classOf[play.api.UsefulException].getName,
            classOf[play.api.PlayException].getName,
            classOf[play.api.PlayException.InterestingLines].getName,
            classOf[play.api.PlayException.RichDescription].getName,
            classOf[play.api.PlayException.ExceptionSource].getName,
            classOf[play.api.PlayException.ExceptionAttachment].getName)

          override def loadClass(name: String): Class[_] = {
            if (sharedClasses.contains(name)) {
              sbtLoader.loadClass(name)
            } else {
              super.loadClass(name)
            }
          }

          // -- Delegate resource loading. We have to hack here because the default implementation are already recursives.

          override def getResource(name: String): java.net.URL = {
            val findResource = classOf[ClassLoader].getDeclaredMethod("findResource", classOf[String])
            findResource.setAccessible(true)
            val resource = reloader.currentApplicationClassLoader.map(findResource.invoke(_, name).asInstanceOf[java.net.URL]).orNull
            if (resource == null) {
              super.getResource(name)
            } else {
              resource
            }
          }

          override def getResources(name: String): java.util.Enumeration[java.net.URL] = {
            val findResources = classOf[ClassLoader].getDeclaredMethod("findResources", classOf[String])
            findResources.setAccessible(true)
            val resources1 = reloader.currentApplicationClassLoader.map(findResources.invoke(_, name).asInstanceOf[java.util.Enumeration[java.net.URL]]).getOrElse(new java.util.Vector[java.net.URL]().elements)
            val resources2 = super.getResources(name)
            val resources = new java.util.Vector[java.net.URL](
              (resources1.asScala.toList ++ resources2.asScala.toList).distinct.asJava
            )
            resources.elements
          }

          override def toString = {
            "SBT/Play shared ClassLoader, with: " + (getURLs.toSeq) + ", using parent: " + (getParent)
          }

        }

        lazy val reloader = newReloader(state, playReload, applicationLoader)

        val mainClass = applicationLoader.loadClass("play.core.server.NettyServer")
        val server = if (httpPort.isDefined) {
          val mainDev = mainClass.getMethod("mainDevHttpMode", classOf[play.core.SBTLink], classOf[Int])
          mainDev.invoke(null, reloader, httpPort.get: java.lang.Integer).asInstanceOf[play.core.server.ServerWithStop]
        } else {
          val mainDev = mainClass.getMethod("mainDevOnlyHttpsMode", classOf[play.core.SBTLink], classOf[Int])
          mainDev.invoke(null, reloader, httpsPort.get: java.lang.Integer).asInstanceOf[play.core.server.ServerWithStop]

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
        reloader.clean()

        // Notify hooks
        extracted.get(playOnStopped).foreach(_())

        newState
      }

      // Remove Java properties
      properties.foreach {
        case (key, _) => System.clearProperty(key)
      }

      println()

      maybeNewState match {
        case Right(x) => x
        case _ => state
      }
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
