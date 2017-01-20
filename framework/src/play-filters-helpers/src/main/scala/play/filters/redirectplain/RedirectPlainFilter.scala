/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.redirectplain

import javax.inject.{ Inject, Provider }

import akka.stream.Materializer
import play.api.Configuration
import play.api.http.HeaderNames._
import play.api.http.Status._
import play.api.inject.SimpleModule
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object RedirectPlainFilter {

  /**
   * Create https redirect url for given request
   *
   * @param req  Request to create https request url for
   * @return
   */
  def createHttpsRedirectUrl(req: RequestHeader): String = s"https://${req.host}${req.uri}"

  /**
   * Checks both the X_FORWARDED_PROTO and FORWARDED headers
   *
   * @see {play.filters.redirectplain.RedirectPlainFilter#isSecureForwarded}
   * @param req  Request to check if it is secure
   * @return
   */
  def isRequestSecure(req: RequestHeader): Boolean = req.secure || isSecureForwarded(req)

  /**
   * Checks to see if either the X_FORWARDED_PROTO or FORWARDED headers
   * contains a https proxy. If either one of them contains https
   * the request is assumed secure.
   *
   * @param req  Request to check if it is forwarded securely
   * @return
   */
  def isSecureForwarded(req: RequestHeader): Boolean = {
    // First check the X_FORWARDED_PROTO
    req.headers.get(X_FORWARDED_PROTO) match {
      case Some(value) => value.contains("https")
      // If X_FORWARDED_PROTO header not found check the forwarded headers
      case None => req.headers.get(FORWARDED) match {
        case Some(value) => value.contains("https")
        case None => false
      }
    }
  }
}

/**
 * A filter that redirects plain request to https requests
 * based on X-Forwarded-Protocol and security request security
 */
case class RedirectPlainFilter @Inject() (val mat: Materializer, config: RedirectPlainConfig)
    extends Filter {
  import RedirectPlainFilter._

  lazy val strictMaxAge = s"max-age=${config.strictTransportSecurityMaxAge}"

  override def apply(nextFilter: RequestHeader => Future[Result])(req: RequestHeader): Future[Result] = {
    if (isRequestSecure(req))
      nextFilter(req).map(_.withHeaders(STRICT_TRANSPORT_SECURITY -> strictMaxAge))
    else
      Future(Results.Redirect(createHttpsRedirectUrl(req), PERMANENT_REDIRECT))
  }

}

case class RedirectPlainConfig(strictTransportSecurityMaxAge: Long)

class RedirectPlainConfigProvider @Inject() (configuration: Configuration)
    extends Provider[RedirectPlainConfig] {

  lazy val strictTransportSecurityMaxAge = configuration
    .getOptional[Long]("play.filters.redirectplain.strict-transport-security.max-age")
    .getOrElse(31536000l)

  lazy val get = RedirectPlainConfig(strictTransportSecurityMaxAge)

}

class RedirectPlainModule extends SimpleModule {
  bind[RedirectPlainConfig].toProvider[RedirectPlainConfigProvider]
}
