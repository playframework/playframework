/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import java.util.regex.Pattern

import scala.annotation.tailrec

import play.api.http.HeaderNames
import play.api.mvc.request.AuthorityPort
import play.api.mvc.request.ForwardingInfo
import play.api.mvc.request.ForwardingSource
import play.api.mvc.request.NodePort
import play.api.mvc.request.RemoteEndpoint
import play.api.mvc.request.RemoteInfo
import play.api.mvc.request.RemoteNode
import play.api.mvc.request.RequestAuthority
import play.api.mvc.request.Scheme
import play.api.mvc.Headers
import play.api.Configuration
import play.api.Logger
import play.core.server.common.NodeIdentifierParser.Ip
import play.core.server.common.NodeIdentifierParser.ObfuscatedIp
import play.core.server.common.NodeIdentifierParser.ObfuscatedPort
import play.core.server.common.NodeIdentifierParser.PortNumber
import play.core.server.common.NodeIdentifierParser.UnknownIp
import ForwardedHeaderHandler._

/**
 * Derives the selected remote identity and endpoint port, effective request scheme,
 * and effective authority from Forwarded and X-Forwarded-* headers supplied by
 * trusted proxies. The algorithm is as follows:
 *
 * 1. Start with the immediate connection to the application.
 * 2. If the proxy *did not* send a valid Forwarded or X-Forwarded-* header then return
 *    the direct remote and don't do any further processing.
 * 3. If the proxy *did* send a valid header then work out whether we trust it by
 *    checking whether the immediate connection is in our list of trusted
 *    proxies or trusted RFC 7239 obfuscated proxy identifiers.
 * 4. If the immediate connection *is* a trusted proxy, then resume at step
 *    1 using the remote info in the forwarded header. If the forwarded
 *    identity is not an IP address, keep it as the selected remote identity
 *    and stop scanning unless it is an explicitly trusted obfuscated identifier.
 * 5. If the immediate remote *is not* a trusted proxy, then return it and don't
 *    do any further processing.
 *
 * Each identity can include its remote endpoint port. The effective request
 * scheme is tracked separately. If the `proto` entry is missing, or if `proto`
 * entries can't be matched with addresses, then the prior verified scheme is retained.
 * When `play.http.forwarded.trustSingleXForwardedProto` is enabled, a lone
 * `X-Forwarded-Proto` entry is associated with the client address instead of
 * being discarded.
 * When `play.http.forwarded.trustXForwardedProtoWithoutXForwardedFor` is
 * enabled, a lone `X-Forwarded-Proto` entry without `X-Forwarded-For` updates
 * the effective scheme of the trusted proxy request without changing the remote
 * identity.
 * When `play.http.forwarded.trustXForwardedSsl` is enabled and
 * `X-Forwarded-Proto` is absent, a lone `X-Forwarded-Ssl` value supplies the
 * original request protocol.
 *
 * Relevant configuration options include:
 * <dl>
 *   <dt>play.http.forwarded.version</dt>
 *   <dd>
 *     The version of the forwarded headers it uses to parse the headers. It can be
 *     <code>x-forwarded</code> for legacy headers or
 *     <code>rfc7239</code> for the definition from the RFC 7239 <br>
 *     Default is x-forwarded.
 *   </dd>
 *   <dt>play.http.forwarded.trustedProxies</dt>
 *   <dd>
 *     A list of proxies that are ignored when getting the remote identity, remote address, or remote port.
 *     It can have optionally an address block size. When the address block size is set,
 *     all IP-addresses in the range of the subnet will be treated as trusted.
 *   </dd>
 *   <dt>play.http.forwarded.trustedProxyIdentifiers</dt>
 *   <dd>
 *     A list of RFC 7239 obfuscated proxy identifiers that are ignored when getting the
 *     remote identity. This only applies when using <code>rfc7239</code>.
 *   </dd>
 *   <dt>play.http.forwarded.trustForwardedHost</dt>
 *   <dd>
 *     Whether to use a trusted RFC 7239 <code>host</code> parameter as the
 *     effective request host. This only applies when using <code>rfc7239</code>.
 *   </dd>
 *   <dt>play.http.forwarded.trustSingleXForwardedProto</dt>
 *   <dd>
 *     Whether one X-Forwarded-Proto value may be associated with a longer
 *     X-Forwarded-For chain.
 *   </dd>
 *   <dt>play.http.forwarded.trustXForwardedProtoWithoutXForwardedFor</dt>
 *   <dd>
 *     Whether one X-Forwarded-Proto value may update the effective scheme
 *     without a forwarded identity.
 *   </dd>
 *   <dt>play.http.forwarded.trustXForwardedPort</dt>
 *   <dd>
 *     Whether one trusted <code>X-Forwarded-Port</code> value replaces the port
 *     in the effective request host. This only applies when using
 *     <code>x-forwarded</code>.
 *   </dd>
 *   <dt>play.http.forwarded.trustXForwardedHost</dt>
 *   <dd>
 *     Whether one trusted <code>X-Forwarded-Host</code> value replaces the
 *     effective request host. This only applies when using
 *     <code>x-forwarded</code>.
 *   </dd>
 *   <dt>play.http.forwarded.trustXForwardedSsl</dt>
 *   <dd>
 *     Whether one trusted <code>X-Forwarded-Ssl</code> value supplies the
 *     original request protocol when <code>X-Forwarded-Proto</code> is absent.
 *     This only applies when using <code>x-forwarded</code>.
 *   </dd>
 * </dl>
 */
private[server] class ForwardedHeaderHandler(configuration: ForwardedHeaderHandlerConfig) {

  /** Update remote, scheme, and authority metadata in one trusted-proxy scan. */
  def forwardedRequest(
      rawRemote: RemoteInfo,
      headers: Headers,
      scheme: Scheme,
      authority: Option[RequestAuthority]
  ): ParsedForwarding = {
    // Forwarding metadata cannot affect a request from an untrusted direct peer,
    // so avoid parsing attacker-controlled header lists that will be discarded.
    if (configuration.isTrustedProxy(rawRemote.node)) {
      parse(rawRemote, headers, scheme, authority)
    } else {
      ParsedForwarding(rawRemote, scheme, authority)
    }
  }

  private def parse(
      rawRemote: RemoteInfo,
      headers: Headers,
      initialScheme: Scheme,
      initialAuthority: Option[RequestAuthority]
  ): ParsedForwarding = {
    // Use a mutable iterator for performance when scanning the
    // header entries. Go through the headers in reverse order because
    // the nearest proxies will be at the end of the list and we need
    // to move backwards through the list to get to the original IP.
    val headerEntries: Iterator[ForwardedEntry] = configuration.forwardedHeaders(headers).reverseIterator
    val forwardingSource                        = configuration.version match {
      case Rfc7239    => ForwardingSource.Rfc7239
      case Xforwarded => ForwardingSource.XForwarded
    }

    def result(
        remote: RemoteInfo,
        scheme: Scheme,
        authority: Option[RequestAuthority],
        acceptedPath: List[RemoteEndpoint]
    ): ParsedForwarding = {
      val selectedRemote = acceptedPath match {
        case _ :: via => remote.copy(forwarding = Some(ForwardingInfo(forwardingSource, via.toVector)))
        case Nil      => remote
      }
      ParsedForwarding(selectedRemote, scheme, authority)
    }

    @tailrec
    def scan(
        prev: RemoteInfo,
        scheme: Scheme,
        authority: Option[RequestAuthority],
        acceptedPath: List[RemoteEndpoint]
    ): ParsedForwarding = {
      // Check if there's a forwarded header for us to scan.
      if (headerEntries.hasNext) {
        // There is a forwarded header from 'prev', so lets check if 'prev' is trusted.
        // If it's a trusted proxy then process the header, otherwise just use 'prev'.

        if (configuration.isTrustedProxy(prev.node)) {
          // 'prev' is a trusted proxy, so we process the next entry.
          val entry = headerEntries.next()
          configuration.validateEntry(entry) match {
            case Left(error) =>
              ForwardedHeaderHandler.logger.debug(
                s"Error with info in forwarding header $entry, using $prev instead: $error."
              )
              result(prev, scheme, authority, acceptedPath)
            case Right(parsedEntry) =>
              val forwardedAuthority = configuration.forwardedAuthority(entry)
              val selectedAuthority  = forwardedAuthority.orElse(authority)
              val selectedScheme     = configuration.forwardedScheme(entry).getOrElse(scheme)
              parsedEntry match {
                case None if forwardedAuthority.isDefined || configuration.forwardedSchemeWithoutFor(entry).isDefined =>
                  // Forwarded metadata can describe the original scheme or host without
                  // identifying the preceding node. Apply that metadata to the current
                  // connection, but do not scan any earlier elements without an identity.
                  result(prev, selectedScheme, selectedAuthority, acceptedPath)
                case None =>
                  ForwardedHeaderHandler.logger.debug(
                    s"Error with info in forwarding header $entry, using $prev instead: No address."
                  )
                  result(prev, scheme, authority, acceptedPath)
                case Some(value) =>
                  val selectedPath = value.remote.endpoint :: acceptedPath
                  if (configuration.canContinueScanning(value.remote.node)) {
                    scan(value.remote, selectedScheme, selectedAuthority, selectedPath)
                  } else {
                    result(value.remote, selectedScheme, selectedAuthority, selectedPath)
                  }
              }
          }
        } else {
          // 'prev' is not a trusted proxy, so we don't scan ahead in the list of
          // forwards, we just return 'prev'.
          result(prev, scheme, authority, acceptedPath)
        }
      } else {
        // No more headers to process, so just use its address.
        result(prev, scheme, authority, acceptedPath)
      }
    }

    // Start scanning through selected remote nodes at the direct peer that
    // connected to the Play server.
    scan(
      rawRemote,
      initialScheme,
      configuration.xForwardedAuthority(rawRemote, headers, initialAuthority),
      acceptedPath = Nil
    )
  }
}

private[server] object ForwardedHeaderHandler {
  private val logger              = Logger(getClass)
  private val XForwardedSsl       = "X-Forwarded-Ssl"
  private val XForwardedSeparator = Pattern.compile(",\\s*")

  /**
   * The version of headers that this Play application understands.
   */
  sealed trait ForwardedHeaderVersion
  case object Rfc7239    extends ForwardedHeaderVersion
  case object Xforwarded extends ForwardedHeaderVersion

  /**
   * Unparsed remote and request metadata from a forwarded header.
   * Each value is optional.
   */
  final case class ForwardedEntry(
      addressString: Option[String],
      protoString: Option[String],
      byString: Option[String] = None,
      hostString: Option[String] = None
  )

  final case class ParsedForwarding(
      remote: RemoteInfo,
      scheme: Scheme,
      authority: Option[RequestAuthority]
  )

  /**
   * Selected remote metadata parsed from a ForwardedEntry.
   */
  final case class ParsedForwardedEntry(remote: RemoteInfo)

  case class ForwardedHeaderHandlerConfig(
      version: ForwardedHeaderVersion,
      trustedProxies: List[Subnet],
      trustedProxyIdentifiers: Set[String] = Set.empty,
      trustSingleXForwardedProto: Boolean = false,
      trustForwardedHost: Boolean = false,
      trustXForwardedProtoWithoutXForwardedFor: Boolean = false,
      trustXForwardedPort: Boolean = false,
      trustXForwardedHost: Boolean = false,
      trustXForwardedSsl: Boolean = false
  ) {
    val nodeIdentifierParser = new NodeIdentifierParser(version)

    private def stripQuotes(s: String): String = {
      if (s.length >= 2 && s.charAt(0) == '"' && s.charAt(s.length - 1) == '"') {
        s.substring(1, s.length - 1)
      } else s
    }

    private def nodePort(port: NodeIdentifierParser.Port): NodePort = port match {
      case PortNumber(number)     => NodePort.Numeric(number)
      case ObfuscatedPort(string) => NodePort.Obfuscated(string)
    }

    /**
     * Parse any Forwarded or X-Forwarded-* headers into a sequence of ForwardedEntry
     * objects containing optional unparsed remote and request metadata.
     * Parameter-specific parsing happens while the trusted proxy chain is scanned.
     * A malformed RFC 7239 field produces an empty entry so trusted-proxy scanning
     * stops at that field.
     */
    def forwardedHeaders(headers: Headers): Seq[ForwardedEntry] = version match {
      case Rfc7239 => {
        val headerValues = headers.getAll("Forwarded")
        val entries      = headerValues.flatMap { headerValue =>
          Rfc7239HeaderParser.parse(headerValue) match {
            case Right(elements) =>
              elements.map { paramMap =>
                ForwardedEntry(
                  paramMap.get("for"),
                  paramMap.get("proto"),
                  byString = paramMap.get("by"),
                  hostString = paramMap.get("host")
                )
              }
            case Left(error) =>
              logger.debug(s"Invalid RFC 7239 Forwarded header, treating it as untrusted: $error")
              Seq(ForwardedEntry(None, None))
          }
        }
        // Forwarded uses 1#forwarded-element, so the combined field value must
        // contain an element even though empty HTTP list members are ignored.
        if (headerValues.nonEmpty && entries.isEmpty) Seq(ForwardedEntry(None, None)) else entries
      }

      case Xforwarded =>
        // Headers may expose a linear Seq. Materialize indexed values once because
        // protocol entries are looked up by their corresponding address index.
        def h(h: Headers, key: String): Vector[String] =
          h.getAll(key)
            .iterator
            .flatMap(value => XForwardedSeparator.split(value).iterator)
            .map(_.trim)
            .map(stripQuotes)
            .toVector

        val forHeaders        = h(headers, HeaderNames.X_FORWARDED_FOR)
        val protoHeaderValues = headers.getAll(HeaderNames.X_FORWARDED_PROTO)
        val protoHeaders      = h(headers, HeaderNames.X_FORWARDED_PROTO)
        val sslProto          = Option
          .when(protoHeaderValues.isEmpty && trustXForwardedSsl)(xForwardedSslProto(headers))
          .flatten

        def protoForIndex(index: Int): Option[String] = {
          if (forHeaders.length == protoHeaders.length) {
            protoHeaders.lift(index)
          } else if (
            trustSingleXForwardedProto &&
            forHeaders.nonEmpty &&
            protoHeaders.length == 1 &&
            index == 0
          ) {
            protoHeaders.headOption
          } else if (protoHeaders.isEmpty && index == 0) {
            sslProto
          } else {
            None
          }
        }

        val protoWithoutFor =
          if (protoHeaders.length == 1 && trustXForwardedProtoWithoutXForwardedFor) {
            protoHeaders.headOption
          } else if (protoHeaderValues.isEmpty) {
            sslProto
          } else {
            None
          }

        if (forHeaders.isEmpty && protoWithoutFor.isDefined) {
          Seq(ForwardedEntry(None, protoWithoutFor))
        } else {
          forHeaders.zipWithIndex.map {
            case (forwardedFor, index) => ForwardedEntry(Some(forwardedFor), protoForIndex(index))
          }
        }
    }

    /**
     * Try to parse a `ForwardedEntry` into a valid `ParsedForwardedEntry`
     * containing the remote identity and port. This method returns `Left` when
     * the forwarded entry is missing an address or either node identifier cannot be parsed.
     */
    def parseEntry(entry: ForwardedEntry): Either[String, ParsedForwardedEntry] = {
      for {
        byNode        <- parseByNode(entry)
        addressString <- entry.addressString.toRight("No address")
        remoteNode    <- parseRemoteNode(addressString).left.map(error => s"Invalid for parameter: $error")
      } yield ParsedForwardedEntry(RemoteInfo(remoteNode, byNode))
    }

    /**
     * Validate every recognized RFC 7239 parameter before an element may affect
     * the selected remote, scheme, or authority. Unknown extension parameters
     * remain intentionally independent of this atomic validation.
     */
    def validateEntry(entry: ForwardedEntry): Either[String, Option[ParsedForwardedEntry]] = {
      val parsedEntry = entry.addressString match {
        case Some(_) => parseEntry(entry).map(Some(_))
        case None    => parseByNode(entry).map(_ => None)
      }

      version match {
        case Rfc7239 =>
          for {
            value <- parsedEntry
            _     <- validateOptional(entry.protoString, "proto")(Scheme.parse)
            _     <- validateOptional(entry.hostString, "host")(RequestAuthority.parse)
          } yield value
        case Xforwarded => parsedEntry
      }
    }

    private def parseByNode(entry: ForwardedEntry): Either[String, Option[RemoteNode]] = {
      entry.byString match {
        case Some(byString) =>
          parseRemoteNode(byString).left.map(error => s"Invalid by parameter: $error").map(Some(_))
        case None => Right(None)
      }
    }

    private def validateOptional[A](
        value: Option[String],
        parameter: String
    )(parse: String => Either[String, A]): Either[String, Unit] = {
      value match {
        case Some(rawValue) => parse(rawValue).left.map(error => s"Invalid $parameter parameter: $error").map(_ => ())
        case None           => Right(())
      }
    }

    private def parseRemoteNode(nodeString: String): Either[String, RemoteNode] = {
      nodeIdentifierParser.parseNode(nodeString).map {
        case (Ip(address), nodePort) =>
          RemoteNode.Ip(address, nodePort.map(this.nodePort))
        case (UnknownIp, nodePort) =>
          RemoteNode.Unknown(nodePort.map(this.nodePort))
        case (ObfuscatedIp(identifier), nodePort) =>
          RemoteNode.Obfuscated(identifier, nodePort.map(this.nodePort))
      }
    }

    /** Check whether a selected remote node may supply trusted forwarding metadata. */
    def isTrustedProxy(node: RemoteNode): Boolean = node match {
      case RemoteNode.Ip(address, _)            => trustedProxies.exists(_.isInRange(address))
      case RemoteNode.Obfuscated(identifier, _) => isTrustedProxyIdentifier(identifier)
      case RemoteNode.Unknown(_)                => false
    }

    /**
     * Check if a forwarded node can be evaluated for trust in the next scan step.
     */
    def canContinueScanning(remoteNode: RemoteNode): Boolean = remoteNode match {
      case RemoteNode.Ip(_, _)                  => true
      case RemoteNode.Obfuscated(identifier, _) => isTrustedProxyIdentifier(identifier)
      case RemoteNode.Unknown(_)                => false
    }

    /**
     * Check if an RFC 7239 obfuscated identifier is considered to be a trusted proxy.
     */
    def isTrustedProxyIdentifier(identifier: String): Boolean = {
      version == Rfc7239 && trustedProxyIdentifiers.contains(identifier)
    }

    /** Return a syntactically valid authority with a usable host from this RFC 7239 element when enabled. */
    def forwardedAuthority(entry: ForwardedEntry): Option[RequestAuthority] = {
      Option
        .when(version == Rfc7239 && trustForwardedHost)(entry.hostString)
        .flatten
        .flatMap(usableForwardedAuthority)
    }

    /** Apply trusted X-Forwarded authority metadata to the effective request host. */
    def xForwardedAuthority(
        remote: RemoteInfo,
        headers: Headers,
        authority: Option[RequestAuthority]
    ): Option[RequestAuthority] = {
      if (
        version == Xforwarded &&
        (trustXForwardedHost || trustXForwardedPort) &&
        isTrustedProxy(remote.node)
      ) {
        val forwardedHost = Option
          .when(trustXForwardedHost)(singleXForwardedValue(headers, HeaderNames.X_FORWARDED_HOST))
          .flatten
          .flatMap(usableForwardedAuthority)
        val forwardedPort = Option
          .when(trustXForwardedPort)(singleXForwardedValue(headers, HeaderNames.X_FORWARDED_PORT))
          .flatten
          .flatMap(parseXForwardedPort)

        (forwardedHost, forwardedPort) match {
          case (Some(value), Some(port)) => Some(value.withPort(Some(port)))
          case (Some(value), None)       => Some(value)
          case (None, Some(port))        => authority.map(_.withPort(Some(port)))
          case (None, None)              => authority
        }
      } else {
        authority
      }
    }

    private def usableForwardedAuthority(value: String): Option[RequestAuthority] = {
      RequestAuthority.parse(value).toOption.filter(_.host.render.nonEmpty)
    }

    private def singleXForwardedValue(headers: Headers, name: String): Option[String] = {
      headers
        .getAll(name)
        .flatMap(_.split(",", -1))
        .map(_.trim) match {
        case Seq(value) if value.nonEmpty => Some(value)
        case _                            => None
      }
    }

    private def parseXForwardedPort(value: String): Option[AuthorityPort] = AuthorityPort.parse(value).toOption

    private def xForwardedSslProto(headers: Headers): Option[String] = {
      singleXForwardedValue(headers, XForwardedSsl).flatMap {
        case value if value.equalsIgnoreCase("on")  => Some("https")
        case value if value.equalsIgnoreCase("off") => Some("http")
        case _                                      => None
      }
    }

    /** Parse trusted protocol metadata from an element. */
    def forwardedScheme(entry: ForwardedEntry): Option[Scheme] = {
      entry.protoString.flatMap(Scheme.parse(_).toOption)
    }

    /** Interpret trusted protocol metadata that has no forwarded identity. */
    def forwardedSchemeWithoutFor(entry: ForwardedEntry): Option[Scheme] = {
      if (
        entry.addressString.isEmpty &&
        (version == Rfc7239 ||
          (version == Xforwarded && (trustXForwardedProtoWithoutXForwardedFor || trustXForwardedSsl)))
      ) {
        forwardedScheme(entry)
      } else {
        None
      }
    }

  }

  object ForwardedHeaderHandlerConfig {
    def apply(configuration: Option[Configuration]): ForwardedHeaderHandlerConfig = {
      val config = configuration.getOrElse(Configuration.reference).get[Configuration]("play.http.forwarded")

      val version = config.get[String]("version") match {
        case "x-forwarded" => Xforwarded
        case "rfc7239"     => Rfc7239
        case _             => throw config.reportError("version", "Forwarded header version must be either x-forwarded or rfc7239")
      }

      val trustedProxyIdentifiers =
        config.getOptional[Seq[String]]("trustedProxyIdentifiers").getOrElse(Seq.empty)
      trustedProxyIdentifiers.foreach {
        case identifier if identifier.equalsIgnoreCase("unknown") =>
          throw config.reportError(
            "trustedProxyIdentifiers",
            "The RFC 7239 unknown identifier cannot be configured as a trusted proxy"
          )
        case identifier if !NodePort.isObfuscatedIdentifier(identifier) =>
          throw config.reportError(
            "trustedProxyIdentifiers",
            s"Invalid RFC 7239 obfuscated identifier '$identifier': expected '_' followed by one or more " +
              "ASCII letters, digits, '.', '_', or '-'"
          )
        case _ =>
      }

      ForwardedHeaderHandlerConfig(
        version,
        config.get[Seq[String]]("trustedProxies").map(Subnet.apply).toList,
        trustedProxyIdentifiers.toSet,
        config.getOptional[Boolean]("trustSingleXForwardedProto").getOrElse(false),
        config.getOptional[Boolean]("trustForwardedHost").getOrElse(false),
        config.getOptional[Boolean]("trustXForwardedProtoWithoutXForwardedFor").getOrElse(false),
        config.getOptional[Boolean]("trustXForwardedPort").getOrElse(false),
        config.getOptional[Boolean]("trustXForwardedHost").getOrElse(false),
        config.getOptional[Boolean]("trustXForwardedSsl").getOrElse(false)
      )
    }
  }
}
