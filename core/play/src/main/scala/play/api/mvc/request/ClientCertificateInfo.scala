/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import java.security.cert.X509Certificate

/** The source from which Play selected an effective client certificate. */
sealed trait ClientCertificateSource {

  /** Convert this source to the Java API. */
  def asJava: play.mvc.Http.ClientCertificateSource = this match {
    case ClientCertificateSource.DirectTransport      => play.mvc.Http.ClientCertificateSource.DIRECT_TRANSPORT
    case ClientCertificateSource.Rfc9440              => play.mvc.Http.ClientCertificateSource.RFC_9440
    case ClientCertificateSource.XForwardedClientCert => play.mvc.Http.ClientCertificateSource.X_FORWARDED_CLIENT_CERT
  }
}

object ClientCertificateSource {

  /** The certificate was observed on the TLS connection directly terminating at Play. */
  case object DirectTransport extends ClientCertificateSource

  /** The certificate was accepted from the RFC 9440 `Client-Cert` header fields. */
  case object Rfc9440 extends ClientCertificateSource

  /** The certificate was accepted from `X-Forwarded-Client-Cert`. */
  case object XForwardedClientCert extends ClientCertificateSource

  /** Java-friendly accessor for the direct-transport source. */
  def directTransport: ClientCertificateSource = DirectTransport

  /** Java-friendly accessor for the RFC 9440 source. */
  def rfc9440: ClientCertificateSource = Rfc9440

  /** Java-friendly accessor for the `X-Forwarded-Client-Cert` source. */
  def xForwardedClientCert: ClientCertificateSource = XForwardedClientCert
}

/**
 * The effective X.509 client certificate selected for a request.
 *
 * @param certificate the leaf certificate identifying the client
 * @param chain remaining effective chain certificates in leaf-to-root order, excluding [[certificate]]
 * @param source where Play obtained the certificate information
 */
final case class ClientCertificateInfo(
    certificate: X509Certificate,
    chain: Vector[X509Certificate],
    source: ClientCertificateSource
) {
  require(certificate != null, "An effective client leaf certificate must not be null")
  require(
    chain != null && chain.forall(_ != null),
    "An effective client certificate chain must not be null or contain null"
  )
  require(!chain.contains(certificate), "An effective client certificate chain must not repeat the leaf certificate")
  require(source != null, "An effective client certificate source must not be null")

  /** The effective leaf-and-chain sequence, with the leaf first. */
  def certificates: Vector[X509Certificate] = certificate +: chain

  /** Convert this client certificate information to the Java API. */
  def asJava: play.mvc.Http.ClientCertificateInfo =
    new play.mvc.Http.ClientCertificateInfo(
      certificate,
      java.util.List.copyOf(play.libs.Scala.asJava(chain)),
      source.asJava
    )
}

object ClientCertificateInfo {

  /** Java-friendly factory from a leaf certificate, its remaining chain, and source. */
  def create(
      certificate: X509Certificate,
      chain: Seq[X509Certificate],
      source: ClientCertificateSource
  ): ClientCertificateInfo = {
    require(chain != null, "An effective client certificate chain must not be null")
    ClientCertificateInfo(certificate, chain.toVector, source)
  }

  /** Derive direct effective client certificate information from Play's transport metadata. */
  private[play] def fromTransport(transport: TransportConnection): Option[ClientCertificateInfo] = {
    require(transport != null, "Direct transport metadata must not be null")
    transport.tls.flatMap { tls =>
      tls.peerCertificates.headOption.map { certificate =>
        ClientCertificateInfo(
          certificate,
          tls.peerCertificates.iterator.drop(1).filterNot(_ == certificate).toVector,
          ClientCertificateSource.DirectTransport
        )
      }
    }
  }
}
