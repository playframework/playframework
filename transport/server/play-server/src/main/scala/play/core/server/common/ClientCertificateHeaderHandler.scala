/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import scala.util.control.NonFatal

import com.typesafe.config.ConfigMemorySize
import play.api.mvc.request.ClientCertificateInfo
import play.api.mvc.request.ClientCertificateSource
import play.api.mvc.request.TransportConnection
import play.api.mvc.request.XForwardedClientCert
import play.api.mvc.Headers
import play.api.Configuration
import play.api.Logger
import play.core.server.common.ClientCertificateHeaderHandler._

/** Selects direct or trusted forwarded client-certificate metadata for a request. */
private[server] final class ClientCertificateHeaderHandler(configuration: Config) {
  private val logger = Logger(classOf[ClientCertificateHeaderHandler])

  def clientCertificates(transport: TransportConnection, headers: Headers): Selection = {
    configuration.mode match {
      case Off                                           => Selection.direct(transport)
      case _ if !configuration.isTrustedProxy(transport) => Selection.direct(transport)
      case Rfc9440                                       =>
        Rfc9440ClientCertificateParser.parse(headers, configuration.limits) match {
          case Right(value) => Selection(value, Vector.empty)
          case Left(error)  =>
            throw new InvalidClientCertificateHeaderException(
              s"Forwarded client certificate limits exceeded: $error"
            )
        }
      case XForwardedClientCertMode =>
        XForwardedClientCertHeaderParser.parse(
          headers.getAll(play.api.http.HeaderNames.X_FORWARDED_CLIENT_CERT),
          XForwardedClientCertHeaderParser.Limits(
            configuration.limits.maxHeaderBytes,
            configuration.limits.maxDecodedBytes,
            configuration.limits.maxCertificateBytes,
            configuration.limits.maxChainLength
          )
        ) match {
          case Right(assertions) => Selection.fromXForwardedClientCert(assertions)
          case Left(error)       =>
            throw new InvalidClientCertificateHeaderException(s"Invalid forwarded client certificate: $error")
        }
    }
  }

  /**
   * Select certificate metadata for an error request without repeating selection that already
   * failed during normal request conversion.
   */
  def clientCertificatesForErrorRequest(
      transport: TransportConnection,
      headers: Headers,
      requestFailure: Throwable
  ): Selection = {
    if (requestFailure.isInstanceOf[InvalidClientCertificateHeaderException]) {
      Selection.empty
    } else {
      try {
        clientCertificates(transport, headers)
      } catch {
        case _: InvalidClientCertificateHeaderException => Selection.empty
        case NonFatal(error)                            =>
          logger.warn(
            "Failed to apply forwarded client certificate metadata to an error request; omitting effective metadata.",
            error
          )
          Selection.empty
      }
    }
  }

  /** Select only the effective certificate when XFCC assertion metadata is not needed. */
  def clientCertificate(transport: TransportConnection, headers: Headers): Option[ClientCertificateInfo] =
    clientCertificates(transport, headers).clientCertificate
}

private[server] object ClientCertificateHeaderHandler {
  private[server] final class InvalidClientCertificateHeaderException(message: String)
      extends IllegalArgumentException(message)

  sealed trait Mode
  case object Off                      extends Mode
  case object Rfc9440                  extends Mode
  case object XForwardedClientCertMode extends Mode

  final case class Selection(
      clientCertificate: Option[ClientCertificateInfo],
      xForwardedClientCertificates: Vector[XForwardedClientCert]
  )

  object Selection {
    val empty: Selection = Selection(None, Vector.empty)

    def direct(transport: TransportConnection): Selection =
      Selection(ClientCertificateInfo.fromTransport(transport), Vector.empty)

    def fromXForwardedClientCert(assertions: Vector[XForwardedClientCert]): Selection = {
      val effective = assertions.headOption.flatMap { assertion =>
        assertion.certificate.map { certificate =>
          ClientCertificateInfo(certificate, assertion.chain, ClientCertificateSource.XForwardedClientCert)
        }
      }
      Selection(effective, assertions)
    }
  }

  final case class Config(mode: Mode, trustedProxies: List[Subnet], limits: ClientCertificateHeaderLimits) {
    def isTrustedProxy(transport: TransportConnection): Boolean =
      trustedProxies.exists(_.isInRange(transport.peer.address))
  }

  object Config {
    def apply(configuration: Option[Configuration]): Config = {
      val config = configuration
        .getOrElse(Configuration.reference)
        .get[Configuration]("play.http.forwarded.clientCertificates")

      val mode = config.get[String]("mode") match {
        case "off"                     => Off
        case "rfc9440"                 => Rfc9440
        case "x-forwarded-client-cert" => XForwardedClientCertMode
        case _                         =>
          throw config.reportError(
            "mode",
            "Forwarded client certificate mode must be off, rfc9440, or x-forwarded-client-cert"
          )
      }

      if (config.get[String]("xForwardedClientCert.policy") != "sanitized-single") {
        throw config.reportError(
          "xForwardedClientCert.policy",
          "X-Forwarded-Client-Cert policy must be sanitized-single"
        )
      }
      if (config.get[String]("xForwardedClientCert.format") != "text") {
        throw config.reportError("xForwardedClientCert.format", "X-Forwarded-Client-Cert format must be text")
      }

      def positiveBytes(path: String): Long = {
        val value = config.get[ConfigMemorySize](path).toBytes
        if (value <= 0) throw config.reportError(path, "Forwarded client certificate size limits must be positive")
        value
      }

      val maxChainLength = config.get[Int]("limits.maxChainLength")
      if (maxChainLength < 0) {
        throw config.reportError("limits.maxChainLength", "maxChainLength must not be negative")
      }
      Config(
        mode,
        config.get[Seq[String]]("trustedProxies").map(Subnet.apply).toList,
        ClientCertificateHeaderLimits(
          positiveBytes("limits.maxHeaderBytes"),
          positiveBytes("limits.maxDecodedBytes"),
          positiveBytes("limits.maxCertificateBytes"),
          maxChainLength
        )
      )
    }
  }
}
