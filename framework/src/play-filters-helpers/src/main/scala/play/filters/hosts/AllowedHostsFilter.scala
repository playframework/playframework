/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.hosts

import javax.inject.{ Inject, Provider, Singleton }

import play.api.MarkerContexts.SecurityMarkerContext
import play.api.{ Configuration, Logger }
import play.api.http.{ HttpErrorHandler, Status }
import play.api.inject._
import play.api.libs.streams.Accumulator
import play.api.mvc.{ EssentialAction, EssentialFilter, RequestHeader }
import play.core.j.{ JavaContextComponents, JavaHttpErrorHandlerAdapter }

/**
 * A filter that denies requests by hosts that do not match a configured list of allowed hosts.
 */
case class AllowedHostsFilter @Inject() (config: AllowedHostsConfig, errorHandler: HttpErrorHandler)
  extends EssentialFilter {

  private val logger = Logger(this.getClass)

  // Java API
  def this(config: AllowedHostsConfig, errorHandler: play.http.HttpErrorHandler, contextComponents: JavaContextComponents) {
    this(config, new JavaHttpErrorHandlerAdapter(errorHandler, contextComponents))
  }

  private val hostMatchers: Seq[HostMatcher] = config.allowed map HostMatcher.apply

  override def apply(next: EssentialAction) = EssentialAction { req =>
    if (!config.shouldProtect(req) || hostMatchers.exists(_(req.host))) {
      next(req)
    } else {
      logger.warn(s"Host not allowed: ${req.host}")(SecurityMarkerContext)
      Accumulator.done(errorHandler.onClientError(req, Status.BAD_REQUEST, s"Host not allowed: ${req.host}"))
    }
  }
}

/**
 * A utility class for matching a host header with a pattern
 */
private[hosts] case class HostMatcher(pattern: String) {
  val isSuffix = pattern startsWith "."
  val (hostPattern, port) = getHostAndPort(pattern)

  def apply(hostHeader: String): Boolean = {
    val (headerHost, headerPort) = getHostAndPort(hostHeader)
    val hostMatches = if (isSuffix) s".$headerHost" endsWith hostPattern else headerHost == hostPattern
    val portMatches = headerPort.forall(_ > 0) && (port.isEmpty || port == headerPort)
    hostMatches && portMatches
  }

  // Get and normalize the host and port
  // Returns None for no port but Some(-1) for an invalid/non-numeric port
  private def getHostAndPort(s: String) = {
    val (h, p) = s.trim.split(":", 2) match {
      case Array(h, p) if p.nonEmpty && p.forall(_.isDigit) => (h, Some(p.toInt))
      case Array(h, _) => (h, Some(-1))
      case Array(h, _*) => (h, None)
    }
    (h.toLowerCase(java.util.Locale.ENGLISH).stripSuffix("."), p)
  }
}

case class AllowedHostsConfig(allowed: Seq[String], shouldProtect: RequestHeader => Boolean = _ => true) {
  import scala.collection.JavaConverters._
  import play.mvc.Http.{ RequestHeader => JRequestHeader }
  import scala.compat.java8.FunctionConverters._

  def withHostPatterns(hosts: java.util.List[String]): AllowedHostsConfig = copy(allowed = hosts.asScala)
  def withShouldProtect(shouldProtect: java.util.function.Predicate[JRequestHeader]): AllowedHostsConfig =
    copy(shouldProtect = shouldProtect.asScala.compose(_.asJava))
}

object AllowedHostsConfig {
  /**
   * Parses out the AllowedHostsConfig from play.api.Configuration (usually this means application.conf).
   */
  def fromConfiguration(conf: Configuration): AllowedHostsConfig = {
    val whiteListRouteModifiers = conf.get[Seq[String]]("play.filters.hosts.routeModifiers.whiteList")
    val blackListRouteModifiers = conf.get[Seq[String]]("play.filters.hosts.routeModifiers.blackList")

    @inline def shouldProtectViaRouteModifiers(rh: RequestHeader): Boolean = {
      import play.api.routing.Router.RequestImplicits._
      if (whiteListRouteModifiers.nonEmpty)
        !whiteListRouteModifiers.exists(rh.hasRouteModifier)
      else
        blackListRouteModifiers.isEmpty || blackListRouteModifiers.exists(rh.hasRouteModifier)
    }

    AllowedHostsConfig(
      allowed = conf.get[Seq[String]]("play.filters.hosts.allowed"),
      shouldProtect = shouldProtectViaRouteModifiers
    )
  }
}

@Singleton
class AllowedHostsConfigProvider @Inject() (configuration: Configuration) extends Provider[AllowedHostsConfig] {
  lazy val get = AllowedHostsConfig.fromConfiguration(configuration)
}

class AllowedHostsModule extends SimpleModule(
  bind[AllowedHostsConfig].toProvider[AllowedHostsConfigProvider],
  bind[AllowedHostsFilter].toSelf
)

trait AllowedHostsComponents {
  def configuration: Configuration
  def httpErrorHandler: HttpErrorHandler

  lazy val allowedHostsConfig: AllowedHostsConfig = AllowedHostsConfig.fromConfiguration(configuration)
  lazy val allowedHostsFilter: AllowedHostsFilter = AllowedHostsFilter(allowedHostsConfig, httpErrorHandler)
}
