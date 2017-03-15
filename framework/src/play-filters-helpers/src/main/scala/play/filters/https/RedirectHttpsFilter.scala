/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.https

import javax.inject.{Inject, Provider}

import play.api.Configuration
import play.api.http.HeaderNames._
import play.api.http.Status
import play.api.http.Status._
import play.api.inject.SimpleModule
import play.api.libs.streams.Accumulator
import play.api.mvc._

import scala.concurrent.ExecutionContext

/**
 * A filter that redirects HTTP requests to https requests.
 *
 * To enable this filter, please add it to to your application.conf file using
 * "play.filters.enabled+=play.filters.https.RedirectHttpsFilter"
 *
 * For documentation on configuring this filter, please see the Play documentation at
 * https://www.playframework.com/documentation/latest/RedirectHttpsFilter
 */
class RedirectHttpsFilter @Inject() (config: RedirectHttpsConfiguration)(implicit ec: ExecutionContext)
    extends EssentialFilter {

  override def apply(next: EssentialAction): EssentialAction = EssentialAction { req =>
    if (req.secure) {
      next(req).map { filter =>
        config.strictTransportSecurity.map { sts =>
          filter.withHeaders(STRICT_TRANSPORT_SECURITY -> sts)
        }.getOrElse(filter)
      }
    } else {
      Accumulator.done(Results.Redirect(createHttpsRedirectUrl(req), config.redirectCode))
    }
  }

  protected def createHttpsRedirectUrl(req: RequestHeader): String = {
    config.httpsPort match {
      case -1 =>
        s"https://${req.domain}${req.uri}"

      case port =>
        s"https://${req.domain}:${port}${req.uri}"
    }
  }
}

case class RedirectHttpsConfiguration(
  strictTransportSecurity: Option[String] = None,
  redirectCode: Int = PERMANENT_REDIRECT,
  httpsPort: Int = -1
)

class RedirectHttpsConfigurationProvider @Inject() (c: Configuration)
    extends Provider[RedirectHttpsConfiguration] {
  private val stsPath = "play.filters.https.strictTransportSecurity"
  private val statusCodePath = "play.filters.https.redirectStatusCode"
  private val portPath = "play.filters.https.port"

  lazy val get: RedirectHttpsConfiguration = {
    val strictTransportSecurityMaxAge = c.get[Option[String]](stsPath)
    val redirectStatusCode = c.get[Int](statusCodePath)
    if (!Status.isRedirect(redirectStatusCode)) {
      throw c.reportError(statusCodePath, s"Status Code $redirectStatusCode is not a Redirect status code!")
    }
    val port = c.get[Int](portPath)

    RedirectHttpsConfiguration(strictTransportSecurityMaxAge, redirectStatusCode, port)
  }
}

class RedirectHttpsModule extends SimpleModule {
  bind[RedirectHttpsConfiguration].toProvider[RedirectHttpsConfigurationProvider]
}
