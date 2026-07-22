/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import java.net.InetAddress

import com.google.common.net.InetAddresses
import org.specs2.mutable.Specification
import play.api.mvc.request.NodePort
import play.api.mvc.request.RemoteInfo
import play.api.mvc.request.RemoteNode
import play.api.mvc.request.RequestAuthority
import play.api.mvc.request.Scheme
import play.api.mvc.Headers
import play.api.Configuration
import play.api.PlayException
import play.core.server.common.ForwardedHeaderHandler._

class ForwardedHeaderTrustedIdentitySpec extends Specification with ForwardedHeaderHandlerSpecSupport {
  "ForwardedHeaderHandler" should {
    "accept valid trusted proxy identifiers in config" in {
      val identifiers   = Seq("_edge", "_A0._-")
      val configuration = ForwardedHeaderHandlerConfig(
        Some(
          Configuration
            .from(version("rfc7239") ++ trustedProxyIdentifiers(identifiers*))
            .withFallback(Configuration.reference)
        )
      )

      configuration.trustedProxyIdentifiers mustEqual identifiers.toSet
    }

    "reject unknown as a trusted proxy identifier in config" in {
      handler(version("rfc7239") ++ trustedProxyIdentifiers("unknown")) must throwA[PlayException]
      handler(version("rfc7239") ++ trustedProxyIdentifiers("UNKNOWN")) must throwA[PlayException]
    }

    "reject malformed trusted proxy identifiers in config" in {
      def isRejected(identifier: String): Boolean = {
        try {
          handler(version("rfc7239") ++ trustedProxyIdentifiers(identifier))
          false
        } catch {
          case _: PlayException => true
        }
      }

      Seq("edge", "_", "_bad!", "_ümlaut").forall(isRejected) must beTrue
    }

    "not reuse proto from a trusted obfuscated proxy for the client with rfc7239" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++
          trustedProxies("192.168.1.1/24", "127.0.0.1") ++
          trustedProxyIdentifiers("_edge"),
        """
          |Forwarded: for=203.0.113.43
          |Forwarded: for="_edge";proto=http
          |Forwarded: for=192.168.1.10
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", false)
    }

    "continue scanning through trusted obfuscated identifiers with rfc7239" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++
          trustedProxies("192.168.1.1/24", "127.0.0.1") ++
          trustedProxyIdentifiers("_edge"),
        """
          |Forwarded: for=203.0.113.43;proto=https
          |Forwarded: for="_edge";proto=http
          |Forwarded: for=192.168.1.10
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", true)
    }

    "match trusted obfuscated identifiers exactly with rfc7239" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++
          trustedProxies("192.168.1.1/24", "127.0.0.1") ++
          trustedProxyIdentifiers("_edge"),
        """
          |Forwarded: for=203.0.113.43;proto=https
          |Forwarded: for="_edge2";proto=http
          |Forwarded: for=192.168.1.10
        """.stripMargin
      ) mustEqual expectedResult(RemoteInfo(RemoteNode.Obfuscated("_edge2", None), None), isHttps = false)
    }

    "not trust whitespace-padded obfuscated identifiers with rfc7239" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1") ++ trustedProxyIdentifiers("_edge"),
        """
          |Forwarded: for=203.0.113.43;proto=https, for=" _edge ";proto=http
        """.stripMargin
      ) mustEqual expectedResult(localhost, false)
    }

  }
}
