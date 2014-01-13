/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws

import play.api.libs.ws.ssl.{ DefaultSSLConfig, DefaultSSLConfigParser, SSLConfig }
import play.api.Configuration

/**
 * A WSConfiguration trait.  This provides bindings that can be passed into any implementation of WSClient.
 */
trait WSClientConfig {

  def connectionTimeout: Option[Long]

  def idleTimeout: Option[Long]

  def requestTimeout: Option[Long]

  def followRedirects: Option[Boolean]

  def useProxyProperties: Option[Boolean]

  def userAgent: Option[String]

  def compressionEnabled: Option[Boolean]

  def acceptAnyCertificate: Option[Boolean]

  def ssl: Option[SSLConfig]
}

/**
 * Default client config option.
 */
case class DefaultWSClientConfig(connectionTimeout: Option[Long] = None,
  idleTimeout: Option[Long] = None,
  requestTimeout: Option[Long] = None,
  followRedirects: Option[Boolean] = None,
  useProxyProperties: Option[Boolean] = None,
  userAgent: Option[String] = None,
  compressionEnabled: Option[Boolean] = None,
  acceptAnyCertificate: Option[Boolean] = None,
  ssl: Option[SSLConfig] = None) extends WSClientConfig

/**
 * This class creates a DefaultWSClientConfig object from the play.api.Configuration.
 */
class DefaultWSConfigParser(configuration: Configuration) {

  def parse(): WSClientConfig = {
    // .getOrElse(120000L)
    val connectionTimeout = configuration.getMilliseconds("ws.timeout.connection")
    val idleTimeout = configuration.getMilliseconds("ws.timeout.idle")
    val requestTimeout = configuration.getMilliseconds("ws.timeout.request")

    // default: true
    val followRedirects = configuration.getBoolean("ws.followRedirects")

    // default: true
    val useProxyProperties = configuration.getBoolean("ws.useProxyProperties")

    val userAgent = configuration.getString("ws.useragent")

    val compressionEnabled = configuration.getBoolean("ws.compressionEnabled")

    val acceptAnyCertificate = configuration.getBoolean("ws.acceptAnyCertificate")

    val sslConfig = configuration.getConfig("ws.ssl").map { sslConfig =>
      val sslContextParser = new DefaultSSLConfigParser(sslConfig)
      sslContextParser.parse()
    }

    DefaultWSClientConfig(
      connectionTimeout = connectionTimeout,
      idleTimeout = idleTimeout,
      requestTimeout = requestTimeout,
      followRedirects = followRedirects,
      useProxyProperties = useProxyProperties,
      userAgent = userAgent,
      compressionEnabled = compressionEnabled,
      acceptAnyCertificate = acceptAnyCertificate,
      ssl = sslConfig)
  }
}
