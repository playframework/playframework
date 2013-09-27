package play.api

import play.utils.Threads

import java.io._

import scala.util.control.NonFatal
import javax.xml.parsers.SAXParserFactory
import org.apache.xerces.impl.Constants
import javax.xml.XMLConstants

/** Application mode, either `DEV`, `TEST`, or `PROD`. */
object Mode extends Enumeration {
  type Mode = Value
  val Dev, Test, Prod = Value
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

  /*
   * A general purpose logger for Play. Intended for internal usage.
   */
  private[play] val logger = Logger("play")

  /*
   * We want control over the sax parser used so we specify the factory required explicitly. We know that
   * SAXParserFactoryImpl will yield a SAXParser having looked at its source code, despite there being
   * no explicit doco stating this is the case. That said, there does not appear to be any other way than
   * declaring a factory in order to yield a parser of a specific type.
   */
  private[play] val xercesSaxParserFactory =
    SAXParserFactory.newInstance("org.apache.xerces.jaxp.SAXParserFactoryImpl", Play.getClass.getClassLoader)
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

    // Ensure routes are eagerly loaded, so that the reverse routers are correctly initialised before plugins are
    // started.
    app.routes
    Threads.withContextClassLoader(classloader(app)) {
      app.plugins.foreach(_.onStart())
    }

    app.mode match {
      case Mode.Test =>
      case mode => logger.info("Application started (" + mode + ")")
    }

  }

  /**
   * Stops the current application.
   */
  def stop() {
    Option(_currentApp).map { app =>
      Threads.withContextClassLoader(classloader(app)) {
        app.plugins.reverse.foreach { p =>
          try { p.onStop() } catch { case NonFatal(e) => logger.warn("Error stopping plugin", e) }
        }
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

  /**
   * Returns the name of the cookie that can be used to permanently set the user's language.
   */
  def langCookieName(implicit app: Application): String = app.configuration.getString("application.lang.cookie").getOrElse("PLAY_LANG")
}
