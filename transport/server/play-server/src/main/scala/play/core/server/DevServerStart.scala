/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import java.io.File
import java.net.InetAddress

import akka.Done
import akka.annotation.InternalApi
import akka.actor.ActorSystem
import akka.actor.CoordinatedShutdown
import akka.stream.Materializer
import play.api._
import play.api.inject.DefaultApplicationLifecycle
import play.core.ApplicationProvider
import play.core.BuildLink
import play.core.SourceMapper
import play.utils.Threads

import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.Failure
import scala.util.Success
import scala.util.Try

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
  def mainDevOnlyHttpsMode(buildLink: BuildLink, httpsPort: Int, httpAddress: String): ReloadableServer = {
    mainDev(buildLink, httpAddress, -1, httpsPort)
  }

  /**
   * Provides an HTTP server for the dev environment
   *
   * <p>This method uses simple Java types so that it can be used with reflection by code
   * compiled with different versions of Scala.
   */
  def mainDevHttpMode(buildLink: BuildLink, httpPort: Int, httpAddress: String): ReloadableServer = {
    mainDev(buildLink, httpAddress, httpPort, -1)
  }

  /**
   * Provides an HTTP and HTTPS server for the dev environment
   *
   * <p>This method uses simple Java types so that it can be used with reflection by code
   * compiled with different versions of Scala.
   */
  def mainDevHttpAndHttpsMode(
      buildLink: BuildLink,
      httpPort: Int,
      httpsPort: Int,
      httpAddress: String
  ): ReloadableServer = {
    mainDev(buildLink, httpAddress, httpPort, httpsPort)
  }

  // Use -1 to opt-out of HTTP or HTTPS
  private def mainDev(
      buildLink: BuildLink,
      httpAddress: String,
      httpPort: Int,
      httpsPort: Int,
  ): ReloadableServer =
    new DevServerStart(mkServerActorSystem, Map()).mainDev(buildLink, httpAddress, httpPort, httpsPort)

  private def mkServerActorSystem(conf: Configuration) = {
    // "play.akka.dev-mode" has the priority, so if there is a conflict
    // between the actor system for dev mode and the application actor system
    // users can resolve it by add a specific configuration for dev mode.
    // We then fallback to the app configuration to avoid losing configurations
    // made using devSettings, system properties and application.conf itself.
    val devModeAkkaConfig = conf.underlying.getConfig("play.akka.dev-mode").withFallback(conf.underlying)
    ActorSystem("play-dev-mode", devModeAkkaConfig)
  }
}

@InternalApi // Reused by Lagom but otherwise not stable
final class DevServerStart(
    mkServerActorSystem: Configuration => ActorSystem,
    additionalSettings: Map[String, AnyRef],
) {
  def mainDev(
      buildLink: BuildLink,
      httpAddress: String,
      httpPort: Int,
      httpsPort: Int,
  ): ReloadableServer = {
    val classLoader = getClass.getClassLoader
    Threads.withContextClassLoader(classLoader) {
      try {
        val process    = new RealServerProcess(args = Seq.empty)
        val path: File = buildLink.projectPath

        val dirAndDevSettings: Map[String, AnyRef] =
          ServerConfig.rootDirConfig(path) ++ buildLink.settings.asScala.toMap ++ additionalSettings

        // Use plain Java call here in case of scala classloader mess
        {
          if (System.getProperty("play.debug.classpath") == "true") {
            System.out.println("\n---- Current ClassLoader ----\n")
            System.out.println(this.getClass.getClassLoader)
            System.out.println("\n---- The where is Scala? test ----\n")
            System.out.println(this.getClass.getClassLoader.getResource("scala/Predef$.class"))
          }
        }

        // A threshold for retrieving the current hostname.
        //
        // If startup takes too long, it can cause a number of issues and we try to detect it using
        // InetAddress.getLocalHost. If it takes longer than this threshold, it might be a signal
        // of a well-known problem with MacOS that might cause issues.
        val startupWarningThreshold = 1000L
        val before                  = System.currentTimeMillis()
        val address                 = InetAddress.getLocalHost
        val after                   = System.currentTimeMillis()
        if (after - before > startupWarningThreshold) {
          println(
            play.utils.Colors.red(
              s"WARNING: Retrieving local host name ${address} took more than ${startupWarningThreshold}ms, this can create problems at startup"
            )
          )
          println(
            play.utils.Colors
              .red("If you are using macOS, see https://thoeni.io/post/macos-sierra-java/ for a potential solution")
          )
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
        LoggerConfigurator(classLoader) match {
          case Some(loggerConfigurator) =>
            loggerConfigurator.init(path, Mode.Dev)
          case None =>
            println("No play.logger.configurator found: logging must be configured entirely by the application.")
        }

        println(play.utils.Colors.magenta("--- (Running the application, auto-reloading is enabled) ---"))
        println()

        // Create reloadable ApplicationProvider
        val appProvider = new ApplicationProvider {
          var lastState: Try[Application]                        = Failure(new PlayException("Not initialized", "?"))
          var lastLifecycle: Option[DefaultApplicationLifecycle] = None
          val isShutdown                                         = new AtomicBoolean(false)

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
            if (isShutdown.get()) {
              // If the app was shutdown already, we return the old app (if it exists)
              // This avoids that reload will be called which might triggers a compilation
              lastState
            } else {
              synchronized {
                buildLink.reload match {
                  case cl: ClassLoader => reload(cl) // New application classes
                  case null            => lastState  // No change in the application classes
                  case NonFatal(t)     => Failure(t) // An error we can display
                  case t: Throwable    => throw t    // An error that we can't handle
                }
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

              // First, stop the old application if it exists
              lastState.foreach(Play.stop)

              // Create the new environment
              val environment = Environment(path, projectClassloader, Mode.Dev)
              val sourceMapper = new SourceMapper {
                def sourceOf(className: String, line: Option[Int]) = {
                  Option(buildLink.findSource(className, line.map(_.asInstanceOf[Integer]).orNull)).flatMap {
                    case Array(file: File, null)          => Some((file, None))
                    case Array(file: File, line: Integer) => Some((file, Some(line)))
                    case _                                => None
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

              newApplication.coordinatedShutdown
                .addTask(CoordinatedShutdown.PhaseBeforeActorSystemTerminate, "force-reload") { () =>
                  // We'll only force a reload if the reason for shutdown is not an Application.stop
                  if (!newApplication.coordinatedShutdown.shutdownReason().contains(ApplicationStoppedReason)) {
                    buildLink.forceReload()
                  }
                  Future.successful(Done)
                }

              Play.start(newApplication)

              lastState = Success(newApplication)
              isShutdown.set(false)
              lastState
            } catch {
              // No binary dependency on play-guice
              case e if e.getClass.getName == "com.google.inject.CreationException" =>
                lastState = Failure(e)
                val hint =
                  "Hint: Maybe you have forgot to enable your service Module class via `play.modules.enabled`? (check in your project's application.conf)"
                logExceptionAndGetResult(path, e, hint)
                lastState

              case e: PlayException =>
                lastState = Failure(e)
                logExceptionAndGetResult(path, e)
                lastState

              case NonFatal(e) =>
                lastState = Failure(UnexpectedException(unexpected = Some(e)))
                logExceptionAndGetResult(path, e)
                lastState

              case e: LinkageError =>
                lastState = Failure(UnexpectedException(unexpected = Some(e)))
                logExceptionAndGetResult(path, e)
                lastState
            }
          }

          private def logExceptionAndGetResult(path: File, e: Throwable, hint: String = ""): Unit = {
            e.printStackTrace()
            println()
            println(
              play.utils.Colors.red(
                s"Stacktrace caused by project ${path.getName} (filesystem path to project is ${path.getAbsolutePath}).\n${hint}"
              )
            )
          }
        }

        // Start server with the application
        val serverConfig = ServerConfig(
          rootDir = path,
          port = if (httpPort >= 0) Some(httpPort) else None,
          sslPort = if (httpsPort >= 0) Some(httpsPort) else None,
          address = httpAddress,
          mode = Mode.Dev,
          properties = process.properties,
          configuration =
            Configuration.load(classLoader, System.getProperties, dirAndDevSettings, allowMissingApplicationConf = true)
        )

        // We *must* use a different Akka configuration in dev mode, since loading two actor systems from the same
        // config will lead to resource conflicts, for example, if the actor system is configured to open a remote port,
        // then both the dev mode and the application actor system will attempt to open that remote port, and one of
        // them will fail.
        val actorSystem = mkServerActorSystem(serverConfig.configuration)
        val serverCs    = CoordinatedShutdown(actorSystem)

        // Registering a task that invokes `Play.stop` is necessary for the scenarios where
        // the Application and the Server use separate ActorSystems (e.g. DevMode).
        serverCs.addTask(CoordinatedShutdown.PhaseServiceStop, "shutdown-application-dev-mode") { () =>
          implicit val ctx = actorSystem.dispatcher
          appProvider.lastState.foreach(Play.stop)
          appProvider.isShutdown.set(true)
          Future(Done)
        }

        val serverContext = ServerProvider.Context(
          serverConfig,
          appProvider,
          actorSystem,
          Materializer.matFromSystem(actorSystem),
          () => Future.successful(())
        )
        val serverProvider = ServerProvider.fromConfiguration(classLoader, serverConfig.configuration)
        serverProvider.createServer(serverContext)
      } catch {
        case e: ExceptionInInitializerError => throw e.getCause
      }
    }
  }
}
