/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import java.security.cert.X509Certificate

import com.google.common.net.InetAddresses
import org.mockito.Mockito
import org.specs2.mutable.Specification
import play.mvc.Http

class ClientCertificateInfoSpec extends Specification {
  private val leaf         = Mockito.mock(classOf[X509Certificate])
  private val intermediate = Mockito.mock(classOf[X509Certificate])

  "Scala effective client certificate metadata" should {
    "keep the leaf separate from its remaining chain" in {
      val info = ClientCertificateInfo(
        leaf,
        Vector(intermediate),
        ClientCertificateSource.DirectTransport
      )

      info.certificate must beEqualTo(leaf)
      info.chain must contain(exactly(intermediate))
      info.certificates must contain(exactly(leaf, intermediate).inOrder)
    }

    "reject null values" in {
      ClientCertificateInfo(null, Vector.empty, ClientCertificateSource.DirectTransport) must
        throwA[IllegalArgumentException]
      ClientCertificateInfo(leaf, null, ClientCertificateSource.DirectTransport) must
        throwA[IllegalArgumentException]
      ClientCertificateInfo(leaf, Vector(null), ClientCertificateSource.DirectTransport) must
        throwA[IllegalArgumentException]
      ClientCertificateInfo(leaf, Vector(leaf), ClientCertificateSource.DirectTransport) must
        throwA[IllegalArgumentException]
      ClientCertificateInfo(leaf, Vector.empty, null) must throwA[IllegalArgumentException]
    }

    "derive a direct leaf and remaining chain from transport metadata" in {
      val peer      = PeerEndpoint(InetAddresses.forString("192.0.2.10"), Some(443))
      val transport = TransportConnection(
        peer,
        Some(TransportTls(Seq(leaf, intermediate)))
      )

      ClientCertificateInfo.fromTransport(transport) must beSome(
        ClientCertificateInfo(leaf, Vector(intermediate), ClientCertificateSource.DirectTransport)
      )
      ClientCertificateInfo.fromTransport(TransportConnection(peer, Some(TransportTls(Seq.empty)))) must beNone
      ClientCertificateInfo.fromTransport(TransportConnection(peer, None)) must beNone
    }

    "remove repeated leaves from direct transport chains" in {
      val peer = PeerEndpoint(InetAddresses.forString("192.0.2.10"), Some(443))

      ClientCertificateInfo.fromTransport(
        TransportConnection(peer, Some(TransportTls(Seq(leaf, leaf))))
      ) must beSome(ClientCertificateInfo(leaf, Vector.empty, ClientCertificateSource.DirectTransport))
      ClientCertificateInfo.fromTransport(
        TransportConnection(peer, Some(TransportTls(Seq(leaf, intermediate, leaf))))
      ) must beSome(ClientCertificateInfo(leaf, Vector(intermediate), ClientCertificateSource.DirectTransport))
    }

    "round-trip through the Java API" in {
      val info = ClientCertificateInfo(leaf, Vector(intermediate), ClientCertificateSource.Rfc9440)

      info.asJava.asScala must_== info
    }
  }

  "Java effective client certificate metadata" should {
    "defensively copy its chain" in {
      val chain = new java.util.ArrayList[X509Certificate]()
      chain.add(intermediate)
      val info = new Http.ClientCertificateInfo(leaf, chain, Http.ClientCertificateSource.X_FORWARDED_CLIENT_CERT)
      chain.clear()

      info.chain must beEqualTo(java.util.List.of(intermediate))
      info.certificates must beEqualTo(java.util.List.of(leaf, intermediate))
      info.chain.add(intermediate) must throwA[UnsupportedOperationException]
      info.certificates.add(intermediate) must throwA[UnsupportedOperationException]
    }

    "reject null values" in {
      new Http.ClientCertificateInfo(null, java.util.List.of(), Http.ClientCertificateSource.DIRECT_TRANSPORT) must
        throwA[NullPointerException]
      new Http.ClientCertificateInfo(leaf, null, Http.ClientCertificateSource.DIRECT_TRANSPORT) must
        throwA[NullPointerException]
      new Http.ClientCertificateInfo(
        leaf,
        java.util.Arrays.asList(null.asInstanceOf[X509Certificate]),
        Http.ClientCertificateSource.DIRECT_TRANSPORT
      ) must throwA[NullPointerException]
      new Http.ClientCertificateInfo(leaf, java.util.List.of(), null) must throwA[NullPointerException]
      new Http.ClientCertificateInfo(
        leaf,
        java.util.List.of(leaf),
        Http.ClientCertificateSource.DIRECT_TRANSPORT
      ) must throwA[IllegalArgumentException]
    }

    "round-trip all sources through the Scala API" in {
      Seq(
        Http.ClientCertificateSource.DIRECT_TRANSPORT,
        Http.ClientCertificateSource.RFC_9440,
        Http.ClientCertificateSource.X_FORWARDED_CLIENT_CERT
      ).foreach { source =>
        val info = new Http.ClientCertificateInfo(leaf, java.util.List.of(intermediate), source)
        info.asScala.asJava must_== info
      }
      ok
    }
  }
}
