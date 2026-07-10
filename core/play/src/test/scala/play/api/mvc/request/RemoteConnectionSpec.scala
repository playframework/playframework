/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import com.google.common.net.InetAddresses
import org.specs2.mutable.Specification

class RemoteConnectionSpec extends Specification {
  "RemoteConnection" should {
    "default to an unknown remote port" in {
      RemoteConnection("127.0.0.1", secure = false, None).remotePort must beNone
    }

    "store the remote port when created from an address string" in {
      RemoteConnection("127.0.0.1", Some(12345), secure = false, None).remotePort must beSome(12345)
    }

    "expose an IP remote node for IP connections" in {
      val connection = RemoteConnection("127.0.0.1", Some(12345), secure = false, None)

      connection.remoteNode must beEqualTo(
        RemoteConnection.RemoteNode.Ip(InetAddresses.forString("127.0.0.1"), Some(12345))
      )
      connection.remoteIpAddress must beSome(InetAddresses.forString("127.0.0.1"))
      connection.remoteIdentity must beEqualTo("127.0.0.1")
    }

    "return no remote IP address for obfuscated remote nodes" in {
      val connection = RemoteConnection(
        InetAddresses.forString("127.0.0.1"),
        RemoteConnection.RemoteNode.Obfuscated("_hidden", None),
        None,
        secure = false,
        None
      )

      connection.remoteNode must beEqualTo(RemoteConnection.RemoteNode.Obfuscated("_hidden", None))
      connection.remoteIpAddress must beNone
      connection.remoteIdentity must beEqualTo("_hidden")
    }

    "return unknown as the remote identity for unknown remote nodes" in {
      val connection = RemoteConnection(
        InetAddresses.forString("127.0.0.1"),
        RemoteConnection.RemoteNode.Unknown(None),
        None,
        secure = false,
        None
      )

      connection.remoteIdentity must beEqualTo("unknown")
      connection.remoteIpAddress must beNone
      connection.remoteNode must beEqualTo(RemoteConnection.RemoteNode.Unknown(None))
    }

    "store the receiving proxy node when supplied" in {
      val connection = RemoteConnection(
        InetAddresses.forString("127.0.0.1"),
        RemoteConnection.RemoteNode.Ip(InetAddresses.forString("127.0.0.1"), None),
        None,
        Some(RemoteConnection.RemoteNode.Obfuscated("_edge", None)),
        secure = false,
        None
      )

      connection.byNode must beSome(RemoteConnection.RemoteNode.Obfuscated("_edge", None))
    }

    "preserve the receiving proxy node when copying connection metadata" in {
      val byNode     = RemoteConnection.RemoteNode.Obfuscated("_edge", None)
      val connection = RemoteConnection(
        InetAddresses.forString("127.0.0.1"),
        RemoteConnection.RemoteNode.Ip(InetAddresses.forString("127.0.0.1"), None),
        None,
        Some(byNode),
        secure = false,
        None
      )

      connection.withRemotePort(Some(12345)).byNode must beSome(byNode)
      connection.withSecure(secure = true).byNode must beSome(byNode)
      connection.withClientCertificateChain(None).byNode must beSome(byNode)
    }

    "store the remote port when created from an inet address" in {
      RemoteConnection(InetAddresses.forString("127.0.0.1"), Some(12345), secure = false, None).remotePort must beSome(
        12345
      )
    }

    "compare connections with the same remote port as equal" in {
      RemoteConnection("127.0.0.1", Some(12345), secure = false, None) must beEqualTo(
        RemoteConnection("127.0.0.1", Some(12345), secure = false, None)
      )
    }

    "include the remote port when comparing connections" in {
      RemoteConnection("127.0.0.1", Some(12345), secure = false, None) must not(
        beEqualTo(RemoteConnection("127.0.0.1", Some(54321), secure = false, None))
      )
    }

    "include the receiving proxy node when comparing connections" in {
      val address    = InetAddresses.forString("127.0.0.1")
      val remoteNode = RemoteConnection.RemoteNode.Ip(address, None)

      RemoteConnection(
        address,
        remoteNode,
        None,
        Some(RemoteConnection.RemoteNode.Obfuscated("_edge", None)),
        secure = false,
        None
      ) must not(
        beEqualTo(RemoteConnection(address, remoteNode, None, None, secure = false, None))
      )
    }

    "use the same hash code for equal connections" in {
      RemoteConnection("127.0.0.1", Some(12345), secure = false, None).hashCode must_== RemoteConnection(
        "127.0.0.1",
        Some(12345),
        secure = false,
        None
      ).hashCode
    }
  }
}
