/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import java.security.cert.X509Certificate

import com.google.common.net.InetAddresses
import org.mockito.Mockito
import org.specs2.mutable.Specification
import play.mvc.Http

class TransportConnectionSpec extends Specification {
  private val address     = InetAddresses.forString("192.0.2.10")
  private val certificate = Mockito.mock(classOf[X509Certificate])

  "Scala transport metadata" should {
    "accept absent ports and both network-port boundaries" in {
      PeerEndpoint(address, None).port must beNone
      PeerEndpoint(address, Some(0)).port must beSome(0)
      PeerEndpoint(address, Some(65535)).port must beSome(65535)
    }

    "reject invalid peer endpoints" in {
      PeerEndpoint(null, None) must throwA[IllegalArgumentException]
      PeerEndpoint(address, null) must throwA[IllegalArgumentException]
      PeerEndpoint(address, Some(-1)) must throwA[IllegalArgumentException]
      PeerEndpoint(address, Some(65536)) must throwA[IllegalArgumentException]
    }

    "accept immutable certificate sequences and reject null values" in {
      TransportTls(Seq.empty).peerCertificates must beEmpty
      TransportTls(Seq(certificate)).peerCertificates must contain(exactly(certificate))
      TransportTls(null) must throwA[IllegalArgumentException]
      TransportTls(Seq(null)) must throwA[IllegalArgumentException]
    }

    "reject null connection values" in {
      val peer = PeerEndpoint(address, None)

      TransportConnection(null, None) must throwA[IllegalArgumentException]
      TransportConnection(peer, null) must throwA[IllegalArgumentException]
      TransportConnection(peer, Some(null)) must throwA[IllegalArgumentException]
    }

    "round-trip through the Java API" in {
      val transport = TransportConnection(
        PeerEndpoint(address, Some(65535)),
        Some(TransportTls(Seq(certificate)))
      )

      transport.asJava.asScala must_== transport
    }
  }

  "Java transport metadata" should {
    "accept absent ports and both network-port boundaries" in {
      new Http.PeerEndpoint(address, java.util.Optional.empty()).port must beEqualTo(java.util.Optional.empty())
      new Http.PeerEndpoint(address, java.util.Optional.of(0)).port must beEqualTo(java.util.Optional.of(0))
      new Http.PeerEndpoint(address, java.util.Optional.of(65535)).port must beEqualTo(java.util.Optional.of(65535))
    }

    "reject invalid peer endpoints" in {
      new Http.PeerEndpoint(null, java.util.Optional.empty()) must throwA[NullPointerException]
      new Http.PeerEndpoint(address, null) must throwA[NullPointerException]
      new Http.PeerEndpoint(address, java.util.Optional.of(-1)) must throwA[IllegalArgumentException]
      new Http.PeerEndpoint(address, java.util.Optional.of(65536)) must throwA[IllegalArgumentException]
    }

    "defensively copy certificate lists and reject null values" in {
      val certificates = new java.util.ArrayList[X509Certificate]()
      certificates.add(certificate)
      val tls = new Http.TransportTls(certificates)
      certificates.clear()

      tls.peerCertificates must beEqualTo(java.util.List.of(certificate))
      tls.peerCertificates.add(certificate) must throwA[UnsupportedOperationException]
      new Http.TransportTls(null) must throwA[NullPointerException]
      new Http.TransportTls(java.util.Arrays.asList(null.asInstanceOf[X509Certificate])) must
        throwA[NullPointerException]
    }

    "reject null connection values" in {
      val peer = new Http.PeerEndpoint(address, java.util.Optional.empty())

      new Http.TransportConnection(null, java.util.Optional.empty()) must throwA[NullPointerException]
      new Http.TransportConnection(peer, null) must throwA[NullPointerException]
    }

    "round-trip through the Scala API" in {
      val transport = new Http.TransportConnection(
        new Http.PeerEndpoint(address, java.util.Optional.of(0)),
        java.util.Optional.of(new Http.TransportTls(java.util.List.of(certificate)))
      )

      transport.asScala.asJava must_== transport
    }
  }
}
