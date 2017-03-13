/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.https

import javax.inject.{ Inject, Provider }

import akka.stream.Materializer
import play.api.Configuration
import play.api.http.HeaderNames._
import play.api.http.Status._
import play.api.inject.SimpleModule
import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }

/**
 * A filter that redirects HTTP requests to https requests.
 */
class RedirectHttpsFilter @Inject() (config: RedirectHttpsConfiguration)(implicit val mat: Materializer, ec: ExecutionContext)
    extends Filter {

  override def apply(nextFilter: RequestHeader => Future[Result])(req: RequestHeader): Future[Result] = {
    if (req.secure) {
      nextFilter(req).map { filter =>
        config.strictTransportSecurity.map { sts =>
          filter.withHeaders(STRICT_TRANSPORT_SECURITY -> sts)
        }.getOrElse(filter)
      }
    } else {
      Future.successful(Results.Redirect(createHttpsRedirectUrl(req), config.redirectCode))
    }
  }

  /**
   * Create https redirect url for given request
   *
   * @param req  Request to create https request url for
   * @return the URL using HTTPS
   */
  protected def createHttpsRedirectUrl(req: RequestHeader): String = s"https://${req.host}${req.uri}"
}

case class RedirectHttpsConfiguration(strictTransportSecurity: Option[String] = None, redirectCode: Int = PERMANENT_REDIRECT)

class RedirectHttpsConfigurationProvider @Inject() (c: Configuration)
    extends Provider[RedirectHttpsConfiguration] {
  lazy val get: RedirectHttpsConfiguration = {
    val strictTransportSecurityMaxAge = c.get[Option[String]]("play.filters.https.strictTransportSecurity")
    val redirectCode = c.get[Int]("play.filters.https.redirectCode")
    RedirectHttpsConfiguration(strictTransportSecurityMaxAge, redirectCode)
  }
}

class RedirectHttpsModule extends SimpleModule {
  bind[RedirectHttpsConfiguration].toProvider[RedirectHttpsConfigurationProvider]
}
