/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api

import akka.stream.Materializer
import play.api.i18n.MessagesApi
import play.utils.Threads

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal
import javax.xml.parsers.SAXParserFactory
import play.libs.XML.Constants
import javax.xml.XMLConstants

/** Application mode, either `DEV`, `TEST`, or `PROD`. */
object Mode extends Enumeration {
  type Mode = Value
  val Dev, Test, Prod = Value
}

/**
 * High-level API to access Play global features.
 */
object Play {

  private val logger = Logger(Play.getClass)

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
   *
   * @deprecated This is a static reference to application, use DI, since 2.5.0
   */
  @deprecated("This is a static reference to application, use DI", "2.5.0")
  def unsafeApplication: Application = privateMaybeApplication.orNull

  /**
   * Optionally returns the current running application.
   *
   * @deprecated This is a static reference to application, use DI, since 2.5.0
   */
  @deprecated("This is a static reference to application, use DI instead", "2.5.0")
  def maybeApplication: Option[Application] = privateMaybeApplication

  private[play] def privateMaybeApplication: Option[Application] = {
    Option(_currentApp) match {
      case Some(app) if !app.configuration.getOptional[Boolean]("play.globalApplication").getOrElse(true) =>
        (new RuntimeException).printStackTrace()
        sys.error("The global application is disabled. Set play.globalApplication to allow global state here")
      case opt => opt
    }
  }

  /* Used by the routes compiler to resolve an application for the injector.  Treat as private. */
  def routesCompilerMaybeApplication: Option[Application] = privateMaybeApplication

  /**
   * Implicitly import the current running application in the context.
   *
   * Note that by relying on this, your code will only work properly in
   * the context of a running application.
   *
   * @deprecated This is a static reference to application, use DI, since 2.5.0
   */
  @deprecated("This is a static reference to application, use DI instead", "2.5.0")
  implicit def current: Application = privateMaybeApplication.getOrElse(sys.error("There is no started application"))

  @volatile private var _currentApp: Application = _

  /**
   * Starts this application.
   *
   * @param app the application to start
   */
  def start(app: Application) {

    // First stop previous app if exists
    stop(_currentApp)

    _currentApp = app

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
      Threads.withContextClassLoader(app.classloader) {
        try { Await.ready(app.stop(), Duration.Inf) } catch { case NonFatal(e) => logger.warn("Error stopping application", e) }
      }
    }
    _currentApp = null
  }

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
