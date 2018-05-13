/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport

import java.io.{ Closeable, File }
import java.net.{ URL, URLClassLoader }
import java.security.{ AccessController, PrivilegedAction }
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import java.util.{ Timer, TimerTask }

import better.files.{ File => _, _ }
import play.api.PlayException
import play.core.{ Build, BuildLink }
import play.dev.filewatch.FileWatchService
import play.runsupport.classloader.{ ApplicationClassLoaderProvider, DelegatingClassLoader }

import scala.collection.JavaConverters._

object Reloader {

  sealed trait CompileResult
  case class CompileSuccess(sources: Map[String, Source], classpath: Seq[File]) extends CompileResult
  case class CompileFailure(exception: PlayException) extends CompileResult

  trait GeneratedSourceMapping {
    def getOriginalLine(generatedSource: File, line: Integer): Integer
  }

  case class Source(file: File, original: Option[File])

  type ClassLoaderCreator = (String, Array[URL], ClassLoader) => ClassLoader

  val SystemProperty = "-D([^=]+)=(.*)".r

  private val accessControlContext = AccessController.getContext

  /**
   * Execute f with context ClassLoader of Reloader
   */
  private def withReloaderContextClassLoader[T](f: => T): T = {
    val thread = Thread.currentThread
    val oldLoader = thread.getContextClassLoader
    // we use accessControlContext & AccessController to avoid a ClassLoader leak (ProtectionDomain class)
    AccessController.doPrivileged(new PrivilegedAction[T]() {
      def run: T = {
        try {
          thread.setContextClassLoader(classOf[Reloader].getClassLoader)
          f
        } finally {
          thread.setContextClassLoader(oldLoader)
        }
      }
    }, accessControlContext)
  }

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

  def filterArgs(
    args: Seq[String],
    defaultHttpPort: Int,
    defaultHttpAddress: String,
    devSettings: Seq[(String, String)]): (Seq[(String, String)], Option[Int], Option[Int], String) = {
    val (propertyArgs, otherArgs) = args.partition(_.startsWith("-D"))

    val properties = propertyArgs.map {
      _.drop(2).span(_ != '=') match {
        case (key, v) => key -> v.tail
      }
    }
    val props = properties.toMap

    def prop(key: String): Option[String] =
      props.get(key) orElse sys.props.get(key)

    def parsePortValue(portValue: Option[String], defaultValue: Option[Int] = None): Option[Int] = {
      portValue match {
        case None => defaultValue
        case Some("disabled") => None
        case Some(s) => Some(parsePort(s))
      }
    }

    val devMap = devSettings.toMap

    // http port can be defined as the first non-property argument, or a -Dhttp.port argument or system property
    // the http port can be disabled (set to None) by setting any of the input methods to "disabled"
    // Or it can be defined in devSettings as "play.server.http.port"
    val httpPortString: Option[String] = otherArgs.headOption orElse prop("http.port") orElse devMap.get("play.server.http.port")
    val httpPort: Option[Int] = parsePortValue(httpPortString, Option(defaultHttpPort))

    // https port can be defined as a -Dhttps.port argument or system property
    val httpsPortString: Option[String] = prop("https.port") orElse devMap.get("play.server.https.port")
    val httpsPort = parsePortValue(httpsPortString)

    // http address can be defined as a -Dhttp.address argument or system property
    val httpAddress = prop("http.address") orElse devMap.get("play.server.http.address") getOrElse defaultHttpAddress

    (properties, httpPort, httpsPort, httpAddress)
  }

  def urls(cp: Seq[File]): Array[URL] = cp.map(_.toURI.toURL).toArray

  def assetsClassLoader(allAssets: Seq[(String, File)])(parent: ClassLoader): ClassLoader = new AssetsClassLoader(parent, allAssets)

  def commonClassLoader(classpath: Seq[File]) = {
    lazy val commonJars: PartialFunction[java.io.File, java.net.URL] = {
      case jar if jar.getName.startsWith("h2-") || jar.getName == "h2.jar" => jar.toURI.toURL
    }

    new java.net.URLClassLoader(classpath.collect(commonJars).toArray, null /* important here, don't depend of the sbt classLoader! */ ) {
      override def toString = "Common ClassLoader: " + getURLs.map(_.toString).mkString(",")
    }
  }

  /**
   * Dev server
   */
  trait DevServer extends Closeable {
    val buildLink: BuildLink

    /** Allows to register a listener that will be triggered a monitored file is changed. */
    def addChangeListener(f: () => Unit): Unit

    /** Reloads the application.*/
    def reload(): Unit

    /** URL at which the application is running (if started) */
    def url(): String
  }

  /**
   * Start the server in dev mode
   *
   * @return A closeable that can be closed to stop the server
   */
  def startDevMode(
    runHooks: Seq[RunHook], javaOptions: Seq[String],
    commonClassLoader: ClassLoader, dependencyClasspath: Seq[File],
    reloadCompile: () => CompileResult, assetsClassLoader: ClassLoader => ClassLoader,
    monitoredFiles: Seq[File], fileWatchService: FileWatchService,
    generatedSourceHandlers: Map[String, GeneratedSourceMapping],
    defaultHttpPort: Int, defaultHttpAddress: String, projectPath: File,
    devSettings: Seq[(String, String)], args: Seq[String],
    mainClassName: String, reloadLock: AnyRef
  ): DevServer = {

    val (properties, httpPort, httpsPort, httpAddress) = filterArgs(args, defaultHttpPort, defaultHttpAddress, devSettings)
    val systemProperties = extractSystemProperties(javaOptions)

    require(httpPort.isDefined || httpsPort.isDefined, "You have to specify https.port when http.port is disabled")

    // Set Java properties
    (properties ++ systemProperties).foreach {
      case (key, value) => System.setProperty(key, value)
    }

    println()

    /*
     * We need to do a bit of classloader magic to run the application.
     *
     * There are six classloaders:
     *
     * 1. buildLoader, the classloader of sbt and the sbt plugin.
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
     * 5. playAssetsClassLoader, serves assets from all projects, prefixed as
     *    configured.  It does no caching, and doesn't need to be reloaded each
     *    time the assets are rebuilt.
     * 6. reloader.currentApplicationClassLoader, contains the user classes
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
      def get: URLClassLoader = { reloader.getClassLoader.orNull }
    })

    lazy val applicationLoader = new NamedURLClassLoader("DependencyClassLoader", urls(dependencyClasspath), delegatingLoader)
    lazy val assetsLoader = assetsClassLoader(applicationLoader)

    lazy val reloader = new Reloader(reloadCompile, assetsLoader, projectPath, devSettings, monitoredFiles, fileWatchService, generatedSourceHandlers, reloadLock)

    try {
      // Now we're about to start, let's call the hooks:
      runHooks.run(_.beforeStarted())

      val server = {
        val mainClass = applicationLoader.loadClass(mainClassName)
        if (httpPort.isDefined) {
          val mainDev = mainClass.getMethod("mainDevHttpMode", classOf[BuildLink], classOf[Int], classOf[String])
          mainDev.invoke(null, reloader, httpPort.get: java.lang.Integer, httpAddress).asInstanceOf[play.core.server.ReloadableServer]
        } else {
          val mainDev = mainClass.getMethod("mainDevOnlyHttpsMode", classOf[BuildLink], classOf[Int], classOf[String])
          mainDev.invoke(null, reloader, httpsPort.get: java.lang.Integer, httpAddress).asInstanceOf[play.core.server.ReloadableServer]
        }
      }

      // Notify hooks
      runHooks.run(_.afterStarted(server.mainAddress))

      new DevServer {
        val buildLink = reloader
        def addChangeListener(f: () => Unit): Unit = reloader.addChangeListener(f)
        def reload(): Unit = server.reload()
        def close(): Unit = {
          server.stop()
          reloader.close()

          // Notify hooks
          runHooks.run(_.afterStopped())

          // Remove Java properties
          properties.foreach {
            case (key, _) => System.clearProperty(key)
          }
        }
        def url(): String = server.mainAddress().getHostName + ":" + server.mainAddress().getPort
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
        // Convert play-server exceptions to our to our ServerStartException
        def getRootCause(t: Throwable): Throwable = if (t.getCause == null) t else getRootCause(t.getCause)
        if (getRootCause(e).getClass.getName == "play.core.server.ServerListenException") {
          throw new ServerStartException(e)
        }
        throw e
    }
  }

  /**
   * Start the server without hot reloading
   */
  def startNoReload(parentClassLoader: ClassLoader, dependencyClasspath: Seq[File], buildProjectPath: File,
    devSettings: Seq[(String, String)], httpPort: Int, mainClassName: String): DevServer = {
    val buildLoader = this.getClass.getClassLoader

    lazy val delegatingLoader: ClassLoader = new DelegatingClassLoader(
      parentClassLoader,
      Build.sharedClasses, buildLoader, new ApplicationClassLoaderProvider {
        def get: URLClassLoader = { applicationLoader }
      })

    lazy val applicationLoader = new NamedURLClassLoader("DependencyClassLoader", urls(dependencyClasspath),
      delegatingLoader)

    val _buildLink = new BuildLink {
      private val initialized = new java.util.concurrent.atomic.AtomicBoolean(false)
      override def reload(): AnyRef = {
        if (initialized.compareAndSet(false, true)) applicationLoader
        else null // this means nothing to reload
      }
      override def projectPath(): File = buildProjectPath
      override def settings(): java.util.Map[String, String] = devSettings.toMap.asJava
      override def forceReload(): Unit = ()
      override def findSource(className: String, line: Integer): Array[AnyRef] = null
    }

    val mainClass = applicationLoader.loadClass(mainClassName)
    val mainDev = mainClass.getMethod("mainDevHttpMode", classOf[BuildLink], classOf[Int])
    val server = mainDev.invoke(null, _buildLink, httpPort: java.lang.Integer).asInstanceOf[play.core.server.ReloadableServer]

    server.reload() // it's important to initialize the server

    new Reloader.DevServer {
      val buildLink: BuildLink = _buildLink

      /** Allows to register a listener that will be triggered a monitored file is changed. */
      def addChangeListener(f: () => Unit): Unit = ()

      /** Reloads the application.*/
      def reload(): Unit = ()

      /** URL at which the application is running (if started) */
      def url(): String = server.mainAddress().getHostName + ":" + server.mainAddress().getPort

      def close(): Unit = server.stop()
    }
  }

}

import play.runsupport.Reloader._

class Reloader(
    reloadCompile: () => CompileResult,
    baseLoader: ClassLoader,
    val projectPath: File,
    devSettings: Seq[(String, String)],
    monitoredFiles: Seq[File],
    fileWatchService: FileWatchService,
    generatedSourceHandlers: Map[String, GeneratedSourceMapping],
    reloadLock: AnyRef) extends BuildLink {

  // The current classloader for the application
  @volatile private var currentApplicationClassLoader: Option[URLClassLoader] = None
  // Flag to force a reload on the next request.
  // This is set if a compile error occurs, and also by the forceReload method on BuildLink, which is called for
  // example when evolutions have been applied.
  @volatile private var forceReloadNextTime = false
  // Whether any source files have changed since the last request.
  @volatile private var changed = false
  // The last successful compile results. Used for rendering nice errors.
  @volatile private var currentSourceMap = Option.empty[Map[String, Source]]
  // Last time the classpath was modified in millis. Used to determine whether anything on the classpath has
  // changed as a result of compilation, and therefore a new classloader is needed and the app needs to be reloaded.
  @volatile private var lastModified: Long = 0L

  // Stores the most recent time that a file was changed
  private val fileLastChanged = new AtomicReference[Instant]()

  // Create the watcher, updates the changed boolean when a file has changed.
  private val watcher = fileWatchService.watch(monitoredFiles, () => {
    changed = true
  })
  private val classLoaderVersion = new java.util.concurrent.atomic.AtomicInteger(0)

  private val quietTimeTimer = new Timer("reloader-timer", true)

  private val listeners = new java.util.concurrent.CopyOnWriteArrayList[() => Unit]()

  private val quietPeriodMs: Long = 200L
  private def onChange(): Unit = {
    val now = Instant.now()
    fileLastChanged.set(now)
    // set timer task
    quietTimeTimer.schedule(new TimerTask {
      override def run(): Unit = quietPeriodFinished(now)
    }, quietPeriodMs)
  }

  private def quietPeriodFinished(start: Instant): Unit = {
    // If our start time is equal to the most recent start time stored, then execute the handlers and set the most
    // recent time to null, otherwise don't do anything.
    if (fileLastChanged.compareAndSet(start, null)) {
      import scala.collection.JavaConverters._
      listeners.iterator().asScala.foreach(listener => listener())
    }
  }

  def addChangeListener(f: () => Unit): Unit = listeners.add(f)

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
    reloadLock.synchronized {
      if (changed || forceReloadNextTime || currentSourceMap.isEmpty || currentApplicationClassLoader.isEmpty) {

        val shouldReload = forceReloadNextTime

        changed = false
        forceReloadNextTime = false

        // use Reloader context ClassLoader to avoid ClassLoader leaks in sbt/scala-compiler threads
        Reloader.withReloaderContextClassLoader {
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
              val classpathFiles = classpath.iterator.filter(_.exists()).flatMap(_.toScala.listRecursively).map(_.toJava)
              val newLastModified =
                classpathFiles.foldLeft(0L) { (acc, file) => math.max(acc, file.lastModified) }
              val triggered = newLastModified > lastModified
              lastModified = newLastModified

              if (triggered || shouldReload || currentApplicationClassLoader.isEmpty) {
                // Create a new classloader
                val version = classLoaderVersion.incrementAndGet
                val name = "ReloadableClassLoader(v" + version + ")"
                val urls = Reloader.urls(classpath)
                val loader = new DelegatedResourcesClassLoader(name, urls, baseLoader)
                currentApplicationClassLoader = Some(loader)
                loader
              } else {
                null // null means nothing changed
              }
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

  def forceReload(): Unit = {
    forceReloadNextTime = true
  }

  def findSource(className: String, line: java.lang.Integer): Array[java.lang.Object] = {
    val topType = className.split('$').head
    currentSourceMap.flatMap { sources =>
      sources.get(topType).map { source =>
        source.original match {
          case Some(origFile) if line != null =>
            generatedSourceHandlers.get(origFile.getName.split('.').drop(1).mkString(".")) match {
              case Some(handler) =>
                Array[java.lang.Object](origFile, handler.getOriginalLine(source.file, line))
              case _ =>
                Array[java.lang.Object](origFile, line)
            }
          case Some(origFile) =>
            Array[java.lang.Object](origFile, null)
          case None =>
            Array[java.lang.Object](source.file, line)
        }
      }
    }.orNull
  }

  def close() = {
    currentApplicationClassLoader = None
    currentSourceMap = None
    watcher.stop()
    quietTimeTimer.cancel()
  }

  def getClassLoader = currentApplicationClassLoader
}
