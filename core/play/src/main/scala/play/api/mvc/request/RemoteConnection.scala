/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import java.net.InetAddress
import java.security.cert.X509Certificate

import scala.jdk.javaapi.OptionConverters

import com.google.common.net.InetAddresses
import play.api.mvc.request.RemoteConnection.RemoteNode

/**
 * Contains metadata about the remote connection for a request.
 *
 * Connection metadata may come from the socket or from metadata attached to the
 * request by an upstream proxy, e.g. `Forwarded` headers.
 */
trait RemoteConnection {

  /**
   * The remote client's IP address, or a fallback IP address when the remote
   * client is represented by an RFC 7239 unknown or obfuscated identifier.
   */
  @deprecated(
    "Use remoteIdentity for the remote identity as a string, remoteNode for the structured RFC 7239 remote identity, or remoteIpAddress for an IP-only value. " +
      "This legacy address cannot represent RFC 7239 unknown or obfuscated identifiers and may " +
      "return a fallback proxy address when the selected forwarded identity is not an IP.",
    "3.1.0"
  )
  def remoteAddress: InetAddress

  /**
   * The remote client's IP address in text form, or a fallback IP address when the
   * remote client is represented by an RFC 7239 unknown or obfuscated identifier.
   */
  @deprecated(
    "Use remoteIdentity for the remote identity as a string, remoteNode for the structured RFC 7239 remote identity, or remoteIpAddress for an IP-only value. " +
      "This legacy address cannot represent RFC 7239 unknown or obfuscated identifiers and may " +
      "return a fallback proxy address when the selected forwarded identity is not an IP.",
    "3.1.0"
  )
  def remoteAddressString: String = remoteAddress.getHostAddress

  /**
   * The selected remote identity.
   *
   * When the identity is an IP address, the node may also include the selected
   * remote port. RFC 7239 `unknown` and obfuscated identifiers are represented
   * explicitly so applications do not need to parse a string value.
   */
  def remoteNode: RemoteNode = RemoteNode.Ip(remoteAddress, remotePort)

  /**
   * The RFC 7239 `by` node for the selected forwarded element, if present.
   *
   * This identifies the proxy interface that received the request represented by
   * `remoteNode`. It is not the selected remote identity; use `remoteNode` for
   * that.
   */
  def byNode: Option[RemoteNode] = None

  /**
   * The selected remote IP address, if the remote identity is an IP address.
   */
  def remoteIpAddress: Option[InetAddress] = remoteNode match {
    case RemoteNode.Ip(address, _) => Some(address)
    case _                         => None
  }

  /**
   * The selected remote identity as a string.
   *
   * If the remote identity is an IP address, this returns the address in textual
   * form. If the remote identity is an RFC 7239 `unknown` or obfuscated
   * identifier, this returns that identifier.
   */
  def remoteIdentity: String = remoteNode match {
    case RemoteNode.Ip(address, _)            => address.getHostAddress
    case RemoteNode.Obfuscated(identifier, _) => identifier
    case RemoteNode.Unknown(_)                => "unknown"
  }

  /**
   * The remote client's port, if known.
   */
  def remotePort: Option[Int] = None

  /**
   * Whether or not the connection was over a secure (e.g. HTTPS) connection.
   */
  def secure: Boolean

  /**
   * The X509 certificate chain presented by a client during SSL requests.
   */
  def clientCertificateChain: Option[Seq[X509Certificate]]

  /**
   * The Java API representation of this remote connection.
   */
  def asJava: play.mvc.Http.RemoteConnection = new play.mvc.Http.RemoteConnection(this)

  /**
   * Return a copy of this remote connection with the given remote port.
   */
  def withRemotePort(remotePort: Option[Int]): RemoteConnection =
    RemoteConnection(remoteAddress, remoteNode, remotePort, byNode, secure, clientCertificateChain)

  /**
   * Return a copy of this remote connection with the given secure flag.
   */
  def withSecure(secure: Boolean): RemoteConnection =
    RemoteConnection(remoteAddress, remoteNode, remotePort, byNode, secure, clientCertificateChain)

  /**
   * Return a copy of this remote connection with the given client certificate chain.
   */
  def withClientCertificateChain(clientCertificateChain: Option[Seq[X509Certificate]]): RemoteConnection =
    RemoteConnection(remoteAddress, remoteNode, remotePort, byNode, secure, clientCertificateChain)

  override def toString: String =
    s"RemoteConnection(address=${remoteAddress.getHostAddress}, remoteNode=$remoteNode, byNode=$byNode, port=$remotePort, secure=$secure, certs=$clientCertificateChain)"

  override def equals(obj: scala.Any): Boolean = obj match {
    case that: RemoteConnection =>
      (this.remoteAddress == that.remoteAddress) &&
      (this.remoteNode == that.remoteNode) &&
      (this.byNode == that.byNode) &&
      (this.remotePort == that.remotePort) &&
      (this.secure == that.secure) &&
      (this.clientCertificateChain == that.clientCertificateChain)
    case _ => false
  }

  override def hashCode(): Int =
    (remoteAddress, remoteNode, byNode, remotePort, secure, clientCertificateChain).hashCode()
}

object RemoteConnection {
  sealed trait RemoteNode {

    /**
     * The Java API representation of this remote node.
     */
    def asJava: play.mvc.Http.RemoteNode = this match {
      case RemoteNode.Ip(address, port) =>
        new play.mvc.Http.RemoteNode.Ip(address, OptionConverters.toJava(port.map(Integer.valueOf)))
      case RemoteNode.Obfuscated(identifier, port) =>
        new play.mvc.Http.RemoteNode.Obfuscated(identifier, OptionConverters.toJava(port))
      case RemoteNode.Unknown(port) =>
        new play.mvc.Http.RemoteNode.Unknown(OptionConverters.toJava(port))
    }
  }

  object RemoteNode {
    final case class Ip(address: InetAddress, port: Option[Int])          extends RemoteNode
    final case class Obfuscated(identifier: String, port: Option[String]) extends RemoteNode
    final case class Unknown(port: Option[String])                        extends RemoteNode

    def ip(address: InetAddress, port: Option[Int]): RemoteNode = Ip(address, port)
  }

  /**
   * Create a RemoteConnection object. The address string is parsed lazily.
   */
  def apply(
      remoteAddressString: String,
      secure: Boolean,
      clientCertificateChain: Option[Seq[X509Certificate]]
  ): RemoteConnection = {
    apply(remoteAddressString, None, secure, clientCertificateChain)
  }

  /**
   * Create a RemoteConnection object. The address string is parsed lazily.
   */
  def apply(
      remoteAddressString: String,
      remotePort: Option[Int],
      secure: Boolean,
      clientCertificateChain: Option[Seq[X509Certificate]]
  ): RemoteConnection = {
    val s   = secure
    val ras = remoteAddressString
    val rp  = remotePort
    val ccc = clientCertificateChain
    new RemoteConnection {
      override lazy val remoteAddress: InetAddress                      = InetAddresses.forString(ras)
      override val remoteAddressString: String                          = ras
      override lazy val remoteNode: RemoteNode                          = RemoteNode.Ip(remoteAddress, rp)
      override val remotePort: Option[Int]                              = rp
      override val secure: Boolean                                      = s
      override val clientCertificateChain: Option[Seq[X509Certificate]] = ccc
    }
  }

  /**
   * Create a RemoteConnection object.
   */
  def apply(
      remoteAddress: InetAddress,
      secure: Boolean,
      clientCertificateChain: Option[Seq[X509Certificate]]
  ): RemoteConnection = {
    apply(remoteAddress, None, secure, clientCertificateChain)
  }

  /**
   * Create a RemoteConnection object.
   */
  def apply(
      remoteAddress: InetAddress,
      remotePort: Option[Int],
      secure: Boolean,
      clientCertificateChain: Option[Seq[X509Certificate]]
  ): RemoteConnection = {
    apply(remoteAddress, RemoteNode.Ip(remoteAddress, remotePort), remotePort, secure, clientCertificateChain)
  }

  /**
   * Create a RemoteConnection object.
   */
  def apply(
      remoteAddress: InetAddress,
      remoteNode: RemoteNode,
      remotePort: Option[Int],
      secure: Boolean,
      clientCertificateChain: Option[Seq[X509Certificate]]
  ): RemoteConnection = {
    apply(remoteAddress, remoteNode, remotePort, None, secure, clientCertificateChain)
  }

  /**
   * Create a RemoteConnection object.
   */
  def apply(
      remoteAddress: InetAddress,
      remoteNode: RemoteNode,
      remotePort: Option[Int],
      byNode: Option[RemoteNode],
      secure: Boolean,
      clientCertificateChain: Option[Seq[X509Certificate]]
  ): RemoteConnection = {
    val s   = secure
    val ra  = remoteAddress
    val rn  = remoteNode
    val rp  = remotePort
    val bn  = byNode
    val ccc = clientCertificateChain
    new RemoteConnection {
      override val remoteAddress: InetAddress                           = ra
      override val remoteNode: RemoteNode                               = rn
      override val byNode: Option[RemoteNode]                           = bn
      override val remotePort: Option[Int]                              = rp
      override val secure: Boolean                                      = s
      override val clientCertificateChain: Option[Seq[X509Certificate]] = ccc
    }
  }
}
