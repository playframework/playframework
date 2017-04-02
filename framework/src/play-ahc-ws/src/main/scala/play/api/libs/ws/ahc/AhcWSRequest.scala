/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.ws.ahc

import java.net.URI

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.ws.{ WSBody, _ }
import play.api.mvc.MultipartFormData
import play.core.formatters.Multipart

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
 * A WS Request backed by AsyncHTTPClient.
 */
case class AhcWSRequest(underlying: StandaloneAhcWSRequest) extends WSRequest {
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

  override def withAuth(username: String, password: String, scheme: WSAuthScheme): Self = toWSRequest {
    underlying.withAuth(username, password, scheme)
  }

  override def withHeaders(hdrs: (String, String)*): Self = toWSRequest {
    underlying.withHeaders(hdrs: _*)
  }

  override def withQueryString(parameters: (String, String)*): Self = toWSRequest {
    underlying.withQueryString(parameters: _*)
  }

  override def withFollowRedirects(follow: Boolean): Self = toWSRequest {
    underlying.withFollowRedirects(follow)
  }

  override def withRequestTimeout(timeout: Duration): Self = toWSRequest {
    underlying.withRequestTimeout(timeout)
  }

  override def withRequestFilter(filter: WSRequestFilter): WSRequest = toWSRequest {
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

  override def withBody(body: WSBody): Self = toWSRequest {
    underlying.withBody(body)
  }

  override def withBody(file: java.io.File): Self = toWSRequest(underlying.withBody(file))

  override def withBody[T: BodyWritable](body: T): Self = toWSRequest(underlying.withBody(body))

  /**
   * Sets a multipart body for this request
   */
  override def withBody(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Self = {
    val boundary = Multipart.randomBoundary()
    val contentType = s"multipart/form-data; boundary=$boundary"
    withBody(StreamedBody(Multipart.transform(body, boundary))).withHeaders("Content-Type" -> contentType)
  }

  override def delete(): Future[Response] = execute("DELETE")

  override def get(): Future[Response] = execute("GET")

  override def head(): Future[Response] = execute("HEAD")

  override def options(): Future[Response] = execute("OPTIONS")

  /**
   * Perform a PATCH on the request asynchronously.
   */
  override def patch(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Future[Response] = {
    withBody(body).execute("PATCH")
  }
  override def patch(file: java.io.File): Future[Response] = withBody(file).execute("PATCH")
  override def patch[T: BodyWritable](body: T): Future[Response] = withBody(body).execute("PATCH")

  /**
   * Perform a POST on the request asynchronously.
   */
  override def post(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Future[Response] =
    withBody(body).execute("POST")
  override def post(body: java.io.File): Future[Response] = withBody(body).execute("POST")
  override def post[T: BodyWritable](body: T): Future[Response] = withBody(body).execute("POST")

  /**
   * Perform a PUT on the request asynchronously.
   */
  override def put(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Future[Response] = withBody(body).execute("PUT")
  override def put[T: BodyWritable](body: T): Future[Response] = withBody(body).execute("PUT")
  override def put(body: java.io.File): Future[Response] = withBody(body).execute("PUT")

  def stream(): Future[StreamedResponse] = underlying.stream()

  def execute(method: String): Future[Response] = {
    withMethod(method).execute()
  }

  override def execute(): Future[Response] = {
    implicit val ec = underlying.client.executionContext
    require(ec != null)

    val futureResponse: Future[StandaloneWSResponse] = underlying.execute()
    futureResponse.map { f =>
      AhcWSResponse(f.asInstanceOf[StandaloneAhcWSResponse])
    }
  }

  private def toWSRequest(request: StandaloneWSRequest): Self = {
    AhcWSRequest(request.asInstanceOf[StandaloneAhcWSRequest])
  }

}
