/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.crypto

import javax.inject.{ Inject, Provider, Singleton }

import org.apache.commons.codec.digest.DigestUtils
import play.api._

/**
 * Configuration for Crypto
 *
 * @param secret The application secret
 * @param provider The crypto provider to use
 */
case class CryptoConfig(secret: String, provider: Option[String] = None)

@Singleton
class CryptoConfigParser @Inject() (environment: Environment, config: Configuration) extends Provider[CryptoConfig] {

  lazy val get = {

    /*
     * The Play secret.
     *
     * We want to:
     *
     * 1) Encourage the practice of *not* using the same secret in dev and prod.
     * 2) Make it obvious that the secret should be changed.
     * 3) Ensure that in dev mode, the secret stays stable across restarts.
     * 4) Ensure that in dev mode, sessions do not interfere with other applications that may be or have been running
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
     */
    val secret = config.getDeprecated[Option[String]]("play.crypto.secret", "application.secret") match {
      case (Some("changeme") | Some(Blank()) | None) if environment.mode == Mode.Prod =>
        logger.error("The application secret has not been set, and we are in prod mode. Your application is not secure.")
        logger.error("To set the application secret, please read http://playframework.com/documentation/latest/ApplicationSecret")
        throw new PlayException("Configuration error", "Application secret not set")
      case Some("changeme") | Some(Blank()) | None =>
        val appConfLocation = environment.resource("application.conf")
        // Try to generate a stable secret. Security is not the issue here, since this is just for tests and dev mode.
        val secret = appConfLocation.fold(
          // No application.conf?  Oh well, just use something hard coded.
          "she sells sea shells on the sea shore"
        )(_.toString)
        val md5Secret = DigestUtils.md5Hex(secret)
        logger.debug(s"Generated dev mode secret $md5Secret for app at ${appConfLocation.getOrElse("unknown location")}")
        md5Secret
      case Some(s) => s
    }

    val provider = config.get[Option[String]]("play.crypto.provider")

    CryptoConfig(secret, provider)
  }
  private val Blank = """\s*""".r
  private val logger = Logger(classOf[CryptoConfigParser])
}

/**
 * Exception thrown by the Crypto APIs.
 *
 * @param message The error message.
 * @param throwable The Throwable associated with the exception.
 */
class CryptoException(val message: String = null, val throwable: Throwable = null) extends RuntimeException(message, throwable)
