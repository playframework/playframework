/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64

import org.specs2.mutable.Specification
import play.api.http.HeaderNames
import play.api.mvc.request.ClientCertificateInfo
import play.api.mvc.request.ClientCertificateSource
import play.api.mvc.Headers

class Rfc9440ClientCertificateParserSpec extends Specification {
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

  private val rootBase64 =
    "MIICBjCCAaygAwIBAgIJAKS0yiqKtlhoMAoGCCqGSM49BAMCMFYxCzAJBgNVBAYT" +
      "AlVTMRswGQYDVQQKDBJMZXQncyBBdXRoZW50aWNhdGUxKjAoBgNVBAMMIUxldCdz" +
      "IEF1dGhlbnRpY2F0ZSBSb290IEF1dGhvcml0eTAeFw0yMDAxMTQyMTI1NDVaFw00" +
      "MDAxMDkyMTI1NDVaMFYxCzAJBgNVBAYTAlVTMRswGQYDVQQKDBJMZXQncyBBdXRo" +
      "ZW50aWNhdGUxKjAoBgNVBAMMIUxldCdzIEF1dGhlbnRpY2F0ZSBSb290IEF1dGhv" +
      "cml0eTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABFoaHU+Z5bPKmGzlYXtCf+E6" +
      "HYj62fORaHDOrt+yyh3H/rTcs7ynFfGn+gyFsrSP3Ez88rajv+U2NfD0o0uZ4Pmj" +
      "YzBhMB0GA1UdDgQWBBTEA2Q6eecKu9g9yb5glbkhhVINGDAfBgNVHSMEGDAWgBTE" +
      "A2Q6eecKu9g9yb5glbkhhVINGDAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQE" +
      "AwIBhjAKBggqhkjOPQQDAgNIADBFAiEAmAeg1ycKHriqHnaD4M/UDBpQRpkmdcRF" +
      "YGMg1Qyrkx4CIB4ivz3wQcQkGhcsUZ1SOImd/lq1Q0FLf09rGfLQPWDc"

  private val leaf         = certificate(leafBase64)
  private val intermediate = certificate(intermediateBase64)
  private val root         = certificate(rootBase64)

  "Rfc9440ClientCertificateParser" should {
    "parse the RFC 9440 Appendix A leaf certificate" in {
      parse(headers(HeaderNames.CLIENT_CERT -> byteSequence(leafBase64))) must beRight.like {
        case Some(info) =>
          (info.certificate.getEncoded must beEqualTo(leaf.getEncoded))
            .and(info.chain must beEmpty)
            .and(info.source must_== ClientCertificateSource.Rfc9440)
      }
    }

    "accept an unpadded RFC 8941 Byte Sequence" in {
      val unpadded = leafBase64.reverse.dropWhile(_ == '=').reverse

      parse(headers(HeaderNames.CLIENT_CERT -> byteSequence(unpadded))) must beRight(beSome[ClientCertificateInfo])
    }

    "accept and ignore valid Structured Fields parameters" in {
      val leafItem =
        s" ${byteSequence(leafBase64)};flag;disabled=?0;count=-42;ratio=1.25;" +
          "label=\"client\";kind=spiffe/client;data=:AQI:;flag=?0 "

      parse(headers(HeaderNames.CLIENT_CERT -> leafItem)) must beRight.like {
        case Some(info) =>
          (info.certificate.getEncoded must beEqualTo(leaf.getEncoded))
            .and(info.chain must beEmpty)
      }
    }

    "preserve certificate chain order across repeated fields" in {
      val requestHeaders = new Headers(
        Seq(
          HeaderNames.CLIENT_CERT       -> byteSequence(leafBase64),
          HeaderNames.CLIENT_CERT_CHAIN -> s"${byteSequence(intermediateBase64)};role=intermediate",
          HeaderNames.CLIENT_CERT_CHAIN -> s"\t${byteSequence(rootBase64)};role=root"
        )
      )

      parse(requestHeaders) must beRight.like {
        case Some(info) =>
          info.chain.map(_.getEncoded) must contain(exactly(intermediate.getEncoded, root.getEncoded).inOrder)
      }
    }

    "accept an explicit empty Structured Fields chain list" in {
      Seq("", "   ").forall { chainValue =>
        parse(
          headers(
            HeaderNames.CLIENT_CERT       -> byteSequence(leafBase64),
            HeaderNames.CLIENT_CERT_CHAIN -> chainValue
          )
        ).exists(_.exists(_.chain.isEmpty))
      } must beTrue
    }

    "ignore a chain with an empty member while retaining a valid leaf" in {
      val chainItem     = byteSequence(intermediateBase64)
      val invalidChains = Seq(
        Seq("", chainItem),
        Seq(chainItem, ""),
        Seq("", "")
      )

      invalidChains.forall { chainValues =>
        val requestHeaders = new Headers(
          Seq(HeaderNames.CLIENT_CERT -> byteSequence(leafBase64)) ++
            chainValues.map(HeaderNames.CLIENT_CERT_CHAIN -> _)
        )
        parse(requestHeaders).exists(_.exists(info => info.certificate == leaf && info.chain.isEmpty))
      } must beTrue
    }

    "ignore duplicate singleton leaf fields" in {
      val requestHeaders = new Headers(Seq.empty) {
        override def getAll(name: String): Seq[String] =
          if (name.equalsIgnoreCase(HeaderNames.CLIENT_CERT)) {
            Vector(byteSequence(leafBase64), byteSequence(leafBase64))
          } else Vector.empty
      }

      parse(requestHeaders) must beRight(beNone)
    }

    "ignore a leaf with malformed Structured Fields parameters or singleton tab whitespace" in {
      val leafItem     = byteSequence(leafBase64)
      val invalidItems = Seq(
        s"\t$leafItem",
        s"$leafItem\t",
        s"$leafItem;Foo",
        s"$leafItem;",
        s"$leafItem;\tfoo",
        s"$leafItem;foo =1",
        s"$leafItem;foo= 1",
        s"$leafItem;foo=-",
        s"$leafItem;foo=1.",
        s"$leafItem;foo=\"bad\\escape\"",
        s"$leafItem;foo=:A:",
        s"$leafItem;foo=?2"
      )

      invalidItems.forall { item =>
        parse(headers(HeaderNames.CLIENT_CERT -> item)).contains(None)
      } must beTrue
    }

    "ignore a chain with a tab before its first Structured Fields member" in {
      parse(
        headers(
          HeaderNames.CLIENT_CERT       -> byteSequence(leafBase64),
          HeaderNames.CLIENT_CERT_CHAIN -> s"\t${byteSequence(intermediateBase64)}"
        )
      ) must beRight.like {
        case Some(info) => info.chain must beEmpty
      }
    }

    "ignore a chain that repeats the leaf certificate" in {
      parse(
        headers(
          HeaderNames.CLIENT_CERT       -> byteSequence(leafBase64),
          HeaderNames.CLIENT_CERT_CHAIN -> byteSequence(leafBase64)
        )
      ) must beRight.like {
        case Some(info) => info.chain must beEmpty
      }
    }

    "ignore a chain without a leaf" in {
      parse(headers(HeaderNames.CLIENT_CERT_CHAIN -> byteSequence(intermediateBase64))) must beRight(beNone)
    }

    "ignore a leaf with malformed Base64 or non-certificate DER" in {
      parse(headers(HeaderNames.CLIENT_CERT -> ":not base64:")) must beRight(beNone)
      parse(
        headers(HeaderNames.CLIENT_CERT -> byteSequence(Base64.getEncoder.encodeToString(Array[Byte](1, 2, 3))))
      ) must beRight(beNone)
    }

    "ignore malformed certificate data in the chain while retaining a valid leaf" in {
      parse(
        headers(
          HeaderNames.CLIENT_CERT       -> byteSequence(leafBase64),
          HeaderNames.CLIENT_CERT_CHAIN -> byteSequence(Base64.getEncoder.encodeToString(Array[Byte](1, 2, 3)))
        )
      ) must beRight.like {
        case Some(info) =>
          (info.certificate must_== leaf)
            .and(info.chain must beEmpty)
      }
    }

    "enforce header, decoded-byte, certificate, and chain limits" in {
      val leafHeader = headers(HeaderNames.CLIENT_CERT -> byteSequence(leafBase64))

      parse(leafHeader, limits.copy(maxHeaderBytes = 1)) must beLeft
      parse(leafHeader, limits.copy(maxDecodedBytes = 1)) must beLeft
      parse(leafHeader, limits.copy(maxCertificateBytes = 1)) must beLeft
      parse(
        headers(
          HeaderNames.CLIENT_CERT       -> byteSequence(leafBase64),
          HeaderNames.CLIENT_CERT_CHAIN -> byteSequence(intermediateBase64)
        ),
        limits.copy(maxChainLength = 0)
      ) must beLeft
      parse(
        headers(HeaderNames.CLIENT_CERT_CHAIN -> byteSequence(intermediateBase64)),
        limits.copy(maxChainLength = 0)
      ) must beLeft

      val repeatedEmptyChain = headers(
        HeaderNames.CLIENT_CERT_CHAIN -> "",
        HeaderNames.CLIENT_CERT_CHAIN -> "",
        HeaderNames.CLIENT_CERT_CHAIN -> ""
      )
      parse(repeatedEmptyChain, limits.copy(maxHeaderBytes = 2)) must beRight(beNone)
      parse(repeatedEmptyChain, limits.copy(maxHeaderBytes = 1)) must beLeft
    }

    "accept exact RFC 9440 limits and reject one unit less" in {
      val leafValue    = byteSequence(leafBase64)
      val leafHeader   = headers(HeaderNames.CLIENT_CERT -> leafValue)
      val rawBytes     = leafValue.getBytes(StandardCharsets.UTF_8).length.toLong
      val decodedBytes = leaf.getEncoded.length.toLong

      parse(leafHeader, limits.copy(maxHeaderBytes = rawBytes)) must beRight(beSome[ClientCertificateInfo])
      parse(leafHeader, limits.copy(maxHeaderBytes = rawBytes - 1)) must beLeft

      parse(leafHeader, limits.copy(maxDecodedBytes = decodedBytes)) must beRight(beSome[ClientCertificateInfo])
      parse(leafHeader, limits.copy(maxDecodedBytes = decodedBytes - 1)) must beLeft

      parse(leafHeader, limits.copy(maxCertificateBytes = decodedBytes)) must beRight(beSome[ClientCertificateInfo])
      parse(leafHeader, limits.copy(maxCertificateBytes = decodedBytes - 1)) must beLeft

      val twoCertificateChain = headers(
        HeaderNames.CLIENT_CERT       -> leafValue,
        HeaderNames.CLIENT_CERT_CHAIN -> s"${byteSequence(intermediateBase64)},${byteSequence(rootBase64)}"
      )
      parse(twoCertificateChain, limits.copy(maxChainLength = 2)) must beRight.like {
        case Some(info) => info.chain must contain(exactly(intermediate, root).inOrder)
      }
      parse(twoCertificateChain, limits.copy(maxChainLength = 1)) must beLeft
    }
  }

  private def parse(
      requestHeaders: Headers,
      parserLimits: ClientCertificateHeaderLimits = limits
  ): Either[String, Option[ClientCertificateInfo]] =
    Rfc9440ClientCertificateParser.parse(requestHeaders, parserLimits)

  private def headers(values: (String, String)*): Headers = new Headers(values)

  private def byteSequence(base64: String): String = s":$base64:"

  private def certificate(base64: String): X509Certificate =
    CertificateFactory
      .getInstance("X.509")
      .generateCertificate(new ByteArrayInputStream(Base64.getDecoder.decode(base64)))
      .asInstanceOf[X509Certificate]
}
