/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.common

import java.net.InetSocketAddress
import play.api.mvc.Headers

/**
 * Common utilities to help servers with processing their requests.
 */
object ServerRequestUtils {

  /**
   * Find the remote address for the connection, taking into account any
   * trusted `Forwarded` or `X-Forwarded-For` headers.
   */
  def findRemoteAddress(
    forwardedHeaderHandler: ForwardedHeaderHandler,
    headers: Headers,
    connectionRemoteAddress: InetSocketAddress): String = {
    val forwardedAddress: Option[String] = forwardedHeaderHandler.remoteAddress(headers)
    forwardedAddress.getOrElse(connectionRemoteAddress.getAddress.getHostAddress)
  }

  /**
   * Find if the remote protocol is secure, taking into account any
   * trusted `Forwarded` or `X-Forwarded-Proto` headers.
   */
  def findSecureProtocol(
    forwardedHeaderHandler: ForwardedHeaderHandler,
    headers: Headers,
    connectionSecureProtocol: Boolean): Boolean = {
    val forwardedSecureFlag: Option[Boolean] = forwardedHeaderHandler.remoteProtocol(headers).map(_ == "https")
    forwardedSecureFlag.getOrElse(connectionSecureProtocol)
  }

}
