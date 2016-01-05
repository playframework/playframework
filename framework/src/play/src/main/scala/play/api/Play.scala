/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import akka.stream.Materializer
import play.api.i18n.MessagesApi
import play.utils.Threads

import java.io._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
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

  private val logger = Logger(Play.getClass)

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

  @volatile private[play] var _currentApp: Application = _

  /**
   * Starts this application.
   *
   * @param app the application to start
   */
  def start(app: Application) {

    // First stop previous app if exists
    stop(_currentApp)

    _currentApp = app

    Threads.withContextClassLoader(classloader(app)) {
      // Call before start now
      app.global.beforeStart(app)

      // Ensure routes are eagerly loaded, so that the reverse routers are
      // correctly initialised before plugins are started.
      app.routes

      // If the global plugin is loaded, then send it a start now.
      app.global.onStart(app)
    }

    app.mode match {
      case Mode.Test =>
      case mode => logger.info("Application started (" + mode + ")")
    }

  }

  /**
   * Stops the given application.
   */
  def stop(app: Application) {
    if (app != null) {
      Threads.withContextClassLoader(classloader(app)) {
        app.global.onStop(app)
        try { Await.ready(app.stop(), Duration.Inf) } catch { case NonFatal(e) => logger.warn("Error stopping application", e) }
      }
    }
    _currentApp = null
  }

  /**
   * @deprecated inject the [[ play.api.Environment ]] instead
   */
  @Deprecated
  def resourceAsStream(name: String)(implicit app: Application): Option[InputStream] = {
    app.resourceAsStream(name)
  }

  /**
   * @deprecated inject the [[ play.api.Environment ]] instead
   */
  @Deprecated
  def resource(name: String)(implicit app: Application): Option[java.net.URL] = {
    app.resource(name)
  }

  /**
   * @deprecated inject the [[ play.api.Environment ]] instead
   */
  @Deprecated
  def getFile(relativePath: String)(implicit app: Application): File = {
    app.getFile(relativePath)
  }

  /**
   * @deprecated inject the [[ play.api.Environment ]] instead
   */
  @Deprecated
  def getExistingFile(relativePath: String)(implicit app: Application): Option[File] = {
    app.getExistingFile(relativePath)
  }

  /**
   * @deprecated inject the [[ play.api.Application ]] instead
   */
  @Deprecated
  def application(implicit app: Application): Application = app

  /**
   * @deprecated inject the [[ play.api.Environment ]] instead
   */
  @Deprecated
  def classloader(implicit app: Application): ClassLoader = app.classloader

  /**
   * @deprecated inject the [[ play.api.Configuration ]] instead
   */
  @Deprecated
  def configuration(implicit app: Application): Configuration = app.configuration

  /**
   * @deprecated inject the [[ play.api.routing.Router ]] instead
   */
  @Deprecated
  def routes(implicit app: Application): play.api.routing.Router = app.routes

  /**
   * @deprecated inject the [[ play.api.Environment ]] instead
   */
  @Deprecated
  def mode(implicit app: Application): Mode.Mode = app.mode

  /**
   * @deprecated inject the [[ play.api.Environment ]] instead
   */
  @Deprecated
  def isDev(implicit app: Application): Boolean = (app.mode == Mode.Dev)

  /**
   * @deprecated inject the [[ play.api.Environment ]] instead
   */
  @Deprecated
  def isProd(implicit app: Application): Boolean = (app.mode == Mode.Prod)

  /**
   * @deprecated inject the [[ play.api.Environment ]] instead
   */
  @Deprecated
  def isTest(implicit app: Application): Boolean = (app.mode == Mode.Test)

  /**
   * Returns the name of the cookie that can be used to permanently set the user's language.
   */
  def langCookieName(implicit messagesApi: MessagesApi): String =
    messagesApi.langCookieName

  /**
   * Returns whether the language cookie should have the secure flag set.
   */
  def langCookieSecure(implicit messagesApi: MessagesApi): Boolean =
    messagesApi.langCookieSecure

  /**
   * Returns whether the language cookie should have the HTTP only flag set.
   */
  def langCookieHttpOnly(implicit messagesApi: MessagesApi): Boolean =
    messagesApi.langCookieHttpOnly

  /**
   * A convenient function for getting an implicit materializer from the current application
   */
  implicit def materializer(implicit app: Application): Materializer = app.materializer
}
