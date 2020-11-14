/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.ip

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import play.api.inject.SimpleModule
import play.api.inject.bind
import play.api.mvc._
import play.api.routing.HandlerDef
import play.api.routing.Router
import play.api.Configuration
import play.api.Environment
import play.api.Logger

/**
 * A filter to restrict access to IP allow list.
 *
 * To enable this filter, please add it to to your application.conf file using
 * "play.filters.enabled+=play.filters.ip.AllowedIPFilter"
 *
 * For documentation on configuring this filter, please see the Play documentation at
 * https://www.playframework.com/documentation/latest/AllowedIPFilter
 */
@Singleton
class AllowedIPFilter @Inject() (config: AllowedIPConfiguration) extends EssentialFilter {

  private val logger = Logger(getClass)

  override def apply(next: EssentialAction): EssentialAction = EssentialAction { req =>
    import play.api.libs.streams.Accumulator

    if (!this.config.ipEnabled || this.config.isAllowed(req)) {
      next(req)
    } else if (isNoIPCheck(req)) {
      logger.debug(s"Not blocked because ${req.path} is an excluded path.")
      next(req)
    } else {
      logger.warn(s"Forbidden to IP ${req.remoteAddress} to access ${req.path}.")
      Accumulator.done(Results.Status(httpStatusCode()))
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

case class AllowedIPConfiguration(
    ipEnabled: Boolean,
    httpStatusCode: Int,
    isAllowed: RequestHeader => Boolean = _ => true
)

private object IPKeys {
  val ipEnabled      = "play.filters.ip.enabled"
  val httpStatusCode = "play.filters.ip.httpStatusCode"
  val whiteList      = "play.filters.ip.whiteList"
  val blackList      = "play.filters.ip.blackList"
}

@Singleton
class AllowedIPConfigurationProvider @Inject() (c: Configuration, e: Environment)
    extends Provider[AllowedIPConfiguration] {

  private val logger = Logger(getClass)

  lazy val get: AllowedIPConfiguration = {
    val ipEnabled = c.getOptional[Boolean](IPKeys.ipEnabled).getOrElse(false)
    if (!ipEnabled) {
      logger.warn("You set AllowedIPFilter in your application.conf but it's disabled!")
    }
    val httpStatusCode = c.getOptional[Int](IPKeys.httpStatusCode).getOrElse(403)
    val whiteList      = c.getOptional[Seq[String]](IPKeys.whiteList).getOrElse(Seq.empty)
    val blackList      = c.getOptional[Seq[String]](IPKeys.blackList).getOrElse(Seq.empty)

    AllowedIPConfiguration(
      ipEnabled,
      httpStatusCode,
      req =>
        if (whiteList.isEmpty) {
          if (blackList.isEmpty) {
            true // default case, both whitelist and blacklist are empty so we gzip it.
          } else {
            // The blacklist is defined, so we accept the request if it's not blacklisted.
            // Ignore case for IPv6.
            blackList.forall(ip => !ip.equalsIgnoreCase(req.remoteAddress))
          }
        } else {
          // The whitelist is defined. We accept the request IFF there is a matching whitelist entry.
          // Ignore case for IPv6.
          whiteList.exists(ip => ip.equalsIgnoreCase(req.remoteAddress))
        },
    )
  }
}

class AllowedIPModule
    extends SimpleModule(
      bind[AllowedIPConfiguration].toProvider[AllowedIPConfigurationProvider],
      bind[AllowedIPFilter].toSelf
    )

/**
 * The allowed IP components.
 */
trait AllowedIPComponents {
  def configuration: Configuration
  def environment: Environment

  lazy val allowedIPConfiguration: AllowedIPConfiguration =
    new AllowedIPConfigurationProvider(configuration, environment).get
  lazy val allowedIPFilter: AllowedIPFilter =
    new AllowedIPFilter(allowedIPConfiguration)
}