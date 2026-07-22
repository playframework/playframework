/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.MessageDigest
import java.util.Base64

import org.specs2.mutable.Specification
import play.api.mvc.request.XForwardedClientCert
import play.core.server.common.XForwardedClientCertHeaderParser.Limits

class XForwardedClientCertHeaderParserSpec extends Specification {
  private val limits = Limits(
    maxHeaderBytes = 64 * 1024,
    maxDecodedBytes = 64 * 1024,
    maxCertificateBytes = 16 * 1024,
    maxChainLength = 8
  )

  private val leafBase64 =
    "MIIBqDCCAU6gAwIBAgIBBzAKBggqhkjOPQQDAjA6MRswGQYDVQQKDBJMZXQncyBB" +
      "dXRoZW50aWNhdGUxGzAZBgNVBAMMEkxBIEludGVybWVkaWF0ZSBDQTAeFw0yMDAx" +
      "MTQyMjU1MzNaFw0yMTAxMjMyMjU1MzNaMA0xCzAJBgNVBAMMAkJDMFkwEwYHKoZI" +
      "zj0CAQYIKoZIzj0DAQcDQgAE8YnXXfaUgmnMtOXU/IncWalRhebrXmckC8vdgJ1p" +
      "5Be5F/3YC8OthxM4+k1M6aEAEFcGzkJiNy6J84y7uzo9M6NyMHAwCQYDVR0TBAIw" +
      "ADAfBgNVHSMEGDAWgBRm3WjLa38lbEYCuiCPct0ZaSED2DAOBgNVHQ8BAf8EBAMC" +
      "BsAwEwYDVR0lBAwwCgYIKwYBBQUHAwIwHQYDVR0RAQH/BBMwEYEPYmRjQGV4YW1w" +
      "bGUuY29tMAoGCCqGSM49BAMCA0gAMEUCIBHda/r1vaL6G3VliL4/Di6YK0Q6bMje" +
      "SkC3dFCOOB8TAiEAx/kHSB4urmiZ0NX5r5XarmPk0wmuydBVoU4hBVZ1yhk="

  private val intermediateBase64 =
    "MIIB5jCCAYugAwIBAgIBFjAKBggqhkjOPQQDAjBWMQswCQYDVQQGEwJVUzEbMBkG" +
      "A1UECgwSTGV0J3MgQXV0aGVudGljYXRlMSowKAYDVQQDDCFMZXQncyBBdXRoZW50" +
      "aWNhdGUgUm9vdCBBdXRob3JpdHkwHhcNMjAwMTE0MjEzMjMwWhcNMzAwMTExMjEz" +
      "MjMwWjA6MRswGQYDVQQKDBJMZXQncyBBdXRoZW50aWNhdGUxGzAZBgNVBAMMEkxB" +
      "IEludGVybWVkaWF0ZSBDQTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABJf+aA54" +
      "RC5pyLAR5yfXVYmNpgd+CGUTDp2KOGhc0gK91zxhHesEYkdXkpS2UN8Kati+yHtW" +
      "CV3kkhCngGyv7RqjZjBkMB0GA1UdDgQWBBRm3WjLa38lbEYCuiCPct0ZaSED2DAf" +
      "BgNVHSMEGDAWgBTEA2Q6eecKu9g9yb5glbkhhVINGDASBgNVHRMBAf8ECDAGAQH/" +
      "AgEAMA4GA1UdDwEB/wQEAwIBhjAKBggqhkjOPQQDAgNJADBGAiEA5pLvaFwRRkxo" +
      "mIAtDIwg9D7gC1xzxBl4r28EzmSO1pcCIQCJUShpSXO9HDIQMUgH69fNDEMHXD3R" +
      "RX5gP7kuu2KGMg=="

  private val leaf         = certificate(leafBase64)
  private val intermediate = certificate(intermediateBase64)
  private val leafHash     = sha256(leaf)

  "XForwardedClientCertHeaderParser" should {
    "return no assertions when the field is absent" in {
      parse() must beRight(Vector.empty[XForwardedClientCert])
    }

    "parse metadata-only assertions and retain repeated fields in order" in {
      parse(
        s"bY=proxy-a;By=proxy-b;HASH=${leafHash.toUpperCase};" +
          "Subject=\"CN=client, O=Example; unit=one\\\"two\\three\";" +
          "URI=spiffe://example.test/workload;uri=https://example.test/client;" +
          "DNS=first.example.test;dns=second.example.test;Future=accepted"
      ) must beRight.like {
        case Vector(assertion) =>
          (assertion.by must_== Vector("proxy-a", "proxy-b"))
            .and(assertion.hash must beSome(leafHash.toUpperCase))
            .and(assertion.certificate must beNone)
            .and(assertion.chain must beEmpty)
            .and(assertion.subject must beSome("CN=client, O=Example; unit=one\\\"two\\three"))
            .and(assertion.uris must_== Vector("spiffe://example.test/workload", "https://example.test/client"))
            .and(assertion.dnsNames must_== Vector("first.example.test", "second.example.test"))
      }
    }

    "allow an explicitly empty Subject" in {
      parse("Subject=\"\"") must beRight.like {
        case Vector(assertion) => assertion.subject must beSome("")
      }
    }

    "preserve RFC 2253 Subject escapes while recognizing escaped quotes" in {
      Seq(
        "Subject=\"CN=Doe\\, John\""         -> "CN=Doe\\, John",
        "Subject=\"CN=one\\+two\""           -> "CN=one\\+two",
        "Subject=\"CN=one\\;two\""           -> "CN=one\\;two",
        "Subject=\"CN=leading\\ space\""     -> "CN=leading\\ space",
        "Subject=\"CN=line\\0Dbreak\""       -> "CN=line\\0Dbreak",
        "Subject=\"CN=one\\\\two\""          -> "CN=one\\\\two",
        ("Subject=\"CN=one" + "\\\\" + "\"") -> ("CN=one" + "\\\\"),
        ("Subject=\"CN=one" + "\\\"" + "\"") -> ("CN=one" + "\\\"")
      ).forall {
        case (header, expected) =>
          parse(header) match {
            case Right(Vector(assertion)) => assertion.subject.contains(expected)
            case _                        => false
          }
      } must beTrue
    }

    "accept Envoy's empty URI marker without inventing an identity" in {
      Seq("URI=", "URI=\"\"").forall { header =>
        parse(header) match {
          case Right(Vector(assertion)) => assertion.uris.isEmpty
          case _                        => false
        }
      } must beTrue
    }

    "retain equals signs in URI values" in {
      parse("URI=\"https://example.test/client?key=value\"") must beRight.like {
        case Vector(assertion) => assertion.uris must_== Vector("https://example.test/client?key=value")
      }
      parse("URI=https://example.test/client?key=value") must beRight.like {
        case Vector(assertion) => assertion.uris must_== Vector("https://example.test/client?key=value")
      }
    }

    "parse Cert, preserve literal plus signs, and verify its SHA-256 Hash" in {
      // Only line endings are escaped. A form decoder would turn the '+' in this PEM into spaces and corrupt it.
      val encodedPem = pem(leafBase64).replace("\n", "%0A")

      parse(s"Hash=${leafHash.toUpperCase};Cert=\"$encodedPem\"") must beRight.like {
        case Vector(assertion) =>
          (assertion.certificate.map(_.getEncoded.toVector) must beSome(leaf.getEncoded.toVector))
            .and(assertion.chain must beEmpty)
      }
    }

    "derive the leaf from Chain and retain only the remaining chain" in {
      val encodedChain = percentEncode(pem(leafBase64) + pem(intermediateBase64))

      parse(s"Chain=$encodedChain") must beRight.like {
        case Vector(assertion) =>
          (assertion.certificate.map(_.getEncoded.toVector) must beSome(leaf.getEncoded.toVector))
            .and(assertion.chain.map(_.getEncoded.toVector) must_== Vector(intermediate.getEncoded.toVector))
      }
    }

    "accept Envoy's quoted Chain value" in {
      val encodedChain = percentEncode(pem(leafBase64) + pem(intermediateBase64))

      parse(s"""Chain="$encodedChain"""") must beRight.like {
        case Vector(assertion) =>
          (assertion.certificate.map(_.getEncoded.toVector) must beSome(leaf.getEncoded.toVector))
            .and(assertion.chain.map(_.getEncoded.toVector) must_== Vector(intermediate.getEncoded.toVector))
      }
    }

    "accept matching Cert and Chain values" in {
      val encodedCert  = percentEncode(pem(leafBase64))
      val encodedChain = percentEncode(pem(leafBase64) + pem(intermediateBase64))

      parse(s"Cert=$encodedCert;Chain=$encodedChain") must beRight.like {
        case Vector(assertion) =>
          assertion.certificates.map(_.getEncoded.toVector) must_==
            Vector(leaf.getEncoded.toVector, intermediate.getEncoded.toVector)
      }
    }

    "reject conflicting Cert and Chain leaves" in {
      parse(s"Cert=${percentEncode(pem(leafBase64))};Chain=${percentEncode(pem(intermediateBase64))}") must beLeft
    }

    "reject a Chain that repeats its leaf" in {
      val repeatedLeafChain = percentEncode(pem(leafBase64) + pem(intermediateBase64) + pem(leafBase64))

      parse(s"Chain=$repeatedLeafChain") must beLeft
    }

    "reject a Hash that does not match an asserted certificate while permitting Hash-only metadata" in {
      val wrongHash = "0" * 64

      parse(s"Hash=$wrongHash") must beRight
      parse(s"Hash=$wrongHash;Cert=${percentEncode(pem(leafBase64))}") must beLeft
    }

    "reject repeated physical fields and multiple information elements" in {
      parse("URI=spiffe://one", "URI=spiffe://two") must beLeft
      parse("URI=spiffe://one,URI=spiffe://two") must beLeft
      parse("Subject=\"a,b\"") must beRight
    }

    "reject duplicate singleton fields case-insensitively" in {
      Seq(
        s"Hash=$leafHash;hAsH=$leafHash",
        "Subject=\"one\";subject=\"two\"",
        s"Cert=${percentEncode(pem(leafBase64))};cert=${percentEncode(pem(leafBase64))}",
        s"Chain=${percentEncode(pem(leafBase64))};chain=${percentEncode(pem(leafBase64))}"
      ).forall(value => parse(value).isLeft) must beTrue
    }

    "reject malformed fields and assertions without recognized metadata" in {
      Seq(
        "Future=accepted",
        "By=",
        "DNS=",
        "URI=spiffe://client;",
        "Subject=unquoted",
        "URI=\"unterminated",
        "URI=spiffe://client;;DNS=client.example",
        "URI spiffe://client",
        "=value",
        ""
      ).forall(value => parse(value).isLeft) must beTrue
    }

    "reject malformed Hash, percent encoding, PEM, and certificate data" in {
      val nonCertificate = percentEncode(pem(Base64.getEncoder.encodeToString(Array[Byte](1, 2, 3))))
      Seq(
        "Hash=abc",
        s"Hash=${"g" * 64}",
        "Cert=%ZZ",
        "Cert=not-a-pem",
        s"Cert=$nonCertificate",
        s"Cert=${percentEncode(pem(leafBase64) + "garbage")}",
        s"Cert=${percentEncode(pem(leafBase64) + pem(intermediateBase64))}"
      ).forall(value => parse(value).isLeft) must beTrue
    }

    "enforce raw, decoded, per-certificate, and chain limits" in {
      val certHeader  = s"Cert=${percentEncode(pem(leafBase64))}"
      val chainHeader = s"Chain=${percentEncode(pem(leafBase64) + pem(intermediateBase64))}"

      parseWith(limits.copy(maxHeaderBytes = 1), certHeader) must beLeft
      parseWith(limits.copy(maxDecodedBytes = 1), certHeader) must beLeft
      parseWith(limits.copy(maxCertificateBytes = 1), certHeader) must beLeft
      parseWith(limits.copy(maxChainLength = 0), chainHeader) must beLeft
      parseWith(limits.copy(maxChainLength = 0), s"Chain=${percentEncode(pem(leafBase64))}") must beRight
    }

    "accept exact XFCC limits and reject one unit less" in {
      val certHeader       = s"Cert=${percentEncode(pem(leafBase64))}"
      val rawBytes         = certHeader.getBytes(StandardCharsets.UTF_8).length.toLong
      val certificateBytes = leaf.getEncoded.length.toLong

      parseWith(limits.copy(maxHeaderBytes = rawBytes), certHeader) must beRight
      parseWith(limits.copy(maxHeaderBytes = rawBytes - 1), certHeader) must beLeft

      parseWith(limits.copy(maxDecodedBytes = certificateBytes), certHeader) must beRight
      parseWith(limits.copy(maxDecodedBytes = certificateBytes - 1), certHeader) must beLeft

      parseWith(limits.copy(maxCertificateBytes = certificateBytes), certHeader) must beRight
      parseWith(limits.copy(maxCertificateBytes = certificateBytes - 1), certHeader) must beLeft

      val chainHeader = s"Chain=${percentEncode(pem(leafBase64) + pem(intermediateBase64))}"
      val chainBytes  = leaf.getEncoded.length.toLong + intermediate.getEncoded.length.toLong
      parseWith(limits.copy(maxDecodedBytes = chainBytes, maxChainLength = 1), chainHeader) must beRight
      parseWith(limits.copy(maxDecodedBytes = chainBytes - 1, maxChainLength = 1), chainHeader) must beLeft
      parseWith(limits.copy(maxDecodedBytes = chainBytes, maxChainLength = 0), chainHeader) must beLeft
    }

    "scan an unusually large single assertion iteratively" in {
      val count       = 4096
      val value       = Vector.tabulate(count)(index => s"URI=spiffe://example.test/$index").mkString(";")
      val largeLimits = limits.copy(maxHeaderBytes = value.length.toLong + 1)

      parseWith(largeLimits, value) must beRight.like {
        case Vector(assertion) =>
          (assertion.uris.length must_== count).and(assertion.uris.last must_== s"spiffe://example.test/${count - 1}")
      }
    }
  }

  private def parse(values: String*) = XForwardedClientCertHeaderParser.parse(values, limits)

  private def parseWith(limits: Limits, values: String*) =
    XForwardedClientCertHeaderParser.parse(values, limits)

  private def pem(base64: String): String =
    s"-----BEGIN CERTIFICATE-----\n$base64\n-----END CERTIFICATE-----\n"

  private def percentEncode(value: String): String = {
    val result = new StringBuilder(value.length * 3)
    val bytes  = value.getBytes(java.nio.charset.StandardCharsets.US_ASCII)
    val hex    = "0123456789ABCDEF"
    bytes.foreach { byte =>
      val unsigned = byte & 0xff
      if (
        (unsigned >= 'a' && unsigned <= 'z') ||
        (unsigned >= 'A' && unsigned <= 'Z') ||
        (unsigned >= '0' && unsigned <= '9') ||
        unsigned == '-' || unsigned == '.' || unsigned == '_' || unsigned == '~'
      ) result.append(unsigned.toChar)
      else {
        result.append('%')
        result.append(hex.charAt(unsigned >>> 4))
        result.append(hex.charAt(unsigned & 0x0f))
      }
    }
    result.result()
  }

  private def certificate(base64: String): X509Certificate =
    CertificateFactory
      .getInstance("X.509")
      .generateCertificate(new ByteArrayInputStream(Base64.getDecoder.decode(base64)))
      .asInstanceOf[X509Certificate]

  private def sha256(certificate: X509Certificate): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(certificate.getEncoded)
      .map(byte => f"${byte & 0xff}%02x")
      .mkString
}
