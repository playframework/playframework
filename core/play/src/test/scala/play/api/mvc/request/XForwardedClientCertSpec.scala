/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import java.security.cert.X509Certificate

import org.mockito.Mockito
import org.specs2.mutable.Specification
import play.mvc.Http

class XForwardedClientCertSpec extends Specification {
  private val leaf         = Mockito.mock(classOf[X509Certificate])
  private val intermediate = Mockito.mock(classOf[X509Certificate])

  private val assertion = XForwardedClientCert(
    by = Vector("spiffe://mesh.example/edge", "spiffe://mesh.example/sidecar"),
    hash = Some("0123456789abcdef"),
    certificate = Some(leaf),
    chain = Vector(intermediate),
    subject = Some("CN=client"),
    uris = Vector("spiffe://example/client", "https://client.example/identity"),
    dnsNames = Vector("client.example", "client.internal")
  )

  "Scala XFCC assertions" should {
    "preserve repeated-value order and keep the leaf out of the remaining chain" in {
      assertion.by must contain(exactly("spiffe://mesh.example/edge", "spiffe://mesh.example/sidecar").inOrder)
      assertion.uris must contain(exactly("spiffe://example/client", "https://client.example/identity").inOrder)
      assertion.dnsNames must contain(exactly("client.example", "client.internal").inOrder)
      assertion.certificate must beSome(leaf)
      assertion.chain must contain(exactly(intermediate))
      assertion.certificates must contain(exactly(leaf, intermediate).inOrder)
    }

    "represent assertions without an encoded certificate" in {
      val metadataOnly = assertion.copy(certificate = None, chain = Vector.empty)

      metadataOnly.certificates must beEmpty
      metadataOnly.hash must beSome("0123456789abcdef")
      metadataOnly.uris must not(beEmpty)
    }

    "reject null values and certificate chains without a leaf" in {
      assertion.copy(by = null) must throwA[IllegalArgumentException]
      assertion.copy(by = Vector(null)) must throwA[IllegalArgumentException]
      assertion.copy(hash = null) must throwA[IllegalArgumentException]
      assertion.copy(certificate = null) must throwA[IllegalArgumentException]
      assertion.copy(chain = null) must throwA[IllegalArgumentException]
      assertion.copy(chain = Vector(null)) must throwA[IllegalArgumentException]
      assertion.copy(subject = null) must throwA[IllegalArgumentException]
      assertion.copy(uris = Vector(null)) must throwA[IllegalArgumentException]
      assertion.copy(dnsNames = Vector(null)) must throwA[IllegalArgumentException]
      assertion.copy(certificate = None) must throwA[IllegalArgumentException]
      assertion.copy(chain = Vector(leaf)) must throwA[IllegalArgumentException]
    }

    "round-trip through the Java API" in {
      assertion.asJava.asScala must_== assertion
    }
  }

  "Java XFCC assertions" should {
    "defensively copy all repeated values" in {
      val by            = new java.util.ArrayList[String](java.util.List.of("spiffe://mesh.example/edge"))
      val chain         = new java.util.ArrayList[X509Certificate](java.util.List.of(intermediate))
      val uris          = new java.util.ArrayList[String](java.util.List.of("spiffe://example/client"))
      val dnsNames      = new java.util.ArrayList[String](java.util.List.of("client.example"))
      val javaAssertion = new Http.XForwardedClientCert(
        by,
        java.util.Optional.of("0123456789abcdef"),
        java.util.Optional.of(leaf),
        chain,
        java.util.Optional.of("CN=client"),
        uris,
        dnsNames
      )
      by.clear()
      chain.clear()
      uris.clear()
      dnsNames.clear()

      javaAssertion.by must beEqualTo(java.util.List.of("spiffe://mesh.example/edge"))
      javaAssertion.chain must beEqualTo(java.util.List.of(intermediate))
      javaAssertion.uris must beEqualTo(java.util.List.of("spiffe://example/client"))
      javaAssertion.dnsNames must beEqualTo(java.util.List.of("client.example"))
      javaAssertion.certificates must beEqualTo(java.util.List.of(leaf, intermediate))
      javaAssertion.by.add("another") must throwA[UnsupportedOperationException]
    }

    "reject certificate chains without a leaf" in {
      new Http.XForwardedClientCert(
        java.util.List.of(),
        java.util.Optional.empty(),
        java.util.Optional.empty(),
        java.util.List.of(intermediate),
        java.util.Optional.empty(),
        java.util.List.of(),
        java.util.List.of()
      ) must throwA[IllegalArgumentException]
    }

    "round-trip through the Scala API" in {
      assertion.asJava.asScala.asJava must_== assertion.asJava
    }
  }
}
