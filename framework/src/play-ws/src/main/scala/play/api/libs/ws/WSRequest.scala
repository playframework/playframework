/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.ws

import java.io.File

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.mvc.{ Cookie, MultipartFormData }

import scala.concurrent.duration.Duration
import scala.concurrent.Future

/**
 * A WS Request builder.
 */
trait WSRequest extends StandaloneWSRequest with WSBodyWritables {
  override type Self = WSRequest
  override type Response = WSResponse

  /**
   * Returns this request with the given headers, adding to the existing ones.
   *
   * @param headers the headers to be used
   */
  @deprecated("Use withHttpHeaders or addHttpHeaders", "2.6.0")
  def withHeaders(headers: (String, String)*): Self

  /**
   * Returns this request with the given headers, discarding the existing ones.
   *
   * @param headers the headers to be used
   */
  override def withHttpHeaders(headers: (String, String)*): Self

  /**
   * Returns this request with the given headers, preserving the existing ones.
   *
   * @param hdrs the headers to be added
   */
  override def addHttpHeaders(hdrs: (String, String)*): Self

  /**
   * Get the value of the header with the specified name. If there are more than one values
   * for this header, the first value is returned. If there are no values, than a None is
   * returned.
   *
   * @param name the header name
   * @return the header value
   */
  override def header(name: String): Option[String]

  /**
   * Get all the values of header with the specified name. If there are no values for
   * the header with the specified name, than an empty sequence is returned.
   *
   * @param name the header name.
   * @return all the values for this header name.
   */
  override def headerValues(name: String): Seq[String]

  /**
   * Returns this request with the given query string parameters, adding to the existing ones.
   *
   * @param parameters the query string parameters
   */
  @deprecated("Use addQueryStringParameters or withQueryStringParameters", "2.6.0")
  def withQueryString(parameters: (String, String)*): Self

  /**
   * Returns this request with the given query string parameters, discarding the existing ones.
   *
   * @param parameters the query string parameters
   */
  override def withQueryStringParameters(parameters: (String, String)*): Self

  /**
   * Returns this request with the given query string parameters, preserving the existing ones.
   *
   * @param parameters the query string parameters
   */
  override def addQueryStringParameters(parameters: (String, String)*): Self

  /**
   * Returns this request with the given cookies, preserving the existing ones.
   *
   * @param cookies the cookies to be used
   */
  override def addCookies(cookies: WSCookie*): Self

  /**
   * Returns this request with the given cookies, discarding the existing ones.
   *
   * @param cookie the cookies to be used
   */
  override def withCookies(cookie: WSCookie*): Self

  /**
   * The method for this request
   */
  override def method: String

  /**
   * The body of this request
   */
  override def body: WSBody

  /**
   * The headers for this request
   */
  override def headers: Map[String, Seq[String]]

  /**
   * The query string for this request
   */
  override def queryString: Map[String, Seq[String]]

  /**
   * A calculator of the signature for this request
   */
  override def calc: Option[WSSignatureCalculator]

  /**
   * The authentication this request should use
   */
  override def auth: Option[(String, String, WSAuthScheme)]

  /**
   * Whether this request should follow redirects
   */
  override def followRedirects: Option[Boolean]

  /**
   * The timeout for the request
   */
  override def requestTimeout: Option[Duration]

  /**
   * The virtual host this request will use
   */
  override def virtualHost: Option[String]

  /**
   * The proxy server this request will use
   */
  override def proxyServer: Option[WSProxyServer]

  /**
   * sets the signature calculator for the request
   * @param calc
   */
  override def sign(calc: WSSignatureCalculator): Self

  /**
   * sets the authentication realm
   */
  override def withAuth(username: String, password: String, scheme: WSAuthScheme): Self

  /**
   * Sets whether redirects (301, 302) should be followed automatically
   */
  override def withFollowRedirects(follow: Boolean): Self

  /**
   * Sets the maximum time you expect the request to take.
   * Use Duration.Inf to set an infinite request timeout.
   * Warning: a stream consumption will be interrupted when this time is reached unless Duration.Inf is set.
   */
  override def withRequestTimeout(timeout: Duration): Self

  /**
   * Adds a filter to the request that can transform the request for subsequent filters.
   */
  override def withRequestFilter(filter: WSRequestFilter): Self

  /**
   * Sets the virtual host to use in this request
   */
  override def withVirtualHost(vh: String): Self

  /**
   * Sets the proxy server to use in this request
   */
  override def withProxyServer(proxyServer: WSProxyServer): Self

  /**
   * Sets the body for this request
   */
  override def withBody[T: BodyWritable](body: T): Self

  /**
   * Sets the method for this request
   */
  override def withMethod(method: String): Self

  //------------------------------------------------
  // GET
  //------------------------------------------------

  /**
   * performs a get
   */
  override def get(): Future[Response]

  //------------------------------------------------
  // POST
  //------------------------------------------------

  /**
   * Performs a POST request.
   *
   * @param body the payload wsBody submitted with this request
   * @return a future with the response for the POST request
   */
  override def post[T: BodyWritable](body: T): Future[Response]

  /**
   * Perform a POST on the request asynchronously.
   * Request body won't be chunked
   */
  def post(body: File): Future[Response]

  /**
   * Perform a POST on the request asynchronously.
   */
  def post(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Future[Response]

  //------------------------------------------------
  // PATCH
  //------------------------------------------------

  /**
   * Performs a PATCH request.
   *
   * @param body the payload wsBody submitted with this request
   * @return a future with the response for the PATCH request
   */
  override def patch[T: BodyWritable](body: T): Future[Response]

  /**
   * Perform a PATCH on the request asynchronously.
   * Request body won't be chunked
   */
  def patch(body: File): Future[Response]

  /**
   * Perform a PATCH on the request asynchronously.
   */
  def patch(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Future[Response]

  //------------------------------------------------
  // PUT
  //------------------------------------------------

  /**
   * Performs a PUT request.
   *
   * @param body the payload wsBody submitted with this request
   * @return a future with the response for the PUT request
   */
  override def put[T: BodyWritable](body: T): Future[Response]

  /**
   * Perform a PUT on the request asynchronously.
   * Request body won't be chunked
   */
  def put(body: File): Future[Response]

  /**
   * Perform a PUT on the request asynchronously.
   */
  def put(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Future[Response]

  //------------------------------------------------
  // DELETE, HEAD, OPTIONS
  //------------------------------------------------

  /**
   * Perform a DELETE on the request asynchronously.
   */
  override def delete(): Future[Response]

  /**
   * Perform a HEAD on the request asynchronously.
   */
  override def head(): Future[Response]

  /**
   * Perform a OPTIONS on the request asynchronously.
   */
  override def options(): Future[Response]

  //------------------------------------------------
  // Generic execution
  //------------------------------------------------

  /**
   * Executes the given HTTP method.
   * @param method the HTTP method that will be executed
   * @return a future with the response for this request
   */
  override def execute(method: String): Future[Response]

  /**
   * Execute this request
   */
  override def execute(): Future[Response]

}
