/*
  * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.ws.ahc

import akka.stream.Materializer
import akka.util.ByteString
import org.asynchttpclient.{ Response => AHCResponse, _ }
import org.asynchttpclient.proxy.{ ProxyServer => AHCProxyServer }
import org.asynchttpclient.Realm
import org.asynchttpclient.cookie.{ Cookie => AHCCookie }
import org.asynchttpclient.util.HttpUtils
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.charset.{ Charset, StandardCharsets }
import javax.inject.{ Inject, Provider, Singleton }
import io.netty.handler.codec.http.HttpHeaders
import play.api._
import play.api.inject.{ ApplicationLifecycle, Module }
import play.api.libs.iteratee.Enumerator
import play.api.libs.ws._
import play.api.libs.ws.ssl._
import play.api.libs.ws.ssl.debug._
import play.core.parsers.FormUrlEncodedParser
import play.core.utils.CaseInsensitiveOrdered
import scala.collection.JavaConverters._
import scala.collection.immutable.TreeMap
import scala.concurrent.{ Future, Promise }
import scala.concurrent.duration.Duration
import akka.stream.scaladsl.Sink

/**
 * A WS client backed by an AsyncHttpClient.
 *
 * If you need to debug AsyncHttpClient, set logger.org.asynchttpclient=DEBUG in your application.conf file.
 *
 * @param config a client configuration object
 */
case class AhcWSClient(config: AsyncHttpClientConfig)(implicit materializer: Materializer) extends WSClient {

  private val asyncHttpClient = new DefaultAsyncHttpClient(config)

  def underlying[T]: T = asyncHttpClient.asInstanceOf[T]

  private[libs] def executeRequest[T](request: Request, handler: AsyncHandler[T]): ListenableFuture[T] = asyncHttpClient.executeRequest(request, handler)

  def close(): Unit = asyncHttpClient.close()

  def url(url: String): WSRequest = AhcWSRequest(this, url, "GET", EmptyBody, Map(), Map(), None, None, None, None, None, None, None)
}

object AhcWSClient {
  /**
   * Convenient factory method that uses a [[WSClientConfig]] value for configuration instead of
   * an [[http://static.javadoc.io/org.asynchttpclient/async-http-client/2.0.0/org/asynchttpclient/AsyncHttpClientConfig.html org.asynchttpclient.AsyncHttpClientConfig]].
   *
   * Typical usage:
   *
   * {{{
   *   val client = AhcWSClient()
   *   val request = client.url(someUrl).get()
   *   request.foreach { response =>
   *     doSomething(response)
   *     client.close()
   *   }
   * }}}
   *
   * @param config configuration settings
   */
  def apply(config: AhcWSClientConfig = AhcWSClientConfig())(implicit materializer: Materializer): AhcWSClient = {
    val client = new AhcWSClient(new AhcConfigBuilder(config).build())
    new SystemConfiguration().configure(config.wsClientConfig)
    client
  }
}

case object AhcWSRequest {
  private[libs] def ahcHeadersToMap(headers: HttpHeaders): TreeMap[String, Seq[String]] = {
    val mutableMap = scala.collection.mutable.HashMap[String, Seq[String]]()
    headers.names().asScala.foreach { name =>
      mutableMap.put(name, headers.getAll(name).asScala)
    }
    TreeMap[String, Seq[String]]()(CaseInsensitiveOrdered) ++ mutableMap
  }
}

/**
 * A Ahc WS Request.
 */
case class AhcWSRequest(client: AhcWSClient,
    url: String,
    method: String,
    body: WSBody,
    headers: Map[String, Seq[String]],
    queryString: Map[String, Seq[String]],
    calc: Option[WSSignatureCalculator],
    auth: Option[(String, String, WSAuthScheme)],
    followRedirects: Option[Boolean],
    requestTimeout: Option[Int],
    virtualHost: Option[String],
    proxyServer: Option[WSProxyServer],
    disableUrlEncoding: Option[Boolean],
    filters: Seq[WSRequestFilter] = Nil)(implicit materializer: Materializer) extends WSRequest {

  def sign(calc: WSSignatureCalculator): WSRequest = copy(calc = Some(calc))

  def withAuth(username: String, password: String, scheme: WSAuthScheme): WSRequest =
    copy(auth = Some((username, password, scheme)))

  def withHeaders(hdrs: (String, String)*): WSRequest = {
    val headers = hdrs.foldLeft(this.headers)((m, hdr) =>
      if (m.contains(hdr._1)) m.updated(hdr._1, m(hdr._1) :+ hdr._2)
      else m + (hdr._1 -> Seq(hdr._2))
    )
    copy(headers = headers)
  }

  def withQueryString(parameters: (String, String)*): WSRequest =
    copy(queryString = parameters.foldLeft(this.queryString) {
      case (m, (k, v)) => m + (k -> (v +: m.getOrElse(k, Nil)))
    })

  def withFollowRedirects(follow: Boolean): WSRequest = copy(followRedirects = Some(follow))

  def withRequestFilter(filter: WSRequestFilter): WSRequest = copy(filters = filters :+ filter)

  def withRequestTimeout(timeout: Duration): WSRequest = {
    timeout match {
      case Duration.Inf =>
        copy(requestTimeout = Some(-1))
      case d =>
        val millis = d.toMillis
        require(millis >= 0 && millis <= Int.MaxValue, s"Request timeout must be between 0 and ${Int.MaxValue} milliseconds")
        copy(requestTimeout = Some(millis.toInt))
    }
  }

  def withVirtualHost(vh: String): WSRequest = copy(virtualHost = Some(vh))

  def withProxyServer(proxyServer: WSProxyServer): WSRequest = copy(proxyServer = Some(proxyServer))

  def withBody(body: WSBody): WSRequest = copy(body = body)

  def withMethod(method: String): WSRequest = copy(method = method)

  def execute(): Future[WSResponse] = {
    val executor = filterWSRequestExecutor(new WSRequestExecutor {
      override def execute(request: WSRequest): Future[WSResponse] =
        request.asInstanceOf[AhcWSRequest].execute(buildRequest())
    })
    executor.execute(this)
  }

  protected def filterWSRequestExecutor(next: WSRequestExecutor): WSRequestExecutor = {
    filters.foldRight(next)(_ apply _)
  }

  def stream(): Future[StreamedResponse] = Streamed.execute(client.underlying, buildRequest())

  @deprecated("2.5", "Use `stream()` instead.")
  def streamWithEnumerator(): Future[(WSResponseHeaders, Enumerator[Array[Byte]])] = Streamed.execute2(client.underlying, buildRequest())

  /**
   * Returns the current headers of the request, using the request builder.  This may be signed,
   * so may return extra headers that were not directly input.
   */
  def requestHeaders: Map[String, Seq[String]] = AhcWSRequest.ahcHeadersToMap(buildRequest().getHeaders)

  /**
   * Returns the HTTP header given by name, using the request builder.  This may be signed,
   * so may return extra headers that were not directly input.
   */
  def requestHeader(name: String): Option[String] = requestHeaders.get(name).flatMap(_.headOption)

  /**
   * Returns the current query string parameters, using the request builder.  This may be signed,
   * so may not return the same parameters that were input.
   */
  def requestQueryParams: Map[String, Seq[String]] = {
    val params: java.util.List[Param] = buildRequest().getQueryParams
    params.asScala.toSeq.groupBy(_.getName).mapValues(_.map(_.getValue))
  }

  /**
   * Returns the current URL, using the request builder.  This may be signed by OAuth, as opposed
   * to request.url.
   */
  def requestUrl: String = buildRequest().getUrl

  /**
   * Returns the body as an array of bytes.
   */
  def getBody: Option[ByteString] = {
    body match {
      case InMemoryBody(bytes) => Some(bytes)
      case _ => None
    }
  }

  private[libs] def authScheme(scheme: WSAuthScheme): Realm.AuthScheme = scheme match {
    case WSAuthScheme.DIGEST => Realm.AuthScheme.DIGEST
    case WSAuthScheme.BASIC => Realm.AuthScheme.BASIC
    case WSAuthScheme.NTLM => Realm.AuthScheme.NTLM
    case WSAuthScheme.SPNEGO => Realm.AuthScheme.SPNEGO
    case WSAuthScheme.KERBEROS => Realm.AuthScheme.KERBEROS
    case _ => throw new RuntimeException("Unknown scheme " + scheme)
  }

  /**
   * Add http auth headers. Defaults to HTTP Basic.
   */
  private[libs] def auth(username: String, password: String, scheme: Realm.AuthScheme = Realm.AuthScheme.BASIC): Realm = {
    new Realm.Builder(username, password)
      .setScheme(scheme)
      .setUsePreemptiveAuth(true)
      .build()
  }

  def contentType: Option[String] = {
    this.headers.find(p => p._1 == HttpHeaders.Names.CONTENT_TYPE).map {
      case (header, values) =>
        values.head
    }
  }

  /**
   * Creates and returns an AHC request, running all operations on it.
   */
  def buildRequest(): Request = {
    // The builder has a bunch of mutable state and is VERY fiddly, so
    // should not be exposed to the outside world.

    val builder = disableUrlEncoding.map { disableEncodingFlag =>
      new RequestBuilder(method, disableEncodingFlag)
    }.getOrElse {
      new RequestBuilder(method)
    }

    // Set the URL.
    builder.setUrl(url)

    // auth
    auth.foreach { data =>
      val realm = auth(data._1, data._2, authScheme(data._3))
      builder.setRealm(realm)
    }

    // queries
    for {
      (key, values) <- queryString
      value <- values
    } builder.addQueryParam(key, value)

    // Configuration settings on the builder, if applicable
    virtualHost.foreach(builder.setVirtualHost)
    followRedirects.foreach(builder.setFollowRedirect)
    proxyServer.foreach(p => builder.setProxyServer(createProxy(p)))
    requestTimeout.foreach(builder.setRequestTimeout)

    val (builderWithBody, updatedHeaders) = body match {
      case EmptyBody => (builder, this.headers)
      case FileBody(file) =>
        import org.asynchttpclient.request.body.generator.FileBodyGenerator
        val bodyGenerator = new FileBodyGenerator(file)
        builder.setBody(bodyGenerator)
        (builder, this.headers)
      case InMemoryBody(bytes) =>
        val ct: String = contentType.getOrElse("text/plain")

        val h = try {
          // Only parse out the form body if we are doing the signature calculation.
          if (ct.contains(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED) && calc.isDefined) {
            // If we are taking responsibility for setting the request body, we should block any
            // externally defined Content-Length field (see #5221 for the details)
            val filteredHeaders = this.headers.filterNot { case (k, v) => k.equalsIgnoreCase(HttpHeaders.Names.CONTENT_LENGTH) }

            // extract the content type and the charset
            val charsetOption = Option(HttpUtils.parseCharset(ct))
            val charset = charsetOption.getOrElse {
              StandardCharsets.UTF_8
            }.name()

            // Get the string body given the given charset...
            val stringBody = bytes.decodeString(charset)
            // The Ahc signature calculator uses request.getFormParams() for calculation,
            // so we have to parse it out and add it rather than using setBody.

            val params = for {
              (key, values) <- FormUrlEncodedParser.parse(stringBody).toSeq
              value <- values
            } yield new Param(key, value)
            builder.setFormParams(params.asJava)
            filteredHeaders
          } else {
            builder.setBody(bytes.toArray)
            this.headers
          }
        } catch {
          case e: UnsupportedEncodingException =>
            throw new RuntimeException(e)
        }

        (builder, h)
      case StreamedBody(source) =>
        // If the body has a streaming interface it should be up to the user to provide a manual Content-Length
        // else every content would be Transfer-Encoding: chunked
        // If the Content-Length is -1 Async-Http-Client sets a Transfer-Encoding: chunked
        // If the Content-Length is great than -1 Async-Http-Client will use the correct Content-Length
        val filteredHeaders = this.headers.filterNot { case (k, v) => k.equalsIgnoreCase(HttpHeaders.Names.CONTENT_LENGTH) }
        val contentLength = this.headers.find { case (k, _) => k.equalsIgnoreCase(HttpHeaders.Names.CONTENT_LENGTH) }.map(_._2.head.toLong)

        (builder.setBody(source.map(_.toByteBuffer).runWith(Sink.asPublisher(false)), contentLength.getOrElse(-1L)), filteredHeaders)
    }

    // headers
    for {
      header <- updatedHeaders
      value <- header._2
    } builder.addHeader(header._1, value)

    // Set the signature calculator.
    calc.map {
      case signatureCalculator: org.asynchttpclient.SignatureCalculator =>
        builderWithBody.setSignatureCalculator(signatureCalculator)
      case _ =>
        throw new IllegalStateException("Unknown signature calculator found: use a class that implements SignatureCalculator")
    }

    builderWithBody.build()
  }

  private[libs] def execute(request: Request): Future[AhcWSResponse] = {
    import org.asynchttpclient.AsyncCompletionHandler
    val result = Promise[AhcWSResponse]()

    client.executeRequest(request, new AsyncCompletionHandler[AHCResponse]() {
      override def onCompleted(response: AHCResponse) = {
        result.success(AhcWSResponse(response))
        response
      }

      override def onThrowable(t: Throwable) = {
        result.failure(t)
      }
    })
    result.future
  }

  private[libs] def createProxy(wsProxyServer: WSProxyServer): AHCProxyServer = {
    val proxyBuilder = new AHCProxyServer.Builder(wsProxyServer.host, wsProxyServer.port)
    if (wsProxyServer.principal.isDefined) {
      val realmBuilder = new Realm.Builder(wsProxyServer.principal.orNull, wsProxyServer.password.orNull)
      val scheme: Realm.AuthScheme = wsProxyServer.protocol.getOrElse("http").toLowerCase(java.util.Locale.ENGLISH) match {
        case "http" | "https" => Realm.AuthScheme.BASIC
        case "kerberos" => Realm.AuthScheme.KERBEROS
        case "ntlm" => Realm.AuthScheme.NTLM
        case "spnego" => Realm.AuthScheme.SPNEGO
        case _ => scala.sys.error("Unrecognized protocol!")
      }
      realmBuilder.setScheme(scheme)
      wsProxyServer.encoding.foreach(enc => realmBuilder.setCharset(Charset.forName(enc)))
      wsProxyServer.ntlmDomain.foreach(realmBuilder.setNtlmDomain)
      proxyBuilder.setRealm(realmBuilder)
    }

    wsProxyServer.nonProxyHosts.foreach { nonProxyHosts =>
      import scala.collection.JavaConverters._
      proxyBuilder.setNonProxyHosts(nonProxyHosts.asJava)
    }
    proxyBuilder.build()
  }

}

class AhcWSModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[WSAPI].to[AhcWSAPI],
      bind[AhcWSClientConfig].toProvider[AhcWSClientConfigParser].in[Singleton],
      bind[WSClientConfig].toProvider[WSConfigParser].in[Singleton],
      bind[WSClient].toProvider[WSClientProvider].in[Singleton]
    )
  }
}

class WSClientProvider @Inject() (wsApi: WSAPI) extends Provider[WSClient] {
  def get() = wsApi.client
}

@Singleton
class AhcWSAPI @Inject() (environment: Environment, clientConfig: AhcWSClientConfig, lifecycle: ApplicationLifecycle)(implicit materializer: Materializer) extends WSAPI {

  private val logger = Logger(classOf[AhcWSAPI])

  lazy val client = {
    if (clientConfig.wsClientConfig.ssl.debug.enabled) {
      environment.mode match {
        case Mode.Prod =>
          logger.warn("AhcWSAPI: ws.ssl.debug settings enabled in production mode!")
        case _ => // do nothing
      }
      new DebugConfiguration().configure(clientConfig.wsClientConfig.ssl.debug)
    }

    val client = AhcWSClient(clientConfig)

    lifecycle.addStopHook { () =>
      Future.successful(client.close())
    }
    client
  }

  def url(url: String) = client.url(url)

}

/**
 * The Ahc implementation of a WS cookie.
 */
private class AhcWSCookie(ahcCookie: AHCCookie) extends WSCookie {

  private def noneIfEmpty(value: String): Option[String] = {
    if (value.isEmpty) None else Some(value)
  }

  /**
   * The underlying cookie object for the client.
   */
  def underlying[T] = ahcCookie.asInstanceOf[T]

  /**
   * The domain.
   */
  def domain: String = ahcCookie.getDomain

  /**
   * The cookie name.
   */
  def name: Option[String] = noneIfEmpty(ahcCookie.getName)

  /**
   * The cookie value.
   */
  def value: Option[String] = noneIfEmpty(ahcCookie.getValue)

  /**
   * The path.
   */
  def path: String = ahcCookie.getPath

  /**
   * The maximum age.
   */
  def maxAge: Option[Long] = if (ahcCookie.getMaxAge <= -1) None else Some(ahcCookie.getMaxAge)

  /**
   * If the cookie is secure.
   */
  def secure: Boolean = ahcCookie.isSecure

  /*
   * Cookie ports should not be used; cookies for a given host are shared across
   * all the ports on that host.
   */

  override def toString: String = ahcCookie.toString
}

/**
 * A WS HTTP response.
 */
case class AhcWSResponse(ahcResponse: AHCResponse) extends WSResponse {

  import play.api.libs.json._

  import scala.xml._

  /**
   * Return the headers of the response as a case-insensitive map
   */
  lazy val allHeaders: Map[String, Seq[String]] = {
    val headers: HttpHeaders = ahcResponse.getHeaders
    AhcWSRequest.ahcHeadersToMap(headers)
  }

  /**
   * @return The underlying response object.
   */
  def underlying[T] = ahcResponse.asInstanceOf[T]

  /**
   * The response status code.
   */
  def status: Int = ahcResponse.getStatusCode

  /**
   * The response status message.
   */
  def statusText: String = ahcResponse.getStatusText

  /**
   * Get a response header.
   */
  def header(key: String): Option[String] = Option(ahcResponse.getHeader(key))

  /**
   * Get all the cookies.
   */
  def cookies: Seq[WSCookie] = {
    ahcResponse.getCookies.asScala.map(new AhcWSCookie(_))
  }

  /**
   * Get only one cookie, using the cookie name.
   */
  def cookie(name: String): Option[WSCookie] = cookies.find(_.name == Option(name))

  /**
   * The response body as String.
   */
  lazy val body: String = {
    // RFC-2616#3.7.1 states that any text/* mime type should default to ISO-8859-1 charset if not
    // explicitly set, while Plays default encoding is UTF-8.  So, use UTF-8 if charset is not explicitly
    // set and content type is not text/*, otherwise default to ISO-8859-1
    val contentType = Option(ahcResponse.getContentType).getOrElse("application/octet-stream")
    val charset = Option(HttpUtils.parseCharset(contentType)).getOrElse {
      if (contentType.startsWith("text/"))
        HttpUtils.DEFAULT_CHARSET
      else
        StandardCharsets.UTF_8
    }
    ahcResponse.getResponseBody(charset)
  }

  /**
   * The response body as Xml.
   */
  lazy val xml: Elem = Play.XML.loadString(body)

  /**
   * The response body as Json.
   */
  lazy val json: JsValue = Json.parse(ahcResponse.getResponseBodyAsBytes)

  /**
   * The response body as a byte string.
   */
  @throws(classOf[IOException])
  def bodyAsBytes: ByteString = ByteString(ahcResponse.getResponseBodyAsBytes)

  override def toString: String =
    s"AhcWSResponse($status, $statusText)"
}

/**
 * Ahc WS API implementation components.
 */
trait AhcWSComponents {

  def environment: Environment

  def configuration: Configuration

  def applicationLifecycle: ApplicationLifecycle

  def materializer: Materializer

  lazy val wsClientConfig: WSClientConfig = new WSConfigParser(configuration, environment).parse()
  lazy val ahcWsClientConfig: AhcWSClientConfig =
    new AhcWSClientConfigParser(wsClientConfig, configuration, environment).parse()
  lazy val wsApi: WSAPI = new AhcWSAPI(environment, ahcWsClientConfig, applicationLifecycle)(materializer)
  lazy val wsClient: WSClient = wsApi.client
}
