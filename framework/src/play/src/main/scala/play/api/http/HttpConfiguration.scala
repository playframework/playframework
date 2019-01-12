/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

import com.typesafe.config.ConfigMemorySize
import javax.inject.{ Inject, Provider, Singleton }
import org.slf4j.LoggerFactory
import play.api._
import play.api.libs.Codecs
import play.api.mvc.Cookie.SameSite
import play.core.cookie.encoding.{ ClientCookieDecoder, ClientCookieEncoder, ServerCookieDecoder, ServerCookieEncoder }

import scala.concurrent.duration._
import scala.util.{ Failure, Success }

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
    fileMimeTypes: FileMimeTypesConfiguration = FileMimeTypesConfiguration(),
    secret: SecretConfiguration = SecretConfiguration())

/**
 * The application secret. Must be set. A value of "changeme" will cause the application to fail to start in
 * production.
 *
 * With the Play secret we want to:
 *
 * 1. Encourage the practice of *not* using the same secret in dev and prod.
 * 2. Make it obvious that the secret should be changed.
 * 3. Ensure that in dev mode, the secret stays stable across restarts.
 * 4. Ensure that in dev mode, sessions do not interfere with other applications that may be or have been running
 *   on localhost.  Eg, if I start Play app 1, and it stores a PLAY_SESSION cookie for localhost:9000, then I stop
 *   it, and start Play app 2, when it reads the PLAY_SESSION cookie for localhost:9000, it should not see the
 *   session set by Play app 1.  This can be achieved by using different secrets for the two, since if they are
 *   different, they will simply ignore the session cookie set by the other.
 *
 * To achieve 1 and 2, we will, in Activator templates, set the default secret to be "changeme".  This should make
 * it obvious that the secret needs to be changed and discourage using the same secret in dev and prod.
 *
 * For safety, if the secret is not set, or if it's changeme, and we are in prod mode, then we will fail fatally.
 * This will further enforce both 1 and 2.
 *
 * To achieve 3, if in dev or test mode, if the secret is either changeme or not set, we will generate a secret
 * based on the location of application.conf.  This should be stable across restarts for a given application.
 *
 * To achieve 4, using the location of application.conf to generate the secret should ensure this.
 *
 * Play secret is checked for a minimum length in production:
 *
 * 1. If the key is fifteen characters or fewer, a warning will be logged.
 * 2. If the key is eight characters or fewer, then an error is thrown and the configuration is invalid.
 *
 * @param secret   the application secret
 * @param provider the JCE provider to use. If null, uses the platform default
 */
case class SecretConfiguration(secret: String = "changeme", provider: Option[String] = None)

object SecretConfiguration {

  // https://crypto.stackexchange.com/a/34866 = 32 bytes (256 bits)
  // https://security.stackexchange.com/a/11224 = (128 bits is more than enough)
  // but if we have less than 8 bytes in production then it's not even 64 bits.
  // which is almost certainly not from base64'ed /dev/urandom in any case, and is most
  // probably a hardcoded text password.
  // https://tools.ietf.org/html/rfc2898#section-4.1
  val SHORTEST_SECRET_LENGTH = 9

  // https://crypto.stackexchange.com/a/34866 = 32 bytes (256 bits)
  // https://security.stackexchange.com/a/11224 = (128 bits is more than enough)
  // 86 bits of random input is enough for a secret.  This rounds up to 11 bytes.
  // If we assume base64 encoded input, this comes out to at least 15 bytes, but
  // it's highly likely to be a user inputted string, which has much, much lower
  // entropy.
  val SHORT_SECRET_LENGTH = 16

}

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
 * @param path       The path for which this cookie is valid
 * @param sameSite   The cookie's SameSite attribute
 * @param jwt        The JWT specific information
 */
case class SessionConfiguration(
    cookieName: String = "PLAY_SESSION",
    secure: Boolean = false,
    maxAge: Option[FiniteDuration] = None,
    httpOnly: Boolean = true,
    domain: Option[String] = None,
    path: String = "/",
    sameSite: Option[SameSite] = Some(SameSite.Lax),
    jwt: JWTConfiguration = JWTConfiguration()
)

/**
 * The flash configuration
 *
 * @param cookieName The name of the cookie used to store the session
 * @param secure     Whether the flash cookie should set the secure flag or not
 * @param httpOnly   Whether the HTTP only attribute of the cookie should be set
 * @param domain     The domain to set for the session cookie, if defined
 * @param path       The path for which this cookie is valid
 * @param sameSite   The cookie's SameSite attribute
 * @param jwt        The JWT specific information
 */
case class FlashConfiguration(
    cookieName: String = "PLAY_FLASH",
    secure: Boolean = false,
    httpOnly: Boolean = true,
    domain: Option[String] = None,
    path: String = "/",
    sameSite: Option[SameSite] = Some(SameSite.Lax),
    jwt: JWTConfiguration = JWTConfiguration()
)

/**
 * Configuration for body parsers.
 *
 * @param maxMemoryBuffer The maximum size that a request body that should be buffered in memory.
 * @param maxDiskBuffer   The maximum size that a request body should be buffered on disk.
 */
case class ParserConfiguration(
    maxMemoryBuffer: Long = 102400,
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

  private val logger = LoggerFactory.getLogger(classOf[HttpConfiguration])
  private val httpConfigurationCache = Application.instanceCache[HttpConfiguration]

  def parseSameSite(config: Configuration, key: String): Option[SameSite] = {
    config.get[Option[String]](key).flatMap { value =>
      val result = SameSite.parse(value)
      if (result.isEmpty) {
        logger.warn(
          s"""Assuming $key = null, since "$value" is not a valid SameSite value (${SameSite.values.mkString(", ")})"""
        )
      }
      result
    }
  }

  def parseFileMimeTypes(config: Configuration): Map[String, String] = config.get[String]("play.http.fileMimeTypes").split('\n').flatMap { l =>
    val line = l.trim

    line.splitAt(1) match {
      case ("", "") => Option.empty[(String, String)] // blank
      case ("#", _) => Option.empty[(String, String)] // comment

      case _ => // "foo=bar".span(_ != '=') -> (foo,=bar)
        line.span(_ != '=') match {
          case (key, v) => Some(key -> v.drop(1)) // '=' prefix
          case _ => Option.empty[(String, String)] // skip invalid
        }
    }
  }(scala.collection.breakOut)

  def fromConfiguration(config: Configuration, environment: Environment) = {

    def getPath(key: String, deprecatedKey: Option[String] = None): String = {
      val path = deprecatedKey match {
        case Some(depKey) => config.getDeprecated[String](key, depKey)
        case None => config.get[String](key)
      }
      if (!path.startsWith("/")) {
        throw config.globalError(s"$key must start with a /")
      }
      path
    }

    val context = getPath("play.http.context", Some("application.context"))
    val sessionPath = getPath("play.http.session.path")
    val flashPath = getPath("play.http.flash.path")

    if (config.has("mimetype")) {
      throw config.globalError("mimetype replaced by play.http.fileMimeTypes map")
    }

    HttpConfiguration(
      context = context,
      parser = ParserConfiguration(
        maxMemoryBuffer = config.getDeprecated[ConfigMemorySize]("play.http.parser.maxMemoryBuffer", "parsers.text.maxLength").toBytes,
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
        domain = config.getDeprecated[Option[String]]("play.http.session.domain", "session.domain"),
        sameSite = parseSameSite(config, "play.http.session.sameSite"),
        path = sessionPath,
        jwt = JWTConfigurationParser(config, "play.http.session.jwt")
      ),
      flash = FlashConfiguration(
        cookieName = config.getDeprecated[String]("play.http.flash.cookieName", "flash.cookieName"),
        secure = config.get[Boolean]("play.http.flash.secure"),
        httpOnly = config.get[Boolean]("play.http.flash.httpOnly"),
        domain = config.get[Option[String]]("play.http.flash.domain"),
        sameSite = parseSameSite(config, "play.http.flash.sameSite"),
        path = flashPath,
        jwt = JWTConfigurationParser(config, "play.http.flash.jwt")
      ),
      fileMimeTypes = FileMimeTypesConfiguration(
        parseFileMimeTypes(config)
      ),
      secret = getSecretConfiguration(config, environment)
    )
  }

  private def getSecretConfiguration(config: Configuration, environment: Environment): SecretConfiguration = {
    val Blank = """\s*""".r

    val secret = config.getDeprecated[Option[String]]("play.http.secret.key", "play.crypto.secret", "application.secret") match {
      case (Some("changeme") | Some(Blank()) | None) if environment.mode == Mode.Prod =>
        val message =
          """
            |The application secret has not been set, and we are in prod mode. Your application is not secure.
            |To set the application secret, please read http://playframework.com/documentation/latest/ApplicationSecret
          """.stripMargin
        throw config.reportError("play.http.secret", message)

      case Some(s) if s.length < SecretConfiguration.SHORTEST_SECRET_LENGTH && environment.mode == Mode.Prod =>
        val message =
          """
            |The application secret is too short and does not have the recommended amount of entropy.  Your application is not secure.
            |To set the application secret, please read http://playframework.com/documentation/latest/ApplicationSecret
          """.stripMargin
        throw config.reportError("play.http.secret", message)

      case Some(s) if s.length < SecretConfiguration.SHORT_SECRET_LENGTH && environment.mode == Mode.Prod =>
        val message =
          """
            |Your secret key is very short, and may be vulnerable to dictionary attacks.  Your application may not be secure.
            |The application secret should ideally be 32 bytes of completely random input, encoded in base64.
            |To set the application secret, please read http://playframework.com/documentation/latest/ApplicationSecret
          """.stripMargin
        logger.warn(message)
        s

      case Some(s) if s.length < SecretConfiguration.SHORTEST_SECRET_LENGTH && !s.equals("changeme") && s.trim.nonEmpty && environment.mode == Mode.Dev =>
        val message =
          """
            |The application secret is too short and does not have the recommended amount of entropy.  Your application is not secure
            |and it will fail to start in production mode.
            |To set the application secret, please read http://playframework.com/documentation/latest/ApplicationSecret
          """.stripMargin
        logger.warn(message)
        s

      case Some(s) if s.length < SecretConfiguration.SHORT_SECRET_LENGTH && !s.equals("changeme") && s.trim.nonEmpty && environment.mode == Mode.Dev =>
        val message =
          """
            |Your secret key is very short, and may be vulnerable to dictionary attacks.  Your application may not be secure.
            |The application secret should ideally be 32 bytes of completely random input, encoded in base64. While the application
            |will be able to start in production mode, you will also see a warning when it is starting.
            |To set the application secret, please read http://playframework.com/documentation/latest/ApplicationSecret
          """.stripMargin
        logger.warn(message)
        s

      case Some("changeme") | Some(Blank()) | None =>
        val appConfLocation = environment.resource("application.conf")
        // Try to generate a stable secret. Security is not the issue here, since this is just for tests and dev mode.
        val secret = appConfLocation.fold(
          // No application.conf?  Oh well, just use something hard coded.
          "she sells sea shells on the sea shore"
        )(_.toString)
        val md5Secret = Codecs.md5(secret)
        logger.debug(s"Generated dev mode secret $md5Secret for app at ${appConfLocation.getOrElse("unknown location")}")
        md5Secret
      case Some(s) => s
    }

    val provider = config.getDeprecated[Option[String]]("play.http.secret.provider", "play.crypto.provider")

    SecretConfiguration(String.valueOf(secret), provider)
  }

  /**
   * Don't use this - only exists for transition from global state
   */
  private[play] def current: HttpConfiguration = Play.privateMaybeApplication match {
    case Success(app) => httpConfigurationCache(app)
    case Failure(_) => HttpConfiguration()
  }

  /**
   * For calling from Java.
   */
  def createWithDefaults() = apply()

  @Singleton
  class HttpConfigurationProvider @Inject() (configuration: Configuration, environment: Environment) extends Provider[HttpConfiguration] {
    lazy val get = fromConfiguration(configuration, environment)
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

  @Singleton
  class SecretConfigurationProvider @Inject() (conf: HttpConfiguration) extends Provider[SecretConfiguration] {
    lazy val get: SecretConfiguration = conf.secret
  }
}

/**
 * The JSON Web Token configuration
 *
 * @param signatureAlgorithm The signature algorithm used to sign the JWT
 * @param expiresAfter The period of time after which the JWT expires, if any.
 * @param clockSkew The amount of clock skew to permit for expiration / not before checks
 * @param dataClaim The claim key corresponding to the data map passed in by the user
 */
case class JWTConfiguration(
    signatureAlgorithm: String = "HS256",
    expiresAfter: Option[FiniteDuration] = None,
    clockSkew: FiniteDuration = 30.seconds,
    dataClaim: String = "data"
)

object JWTConfigurationParser {
  def apply(config: Configuration, parent: String): JWTConfiguration = {
    JWTConfiguration(
      signatureAlgorithm = config.get[String](s"${parent}.signatureAlgorithm"),
      expiresAfter = config.get[Option[FiniteDuration]](s"${parent}.expiresAfter"),
      clockSkew = config.get[FiniteDuration](s"${parent}.clockSkew"),
      dataClaim = config.get[String](s"${parent}.dataClaim")
    )
  }
}
