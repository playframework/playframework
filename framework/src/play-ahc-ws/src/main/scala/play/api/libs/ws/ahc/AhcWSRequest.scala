/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.ws.ahc

import java.io.File
import java.net.URI

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.ws._
import play.api.mvc.MultipartFormData

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
 * A WS Request backed by AsyncHTTPClient.
 */
case class AhcWSRequest(underlying: StandaloneAhcWSRequest) extends WSRequest with WSBodyWritables {
  override type Self = WSRequest
  override type Response = WSResponse

  /**
   * The URI for this request
   */
  override def uri: URI = underlying.uri

  /**
   * The base URL for this request
   */
  override def url: String = underlying.url

  /**
   * The method for this request
   */
  override def method: String = underlying.method

  /**
   * The body of this request
   */
  override def body: WSBody = underlying.body

  /**
   * The headers for this request
   */
  override def headers: Map[String, Seq[String]] = underlying.headers

  /**
   * The cookies for this request
   */
  override def cookies: Seq[WSCookie] = underlying.cookies

  /**
   * The query string for this request
   */
  override def queryString: Map[String, Seq[String]] = underlying.queryString

  /**
   * A calculator of the signature for this request
   */
  override def calc: Option[WSSignatureCalculator] = underlying.calc
  /**
   * The authentication this request should use
   */
  override def auth: Option[(String, String, WSAuthScheme)] = underlying.auth

  /**
   * Whether this request should follow redirects
   */
  override def followRedirects: Option[Boolean] = underlying.followRedirects

  /**
   * The timeout for the request
   */
  override def requestTimeout: Option[Int] = underlying.requestTimeout

  /**
   * The virtual host this request will use
   */
  override def virtualHost: Option[String] = underlying.virtualHost

  /**
   * The proxy server this request will use
   */
  override def proxyServer: Option[WSProxyServer] = underlying.proxyServer

  override def contentType: Option[String] = underlying.contentType

  override def sign(calc: WSSignatureCalculator): Self = toWSRequest {
    underlying.sign(calc)
  }

  override def withCookies(cookies: WSCookie*): Self = toWSRequest {
    underlying.withCookies(cookies: _*)
  }

  override def addCookies(cookies: WSCookie*): Self = toWSRequest {
    underlying.addCookies(cookies: _*)
  }

  override def withQueryStringParameters(parameters: (String, String)*): Self = toWSRequest {
    underlying.withQueryStringParameters(parameters: _*)
  }

  override def withAuth(username: String, password: String, scheme: WSAuthScheme): Self = toWSRequest {
    underlying.withAuth(username, password, scheme)
  }

  @deprecated("Use addHttpHeaders or withHttpHeaders", "2.6.0")
  override def withHeaders(hdrs: (String, String)*): Self = toWSRequest {
    underlying.addHttpHeaders(hdrs: _*)
  }

  override def withHttpHeaders(headers: (String, String)*): Self = toWSRequest {
    underlying.withHttpHeaders(headers: _*)
  }

  @deprecated("Use addQueryStringParameters or withQueryStringParameters", "2.6.0")
  override def withQueryString(parameters: (String, String)*): Self = toWSRequest {
    underlying.addQueryStringParameters(parameters: _*)
  }

  override def withFollowRedirects(follow: Boolean): Self = toWSRequest {
    underlying.withFollowRedirects(follow)
  }

  override def withRequestTimeout(timeout: Duration): Self = toWSRequest {
    underlying.withRequestTimeout(timeout)
  }

  override def withRequestFilter(filter: WSRequestFilter): Self = toWSRequest {
    underlying.withRequestFilter(filter.asInstanceOf[WSRequestFilter])
  }

  override def withVirtualHost(vh: String): Self = toWSRequest {
    underlying.withVirtualHost(vh)
  }

  override def withProxyServer(proxyServer: WSProxyServer): Self = toWSRequest {
    underlying.withProxyServer(proxyServer)
  }

  override def withMethod(method: String): Self = toWSRequest {
    underlying.withMethod(method)
  }

  override def withBody[T: BodyWritable](body: T): Self = toWSRequest(underlying.withBody(body))

  //-------------------------------------------------
  // PATCH
  //-------------------------------------------------

  /**
   * Perform a PATCH on the request asynchronously.
   */
  override def patch[T: BodyWritable](body: T): Future[Response] = withBody(body).execute("PATCH")

  /**
   * Perform a PATCH on the request asynchronously.
   * Request body won't be chunked
   *
   *
   * @deprecated Import WSBodyWritables and use the typeclass in preference to this method, since 2.6.0
   */
  @deprecated("Use patch(bodyWritable)", "2.6.0")
  override def patch(body: File): Future[WSResponse] = {
    patch[File](body)
  }

  /**
   * Perform a PATCH on the request asynchronously.
   *
   * @deprecated Import WSBodyWritables and use the typeclass in preference to this method, since 2.6.0
   */
  @deprecated("Use patch(bodyWritable)", "2.6.0")
  override def patch(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Future[WSResponse] = {
    patch[Source[MultipartFormData.Part[Source[ByteString, _]], _]](body)
  }

  //-------------------------------------------------
  // POST
  //-------------------------------------------------

  /**
   * Perform a POST on the request asynchronously.
   */
  override def post[T: BodyWritable](body: T): Future[Response] = withBody(body).execute("POST")

  /**
   * Perform a POST on the request asynchronously.
   *
   * @deprecated Import WSBodyWritables and use the typeclass in preference to this method, since 2.6.0
   */
  @deprecated("Use post(BodyWritable)", "2.6.0")
  override def post(body: File): Future[WSResponse] = {
    post[File](body)
  }

  /**
   * Performs a POST on the request asynchronously.
   *
   * @deprecated Import WSBodyWritables and use the typeclass in preference to this method, since 2.6.0
   */
  @deprecated("Use post(BodyWritable)", "2.6.0")
  override def post(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Future[WSResponse] = {
    post[Source[MultipartFormData.Part[Source[ByteString, _]], _]](body)
  }

  //-------------------------------------------------
  // PUT
  //-------------------------------------------------

  /**
   * Perform a PUT on the request asynchronously.
   */
  override def put[T: BodyWritable](body: T): Future[Response] = withBody(body).execute("PUT")

  /**
   * Perform a PUT on the request asynchronously.
   * Request body won't be chunked
   *
   * @deprecated Import WSBodyWritables and use the typeclass in preference to this method, since 2.6.0
   */
  @deprecated("Use put(BodyWritable)", "2.6.0")
  override def put(body: File): Future[WSResponse] = {
    put[File](body)
  }

  /**
   * Perform a PUT on the request asynchronously.
   *
   * @deprecated Import WSBodyWritables and use the typeclass in preference to this method, since 2.6.0
   */
  @deprecated("Use put(BodyWritable)", "2.6.0")
  override def put(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Future[WSResponse] = {
    put[Source[MultipartFormData.Part[Source[ByteString, _]], _]](body)
  }

  override def delete(): Future[Response] = execute("DELETE")

  override def get(): Future[Response] = execute("GET")

  override def head(): Future[Response] = execute("HEAD")

  override def options(): Future[Response] = execute("OPTIONS")

  override def stream(): Future[Response] = {
    val futureResponse: Future[StandaloneWSResponse] = underlying.stream()
    futureResponse.map { f =>
      AhcWSResponse(f)
    }(play.core.Execution.trampoline)
  }

  def execute(method: String): Future[Response] = {
    withMethod(method).execute()
  }

  override def execute(): Future[Response] = {
    val futureResponse: Future[StandaloneWSResponse] = underlying.execute()
    futureResponse.map { f =>
      AhcWSResponse(f)
    }(play.core.Execution.trampoline)
  }

  private def toWSRequest(request: StandaloneWSRequest): Self = {
    AhcWSRequest(request.asInstanceOf[StandaloneAhcWSRequest])
  }

}
