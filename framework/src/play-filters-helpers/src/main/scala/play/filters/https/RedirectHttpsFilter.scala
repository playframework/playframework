/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.https

import javax.inject.{ Inject, Provider }

import play.api.{ Configuration, Environment, Mode }
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
class RedirectHttpsFilter @Inject() (config: RedirectHttpsConfiguration, environment: Environment)
    extends EssentialFilter {

  override def apply(next: EssentialAction): EssentialAction = EssentialAction { req =>
    import play.core.Execution.Implicits.trampoline
    if (req.secure) {
      next(req).map { result =>
        config.strictTransportSecurity.map { sts =>
          environment.mode match {
            case Mode.Prod =>
              result.withHeaders(STRICT_TRANSPORT_SECURITY -> sts)
            case other =>
              result
          }
        }.getOrElse(result)
      }
    } else {
      Accumulator.done(Results.Redirect(createHttpsRedirectUrl(req), config.redirectStatusCode))
    }
  }

  protected def createHttpsRedirectUrl(req: RequestHeader): String = {
    config.sslPort match {
      case None =>
        s"https://${req.domain}${req.uri}"

      case Some(port) =>
        s"https://${req.domain}:${port}${req.uri}"
    }
  }
}

case class RedirectHttpsConfiguration(
  strictTransportSecurity: Option[String] = Some("max-age=31536000; includeSubDomains"),
  redirectStatusCode: Int = PERMANENT_REDIRECT,
  sslPort: Option[Int] = None // should match up to ServerConfig.sslPort
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
    val port = c.getOptional[Int](portPath)

    RedirectHttpsConfiguration(strictTransportSecurityMaxAge, redirectStatusCode, port)
  }
}

class RedirectHttpsModule extends SimpleModule {
  bind[RedirectHttpsConfiguration].toProvider[RedirectHttpsConfigurationProvider]
}
