/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play

import java.io.Closeable

import com.typesafe.sbt.web.SbtWeb
import sbt._
import Keys._
import play.PlayImport._
import PlayKeys._
import play.sbtplugin.Colors
import play.core.{ BuildLink, BuildDocHandler }
import play.core.classloader._
import annotation.tailrec
import scala.collection.JavaConverters._
import java.net.URLClassLoader
import java.util.jar.JarFile
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import play.runsupport.{ PlayWatchService, AssetsClassLoader }
import play.sbtplugin.run._

/**
 * Provides mechanisms for running a Play application in SBT
 */
trait PlayRun extends PlayInternalKeys {
  this: PlayReloader =>

  /**
   * Configuration for the Play docs application's dependencies. Used to build a classloader for
   * that application. Hidden so that it isn't exposed when the user application is published.
   */
  val DocsApplication = config("docs").hide

  // Regex to match Java System Properties of the format -Dfoo=bar
  private val SystemProperty = "-D([^=]+)=(.*)".r

  /**
   * Take all the options in javaOptions of the format "-Dfoo=bar" and return them as a Seq of key value pairs of the format ("foo" -> "bar")
   */
  private def extractSystemProperties(javaOptions: Seq[String]): Seq[(String, String)] = {
    javaOptions.collect { case SystemProperty(key, value) => key -> value }
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
    else (javaProperties, maybePort.map(parsePort).orElse(Some(defaultHttpPort)), maybeHttpsPort)
  }

  val createURLClassLoader: ClassLoaderCreator = (name, urls, parent) => new java.net.URLClassLoader(urls, parent) {
    override def toString = name + "{" + getURLs.map(_.toString).mkString(", ") + "}"
  }

  val createDelegatedResourcesClassLoader: ClassLoaderCreator = (name, urls, parent) => new java.net.URLClassLoader(urls, parent) {
    require(parent ne null)
    override def getResources(name: String): java.util.Enumeration[java.net.URL] = getParent.getResources(name)
    override def toString = name + "{" + getURLs.map(_.toString).mkString(", ") + "}"
  }

  val playDefaultRunTask = playRunTask(playRunHooks, playDependencyClasspath, playDependencyClassLoader,
    playReloaderClasspath, playReloaderClassLoader, playAssetsClassLoader)

  /**
   * This method is public API, used by sbt-echo, which is used by Activator:
   *
   * https://github.com/typesafehub/sbt-echo/blob/v0.1.3/play/src/main/scala-sbt-0.13/com/typesafe/sbt/echo/EchoPlaySpecific.scala#L20
   *
   * Do not change its signature without first consulting the Activator team.  Do not change its signature in a minor
   * release.
   */
  def playRunTask(runHooks: TaskKey[Seq[play.PlayRunHook]],
    dependencyClasspath: TaskKey[Classpath], dependencyClassLoader: TaskKey[ClassLoaderCreator],
    reloaderClasspath: TaskKey[Classpath], reloaderClassLoader: TaskKey[ClassLoaderCreator],
    assetsClassLoader: TaskKey[ClassLoader => ClassLoader]): Def.Initialize[InputTask[Unit]] = Def.inputTask {

    val args = Def.spaceDelimited().parsed

    val state = Keys.state.value
    val interaction = playInteractionMode.value

    lazy val devModeServer = startDevMode(
      state,
      runHooks.value,
      (javaOptions in Runtime).value,
      dependencyClasspath.value,
      dependencyClassLoader.value,
      reloaderClasspath,
      reloaderClassLoader.value,
      assetsClassLoader.value,
      playCommonClassloader.value,
      playMonitoredFiles.value,
      playWatchService.value,
      (managedClasspath in DocsApplication).value,
      playDocsJar.value,
      interaction,
      playDefaultPort.value,
      args
    )

    interaction match {
      case nonBlocking: PlayNonBlockingInteractionMode =>
        nonBlocking.start(devModeServer)
      case blocking =>
        devModeServer

        println()
        println(Colors.green("(Server started, use Ctrl+D to stop and go back to the console...)"))
        println()

        // If we have both Watched.Configuration and Watched.ContinuousState
        // attributes and if Watched.ContinuousState.count is 1 then we assume
        // we're in ~ run mode
        val maybeContinuous = for {
          watched <- state.get(Watched.Configuration)
          watchState <- state.get(Watched.ContinuousState)
          if watchState.count == 1
        } yield watched

        maybeContinuous match {
          case Some(watched) =>
            // ~ run mode
            interaction doWithoutEcho {
              twiddleRunMonitor(watched, state, devModeServer.buildLink, Some(WatchState.empty))
            }
          case None =>
            // run mode
            interaction.waitForCancel()
        }

        devModeServer.close()
        println()
    }
  }

  /**
   * Monitor changes in ~run mode.
   */
  @tailrec
  private def twiddleRunMonitor(watched: Watched, state: State, reloader: BuildLink, ws: Option[WatchState] = None): Unit = {
    val ContinuousState = AttributeKey[WatchState]("watch state", "Internal: tracks state for continuous execution.")
    def isEOF(c: Int): Boolean = c == 4

    @tailrec def shouldTerminate: Boolean = (System.in.available > 0) && (isEOF(System.in.read()) || shouldTerminate)

    val sourcesFinder = PathFinder { watched watchPaths state }
    val watchState = ws.getOrElse(state get ContinuousState getOrElse WatchState.empty)

    val (triggered, newWatchState, newState) =
      try {
        val (triggered, newWatchState) = SourceModificationWatch.watch(sourcesFinder, watched.pollInterval, watchState)(shouldTerminate)
        (triggered, newWatchState, state)
      } catch {
        case e: Exception =>
          val log = state.log
          log.error("Error occurred obtaining files to watch.  Terminating continuous execution...")
          (false, watchState, state.fail)
      }

    if (triggered) {
      //Then launch compile
      Project.synchronized {
        val start = System.currentTimeMillis
        Project.runTask(compile in Compile, newState).get._2.toEither.right.map { _ =>
          val duration = System.currentTimeMillis - start
          val formatted = duration match {
            case ms if ms < 1000 => ms + "ms"
            case seconds => (seconds / 1000) + "s"
          }
          println("[" + Colors.green("success") + "] Compiled in " + formatted)
        }
      }

      // Avoid launching too much compilation
      Thread.sleep(Watched.PollDelayMillis)

      // Call back myself
      twiddleRunMonitor(watched, newState, reloader, Some(newWatchState))
    } else {
      ()
    }
  }

  /**
   * Play dev server
   */
  private trait PlayDevServer extends Closeable {
    val buildLink: BuildLink
  }

  /**
   * Start the Play server in dev mode
   *
   * @return A closeable that can be closed to stop the server
   */
  private def startDevMode(state: State, runHooks: Seq[play.PlayRunHook], javaOptions: Seq[String],
    dependencyClasspath: Classpath, dependencyClassLoader: ClassLoaderCreator,
    reloaderClasspathTask: TaskKey[Classpath], reloaderClassLoader: ClassLoaderCreator,
    assetsClassLoader: ClassLoader => ClassLoader, commonClassLoader: ClassLoader,
    monitoredFiles: Seq[String], playWatchService: PlayWatchService,
    docsClasspath: Classpath, docsJar: Option[File],
    interaction: PlayInteractionMode, defaultHttpPort: Int,
    args: Seq[String]): PlayDevServer = {

    val (properties, httpPort, httpsPort) = filterArgs(args, defaultHttpPort = defaultHttpPort)
    val systemProperties = extractSystemProperties(javaOptions)

    require(httpPort.isDefined || httpsPort.isDefined, "You have to specify https.port when http.port is disabled")

    // Set Java properties
    (properties ++ systemProperties).foreach {
      case (key, value) => System.setProperty(key, value)
    }

    println()

    /*
     * We need to do a bit of classloader magic to run the Play application.
     *
     * There are seven classloaders:
     *
     * 1. buildLoader, the classloader of sbt and the Play sbt plugin.
     * 2. commonLoader, a classloader that persists across calls to run.
     *    This classloader is stored inside the
     *    PlayInternalKeys.playCommonClassloader task. This classloader will
     *    load the classes for the H2 database if it finds them in the user's
     *    classpath. This allows H2's in-memory database state to survive across
     *    calls to run.
     * 3. delegatingLoader, a special classloader that overrides class loading
     *    to delegate shared classes for build link to the buildLoader, and accesses
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
     * 6. playAssetsClassLoader, serves assets from all projects, prefixed as
     *    configured.  It does no caching, and doesn't need to be reloaded each
     *    time the assets are rebuilt.
     * 7. reloader.currentApplicationClassLoader, contains the user classes
     *    and resources. Has applicationLoader as its parent, where the
     *    application dependencies are found, and which will delegate through
     *    to the buildLoader via the delegatingLoader for the shared link.
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

    val buildLoader = this.getClass.getClassLoader

    /**
     * ClassLoader that delegates loading of shared build link classes to the
     * buildLoader. Also accesses the reloader resources to make these available
     * to the applicationLoader, creating a full circle for resource loading.
     */
    lazy val delegatingLoader: ClassLoader = new DelegatingClassLoader(commonClassLoader, buildLoader, new ApplicationClassLoaderProvider {
      def get: ClassLoader = { reloader.getClassLoader.orNull }
    })

    lazy val applicationLoader = dependencyClassLoader("PlayDependencyClassLoader", urls(dependencyClasspath), delegatingLoader)
    lazy val assetsLoader = assetsClassLoader(applicationLoader)

    lazy val reloader = newReloader(state, playReload, reloaderClassLoader, reloaderClasspathTask, assetsLoader,
      monitoredFiles, playWatchService)

    try {
      // Now we're about to start, let's call the hooks:
      runHooks.run(_.beforeStarted())

      // Get a handler for the documentation. The documentation content lives in play/docs/content
      // within the play-docs JAR.
      val docsLoader = new URLClassLoader(urls(docsClasspath), applicationLoader)
      val maybeDocsJarFile = docsJar map { f => new JarFile(f) }
      val docHandlerFactoryClass = docsLoader.loadClass("play.docs.BuildDocHandlerFactory")
      val buildDocHandler = maybeDocsJarFile match {
        case Some(docsJarFile) =>
          val factoryMethod = docHandlerFactoryClass.getMethod("fromJar", classOf[JarFile], classOf[String])
          factoryMethod.invoke(null, docsJarFile, "play/docs/content").asInstanceOf[BuildDocHandler]
        case None =>
          val factoryMethod = docHandlerFactoryClass.getMethod("empty")
          factoryMethod.invoke(null).asInstanceOf[BuildDocHandler]
      }

      val server = {
        val mainClass = applicationLoader.loadClass("play.core.server.NettyServer")
        if (httpPort.isDefined) {
          val mainDev = mainClass.getMethod("mainDevHttpMode", classOf[BuildLink], classOf[BuildDocHandler], classOf[Int])
          mainDev.invoke(null, reloader, buildDocHandler, httpPort.get: java.lang.Integer).asInstanceOf[play.core.server.ServerWithStop]
        } else {
          val mainDev = mainClass.getMethod("mainDevOnlyHttpsMode", classOf[BuildLink], classOf[BuildDocHandler], classOf[Int])
          mainDev.invoke(null, reloader, buildDocHandler, httpsPort.get: java.lang.Integer).asInstanceOf[play.core.server.ServerWithStop]
        }
      }

      // Notify hooks
      runHooks.run(_.afterStarted(server.mainAddress))

      new PlayDevServer {
        val buildLink = reloader

        def close() = {
          server.stop()
          maybeDocsJarFile.foreach(_.close())
          reloader.close()

          // Notify hooks
          runHooks.run(_.afterStopped())

          // Remove Java properties
          properties.foreach {
            case (key, _) => System.clearProperty(key)
          }
        }
      }
    } catch {
      case e: Throwable =>
        // Let hooks clean up
        runHooks.foreach { hook =>
          try {
            hook.onError()
          } catch {
            case e: Throwable => // Swallow any exceptions so that all `onError`s get called.
          }
        }
        throw e
    }
  }

  val playPrefixAndAssetsSetting = playPrefixAndAssets := {
    assetsPrefix.value -> (WebKeys.public in Assets).value
  }

  val playAllAssetsSetting = playAllAssets := Seq(playPrefixAndAssets.value)

  val playAssetsClassLoaderSetting = playAssetsClassLoader := { parent =>
    new AssetsClassLoader(parent, playAllAssets.value)
  }

  val playStartCommand = Command.args("start", "<port>") { (state: State, args: Seq[String]) =>

    val extracted = Project.extract(state)

    val interaction = extracted.get(playInteractionMode)
    // Parse HTTP port argument
    val (properties, httpPort, httpsPort) = filterArgs(args, defaultHttpPort = extracted.get(playDefaultPort))
    require(httpPort.isDefined || httpsPort.isDefined, "You have to specify https.port when http.port is disabled")

    Project.runTask(stage, state).get._2.toEither match {
      case Left(_) =>
        println()
        println("Cannot start with errors.")
        println()
        state.fail
      case Right(_) =>
        val stagingBin = Some(extracted.get(stagingDirectory in Universal) / "bin" / extracted.get(normalizedName in Universal)).map {
          f =>
            if (System.getProperty("os.name").toLowerCase.contains("win")) f.getAbsolutePath + ".bat" else f.getAbsolutePath
        }.get
        val javaProductionOptions = Project.runTask(javaOptions in Production, state).get._2.toEither.right.getOrElse(Seq[String]())

        // Note that I'm unable to pass system properties along with properties... if I do then I receive:
        //  java.nio.charset.IllegalCharsetNameException: "UTF-8"
        // Things are working without passing system properties, and I'm unsure that they need to be passed explicitly. If def main(args: Array[String]){
        // problem occurs in this area then at least we know what to look at.
        val args = Seq(stagingBin) ++
          properties.map {
            case (key, value) => s"-D$key=$value"
          } ++
          javaProductionOptions ++
          Seq("-Dhttp.port=" + httpPort.getOrElse("disabled"))
        val builder = new java.lang.ProcessBuilder(args.asJava)
        new Thread {
          override def run() {
            System.exit(Process(builder).!)
          }
        }.start()

        println(Colors.green(
          """|
            |(Starting server. Type Ctrl+D to exit logs, the server will remain in background)
            | """.stripMargin))

        interaction.waitForCancel()

        println()

        state.copy(remainingCommands = Seq.empty)
    }

  }

}
