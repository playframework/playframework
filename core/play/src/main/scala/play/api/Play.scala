/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import javax.xml.parsers.SAXParserFactory
import javax.xml.XMLConstants

import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.util.control.NonFatal

import org.apache.pekko.stream.Materializer
import play.api.i18n.MessagesApi
import play.libs.XML.Constants
import play.utils.Threads

/**
 * High-level API to access Play global features.
 */
object Play {
  private val logger = Logger(Play.getClass)

  private[play] lazy val xercesSaxParserFactory = {
    val saxParserFactory = SAXParserFactory.newInstance()
    saxParserFactory.setFeature(Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_GENERAL_ENTITIES_FEATURE, false)
    saxParserFactory.setFeature(Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_PARAMETER_ENTITIES_FEATURE, false)
    saxParserFactory.setFeature(Constants.XERCES_FEATURE_PREFIX + Constants.DISALLOW_DOCTYPE_DECL_FEATURE, true)
    saxParserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    saxParserFactory
  }

  /*
   * A parser to be used that is configured to ensure that no schemas are loaded.
   */
  private[play] def XML = scala.xml.XML.withSAXParser(xercesSaxParserFactory.newSAXParser())

  /**
   * Starts the application instance.
   *
   * @param app the application to start
   */
  def start(app: Application): Unit = synchronized {
    app.mode match {
      case Mode.Test =>
      case mode =>
        logger.info(s"Application started ($mode)")
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
