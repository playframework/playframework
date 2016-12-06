/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.http

import javax.inject.{ Inject, Provider, Singleton }

import com.typesafe.config.ConfigMemorySize
import play.api.{ Application, Configuration, Play }
import play.core.netty.utils.{ ClientCookieDecoder, ClientCookieEncoder, ServerCookieDecoder, ServerCookieEncoder }

import scala.concurrent.duration.FiniteDuration

/**
 * HTTP related configuration of a Play application
 *
 * @param context       The HTTP context
 * @param parser        The parser configuration
 * @param session       The session configuration
 * @param flash         The flash configuration
 * @param fileMimeTypes The fileMimeTypes configuration
 */
case class HttpConfiguration(
  context: String = "/",
  parser: ParserConfiguration = ParserConfiguration(),
  actionComposition: ActionCompositionConfiguration = ActionCompositionConfiguration(),
  cookies: CookiesConfiguration = CookiesConfiguration(),
  session: SessionConfiguration = SessionConfiguration(),
  flash: FlashConfiguration = FlashConfiguration(),
  fileMimeTypes: FileMimeTypesConfiguration = FileMimeTypesConfiguration())

/**
 * The cookies configuration
 *
 * @param strict Whether strict cookie parsing should be used. If true, will cause the entire cookie header to be
 *               discarded if a single cookie is found to be invalid.
 */
case class CookiesConfiguration(strict: Boolean = true) {
  val serverEncoder: ServerCookieEncoder = if (strict) ServerCookieEncoder.STRICT else ServerCookieEncoder.LAX
  val serverDecoder: ServerCookieDecoder = if (strict) ServerCookieDecoder.STRICT else ServerCookieDecoder.LAX
  val clientEncoder: ClientCookieEncoder = if (strict) ClientCookieEncoder.STRICT else ClientCookieEncoder.LAX
  val clientDecoder: ClientCookieDecoder = if (strict) ClientCookieDecoder.STRICT else ClientCookieDecoder.LAX
}

/**
 * The session configuration
 *
 * @param cookieName The name of the cookie used to store the session
 * @param secure     Whether the session cookie should set the secure flag or not
 * @param maxAge     The max age of the session, none, use "session" sessions
 * @param httpOnly   Whether the HTTP only attribute of the cookie should be set
 * @param domain     The domain to set for the session cookie, if defined
 */
case class SessionConfiguration(cookieName: String = "PLAY_SESSION", secure: Boolean = false,
  maxAge: Option[FiniteDuration] = None, httpOnly: Boolean = true,
  domain: Option[String] = None)

/**
 * The flash configuration
 *
 * @param cookieName The name of the cookie used to store the session
 * @param secure     Whether the flash cookie should set the secure flag or not
 * @param httpOnly   Whether the HTTP only attribute of the cookie should be set
 */
case class FlashConfiguration(cookieName: String = "PLAY_FLASH", secure: Boolean = false, httpOnly: Boolean = true)

/**
 * Configuration for body parsers.
 *
 * @param maxMemoryBuffer The maximum size that a request body that should be buffered in memory.
 * @param maxDiskBuffer   The maximum size that a request body should be buffered on disk.
 */
case class ParserConfiguration(
  maxMemoryBuffer: Int = 102400,
  maxDiskBuffer: Long = 10485760)

/**
 * Configuration for action composition.
 *
 * @param controllerAnnotationsFirst      If annotations put on controllers should be executed before the ones put on actions.
 * @param executeActionCreatorActionFirst If the action returned by the action creator should be
 *                                        executed before the action composition ones.
 */
case class ActionCompositionConfiguration(
  controllerAnnotationsFirst: Boolean = false,
  executeActionCreatorActionFirst: Boolean = false)

/**
 * Configuration for file MIME types, mapping from extension to content type.
 *
 * @param mimeTypes     the extension to mime type mapping.
 */
case class FileMimeTypesConfiguration(mimeTypes: Map[String, String] = Map.empty)

object HttpConfiguration {

  private val httpConfigurationCache = Application.instanceCache[HttpConfiguration]

  def fromConfiguration(config: Configuration) = {
    val context = {
      val ctx = config.getDeprecated[String]("play.http.context", "application.context")
      if (!ctx.startsWith("/")) {
        throw config.globalError("play.http.context must start with a /")
      }
      ctx
    }

    if (config.has("mimetype")) {
      throw config.globalError("mimetype replaced by play.http.fileMimeTypes map")
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
      ),
      fileMimeTypes = FileMimeTypesConfiguration(
        config.get[String]("play.http.fileMimeTypes")
        .split('\n')
        .map(_.trim)
        .filter(_.size > 0)
        .filter(_ (0) != '#')
        .map(_.split('='))
        .map(parts => parts(0) -> parts.drop(1).mkString).toMap
      )
    )
  }

  /**
   * Don't use this - only exists for transition from global state
   */
  private[play] def current = Play.privateMaybeApplication.fold(HttpConfiguration())(httpConfigurationCache)

  /**
   * For calling from Java.
   */
  def createWithDefaults() = apply()

  @Singleton
  class HttpConfigurationProvider @Inject() (configuration: Configuration) extends Provider[HttpConfiguration] {
    lazy val get = fromConfiguration(configuration)
  }

  @Singleton
  class ParserConfigurationProvider @Inject() (conf: HttpConfiguration) extends Provider[ParserConfiguration] {
    lazy val get = conf.parser
  }

  @Singleton
  class CookiesConfigurationProvider @Inject() (conf: HttpConfiguration) extends Provider[CookiesConfiguration] {
    lazy val get = conf.cookies
  }

  @Singleton
  class SessionConfigurationProvider @Inject() (conf: HttpConfiguration) extends Provider[SessionConfiguration] {
    lazy val get = conf.session
  }

  @Singleton
  class FlashConfigurationProvider @Inject() (conf: HttpConfiguration) extends Provider[FlashConfiguration] {
    lazy val get = conf.flash
  }

  @Singleton
  class ActionCompositionConfigurationProvider @Inject() (conf: HttpConfiguration) extends Provider[ActionCompositionConfiguration] {
    lazy val get = conf.actionComposition
  }

  @Singleton
  class FileMimeTypesConfigurationProvider @Inject() (conf: HttpConfiguration) extends Provider[FileMimeTypesConfiguration] {
    lazy val get = conf.fileMimeTypes
  }
}
