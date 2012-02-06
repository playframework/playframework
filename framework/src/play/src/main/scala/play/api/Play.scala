package play.api

import play.core._

import play.api.mvc._

import java.io._

import scala.collection.JavaConverters._

/** Application mode, either `DEV` or `PROD`. */
object Mode extends Enumeration {
  type Mode = Value
  val Dev, Prod, Test = Value
}

/**
 * High-level API to access Play global features.
 *
 * Note that this API depends on a running application.
 * You can import the currently running application in a scope using:
 * {{{
 * import play.api.Play.current
 * }}}
 */
object Play {

  /**
   * Returns the currently running application, or `null` if not defined.
   */
  def unsafeApplication: Application = _currentApp

  /**
   * Optionally returns the current running application.
   */
  def maybeApplication: Option[Application] = Option(_currentApp)

  /**
   * Implicitly import the current running application in the context.
   *
   * Note that by relying on this, your code will only work properly in
   * the context of a running application.
   */
  implicit def current: Application = maybeApplication.getOrElse(sys.error("There is no started application"))

  private[play] var _currentApp: Application = _

  /**
   * Starts this application.
   *
   * @param the application to start
   */
  def start(app: Application) {

    // First stop previous app if exists
    stop()

    _currentApp = app

    app.plugins.foreach(_.onStart)

    app.mode match {
      case Mode.Test =>
      case mode => Logger("play").info("Application started (" + mode + ")")
    }

  }

  /**
   * Stops the current application.
   */
  def stop() {
    Option(_currentApp).map {
      _.plugins.foreach { p =>
        try { p.onStop } catch { case _ => }
      }
    }
    _currentApp = null
  }

  /**
   * Scans the current application classloader to retrieve a resources contents as a stream.
   *
   * For example, to retrieve a configuration file:
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
   * Scans the current application classloader to retrieve a resource.
   *
   * For example, to retrieve a configuration file:
   * {{{
   * val maybeConf = application.resource("conf/logger.xml")
   * }}}
   *
   * @param name absolute name of the resource (from the classpath root)
   * @return the resource URL, if found
   */
  def resource(name: String)(implicit app: Application): Option[java.net.URL] = {
    app.resource(name)
  }

  /**
   * Retrieves a file relative to the current application root path.
   *
   * For example, to retrieve a configuration file:
   * {{{
   * val myConf = application.getFile("conf/myConf.yml")
   * }}}
   *
   * @param relativePath the relative path of the file to fetch
   * @return a file instance; it is not guaranteed that the file exists
   */
  def getFile(relativePath: String)(implicit app: Application): File = {
    app.getFile(relativePath)
  }

  /**
   * Retrieves a file relative to the current application root path.
   *
   * For example, to retrieve a configuration file:
   * {{{
   * val myConf = application.getExistingFile("conf/myConf.yml")
   * }}}
   *
   * @param relativePath relative path of the file to fetch
   * @return an existing file
   */
  def getExistingFile(relativePath: String)(implicit app: Application): Option[File] = {
    app.getExistingFile(relativePath)
  }

  /**
   * Returns the current application.
   */
  def application(implicit app: Application): Application = app

  /**
   * Returns the current application classloader.
   */
  def classloader(implicit app: Application): ClassLoader = app.classloader

  /**
   * Returns the current application configuration.
   */
  def configuration(implicit app: Application): Configuration = app.configuration

  /**
   * Returns the current application router.
   */
  def routes(implicit app: Application): Option[play.core.Router.Routes] = app.routes

  /**
   * Returns the current application global settings.
   */
  def global(implicit app: Application): GlobalSettings = app.global

  /**
   * Returns the current application mode.
   */
  def mode(implicit app: Application): Mode.Mode = app.mode

  /**
   * Returns `true` if the current application is `DEV` mode.
   */
  def isDev(implicit app: Application): Boolean = (app.mode == Mode.Dev)

  /**
   * Returns `true` if the current application is `PROD` mode.
   */
  def isProd(implicit app: Application): Boolean = (app.mode == Mode.Prod)

  /**
   * Returns `true` if the current application is `TEST` mode.
   */
  def isTest(implicit app: Application): Boolean = (app.mode == Mode.Test)

}
