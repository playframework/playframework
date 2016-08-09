/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.ws

import java.io.Closeable
import java.io.File
import java.net.URI
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.xml.Elem
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.Application
import play.api.http.Writeable
import play.api.libs.json.JsValue
import play.core.formatters.Multipart
import play.api.mvc.MultipartFormData
import java.io.IOException

/**
 * The WSClient holds the configuration information needed to build a request, and provides a way to get a request holder.
 */
trait WSClient extends Closeable {

  /**
   * The underlying implementation of the client, if any.  You must cast explicitly to the type you want.
   * @tparam T the type you are expecting (i.e. isInstanceOf)
   * @return the backing class.
   */
  def underlying[T]: T

  /**
   * Generates a request holder which can be used to build requests.
   *
   * @param url The base URL to make HTTP requests to.
   * @return a WSRequestHolder
   */
  def url(url: String): WSRequest

  /** Closes this client, and releases underlying resources. */
  @throws[IOException] def close(): Unit
}

/**
 * WSRequestMagnet magnet.  Please see the companion object for implicit definitions.
 *
 * @see <a href="http://spray.io/blog/2012-12-13-the-magnet-pattern/">The magnet pattern</a>
 */
trait WSRequestMagnet {
  def apply(): WSRequest
}

trait WSRequestExecutor {
  def execute(request: WSRequest): Future[WSResponse]
}

trait WSRequestFilter {
  def apply(next: WSRequestExecutor): WSRequestExecutor
}

/**
 * The base WS API trait.
 */
trait WSAPI {

  def client: WSClient

  def url(url: String): WSRequest
}

/**
 *
 */
trait WSResponse {

  /**
   * Return the current headers of the request being constructed
   */
  def allHeaders: Map[String, Seq[String]]

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

  /**
   * The response body as a byte string.
   */
  def bodyAsBytes: ByteString
}

/**
 * A body for the request
 */
sealed trait WSBody

/**
 * An in memory body
 *
 * @param bytes The bytes of the body
 */
case class InMemoryBody(bytes: ByteString) extends WSBody

/**
 * A streamed body
 *
 * @param bytes A flow of the bytes of the body
 */
case class StreamedBody(bytes: Source[ByteString, _]) extends WSBody

/**
 * A file body
 */
case class FileBody(file: File) extends WSBody

/**
 * An empty body
 */
case object EmptyBody extends WSBody

/**
 * A streamed response containing a response header and a streamable body.
 */
case class StreamedResponse(headers: WSResponseHeaders, body: Source[ByteString, _])

/**
 * A WS Request builder.
 */
trait WSRequest {

  /**
   * The base URL for this request
   */
  val url: String

  /**
   * The URI for this request
   */
  lazy val uri: URI = {
    val enc = (p: String) => java.net.URLEncoder.encode(p, "utf-8")
    new java.net.URI(if (queryString.isEmpty) url else {
      val qs = (for {
        (n, vs) <- queryString
        v <- vs
      } yield s"${enc(n)}=${enc(v)}").mkString("&")
      s"$url?$qs"
    })
  }

  /**
   * The method for this request
   */
  val method: String

  /**
   * The body of this request
   */
  val body: WSBody

  /**
   * The headers for this request
   */
  val headers: Map[String, Seq[String]]

  /**
   * The query string for this request
   */
  val queryString: Map[String, Seq[String]]

  /**
   * A calculator of the signature for this request
   */
  val calc: Option[WSSignatureCalculator]

  /**
   * The authentication this request should use
   */
  val auth: Option[(String, String, WSAuthScheme)]

  /**
   * Whether this request should follow redirects
   */
  val followRedirects: Option[Boolean]

  /**
   * The timeout for the request
   */
  val requestTimeout: Option[Int]

  /**
   * The virtual host this request will use
   */
  val virtualHost: Option[String]

  /**
   * The proxy server this request will use
   */
  val proxyServer: Option[WSProxyServer]

  /**
   * sets the signature calculator for the request
   * @param calc
   */
  def sign(calc: WSSignatureCalculator): WSRequest

  /**
   * sets the authentication realm
   */
  def withAuth(username: String, password: String, scheme: WSAuthScheme): WSRequest

  /**
   * adds any number of HTTP headers
   * @param hdrs
   */
  def withHeaders(hdrs: (String, String)*): WSRequest

  /**
   * adds any number of query string parameters to this request
   */
  def withQueryString(parameters: (String, String)*): WSRequest

  /**
   * Sets whether redirects (301, 302) should be followed automatically
   */
  def withFollowRedirects(follow: Boolean): WSRequest

  /**
   * Sets the maximum time you expect the request to take.
   * Use Duration.Inf to set an infinite request timeout.
   * Warning: a stream consumption will be interrupted when this time is reached unless Duration.Inf is set.
   */
  def withRequestTimeout(timeout: Duration): WSRequest

  /**
   * Adds a filter to the request that can transform the request for subsequent filters.
   */
  def withRequestFilter(filter: WSRequestFilter): WSRequest

  /**
   * Sets the virtual host to use in this request
   */
  def withVirtualHost(vh: String): WSRequest

  /**
   * Sets the proxy server to use in this request
   */
  def withProxyServer(proxyServer: WSProxyServer): WSRequest

  /**
   * Sets the body for this request
   */
  def withBody(body: WSBody): WSRequest

  /**
   * Sets the body for this request
   */
  def withBody[T](body: T)(implicit wrt: Writeable[T]): WSRequest = {
    val wsBody = InMemoryBody(wrt.transform(body))
    if (headers.contains("Content-Type")) {
      withBody(wsBody)
    } else {
      wrt.contentType.fold(withBody(wsBody)) { contentType =>
        withBody(wsBody).withHeaders("Content-Type" -> contentType)
      }
    }
  }

  /**
   * Sets a multipart body for this request
   */
  def withBody(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): WSRequest = {
    val boundary = Multipart.randomBoundary()
    val contentType = s"multipart/form-data; boundary=$boundary"
    withBody(StreamedBody(Multipart.transform(body, boundary))).withHeaders("Content-Type" -> contentType)
  }

  /**
   * Sets the method for this request
   */
  def withMethod(method: String): WSRequest

  /**
   * performs a get
   */
  def get(): Future[WSResponse] = withMethod("GET").execute()

  /**
   * Perform a PATCH on the request asynchronously.
   */
  def patch[T](body: T)(implicit wrt: Writeable[T]): Future[WSResponse] =
    withMethod("PATCH").withBody(body).execute()

  /**
   * Perform a PATCH on the request asynchronously.
   * Request body won't be chunked
   */
  def patch(body: File): Future[WSResponse] = withMethod("PATCH").withBody(FileBody(body)).execute()

  /**
   * Perform a PATCH on the request asynchronously.
   */
  def patch(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Future[WSResponse] = {
    withMethod("PATCH").withBody(body).execute()
  }

  /**
   * Perform a POST on the request asynchronously.
   */
  def post[T](body: T)(implicit wrt: Writeable[T]): Future[WSResponse] =
    withMethod("POST").withBody(body).execute()

  /**
   * Perform a POST on the request asynchronously.
   * Request body won't be chunked
   */
  def post(body: File): Future[WSResponse] = withMethod("POST").withBody(FileBody(body)).execute()

  /**
   * Perform a POST on the request asynchronously.
   */
  def post(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Future[WSResponse] = {
    withMethod("POST").withBody(body).execute()
  }

  /**
   * Perform a PUT on the request asynchronously.
   */
  def put[T](body: T)(implicit wrt: Writeable[T]): Future[WSResponse] =
    withMethod("PUT").withBody(body).execute()

  /**
   * Perform a PUT on the request asynchronously.
   * Request body won't be chunked
   */
  def put(body: File): Future[WSResponse] = withMethod("PUT").withBody(FileBody(body)).execute()

  /**
   * Perform a PUT on the request asynchronously.
   */
  def put(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Future[WSResponse] = {
    withMethod("PUT").withBody(body).execute()
  }

  /**
   * Perform a DELETE on the request asynchronously.
   */
  def delete(): Future[WSResponse] = withMethod("DELETE").execute()

  /**
   * Perform a HEAD on the request asynchronously.
   */
  def head(): Future[WSResponse] = withMethod("HEAD").execute()

  /**
   * Perform a OPTIONS on the request asynchronously.
   */
  def options(): Future[WSResponse] = withMethod("OPTIONS").execute()

  def execute(method: String): Future[WSResponse] = withMethod(method).execute()

  /**
   * Execute this request
   */
  def execute(): Future[WSResponse]

  /**
   * Execute this request and stream the response body.
   */
  def stream(): Future[StreamedResponse]
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
  def maxAge: Option[Long]

  /**
   * If the cookie is secure.
   */
  def secure: Boolean
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

  /** The realm's charset. */
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

  /** The realm's charset. */
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
 * Sign a WS call with OAuth.
 */
trait WSSignatureCalculator
