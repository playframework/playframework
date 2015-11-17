/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.common

import java.net.InetAddress
import play.api.{ Configuration, PlayException }
import play.api.mvc.Headers
import play.core.server.common.NodeIdentifierParser.Ip
import scala.annotation.tailrec

import ForwardedHeaderHandler._

/**
 * The ForwardedHeaderHandler class works out the remote address and protocol
 * by taking
 * into account Forward and X-Forwarded-* headers from trusted proxies. The
 * algorithm it uses is as follows:
 *
 * 1. Start with the immediate connection to the application.
 * 2. If this address is in the subnet of our trusted proxies, then look for
 *    a Forward or X-Forward-* header sent by that proxy.
 * 2a. If the proxy sent a header, then go back to step 2 using the address
 *     that it sent.
 * 2b. If the proxy didn't send a header, then use the proxy's address.
 * 3. If the address is not a trusted proxy, use that address.
 *
 * Each address is associated with a secure or insecure protocol by pairing
 * it with a `proto` entry in the headers. If the `proto` entry is missing or
 * if `proto` entries can't be matched with addresses, then the default is
 * insecure.
 *
 * It is configured by two configuration options:
 * <dl>
 *   <dt>play.http.forwarded.version</dt>
 *   <dd>
 *     The version of the forwarded headers ist uses to parse the headers. It can be
 *     <code>x-forwarded</code> for legacy headers or
 *     <code>rfc7239</code> for the definition from the RFC 7239 <br>
 *     Default is x-forwarded.
 *   </dd>
 *   <dt>play.http.forwarded.trustedProxies</dt>
 *   <dd>
 *     A list of proxies that are ignored when getting the remote address or the remote port.
 *     It can have optionally an address block size. When the address block size is set,
 *     all IP-addresses in the range of the subnet will be treated as trusted.
 *   </dd>
 * </dl>
 */
private[server] class ForwardedHeaderHandler(configuration: ForwardedHeaderHandlerConfig) {

  def remoteConnection(
    rawRemoteAddress: InetAddress,
    rawSecure: Boolean,
    headers: Headers): ConnectionInfo = {
    val rawConnection = ConnectionInfo(rawRemoteAddress, rawSecure)
    remoteConnection(rawConnection, headers)
  }

  def remoteConnection(rawConnection: ConnectionInfo, headers: Headers): ConnectionInfo = {

    def isTrustedProxy(connection: ConnectionInfo): Boolean = {
      configuration.trustedProxies.exists(_.isInRange(connection.address))
    }

    // Use a mutable iterator for performance when scanning the
    // header entries. Go through the headers in reverse order because
    // the nearest proxies will be at the end of the list and we need
    // to move backwards through the list to get to the original IP.
    val headerEntries: Iterator[ForwardedEntry] = configuration.forwardedHeaders(headers).reverseIterator

    @tailrec
    def scan(prev: ConnectionInfo): ConnectionInfo = {
      if (isTrustedProxy(prev)) {
        // 'prev' is a trusted proxy, so we look to see if there are any
        // headers from prev about forwarding
        if (headerEntries.hasNext) {
          // There is a forwarded header from 'prev', so lets process it and get the
          // address.
          val entry = headerEntries.next()
          val addressString: String = entry.addressString.getOrElse(throw new PlayException(
            "Invalid forwarded header",
            s"""|Forwarding header supplied by trusted proxy $prev is missing an
                |address entry: fix proxy header or remove proxy from
                |$TrustedProxiesConfigPath config entry""".stripMargin))
          val address: InetAddress = NodeIdentifierParser.parseNode(addressString) match {
            case Right((Ip(address), _)) => address
            case Right((nonIpAddress, _)) => throw new PlayException(
              "Invalid forwarded header",
              s"""|Forwarding header '$addressString' supplied by trusted proxy
                  |$prev has a non-IP address '$nonIpAddress': fix proxy header
                  |or remove proxy from $TrustedProxiesConfigPath config entry""".stripMargin)
            case Left(error) => throw new PlayException(
              "Invalid forwarded header",
              s"""|Forwarding header '$addressString' supplied by trusted proxy
                  |$prev could not be parsed: $error: fix proxy header or
                  |remove proxy from $TrustedProxiesConfigPath config entry""".stripMargin)
          }
          val secure = entry.protoString.fold(false)(_ == "https") // Assume insecure by default
          val connection = ConnectionInfo(address, secure)
          scan(connection)
        } else {
          // No more headers to process. Even though we trust 'prev' as a proxy,
          // it hasn't sent us any headers, so just use its address.
          prev
        }
      } else {
        // 'prev' is not a trusted proxy, so we don't scan ahead in the list of
        // forwards, we just return 'prev'.
        prev
      }
    }

    // Start scanning through connections starting at the rawConnection that
    // was made the the Play server.
    scan(rawConnection)
  }

}

private[server] object ForwardedHeaderHandler {

  sealed trait Version
  case object Rfc7239 extends Version
  case object Xforwarded extends Version

  type HeaderParser = Headers => Seq[ForwardedEntry]

  final case class ForwardedEntry(addressString: Option[String], protoString: Option[String])

  case class ForwardedHeaderHandlerConfig(version: Version, trustedProxies: List[Subnet]) {
    def forwardedHeaders: HeaderParser = version match {
      case Rfc7239 => rfc7239Headers
      case Xforwarded => xforwardedHeaders
    }

    private val rfc7239Headers: HeaderParser = { (headers: Headers) =>
      (for {
        fhs <- headers.getAll("Forwarded")
        fh <- fhs.split(",\\s*")
      } yield fh).map(_.split(";").map(s => {
        val splitted = s.split("=", 2)
        splitted(0).toLowerCase(java.util.Locale.ENGLISH) -> splitted(1)
      }).toMap).map { paramMap: Map[String, String] =>
        ForwardedEntry(paramMap.get("for"), paramMap.get("proto"))
      }
    }

    private val xforwardedHeaders: HeaderParser = { (headers: Headers) =>
      def h(h: Headers, key: String) = h.getAll(key).flatMap(s => s.split(",\\s*"))
      val forHeaders = h(headers, "X-Forwarded-For")
      val protoHeaders = h(headers, "X-Forwarded-Proto")
      if (forHeaders.length == protoHeaders.length) {
        forHeaders.zip(protoHeaders).map {
          case (f, p) => ForwardedEntry(Some(f), Some(p))
        }
      } else {
        // If the lengths vary, then discard the protoHeaders because we can't tell which
        // proto matches which header. The connections will all appear to be insecure by
        // default.
        forHeaders.map {
          case f => ForwardedEntry(Some(f), None)
        }
      }
    }
  }

  val ForwardingVersionConfigPath = "play.http.forwarded.version"
  val TrustedProxiesConfigPath = "play.http.forwarded.trustedProxies"

  object ForwardedHeaderHandlerConfig {
    def apply(configuration: Option[Configuration]): ForwardedHeaderHandlerConfig = configuration.map { c =>
      ForwardedHeaderHandlerConfig(
        c.getString(ForwardingVersionConfigPath, Some(Set("x-forwarded", "rfc7239")))
          .fold(Xforwarded: Version)(version => if (version == "rfc7239") Rfc7239 else Xforwarded),
        c.getStringSeq(TrustedProxiesConfigPath)
          .getOrElse(List("::1", "127.0.0.1"))
          .map(Subnet.apply).toList
      )
    }.getOrElse(ForwardedHeaderHandlerConfig(Xforwarded, List(Subnet("::1"), Subnet("172.0.0.1"))))
  }
}
