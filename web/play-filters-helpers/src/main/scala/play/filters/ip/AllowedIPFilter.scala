/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.ip

import com.google.common.net.InetAddresses
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

    if (!this.config.ipEnabled || isAllowed(req)) {
      next(req)
    } else if (isNoIPCheck(req)) {
      logger.debug(s"Not blocked because ${req.path} is an excluded path.")
      next(req)
    } else {
      logger.warn(s"Forbidden to IP ${req.remoteAddress} to access ${req.path}.")
      Accumulator.done(Results.Forbidden)
    }
  }

  @inline
  private[this] def isAllowed(req: RequestHeader): Boolean =
    this.config.allowList.contains(req.remoteAddress)

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
    allowList: Seq[String]
)

private object IPKeys {
  val ipEnabled = "play.filters.ip.enabled"
  val allowList = "play.filters.ip.allowList"
}

@Singleton
class AllowedIPConfigurationProvider @Inject() (c: Configuration, e: Environment)
    extends Provider[AllowedIPConfiguration] {

  private val logger = Logger(getClass)

  lazy val get: AllowedIPConfiguration = {
    val ipEnabled = c.getOptional[Boolean](IPKeys.ipEnabled).getOrElse(true)
    if (!ipEnabled) {
      logger.warn("You set AllowedIPFilter in your application.conf but it's disabled!")
    }
    val allowList = c.getOptional[Seq[String]](IPKeys.allowList).getOrElse(Seq())

    AllowedIPConfiguration(
      ipEnabled,
      allowList.map(InetAddresses.forString).map(_.getHostAddress)
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
