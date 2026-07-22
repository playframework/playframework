/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import java.net.InetAddress

import scala.jdk.javaapi.OptionConverters

/** A typed RFC 7239 node identity with an optional numeric or obfuscated port. */
sealed trait RemoteNode {

  /** The optional numeric or obfuscated port attached to this node. */
  def port: Option[NodePort]

  /** Return a copy of this node with the given port. */
  def withPort(port: Option[NodePort]): RemoteNode = this match {
    case RemoteNode.Ip(address, _)            => RemoteNode.Ip(address, port)
    case RemoteNode.Obfuscated(identifier, _) => RemoteNode.Obfuscated(identifier, port)
    case RemoteNode.Unknown(_)                => RemoteNode.Unknown(port)
  }

  /** Convert this node to the Java API. */
  def asJava: play.mvc.Http.RemoteNode = this match {
    case RemoteNode.Ip(address, port) =>
      new play.mvc.Http.RemoteNode.Ip(address, OptionConverters.toJava(port.map(_.asJava)))
    case RemoteNode.Obfuscated(identifier, port) =>
      new play.mvc.Http.RemoteNode.Obfuscated(identifier, OptionConverters.toJava(port.map(_.asJava)))
    case RemoteNode.Unknown(port) =>
      new play.mvc.Http.RemoteNode.Unknown(OptionConverters.toJava(port.map(_.asJava)))
  }
}

object RemoteNode {

  /** An IP node identity. */
  final case class Ip(address: InetAddress, port: Option[NodePort]) extends RemoteNode {
    require(address != null, "A remote IP address must not be null")
    validatePort(port)
  }

  /** An RFC 7239 obfuscated node identity. */
  final case class Obfuscated(identifier: String, port: Option[NodePort]) extends RemoteNode {
    require(NodePort.isObfuscatedIdentifier(identifier), s"Invalid obfuscated remote identifier: '$identifier'")
    validatePort(port)
  }

  /** An RFC 7239 unknown node identity. */
  final case class Unknown(port: Option[NodePort]) extends RemoteNode {
    validatePort(port)
  }

  /** Java-friendly factory for an IP node. */
  def ip(address: InetAddress, port: Option[NodePort]): RemoteNode = Ip(address, port)

  /** Java-friendly factory for an obfuscated node. */
  def obfuscated(identifier: String, port: Option[NodePort]): RemoteNode = Obfuscated(identifier, port)

  /** Java-friendly factory for an unknown node. */
  def unknown(port: Option[NodePort]): RemoteNode = Unknown(port)

  private def validatePort(port: Option[NodePort]): Unit =
    require(port != null && port.forall(_ != null), "A remote node port option must not be null or contain null")

}
