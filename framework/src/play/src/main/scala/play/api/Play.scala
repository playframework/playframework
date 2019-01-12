/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import java.util.concurrent.atomic.AtomicReference

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.stream.Materializer
import play.api.i18n.MessagesApi
import play.utils.Threads

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal
import javax.xml.parsers.SAXParserFactory
import play.libs.XML.Constants
import javax.xml.XMLConstants

import scala.util.{ Failure, Success, Try }

/**
 * Application mode, either `Dev`, `Test`, or `Prod`.
 *
 * @see [[play.Mode]]
 */
sealed abstract class Mode(val asJava: play.Mode)

object Mode {

  @deprecated("Use play.api.Mode instead of play.api.Mode.Mode", "2.6.0")
  type Mode = play.api.Mode

  @deprecated("Use play.api.Mode instead of play.api.Mode.Value", "2.6.0")
  type Value = play.api.Mode

  case object Dev extends play.api.Mode(play.Mode.DEV)
  case object Test extends play.api.Mode(play.Mode.TEST)
  case object Prod extends play.api.Mode(play.Mode.PROD)

  lazy val values: Set[play.api.Mode] = Set(Dev, Test, Prod)
}

/**
 * High-level API to access Play global features.
 */
object Play {

  private val logger = Logger(Play.getClass)

  private[play] val GlobalAppConfigKey = "play.allowGlobalApplication"

  /*
   * We want control over the sax parser used so we specify the factory required explicitly. We know that
   * SAXParserFactoryImpl will yield a SAXParser having looked at its source code, despite there being
   * no explicit doco stating this is the case. That said, there does not appear to be any other way than
   * declaring a factory in order to yield a parser of a specific type.
   */
  private[play] val xercesSaxParserFactory = SAXParserFactory.newInstance()
  xercesSaxParserFactory.setFeature(Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_GENERAL_ENTITIES_FEATURE, false)
  xercesSaxParserFactory.setFeature(Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_PARAMETER_ENTITIES_FEATURE, false)
  xercesSaxParserFactory.setFeature(Constants.XERCES_FEATURE_PREFIX + Constants.DISALLOW_DOCTYPE_DECL_FEATURE, true)
  xercesSaxParserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)

  /*
   * A parser to be used that is configured to ensure that no schemas are loaded.
   */
  private[play] def XML = scala.xml.XML.withSAXParser(xercesSaxParserFactory.newSAXParser())

  /**
   * Returns the currently running application, or `null` if not defined.
   */
  @deprecated("This is a static reference to application, use DI", "2.5.0")
  def unsafeApplication: Application = privateMaybeApplication.get

  /**
   * Optionally returns the current running application.
   */
  @deprecated("This is a static reference to application, use DI instead", "2.5.0")
  def maybeApplication: Option[Application] = privateMaybeApplication.toOption

  private[play] def privateMaybeApplication: Try[Application] = {
    if (_currentApp.get != null) {
      Success(_currentApp.get)
    } else {
      Failure(sys.error(
        s"""
           |The global application reference is disabled. Play's global state is deprecated and will
           |be removed in a future release. You should use dependency injection instead. To enable
           |the global application anyway, set $GlobalAppConfigKey = true.
       """.stripMargin
      ))

    }
  }

  /* Used by the routes compiler to resolve an application for the injector.  Treat as private. */
  def routesCompilerMaybeApplication: Option[Application] = privateMaybeApplication.toOption

  /**
   * Implicitly import the current running application in the context.
   *
   * Note that by relying on this, your code will only work properly in
   * the context of a running application.
   */
  @deprecated("This is a static reference to application, use DI instead", "2.5.0")
  implicit def current: Application = privateMaybeApplication.getOrElse(sys.error("There is no started application"))

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
      logger.warn(
        s"""
          |You are using the deprecated global state to set and access the current running application. If you
          |need an instance of Application, set $GlobalAppConfigKey = false and use Dependency Injection instead.
        """.stripMargin)
      _currentApp.set(app)

      // It's possible to stop the Application using Coordinated Shutdown, when that happens the Application
      // should no longer be considered the current App
      app.coordinatedShutdown.addTask(CoordinatedShutdown.PhaseBeforeActorSystemTerminate, "unregister-global-app"){ () =>
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
        try { Await.ready(app.stop(), Duration.Inf) } catch { case NonFatal(e) => logger.warn("Error stopping application", e) }
      }
    }
  }

  private def unsetGlobalApp(app: Application) = {
    // Don't bother un-setting the current app unless it's our app
    _currentApp.compareAndSet(app, null)
  }

  /**
   * Returns the name of the cookie that can be used to permanently set the user's language.
   */
  @deprecated("Use the MessagesApi itself", "2.7.0")
  def langCookieName(implicit messagesApi: MessagesApi): String =
    messagesApi.langCookieName

  /**
   * Returns whether the language cookie should have the secure flag set.
   */
  @deprecated("Use the MessagesApi itself", "2.7.0")
  def langCookieSecure(implicit messagesApi: MessagesApi): Boolean =
    messagesApi.langCookieSecure

  /**
   * Returns whether the language cookie should have the HTTP only flag set.
   */
  @deprecated("Use the MessagesApi itself", "2.7.0")
  def langCookieHttpOnly(implicit messagesApi: MessagesApi): Boolean =
    messagesApi.langCookieHttpOnly

  /**
   * A convenient function for getting an implicit materializer from the current application
   */
  implicit def materializer(implicit app: Application): Materializer = app.materializer
}