/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.runsupport

import java.io.{ Closeable, File }
import java.net.{ URL, URLClassLoader }
import java.util.jar.JarFile
import play.api.PlayException
import play.core.{ Build, BuildLink, BuildDocHandler }
import play.runsupport.classloader.{ ApplicationClassLoaderProvider, DelegatingClassLoader }
import sbt.{ PathFinder, WatchState, SourceModificationWatch }

object Reloader {

  sealed trait CompileResult
  case class CompileSuccess(sources: SourceMap, classpath: Classpath) extends CompileResult
  case class CompileFailure(exception: PlayException) extends CompileResult

  type SourceMap = Map[String, Source]
  case class Source(file: File, original: Option[File])

  type Classpath = Seq[File]

  type ClassLoaderCreator = (String, Array[URL], ClassLoader) => ClassLoader

  val SystemProperty = "-D([^=]+)=(.*)".r

  /**
   * Take all the options in javaOptions of the format "-Dfoo=bar" and return them as a Seq of key value pairs of the format ("foo" -> "bar")
   */
  def extractSystemProperties(javaOptions: Seq[String]): Seq[(String, String)] = {
    javaOptions.collect { case SystemProperty(key, value) => key -> value }
  }

  def parsePort(portString: String): Int = {
    try {
      Integer.parseInt(portString)
    } catch {
      case e: NumberFormatException => sys.error("Invalid port argument: " + portString)
    }
  }

  def filterArgs(args: Seq[String], defaultHttpPort: Int, defaultHttpAddress: String): (Seq[(String, String)], Option[Int], Option[Int], String) = {
    val (propertyArgs, otherArgs) = args.partition(_.startsWith("-D"))

    val properties = propertyArgs.map(_.drop(2).split('=')).map(a => a(0) -> a(1)).toSeq

    val props = properties.toMap
    def prop(key: String): Option[String] = props.get(key) orElse sys.props.get(key)

    // http port can be defined as the first non-property argument, or a -Dhttp.port argument or system property
    // the http port can be disabled (set to None) by setting any of the input methods to "disabled"
    val httpPortString = otherArgs.headOption orElse prop("http.port")
    val httpPort = {
      if (httpPortString.exists(_ == "disabled")) None
      else httpPortString map parsePort orElse Option(defaultHttpPort)
    }

    // https port can be defined as a -Dhttps.port argument or system property
    val httpsPort = prop("https.port") map parsePort

    // http address can be defined as a -Dhttp.address argument or system property
    val httpAddress = prop("http.address") getOrElse defaultHttpAddress

    (properties, httpPort, httpsPort, httpAddress)
  }

  def urls(cp: Classpath): Array[URL] = cp.map(_.toURI.toURL).toArray

  val createURLClassLoader: ClassLoaderCreator = (name, urls, parent) => new java.net.URLClassLoader(urls, parent) {
    override def toString = name + "{" + getURLs.map(_.toString).mkString(", ") + "}"
  }

  val createDelegatedResourcesClassLoader: ClassLoaderCreator = (name, urls, parent) => new java.net.URLClassLoader(urls, parent) {
    require(parent ne null)
    override def getResources(name: String): java.util.Enumeration[java.net.URL] = getParent.getResources(name)
    override def toString = name + "{" + getURLs.map(_.toString).mkString(", ") + "}"
  }

  def assetsClassLoader(allAssets: Seq[(String, File)])(parent: ClassLoader): ClassLoader = new AssetsClassLoader(parent, allAssets)

  def commonClassLoader(classpath: Classpath) = {
    lazy val commonJars: PartialFunction[java.io.File, java.net.URL] = {
      case jar if jar.getName.startsWith("h2-") || jar.getName == "h2.jar" => jar.toURI.toURL
    }

    new java.net.URLClassLoader(classpath.collect(commonJars).toArray, null /* important here, don't depend of the sbt classLoader! */ ) {
      override def toString = "Common ClassLoader: " + getURLs.map(_.toString).mkString(",")
    }
  }

  /**
   * Play dev server
   */
  trait PlayDevServer extends Closeable {
    val buildLink: BuildLink
  }

  /**
   * Start the Play server in dev mode
   *
   * @return A closeable that can be closed to stop the server
   */
  def startDevMode(runHooks: Seq[RunHook], javaOptions: Seq[String],
    dependencyClasspath: Classpath, dependencyClassLoader: ClassLoaderCreator,
    reloadCompile: () => CompileResult, reloaderClassLoader: ClassLoaderCreator,
    assetsClassLoader: ClassLoader => ClassLoader, commonClassLoader: ClassLoader,
    monitoredFiles: Seq[String], fileWatchService: FileWatchService,
    docsClasspath: Classpath, docsJar: Option[File],
    defaultHttpPort: Int, defaultHttpAddress: String, projectPath: File,
    devSettings: Seq[(String, String)], args: Seq[String],
    runSbtTask: String => AnyRef, mainClassName: String): PlayDevServer = {

    val (properties, httpPort, httpsPort, httpAddress) = filterArgs(args, defaultHttpPort, defaultHttpAddress)
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

    val buildLoader = this.getClass.getClassLoader

    /**
     * ClassLoader that delegates loading of shared build link classes to the
     * buildLoader. Also accesses the reloader resources to make these available
     * to the applicationLoader, creating a full circle for resource loading.
     */
    lazy val delegatingLoader: ClassLoader = new DelegatingClassLoader(commonClassLoader, Build.sharedClasses, buildLoader, new ApplicationClassLoaderProvider {
      def get: ClassLoader = { reloader.getClassLoader.orNull }
    })

    lazy val applicationLoader = dependencyClassLoader("PlayDependencyClassLoader", urls(dependencyClasspath), delegatingLoader)
    lazy val assetsLoader = assetsClassLoader(applicationLoader)

    lazy val reloader = new Reloader(reloadCompile, reloaderClassLoader, assetsLoader, projectPath, devSettings, monitoredFiles, fileWatchService, runSbtTask)

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
        val mainClass = applicationLoader.loadClass(mainClassName)
        if (httpPort.isDefined) {
          val mainDev = mainClass.getMethod("mainDevHttpMode", classOf[BuildLink], classOf[BuildDocHandler], classOf[Int], classOf[String])
          mainDev.invoke(null, reloader, buildDocHandler, httpPort.get: java.lang.Integer, httpAddress).asInstanceOf[play.core.server.ServerWithStop]
        } else {
          val mainDev = mainClass.getMethod("mainDevOnlyHttpsMode", classOf[BuildLink], classOf[BuildDocHandler], classOf[Int], classOf[String])
          mainDev.invoke(null, reloader, buildDocHandler, httpsPort.get: java.lang.Integer, httpAddress).asInstanceOf[play.core.server.ServerWithStop]
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

}

import Reloader.{ CompileResult, CompileSuccess, CompileFailure }
import Reloader.{ ClassLoaderCreator, SourceMap }

class Reloader(
    reloadCompile: () => CompileResult,
    createClassLoader: ClassLoaderCreator,
    baseLoader: ClassLoader,
    val projectPath: File,
    devSettings: Seq[(String, String)],
    monitoredFiles: Seq[String],
    fileWatchService: FileWatchService,
    runSbtTask: String => AnyRef) extends BuildLink {

  // The current classloader for the application
  @volatile private var currentApplicationClassLoader: Option[ClassLoader] = None
  // Flag to force a reload on the next request.
  // This is set if a compile error occurs, and also by the forceReload method on BuildLink, which is called for
  // example when evolutions have been applied.
  @volatile private var forceReloadNextTime = false
  // Whether any source files have changed since the last request.
  @volatile private var changed = false
  // The last successful compile results. Used for rendering nice errors.
  @volatile private var currentSourceMap = Option.empty[SourceMap]
  // A watch state for the classpath. Used to determine whether anything on the classpath has changed as a result
  // of compilation, and therefore a new classloader is needed and the app needs to be reloaded.
  @volatile private var watchState: WatchState = WatchState.empty

  // Create the watcher, updates the changed boolean when a file has changed.
  private val watcher = fileWatchService.watch(monitoredFiles.map(new File(_)), () => {
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
    Reloader.synchronized {
      if (changed || forceReloadNextTime || currentSourceMap.isEmpty || currentApplicationClassLoader.isEmpty) {

        val shouldReload = forceReloadNextTime

        changed = false
        forceReloadNextTime = false

        // Run the reload task, which will trigger everything to compile
        reloadCompile() match {
          case CompileFailure(exception) =>
            // We force reload next time because compilation failed this time
            forceReloadNextTime = true
            exception

          case CompileSuccess(sourceMap, classpath) =>

            currentSourceMap = Some(sourceMap)

            // We only want to reload if the classpath has changed.  Assets don't live on the classpath, so
            // they won't trigger a reload.
            // Use the SBT watch service, passing true as the termination to force it to break after one check
            val (_, newState) = SourceModificationWatch.watch(PathFinder.strict(classpath).***, 0, watchState)(true)
            // SBT has a quiet wait period, if that's set to true, sources were modified
            val triggered = newState.awaitingQuietPeriod
            watchState = newState

            if (triggered || shouldReload || currentApplicationClassLoader.isEmpty) {
              // Create a new classloader
              val version = classLoaderVersion.incrementAndGet
              val name = "ReloadableClassLoader(v" + version + ")"
              val urls = Reloader.urls(classpath)
              val loader = createClassLoader(name, urls, baseLoader)
              currentApplicationClassLoader = Some(loader)
              loader
            } else {
              null // null means nothing changed
            }
        }
      } else {
        null // null means nothing changed
      }
    }
  }

  lazy val settings = {
    import scala.collection.JavaConverters._
    devSettings.toMap.asJava
  }

  def forceReload() {
    forceReloadNextTime = true
  }

  def findSource(className: String, line: java.lang.Integer): Array[java.lang.Object] = {
    val topType = className.split('$').head
    currentSourceMap.flatMap { sources =>
      sources.get(topType).map { source =>
        Array[java.lang.Object](source.original.getOrElse(source.file), line)
      }
    }.orNull
  }

  def runTask(task: String): AnyRef = runSbtTask(task)

  def close() = {
    currentApplicationClassLoader = None
    currentSourceMap = None
    watcher.stop()
  }

  def getClassLoader = currentApplicationClassLoader
}
