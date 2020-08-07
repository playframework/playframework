/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import java.util.concurrent.atomic.AtomicReference

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.stream.Materializer
import play.utils.Threads

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

import scala.util.Failure
import scala.util.Success
import scala.util.Try

/**
 * High-level API to access Play global features.
 */
object Play {
  private val logger = Logger(Play.getClass)

  private[play] val GlobalAppConfigKey = "play.allowGlobalApplication"

  private[play] def privateMaybeApplication: Try[Application] = {
    if (_currentApp.get != null) {
      Success(_currentApp.get)
    } else {
      Failure(
        new RuntimeException(
          s"""
             |The global application reference is disabled. Play's global state is deprecated and will
             |be removed in a future release. You should use dependency injection instead. To enable
             |the global application anyway, set $GlobalAppConfigKey = true.
       """.stripMargin
        )
      )
    }
  }

  /* Used by the routes compiler to resolve an application for the injector.  Treat as private. */
  def routesCompilerMaybeApplication: Option[Application] = privateMaybeApplication.toOption

  // _currentApp is an AtomicReference so that `start()` can invoke `stop()`
  // without causing a deadlock. That potential deadlock (and this derived complexity)
  // was introduced when using CoordinatedShutdown because `unsetGlobalApp(app)`
  // may run from a different thread.
  private val _currentApp: AtomicReference[Application] = new AtomicReference[Application]()

  /**
   * Sets the global application instance.
   *
   * If another app was previously started using this API and the global application is enabled, Play.stop will be
   * called on the existing application.
   *
   * @param app the application to start
   */
  def start(app: Application): Unit = synchronized {
    val globalApp = app.globalApplicationEnabled

    // Stop the current app if the new app needs to replace the current app instance
    if (globalApp && _currentApp.get != null) {
      logger.info("Stopping current application")
      stop(_currentApp.get())
    }

    app.mode match {
      case Mode.Test =>
      case mode =>
        logger.info(s"Application started ($mode)${if (!globalApp) " (no global state)" else ""}")
    }

    // Set the current app if the global application is enabled
    // Also set it if the current app is null, in order to display more useful errors if we try to use the app
    if (globalApp) {
      logger.warn(s"""
                     |You are using the deprecated global state to set and access the current running application. If you
                     |need an instance of Application, set $GlobalAppConfigKey = false and use Dependency Injection instead.
        """.stripMargin)
      _currentApp.set(app)

      // It's possible to stop the Application using Coordinated Shutdown, when that happens the Application
      // should no longer be considered the current App
      app.coordinatedShutdown.addTask(CoordinatedShutdown.PhaseBeforeActorSystemTerminate, "unregister-global-app") {
        () =>
          unsetGlobalApp(app)
          Future.successful(Done)
      }
    }
  }

  /**
   * Stops the given application.
   */
  def stop(app: Application): Unit = {
    if (app != null) {
      Threads.withContextClassLoader(app.classloader) {
        try {
          Await.ready(app.stop(), Duration.Inf)
        } catch { case NonFatal(e) => logger.warn("Error stopping application", e) }
      }
    }
  }

  private def unsetGlobalApp(app: Application) = {
    // Don't bother un-setting the current app unless it's our app
    _currentApp.compareAndSet(app, null)
  }

  /**
   * A convenient function for getting an implicit materializer from the current application
   */
  implicit def materializer(implicit app: Application): Materializer = app.materializer
}
