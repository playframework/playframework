/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import org.specs2.mutable.Specification
import play.api.mvc.request.ForwardingInfo
import play.api.mvc.request.ForwardingSource
import play.api.mvc.request.RemoteEndpoint
import play.api.mvc.request.RemoteNode

class ForwardedHeaderProvenanceSpec extends Specification with ForwardedHeaderHandlerSpecSupport {
  "ForwardedHeaderHandler remote provenance" should {
    "leave direct and metadata-only requests without a forwarded path" in {
      val direct = forwardedRequestToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        ""
      )
      val metadataOnly = forwardedRequestToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1") ++ trustForwardedHost(true),
        """
          |Forwarded: by=_edge;proto=https;host=public.example
        """.stripMargin
      )

      direct.remote.forwarding must beNone
      direct.remote.isForwarded must beFalse
      metadataOnly.remote.forwarding must beNone
      metadataOnly.remote.isForwarded must beFalse
    }

    "distinguish one accepted forwarded endpoint from a direct request" in {
      val result = forwardedRequestToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for=203.0.113.43;by=_edge
        """.stripMargin
      )

      result.remote.forwarding must beSome(ForwardingInfo(ForwardingSource.Rfc7239, Vector.empty))
      result.remote.path must_== Vector(
        RemoteEndpoint(
          RemoteNode.Ip(addr("203.0.113.43"), None),
          Some(RemoteNode.Obfuscated("_edge", None))
        )
      )
    }

    "retain the accepted RFC 7239 path and every element's by node" in {
      val result = forwardedRequestToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1", "192.0.2.0/24"),
        """
          |Forwarded: for=203.0.113.43;by=_client-edge
          |Forwarded: for=192.0.2.10;by=_middle-edge
          |Forwarded: for=192.0.2.20;by=_play-edge
        """.stripMargin
      )

      result.remote.forwarding.map(_.source) must_== Some(ForwardingSource.Rfc7239)
      result.remote.path must_== Vector(
        RemoteEndpoint(
          RemoteNode.Ip(addr("203.0.113.43"), None),
          Some(RemoteNode.Obfuscated("_client-edge", None))
        ),
        RemoteEndpoint(
          RemoteNode.Ip(addr("192.0.2.10"), None),
          Some(RemoteNode.Obfuscated("_middle-edge", None))
        ),
        RemoteEndpoint(
          RemoteNode.Ip(addr("192.0.2.20"), None),
          Some(RemoteNode.Obfuscated("_play-edge", None))
        )
      )
      result.remote.path.map(_.node) must not(contain(RemoteNode.Ip(addr("127.0.0.1"), None)))
    }

    "retain the accepted X-Forwarded-For path in client-to-Play order" in {
      val result = forwardedRequestToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1", "192.0.2.0/24"),
        """
          |X-Forwarded-For: 203.0.113.43, 192.0.2.10, 192.0.2.20
        """.stripMargin
      )

      result.remote.forwarding must beSome(
        ForwardingInfo(
          ForwardingSource.XForwarded,
          Vector(
            RemoteEndpoint(RemoteNode.Ip(addr("192.0.2.10"), None), None),
            RemoteEndpoint(RemoteNode.Ip(addr("192.0.2.20"), None), None)
          )
        )
      )
      result.remote.path.map(_.node) must_== Vector(
        RemoteNode.Ip(addr("203.0.113.43"), None),
        RemoteNode.Ip(addr("192.0.2.10"), None),
        RemoteNode.Ip(addr("192.0.2.20"), None)
      )
    }

    "exclude entries beyond the first untrusted endpoint" in {
      val result = forwardedRequestToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1", "192.0.2.0/24"),
        """
          |X-Forwarded-For: 203.0.113.43, 198.51.100.17, 192.0.2.20
        """.stripMargin
      )

      result.remote.path.map(_.node) must_== Vector(
        RemoteNode.Ip(addr("198.51.100.17"), None),
        RemoteNode.Ip(addr("192.0.2.20"), None)
      )
    }

    "preserve the accepted suffix when an older RFC 7239 field is malformed" in {
      val result = forwardedRequestToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1", "192.0.2.0/24"),
        """
          |Forwarded: for=203.0.113.43
          |Forwarded: for="???"
          |Forwarded: for=192.0.2.20;by=_play-edge
        """.stripMargin
      )

      result.remote.path must_== Vector(
        RemoteEndpoint(
          RemoteNode.Ip(addr("192.0.2.20"), None),
          Some(RemoteNode.Obfuscated("_play-edge", None))
        )
      )
      result.remote.forwarding must beSome(ForwardingInfo(ForwardingSource.Rfc7239, Vector.empty))
    }

    "retain provenance when unknown terminates trusted-proxy scanning" in {
      val result = forwardedRequestToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for=unknown;by=_edge
        """.stripMargin
      )

      result.remote.node must_== RemoteNode.Unknown(None)
      result.remote.forwarding must beSome(ForwardingInfo(ForwardingSource.Rfc7239, Vector.empty))
    }
  }
}
