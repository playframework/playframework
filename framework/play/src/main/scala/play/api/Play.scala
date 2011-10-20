package play.api

import play.core._

import play.api.mvc._

import java.io._

import scala.collection.JavaConverters._

/**
 * High-level API to access Play global features.
 *
 * Note that this API depends of a running application.
 * You can import the currently running application in a scope using:
 * {{{
 * import play.api.Play.current
 * }}}
 */
object Play {

  /**
   * Returns the current running application, or null if not defined.
   */
  def unsafeApplication = _currentApp

  /**
   * Optionally returns the current running application.
   */
  def maybeApplication = Option(_currentApp)

  /**
   * Import implicitely the current running application in the context.
   *
   * Note that by relying on this, your code will only work properly in
   * the context of running application.
   */
  implicit def current = maybeApplication.get

  /**
   * Application mode, either DEV or PROD
   */
  object Mode extends Enumeration {
    type Mode = Value
    val Dev, Prod = Value
  }

  private[play] var _currentApp: Application = _

  /**
   * Starts this application.
   *
   * @param The application to start.
   */
  def start(app: Application) {

    // First stop previous app if exists
    stop()

    _currentApp = app

    if (app.mode == Mode.Dev) {
      println()
      println(new jline.ANSIBuffer().magenta("--- (RELOAD) ---"))
      println()
    }

    app.plugins.values.foreach(_.onStart)

    Logger("play").info("Application is started")

  }

  /**
   * Stop the current application.
   */
  def stop() {
    Option(_currentApp).map {
      _.plugins.values.foreach { p =>
        try { p.onStop } catch { case _ => }
      }
    }
  }

  /**
   * Scan the current application classloader to retrieve a resource content as stream.
   *
   * For example, retrieving a configuration file:
   * {{{
   * val maybeConf = application.resourceAsStream("conf/logger.xml")
   * }}}
   *
   * @param name Absolute name of the resource (from the classpath root).
   * @return Maybe a stream if found.
   */
  def resourceAsStream(name: String)(implicit app: Application): Option[InputStream] = {
    app.resourceAsStream(name)
  }

  /**
   * Scan the current application classloader to retrieve a resource.
   *
   * For example, retrieving a configuration file:
   * {{{
   * val maybeConf = application.resource("conf/logger.xml")
   * }}}
   *
   * @param name Absolute name of the resource (from the classpath root).
   * @return Maybe the resource URL if found.
   */
  def resource(name: String)(implicit app: Application): Option[java.net.URL] = {
    app.resource(name)
  }

  /**
   * Retrieve a file relatively to the current application root path.
   *
   * For example to retrieve a configuration file:
   * {{{
   * val myConf = application.getFile("conf/myConf.yml")
   * }}}
   *
   * @param relativePath Relative path of the file to fetch
   * @return A file instance, but it is not guaranteed that the file exist.
   */
  def getFile(relativePath: String)(implicit app: Application) = {
    app.getFile(relativePath)
  }

  /**
   * Retrieve a file relatively to the current application root path.
   *
   * For example to retrieve a configuration file:
   * {{{
   * val myConf = application.getExistingFile("conf/myConf.yml")
   * }}}
   *
   * @param relativePath Relative path of the file to fetch
   * @return Maybe an existing file.
   */
  def getExistingFile(relativePath: String)(implicit app: Application): Option[File] = {
    app.getExistingFile(relativePath)
  }

  /**
   * Get the current application.
   */
  def application(implicit app: Application) = app

  /**
   * Get the current application classloader.
   */
  def classloader(implicit app: Application) = app.classloader

  /**
   * Get the current application configuration.
   */
  def configuration(implicit app: Application) = app.configuration

  /**
   * Get the current application router.
   */
  def routes(implicit app: Application) = app.routes

  /**
   * Get the current application global settings.
   */
  def global(implicit app: Application) = app.global

  /**
   * Get the current application mode.
   */
  def mode(implicit app: Application) = app.mode

  /**
   * Is the current application is DEV mode?
   */
  def isDev(implicit app: Application) = app.mode == Play.Mode.Dev

  /**
   * Is the current application is PROD mode?
   */
  def isProd(implicit app: Application) = app.mode == Play.Mode.Prod

}
