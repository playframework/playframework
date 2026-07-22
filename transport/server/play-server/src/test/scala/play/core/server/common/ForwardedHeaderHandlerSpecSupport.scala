/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import java.net.InetAddress

import com.google.common.net.InetAddresses
import org.specs2.mutable.Specification
import play.api.mvc.request.NodePort
import play.api.mvc.request.RemoteInfo
import play.api.mvc.request.RequestAuthority
import play.api.mvc.request.Scheme
import play.api.mvc.Headers
import play.api.Configuration
import play.core.server.common.ForwardedHeaderHandler._

private[common] trait ForwardedHeaderHandlerSpecSupport { self: Specification =>
  def noConfigHandler =
    new ForwardedHeaderHandler(ForwardedHeaderHandlerConfig(None))

  def handler(config: Map[String, Any]) =
    new ForwardedHeaderHandler(
      ForwardedHeaderHandlerConfig(Some(Configuration.from(config).withFallback(Configuration.reference)))
    )

  def forwardedResultToLocalhost(config: Map[String, Any], headersText: String): ForwardedResult =
    forwardedResult(forwardedRequestToLocalhost(config, headersText))

  def forwardedHostToLocalhost(
      config: Map[String, Any],
      headersText: String,
      host: String = "localhost"
  ): Option[String] = {
    val initial = RequestAuthority.parse(host).toOption.map(_.render)
    forwardedRequestToLocalhost(config, headersText, host).authority.map(_.render).filterNot(initial.contains)
  }

  def forwardedRequestToLocalhost(
      config: Map[String, Any],
      headersText: String,
      host: String = "localhost"
  ): ParsedForwarding =
    handler(config).forwardedRequest(
      RemoteInfo.ip("127.0.0.1", None),
      headers(headersText),
      Scheme.Http,
      RequestAuthority.parse(host).toOption
    )

  def forwardedResultToLocalhostWithPort(
      config: Map[String, Any],
      remotePort: Option[Int],
      headersText: String
  ): ForwardedResult =
    forwardedResult(
      handler(config).forwardedRequest(
        RemoteInfo.ip("127.0.0.1", remotePort.map(NodePort.Numeric.apply)),
        headers(headersText),
        Scheme.Http,
        authority = None
      )
    )

  def forwardedResult(parsed: ParsedForwarding): ForwardedResult = {
    // Existing scanner tests compare only the selected endpoint and scheme. Dedicated provenance
    // tests assert the complete accepted path so those concerns remain independently reviewable.
    ForwardedResult(parsed.remote.copy(forwarding = None), parsed.scheme)
  }

  def forwardedResultFrom(
      forwardedHeaderHandler: ForwardedHeaderHandler,
      initial: ForwardedResult,
      requestHeaders: Headers
  ): ForwardedResult =
    forwardedResult(
      forwardedHeaderHandler.forwardedRequest(
        initial.remote,
        requestHeaders,
        initial.scheme,
        authority = None
      )
    )

  def expectedResult(address: String, isHttps: Boolean): ForwardedResult =
    expectedResult(InetAddresses.forString(address), isHttps)

  def expectedResult(remote: RemoteInfo, isHttps: Boolean): ForwardedResult =
    ForwardedResult(remote, if (isHttps) Scheme.Https else Scheme.Http)

  def expectedResult(
      address: String,
      port: Option[Int],
      isHttps: Boolean
  ): ForwardedResult =
    expectedResult(InetAddresses.forString(address), port, isHttps)

  def expectedResult(address: InetAddress, isHttps: Boolean): ForwardedResult =
    expectedResult(RemoteInfo.ip(address, None), isHttps)

  def expectedResult(
      address: InetAddress,
      port: Option[Int],
      isHttps: Boolean
  ): ForwardedResult =
    expectedResult(RemoteInfo.ip(address, port.map(NodePort.Numeric.apply)), isHttps)

  def version(s: String) = {
    Map("play.http.forwarded.version" -> s)
  }

  def trustedProxies(s: String*) = {
    Map("play.http.forwarded.trustedProxies" -> s)
  }

  def trustedProxyIdentifiers(s: String*) = {
    Map("play.http.forwarded.trustedProxyIdentifiers" -> s)
  }

  def trustSingleXForwardedProto(b: Boolean) = {
    Map("play.http.forwarded.trustSingleXForwardedProto" -> b)
  }

  def trustXForwardedProtoWithoutXForwardedFor(b: Boolean) = {
    Map("play.http.forwarded.trustXForwardedProtoWithoutXForwardedFor" -> b)
  }

  def trustXForwardedSsl(b: Boolean) = {
    Map("play.http.forwarded.trustXForwardedSsl" -> b)
  }

  def trustXForwardedPort(b: Boolean) = {
    Map("play.http.forwarded.trustXForwardedPort" -> b)
  }

  def trustXForwardedHost(b: Boolean) = {
    Map("play.http.forwarded.trustXForwardedHost" -> b)
  }

  def trustForwardedHost(b: Boolean) = {
    Map("play.http.forwarded.trustForwardedHost" -> b)
  }

  def headers(s: String): Headers = {
    def split(s: String, regex: String): Option[(String, String)] = s.split(regex, 2).toList match {
      case k :: v :: Nil => Some(k -> v)
      case _             => None
    }

    new Headers(s.split("\r?\n").toSeq.flatMap(split(_, ":\\s*")))
  }

  def processHeaders(
      config: Map[String, Any],
      headers: Headers
  ): Seq[(ForwardedEntry, Either[String, ParsedForwardedEntry], Option[Boolean], Option[Scheme])] = {
    val configuration = ForwardedHeaderHandlerConfig(Some(Configuration.from(config)))
    configuration.forwardedHeaders(headers).map { forwardedEntry =>
      val errorOrRemote = configuration.parseEntry(forwardedEntry)
      val trusted       = errorOrRemote match {
        case Left(_)      => None
        case Right(entry) => Some(configuration.isTrustedProxy(entry.remote.node))
      }
      (forwardedEntry, errorOrRemote, trusted, forwardedEntry.protoString.flatMap(Scheme.parse(_).toOption))
    }
  }

  def addr(ip: String): InetAddress = InetAddresses.forString(ip)

  val localhost: InetAddress = addr("127.0.0.1")
}

final case class ForwardedResult(remote: RemoteInfo, scheme: Scheme)
