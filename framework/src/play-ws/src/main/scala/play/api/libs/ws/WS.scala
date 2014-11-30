/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.ws

import java.net.URI

import scala.concurrent.{ Future, ExecutionContext }

import java.io.File

import play.api.http.Writeable
import play.api.libs.iteratee._

import play.api._
import scala.xml.Elem
import play.api.libs.json.JsValue

/**
 * The WSClient holds the configuration information needed to build a request, and provides a way to get a request holder.
 */
trait WSClient {

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
  def url(url: String): WSRequestHolder
}

/**
 * WSRequestHolderMagnet magnet.  Please see the companion object for implicit definitions.
 *
 * @see <a href="http://spray.io/blog/2012-12-13-the-magnet-pattern/">The magnet pattern</a>
 */
trait WSRequestHolderMagnet {
  def apply(): WSRequestHolder
}

/**
 * Asynchronous API to to query web services, as an http client.
 *
 * Usage example:
 * {{{
 * WS.url("http://example.com/feed").get()
 * WS.url("http://example.com/item").post("content")
 * }}}
 *
 * When greater flexibility is needed, you can also create clients explicitly and pass them into WS:
 *
 * {{{
 * implicit val client = new NingWSClient(builder.build())
 * WS.url("http://example.com/feed").get()
 * }}}
 *
 * Or call the client directly:
 *
 * {{{
 * import com.typesafe.config.ConfigFactory
 * import play.api.libs.ws._
 * import play.api.libs.ws.ning._
 *
 * val configuration = play.api.Configuration(ConfigFactory.parseString(
 * """
 *   |ws.ssl.trustManager = ...
 * """.stripMargin))
 * val parser = new DefaultWSConfigParser(configuration, Play.current.classloader)
 * val builder = new NingAsyncHttpClientConfigBuilder(parser.parse())
 * val secureClient : WSClient = new NingWSClient(builder.build())
 * val response = secureClient.url("https://secure.com").get()
 * }}}
 *
 * Note that the resolution of URL is done through the magnet pattern defined in
 * `WSRequestHolderMagnet`.
 *
 * The value returned is a {@code Future[WSResponse]}, and you should use Play's asynchronous mechanisms to
 * use this response.
 */
object WS {

  @deprecated("Please use play.api.libs.ws.WSRequest", "2.3.0")
  type WSRequest = play.api.libs.ws.WSRequest

  @deprecated("Please use play.api.libs.ws.WSResponse", "2.3.0")
  type Response = play.api.libs.ws.WSResponse

  @deprecated("Please use play.api.libs.ws.WSRequestHolder", "2.3.0")
  type WSRequestHolder = play.api.libs.ws.WSRequestHolder

  private val wsapiCache = Application.instanceCache[WSAPI]
  protected[play] def wsapi(implicit app: Application): WSAPI = wsapiCache(app)

  import scala.language.implicitConversions

  /**
   * Retrieves or creates underlying HTTP client.  Note that due to the Plugin architecture, an
   * implicit application must be in scope.  Most of the time you will want the current app:
   *
   * {{{
   * import play.api.Play.current
   * val client = WS.client
   * }}}
   */
  def client(implicit app: Application): WSClient = wsapi.client

  /**
   * Prepares a new request using an implicit application.  This creates a default client, which you can then
   * use to construct a request.
   *
   * {{{
   *   import play.api.Play.current
   *   WS.url("http://localhost/").get()
   * }}}
   *
   * @param url the URL to request
   * @param app the implicit application to use.
   */
  def url(url: String)(implicit app: Application): play.api.libs.ws.WSRequestHolder = wsapi(app).url(url)

  /**
   * Prepares a new request using a provided magnet.  This method gives you the ability to create your own
   * URL implementations at the cost of some complexity.
   *
   * {{{
   * object PairMagnet {
   *   implicit def fromPair(pair: Pair[WSClient, java.net.URL]) =
   *     new WSRequestHolderMagnet {
   *       def apply(): WSRequestHolder = {
   *         val (client, netUrl) = pair
   *         client.url(netUrl.toString)
   *       }
   *    }
   * }
   * import scala.language.implicitConversions
   * val client = WS.client
   * val exampleURL = new java.net.URL("http://example.com")
   * WS.url(client -> exampleURL).get()
   * }}}
   *
   * @param magnet a magnet pattern.
   * @see <a href="http://spray.io/blog/2012-12-13-the-magnet-pattern/">The magnet pattern</a>
   */
  def url(magnet: WSRequestHolderMagnet): play.api.libs.ws.WSRequestHolder = magnet()

  /**
   * Prepares a new request using an implicit client.  The client must be in scope and configured, i.e.
   *
   * {{{
   * implicit val sslClient = new play.api.libs.ws.ning.NingWSClient(sslBuilder.build())
   * WS.clientUrl("http://example.com/feed")
   * }}}
   *
   * @param url the URL to request
   * @param client the client to use to make the request.
   */
  def clientUrl(url: String)(implicit client: WSClient): play.api.libs.ws.WSRequestHolder = client.url(url)
}

/**
 * The base WS API trait.  Plugins should extend this.
 */
trait WSAPI {

  def client: WSClient

  def url(url: String): WSRequestHolder
}

/**
 * WSRequest is used internally.  Please use WSRequestHolder.
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
   * Get the body.
   *
   * Will only return the body if the body exists in memory, will not return it if it's a stream.
   */
  def getBody: Option[Array[Byte]]

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
   * The response body as a byte array.
   */
  def bodyAsBytes: Array[Byte]
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
case class InMemoryBody(bytes: Array[Byte]) extends WSBody

/**
 * A streamed body
 *
 * @param bytes An enumerator of the bytes of the body
 */
case class StreamedBody(bytes: Enumerator[Array[Byte]]) extends WSBody {
  throw new NotImplementedError("A streaming request body is not yet implemented")
}

/**
 * A file body
 */
case class FileBody(file: File) extends WSBody

/**
 * An empty body
 */
case object EmptyBody extends WSBody

/**
 * A WS Request builder.
 */
trait WSRequestHolder {

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

  /**
   * Sets the maximum time in milliseconds you expect the request to take.
   * Warning: a stream consumption will be interrupted when this time is reached.
   */
  def withRequestTimeout(timeout: Long): WSRequestHolder

  /**
   * Sets the virtual host to use in this request
   */
  def withVirtualHost(vh: String): WSRequestHolder

  /**
   * Sets the proxy server to use in this request
   */
  def withProxyServer(proxyServer: WSProxyServer): WSRequestHolder

  /**
   * Sets the body for this request
   */
  def withBody(body: WSBody): WSRequestHolder

  /**
   * Sets the body for this request
   */
  def withBody[T](body: T)(implicit wrt: Writeable[T]): WSRequestHolder = {
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
   * Sets the method for this request
   */
  def withMethod(method: String): WSRequestHolder

  /**
   * performs a get
   */
  def get() = withMethod("GET").execute()

  /**
   * performs a get
   * @param consumer that's handling the response
   */
  def get[A](consumer: WSResponseHeaders => Iteratee[Array[Byte], A])(implicit ec: ExecutionContext): Future[Iteratee[Array[Byte], A]] = {
    getStream().flatMap {
      case (response, enumerator) =>
        enumerator(consumer(response))
    }
  }

  /**
   * performs a get
   */
  def getStream(): Future[(WSResponseHeaders, Enumerator[Array[Byte]])] = {
    withMethod("GET").stream()
  }

  /**
   * Perform a PATCH on the request asynchronously.
   */
  def patch[T](body: T)(implicit wrt: Writeable[T]) =
    withMethod("PATCH").withBody(body).execute()

  /**
   * Perform a PATCH on the request asynchronously.
   * Request body won't be chunked
   */
  def patch(body: File) = withMethod("PATCH").withBody(FileBody(body)).execute()

  /**
   * performs a POST with supplied body
   * @param consumer that's handling the response
   */
  def patchAndRetrieveStream[A, T](body: T)(consumer: WSResponseHeaders => Iteratee[Array[Byte], A])(implicit wrt: Writeable[T], ec: ExecutionContext): Future[Iteratee[Array[Byte], A]] = {
    withMethod("PATCH").withBody(body).stream().flatMap {
      case (response, enumerator) =>
        enumerator(consumer(response))
    }
  }

  /**
   * Perform a POST on the request asynchronously.
   */
  def post[T](body: T)(implicit wrt: Writeable[T]) =
    withMethod("POST").withBody(body).execute()

  /**
   * Perform a POST on the request asynchronously.
   * Request body won't be chunked
   */
  def post(body: File) = withMethod("POST").withBody(FileBody(body)).execute()

  /**
   * performs a POST with supplied body
   * @param consumer that's handling the response
   */
  def postAndRetrieveStream[A, T](body: T)(consumer: WSResponseHeaders => Iteratee[Array[Byte], A])(implicit wrt: Writeable[T], ec: ExecutionContext): Future[Iteratee[Array[Byte], A]] = {
    withMethod("POST").withBody(body).stream().flatMap {
      case (response, enumerator) =>
        enumerator(consumer(response))
    }
  }

  /**
   * Perform a PUT on the request asynchronously.
   */
  def put[T](body: T)(implicit wrt: Writeable[T]) =
    withMethod("PUT").withBody(body).execute()

  /**
   * Perform a PUT on the request asynchronously.
   * Request body won't be chunked
   */
  def put(body: File) = withMethod("PUT").withBody(FileBody(body)).execute()

  /**
   * performs a PUT with supplied body
   * @param consumer that's handling the response
   */
  def putAndRetrieveStream[A, T](body: T)(consumer: WSResponseHeaders => Iteratee[Array[Byte], A])(implicit wrt: Writeable[T], ec: ExecutionContext): Future[Iteratee[Array[Byte], A]] = {
    withMethod("PUT").withBody(body).stream().flatMap {
      case (response, enumerator) =>
        enumerator(consumer(response))
    }
  }

  /**
   * Perform a DELETE on the request asynchronously.
   */
  def delete() = withMethod("DELETE").execute()

  /**
   * Perform a HEAD on the request asynchronously.
   */
  def head() = withMethod("HEAD").execute()

  /**
   * Perform a OPTIONS on the request asynchronously.
   */
  def options() = withMethod("OPTIONS").execute()

  def execute(method: String): Future[WSResponse] = withMethod(method).execute()

  /**
   * Execute this request
   */
  def execute(): Future[WSResponse]

  /**
   * Execute this request and stream the response body in an enumerator
   */
  def stream(): Future[(WSResponseHeaders, Enumerator[Array[Byte]])]
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
   * The expiry date.
   */
  def expires: Option[Long]

  /**
   * The maximum age.
   */
  def maxAge: Option[Int]

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

