/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws

import javax.inject.{ Singleton, Inject, Provider }

import play.api.libs.ws.ssl.{ SSLConfigParser, SSLConfig }
import play.api.{ PlayConfig, Environment, Configuration }

import scala.concurrent.duration._

/**
 * WS client config
 */
case class WSClientConfig(connectionTimeout: Duration = 2.minutes,
  idleTimeout: Duration = 2.minutes,
  requestTimeout: Duration = 2.minutes,
  followRedirects: Boolean = true,
  useProxyProperties: Boolean = true,
  userAgent: Option[String] = None,
  compressionEnabled: Boolean = false,
  ssl: SSLConfig = SSLConfig())

/**
 * This class creates a DefaultWSClientConfig object from the play.api.Configuration.
 */
@Singleton
class WSConfigParser @Inject() (configuration: Configuration, environment: Environment) extends Provider[WSClientConfig] {

  def get = parse()

  def parse(): WSClientConfig = {

    val config = PlayConfig(configuration).getDeprecatedWithFallback("play.ws", "ws")

    val connectionTimeout = config.get[Duration]("timeout.connection")
    val idleTimeout = config.get[Duration]("timeout.idle")
    val requestTimeout = config.get[Duration]("timeout.request")

    val followRedirects = config.get[Boolean]("followRedirects")
    val useProxyProperties = config.get[Boolean]("useProxyProperties")

    val userAgent = config.getOptional[String]("useragent")

    val compressionEnabled = config.get[Boolean]("compressionEnabled")

    val sslConfig = new SSLConfigParser(config.get[PlayConfig]("ssl"), environment.classLoader).parse()

    WSClientConfig(
      connectionTimeout = connectionTimeout,
      idleTimeout = idleTimeout,
      requestTimeout = requestTimeout,
      followRedirects = followRedirects,
      useProxyProperties = useProxyProperties,
      userAgent = userAgent,
      compressionEnabled = compressionEnabled,
      ssl = sslConfig)
  }
}
