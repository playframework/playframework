/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.http

import javax.inject.{ Singleton, Inject, Provider }

import play.api.{ Application, Play, Configuration }

import scala.concurrent.duration.FiniteDuration

/**
 * HTTP related configuration of a Play application
 *
 * @param context The HTTP context
 * @param session The session configuration
 */
case class HttpConfiguration(context: String = "/", session: SessionConfiguration = SessionConfiguration(),
  flash: FlashConfiguration = FlashConfiguration())

/**
 * The session configuration
 *
 * @param cookieName The name of the cookie used to store the session
 * @param secure Whether the session cookie should set the secure flag or not
 * @param maxAge The max age of the session, none, use "session" sessions
 * @param httpOnly Whether the HTTP only attribute of the cookie should be set
 * @param domain The domain to set for the session cookie, if defined
 */
case class SessionConfiguration(cookieName: String = "PLAY_SESSION", secure: Boolean = false,
  maxAge: Option[FiniteDuration] = None, httpOnly: Boolean = true,
  domain: Option[String] = None)

/**
 * The flash configuration
 *
 * @param cookieName The name of the cookie used to store the session
 */
case class FlashConfiguration(cookieName: String = "PLAY_FLASH", secure: Boolean = false)

object HttpConfiguration {

  @Singleton
  class HttpConfigurationProvider @Inject() (configuration: Configuration) extends Provider[HttpConfiguration] {
    lazy val get = fromConfiguration(configuration)
  }

  def fromConfiguration(configuration: Configuration) = {
    val context = {
      val ctx = configuration.getDeprecatedString("play.http.context", "application.context")
      if (!ctx.startsWith("/")) {
        throw configuration.globalError("play.http.context must start with a /")
      }
      ctx
    }

    HttpConfiguration(
      context = context,
      session = SessionConfiguration(
        cookieName = configuration.getDeprecatedString("play.http.session.cookieName", "session.cookieName"),
        secure = configuration.getDeprecatedBoolean("play.http.session.secure", "session.secure"),
        maxAge = configuration.getDeprecatedDurationOpt("play.http.session.maxAge", "session.maxAge"),
        httpOnly = configuration.getDeprecatedBoolean("play.http.session.httpOnly", "session.httpOnly"),
        domain = configuration.getDeprecatedStringOpt("play.http.session.domain", "session.domain")
      ),
      flash = FlashConfiguration(
        cookieName = configuration.getDeprecatedString("play.http.flash.cookieName", "flash.cookieName"),
        secure = configuration.getBoolean("play.http.flash.secure").getOrElse(false)
      )
    )
  }

  private val httpConfigurationCache = Application.instanceCache[HttpConfiguration]
  /**
   * Don't use this - only exists for transition from global state
   */
  def current = Play.maybeApplication.fold(HttpConfiguration())(httpConfigurationCache)
}
