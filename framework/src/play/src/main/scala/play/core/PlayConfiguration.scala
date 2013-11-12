/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core

import play.api.Play
import play.api.Application

import scala.concurrent.duration._

private[play] case class PlayConfiguration(application: Application) {

  val configuration = application.configuration

  def getString(k: String) = configuration.getString(k)
  def getInt(k: String) = configuration.getInt(k)
  def getBoolean(k: String) = configuration.getBoolean(k)
  def getMilliseconds(k: String) = configuration.getMilliseconds(k)

  lazy val ApplicationGlobal = getString("application.global")
  lazy val ApplicationRouter = getString("application.router")
  lazy val ApplicationContext = getString("application.context").map { prefix =>
    if (!prefix.startsWith("/")) {
      throw configuration.reportError("application.context", "Invalid application context")
    }
    prefix
  }.getOrElse("/")
  lazy val ApplicationLangCookie = getString("application.lang.cookie").getOrElse("PLAY_LANG")
  lazy val ApplicationSecret = getString("application.secret")
  lazy val ApplicationLangs = getString("application.langs").map { langs =>
    import play.api.i18n.Lang
    import scala.util.control.NonFatal
    langs.split(",").map(_.trim).map { lang =>
      try { Lang(lang) } catch {
        case NonFatal(e) => throw configuration.reportError("application.langs", "Invalid language code [" + lang + "]", Some(e))
      }
    }.toSeq
  }.getOrElse(Nil)

  lazy val DefaultCharset = getString("default.charset").getOrElse("utf-8")

  lazy val AssetsDefaultCache = getString("assets.defaultCache").getOrElse("max-age=3600")

  lazy val MessagesPath = getString("messages.path")

  lazy val Defaultmessagesplugin = getString("defaultmessagesplugin")

  lazy val SessionCookieName = getString("session.cookieName")
  lazy val SessionDomain = getString("session.domain")
  lazy val SessionSecure = getBoolean("session.secure")
  lazy val SessionMaxAge = getMilliseconds("session.maxAge")
    .map(Duration(_, MILLISECONDS).toSeconds.toInt)
  lazy val SessionHttpOnly = getBoolean("session.httpOnly")
  lazy val SessionUsername = getString("session.username")

  lazy val FlashCookieName = getString("flash.cookieName")

  lazy val Mimetype = getString("mimetype")

  lazy val ParsersTextMaxLength = getInt("parsers.text.maxLength")

  lazy val Trustxforwarded = getBoolean("trustxforwarded")

  lazy val Internalthreadpoolsize = getInt("internal-threadpool-size")

  lazy val Ehcacheplugin = getString("ehcacheplugin")

  lazy val Evolutionplugin = getString("evolutionplugin")
  lazy val EvolutionUseLocks = getBoolean("evolutions.use.locks")

  lazy val Dbplugin = getString("dbplugin")
}

private[play] object PlayConfiguration {

  def impl(implicit app: Application) = PlayConfiguration(app)

  private def maybeApp = Play.maybeApplication
    .map(PlayConfiguration.apply _)

  def NumberOfThreads = maybeApp
    .flatMap(_.Internalthreadpoolsize)
    .getOrElse(Runtime.getRuntime.availableProcessors)

  def SessionUsername = maybeApp
    .flatMap(_.SessionUsername)
    .getOrElse("username")

  def SessionSecure = maybeApp
    .flatMap(_.SessionSecure)
    .getOrElse(false)

  def SessionMaxAge = maybeApp
    .flatMap(_.SessionMaxAge)

  def SessionDomain = maybeApp
    .flatMap(_.SessionDomain)

  def SessionHttpOnly = maybeApp
    .flatMap(_.SessionHttpOnly)
    .getOrElse(true)

  def ApplicationContext = maybeApp
    .map(_.ApplicationContext)
    .getOrElse("/")

  def SessionCookieName = maybeApp
    .flatMap(_.SessionCookieName)
    .getOrElse("PLAY_SESSION")

  def FlashCookieName = maybeApp
    .flatMap(_.FlashCookieName)
    .getOrElse("FLASH_SESSION")

  def ParsersTextMaxLength: Int = maybeApp
    .flatMap(_.ParsersTextMaxLength)
    .getOrElse(1024 * 100)

}

