/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.hosts

import javax.inject.{ Inject, Provider, Singleton }

import play.api.http.{ HttpErrorHandler, Status }
import play.api.inject.Module
import play.api.libs.streams.Accumulator
import play.api.mvc.{ EssentialAction, EssentialFilter }
import play.api.{ Configuration, Environment, PlayConfig }
import play.core.j.JavaHttpErrorHandlerAdapter

/**
 * A filter that denies requests by hosts that do not match a configured list of allowed hosts.
 */
case class AllowedHostsFilter @Inject() (config: AllowedHostsConfig, errorHandler: HttpErrorHandler)
    extends EssentialFilter {

  // Java API
  def this(config: AllowedHostsConfig, errorHandler: play.http.HttpErrorHandler) {
    this(config, new JavaHttpErrorHandlerAdapter(errorHandler))
  }

  private val hostMatchers: Seq[HostMatcher] = config.allowed map HostMatcher.apply

  override def apply(next: EssentialAction) = EssentialAction { req =>
    if (hostMatchers.exists(_(req.host))) {
      next(req)
    } else {
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

case class AllowedHostsConfig(allowed: Seq[String]) {
  def this() {
    this(Seq.empty)
  }

  import scala.collection.JavaConverters._
  def withHostPatterns(hosts: java.util.List[String]): AllowedHostsConfig = copy(hosts.asScala.toSeq)
}

object AllowedHostsConfig {
  /**
   * Parses out the AllowedHostsConfig from play.api.Configuration (usually this means application.conf).
   */
  def fromConfiguration(conf: Configuration): AllowedHostsConfig = {
    AllowedHostsConfig(PlayConfig(conf).get[Seq[String]]("play.filters.hosts.allowed"))
  }
}

@Singleton
class AllowedHostsConfigProvider @Inject() (configuration: Configuration) extends Provider[AllowedHostsConfig] {
  lazy val get = AllowedHostsConfig.fromConfiguration(configuration)
}

class AllowedHostsModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = Seq(
    bind[AllowedHostsConfig].toProvider[AllowedHostsConfigProvider],
    bind[AllowedHostsFilter].toSelf
  )
}

trait AllowedHostsComponents {
  def configuration: Configuration
  def httpErrorHandler: HttpErrorHandler

  lazy val allowedHostsConfig: AllowedHostsConfig = AllowedHostsConfig.fromConfiguration(configuration)
  lazy val allowedHostsFilter: AllowedHostsFilter = AllowedHostsFilter(allowedHostsConfig, httpErrorHandler)
}
