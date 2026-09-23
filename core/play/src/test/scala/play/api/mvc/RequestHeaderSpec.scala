/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import java.security.cert.X509Certificate
import java.util.Locale

import scala.jdk.CollectionConverters._

import com.google.common.net.InetAddresses
import org.mockito.Mockito
import org.specs2.mutable.Specification
import play.api.http.HeaderNames._
import play.api.http.HttpConfiguration
import play.api.i18n.Lang
import play.api.i18n.Messages
import play.api.libs.typedmap.TypedEntry
import play.api.libs.typedmap.TypedKey
import play.api.libs.typedmap.TypedMap
import play.api.mvc.request.ClientCertificateInfo
import play.api.mvc.request.ClientCertificateSource
import play.api.mvc.request.DefaultRequestFactory
import play.api.mvc.request.NodePort
import play.api.mvc.request.PeerEndpoint
import play.api.mvc.request.RemoteInfo
import play.api.mvc.request.RemoteNode
import play.api.mvc.request.RequestAttrKey
import play.api.mvc.request.RequestAuthority
import play.api.mvc.request.RequestFactory
import play.api.mvc.request.RequestTarget
import play.api.mvc.request.Scheme
import play.api.mvc.request.TransportConnection
import play.api.mvc.request.TransportTls
import play.api.mvc.request.XForwardedClientCert
import play.mvc.Http

class RequestHeaderSpec extends Specification {
  "request header" should {
    "convert to java" in {
      "keep all the headers" in {
        val rh = dummyRequestHeader("GET", "/", Headers(HOST -> "playframework.com"))
        rh.asJava.headers.contains(HOST) must beTrue
      }
      "keep the headers accessible case insensitively" in {
        val rh = dummyRequestHeader("GET", "/", Headers(HOST -> "playframework.com"))
        rh.asJava.headers.contains("host") must beTrue
      }
      "keep the remote port in selected remote metadata" in {
        val rh = dummyRequestHeader(remotePort = Some(12345))
        rh.remote.port must beSome(12345)
        rh.asJava.remote.port must beEqualTo(java.util.Optional.of(12345))
      }
      "keep the remote node and remote IP address" in {
        val rh = dummyRequestHeader(remotePort = Some(12345))
        rh.remote.node must beEqualTo(
          RemoteNode.Ip(
            InetAddresses.forString("127.0.0.1"),
            Some(NodePort.Numeric(12345))
          )
        )
        rh.remote.ipAddress must beSome(InetAddresses.forString("127.0.0.1"))
        rh.remote.identity must beEqualTo("127.0.0.1")
        rh.asJava.remote.node must beEqualTo(
          new Http.RemoteNode.Ip(
            InetAddresses.forString("127.0.0.1"),
            java.util.Optional.of(new Http.NodePort.Numeric(12345))
          )
        )
        rh.asJava.remote.ipAddress must beEqualTo(
          java.util.Optional.of(InetAddresses.forString("127.0.0.1"))
        )
        rh.asJava.remote.identity must beEqualTo("127.0.0.1")
      }
      "keep direct transport metadata" in {
        val rh = dummyRequestHeader(remotePort = Some(12345))

        rh.transport.peer must beEqualTo(
          PeerEndpoint(InetAddresses.forString("127.0.0.1"), Some(12345))
        )
        rh.asJava.transport must beEqualTo(
          new Http.TransportConnection(
            new Http.PeerEndpoint(InetAddresses.forString("127.0.0.1"), java.util.Optional.of(12345)),
            java.util.Optional.empty[Http.TransportTls]
          )
        )
      }
    }

    "preserve direct transport metadata across request copies" in {
      val transport = TransportConnection(
        PeerEndpoint(InetAddresses.forString("192.0.2.10"), Some(53124)),
        Some(TransportTls(Seq.empty))
      )
      val request = dummyRequestHeader().withTransport(transport)

      Seq(
        request.withRemote(RemoteInfo.ip("203.0.113.43", None)),
        request.withMethod("POST"),
        request.withTarget(RequestTarget("/copy", "/copy", Map.empty)),
        request.withVersion("HTTP/2"),
        request.withHeaders(Headers("X-Test" -> "copy")),
        request.withAttrs(TypedMap.empty),
        request.withBody("body")
      ).foreach(_.transport must beEqualTo(transport))
      ok
    }

    "preserve effective client certificate metadata across unrelated request copies" in {
      val leaf         = Mockito.mock(classOf[X509Certificate])
      val intermediate = Mockito.mock(classOf[X509Certificate])
      val certificate  = ClientCertificateInfo(leaf, Vector(intermediate), ClientCertificateSource.Rfc9440)
      val request      = dummyRequestHeader().withClientCertificate(Some(certificate))

      request.asJava.clientCertificate.orElseThrow() must beEqualTo(certificate.asJava)
      Seq(
        request.withTransport(
          TransportConnection(PeerEndpoint(InetAddresses.forString("192.0.2.10"), Some(53124)), None)
        ),
        request.withRemote(RemoteInfo.ip("203.0.113.43", None)),
        request.withScheme(Scheme.Https),
        request.withAuthority(Some(RequestAuthority.parseOrThrow("public.example"))),
        request.withMethod("POST"),
        request.withTarget(RequestTarget("/copy", "/copy", Map.empty)),
        request.withVersion("HTTP/2"),
        request.withHeaders(Headers("X-Test" -> "copy")),
        request.withAttrs(TypedMap.empty),
        request.withBody("body")
      ).foreach(_.clientCertificate must beSome(certificate))

      request.withClientCertificate(None).clientCertificate must beNone
      request.withClientCertificate(null) must throwA[IllegalArgumentException]
      request.withClientCertificate(Some(null)) must throwA[IllegalArgumentException]
    }

    "preserve ordered XFCC assertions across unrelated request copies" in {
      def assertion(uri: String) = XForwardedClientCert(
        by = Vector.empty,
        hash = None,
        certificate = None,
        chain = Vector.empty,
        subject = None,
        uris = Vector(uri),
        dnsNames = Vector.empty
      )
      val assertions = Vector(assertion("spiffe://example/client"), assertion("spiffe://example/proxy"))
      val request    = dummyRequestHeader().withXForwardedClientCertificates(assertions)

      request.xForwardedClientCertificates must contain(exactly(assertions*).inOrder)
      request.asJava.xForwardedClientCertificates must beEqualTo(assertions.map(_.asJava).asJava)
      Seq(
        request.withTransport(
          TransportConnection(PeerEndpoint(InetAddresses.forString("192.0.2.10"), Some(53124)), None)
        ),
        request.withClientCertificate(None),
        request.withRemote(RemoteInfo.ip("203.0.113.43", None)),
        request.withScheme(Scheme.Https),
        request.withAuthority(Some(RequestAuthority.parseOrThrow("public.example"))),
        request.withMethod("POST"),
        request.withTarget(RequestTarget("/copy", "/copy", Map.empty)),
        request.withVersion("HTTP/2"),
        request.withHeaders(Headers("X-Test" -> "copy")),
        request.withAttrs(TypedMap.empty),
        request.withBody("body")
      ).foreach(_.xForwardedClientCertificates must contain(exactly(assertions*).inOrder))

      request.withXForwardedClientCertificates(Vector.empty).xForwardedClientCertificates must beEmpty
      request.withXForwardedClientCertificates(null) must throwA[IllegalArgumentException]
      request.withXForwardedClientCertificates(Vector(null)) must throwA[IllegalArgumentException]
    }

    "preserve selected remote metadata across unrelated request copies" in {
      val remote  = RemoteInfo(RemoteNode.Obfuscated("_anonymous", Some(NodePort.Obfuscated("_source"))), None)
      val request = dummyRequestHeader().withRemote(remote)

      Seq(
        request.withTransport(
          TransportConnection(PeerEndpoint(InetAddresses.forString("192.0.2.10"), Some(53124)), None)
        ),
        request.withScheme(Scheme.Https),
        request.withAuthority(Some(RequestAuthority.parseOrThrow("public.example"))),
        request.withMethod("POST"),
        request.withTarget(RequestTarget("/copy", "/copy", Map.empty)),
        request.withVersion("HTTP/2"),
        request.withHeaders(Headers("X-Test" -> "copy")),
        request.withAttrs(TypedMap.empty),
        request.withBody("body")
      ).foreach(_.remote must beEqualTo(remote))
      ok
    }

    "reject null typed request metadata in copy and plain-factory paths" in {
      val request = dummyRequestHeader()

      request.withRemote(null) must throwA[IllegalArgumentException]
      request.withTransport(null) must throwA[IllegalArgumentException]
      request.withScheme(null) must throwA[IllegalArgumentException]
      request.withAuthority(null) must throwA[IllegalArgumentException]
      request.withAuthority(Some(null)) must throwA[IllegalArgumentException]

      def create(
          transport: TransportConnection = request.transport,
          remote: RemoteInfo = request.remote,
          scheme: Scheme = request.scheme,
          authority: Option[RequestAuthority] = request.authority
      ): RequestHeader =
        RequestFactory.plain.createRequestHeader(
          transport,
          request.clientCertificate,
          request.xForwardedClientCertificates,
          remote,
          scheme,
          authority,
          request.method,
          request.target,
          request.version,
          request.headers,
          request.attrs
        )

      create(remote = null) must throwA[IllegalArgumentException]
      create(transport = null) must throwA[IllegalArgumentException]
      create(scheme = null) must throwA[IllegalArgumentException]
      create(authority = null) must throwA[IllegalArgumentException]
      create(authority = Some(null)) must throwA[IllegalArgumentException]
    }

    "allow custom request headers to define their own remote shortcut names" in {
      final class CustomRequestHeader(delegate: RequestHeader) extends RequestHeader {
        override def transport: TransportConnection                             = delegate.transport
        override def clientCertificate: Option[ClientCertificateInfo]           = delegate.clientCertificate
        override def xForwardedClientCertificates: Vector[XForwardedClientCert] =
          delegate.xForwardedClientCertificates
        override def remote: RemoteInfo                  = delegate.remote
        override def scheme: Scheme                      = delegate.scheme
        override def authority: Option[RequestAuthority] = delegate.authority
        override def method: String                      = delegate.method
        override def target: RequestTarget               = delegate.target
        override def version: String                     = delegate.version
        override def headers: Headers                    = delegate.headers
        override def attrs: TypedMap                     = delegate.attrs

        def remoteIdentity: String  = "application-defined"
        def remotePort: Option[Int] = Some(43210)
      }
      val custom = new CustomRequestHeader(
        dummyRequestHeader().withRemote(RemoteInfo.ip("192.0.2.43", None))
      )

      custom.remoteIdentity must_== "application-defined"
      custom.remotePort must beSome(43210)
      custom.remote.identity must_== "192.0.2.43"
      custom.remote.port must beNone
    }

    "have typed attributes" in {
      "can set and get a single attribute" in {
        val x = TypedKey[Int]("x")
        dummyRequestHeader().withAttrs(TypedMap(x -> 3)).attrs(x) must_== 3
      }
      "can set two attributes and get one back" in {
        val x = TypedKey[Int]("x")
        val y = TypedKey[String]("y")
        dummyRequestHeader().withAttrs(TypedMap(x -> 3, y -> "hello")).attrs(y) must_== "hello"
      }
      "getting a set attribute should be Some" in {
        val x = TypedKey[Int]("x")
        dummyRequestHeader().withAttrs(TypedMap(x -> 5)).attrs.get(x) must beSome(5)
      }
      "getting a nonexistent attribute should be None" in {
        val x = TypedKey[Int]("x")
        dummyRequestHeader().attrs.get(x) must beNone
      }
      "can add single attribute" in {
        val x = TypedKey[Int]("x")
        dummyRequestHeader().addAttr(x, 3).attrs(x) must_== 3
      }
      "keep current attributes when adding a new one" in {
        val x = TypedKey[Int]
        val y = TypedKey[String]
        dummyRequestHeader().withAttrs(TypedMap(y -> "hello")).addAttr(x, 3).attrs(y) must_== "hello"
      }
      "overrides current attribute value" in {
        val x             = TypedKey[Int]
        val y             = TypedKey[String]
        val requestHeader = dummyRequestHeader()
          .withAttrs(TypedMap(y -> "hello"))
          .addAttr(x, 3)
          .addAttr(y, "white")

        requestHeader.attrs(y) must_== "white"
        requestHeader.attrs(x) must_== 3
      }
      "can add multiple attributes" in {
        val x   = TypedKey[Int]("x")
        val y   = TypedKey[Int]("y")
        val req = dummyRequestHeader().addAttrs(TypedEntry(x, 3), TypedEntry(y, 4))
        req.attrs(x) must_== 3
        req.attrs(y) must_== 4
      }
      "keep current attributes when adding multiple ones" in {
        val x = TypedKey[Int]
        val y = TypedKey[Int]
        val z = TypedKey[String]
        dummyRequestHeader()
          .withAttrs(TypedMap(z -> "hello"))
          .addAttrs(TypedEntry(x, 3), TypedEntry(y, 4))
          .attrs(z) must_== "hello"
      }
      "overrides current attribute value when adding multiple attributes" in {
        val x             = TypedKey[Int]
        val y             = TypedKey[Int]
        val z             = TypedKey[String]
        val requestHeader = dummyRequestHeader()
          .withAttrs(TypedMap(z -> "hello"))
          .addAttrs(TypedEntry(x, 3), TypedEntry(y, 4), TypedEntry(z, "white"))

        requestHeader.attrs(z) must_== "white"
        requestHeader.attrs(x) must_== 3
        requestHeader.attrs(y) must_== 4
      }
      "can set two attributes and get both back" in {
        val x = TypedKey[Int]("x")
        val y = TypedKey[String]("y")
        val r = dummyRequestHeader().withAttrs(TypedMap(x -> 3, y -> "hello"))
        r.attrs(x) must_== 3
        r.attrs(y) must_== "hello"
      }
      "can set two attributes and remove one of them" in {
        val x   = TypedKey[Int]("x")
        val y   = TypedKey[String]("y")
        val req = dummyRequestHeader().withAttrs(TypedMap(x -> 3, y -> "hello")).removeAttr(x)
        req.attrs.get(x) must beNone
        req.attrs(y) must_== "hello"
      }
      "can set two attributes and remove both again" in {
        val x   = TypedKey[Int]("x")
        val y   = TypedKey[String]("y")
        val req = dummyRequestHeader().withAttrs(TypedMap(x -> 3, y -> "hello")).removeAttr(x).removeAttr(y)
        req.attrs.get(x) must beNone
        req.attrs.get(y) must beNone
      }
      "handle empty attributes" in {
        "always return (at least an empty) cookies" in {
          dummyRawRequestHeaderWithEmptyAttrs().cookies.size must_== 0
        }

        "always return (at least an empty) session" in {
          dummyRawRequestHeaderWithEmptyAttrs().session.isEmpty must_== true
        }

        "always return (at least an empty) flash" in {
          dummyRawRequestHeaderWithEmptyAttrs().flash.isEmpty must_== true
        }
      }
    }
    "handle transient lang" in {
      val req1 = dummyRequestHeader()
      req1.transientLang() must beNone
      req1.attrs.get(Messages.Attrs.CurrentLang) must beNone

      val req2 = req1.withTransientLang(new Lang(Locale.GERMAN))
      req1 mustNotEqual req2
      req2.transientLang() must beSome(new Lang(Locale.GERMAN))
      req2.attrs.get(Messages.Attrs.CurrentLang) must beSome(new Lang(Locale.GERMAN))

      val req3 = req2.withoutTransientLang()
      req2 mustNotEqual req3
      req3.transientLang() must beNone
      req3.attrs.get(Messages.Attrs.CurrentLang) must beNone
    }

    "handle host" in {
      "relative uri with host header" in {
        val rh = dummyRequestHeader("GET", "/", Headers(HOST -> "playframework.com"))
        rh.host must_== "playframework.com"
      }
      "absolute uri" in {
        val rh = dummyRequestHeader("GET", "https://example.com/test", Headers(HOST -> "playframework.com"))
        rh.host must_== "example.com"
      }
      "absolute uri with port" in {
        val rh = dummyRequestHeader("GET", "https://example.com:8080/test", Headers(HOST -> "playframework.com"))
        rh.host must_== "example.com:8080"
      }
      "absolute uri with port and invalid characters" in {
        val rh = dummyRequestHeader(
          "GET",
          "https://example.com:8080/classified-search/classifieds?version=GTI|V8",
          Headers(HOST -> "playframework.com")
        )
        rh.host must_== "example.com:8080"
      }
      "relative uri with invalid characters" in {
        val rh = dummyRequestHeader(
          "GET",
          "/classified-search/classifieds?version=GTI|V8",
          Headers(HOST -> "playframework.com")
        )
        rh.host must_== "playframework.com"
      }
    }

    "handle scheme and authority" in {
      "normalize a relative target Host and expose its typed components" in {
        val rh = dummyRequestHeader("GET", "/", Headers(HOST -> "PLAYFRAMEWORK.com:09000"))

        rh.scheme must_== Scheme.Http
        rh.secure must beFalse
        rh.host must_== "playframework.com:9000"
        rh.domain must_== "playframework.com"
        rh.headers.getAll(HOST) must contain(exactly("playframework.com:9000"))
      }

      "retain a typed bracketed IPv6 authority" in {
        val rh = dummyRequestHeader("GET", "/", Headers(HOST -> "[2001:0DB8::1]:9000"))

        rh.host must_== "[2001:db8::1]:9000"
        rh.domain must_== "[2001:db8::1]"
      }

      "apply strict IP-literal syntax to the direct Host field" in {
        val mapped = dummyRequestHeader("GET", "/", Headers(HOST -> "[::ffff:192.0.2.43]:9000"))
        mapped.host must_== "[::ffff:c000:22b]:9000"

        Seq("[fe80::1%1]", "[fe80::1%25eth0]", "１２７.０.０.１", "١٢٧.٠.٠.١").foreach { value =>
          dummyRequestHeader("GET", "/", Headers(HOST -> value)) must throwA[IllegalArgumentException]
        }
        ok
      }

      "reject an invalid Host authority" in {
        dummyRequestHeader("GET", "/", Headers(HOST -> "2001:db8::1")) must throwA[IllegalArgumentException]
      }

      "take scheme and authority from an absolute target" in {
        val initial = initialRequestTarget(
          "GET",
          "https://EXAMPLE.com:08443?x=1",
          "HTTP/1.1",
          Headers(HOST -> "proxy.internal")
        ).toOption.get

        initial.authority must beSome(RequestAuthority.parseOrThrow("example.com:8443"))
        initial.scheme must beSome(Scheme.Https)
      }

      "normalize an empty HTTP absolute path without parsing or replacing the raw target" in {
        val target = RequestTarget(
          "http://example.com?version=GTI|V8",
          "",
          Map("version" -> Seq("GTI|V8"))
        )
        val initial = RequestHeader
          .initialRequestTarget("GET", target, "HTTP/1.1", Headers(HOST -> "example.com"))
          .toOption
          .get
        val normalized = RequestHeader.normalizeRequestTargetPath(target, initial)

        normalized.path must_== "/"
        normalized.uriString must_== "http://example.com?version=GTI|V8"
        normalized.queryMap must_== Map("version" -> Seq("GTI|V8"))
      }

      "leave asterisk-form and CONNECT authority-form paths unchanged" in {
        val targets = Seq(
          ("OPTIONS", RequestTarget("*", "", Map.empty), Headers(HOST -> "example.com")),
          ("CONNECT", RequestTarget("example.com:443", "", Map.empty), Headers(HOST -> "example.com"))
        )

        targets.foreach {
          case (method, target, headers) =>
            val initial = RequestHeader.initialRequestTarget(method, target, "HTTP/1.1", headers).toOption.get
            RequestHeader.normalizeRequestTargetPath(target, initial).path must beEmpty
        }
        ok
      }

      "use an absolute target authority when HTTP/1.1 carries an empty Host field" in {
        val rh = dummyRequestHeader("GET", "http://TARGET.example:080/path", Headers(HOST -> ""))

        rh.authority must beSome(RequestAuthority.parseOrThrow("target.example:80"))
        rh.host must_== "target.example:80"
        rh.headers.getAll(HOST) must contain(exactly("target.example:80"))
      }

      "accept an absolute target scheme from either side of a trusted gateway" in {
        RequestHeader.effectiveScheme(None, Scheme.Http, Scheme.Https) must beRight(Scheme.Https)
        RequestHeader.effectiveScheme(Some(Scheme.Https), Scheme.Http, Scheme.Https) must beRight(Scheme.Https)
        RequestHeader.effectiveScheme(Some(Scheme.Http), Scheme.Http, Scheme.Https) must beRight(Scheme.Https)
        RequestHeader.effectiveScheme(Some(Scheme.Https), Scheme.Https, Scheme.Http) must beRight(Scheme.Http)
        RequestHeader.effectiveScheme(Some(Scheme.Http), Scheme.Http, Scheme.Http) must beRight(Scheme.Http)

        RequestHeader.effectiveScheme(Some(Scheme.Https), Scheme.Http, Scheme.Http) must beLeft
        RequestHeader.effectiveScheme(Some(Scheme.Http), Scheme.Https, Scheme.Https) must beLeft
      }

      "extract an absolute authority without parsing invalid path or query characters" in {
        val rh = dummyRequestHeader(
          "GET",
          "https://example.com:8080/classified-search/classifieds?version=GTI|V8",
          Headers(HOST -> "proxy.internal")
        )

        rh.host must_== "example.com:8080"
      }

      "reject fragments in request targets" in {
        dummyRequestHeader("GET", "http://example.com/path#fragment", Headers(HOST -> "example.com")) must
          throwA[IllegalArgumentException]
        dummyRequestHeader("GET", "/path#fragment", Headers(HOST -> "example.com")) must
          throwA[IllegalArgumentException]
      }

      "reject an empty host in an absolute-form request target" in {
        dummyRequestHeader("GET", "http:///path", Headers(HOST -> "example.com")) must
          throwA[IllegalArgumentException]
        dummyRequestHeader("GET", "https:///path", Headers(HOST -> "example.com")) must
          throwA[IllegalArgumentException]
        dummyRequestHeader("GET", "http://:8080/path", Headers(HOST -> "example.com")) must
          throwA[IllegalArgumentException]
        dummyRequestHeader("GET", "http:opaque", Headers(HOST -> "example.com")) must
          throwA[IllegalArgumentException]
      }

      "reject unsupported absolute schemes without parsing their remainder" in {
        RequestHeader.absoluteTarget("urn:example:test") must beLeft("Unsupported absolute request scheme 'urn'")
        RequestHeader.absoluteTarget("urn:example|test") must beLeft("Unsupported absolute request scheme 'urn'")
        RequestHeader.absoluteTarget("custom:///path") must beLeft("Unsupported absolute request scheme 'custom'")
        RequestHeader.absoluteTarget("custom:") must beLeft("Unsupported absolute request scheme 'custom'")
        initialRequestTarget("GET", "urn:example:test", "HTTP/1.1", Headers(HOST -> "")) must
          beLeft("Unsupported absolute request scheme 'urn'")
        initialRequestTarget("GET", "urn:example:test", "HTTP/1.1", Headers()) must
          beLeft("An HTTP/1.1 request must contain a Host header")
        initialRequestTarget("GET", "urn:example:test", "HTTP/1.1", Headers(HOST -> "example.com")) must
          beLeft("Unsupported absolute request scheme 'urn'")
        initialRequestTarget("GET", "urn:example:test", "HTTP/1.1", Headers(HOST -> "user@example.com")) must beLeft
        initialRequestTarget("GET", "custom://example.com/path", "HTTP/1.1", Headers(HOST -> "example.com")) must
          beLeft("Unsupported absolute request scheme 'custom'")
        initialRequestTarget("GET", "urn:example:test", "HTTP/1.0", Headers()) must
          beLeft("Unsupported absolute request scheme 'urn'")
      }

      "take a CONNECT authority from its request target" in {
        val rh = dummyRequestHeader("CONNECT", "public.example:443", Headers(HOST -> "proxy.internal"))

        rh.host must_== "public.example:443"
      }

      "use a CONNECT target authority when HTTP/1.1 carries an empty Host field" in {
        val rh = dummyRequestHeader("CONNECT", "PUBLIC.example:443", Headers(HOST -> ""))

        rh.authority must beSome(RequestAuthority.parseOrThrow("public.example:443"))
        rh.host must_== "public.example:443"
        rh.headers.getAll(HOST) must contain(exactly("public.example:443"))
      }

      "accept the largest usable CONNECT destination port" in {
        val rh = dummyRequestHeader("CONNECT", "public.example:65535", Headers(HOST -> "proxy.internal"))

        rh.host must_== "public.example:65535"
        rh.authority.flatMap(_.port).flatMap(_.tcpPort) must beSome(65535)
      }

      "reject a CONNECT target without an explicit port" in {
        dummyRequestHeader("CONNECT", "public.example", Headers(HOST -> "proxy.internal")) must
          throwA[IllegalArgumentException]
      }

      "reject CONNECT targets that are not authority-form or have an unusable port" in {
        Seq(
          "http://public.example:443",
          "public.example:0",
          "public.example:65536",
          "public.example:999999999999999999999999999999"
        ).foreach { target =>
          dummyRequestHeader("CONNECT", target, Headers(HOST -> "proxy.internal")) must
            throwA[IllegalArgumentException]
        }
        ok
      }

      "enforce request-target forms for other methods" in {
        dummyRequestHeader("GET", "example.com:443", Headers(HOST -> "example.com")) must
          throwA[IllegalArgumentException]
        dummyRequestHeader("GET", "*", Headers(HOST -> "example.com")) must
          throwA[IllegalArgumentException]
        dummyRequestHeader("OPTIONS", "*", Headers(HOST -> "example.com")).uri must_== "*"
      }

      "require a non-empty Host authority when the request target does not supply one" in {
        initialRequestTarget("GET", "/", "HTTP/1.1", Headers(HOST -> "")) must
          beLeft("A Host header must contain a non-empty host")
        initialRequestTarget("OPTIONS", "*", "HTTP/1.1", Headers(HOST -> "")) must
          beLeft("A Host header must contain a non-empty host")
      }

      "require one valid Host header for HTTP/1.1 before target precedence" in {
        initialRequestTarget("GET", "/", "HTTP/1.1", Headers()) must beLeft
        initialRequestTarget("GET", "http://target.example/", "HTTP/1.1", Headers()) must beLeft
        initialRequestTarget(
          "GET",
          "http://target.example/",
          "HTTP/1.1",
          Headers(HOST -> "user@ignored.example")
        ) must beLeft
      }

      "allow a missing Host outside HTTP/1.1 while still validating one when present" in {
        initialRequestTarget("GET", "/", "HTTP/1.0", Headers()).map(_.authority) must
          beRight(Option.empty[RequestAuthority])
        initialRequestTarget("GET", "/", "HTTP/2", Headers()).map(_.authority) must
          beRight(Option.empty[RequestAuthority])
        initialRequestTarget("GET", "/", "HTTP/1.0", Headers(HOST -> "")) must beLeft
      }

      "reject duplicate Host fields" in {
        dummyRequestHeader("GET", "/", Headers(HOST -> "one.example", HOST -> "two.example")) must
          throwA[IllegalArgumentException]
      }

      "replace an absolute target authority explicitly without changing the target" in {
        val publicAuthority = RequestAuthority.parseOrThrow("public.example")
        val requestHeader   = dummyRequestHeader(
          requestMethod = "GET",
          requestUri = "https://internal.example/test",
          headers = Headers(HOST -> "internal.example"),
          requestPath = "/test"
        ).withAuthority(Some(publicAuthority))
        val request = requestHeader.withBody("body")

        requestHeader.authority must beSome(publicAuthority)
        requestHeader.host must_== "public.example"
        request.host must_== "public.example"
        requestHeader.asJava.authority must_== java.util.Optional.of(publicAuthority.asJava)
        request.asJava.host must_== "public.example"
        requestHeader.headers.getAll(HOST) must contain(exactly("public.example"))
        requestHeader.uri must_== "https://internal.example/test"
        request.uri must_== "https://internal.example/test"
      }

      "keep Host as a canonical view when replacing headers" in {
        val rh = dummyRequestHeader("GET", "/", Headers(HOST -> "example.com"))

        rh.withHeaders(Headers("X-Test" -> "one")).headers.getAll(HOST) must contain(exactly("example.com"))
        rh.withHeaders(Headers(HOST -> "example.com", "X-Test" -> "one")).host must_== "example.com"
        rh.withHeaders(Headers(HOST -> "other.example")) must throwA[IllegalArgumentException]
        rh.withHeaders(Headers(HOST -> "example.com", HOST -> "example.com")) must
          throwA[IllegalArgumentException]
      }

      "remove and restore authority only through withAuthority" in {
        val rh      = dummyRequestHeader("GET", "/", Headers(HOST -> "example.com"))
        val without = rh.withAuthority(None)

        without.authority must beNone
        without.host must beEmpty
        without.headers.getAll(HOST) must beEmpty
        without.withHeaders(Headers(HOST -> "example.com")) must throwA[IllegalArgumentException]
        without.withAuthority(Some(RequestAuthority.parseOrThrow("new.example"))).host must_== "new.example"
      }

      "preserve a custom effective scheme in request-aware absolute URLs" in {
        implicit val request: RequestHeader = dummyRequestHeader(
          "GET",
          "https://internal.example/current",
          Headers(HOST -> "internal.example")
        ).withScheme(Scheme.parseOrThrow("Git+SSH.v1-2"))
          .withAuthority(Some(RequestAuthority.parseOrThrow("PUBLIC.example:08443")))

        Call("GET", "/next").absoluteURL() must_== "git+ssh.v1-2://public.example:8443/next"
        Call("GET", "/next").absoluteURL(secure = true) must_== "https://public.example:8443/next"
      }
    }

    "parse accept languages" in {
      "return an empty sequence when no accept languages specified" in {
        dummyRequestHeader().acceptLanguages must beEmpty
      }

      "parse a single accept language" in {
        accept("en") must contain(exactly(Lang("en")))
      }

      "parse a single accept language and country" in {
        accept("en-US") must contain(exactly(Lang("en-US")))
      }

      "parse multiple accept languages" in {
        accept("en-US, es") must contain(exactly(Lang("en-US"), Lang("es")).inOrder)
      }

      "sort accept languages by quality" in {
        accept("en-US;q=0.8, es;q=0.7") must contain(exactly(Lang("en-US"), Lang("es")).inOrder)
        accept("en-US;q=0.7, es;q=0.8") must contain(exactly(Lang("es"), Lang("en-US")).inOrder)
      }

      "default accept language quality to 1" in {
        accept("en-US, es;q=0.7") must contain(exactly(Lang("en-US"), Lang("es")).inOrder)
        accept("en-US;q=0.7, es") must contain(exactly(Lang("es"), Lang("en-US")).inOrder)
      }
    }

    "have request id" in {
      "generated if it does not exist yet" in {
        val rh = dummyRequestHeader()
        // The request id will likely be somewhere from 1 to 10000 in the tests
        rh.id must beBetween(1L, 10000L)
        rh.attrs(RequestAttrKey.Id) must beBetween(1L, 10000L)
      }

      "not generated if one exists in the attrs already" in {
        dummyRequestHeader(attrs = TypedMap(RequestAttrKey.Id -> 987656789)).id must_== 987656789
      }
    }
  }

  private def accept(value: String) =
    dummyRequestHeader(
      headers = Headers("Accept-Language" -> value)
    ).acceptLanguages

  private def dummyRequestHeader(
      requestMethod: String = "GET",
      requestUri: String = "/",
      headers: Headers = Headers(),
      attrs: TypedMap = TypedMap.empty,
      remotePort: Option[Int] = None,
      requestPath: String = ""
  ): RequestHeader = {
    val peer      = PeerEndpoint(InetAddresses.forString("127.0.0.1"), remotePort)
    val transport = TransportConnection(peer, None)
    val remote    = RemoteInfo.fromPeer(peer)
    val target    = RequestTarget(requestUri, requestPath, Map.empty)
    val authority = RequestHeader
      .initialAuthority(requestMethod, target, headers)
      .fold(error => throw new IllegalArgumentException(error), identity)
    new DefaultRequestFactory(HttpConfiguration()).createRequestHeader(
      transport = transport,
      clientCertificate = None,
      xForwardedClientCertificates = Vector.empty,
      remote = remote,
      scheme = RequestHeader.initialScheme(transport),
      authority = authority,
      method = requestMethod,
      target = target,
      version = "",
      headers = headers,
      attrs = attrs
    )
  }

  private def initialRequestTarget(
      method: String,
      uri: String,
      version: String,
      headers: Headers
  ): Either[String, RequestHeader.InitialRequestTarget] =
    RequestHeader.initialRequestTarget(method, RequestTarget(uri, "", Map.empty), version, headers)

  private def dummyRawRequestHeaderWithEmptyAttrs() = new RequestHeader {
    override def transport: TransportConnection                             = ???
    override def clientCertificate: Option[ClientCertificateInfo]           = ???
    override def xForwardedClientCertificates: Vector[XForwardedClientCert] = ???
    override def scheme: Scheme                                             = ???
    override def authority: Option[RequestAuthority]                        = ???
    override def remote: RemoteInfo                                         = ???
    override def method: String                                             = ???
    override def target: RequestTarget                                      = ???
    override def version: String                                            = ???
    override def headers: Headers                                           = ???
    override def attrs: TypedMap                                            = TypedMap.empty
  }
}
