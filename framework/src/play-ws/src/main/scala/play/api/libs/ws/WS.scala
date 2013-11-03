/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.ws

import scala.concurrent.{ Future, ExecutionContext }

import java.io.File

import play.api.http.{ Writeable, ContentTypeOf }
import play.api.libs.iteratee._

import play.api.{ Play, Application, Plugin }
import scala.xml.Elem
import play.api.libs.json.JsValue

/**
 * Asynchronous API to to query web services, as an http client.
 *
 * Usage example:
 * {{{
 * WS.url("http://example.com/feed").get()
 * WS.url("http://example.com/item").post("content")
 * }}}
 *
 * The value returned is a Future[Response],
 * and you should use Play's asynchronous mechanisms to use this response.
 *
 */
object WS {

  @deprecated("Please use play.api.libs.ws.WSRequest", "2.3.0")
  type WSRequest = play.api.libs.ws.WSRequest

  @deprecated("Please use play.api.libs.ws.WSResponse", "2.3.0")
  type Response = play.api.libs.ws.WSResponse

  @deprecated("Please use play.api.libs.ws.WSRequestHolder", "2.3.0")
  type WSRequestHolder = play.api.libs.ws.WSRequestHolder

  protected[play] def wsapi(implicit app: Application): WSAPI = {
    app.plugin[WSPlugin] match {
      case Some(plugin) => plugin.api
      case None => throw new Exception("There is no WS plugin registered.")
    }
  }

  /**
   * retrieves or creates underlying HTTP client.
   */
  def client(implicit app: Application): WSClient = wsapi.client

  /**
   * Prepare a new request. You can then construct it by chaining calls.
   *
   * @param url the URL to request
   */
  def url(url: String)(implicit app: Application): play.api.libs.ws.WSRequestHolder = wsapi.url(url)
}

/**
 *
 */
trait WSRequest {

  /**
   * Return the current headers of the request being constructed
   */
  def allHeaders: Map[String, Seq[String]]

  /**
   * Return the current query string parameters
   */
  def queryString: Map[String, Seq[String]]

  /**
   * Retrieve an HTTP header.
   */
  def header(name: String): Option[String]

  /**
   * The HTTP method.
   */
  def method: String

  /**
   * The URL
   */
  def url: String

  /**
   * The body of the request as a string.
   */
  def getStringData: String

  /**
   * Set an HTTP header.
   */
  @scala.deprecated("Use WSRequestHolder", "2.3.0")
  def setHeader(name: String, value: String): WSRequest

  /**
   * Add an HTTP header (used for headers with multiple values).
   */
  @scala.deprecated("Use WSRequestHolder", "2.3.0")
  def addHeader(name: String, value: String): WSRequest

  //@scala.deprecated
  //override def setHeaders(hdrs: FluentCaseInsensitiveStringsMap)

  /**
   * Defines the request headers.
   */
  @scala.deprecated("Use WSRequestHolder", "2.3.0")
  def setHeaders(hdrs: java.util.Map[String, java.util.Collection[String]]): WSRequest

  /**
   * Defines the request headers.
   */
  @scala.deprecated("Use WSRequestHolder", "2.3.0")
  def setHeaders(hdrs: Map[String, Seq[String]]): WSRequest

  /**
   * Defines the query string.
   */
  @scala.deprecated("Use WSRequestHolder", "2.3.0")
  def setQueryString(queryString: Map[String, Seq[String]]): WSRequest

  @scala.deprecated("Use WSRequestHolder", "2.3.0")
  def setUrl(url: String): WSRequest
}

/**
 *
 */
trait WSResponse {

  /**
   * Get the underlying response object.
   */
  def underlying[T]: T

  /**
   * The response status code.
   */
  def status: Int

  /**
   * The response status message.
   */
  def statusText: String

  /**
   * Get a response header.
   */
  def header(key: String): Option[String]

  /**
   * Get all the cookies.
   */
  def cookies: Seq[WSCookie]

  /**
   * Get only one cookie, using the cookie name.
   */
  def cookie(name: String): Option[WSCookie]

  /**
   * The response body as String.
   */
  def body: String

  /**
   * The response body as Xml.
   */
  def xml: Elem

  /**
   * The response body as Json.
   */
  def json: JsValue

}

/**
 * A WS Request builder.
 */
trait WSRequestHolder {

  val url: String

  val headers: Map[String, Seq[String]]

  val queryString: Map[String, Seq[String]]

  val calc: Option[WSSignatureCalculator]

  val auth: Option[(String, String, WSAuthScheme)]

  val followRedirects: Option[Boolean]

  val requestTimeout: Option[Int]

  val virtualHost: Option[String]

  val proxyServer: Option[WSProxyServer]

  /**
   * sets the signature calculator for the request
   * @param calc
   */
  def sign(calc: WSSignatureCalculator): WSRequestHolder

  /**
   * sets the authentication realm
   */
  def withAuth(username: String, password: String, scheme: WSAuthScheme): WSRequestHolder

  /**
   * adds any number of HTTP headers
   * @param hdrs
   */
  def withHeaders(hdrs: (String, String)*): WSRequestHolder

  /**
   * adds any number of query string parameters to the
   */
  def withQueryString(parameters: (String, String)*): WSRequestHolder

  /**
   * Sets whether redirects (301, 302) should be followed automatically
   */
  def withFollowRedirects(follow: Boolean): WSRequestHolder

  @scala.deprecated("use withRequestTimeout instead", "2.1.0")
  def withTimeout(timeout: Int): WSRequestHolder

  /**
   * Sets the maximum time in millisecond you accept the request to take.
   * Warning: a stream consumption will be interrupted when this time is reached.
   */
  def withRequestTimeout(timeout: Int): WSRequestHolder

  def withVirtualHost(vh: String): WSRequestHolder

  def withProxyServer(proxyServer: WSProxyServer): WSRequestHolder

  /**
   * performs a get with supplied body
   */

  def get(): Future[WSResponse]

  /**
   * performs a get with supplied body
   * @param consumer that's handling the response
   */
  def get[A](consumer: WSResponseHeaders => Iteratee[Array[Byte], A])(implicit ec: ExecutionContext): Future[Iteratee[Array[Byte], A]]

  /**
   * Perform a PATCH on the request asynchronously.
   */
  def patch[T](body: T)(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): Future[WSResponse]

  /**
   * Perform a PATCH on the request asynchronously.
   * Request body won't be chunked
   */
  def patch(body: File): Future[WSResponse]

  /**
   * performs a POST with supplied body
   * @param consumer that's handling the response
   */
  def patchAndRetrieveStream[A, T](body: T)(consumer: WSResponseHeaders => Iteratee[Array[Byte], A])(implicit wrt: Writeable[T], ct: ContentTypeOf[T], ec: ExecutionContext): Future[Iteratee[Array[Byte], A]]

  /**
   * Perform a POST on the request asynchronously.
   */
  def post[T](body: T)(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): Future[WSResponse]

  /**
   * Perform a POST on the request asynchronously.
   * Request body won't be chunked
   */
  def post(body: File): Future[WSResponse]

  /**
   * performs a POST with supplied body
   * @param consumer that's handling the response
   */
  def postAndRetrieveStream[A, T](body: T)(consumer: WSResponseHeaders => Iteratee[Array[Byte], A])(implicit wrt: Writeable[T], ct: ContentTypeOf[T], ec: ExecutionContext): Future[Iteratee[Array[Byte], A]]

  /**
   * Perform a PUT on the request asynchronously.
   */
  def put[T](body: T)(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): Future[WSResponse]

  /**
   * Perform a PUT on the request asynchronously.
   * Request body won't be chunked
   */
  def put(body: File): Future[WSResponse]

  /**
   * performs a PUT with supplied body
   * @param consumer that's handling the response
   */
  def putAndRetrieveStream[A, T](body: T)(consumer: WSResponseHeaders => Iteratee[Array[Byte], A])(implicit wrt: Writeable[T], ct: ContentTypeOf[T], ec: ExecutionContext): Future[Iteratee[Array[Byte], A]]

  /**
   * Perform a DELETE on the request asynchronously.
   */
  def delete(): Future[WSResponse]

  /**
   * Perform a HEAD on the request asynchronously.
   */
  def head(): Future[WSResponse]

  /**
   * Perform a OPTIONS on the request asynchronously.
   */
  def options(): Future[WSResponse]

  def execute(method: String): Future[WSResponse]
}

/**
 *
 */
trait WSAuthScheme {
  // Purposely not sealed in case clients want to add their own auth schemes.
}

object WSAuthScheme {

  case object DIGEST extends WSAuthScheme

  case object BASIC extends WSAuthScheme

  case object NTLM extends WSAuthScheme

  case object SPNEGO extends WSAuthScheme

  case object KERBEROS extends WSAuthScheme

  case object NONE extends WSAuthScheme

}

/**
 * A WS Cookie.  This is a trait so that we are not tied to a specific client.
 */
trait WSCookie {

  /**
   * The underlying "native" cookie object for the client.
   */
  def underlying[T]: T

  /**
   * The domain.
   */
  def domain: String

  /**
   * The cookie name.
   */
  def name: Option[String]

  /**
   * The cookie value.
   */
  def value: Option[String]

  /**
   * The path.
   */
  def path: String

  /**
   * The maximum age.
   */
  def maxAge: Int

  /**
   * If the cookie is secure.
   */
  def secure: Boolean

  /**
   * The cookie version.
   */
  def version: Int
}

/**
 * A WS proxy.
 */
trait WSProxyServer {
  /** The hostname of the proxy server. */
  def host: String

  /** The port of the proxy server. */
  def port: Int

  /** The protocol of the proxy server.  Use "http" or "https".  Defaults to "http" if not specified. */
  def protocol: Option[String]

  /** The principal (aka username) of the credentials for the proxy server. */
  def principal: Option[String]

  /** The password for the credentials for the proxy server. */
  def password: Option[String]

  def ntlmDomain: Option[String]

  def encoding: Option[String]

  def nonProxyHosts: Option[Seq[String]]
}

/**
 * A WS proxy.
 */
case class DefaultWSProxyServer(
  /** The hostname of the proxy server. */
  host: String,

  /** The port of the proxy server. */
  port: Int,

  /** The protocol of the proxy server.  Use "http" or "https".  Defaults to "http" if not specified. */
  protocol: Option[String] = None,

  /** The principal (aka username) of the credentials for the proxy server. */
  principal: Option[String] = None,

  /** The password for the credentials for the proxy server. */
  password: Option[String] = None,

  ntlmDomain: Option[String] = None,

  encoding: Option[String] = None,

  nonProxyHosts: Option[Seq[String]] = None) extends WSProxyServer

/**
 * An HTTP response header (the body has not been retrieved yet)
 */
trait WSResponseHeaders {

  def status: Int

  def headers: Map[String, Seq[String]]
}

case class DefaultWSResponseHeaders(status: Int, headers: Map[String, Seq[String]]) extends WSResponseHeaders

/**
 * Sign a WS call.
 */
trait WSSignatureCalculator {

  /**
   * Sign it.
   */
  def sign(request: WSRequest)

}

/**
 *
 */
abstract class WSPlugin extends Plugin {

  def api: WSAPI

  private[ws] def loaded: Boolean
}

/**
 *
 */
trait WSClient {

  def underlying[T]: T

  def url(url: String): WSRequestHolder

}

/**
 *
 */
trait WSAPI {

  def client: WSClient

  def url(url: String): WSRequestHolder

}

