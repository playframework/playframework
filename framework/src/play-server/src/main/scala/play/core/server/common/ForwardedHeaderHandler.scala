/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import java.net.InetAddress
import java.security.cert.X509Certificate

import play.api.{ Configuration, Logger }
import play.api.mvc.Headers
import play.core.server.common.NodeIdentifierParser.Ip

import scala.annotation.tailrec
import ForwardedHeaderHandler._
import play.api.mvc.request.RemoteConnection

/**
 * The ForwardedHeaderHandler class works out the remote address and protocol
 * by taking
 * into account Forward and X-Forwarded-* headers from trusted proxies. The
 * algorithm it uses is as follows:
 *
 * 1. Start with the immediate connection to the application.
 * 2. If the proxy *did not* send a valid Forward or X-Forward-* header then return
 *    that connection and don't do any further processing.
 * 3. If the proxy *did* send a valid header then work out whether we trust it by
 *    checking whether the immediate connection is in our list of trusted
 *    proxies.
 * 4. If the immediate connection *is* a trusted proxy, then resume at step
 *    1 using the connection info in the forwarded header.
 * 5. If the immediate connection *is not* a trusted proxy, then return the
 *    immediate connection info and don't do any further processing.
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

  /**
   * Update connection information based on any forwarding information in the headers.
   *
   * @param rawConnection The raw connection that connected to the Play server.
   * @param headers The request headers.
   * @return An updated connection
   */
  def forwardedConnection(rawConnection: RemoteConnection, headers: Headers): RemoteConnection = new RemoteConnection {

    // All public methods delegate to the lazily calculated connection info
    override def remoteAddress: InetAddress = parsed.remoteAddress
    override def secure: Boolean = parsed.secure
    override def clientCertificateChain: Option[Seq[X509Certificate]] = parsed.clientCertificateChain

    /**
     * Perform header parsing lazily, yielding a RemoteConnection with the results.
     */
    private lazy val parsed: RemoteConnection = {
      // Use a mutable iterator for performance when scanning the
      // header entries. Go through the headers in reverse order because
      // the nearest proxies will be at the end of the list and we need
      // to move backwards through the list to get to the original IP.
      val headerEntries: Iterator[ForwardedEntry] = configuration.forwardedHeaders(headers).reverseIterator

      @tailrec
      def scan(prev: RemoteConnection): RemoteConnection = {
        // Check if there's a forwarded header for us to scan.
        if (headerEntries.hasNext) {
          // There is a forwarded header from 'prev', so lets check if 'prev' is trusted.
          // If it's a trusted proxy then process the header, otherwise just use 'prev'.

          if (configuration.isTrustedProxy(prev.remoteAddress)) {
            // 'prev' is a trusted proxy, so we process the next entry.
            val entry = headerEntries.next()
            configuration.parseEntry(entry) match {
              case Left(error) =>
                ForwardedHeaderHandler.logger.debug(
                  s"Error with info in forwarding header $entry, using $prev instead: $error."
                )
                prev
              case Right(parsedEntry) =>
                scan(RemoteConnection(parsedEntry.address, parsedEntry.secure, None /* No cert chain for forward headers */ ))
            }
          } else {
            // 'prev' is not a trusted proxy, so we don't scan ahead in the list of
            // forwards, we just return 'prev'.
            prev
          }
        } else {
          // No more headers to process, so just use its address.
          prev
        }
      }

      // Start scanning through connections starting at the rawConnection that
      // was made the Play server.
      scan(rawConnection)
    }
  }

}

private[server] object ForwardedHeaderHandler {

  private val logger = Logger(getClass)

  /**
   * The version of headers that this Play application understands.
   */
  sealed trait ForwardedHeaderVersion
  case object Rfc7239 extends ForwardedHeaderVersion
  case object Xforwarded extends ForwardedHeaderVersion

  type HeaderParser = Headers => Seq[ForwardedEntry]

  /**
   * An unparsed address and protocol pair from a forwarded header. Both values are
   * optional.
   */
  final case class ForwardedEntry(addressString: Option[String], protoString: Option[String])

  /**
   * Basic information about an HTTP connection, parsed from a ForwardedEntry.
   */
  final case class ParsedForwardedEntry(address: InetAddress, secure: Boolean)

  case class ForwardedHeaderHandlerConfig(version: ForwardedHeaderVersion, trustedProxies: List[Subnet]) {

    val nodeIdentifierParser = new NodeIdentifierParser(version)

    /**
     * Removes surrounding quotes if present, otherwise returns original string.
     * Not RFC compliant. To be compliant we need proper header field parsing.
     */
    private def unquote(s: String): String = {
      if (s.length >= 2 && s.charAt(0) == '"' && s.charAt(s.length - 1) == '"') {
        s.substring(1, s.length - 1)
      } else s
    }

    /**
     * Parse any Forward or X-Forwarded-* headers into a sequence of ForwardedEntry
     * objects. Each object a pair with an optional unparsed address and an
     * optional unparsed protocol. Further parsing may happen later, see `remoteConnection`.
     */
    def forwardedHeaders(headers: Headers): Seq[ForwardedEntry] = version match {
      case Rfc7239 => {
        val params = (for {
          fhs <- headers.getAll("Forwarded")
          fh <- fhs.split(",\\s*")
        } yield (fh.split(";").flatMap {
          _.span(_ != '=') match {
            case (_, "") => Option.empty[(String, String)] // no value

            case (rawName, v) => {
              // Remove surrounding quotes
              val name = rawName.toLowerCase(java.util.Locale.ENGLISH)
              val value = unquote(v.tail)

              Some(name -> value)
            }
          }
        }(scala.collection.breakOut): Map[String, String]))

        params.map { paramMap: Map[String, String] =>
          ForwardedEntry(paramMap.get("for"), paramMap.get("proto"))
        }
      }

      case Xforwarded =>
        def h(h: Headers, key: String) = h.getAll(key).flatMap(s => s.split(",\\s*")).map(unquote)
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

    /**
     * Try to parse a `ForwardedEntry` into a valid `ConnectionInfo` with an IP address
     * and information about the protocol security. If this cannot happen, either because
     * parsing fails or because the connection info doesn't include an IP address, this
     * method will return `Left` with an error message.
     */
    def parseEntry(entry: ForwardedEntry): Either[String, ParsedForwardedEntry] = {
      entry.addressString match {
        case None =>
          // We had a forwarding header, but it was missing the address entry for some reason.
          Left("No address")
        case Some(addressString) =>
          nodeIdentifierParser.parseNode(addressString) match {
            case Right((Ip(address), _)) =>
              // Parsing was successful, use this connection and scan for another connection.
              val secure = entry.protoString.fold(false)(_ == "https") // Assume insecure by default
              val connection = ParsedForwardedEntry(address, secure)
              Right(connection)
            case errorOrNonIp =>
              // The forwarding address entry couldn't be parsed for some reason.
              Left(s"Parse error: $errorOrNonIp")
          }
      }
    }

    /**
     * Check if a connection is considered to be a trusted proxy, i.e. a proxy whose
     * forwarding headers we will process.
     */
    def isTrustedProxy(address: InetAddress): Boolean = {
      trustedProxies.exists(_.isInRange(address))
    }
  }

  object ForwardedHeaderHandlerConfig {
    def apply(configuration: Option[Configuration]): ForwardedHeaderHandlerConfig = {
      val config = configuration.getOrElse(Configuration.reference).get[Configuration]("play.http.forwarded")

      val version = config.get[String]("version") match {
        case "x-forwarded" => Xforwarded
        case "rfc7239" => Rfc7239
        case _ => throw config.reportError("version", "Forwarded header version must be either x-forwarded or rfc7239")
      }

      ForwardedHeaderHandlerConfig(version, config.get[Seq[String]]("trustedProxies").map(Subnet.apply).toList)
    }
  }
}
