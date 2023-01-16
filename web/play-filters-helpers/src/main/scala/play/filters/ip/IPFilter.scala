/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.ip

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import play.api.inject.SimpleModule
import play.api.inject.bind
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.routing.HandlerDef
import play.api.routing.Router
import play.api.Configuration
import play.api.Logger
import play.api.http.HttpErrorHandler
import play.api.http.HttpErrorInfo
import play.core.j.JavaHttpErrorHandlerAdapter

/**
 * A filter to black-/whitelist IP addresses.
 *
 * For documentation on configuring this filter, please see the Play documentation at
 * [[https://www.playframework.com/documentation/latest/IPFilter]]
 *
 * @param config A ip filter configuration object
 * @param httpErrorHandler handling failed token error.
 */
@Singleton
class IPFilter @Inject() (config: IPFilterConfig, httpErrorHandler: HttpErrorHandler) extends EssentialFilter {

  private val logger = Logger(getClass)

  // Java API
  def this(
      config: IPFilterConfig,
      errorHandler: play.http.HttpErrorHandler
  ) = {
    this(config, new JavaHttpErrorHandlerAdapter(errorHandler))
  }

  override def apply(next: EssentialAction): EssentialAction = EssentialAction { req =>
    if (this.config.isAllowed(req)) {
      next(req)
    } else if (isNoIPCheck(req)) {
      logger.debug(s"Not blocked because ${req.path} is an excluded path.")
      next(req)
    } else {
      logger.warn(s"Forbidden IP ${req.remoteAddress} to access ${req.path}.")
      Accumulator.done(
        httpErrorHandler.onClientError(
          req.addAttr(HttpErrorHandler.Attrs.HttpErrorInfo, HttpErrorInfo("ip-filter")),
          httpStatusCode(),
          s"IP not allowed: ${req.remoteAddress}"
        )
      )
    }
  }

  @inline
  private[this] def httpStatusCode(): Int =
    this.config.httpStatusCode

  @inline
  private[this] def isNoIPCheck(req: RequestHeader): Boolean = {
    // See more about it:
    // https://www.playframework.com/documentation/2.8.x/Highlights26#Route-modifier-tags
    req.attrs
      .get[HandlerDef](Router.Attrs.HandlerDef)
      .map(_.modifiers)
      .getOrElse(List.empty)
      .contains("noipcheck")
  }

}

case class IPFilterConfig(
    httpStatusCode: Int,
    isAllowed: RequestHeader => Boolean = _ => true
)

object IPFilterConfig {

  /**
   * Parses out the IPFilterConfig from play.api.Configuration (usually this means application.conf).
   */
  def fromConfiguration(conf: Configuration): IPFilterConfig = {
    val ipConfig       = conf.get[Configuration]("play.filters.ip")
    val httpStatusCode = ipConfig.getOptional[Int]("httpStatusCode").getOrElse(403)
    val whiteList      = ipConfig.getOptional[Seq[String]]("whiteList").getOrElse(Seq.empty)
    val blackList      = ipConfig.getOptional[Seq[String]]("blackList").getOrElse(Seq.empty)

    IPFilterConfig(
      httpStatusCode,
      req =>
        if (whiteList.isEmpty) {
          if (blackList.isEmpty) {
            true // default case, both whitelist and blacklist are empty so all IPs are allowed.
          } else {
            // The blacklist is defined, so we accept the IP if it's not blacklisted.
            blackList.forall(!_.equalsIgnoreCase(req.remoteAddress)) // Ignore case needed for IPv6
          }
        } else {
          // The whitelist is defined. We accept the IP if there is a matching whitelist entry.
          whiteList.exists(_.equalsIgnoreCase(req.remoteAddress)) // Ignore case needed for IPv6
        },
    )
  }

}

@Singleton
class IPFilterConfigProvider @Inject() (conf: Configuration) extends Provider[IPFilterConfig] {
  lazy val get: IPFilterConfig = IPFilterConfig.fromConfiguration(conf)
}

class IPFilterModule
    extends SimpleModule(
      bind[IPFilterConfig].toProvider[IPFilterConfigProvider],
      bind[IPFilter].toSelf
    )

/**
 * The IP filter components.
 */
trait IPFilterComponents {
  def configuration: Configuration

  def httpErrorHandler: HttpErrorHandler

  lazy val ipFilterConfig: IPFilterConfig = IPFilterConfig.fromConfiguration(configuration)
  lazy val ipFilter: IPFilter             = new IPFilter(ipFilterConfig, httpErrorHandler)
}
