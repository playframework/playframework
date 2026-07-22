/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc

import java.net.InetAddress

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

import com.google.common.net.InetAddresses
import org.specs2.mutable.Specification
import play.api.http.HttpConfiguration
import play.api.libs.typedmap.TypedMap
import play.api.mvc.request.DefaultRequestFactory
import play.api.mvc.request.NodePort
import play.api.mvc.request.PeerEndpoint
import play.api.mvc.request.RemoteInfo
import play.api.mvc.request.RemoteNode
import play.api.mvc.request.RequestAuthority
import play.api.mvc.request.RequestTarget
import play.api.mvc.request.Scheme
import play.api.mvc.request.TransportConnection
import play.api.mvc.Headers
import play.api.mvc.RequestHeader
import play.mvc.Http.HeaderNames

class RequestHeaderSpec extends Specification {
  private def requestHeader(headers: (String, String)*): RequestHeader = {
    val peer         = PeerEndpoint(InetAddresses.forString("127.0.0.1"), None)
    val transport    = TransportConnection(peer, None)
    val remote       = RemoteInfo.fromPeer(peer)
    val target       = RequestTarget("/", "", Map.empty)
    val scalaHeaders = Headers(headers*)
    new DefaultRequestFactory(HttpConfiguration()).createRequestHeader(
      transport = transport,
      clientCertificate = None,
      xForwardedClientCertificates = Vector.empty,
      remote = remote,
      scheme = Scheme.Http,
      authority = RequestHeader
        .initialAuthority("GET", target, scalaHeaders)
        .fold(error => throw new IllegalArgumentException(error), identity),
      method = "GET",
      target = target,
      version = "",
      headers = scalaHeaders,
      attrs = TypedMap.empty
    )
  }

  def headers(additionalHeaders: Map[String, java.util.List[String]] = Map.empty) = {
    val headers = (Map("a" -> List("b1", "b2").asJava, "c" -> List("d1", "d2").asJava) ++ additionalHeaders).asJava
    new Http.Headers(headers)
  }

  "RequestHeader" should {
    "headers" in {
      "check if the header exists" in {
        headers().contains("a") must beTrue
        headers().contains("non-existent") must beFalse
      }

      "get a single header value" in {
        headers().get("a").toScala must beSome("b1")
        headers().get("c").toScala must beSome("d1")
      }

      "get all header values" in {
        headers().getAll("a").asScala must containTheSameElementsAs(Seq("b1", "b2"))
        headers().getAll("c").asScala must containTheSameElementsAs(Seq("d1", "d2"))
      }

      "handle header names case insensitively" in {
        "when getting the header" in {
          headers().get("a").toScala must beSome("b1")
          headers().get("c").toScala must beSome("d1")

          headers().get("A").toScala must beSome("b1")
          headers().get("C").toScala must beSome("d1")
        }

        "when checking if the header exists" in {
          headers().contains("a") must beTrue
          headers().contains("A") must beTrue
        }
      }

      "can add new headers" in {
        val hs = headers()
        val h  = hs.adding("new", "value")
        hs mustNotEqual h
        h.contains("new") must beTrue
        hs.contains("new") must beFalse
        h.get("new").toScala must beSome("value")
        hs.get("new").toScala must beNone
      }

      "can add new headers with a list of values" in {
        val hs = headers()
        val h  = hs.adding("new", List("v1", "v2", "v3").asJava)
        hs mustNotEqual h
        h.getAll("new").asScala must containTheSameElementsAs(Seq("v1", "v2", "v3"))
        hs.getAll("new").asScala must not contain (anyOf("v1", "v2", "v3"))
      }

      "remove a header" in {
        val hs = headers()
        val h  = hs.adding("to-be-removed", "value")
        hs mustNotEqual h
        h.contains("to-be-removed") must beTrue
        hs.contains("to-be-removed") must beFalse
        val rh = h.removing("to-be-removed")
        rh mustNotEqual h
        rh.contains("to-be-removed") must beFalse
        h.contains("to-be-removed") must beTrue
      }
    }

    "has body" in {
      "when there is a content-length greater than zero" in {
        requestHeader(HeaderNames.CONTENT_LENGTH -> "10").asJava.hasBody must beTrue
      }

      "when there is a transfer-encoding header" in {
        requestHeader(HeaderNames.TRANSFER_ENCODING -> "gzip").asJava.hasBody must beTrue
      }
    }

    "has no body" in {
      "when there is not a content-length greater than zero" in {
        requestHeader(HeaderNames.CONTENT_LENGTH -> "0").asJava.hasBody must beFalse
      }

      "when there is not a transfer-encoding header" in {
        requestHeader().asJava.hasBody must beFalse
      }
    }

    "remote port" in {
      "expose the selected remote port" in {
        val remote = RemoteInfo(
          RemoteNode.Ip(InetAddresses.forString("127.0.0.1"), Some(NodePort.Numeric(12345))),
          None
        )
        val request = requestHeader().withRemote(remote)

        request.asJava.remote.port must beEqualTo(java.util.Optional.of(12345))
      }

      "leave the numeric projection empty for an obfuscated port" in {
        val remote = RemoteInfo(
          RemoteNode.Obfuscated("_hidden", Some(NodePort.Obfuscated("_port"))),
          None
        )
        val request = requestHeader().withRemote(remote).asJava

        request.remote.nodePort must beEqualTo(
          java.util.Optional.of(new Http.NodePort.Obfuscated("_port"))
        )
        request.remote.port must beEqualTo(java.util.Optional.empty[Integer])
      }

      "be set by the request builder" in {
        val remote = new Http.RemoteInfo(
          new Http.RemoteNode.Ip(
            InetAddresses.forString("127.0.0.1"),
            java.util.Optional.of(new Http.NodePort.Numeric(12345))
          ),
          java.util.Optional.empty[Http.RemoteNode]
        )
        val request = new Http.RequestBuilder().remote(remote).build()

        request.remote must beEqualTo(remote)
        request.asScala.remote.port must beSome(12345)
        request.asScala.remote.node must beEqualTo(
          RemoteNode.Ip(
            InetAddresses.forString("127.0.0.1"),
            Some(NodePort.Numeric(12345))
          )
        )
      }
    }

    "remote node" in {
      "convert selected remote metadata between Scala and Java" in {
        val via = Vector(
          play.api.mvc.request.RemoteEndpoint(
            RemoteNode.Obfuscated("_proxy", None),
            Some(RemoteNode.Ip(InetAddresses.forString("192.0.2.10"), None))
          )
        )
        val scalaRemote = RemoteInfo(
          RemoteNode.Ip(
            InetAddresses.forString("127.0.0.1"),
            Some(NodePort.Numeric(12345))
          ),
          Some(RemoteNode.Obfuscated("_edge", None)),
          Some(
            play.api.mvc.request.ForwardingInfo(
              play.api.mvc.request.ForwardingSource.Rfc7239,
              via
            )
          )
        )
        val javaRemote = scalaRemote.asJava

        javaRemote.asScala must beEqualTo(scalaRemote)
        javaRemote.node must beEqualTo(
          new Http.RemoteNode.Ip(
            InetAddresses.forString("127.0.0.1"),
            java.util.Optional.of(new Http.NodePort.Numeric(12345))
          )
        )
        javaRemote.byNode must beEqualTo(
          java.util.Optional.of(new Http.RemoteNode.Obfuscated("_edge", java.util.Optional.empty[Http.NodePort]))
        )
        javaRemote.ipAddress must beEqualTo(java.util.Optional.of(InetAddresses.forString("127.0.0.1")))
        javaRemote.identity must beEqualTo("127.0.0.1")
        javaRemote.nodePort must beEqualTo(
          java.util.Optional.of(new Http.NodePort.Numeric(12345))
        )
        javaRemote.port must beEqualTo(java.util.Optional.of(12345))
        javaRemote.isForwarded must beTrue
        javaRemote.forwarding.orElseThrow().source must beEqualTo(Http.ForwardingSource.RFC_7239)
        javaRemote.path.size must beEqualTo(2)
        javaRemote.path.get(1).asScala must beEqualTo(via.head)
      }

      "give Java selected remote records value semantics" in {
        val first = new Http.RemoteInfo(
          new Http.RemoteNode.Unknown(java.util.Optional.empty[Http.NodePort]),
          java.util.Optional.empty[Http.RemoteNode]
        )
        val second = new Http.RemoteInfo(
          new Http.RemoteNode.Unknown(java.util.Optional.empty[Http.NodePort]),
          java.util.Optional.empty[Http.RemoteNode]
        )
        val different = new Http.RemoteInfo(
          new Http.RemoteNode.Obfuscated("_hidden", java.util.Optional.empty[Http.NodePort]),
          java.util.Optional.empty[Http.RemoteNode]
        )

        first mustEqual second
        first.hashCode mustEqual second.hashCode
        first mustNotEqual different
      }

      "convert remote node wrappers to and from scala" in {
        val ip = new Http.RemoteNode.Ip(
          InetAddresses.forString("192.0.2.1"),
          java.util.Optional.of(new Http.NodePort.Numeric(443))
        )
        val obfuscated = new Http.RemoteNode.Obfuscated(
          "_hidden",
          java.util.Optional.of(new Http.NodePort.Obfuscated("_port"))
        )
        val unknown = new Http.RemoteNode.Unknown(java.util.Optional.empty[Http.NodePort])

        Seq[Http.RemoteNode](ip, obfuscated, unknown).foreach { node =>
          node.asScala.asJava must beEqualTo(node)
        }
        ok
      }

      "expose the scala request remote node" in {
        val request = requestHeader().withRemote(
          RemoteInfo(
            RemoteNode.Ip(InetAddresses.forString("127.0.0.1"), Some(NodePort.Numeric(12345))),
            None
          )
        )

        request.asJava.remote.node must beEqualTo(
          new Http.RemoteNode.Ip(
            InetAddresses.forString("127.0.0.1"),
            java.util.Optional.of(new Http.NodePort.Numeric(12345))
          )
        )
        request.asJava.remote.ipAddress must beEqualTo(
          java.util.Optional.of(InetAddresses.forString("127.0.0.1"))
        )
      }

      "expose an empty remote IP address for obfuscated remote nodes" in {
        val request = requestHeader().withRemote(RemoteInfo(RemoteNode.Obfuscated("_hidden", None), None))

        request.asJava.remote.node must beEqualTo(
          new Http.RemoteNode.Obfuscated("_hidden", java.util.Optional.empty[Http.NodePort])
        )
        request.asJava.remote.ipAddress must beEqualTo(java.util.Optional.empty[InetAddress])
        request.asJava.remote.identity must beEqualTo("_hidden")
      }

      "reject invalid Java remote values at construction" in {
        ({
          new Http.RemoteInfo(null, java.util.Optional.empty[Http.RemoteNode])
        } must throwA[NullPointerException])
          .and({
            new Http.RemoteInfo(
              new Http.RemoteNode.Unknown(java.util.Optional.empty[Http.NodePort]),
              null
            )
          } must throwA[NullPointerException])
          .and({
            new Http.RemoteNode.Obfuscated("not-obfuscated", java.util.Optional.empty[Http.NodePort])
          } must throwA[IllegalArgumentException])
      }
    }

    "transport connection" in {
      "be set by the request builder with value semantics" in {
        val transport = new Http.TransportConnection(
          new Http.PeerEndpoint(InetAddresses.forString("192.0.2.10"), java.util.Optional.of(53124)),
          java.util.Optional.of(new Http.TransportTls(java.util.List.of()))
        )
        val request = new Http.RequestBuilder().transport(transport).build()

        request.transport must beEqualTo(transport)
        request.transport.tls.orElseThrow().peerCertificates.isEmpty must beTrue
        request.asScala.transport.asJava must beEqualTo(transport)
        request.remote.ipAddress must beEqualTo(
          java.util.Optional.of(InetAddresses.forString("127.0.0.1"))
        )
        request.secure must beFalse
      }
    }
  }
}
