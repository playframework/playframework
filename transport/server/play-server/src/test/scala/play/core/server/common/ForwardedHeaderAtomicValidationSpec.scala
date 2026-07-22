/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import java.net.InetAddress

import com.google.common.net.InetAddresses
import org.specs2.mutable.Specification
import play.api.mvc.request.ForwardingInfo
import play.api.mvc.request.ForwardingSource
import play.api.mvc.request.NodePort
import play.api.mvc.request.RemoteInfo
import play.api.mvc.request.RemoteNode
import play.api.mvc.request.RequestAuthority
import play.api.mvc.request.Scheme
import play.api.mvc.Headers
import play.api.Configuration
import play.api.PlayException
import play.core.server.common.ForwardedHeaderHandler._

class ForwardedHeaderAtomicValidationSpec extends Specification with ForwardedHeaderHandlerSpecSupport {
  "ForwardedHeaderHandler" should {
    "reject invalid known rfc7239 parameters atomically at a multi-hop trust boundary" in {
      val config = version("rfc7239") ++
        trustedProxies("127.0.0.1", "192.168.1.1/24") ++
        trustForwardedHost(true)
      val invalidEntries = Seq(
        """for="???";by=198.51.100.10;proto=http;host=attacker.example""",
        """for="[198.51.100.17]";by=198.51.100.10;proto=http;host=attacker.example""",
        """for=[198.51.100.17];by=198.51.100.10;proto=http;host=attacker.example""",
        """for=198.51.100.17;by="???";proto=http;host=attacker.example""",
        """for=198.51.100.17;by="[198.51.100.10]";proto=http;host=attacker.example""",
        """for=198.51.100.17;by=[198.51.100.10];proto=http;host=attacker.example""",
        """for=198.51.100.17;by=198.51.100.10;proto=1https;host=attacker.example""",
        """for=198.51.100.17;by=198.51.100.10;proto=http;host="user@attacker.example"""
      )
      val expected = ParsedForwarding(
        RemoteInfo(
          RemoteNode.Ip(addr("192.168.1.43"), None),
          Some(RemoteNode.Obfuscated("_verified", None)),
          Some(ForwardingInfo(ForwardingSource.Rfc7239, Vector.empty))
        ),
        Scheme.Https,
        Some(RequestAuthority.parseOrThrow("verified.example"))
      )

      invalidEntries.map { invalidEntry =>
        forwardedRequestToLocalhost(
          config,
          s"""
             |Forwarded: for=203.0.113.43;by=_client;proto=ws;host=client.example
             |Forwarded: $invalidEntry
             |Forwarded: for=192.168.1.43;by=_verified;proto=https;host=verified.example
          """.stripMargin,
          "internal.example"
        )
      } mustEqual Seq.fill(invalidEntries.size)(expected)
    }

    "apply none of a metadata-only rfc7239 element when one known parameter is invalid" in {
      val config         = version("rfc7239") ++ trustedProxies("127.0.0.1") ++ trustForwardedHost(true)
      val invalidEntries = Seq(
        """proto=https;host="user@public.example""",
        """proto=1https;host=public.example""",
        """by="???";proto=https;host=public.example"""
      )
      val expected = ParsedForwarding(
        RemoteInfo.ip(localhost, None),
        Scheme.Http,
        Some(RequestAuthority.parseOrThrow("internal.example"))
      )

      invalidEntries.map { invalidEntry =>
        forwardedRequestToLocalhost(
          config,
          s"""
             |Forwarded: for=203.0.113.43;proto=https;host=client.example
             |Forwarded: $invalidEntry
          """.stripMargin,
          "internal.example"
        )
      } mustEqual Seq.fill(invalidEntries.size)(expected)
    }

    "ignore valid unknown rfc7239 extension parameters" in {
      val forwarding = forwardedRequestToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1") ++ trustForwardedHost(true),
        """
          |Forwarded: for=203.0.113.43;by=_edge;proto=https;host=public.example;ext="???";another=1https
        """.stripMargin,
        "internal.example"
      )

      forwarding mustEqual ParsedForwarding(
        RemoteInfo(
          RemoteNode.Ip(addr("203.0.113.43"), None),
          Some(RemoteNode.Obfuscated("_edge", None)),
          Some(ForwardingInfo(ForwardingSource.Rfc7239, Vector.empty))
        ),
        Scheme.Https,
        Some(RequestAuthority.parseOrThrow("public.example"))
      )
    }

    "reject invalid rfc7239 host values even when host forwarding is disabled" in {
      val forwarding = forwardedRequestToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Host: internal.example
          |Forwarded: for=203.0.113.43;proto=https;host="user@example.org"
        """.stripMargin,
        "internal.example"
      )

      forwarding.authority must beSome(RequestAuthority.parseOrThrow("internal.example"))
      forwarding.scheme mustEqual Scheme.Http
      forwarding.remote mustEqual RemoteInfo.ip(localhost, None)
    }

  }
}
