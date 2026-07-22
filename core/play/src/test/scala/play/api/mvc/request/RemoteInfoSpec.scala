/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import scala.util.Try

import org.specs2.mutable.Specification
import com.google.common.net.InetAddresses

class RemoteInfoSpec extends Specification {
  "NodePort" should {
    "accept numeric boundary values and valid obfuscated identifiers" in {
      NodePort.Numeric(0).value must_== 0
      NodePort.Numeric(65535).value must_== 65535
      NodePort.Obfuscated("_Edge.1_test-port").value must_== "_Edge.1_test-port"
    }

    "reject numeric values outside the network-port range" in {
      Seq(-1, 65536, Int.MinValue, Int.MaxValue)
        .forall(value => Try(NodePort.Numeric(value)).isFailure) must beTrue
    }

    "reject malformed obfuscated ports" in {
      Seq(null, "", "_", "port", "_bad value", "_bad!")
        .forall(value => Try(NodePort.Obfuscated(value)).isFailure) must beTrue
    }
  }

  "RemoteInfo" should {
    "derive every IP view from its selected node" in {
      val address = InetAddresses.forString("192.0.2.43")
      val remote  = RemoteInfo(RemoteNode.Ip(address, Some(NodePort.Numeric(53124))), None)

      remote.identity must_== "192.0.2.43"
      remote.ipAddress must beSome(address)
      remote.nodePort must beSome(NodePort.Numeric(53124))
      remote.port must beSome(53124)
    }

    "represent obfuscated identities without inventing a fallback IP" in {
      val remote = RemoteInfo(RemoteNode.Obfuscated("_anonymous", Some(NodePort.Obfuscated("_source"))), None)

      remote.identity must_== "_anonymous"
      remote.ipAddress must beNone
      remote.nodePort must beSome(NodePort.Obfuscated("_source"))
      remote.port must beNone
    }

    "represent unknown identities without inventing a fallback IP" in {
      val remote = RemoteInfo(RemoteNode.Unknown(Some(NodePort.Numeric(1234))), None)

      remote.identity must_== "unknown"
      remote.ipAddress must beNone
      remote.nodePort must beSome(NodePort.Numeric(1234))
      remote.port must beSome(1234)
    }

    "retain the receiving proxy node when changing the selected port" in {
      val byNode     = RemoteNode.Obfuscated("_edge", None)
      val forwarding = ForwardingInfo(
        ForwardingSource.Rfc7239,
        Vector(RemoteEndpoint(RemoteNode.Obfuscated("_proxy", None), None))
      )
      val remote = RemoteInfo(RemoteNode.Unknown(None), Some(byNode), Some(forwarding))

      remote.withPort(Some(1234)) must_==
        RemoteInfo(RemoteNode.Unknown(Some(NodePort.Numeric(1234))), Some(byNode), Some(forwarding))
      remote.withNodePort(Some(NodePort.Obfuscated("_source"))) must_==
        RemoteInfo(RemoteNode.Unknown(Some(NodePort.Obfuscated("_source"))), Some(byNode), Some(forwarding))
    }

    "clear a selected node port" in {
      val remote = RemoteInfo(RemoteNode.Ip(InetAddresses.forString("192.0.2.43"), Some(NodePort.Numeric(443))), None)

      remote.withPort(None).node must_== RemoteNode.Ip(InetAddresses.forString("192.0.2.43"), None)
    }

    "validate numeric ports changed through the convenience API" in {
      val remote = RemoteInfo(RemoteNode.Unknown(None), None)

      remote.withPort(Some(0)).port must beSome(0)
      remote.withPort(Some(65535)).port must beSome(65535)
      remote.withPort(Some(-1)) must throwA[IllegalArgumentException]
      remote.withPort(Some(65536)) must throwA[IllegalArgumentException]
    }

    "derive the initial selected node from a direct peer" in {
      val address = InetAddresses.forString("127.0.0.1")

      RemoteInfo.fromPeer(PeerEndpoint(address, Some(43210))) must_==
        RemoteInfo(RemoteNode.Ip(address, Some(NodePort.Numeric(43210))), None)
    }

    "distinguish direct remotes from a single accepted forwarded endpoint" in {
      val selected  = RemoteNode.Ip(InetAddresses.forString("203.0.113.43"), None)
      val direct    = RemoteInfo(selected, None)
      val forwarded = RemoteInfo(
        selected,
        None,
        Some(ForwardingInfo(ForwardingSource.XForwarded, Vector.empty))
      )

      direct.isForwarded must beFalse
      direct.forwarding must beNone
      direct.path must_== Vector(RemoteEndpoint(selected, None))
      forwarded.isForwarded must beTrue
      forwarded.forwarding must beSome(ForwardingInfo(ForwardingSource.XForwarded, Vector.empty))
      forwarded.path must_== Vector(RemoteEndpoint(selected, None))
      forwarded must not(beEqualTo(direct))
    }

    "expose an accepted forwarding path without duplicating the direct transport peer" in {
      val selected   = RemoteEndpoint(RemoteNode.Ip(InetAddresses.forString("203.0.113.43"), None), None)
      val firstProxy = RemoteEndpoint(
        RemoteNode.Ip(InetAddresses.forString("192.0.2.10"), None),
        Some(RemoteNode.Obfuscated("_edge", None))
      )
      val secondProxy = RemoteEndpoint(RemoteNode.Obfuscated("_internal", None), None)
      val remote      = RemoteInfo(
        selected.node,
        selected.byNode,
        Some(ForwardingInfo(ForwardingSource.Rfc7239, Vector(firstProxy, secondProxy)))
      )

      remote.endpoint must_== selected
      remote.path must_== Vector(selected, firstProxy, secondProxy)
    }

    "create IP nodes from literals without DNS resolution" in {
      RemoteInfo.ip("192.0.2.43", None).node must beAnInstanceOf[RemoteNode.Ip]
      RemoteInfo.ip("example.com", None) must throwA[IllegalArgumentException]
    }

    "reject null selected and receiving nodes" in {
      RemoteInfo(null, None) must throwA[IllegalArgumentException]
      RemoteInfo(RemoteNode.Unknown(None), null) must throwA[IllegalArgumentException]
      RemoteInfo(RemoteNode.Unknown(None), Some(null)) must throwA[IllegalArgumentException]
      RemoteInfo(RemoteNode.Unknown(None), None, null) must throwA[IllegalArgumentException]
      RemoteInfo(RemoteNode.Unknown(None), None, Some(null)) must throwA[IllegalArgumentException]
      RemoteEndpoint(null, None) must throwA[IllegalArgumentException]
      RemoteEndpoint(RemoteNode.Unknown(None), null) must throwA[IllegalArgumentException]
      ForwardingInfo(null, Vector.empty) must throwA[IllegalArgumentException]
      ForwardingInfo(ForwardingSource.Rfc7239, null) must throwA[IllegalArgumentException]
      ForwardingInfo(ForwardingSource.Rfc7239, Vector(null)) must throwA[IllegalArgumentException]
    }

    "use structural value semantics" in {
      val first  = RemoteInfo.ip("192.0.2.43", Some(NodePort.Numeric(1234)))
      val second = RemoteInfo.ip("192.0.2.43", Some(NodePort.Numeric(1234)))

      first must_== second
      first.hashCode must_== second.hashCode
      first must not(beEqualTo(RemoteInfo.ip("192.0.2.43", Some(NodePort.Numeric(4321)))))
    }
  }

  "RemoteNode" should {
    "support every valid RFC 7239 obfuscated identifier character" in {
      RemoteNode.Obfuscated("_Edge.1_test-node", None).identifier must_== "_Edge.1_test-node"
    }

    "reject malformed obfuscated identifiers" in {
      Seq(null, "", "_", "edge", "_bad value", "_bad!")
        .forall(identifier => Try(RemoteNode.Obfuscated(identifier, None)).isFailure) must beTrue
    }

    "reject null addresses and port options" in {
      RemoteNode.Ip(null, None) must throwA[IllegalArgumentException]
      RemoteNode.Ip(InetAddresses.forString("192.0.2.43"), null) must throwA[IllegalArgumentException]
      RemoteNode.Unknown(null) must throwA[IllegalArgumentException]
      RemoteNode.Unknown(Some(null)) must throwA[IllegalArgumentException]
    }

    "preserve the concrete node variant when changing its port" in {
      val port = Some(NodePort.Numeric(1234))

      RemoteNode.Ip(InetAddresses.forString("192.0.2.43"), None).withPort(port) must
        beAnInstanceOf[RemoteNode.Ip]
      RemoteNode.Obfuscated("_hidden", None).withPort(port) must beAnInstanceOf[RemoteNode.Obfuscated]
      RemoteNode.Unknown(None).withPort(port) must beAnInstanceOf[RemoteNode.Unknown]
    }
  }
}
