/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64

import com.google.common.net.InetAddresses
import org.specs2.mutable.Specification
import play.api.http.HeaderNames
import play.api.mvc.request.ClientCertificateInfo
import play.api.mvc.request.ClientCertificateSource
import play.api.mvc.request.PeerEndpoint
import play.api.mvc.request.TransportConnection
import play.api.mvc.request.TransportTls
import play.api.mvc.Headers
import play.api.Configuration
import play.core.server.common.ClientCertificateHeaderHandler.Config
import play.core.server.common.ClientCertificateHeaderHandler.Off
import play.core.server.common.ClientCertificateHeaderHandler.Rfc9440
import play.core.server.common.ClientCertificateHeaderHandler.XForwardedClientCertMode

class ClientCertificateHeaderHandlerSpec extends Specification {
  private val limits = ClientCertificateHeaderLimits(
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

  "ClientCertificateHeaderHandler" should {
    "ignore forwarded certificate fields when handling is off" in {
      val handler = new ClientCertificateHeaderHandler(Config(Off, Nil, limits))

      handler.clientCertificate(transport("127.0.0.1", Seq(leaf)), headers(HeaderNames.CLIENT_CERT -> ":bad:")) must
        beSome[ClientCertificateInfo].like {
          case info =>
            info.certificate must_== leaf
            info.source must_== ClientCertificateSource.DirectTransport
        }
      handler
        .clientCertificates(
          transport("127.0.0.1", Seq(leaf)),
          headers(HeaderNames.X_FORWARDED_CLIENT_CERT -> "bad")
        )
        .xForwardedClientCertificates must beEmpty
    }

    "ignore forwarded certificate fields from an untrusted direct peer" in {
      val handler = rfcHandler("192.0.2.0/24")

      handler.clientCertificate(transport("127.0.0.1", Seq(leaf)), headers(HeaderNames.CLIENT_CERT -> ":bad:")) must
        beSome[ClientCertificateInfo].like {
          case info => info.source must_== ClientCertificateSource.DirectTransport
        }
    }

    "not mistake a trusted proxy certificate for the original client when fields are absent" in {
      val handler = rfcHandler("127.0.0.1")

      handler.clientCertificate(transport("127.0.0.1", Seq(leaf)), Headers()) must beNone
    }

    "select a trusted RFC 9440 client certificate" in {
      val handler = rfcHandler("127.0.0.1")

      handler.clientCertificate(
        transport("127.0.0.1"),
        headers(HeaderNames.CLIENT_CERT -> byteSequence(leafBase64))
      ) must beSome[ClientCertificateInfo].like {
        case info =>
          (info.certificate.getEncoded must beEqualTo(leaf.getEncoded))
            .and(info.source must_== ClientCertificateSource.Rfc9440)
      }
    }

    "preserve valid forwarded certificate metadata when constructing an error request" in {
      val selected = rfcHandler("127.0.0.1").clientCertificatesForErrorRequest(
        transport("127.0.0.1", Seq(intermediate)),
        headers(HeaderNames.CLIENT_CERT -> byteSequence(leafBase64)),
        new IllegalArgumentException("invalid request target")
      )

      selected.clientCertificate must beSome[ClientCertificateInfo].like {
        case info =>
          (info.certificate.getEncoded must beEqualTo(leaf.getEncoded))
            .and(info.source must_== ClientCertificateSource.Rfc9440)
      }
    }

    "omit effective certificate metadata after a forwarding error instead of selecting the proxy certificate" in {
      val handler         = xfccHandler("127.0.0.1")
      val directTransport = transport("127.0.0.1", Seq(leaf))
      val requestHeaders  = headers(HeaderNames.X_FORWARDED_CLIENT_CERT -> "not-valid-xfcc")
      val requestFailure  = scala.util.Try(handler.clientCertificates(directTransport, requestHeaders)).failed.get
      val selected        =
        handler.clientCertificatesForErrorRequest(directTransport, requestHeaders, requestFailure)

      selected.clientCertificate must beNone
      selected.xForwardedClientCertificates must beEmpty
    }

    "not repeat certificate selection after it failed during normal request conversion" in {
      val handler         = xfccHandler("127.0.0.1")
      val directTransport = transport("127.0.0.1", Seq(intermediate))
      val requestFailure  = scala.util
        .Try(
          handler.clientCertificates(
            directTransport,
            headers(HeaderNames.X_FORWARDED_CLIENT_CERT -> "not-valid-xfcc")
          )
        )
        .failed
        .get

      val selected = handler.clientCertificatesForErrorRequest(
        directTransport,
        headers(HeaderNames.X_FORWARDED_CLIENT_CERT -> s"Cert=${percentEncode(pem(leafBase64))}"),
        requestFailure
      )

      selected must_== ClientCertificateHeaderHandler.Selection.empty
    }

    "retain a trusted metadata-only XFCC assertion without inventing an effective certificate" in {
      val handler  = xfccHandler("127.0.0.1")
      val selected = handler.clientCertificates(
        transport("127.0.0.1", Seq(leaf)),
        headers(
          HeaderNames.X_FORWARDED_CLIENT_CERT ->
            s"Hash=${"a" * 64};Subject=\"\";URI=spiffe://example.test/workload;DNS=client.example.test"
        )
      )

      selected.clientCertificate must beNone
      selected.xForwardedClientCertificates must beLike {
        case Vector(assertion) =>
          (assertion.hash must beSome("a" * 64))
            .and(assertion.subject must beSome(""))
            .and(assertion.uris must_== Vector("spiffe://example.test/workload"))
            .and(assertion.dnsNames must_== Vector("client.example.test"))
      }
    }

    "select a trusted XFCC certificate as the effective client certificate" in {
      val handler  = xfccHandler("127.0.0.1")
      val selected = handler.clientCertificates(
        transport("127.0.0.1"),
        headers(HeaderNames.X_FORWARDED_CLIENT_CERT -> s"Cert=${percentEncode(pem(leafBase64))}")
      )

      selected.clientCertificate must beSome[ClientCertificateInfo].like {
        case info =>
          (info.certificate.getEncoded.toVector must_== leaf.getEncoded.toVector)
            .and(info.chain must beEmpty)
            .and(info.source must_== ClientCertificateSource.XForwardedClientCert)
      }
      selected.xForwardedClientCertificates must haveSize(1)
    }

    "ignore malformed XFCC from an untrusted direct peer" in {
      val handler  = xfccHandler("192.0.2.0/24")
      val selected = handler.clientCertificates(
        transport("127.0.0.1", Seq(leaf)),
        headers(HeaderNames.X_FORWARDED_CLIENT_CERT -> "not-valid-xfcc")
      )

      selected.clientCertificate must beSome[ClientCertificateInfo].like {
        case info => info.source must_== ClientCertificateSource.DirectTransport
      }
      selected.xForwardedClientCertificates must beEmpty
    }

    "not mistake a trusted proxy certificate for an absent XFCC assertion" in {
      val selected = xfccHandler("127.0.0.1").clientCertificates(
        transport("127.0.0.1", Seq(leaf)),
        Headers()
      )

      selected.clientCertificate must beNone
      selected.xForwardedClientCertificates must beEmpty
    }

    "validate configuration eagerly" in {
      Config(Some(config("play.http.forwarded.clientCertificates.mode" -> "rfc9440"))).mode must_== Rfc9440
      Config(Some(config("play.http.forwarded.clientCertificates.mode" -> "x-forwarded-client-cert"))).mode must_==
        XForwardedClientCertMode
      Config(Some(config("play.http.forwarded.clientCertificates.mode" -> "unknown"))) must
        throwA[Exception]
      Config(Some(config("play.http.forwarded.clientCertificates.trustedProxies" -> Seq("127.0.0.1/999")))) must
        throwA[Exception]
      Config(Some(config("play.http.forwarded.clientCertificates.limits.maxChainLength" -> -1))) must
        throwA[Exception]
      Config(Some(config("play.http.forwarded.clientCertificates.xForwardedClientCert.policy" -> "append"))) must
        throwA[Exception]
      Config(Some(config("play.http.forwarded.clientCertificates.xForwardedClientCert.format" -> "json"))) must
        throwA[Exception]
    }
  }

  private def rfcHandler(trustedProxy: String): ClientCertificateHeaderHandler =
    new ClientCertificateHeaderHandler(Config(Rfc9440, List(Subnet(trustedProxy)), limits))

  private def xfccHandler(trustedProxy: String): ClientCertificateHeaderHandler =
    new ClientCertificateHeaderHandler(Config(XForwardedClientCertMode, List(Subnet(trustedProxy)), limits))

  private def transport(address: String, certificates: Seq[X509Certificate] = Seq.empty): TransportConnection =
    TransportConnection(
      PeerEndpoint(InetAddresses.forString(address), Some(443)),
      Option.when(certificates.nonEmpty)(TransportTls(certificates))
    )

  private def headers(values: (String, String)*): Headers = new Headers(values)

  private def byteSequence(base64: String): String = s":$base64:"

  private def pem(base64: String): String =
    s"-----BEGIN CERTIFICATE-----\n$base64\n-----END CERTIFICATE-----\n"

  private def percentEncode(value: String): String = {
    val result = new StringBuilder(value.length * 3)
    val hex    = "0123456789ABCDEF"
    value.getBytes(java.nio.charset.StandardCharsets.US_ASCII).foreach { byte =>
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

  private def config(values: (String, Any)*): Configuration =
    Configuration.from(values.toMap).withFallback(Configuration.reference)
}
