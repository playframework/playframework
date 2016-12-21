package play.filters.redirectplain

import javax.inject.{ Inject, Provider }

import akka.stream.Materializer
import play.api.Configuration
import play.api.http.HeaderNames._
import play.api.inject.SimpleModule
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * A filter that redirects plain request to https requests
 * based on X-Forwarded-Protocol and security request security
 */
case class RedirectPlainFilter @Inject() (val mat: Materializer, config: RedirectPlainConfig)
    extends Filter {

  override def apply(nextFilter: RequestHeader => Future[Result])(req: RequestHeader): Future[Result] = {
    if (config.enabled) {
      (req.headers.get(X_FORWARDED_PROTO) match {
        case Some(value) => (value == "https")
        case None => req.secure
      }) match {
        case true => nextFilter(req).map(_.withHeaders(STRICT_TRANSPORT_SECURITY -> s"max-age=${config.strictTransportSecurityMaxAge}"))
        case false => Future(Results.Redirect(s"https://${req.host}${req.uri}", 301))
      }
    } else {
      nextFilter(req)
    }
  }
}

case class RedirectPlainConfig(enabled: Boolean, strictTransportSecurityMaxAge: Long)

class RedirectPlainConfigProvider @Inject() (configuration: Configuration)
    extends Provider[RedirectPlainConfig] {

  lazy val enabled: Boolean = configuration
    .getOptional[Boolean]("filters.redirectplain.enabled")
    .getOrElse(false)

  lazy val strictTransportSecurityMaxAge = configuration
    .getOptional[Long]("filters.redirectplain.strict-transport-security.max-age")
    .getOrElse(31536000l)

  lazy val get = RedirectPlainConfig(enabled, strictTransportSecurityMaxAge)

}

class RedirectPlainModule extends SimpleModule {
  bind[RedirectPlainConfig].toProvider[RedirectPlainConfigProvider]
}
