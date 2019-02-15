/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import java.io._

import akka.Done
import akka.actor.{ ActorSystem, CoordinatedShutdown }
import akka.stream.ActorMaterializer
import play.api._
import play.api.inject.DefaultApplicationLifecycle
import play.core._
import play.utils.Threads

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.util.control.NonFatal
import scala.util.{ Failure, Success, Try }

/**
 * Used to start servers in 'dev' mode, a mode where the application
 * is reloaded whenever its source changes.
 */
object DevServerStart {

  /**
   * Provides an HTTPS-only server for the dev environment.
   *
   * <p>This method uses simple Java types so that it can be used with reflection by code
   * compiled with different versions of Scala.
   */
  def mainDevOnlyHttpsMode(
    buildLink: BuildLink,
    httpsPort: Int,
    httpAddress: String): ReloadableServer = {
    mainDev(buildLink, None, Some(httpsPort), httpAddress)
  }

  /**
   * Provides an HTTP server for the dev environment
   *
   * <p>This method uses simple Java types so that it can be used with reflection by code
   * compiled with different versions of Scala.
   */
  def mainDevHttpMode(
    buildLink: BuildLink,
    httpPort: Int,
    httpAddress: String): ReloadableServer = {
    mainDev(buildLink, Some(httpPort), Option(System.getProperty("https.port")).map(Integer.parseInt), httpAddress)
  }

  private def mainDev(
    buildLink: BuildLink,
    httpPort: Option[Int],
    httpsPort: Option[Int],
    httpAddress: String): ReloadableServer = {
    val classLoader = getClass.getClassLoader
    Threads.withContextClassLoader(classLoader) {
      try {
        val process = new RealServerProcess(args = Seq.empty)
        val path: File = buildLink.projectPath

        val dirAndDevSettings: Map[String, String] = ServerConfig.rootDirConfig(path) ++ buildLink.settings.asScala.toMap

        // Use plain Java call here in case of scala classloader mess
        {
          if (System.getProperty("play.debug.classpath") == "true") {
            System.out.println("\n---- Current ClassLoader ----\n")
            System.out.println(this.getClass.getClassLoader)
            System.out.println("\n---- The where is Scala? test ----\n")
            System.out.println(this.getClass.getClassLoader.getResource("scala/Predef$.class"))
          }
        }

        // First delete the default log file for a fresh start (only in Dev Mode)
        try {
          new File(path, "logs/application.log").delete()
        } catch {
          case NonFatal(_) =>
        }

        // Configure the logger for the first time.
        // This is usually done by Application itself when it's instantiated, which for other types of ApplicationProviders,
        // is usually instantiated along with or before the provider.  But in dev mode, no application exists initially, so
        // configure it here.
        LoggerConfigurator(this.getClass.getClassLoader) match {
          case Some(loggerConfigurator) =>
            loggerConfigurator.init(path, Mode.Dev)
          case None =>
            System.out.println("No play.logger.configurator found: logging must be configured entirely by the application.")
        }

        println(play.utils.Colors.magenta("--- (Running the application, auto-reloading is enabled) ---"))
        println()

        // Create reloadable ApplicationProvider
        val appProvider = new ApplicationProvider {
          // Use a stamped lock over a synchronized block so we can better control concurrency and avoid
          // blocking.  This improves performance from 4851.53 req/s to 7133.80 req/s and fixes #7614.
          // Arguably performance shouldn't matter because load tests should be run against a production
          // configuration, but there's no point in making it slower than it has to be...
          val sl = new java.util.concurrent.locks.StampedLock

          var lastState: Try[Application] = Failure(new PlayException("Not initialized", "?"))
          var lastLifecycle: Option[DefaultApplicationLifecycle] = None
          var currentWebCommands: Option[WebCommands] = None

          override def current: Option[Application] = lastState.toOption

          /**
           * Calls the BuildLink to recompile the application if files have changed and constructs a new application
           * using the new classloader. Returns the existing application if nothing has changed.
           *
           * @return a Try, which is either a Success containing the application or Failure with exception.
           * When a Failure is returned, the server handles it by returning an error page, so that the error
           * can be displayed in the user's browser. Failure is usually the result of a compilation error.
           */
          def get: Try[Application] = {
            // Block here while the reload happens. Reloading may take seconds or minutes
            // so this is a potentially very long operation!
            // TODO: Make this method return a Future[Application] so we don't need to block more than one thread.
            synchronized {
              buildLink.reload match {
                case cl: ClassLoader => reload(cl) // New application classes
                case null => lastState // No change in the application classes
                case NonFatal(t) => Failure(t) // An error we can display
                case t: Throwable => throw t // An error that we can't handle
              }
            }

          }

          def reload(projectClassloader: ClassLoader): Try[Application] = {
            try {
              if (lastState.isSuccess) {
                println()
                println(play.utils.Colors.magenta("--- (RELOAD) ---"))
                println()
              }

              val reloadable = this

              // First, stop the old application if it exists
              lastState.foreach(Play.stop)

              // Basically no matter if the last state was a Success, we need to
              // call all remaining hooks
              lastLifecycle.foreach(cycle => Await.result(cycle.stop(), 10.minutes))

              // Create the new environment
              val environment = Environment(path, projectClassloader, Mode.Dev)
              val sourceMapper = new SourceMapper {
                def sourceOf(className: String, line: Option[Int]) = {
                  Option(buildLink.findSource(className, line.map(_.asInstanceOf[java.lang.Integer]).orNull)).flatMap {
                    case Array(file: java.io.File, null) => Some((file, None))
                    case Array(file: java.io.File, line: java.lang.Integer) => Some((file, Some(line)))
                    case _ => None
                  }
                }
              }

              val lifecycle = new DefaultApplicationLifecycle()
              lastLifecycle = Some(lifecycle)

              val newApplication: Application = Threads.withContextClassLoader(projectClassloader) {
                val context = ApplicationLoader.Context.create(
                  environment,
                  initialSettings = dirAndDevSettings,
                  lifecycle = lifecycle,
                  devContext = Some(ApplicationLoader.DevContext(sourceMapper, buildLink))
                )
                val loader = ApplicationLoader(context)
                loader.load(context)
              }

              newApplication.coordinatedShutdown.addTask(CoordinatedShutdown.PhaseBeforeActorSystemTerminate, "force-reload") { () =>
                // We'll only force a reload if the reason for shutdown is not an Application.stop
                if (!newApplication.coordinatedShutdown.shutdownReason().contains(ApplicationStoppedReason)) {
                  buildLink.forceReload()
                }
                Future.successful(Done)
              }

              Play.start(newApplication)
              lastState = Success(newApplication)
              lastState
            } catch {
              case e: PlayException => {
                lastState = Failure(e)
                lastState
              }
              case NonFatal(e) => {
                lastState = Failure(UnexpectedException(unexpected = Some(e)))
                lastState
              }
              case e: LinkageError => {
                lastState = Failure(UnexpectedException(unexpected = Some(e)))
                lastState
              }
            }
          }
        }

        // Start server with the application
        val serverConfig = ServerConfig(
          rootDir = path,
          port = httpPort,
          sslPort = httpsPort,
          address = httpAddress,
          mode = Mode.Dev,
          properties = process.properties,
          configuration = Configuration.load(classLoader, System.getProperties, dirAndDevSettings, allowMissingApplicationConf = true)
        )

        // We *must* use a different Akka configuration in dev mode, since loading two actor systems from the same
        // config will lead to resource conflicts, for example, if the actor system is configured to open a remote port,
        // then both the dev mode and the application actor system will attempt to open that remote port, and one of
        // them will fail.
        val devModeAkkaConfig = {
          serverConfig
            .configuration
            .underlying
            // "play.akka.dev-mode" has the priority, so if there is a conflict
            // between the actor system for dev mode and the application actor system
            // users can resolve it by add a specific configuration for dev mode.
            .getConfig("play.akka.dev-mode")
            // We then fallback to the app configuration to avoid losing configurations
            // made using devSettings, system properties and application.conf itself.
            .withFallback(serverConfig.configuration.underlying)
        }
        val actorSystem = ActorSystem("play-dev-mode", devModeAkkaConfig)
        val serverCs = CoordinatedShutdown(actorSystem)

        // Registering a task that invokes `Play.stop` is necessary for the scenarios where
        // the Application and the Server use separate ActorSystems (e.g. DevMode).
        serverCs.addTask(CoordinatedShutdown.PhaseServiceStop, "shutdown-application-dev-mode") {
          () =>
            implicit val ctx = actorSystem.dispatcher
            val stoppedApp = appProvider.get.map(Play.stop)
            Future.fromTry(stoppedApp).map(_ => Done)
        }

        val serverContext = ServerProvider.Context(serverConfig, appProvider, actorSystem,
          ActorMaterializer()(actorSystem), () => Future.successful(()))
        val serverProvider = ServerProvider.fromConfiguration(classLoader, serverConfig.configuration)
        serverProvider.createServer(serverContext)
      } catch {
        case e: ExceptionInInitializerError => throw e.getCause
      }

    }
  }

}
