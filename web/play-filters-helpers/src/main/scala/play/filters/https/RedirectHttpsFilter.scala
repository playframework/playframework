/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.https

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import play.api.http.HeaderNames._
import play.api.http.Status._
import play.api.inject.SimpleModule
import play.api.inject.bind
import play.api.mvc._
import play.api.Configuration
import play.api.Environment
import play.api.Mode
import play.api.Logger
import play.api.http.HeaderNames

/**
 * A filter that redirects HTTP requests to https requests.
 *
 * To enable this filter, please add it to to your application.conf file using
 * "play.filters.enabled+=play.filters.https.RedirectHttpsFilter"
 *
 * For documentation on configuring this filter, please see the Play documentation at
 * https://www.playframework.com/documentation/latest/RedirectHttpsFilter
 */
@Singleton
class RedirectHttpsFilter @Inject() (config: RedirectHttpsConfiguration) extends EssentialFilter {
  import RedirectHttpsKeys._
  import config._

  private val logger = Logger(getClass)

  private[this] lazy val stsHeaders = {
    if (!redirectEnabled) Seq.empty
    else strictTransportSecurity.toSeq.map(STRICT_TRANSPORT_SECURITY -> _)
  }

  @inline
  private[this] def shouldRedirect(req: RequestHeader) = {
    if (xForwardedProtoEnabled) req.headers.get(HeaderNames.X_FORWARDED_PROTO).contains("http")
    else true
  }

  @inline
  private[this] def isSecure(req: RequestHeader) =
    req.secure || req.headers.get(HeaderNames.X_FORWARDED_PROTO).contains("https")

  override def apply(next: EssentialAction): EssentialAction = EssentialAction { req =>
    import play.api.libs.streams.Accumulator
    import play.core.Execution.Implicits.trampoline
    if (isSecure(req)) {
      next(req).map(_.withHeaders(stsHeaders: _*))
    } else if (isExcluded(req)) {
      logger.debug(s"Not redirecting to HTTPS because the path is included in exclude paths")
      next(req)
    } else {
      val redirect = shouldRedirect(req)
      if (redirectEnabled && redirect) {
        Accumulator.done(Results.Redirect(createHttpsRedirectUrl(req), redirectStatusCode))
      } else {
        if (redirect) {
          logger.debug(s"Not redirecting to HTTPS because $redirectEnabledPath flag is not set.")
        } else {
          logger.debug(
            s"Not redirecting to HTTPS because $forwardedProtoEnabled flag is set and " +
              "X-Forwarded-Proto is not present."
          )
        }
        next(req)
      }
    }
  }

  protected def createHttpsRedirectUrl(req: RequestHeader): String = {
    import req.domain
    import req.uri
    sslPort match {
      case None | Some(443) =>
        s"https://$domain$uri"
      case Some(port) =>
        s"https://$domain:$port$uri"
    }
  }

  protected def isExcluded(req: RequestHeader): Boolean = {
    config.excludePaths.contains(req.path)
  }
}

case class RedirectHttpsConfiguration(
    strictTransportSecurity: Option[String] = Some("max-age=31536000; includeSubDomains"),
    redirectStatusCode: Int = PERMANENT_REDIRECT,
    sslPort: Option[Int] = None, // should match up to ServerConfig.sslPort
    redirectEnabled: Boolean = true,
    xForwardedProtoEnabled: Boolean = false,
    excludePaths: Seq[String] = Seq()
) {
  @deprecated("Use redirectEnabled && strictTransportSecurity.isDefined", "2.7.0")
  def hstsEnabled: Boolean = redirectEnabled && strictTransportSecurity.isDefined
}

private object RedirectHttpsKeys {
  val stsPath               = "play.filters.https.strictTransportSecurity"
  val statusCodePath        = "play.filters.https.redirectStatusCode"
  val portPath              = "play.filters.https.port"
  val redirectEnabledPath   = "play.filters.https.redirectEnabled"
  val forwardedProtoEnabled = "play.filters.https.xForwardedProtoEnabled"
  val excludePaths          = "play.filters.https.excludePaths"
}

@Singleton
class RedirectHttpsConfigurationProvider @Inject() (c: Configuration, e: Environment)
    extends Provider[RedirectHttpsConfiguration] {
  import RedirectHttpsKeys._

  private val logger = Logger(getClass)

  lazy val get: RedirectHttpsConfiguration = {
    val strictTransportSecurity = c.get[Option[String]](stsPath)
    val redirectStatusCode      = c.get[Int](statusCodePath)
    if (!isRedirect(redirectStatusCode)) {
      throw c.reportError(statusCodePath, s"Status Code $redirectStatusCode is not a Redirect status code!")
    }
    val port = c.get[Option[Int]](portPath)
    val redirectEnabled = c.get[Option[Boolean]](redirectEnabledPath).getOrElse {
      if (e.mode != Mode.Prod) {
        logger.info(
          s"RedirectHttpsFilter is disabled by default except in Prod mode.\n" +
            s"See https://www.playframework.com/documentation/2.6.x/RedirectHttpsFilter"
        )
      }
      e.mode == Mode.Prod
    }
    val xProtoEnabled = c.get[Boolean](forwardedProtoEnabled)
    val excludePaths  = c.get[Seq[String]](RedirectHttpsKeys.excludePaths)

    RedirectHttpsConfiguration(
      strictTransportSecurity,
      redirectStatusCode,
      port,
      redirectEnabled,
      xProtoEnabled,
      excludePaths
    )
  }
}

class RedirectHttpsModule
    extends SimpleModule(
      bind[RedirectHttpsConfiguration].toProvider[RedirectHttpsConfigurationProvider],
      bind[RedirectHttpsFilter].toSelf
    )

/**
 * The Redirect to HTTPS filter components for compile time dependency injection.
 */
trait RedirectHttpsComponents {
  def configuration: Configuration
  def environment: Environment

  lazy val redirectHttpsConfiguration: RedirectHttpsConfiguration =
    new RedirectHttpsConfigurationProvider(configuration, environment).get
  lazy val redirectHttpsFilter: RedirectHttpsFilter =
    new RedirectHttpsFilter(redirectHttpsConfiguration)
}
