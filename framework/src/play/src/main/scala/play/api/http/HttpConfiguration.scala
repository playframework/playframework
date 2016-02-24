/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.http

import javax.inject.{ Singleton, Inject, Provider }

import com.typesafe.config.ConfigMemorySize
import play.api.{ PlayConfig, Application, Play, Configuration }
import play.core.netty.utils.{ ServerCookieDecoder, ClientCookieEncoder, ClientCookieDecoder, ServerCookieEncoder }

import scala.concurrent.duration.FiniteDuration

/**
 * HTTP related configuration of a Play application
 *
 * @param context The HTTP context
 * @param parser The parser configuration
 * @param session The session configuration
 * @param flash The flash configuration
 */
case class HttpConfiguration(
  context: String = "/",
  parser: ParserConfiguration = ParserConfiguration(),
  actionComposition: ActionCompositionConfiguration = ActionCompositionConfiguration(),
  cookies: CookiesConfiguration = CookiesConfiguration(),
  session: SessionConfiguration = SessionConfiguration(),
  flash: FlashConfiguration = FlashConfiguration())

/**
 * The cookies configuration
 *
 * @param strict Whether strict cookie parsing should be used. If true, will cause the entire cookie header to be
 *               discarded if a single cookie is found to be invalid.
 */
case class CookiesConfiguration(
    strict: Boolean = true) {
  def serverEncoder: ServerCookieEncoder = strict match {
    case true => ServerCookieEncoder.STRICT
    case false => ServerCookieEncoder.LAX
  }
  def clientEncoder: ClientCookieEncoder = strict match {
    case true => ClientCookieEncoder.STRICT
    case false => ClientCookieEncoder.LAX
  }
  def serverDecoder: ServerCookieDecoder = strict match {
    case true => ServerCookieDecoder.STRICT
    case false => ServerCookieDecoder.LAX
  }
  def clientDecoder: ClientCookieDecoder = strict match {
    case true => ClientCookieDecoder.STRICT
    case false => ClientCookieDecoder.LAX
  }
}

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
 * @param secure Whether the flash cookie should set the secure flag or not
 * @param httpOnly Whether the HTTP only attribute of the cookie should be set
 */
case class FlashConfiguration(cookieName: String = "PLAY_FLASH", secure: Boolean = false, httpOnly: Boolean = true)

/**
 * Configuration for body parsers.
 *
 * @param maxMemoryBuffer The maximum size that a request body that should be buffered in memory.
 * @param maxDiskBuffer The maximum size that a request body should be buffered on disk.
 */
case class ParserConfiguration(
  maxMemoryBuffer: Int = 102400,
  maxDiskBuffer: Long = 10485760)

/**
 * Configuration for action composition.
 *
 * @param controllerAnnotationsFirst If annotations put on controllers should be executed before the ones put on actions.
 * @param executeActionCreatorActionFirst If the action returned by the action creator should be
 *                                         executed before the action composition ones.
 */
case class ActionCompositionConfiguration(
  controllerAnnotationsFirst: Boolean = false,
  executeActionCreatorActionFirst: Boolean = false)

object HttpConfiguration {

  @Singleton
  class HttpConfigurationProvider @Inject() (configuration: Configuration) extends Provider[HttpConfiguration] {
    lazy val get = fromConfiguration(configuration)
  }

  def fromConfiguration(configuration: Configuration) = {
    val config = PlayConfig(configuration)
    val context = {
      val ctx = config.getDeprecated[String]("play.http.context", "application.context")
      if (!ctx.startsWith("/")) {
        throw configuration.globalError("play.http.context must start with a /")
      }
      ctx
    }

    HttpConfiguration(
      context = context,
      parser = ParserConfiguration(
        maxMemoryBuffer = config.getDeprecated[ConfigMemorySize]("play.http.parser.maxMemoryBuffer", "parsers.text.maxLength")
          .toBytes.toInt,
        maxDiskBuffer = config.get[ConfigMemorySize]("play.http.parser.maxDiskBuffer").toBytes
      ),
      actionComposition = ActionCompositionConfiguration(
        controllerAnnotationsFirst = config.get[Boolean]("play.http.actionComposition.controllerAnnotationsFirst"),
        executeActionCreatorActionFirst = config.get[Boolean]("play.http.actionComposition.executeActionCreatorActionFirst")
      ),
      cookies = CookiesConfiguration(
        strict = config.get[Boolean]("play.http.cookies.strict")
      ),
      session = SessionConfiguration(
        cookieName = config.getDeprecated[String]("play.http.session.cookieName", "session.cookieName"),
        secure = config.getDeprecated[Boolean]("play.http.session.secure", "session.secure"),
        maxAge = config.getDeprecated[Option[FiniteDuration]]("play.http.session.maxAge", "session.maxAge"),
        httpOnly = config.getDeprecated[Boolean]("play.http.session.httpOnly", "session.httpOnly"),
        domain = config.getDeprecated[Option[String]]("play.http.session.domain", "session.domain")
      ),
      flash = FlashConfiguration(
        cookieName = config.getDeprecated[String]("play.http.flash.cookieName", "flash.cookieName"),
        secure = config.get[Boolean]("play.http.flash.secure"),
        httpOnly = config.get[Boolean]("play.http.flash.httpOnly")
      )
    )
  }

  private val httpConfigurationCache = Application.instanceCache[HttpConfiguration]
  /**
   * Don't use this - only exists for transition from global state
   */
  private[play] def current = Play.privateMaybeApplication.fold(HttpConfiguration())(httpConfigurationCache)
}
