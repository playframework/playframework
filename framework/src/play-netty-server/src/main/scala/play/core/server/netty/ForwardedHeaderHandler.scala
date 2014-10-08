/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.netty

import play.api.Configuration
import play.api.mvc.Headers

/**
 * The ForwardedHeaderHandler class retrieves the last untrusted proxy
 * from the Forwarded-Headers or the X-Forwarded-*-Headers.
 *
 * To find the last untrusted proxy it removes all trusted proxy ip addresses
 * from the end of the forwarded headers. The last header that is present
 * is the last untrusted proxy.
 *
 * It is configured by two configuration options:
 * <dl>
 *   <dt>play.http.forwarded.version</dt>
 *   <dd>
 *     The version of the forwarded headers ist uses to parse the headers. It can be
 *     <code>x-forwarded</code> for legacy headers or
 *     <code>rfc7239</code> for the definition from the RFC 7239
 *   </dd>
 *   <dt>play.http.forwarded.trustedProxies</dt>
 *   <dd>
 *     A list of proxies that are ignored when getting the remote address or the remote port.
 *     It can have optionally an address block size. When the address block size is set,
 *     all IP-addresses in the range of the subnet will be treated as trusted.
 *   </dd>
 * </dl>
 */
private[netty] trait ForwardedHeaderHandler {

  def configuration: Option[Configuration]

  def remoteProtocol(headers: Headers): Option[String] = {
    firstUntrustedForwarded(forwardedHeaders(headers), trustedProxies).get("proto")
  }

  def remoteAddress(headers: Headers): Option[String] = {
    firstUntrustedForwarded(forwardedHeaders(headers), trustedProxies).get("for")
  }

  private val rfc7239Headers: (Headers) => Seq[Map[String, String]] = { (headers: Headers) =>
    (for {
      fhs <- headers.getAll("Forwarded")
      fh <- fhs.split(",\\s*")
    } yield fh).map(_.split(";").map(s => {
      val splitted = s.split("=", 2)
      splitted(0).toLowerCase -> splitted(1)
    }).toMap)
  }

  private val xforwardedHeaders: (Headers) => Seq[Map[String, String]] = { (headers: Headers) =>
    def h(h: Headers, key: String) = h.getAll(key).flatMap(s => s.split(",\\s*"))
    h(headers, "X-Forwarded-For").zipAll(h(headers, "X-Forwarded-Proto"), "", "")
      .map { case (f, p) => Map("for" -> f, "proto" -> p) }
  }

  private def firstUntrustedForwarded(
    forwardedHeaders: Seq[Map[String, String]],
    trustedProxies: Seq[Subnet]): Map[String, String] = forwardedHeaders
    .reverse
    .dropWhile(m => { isTrusted(m.getOrElse("for", "unknown"), trustedProxies) })
    .headOption
    .getOrElse(Map.empty)

  private def isTrusted(s: String, trustedProxies: Seq[Subnet]): Boolean =
    NodeIdentifierParser.parseNode(s).fold(_ => false, _._1.fold(_ => false, inet => trustedProxies.exists(_.isInRange(inet))))

  private def forwardedHeaders: (Headers) => Seq[Map[String, String]] = {
    val xForward = "x-forwarded"
    for {
      c <- configuration
      version <- c.getString("play.http.forwarded.version", Some(Set("rfc7239", xForward)))
      if version == xForward
    } yield xforwardedHeaders
  }.getOrElse(rfc7239Headers)

  private def trustedProxies: List[Subnet] = for {
    c <- configuration.toList
    trustedIp <- c.getStringSeq("play.http.forwarded.trustedProxies").getOrElse(List("::1", "127.0.0.1"))
  } yield Subnet(trustedIp)
}
