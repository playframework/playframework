/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import java.net.InetAddress
import java.security.cert.X509Certificate

import scala.jdk.javaapi.OptionConverters

/** The network endpoint directly connected to Play. */
final case class PeerEndpoint(address: InetAddress, port: Option[Int]) {
  require(address != null, "A direct transport peer address must not be null")
  require(port != null, "A direct transport peer port option must not be null")
  require(
    port.forall(value => value >= 0 && value <= PeerEndpoint.MaxNetworkPort),
    s"A direct transport peer port must be between 0 and ${PeerEndpoint.MaxNetworkPort}: $port"
  )

  def asJava: play.mvc.Http.PeerEndpoint =
    new play.mvc.Http.PeerEndpoint(address, OptionConverters.toJava(port.map(Integer.valueOf)))
}

object PeerEndpoint {
  private val MaxNetworkPort = 65535

  def create(address: InetAddress, port: Option[Int]): PeerEndpoint = PeerEndpoint(address, port)
}

/** TLS metadata observed on the connection directly terminating at Play. */
final case class TransportTls(peerCertificates: Seq[X509Certificate]) {
  require(
    peerCertificates != null && peerCertificates.forall(_ != null),
    "A direct transport peer certificate sequence must not be null or contain null"
  )

  def asJava: play.mvc.Http.TransportTls =
    new play.mvc.Http.TransportTls(java.util.List.copyOf(play.libs.Scala.asJava(peerCertificates)))
}

object TransportTls {
  def create(peerCertificates: Seq[X509Certificate]): TransportTls = TransportTls(peerCertificates)
}

/** Immutable metadata about the transport connection directly terminating at Play. */
final case class TransportConnection(peer: PeerEndpoint, tls: Option[TransportTls]) {
  require(peer != null, "A direct transport peer must not be null")
  require(tls != null && tls.forall(_ != null), "A direct transport TLS option must not be null or contain null")

  def asJava: play.mvc.Http.TransportConnection =
    new play.mvc.Http.TransportConnection(peer.asJava, OptionConverters.toJava(tls.map(_.asJava)))
}

object TransportConnection {

  def create(peer: PeerEndpoint, tls: Option[TransportTls]): TransportConnection = TransportConnection(peer, tls)
}
