/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import java.net.InetAddress
import java.security.cert.X509Certificate

import com.google.common.net.InetAddresses

/**
 * Contains information about the connection from the remote client to the server.
 * Connection information may come from the socket or from other metadata attached
 * to the request by an upstream proxy, e.g. `Forwarded` headers.
 */
trait RemoteConnection {
  /**
   * The remote client's address.
   */
  def remoteAddress: InetAddress

  /**
   * The remote client's address in text form.
   */
  def remoteAddressString: String = remoteAddress.getHostAddress

  /**
   * Whether or not the connection was over a secure (e.g. HTTPS) connection.
   */
  def secure: Boolean

  /**
   * The X509 certificate chain presented by a client during SSL requests.
   */
  def clientCertificateChain: Option[Seq[X509Certificate]]

  override def toString: String = s"RemoteAddress($remoteAddressString, secure=$secure, certs=$clientCertificateChain)"

  override def equals(obj: scala.Any): Boolean = obj match {
    case that: RemoteConnection =>
      (this.remoteAddress == that.remoteAddress) &&
        (this.secure == that.secure) &&
        (this.clientCertificateChain == that.clientCertificateChain)
    case _ => false
  }
}

object RemoteConnection {
  /**
   * Create a RemoteConnection object. The address string is parsed lazily.
   */
  def apply(remoteAddressString: String, secure: Boolean, clientCertificateChain: Option[Seq[X509Certificate]]): RemoteConnection = {
    val s = secure
    val ras = remoteAddressString
    val ccc = clientCertificateChain
    new RemoteConnection {
      override lazy val remoteAddress: InetAddress = InetAddresses.forString(ras)
      override val remoteAddressString: String = ras
      override val secure: Boolean = s
      override val clientCertificateChain: Option[Seq[X509Certificate]] = ccc
    }
  }

  /**
   * Create a RemoteConnection object.
   */
  def apply(remoteAddress: InetAddress, secure: Boolean, clientCertificateChain: Option[Seq[X509Certificate]]): RemoteConnection = {
    val s = secure
    val ra = remoteAddress
    val ccc = clientCertificateChain
    new RemoteConnection {
      override val remoteAddress: InetAddress = ra
      override val secure: Boolean = s
      override val clientCertificateChain: Option[Seq[X509Certificate]] = ccc
    }
  }
}