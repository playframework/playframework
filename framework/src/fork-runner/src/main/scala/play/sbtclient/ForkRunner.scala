/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.forkrunner

import play.runsupport.{ PlayExceptions, Serializers }
import java.io.{ File, Closeable }
import java.net.{ URI, URLClassLoader }
import java.util.jar.JarFile
import sbt.client.{ SbtClient, SbtConnector, TaskKey }
import sbt.protocol.{ ScopedKey, TaskSuccess, TaskFailure }
import scala.concurrent.ExecutionContext.Implicits.global
import play.runsupport.protocol.{ PlayForkSupportResult, SourceMapTarget }
import play.runsupport.{ PlayExceptionNoSource, PlayExceptionWithSource }
import play.core.{ BuildLink, BuildDocHandler }
import play.core.classloader.ApplicationClassLoaderProvider
import scala.concurrent.{ Promise, Future }
import play.runsupport.{ PlayWatchService, LoggerProxy, AssetsClassLoader }
import sbt.{ IO, PathFinder, WatchState, SourceModificationWatch }
import scala.annotation.tailrec
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CountDownLatch
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }
import akka.actor._
import com.typesafe.config.{ ConfigFactory, Config }
import akka.event.LoggingAdapter
import sbt.client.actors.{ SbtClientProxy, SbtConnectionProxy }

object ForkRunner {
  import scala.language.implicitConversions
  import Serializers._

  type URL = java.net.URL
  type Classpath = Seq[File]
  type ClassLoaderCreator = (String, Array[URL], ClassLoader) => ClassLoader
  trait PlayBuildLink extends BuildLink {
    def close()
    def getClassLoader: Option[ClassLoader]
  }

  case class Reload(expected: Promise[Either[Throwable, PlayForkSupportResult]])

  import PlayExceptions._

  def urls(cp: Classpath): Array[URL] = cp.map(_.toURI.toURL).toArray

  // dependencyClassLoader: ClassLoaderCreator,
  def uRLClassLoaderCreator(name: String, urls: Array[URL], parent: ClassLoader): ClassLoader = new java.net.URLClassLoader(urls, parent) {
    override def toString = name + "{" + getURLs.map(_.toString).mkString(", ") + "}"
  }

  def assetsClassLoader(parent: ClassLoader, allAssets: Seq[(String, File)]): ClassLoader = new AssetsClassLoader(parent, allAssets)

  // reloaderClassLoader: ClassLoaderCreator
  def delegatedResourcesClassLoaderCreator(name: String, urls: Array[URL], parent: ClassLoader): ClassLoader = {
    new java.net.URLClassLoader(urls, parent) {
      require(parent ne null)
      override def getResources(name: String): java.util.Enumeration[java.net.URL] = {
        getParent.getResources(name)
      }
      override def toString = name + "{" + getURLs.map(_.toString).mkString(", ") + "}"
    }
  }

  private implicit def convertToOption[T](o: xsbti.Maybe[T]): Option[T] =
    if (o.isDefined()) Some(o.get())
    else None

  // commonClassLoader: ClassLoader
  def playCommonClassloaderTask(classpath: Classpath) = {
    lazy val commonJars: PartialFunction[java.io.File, java.net.URL] = {
      case jar if jar.getName.startsWith("h2-") || jar.getName == "h2.jar" => jar.toURI.toURL
    }

    new java.net.URLClassLoader(classpath.collect(commonJars).toArray, null /* important here, don't depend of the sbt classLoader! */ ) {
      override def toString = "Common ClassLoader: " + getURLs.map(_.toString).mkString(",")
    }
  }

  def newReloader(runReload: () => Either[Throwable, PlayForkSupportResult],
    createClassLoader: ClassLoaderCreator,
    baseLoader: ClassLoader,
    monitoredFiles: Seq[String],
    _projectPath: File,
    devSettings: Seq[(String, String)],
    playWatchService: PlayWatchService): PlayBuildLink = {
    new PlayBuildLink {
      val projectPath: File = _projectPath
      // The current classloader for the application
      @volatile private var currentApplicationClassLoader: Option[ClassLoader] = None
      // Flag to force a reload on the next request.
      // This is set if a compile error occurs, and also by the forceReload method on BuildLink, which is called for
      // example when evolutions have been applied.
      @volatile private var forceReloadNextTime = false
      // Whether any source files have changed since the last request.
      @volatile private var changed = false
      // The last succesful source map
      @volatile private var sourceMap = Option.empty[Map[String, SourceMapTarget]]
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
        if (changed || forceReloadNextTime || sourceMap.isEmpty || currentApplicationClassLoader.isEmpty) {

          val shouldReload = forceReloadNextTime

          changed = false
          forceReloadNextTime = false

          // Run the reload task, which will trigger everything to compile
          runReload()
            .left.map(taskFailureHandler)
            .right.map { compilationResult =>

              sourceMap = Some(compilationResult.sourceMap)

              // We only want to reload if the classpath has changed.  Assets don't live on the classpath, so
              // they won't trigger a reload.
              // Use the SBT watch service, passing true as the termination to force it to break after one check
              val (_, newState) = SourceModificationWatch.watch(PathFinder.strict(compilationResult.reloaderClasspath).***, 0, watchState)(true)
              // SBT has a quiet wait period, if that's set to true, sources were modified
              val triggered = newState.awaitingQuietPeriod
              watchState = newState

              if (triggered || shouldReload || currentApplicationClassLoader.isEmpty) {

                // Create a new classloader
                val version = classLoaderVersion.incrementAndGet
                val name = "ReloadableClassLoader(v" + version + ")"
                val urls = ForkRunner.urls(compilationResult.reloaderClasspath)
                val loader = createClassLoader(name, urls, baseLoader)
                currentApplicationClassLoader = Some(loader)
                loader
              } else {
                null // null means nothing changed
              }
            }.fold(identity, identity)
        } else {
          null // null means nothing changed
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
        sourceMap.flatMap {
          _.get(topType).map { st =>
            Array[java.lang.Object](st.originalSource.getOrElse(st.sourceFile), line)
          }
        }.orNull
      }

      private def taskFailureHandler(in: Throwable): Exception = {
        // We force reload next time because compilation failed this time
        forceReloadNextTime = true
        in match {
          case e: PlayExceptionNoSource => e
          case e: PlayExceptionWithSource => e
          case e: Exception => UnexpectedException(unexpected = Some(e))
        }
      }

      // TODO - Cannot implement in the forked running mode
      def runTask(task: String): AnyRef = {
        null
      }

      def close() = {
        currentApplicationClassLoader = None
        sourceMap = None
        watcher.stop()
      }

      def isForked(): Boolean = true

      def getClassLoader = currentApplicationClassLoader
    }

  }

  case class Config(connector: SbtConnector,
      latch: CountDownLatch,
      command: String,
      projectDir: File,
      buildUri: URI,
      project: String,
      serverBuilder: PlayForkSupportResult => (() => Either[Throwable, PlayForkSupportResult]) => PlayDevServer) {

    def notifyServerStartCommand(urlString: String): String = s"$project/playNotifyServerStart $urlString"
  }

  object Int {
    def unapply(s: String): Option[Int] = try {
      Some(s.toInt)
    } catch {
      case _: java.lang.NumberFormatException => None
    }
  }

  def wrapLogger(logger: LoggingAdapter): LoggerProxy = new LoggerProxy {
    def verbose(message: => String): Unit = logger.debug(message)
    def debug(message: => String): Unit = logger.debug(message)
    def info(message: => String): Unit = logger.info(message)
    def warn(message: => String): Unit = logger.warning(message)
    def error(message: => String): Unit = logger.error(message)
    def trace(t: => Throwable): Unit = logger.error(t, "trace")
    def success(message: => String): Unit = logger.info(message)
  }

  object AkkaConfig {
    def config(projectRoot: File) = {
      val fallback = ConfigFactory.load()
      ConfigFactory.parseFileAnySyntax(new File(projectRoot, "conf/application.conf")).withFallback(fallback).getConfig("play-dev")
    }
  }

  def main(args: Array[String]): Unit = {
    val baseDirectoryString = args(0)
    val buildUriString = args(1)
    val targetDirectory = args(2)
    val project = args(3)
    val httpPort: Option[Int] = Int.unapply(args(4))
    val httpsPort: Option[Int] = Int.unapply(args(5))
    val pollDelayMillis: Int = args(6).toInt
    val akkaConfig = AkkaConfig.config(new File(baseDirectoryString))
    val buildUri = new URI(buildUriString)

    val system = ActorSystem("play-dev-mode-runner", akkaConfig)
    val log = system.log
    log.debug(s"Forked Play dev-mode runner started")
    log.debug(s"baseDirectoryString: $baseDirectoryString")
    log.debug(s"buildUriString: $buildUriString")
    log.debug(s"targetDirectory: $targetDirectory")
    log.debug(s"project: $project")
    log.debug(s"httpPort: $httpPort")
    log.debug(s"httpsPort: $httpsPort")
    log.debug(s"pollDelayMillis: $pollDelayMillis")
    log.debug(s"akkaConfig: $akkaConfig")

    val latch = new CountDownLatch(1)
    val projectDir = new File(baseDirectoryString)
    val conn = SbtConnector("play-fork", "play-fork", projectDir)
    val serverBuilder = runServer(httpPort, httpsPort, new File(buildUri), new File(targetDirectory), pollDelayMillis, wrapLogger(log))_
    val config = Config(conn, latch, s"$project/play-default-fork-run-support", projectDir, buildUri, project, serverBuilder)
    val runner = system.actorOf(Props(new ForkRunner(config)))
    log.debug("Awaiting ForkRunner shutdown")
    latch.await()
    log.debug("Exiting, awaiting actor system shutdown")
    system.shutdown()
    log.debug("Exited.")
  }

  private[forkrunner] trait PlayDevServer extends Closeable {
    val buildLink: BuildLink
    def urlString: String
  }

  def runServer(httpPort: Option[Int],
    httpsPort: Option[Int],
    projectPath: File,
    targetDirectory: File,
    pollDelayMillis: Int,
    logger: LoggerProxy)(in: PlayForkSupportResult)(runReload: () => Either[Throwable, PlayForkSupportResult]): PlayDevServer = {
    try {
      val buildLoader = this.getClass.getClassLoader
      val commonClassLoader = playCommonClassloaderTask(in.dependencyClasspath)

      lazy val delegatingLoader: ClassLoader = new DelegatingClassLoader(commonClassLoader, buildLoader, new ApplicationClassLoaderProvider {
        def get: ClassLoader = { reloader.getClassLoader.orNull }
      })

      lazy val applicationLoader = uRLClassLoaderCreator("PlayDependencyClassLoader", urls(in.dependencyClasspath), delegatingLoader)
      lazy val assetsLoader = assetsClassLoader(applicationLoader, in.allAssets)

      lazy val reloader: PlayBuildLink = newReloader(runReload,
        delegatedResourcesClassLoaderCreator _,
        assetsLoader,
        in.monitoredFiles,
        projectPath,
        in.devSettings,
        PlayWatchService.default(targetDirectory, pollDelayMillis, logger))

      val docsLoader = new URLClassLoader(urls(in.docsClasspath), applicationLoader)
      val docsJarFile = {
        val f = in.docsClasspath.filter(_.getName.startsWith("play-docs")).head
        new JarFile(f)
      }
      val buildDocHandler = {
        val docHandlerFactoryClass = docsLoader.loadClass("play.docs.BuildDocHandlerFactory")
        val factoryMethod = docHandlerFactoryClass.getMethod("fromJar", classOf[JarFile], classOf[String])
        factoryMethod.invoke(null, docsJarFile, "play/docs/content").asInstanceOf[BuildDocHandler]
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

      new PlayDevServer {
        val buildLink = reloader

        val urlString: String = httpPort match {
          case Some(port) => s"http://localhost:$port"
          case None => s"https://localhost:${httpsPort.get}"
        }

        def close() = {
          server.stop()
          docsJarFile.close()
          reloader.close()
          sys.exit(0)
        }
      }
    } catch {
      case e: Throwable =>
        throw e
        sys.exit(-1)
    }
  }
}

final class ForkRunner(config: ForkRunner.Config) extends Actor with ActorLogging {
  import ForkRunner._, Serializers._, PlayExceptions._

  val connectorProxy = context.actorOf(SbtConnectionProxy.props(config.connector))

  private def runReload(self: ActorRef)(): Either[Throwable, PlayForkSupportResult] = {
    val expected = Promise[Either[Throwable, PlayForkSupportResult]]()
    log.debug(s"Requesting reload")
    self.tell(Reload(expected), self)
    val f = expected.future
    Await.ready(f, 3.minutes)
    f.value.get match {
      case Success(v) => v
      case Failure(t) => Left(t)
    }
  }

  private def reloading(client: ActorRef, command: ScopedKey, server: PlayDevServer, expected: Promise[Either[Throwable, PlayForkSupportResult]]): Receive = {
    log.debug(s"Doing reload")
    client ! SbtClientProxy.RequestExecution.ByScopedKey(command, None, self)

    {
      case _: SbtClientProxy.ExecutionId => // ignore?
      case SbtClientProxy.WatchEvent(`command`, result) =>
        result.resultWithCustomThrowables[PlayForkSupportResult](Serializers.throwableDeserializers) match {
          case Success(x) =>
            expected.success(Right(x))
          case Failure(x: PlayExceptionNoSource) =>
            log.error(s"PlayExceptionNoSource: ${x.getClass.getName} - $x")
            expected.success(Left(x))
          case Failure(x: PlayExceptionWithSource) =>
            log.error(s"PlayExceptionWithSource: ${x.getClass.getName} - $x")
            expected.success(Left(x))
          case Failure(x) =>
            log.error(s"Unknown failure: ${x.getClass.getName} - $x")
            expected.success(Left(x))
        }
        context.become(waitingForReload(client, command, server))
    }
  }

  private def waitingForReload(client: ActorRef, command: ScopedKey, server: PlayDevServer): Receive = {
    case _: SbtClientProxy.ExecutionId => // ignore?
    case Reload(expected) =>
      log.debug(s"Got reload")
      context.become(reloading(client, command, server, expected))
  }

  private def waitingForInitialBuild(client: ActorRef, command: ScopedKey, server: Option[PlayDevServer], taskId: Option[Long]): Receive = (server, taskId) match {
    case (Some(s), Some(tid)) =>
      client ! SbtClientProxy.RequestExecution.ByCommandOrTask(config.notifyServerStartCommand(s.urlString), None, self)
      waitingForReload(client, command, s)
    case (_, _) =>

      {
        case SbtClientProxy.ExecutionId(Success(tid), _) =>
          context.become(waitingForInitialBuild(client, command, server, Some(tid)))
        case SbtClientProxy.ExecutionId(Failure(error), _) =>
          log.error(s"Got failure on initial build -- could not get task ID.  Cannot continue[terminating]: ${error}")
          exit()
        case SbtClientProxy.WatchEvent(command, TaskSuccess(result)) =>
          log.debug(s"Got successful result from initial build: $result")
          result.value[PlayForkSupportResult] match {
            case Some(r) =>
              log.debug("Starting server")
              val server = config.serverBuilder(r)(runReload(self)_)
              context.become(waitingForInitialBuild(client, command, Some(server), taskId))
            case None =>
              log.error(s"could not decode result into PlayForkSupportResult[terminating]: $result")
              exit()
          }
        case SbtClientProxy.WatchEvent(command, TaskFailure(result)) =>
          log.error(s"Got failure on initial build.  Cannot continue[terminating]: ${result.stringValue}")
          exit()
      }
  }

  private def initialBuild(client: ActorRef, command: ScopedKey): Receive = {
    client ! SbtClientProxy.WatchTask(TaskKey[PlayForkSupportResult](command), self)

    {
      case SbtClientProxy.WatchingTask(_) =>
        log.debug(s"Doing initial build")
        client ! SbtClientProxy.RequestExecution.ByScopedKey(command, None, self)
        context.become(waitingForInitialBuild(client, command, None, None))
    }
  }

  private def lookupKey(client: ActorRef): Receive = {
    client ! SbtClientProxy.LookupScopedKey(config.command, self)

    {
      case SbtClientProxy.LookupScopedKeyResponse(command, Success(keys)) =>
        log.debug(s"Retrirved key.  Doing initial build")
        context.become(initialBuild(client, keys.head))
      case SbtClientProxy.LookupScopedKeyResponse(command, Failure(error)) =>
        log.error(s"Could not look up command[terminating]: $command - received: $error")
        exit()
    }
  }

  private def exiting: Receive = {
    connectorProxy ! SbtConnectionProxy.Close(self)

    {
      case SbtConnectionProxy.Closed =>
        context stop self
        config.latch.countDown()
    }
  }

  private def exit(): Unit = context.become(exiting)

  private def waitForClient: Receive = {
    case SbtConnectionProxy.NewClientResponse.Connected(client) =>
      log.debug(s"Got client. Looking up key for ${config.command}")
      context.become(lookupKey(client))
    case SbtConnectionProxy.NewClientResponse.Error(true, error) =>
      log.warning(s"recoverable error getting client: $error")
    case SbtConnectionProxy.NewClientResponse.Error(false, error) =>
      log.error(s"could not get client[terminating]: $error")
      exit()
  }

  def receive: Receive = {
    connectorProxy ! SbtConnectionProxy.NewClient(self)
    waitForClient
  }
}
