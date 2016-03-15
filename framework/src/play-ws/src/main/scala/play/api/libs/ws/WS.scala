/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.ws

import java.io.Closeable
import java.io.File
import java.net.URI
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.xml.Elem
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.Application
import play.api.http.Writeable
import play.api.libs.json.JsValue
import play.api.libs.iteratee._
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
 * Asynchronous API to to query web services, as an http client.
 *
 * Usage example:
 * {{{
 * class MyService @Inject() (ws: WSClient) {
 *   ws.url("http://example.com/feed").get()
 *   ws.url("http://example.com/item").post("content")
 * }
 * }}}
 *
 * When greater flexibility is needed, you can also create clients explicitly and pass them into WS:
 *
 * {{{
 * import play.api.libs.ws._
 * import play.api.libs.ws.ahc._
 * import akka.stream.Materializer
 * import play.api.ApplicationLifecycle
 * import javax.inject.Inject
 * import scala.concurrent.Future
 *
 * class MyService @Inject() (lifecycle: ApplicationLifecycle)(implicit mat: Materializer) {
 *   private val client = new AhcWSClient(new AhcConfigBuilder().build())
 *   client.url("http://example.com/feed").get()
 *   lifecycle.addStopHook(() =>
 *     // Make sure you close the client after use, otherwise you'll leak threads and connections
 *     client.close()
 *     Future.successful(())
 *   }
 * }
 * }}}
 *
 * Or call the client directly:
 *
 * {{{
 * import com.typesafe.config.ConfigFactory
 * import play.api.libs.ws._
 * import play.api.libs.ws.ahc._
 *
 * val configuration = play.api.Configuration(ConfigFactory.parseString(
 * """
 *   |play.ws.ssl.trustManager = ...
 * """.stripMargin))
 * val parser = new DefaultWSConfigParser(configuration, app.classloader)
 * val builder = new AhcConfigBuilder(parser.parse())
 * val secureClient: WSClient = new AhcWSClient(builder.build())
 * val response = secureClient.url("https://secure.com").get()
 * secureClient.close() // must explicitly manage lifecycle
 * }}}
 *
 * The value returned is a {@code Future[WSResponse]}, and you should use Play's asynchronous mechanisms to
 * use this response.
 */
@deprecated("Inject WSClient into your component", "2.5.0")
object WS {

  private val wsapiCache = Application.instanceCache[WSAPI]
  protected[play] def wsapi(implicit app: Application): WSAPI = wsapiCache(app)

  import scala.language.implicitConversions

  /**
   * Retrieves or creates underlying HTTP client.  Note that due to the Plugin architecture, an
   * implicit application must be in scope.  Most of the time you will want the current app:
   *
   * {{{
   * val client = WS.client(app)
   * }}}
   */
  @deprecated("Inject WSClient into your component", "2.5.0")
  def client(implicit app: Application): WSClient = wsapi.client

  /**
   * Prepares a new request using an implicit application.  This creates a default client, which you can then
   * use to construct a request.
   *
   * {{{
   *   WS.url("http://localhost/")(app).get()
   * }}}
   *
   * @param url the URL to request
   * @param app the implicit application to use.
   */
  @deprecated("Inject WSClient into your component", "2.5.0")
  def url(url: String)(implicit app: Application): play.api.libs.ws.WSRequest = wsapi(app).url(url)

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
  def url(magnet: WSRequestMagnet): play.api.libs.ws.WSRequest = magnet()

  /**
   * Prepares a new request using an implicit client.  The client must be in scope and configured, i.e.
   *
   * {{{
   * implicit val sslClient = new play.api.libs.ws.ahc.AhcWSClient(sslBuilder.build())
   * WS.clientUrl("http://example.com/feed")
   * }}}
   *
   * @param url the URL to request
   * @param client the client to use to make the request.
   */
  def clientUrl(url: String)(implicit client: WSClient): play.api.libs.ws.WSRequest = client.url(url)
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
   * Helper method for multipart body
   */
  private[libs] def withMultipartBody(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): WSRequest = {
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
   * performs a get
   * @param consumer that's handling the response
   */
  @deprecated("2.5.0", """Use WS.withMethod("GET").stream()""")
  def get[A](consumer: WSResponseHeaders => Iteratee[Array[Byte], A])(implicit ec: ExecutionContext): Future[Iteratee[Array[Byte], A]] = {
    getStream().flatMap {
      case (response, enumerator) =>
        enumerator(consumer(response))
    }
  }

  /**
   * performs a get
   */
  @deprecated("2.5.0", """Use WS.withMethod("GET").stream()""")
  def getStream(): Future[(WSResponseHeaders, Enumerator[Array[Byte]])] = {
    withMethod("GET").streamWithEnumerator()
  }

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
    withMethod("PATCH").withMultipartBody(body).execute()
  }

  /**
   * performs a POST with supplied body
   * @param consumer that's handling the response
   */
  @deprecated("2.5.0", """Use WS.withMethod("PATCH").stream()""")
  def patchAndRetrieveStream[A, T](body: T)(consumer: WSResponseHeaders => Iteratee[Array[Byte], A])(implicit wrt: Writeable[T], ec: ExecutionContext): Future[Iteratee[Array[Byte], A]] = {
    withMethod("PATCH").withBody(body).streamWithEnumerator().flatMap {
      case (response, enumerator) =>
        enumerator(consumer(response))
    }
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
    withMethod("POST").withMultipartBody(body).execute()
  }

  /**
   * performs a POST with supplied body
   * @param consumer that's handling the response
   */
  @deprecated("2.5.0", """Use WS.withMethod("POST").stream()""")
  def postAndRetrieveStream[A, T](body: T)(consumer: WSResponseHeaders => Iteratee[Array[Byte], A])(implicit wrt: Writeable[T], ec: ExecutionContext): Future[Iteratee[Array[Byte], A]] = {
    withMethod("POST").withBody(body).streamWithEnumerator().flatMap {
      case (response, enumerator) =>
        enumerator(consumer(response))
    }
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
    withMethod("PUT").withMultipartBody(body).execute()
  }

  /**
   * performs a PUT with supplied body
   * @param consumer that's handling the response
   */
  @deprecated("2.5.0", """Use WS.withMethod("PUT").stream()""")
  def putAndRetrieveStream[A, T](body: T)(consumer: WSResponseHeaders => Iteratee[Array[Byte], A])(implicit wrt: Writeable[T], ec: ExecutionContext): Future[Iteratee[Array[Byte], A]] = {
    withMethod("PUT").withBody(body).streamWithEnumerator().flatMap {
      case (response, enumerator) =>
        enumerator(consumer(response))
    }
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

  /**
   * Execute this request and stream the response body.
   * @note This method used to be named `stream`, but it was renamed because the method's signature was
   *       changed and the JVM doesn't allow overloading on the return type.
   */
  @deprecated("2.5.0", "Use `WS.stream()` instead.")
  def streamWithEnumerator(): Future[(WSResponseHeaders, Enumerator[Array[Byte]])]
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
