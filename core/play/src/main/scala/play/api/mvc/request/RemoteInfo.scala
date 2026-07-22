/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import java.net.InetAddress

import scala.jdk.javaapi.OptionConverters

import com.google.common.net.InetAddresses

/** A selected or intermediate endpoint from an accepted forwarding path. */
final case class RemoteEndpoint(node: RemoteNode, byNode: Option[RemoteNode]) {
  require(node != null, "A remote endpoint node must not be null")
  require(
    byNode != null && byNode.forall(_ != null),
    "A remote endpoint by-node option must not be null or contain null"
  )

  /** Convert this remote endpoint to the Java API. */
  def asJava: play.mvc.Http.RemoteEndpoint =
    new play.mvc.Http.RemoteEndpoint(node.asJava, OptionConverters.toJava(byNode.map(_.asJava)))
}

object RemoteEndpoint {

  /** Java-friendly factory from typed node values. */
  def create(node: RemoteNode, byNode: Option[RemoteNode]): RemoteEndpoint = RemoteEndpoint(node, byNode)
}

/** The header family from which accepted remote forwarding metadata was derived. */
sealed trait ForwardingSource {

  /** Convert this forwarding source to the Java API. */
  def asJava: play.mvc.Http.ForwardingSource = this match {
    case ForwardingSource.Rfc7239    => play.mvc.Http.ForwardingSource.RFC_7239
    case ForwardingSource.XForwarded => play.mvc.Http.ForwardingSource.X_FORWARDED
  }
}

object ForwardingSource {

  /** The standardized RFC 7239 `Forwarded` header. */
  case object Rfc7239 extends ForwardingSource

  /** The de facto `X-Forwarded-*` header family. */
  case object XForwarded extends ForwardingSource

  /** Java-friendly accessor for the RFC 7239 source. */
  def rfc7239: ForwardingSource = Rfc7239

  /** Java-friendly accessor for the X-Forwarded source. */
  def xForwarded: ForwardingSource = XForwarded
}

/** Accepted forwarding metadata for a selected remote endpoint. */
final case class ForwardingInfo(source: ForwardingSource, via: Vector[RemoteEndpoint]) {
  require(source != null, "A forwarding source must not be null")
  require(via != null && via.forall(_ != null), "A forwarding path must not be null or contain null")

  /** Convert this forwarding metadata to the Java API. */
  def asJava: play.mvc.Http.ForwardingInfo =
    new play.mvc.Http.ForwardingInfo(
      source.asJava,
      java.util.List.copyOf(play.libs.Scala.asJava(via.map(_.asJava)))
    )
}

object ForwardingInfo {

  /** Java-friendly factory from a source and accepted intermediate endpoints. */
  def create(source: ForwardingSource, via: Seq[RemoteEndpoint]): ForwardingInfo = {
    require(via != null, "A forwarding path must not be null")
    ForwardingInfo(source, via.toVector)
  }
}

/** Immutable metadata about the selected remote node for a request. */
final case class RemoteInfo(
    node: RemoteNode,
    byNode: Option[RemoteNode],
    forwarding: Option[ForwardingInfo] = None
) {
  require(node != null, "A selected remote node must not be null")
  require(byNode != null && byNode.forall(_ != null), "A by-node option must not be null or contain null")
  require(
    forwarding != null && forwarding.forall(_ != null),
    "A forwarding metadata option must not be null or contain null"
  )

  /** The selected endpoint represented by [[node]] and [[byNode]]. */
  def endpoint: RemoteEndpoint = RemoteEndpoint(node, byNode)

  /**
   * The accepted remote path in client-to-Play order.
   *
   * For a direct request this contains only [[endpoint]], which represents the direct transport
   * peer. For a forwarded request it contains the selected endpoint followed by the trusted
   * intermediate proxy endpoints Play traversed, but excludes the independently observed direct
   * transport peer.
   */
  def path: Vector[RemoteEndpoint] = endpoint +: forwarding.fold(Vector.empty[RemoteEndpoint])(_.via)

  /** Whether the selected endpoint was obtained from accepted forwarding metadata. */
  def isForwarded: Boolean = forwarding.isDefined

  /** The selected remote identity as an IP literal, obfuscated identifier, or `unknown`. */
  def identity: String = node match {
    case RemoteNode.Ip(address, _)            => address.getHostAddress
    case RemoteNode.Obfuscated(identifier, _) => identifier
    case RemoteNode.Unknown(_)                => "unknown"
  }

  /** The selected remote IP address, when the node is an IP identity. */
  def ipAddress: Option[InetAddress] = node match {
    case RemoteNode.Ip(address, _) => Some(address)
    case _                         => None
  }

  /** The numeric or obfuscated port attached to the selected node. */
  def nodePort: Option[NodePort] = node.port

  /** The numeric projection of [[nodePort]], when present. */
  def port: Option[Int] = nodePort.collect { case NodePort.Numeric(value) => value }

  /** Return a copy with a different selected-node port. */
  def withNodePort(nodePort: Option[NodePort]): RemoteInfo = copy(node = node.withPort(nodePort))

  /** Return a copy with a different numeric selected-node port. */
  def withPort(port: Option[Int]): RemoteInfo = withNodePort(port.map(NodePort.Numeric.apply))

  /** Convert this selected remote metadata to the Java API. */
  def asJava: play.mvc.Http.RemoteInfo =
    new play.mvc.Http.RemoteInfo(
      node.asJava,
      OptionConverters.toJava(byNode.map(_.asJava)),
      OptionConverters.toJava(forwarding.map(_.asJava))
    )
}

object RemoteInfo {

  /** Java-friendly factory from typed node values. */
  def create(node: RemoteNode, byNode: Option[RemoteNode]): RemoteInfo = RemoteInfo(node, byNode)

  /** Java-friendly factory from typed node and forwarding values. */
  def create(
      node: RemoteNode,
      byNode: Option[RemoteNode],
      forwarding: Option[ForwardingInfo]
  ): RemoteInfo = RemoteInfo(node, byNode, forwarding)

  /** Create selected remote metadata for an IP literal without performing DNS resolution. */
  def ip(address: String, port: Option[NodePort]): RemoteInfo =
    ip(InetAddresses.forString(address), port)

  /** Create selected remote metadata for an IP address. */
  def ip(address: InetAddress, port: Option[NodePort]): RemoteInfo =
    RemoteInfo(RemoteNode.Ip(address, port), None)

  /** Derive the initial selected remote node from Play's direct transport peer. */
  def fromPeer(peer: PeerEndpoint): RemoteInfo = {
    require(peer != null, "A direct transport peer must not be null")
    ip(peer.address, peer.port.map(value => NodePort.Numeric(value)))
  }
}
