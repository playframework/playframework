/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.Future
import scala.util.Random

import com.google.common.net.InetAddresses
import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.mvc.request.ClientCertificateInfo
import play.api.mvc.request.ClientCertificateSource
import play.api.mvc.request.ForwardingInfo
import play.api.mvc.request.ForwardingSource
import play.api.mvc.request.RemoteEndpoint
import play.api.mvc.request.RemoteInfo
import play.api.mvc.request.RemoteNode
import play.api.mvc.request.Scheme
import play.api.routing._
import play.api.test._
import play.filters.HttpFiltersComponents
import play.it._

class NettyBadClientHandlingSpec     extends BadClientHandlingSpec with NettyIntegrationSpecification
class PekkoHttpBadClientHandlingSpec extends BadClientHandlingSpec with PekkoHttpIntegrationSpecification

trait BadClientHandlingSpec extends PlaySpecification with ServerIntegrationSpecification {
  // RFC 9440 Appendix A's leaf certificate (serial number 7).
  private val forwardedClientCertificateBase64 =
    "MIIBqDCCAU6gAwIBAgIBBzAKBggqhkjOPQQDAjA6MRswGQYDVQQKDBJMZXQncyBB" +
      "dXRoZW50aWNhdGUxGzAZBgNVBAMMEkxBIEludGVybWVkaWF0ZSBDQTAeFw0yMDAx" +
      "MTQyMjU1MzNaFw0yMTAxMjMyMjU1MzNaMA0xCzAJBgNVBAMMAkJDMFkwEwYHKoZI" +
      "zj0CAQYIKoZIzj0DAQcDQgAE8YnXXfaUgmnMtOXU/IncWalRhebrXmckC8vdgJ1p" +
      "5Be5F/3YC8OthxM4+k1M6aEAEFcGzkJiNy6J84y7uzo9M6NyMHAwCQYDVR0TBAIw" +
      "ADAfBgNVHSMEGDAWgBRm3WjLa38lbEYCuiCPct0ZaSED2DAOBgNVHQ8BAf8EBAMC" +
      "BsAwEwYDVR0lBAwwCgYIKwYBBQUHAwIwHQYDVR0RAQH/BBMwEYEPYmRjQGV4YW1w" +
      "bGUuY29tMAoGCCqGSM49BAMCA0gAMEUCIBHda/r1vaL6G3VliL4/Di6YK0Q6bMje" +
      "SkC3dFCOOB8TAiEAx/kHSB4urmiZ0NX5r5XarmPk0wmuydBVoU4hBVZ1yhk="

  "Play" should {
    def withServer[T](
        errorHandler: HttpErrorHandler = DefaultHttpErrorHandler,
        settings: Map[String, AnyRef] = Map.empty
    )(block: Port => T) = {
      val app = new BuiltInComponentsFromContext(ApplicationLoader.Context.create(Environment.simple(), settings))
        with HttpFiltersComponents {
        def router = {
          import sird._
          Router.from {
            case sird.POST(p"/action" ? q_o"query=$query") =>
              Action { request => Results.Ok(query.getOrElse("_")) }
            case _ =>
              Action {
                Results.Ok
              }
          }
        }
        override lazy val httpErrorHandler = errorHandler
      }.application

      runningWithPort(TestServer(testServerPort, app)) { port =>
        block(port)
      }
    }

    def capturingErrorHandler(seen: AtomicReference[RequestHeader]): HttpErrorHandler = new HttpErrorHandler {
      def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
        seen.set(request)
        Future.successful(Results.BadRequest)
      }

      def onServerError(request: RequestHeader, exception: Throwable): Future[Result] =
        Future.successful(Results.InternalServerError)
    }

    "gracefully handle long urls and return 414" in withServer() { port =>
      val url = new String(Random.alphanumeric.take(5 * 1024).toArray)

      val response = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/" + url, "HTTP/1.1", Map(), "")
      )(0)

      response.status must_== 414
    }

    "return a 400 error on invalid URI" in withServer() { port =>
      val response = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/[", "HTTP/1.1", Map(), "")
      )(0)

      response.status must_== 400
      response.body must beLeft
    }

    "still serve requests if query string won't parse" in withServer() { port =>
      val response = BasicHttpClient.makeRequests(port)(
        BasicRequest("POST", "/action?foo=query=bar=", "HTTP/1.1", Map(), "")
      )(0)

      response.status must_== 200
      response.body must beLeft("_")
    }

    "allow accessing the raw unparsed path and request-id from an error handler" in withServer(new HttpErrorHandler() {
      def onClientError(request: RequestHeader, statusCode: Int, message: String) =
        Future.successful(
          Results.BadRequest("Bad path: " + request.path + " message: " + message + " r.id: " + request.id)
        )
      def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = Future.successful(Results.Ok)
    }) { port =>
      val response = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/[", "HTTP/1.1", Map(), "")
      )(0)

      val expectedBodyTrailing = "Bad path: /[ message: Cannot parse path from URI: /[ r.id: "
      response.status must_== 400
      response.body.isLeft must_== true
      val responseBody = response.body.swap.getOrElse("<empty>")
      responseBody must startWith(expectedBodyTrailing)
      responseBody.substring(expectedBodyTrailing.length).matches("[0-9]+") must_== true // must have request id
    }

    "preserve valid trusted forwarding metadata in an error handler" in withServer(
      new HttpErrorHandler() {
        def onClientError(request: RequestHeader, statusCode: Int, message: String) =
          Future.successful(
            Results.BadRequest(
              s"${request.remote.identity}|${request.scheme.render}|${request.secure}|" +
                request.authority.map(_.render).getOrElse("<none>")
            )
          )
        def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = Future.successful(Results.Ok)
      },
      Map(
        "play.http.forwarded.version"            -> "rfc7239",
        "play.http.forwarded.trustForwardedHost" -> java.lang.Boolean.TRUE
      )
    ) { port =>
      val response = BasicHttpClient.makeRequests(port)(
        BasicRequest(
          "GET",
          "/[",
          "HTTP/1.1",
          Map(
            "Forwarded" -> "for=203.0.113.43;proto=https;host=public.example"
          ),
          ""
        )
      )(0)

      response.status must_== 400
      response.body must beLeft("203.0.113.43|https|true|public.example")
    }

    "preserve a trusted RFC 9440 client certificate in an error handler" in {
      val seen = new AtomicReference[RequestHeader]()

      withServer(
        capturingErrorHandler(seen),
        Map(
          "play.http.forwarded.clientCertificates.mode"           -> "rfc9440",
          "play.http.forwarded.clientCertificates.trustedProxies" -> java.util.List.of("127.0.0.0/8", "::1/128")
        )
      ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest(
            "GET",
            "/[",
            "HTTP/1.1",
            Map("Client-Cert" -> s":$forwardedClientCertificateBase64:"),
            ""
          )
        )(0)

        response.status must_== 400
      }

      val request = Option(seen.get()).getOrElse(throw new IllegalStateException("The error handler was not called"))

      request.clientCertificate must beSome[ClientCertificateInfo].like {
        case certificate =>
          (certificate.source must_== ClientCertificateSource.Rfc9440)
            .and(certificate.certificate.getSerialNumber.intValue must_== 7)
      }
      request.xForwardedClientCertificates must beEmpty
    }

    "preserve trusted XFCC assertion metadata in an error handler" in {
      val seen = new AtomicReference[RequestHeader]()

      withServer(
        capturingErrorHandler(seen),
        Map(
          "play.http.forwarded.clientCertificates.mode"           -> "x-forwarded-client-cert",
          "play.http.forwarded.clientCertificates.trustedProxies" ->
            java.util.List.of("127.0.0.0/8", "::1/128")
        )
      ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest(
            "GET",
            "/[",
            "HTTP/1.1",
            Map(
              "X-Forwarded-Client-Cert" ->
                s"Hash=${"a" * 64};URI=spiffe://example.test/workload"
            ),
            ""
          )
        )(0)

        response.status must_== 400
      }

      val request = Option(seen.get()).getOrElse(throw new IllegalStateException("The error handler was not called"))

      request.clientCertificate must beNone
      request.xForwardedClientCertificates must beLike {
        case Vector(assertion) =>
          (assertion.hash must beSome("a" * 64))
            .and(assertion.uris must_== Vector("spiffe://example.test/workload"))
      }
    }

    "omit malformed trusted client certificate metadata in an error handler" in {
      val seen = new AtomicReference[RequestHeader]()

      withServer(
        capturingErrorHandler(seen),
        Map(
          "play.http.forwarded.clientCertificates.mode"           -> "rfc9440",
          "play.http.forwarded.clientCertificates.trustedProxies" -> java.util.List.of("127.0.0.0/8", "::1/128")
        )
      ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/[", "HTTP/1.1", Map("Client-Cert" -> ":not base64:"), "")
        )(0)

        response.status must_== 400
      }

      val request = Option(seen.get()).getOrElse(throw new IllegalStateException("The error handler was not called"))

      request.clientCertificate must beNone
      request.xForwardedClientCertificates must beEmpty
    }

    "preserve the accepted forwarding path and provenance in an error handler" in {
      val seen = new AtomicReference[RequestHeader]()

      withServer(
        capturingErrorHandler(seen),
        Map(
          "play.http.forwarded.version"            -> "rfc7239",
          "play.http.forwarded.trustedProxies"     -> java.util.List.of("127.0.0.1", "::1", "192.0.2.0/24"),
          "play.http.forwarded.trustForwardedHost" -> java.lang.Boolean.TRUE
        )
      ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest(
            "GET",
            "/[",
            "HTTP/1.1",
            Map(
              "Forwarded" -> (
                "for=203.0.113.43;by=_client-edge;proto=https;host=public.example, " +
                  "for=192.0.2.10;by=_play-edge"
              )
            ),
            ""
          )
        )(0)

        response.status must_== 400
      }

      val request = Option(seen.get()).getOrElse(throw new IllegalStateException("The error handler was not called"))
      val client  = RemoteEndpoint(
        RemoteNode.Ip(InetAddresses.forString("203.0.113.43"), None),
        Some(RemoteNode.Obfuscated("_client-edge", None))
      )
      val proxy = RemoteEndpoint(
        RemoteNode.Ip(InetAddresses.forString("192.0.2.10"), None),
        Some(RemoteNode.Obfuscated("_play-edge", None))
      )

      request.remote.isForwarded must beTrue
      request.remote.forwarding must beSome(
        ForwardingInfo(ForwardingSource.Rfc7239, Vector(proxy))
      )
      request.remote.path must_== Vector(client, proxy)
      request.scheme must_== Scheme.Https
      request.authority.map(_.render) must beSome("public.example")
    }

    "preserve a trusted gateway scheme transition in an error handler" in {
      val seen = new AtomicReference[RequestHeader]()

      withServer(
        capturingErrorHandler(seen),
        Map("play.http.forwarded.version" -> "rfc7239")
      ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest(
            "GET",
            "http://localhost/[",
            "HTTP/1.1",
            Map("Forwarded" -> "for=203.0.113.43;proto=https"),
            ""
          )
        )(0)

        response.status must_== 400
      }

      val request = Option(seen.get()).getOrElse(throw new IllegalStateException("The error handler was not called"))

      request.uri must_== "http://localhost/["
      request.remote.identity must_== "203.0.113.43"
      request.remote.isForwarded must beTrue
      request.scheme must_== Scheme.Https
      request.secure must beTrue
      request.authority.map(_.render) must beSome("localhost")
    }

    "treat a malformed Forwarded field as untrusted in an error handler" in {
      val seen = new AtomicReference[RequestHeader]()

      withServer(
        capturingErrorHandler(seen),
        Map(
          "play.http.forwarded.version"            -> "rfc7239",
          "play.http.forwarded.trustForwardedHost" -> java.lang.Boolean.TRUE
        )
      ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest(
            "GET",
            "/[",
            "HTTP/1.1",
            Map(
              "Forwarded" -> "for=203.0.113.43;proto=https;host=attacker.example;broken"
            ),
            ""
          )
        )(0)

        response.status must_== 400
      }

      val request = Option(seen.get()).getOrElse(throw new IllegalStateException("The error handler was not called"))

      request.remote must_== RemoteInfo.fromPeer(request.transport.peer)
      request.remote.isForwarded must beFalse
      request.remote.forwarding must beNone
      request.remote.path must_== Vector(request.remote.endpoint)
      request.scheme must_== Scheme.Http
      request.secure must beFalse
      request.authority.map(_.render) must beSome("localhost")
    }

    "allow accessing (empty) cookies, (empty) session and (empty) flash from an error handler if no headers are given" in withServer(
      new HttpErrorHandler() {
        def onClientError(request: RequestHeader, statusCode: Int, message: String) =
          Future.successful(
            Results.BadRequest(
              "cookies: " + request.cookies + " session: " + request.session + " flash: " + request.flash
            )
          )
        def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = Future.successful(Results.Ok)
      }
    ) { port =>
      val response = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/[", "HTTP/1.1", Map(), "")
      )(0)

      response.status must_== 400
      response.body must beLeft("cookies: Map() session: Session(Map()) flash: Flash(Map())")
    }

    "allow accessing cookies, session and flash from an error handler if headers are set" in withServer(
      new HttpErrorHandler() {
        def onClientError(request: RequestHeader, statusCode: Int, message: String) =
          Future.successful(
            Results.BadRequest(
              "cookies: " + request.cookies + " session: " + request.session + " flash: " + request.flash
            )
          )
        def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = Future.successful(Results.Ok)
      }
    ) { port =>
      val response = BasicHttpClient.makeRequests(port)(
        BasicRequest(
          "GET",
          "/[",
          "HTTP/1.1",
          Map(
            "Cookie" ->
              ("PLAY_SESSION=eyJhbGciOiJIUzI1NiJ9.eyJkYXRhIjp7InNlc3Npb25mb28iOiJzZXNzaW9uYmFyIn0sIm5iZiI6MTc1MjY2ODA1NSwiaWF0IjoxNzUyNjY4MDU1fQ.HgN1CB4OqFE7NlAwuOKMpn5733_wXq295wC_gX34VvU; " +
                "PLAY_FLASH=eyJhbGciOiJIUzI1NiJ9.eyJkYXRhIjp7ImZsYXNoZm9vIjoiZmxhc2hiYXIifSwibmJmIjoxNzUyNjY3OTg0LCJpYXQiOjE3NTI2Njc5ODR9.LXzAn-N8BnlodhFhG3Q4YGAVd47jqq7gGAGrYCrLCEQ")
          ),
          ""
        )
      )(0)

      response.status must_== 400
      response.body must beLeft(
        "cookies: Map(PLAY_SESSION -> Cookie(PLAY_SESSION,eyJhbGciOiJIUzI1NiJ9.eyJkYXRhIjp7InNlc3Npb25mb28iOiJzZXNzaW9uYmFyIn0sIm5iZiI6MTc1MjY2ODA1NSwiaWF0IjoxNzUyNjY4MDU1fQ.HgN1CB4OqFE7NlAwuOKMpn5733_wXq295wC_gX34VvU,None,/,None,false,true,None,false), PLAY_FLASH -> Cookie(PLAY_FLASH,eyJhbGciOiJIUzI1NiJ9.eyJkYXRhIjp7ImZsYXNoZm9vIjoiZmxhc2hiYXIifSwibmJmIjoxNzUyNjY3OTg0LCJpYXQiOjE3NTI2Njc5ODR9.LXzAn-N8BnlodhFhG3Q4YGAVd47jqq7gGAGrYCrLCEQ,None,/,None,false,true,None,false)) " +
          "session: Session(Map(sessionfoo -> sessionbar)) " +
          "flash: Flash(Map(flashfoo -> flashbar))"
      )
    }
  }
}
