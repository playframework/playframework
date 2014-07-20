/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.ws.ning

import com.ning.http.client.{ Response => AHCResponse, ProxyServer => AHCProxyServer, _ }
import com.ning.http.client.cookie.{ Cookie => AHCCookie }
import com.ning.http.client.Realm.{ RealmBuilder, AuthScheme }
import com.ning.http.util.AsyncHttpProviderUtils

import collection.immutable.TreeMap

import scala.concurrent.{ Future, Promise }

import java.util.concurrent.atomic.AtomicReference

import play.api.libs.ws._
import play.api.libs.ws.ssl._

import play.api.libs.iteratee._
import play.api.{ Mode, Application, Play }
import play.core.utils.CaseInsensitiveOrdered
import play.api.libs.ws.DefaultWSResponseHeaders
import play.api.libs.iteratee.Input.El
import play.api.libs.ws.ssl.debug._

import scala.collection.JavaConverters._

/**
 * A WS client backed by a Ning AsyncHttpClient.
 *
 * If you need to debug Ning, set logger.com.ning.http.client=DEBUG in your application.conf file.
 *
 * @param config a client configuration object
 */
class NingWSClient(config: AsyncHttpClientConfig) extends WSClient {

  private val asyncHttpClient = new AsyncHttpClient(config)

  def underlying[T] = asyncHttpClient.asInstanceOf[T]

  private[libs] def executeRequest[T](request: Request, handler: AsyncHandler[T]): ListenableFuture[T] = asyncHttpClient.executeRequest(request, handler)

  def close() = asyncHttpClient.close()

  def url(url: String): WSRequestHolder = NingWSRequestHolder(this, url, "GET", EmptyBody, Map(), Map(), None, None, None, None, None, None)
}

/**
 * A WS Request.
 */
case class NingWSRequest(client: NingWSClient,
  method: String,
  private val auth: Option[(String, String, WSAuthScheme)],
  private val calc: Option[WSSignatureCalculator],
  private val headers: Map[String, Seq[String]],
  private val body: WSBody,
  builder: RequestBuilder)
    extends WSRequest {

  //this will do a java mutable set hence the {} response
  auth.map(data => auth(data._1, data._2, authScheme(data._3))).getOrElse({})

  /**
   * Return the current headers of the request being constructed
   */
  def allHeaders: Map[String, Seq[String]] = headers

  /**
   * Return the current query string parameters
   */
  def queryString: Map[String, Seq[String]] = {
    val request = builder.build()
    val params = request.getParams
    require(params != null)
    mapAsScalaMapConverter(params).asScala.map {
      e =>
        e._1 -> e._2.asScala.toSeq
    }.toMap
  }

  /**
   * Retrieve an HTTP header.
   */
  def header(name: String): Option[String] = headers.get(name).flatMap(_.headOption)

  def url: String = {
    val request = builder.build()
    request.getUrl
  }

  def getBody: Option[Array[Byte]] = {
    body match {
      case InMemoryBody(bytes) => Some(bytes)
      case _ => None
    }
  }

  /**
   * Set an HTTP header.
   */
  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  override def setHeader(name: String, value: String): NingWSRequest = {
    this.copy(builder = builder.setHeader(name, value))
  }

  /**
   * Add an HTTP header (used for headers with multiple values).
   */
  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  override def addHeader(name: String, value: String): NingWSRequest = {
    this.copy(builder = builder.addHeader(name, value))
  }

  /**
   * Defines the request headers.
   */
  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  def setHeaders(hdrs: FluentCaseInsensitiveStringsMap): NingWSRequest = {
    this.copy(builder = builder.setHeaders(hdrs))
  }

  /**
   * Defines the request headers.
   */
  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  def setHeaders(hdrs: java.util.Map[String, java.util.Collection[String]]): NingWSRequest = {
    this.copy(builder = builder.setHeaders(hdrs))
  }

  /**
   * Defines the request headers.
   */
  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  def setHeaders(hdrs: Map[String, Seq[String]]): NingWSRequest = {
    // roll up the builders using two foldlefts...
    val newBuilder = hdrs.foldLeft(builder) {
      (b, header) =>
        header._2.foldLeft(b) {
          (b2, value) =>
            b2.addHeader(header._1, value)
        }
    }
    this.copy(builder = newBuilder)
  }

  /**
   * Defines the query string.
   */
  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  def setQueryString(queryString: Map[String, Seq[String]]): NingWSRequest = {
    val newBuilder = queryString.foldLeft(builder) {
      (b, entry) =>
        val (key, values) = entry
        values.foldLeft(b) {
          (b2, value) =>
            b2.addQueryParameter(key, value)
        }
    }
    this.copy(builder = newBuilder)
  }

  /**
   * Defines the URL.
   */
  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  def setUrl(url: String): NingWSRequest = {
    this.copy(builder = builder.setUrl(url))
  }

  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  def setPerRequestConfig(config: PerRequestConfig): NingWSRequest = {
    this.copy(builder = builder.setPerRequestConfig(config))
  }

  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  def setFollowRedirects(followRedirects: Boolean): NingWSRequest = {
    this.copy(builder = builder.setFollowRedirects(followRedirects))
  }

  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  def setVirtualHost(virtualHost: String): NingWSRequest = {
    this.copy(builder = builder.setVirtualHost(virtualHost))
  }

  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  def setProxyServer(proxyServer: AHCProxyServer): NingWSRequest = {
    this.copy(builder = builder.setProxyServer(proxyServer))
  }

  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  def setBody(s: String): NingWSRequest = {
    this.copy(builder = builder.setBody(s))
  }

  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  def setBody(bodyGenerator: BodyGenerator): NingWSRequest = {
    this.copy(builder = builder.setBody(bodyGenerator))
  }

  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  def setBody(byteArray: Array[Byte]): NingWSRequest = {
    this.copy(builder = builder.setBody(byteArray))
  }

  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  def build: com.ning.http.client.Request = {
    builder.build()
  }

  private def authScheme(scheme: WSAuthScheme): AuthScheme = scheme match {
    case WSAuthScheme.DIGEST => AuthScheme.DIGEST
    case WSAuthScheme.BASIC => AuthScheme.BASIC
    case WSAuthScheme.NTLM => AuthScheme.NTLM
    case WSAuthScheme.SPNEGO => AuthScheme.SPNEGO
    case WSAuthScheme.KERBEROS => AuthScheme.KERBEROS
    case WSAuthScheme.NONE => AuthScheme.NONE
    case _ => throw new RuntimeException("Unknown scheme " + scheme)
  }

  /**
   * Add http auth headers. Defaults to HTTP Basic.
   */
  private def auth(username: String, password: String, scheme: AuthScheme = AuthScheme.BASIC): Unit = {
    builder.setRealm((new RealmBuilder)
      .setScheme(scheme)
      .setPrincipal(username)
      .setPassword(password)
      .setUsePreemptiveAuth(true)
      .build())
  }

  private def ningHeadersToMap(headers: java.util.Map[String, java.util.Collection[String]]) =
    mapAsScalaMapConverter(headers).asScala.map(e => e._1 -> e._2.asScala.toSeq).toMap

  private def ningHeadersToMap(headers: FluentCaseInsensitiveStringsMap) = {
    val res = mapAsScalaMapConverter(headers).asScala.map(e => e._1 -> e._2.asScala.toSeq).toMap
    //todo: wrap the case insensitive ning map instead of creating a new one (unless perhaps immutabilty is important)
    TreeMap(res.toSeq: _*)(CaseInsensitiveOrdered)
  }

  private[libs] def execute: Future[NingWSResponse] = {
    import com.ning.http.client.AsyncCompletionHandler
    var result = Promise[NingWSResponse]()
    calc.map(_.sign(this))

    try {
      client.executeRequest(builder.build(), new AsyncCompletionHandler[AHCResponse]() {
        override def onCompleted(response: AHCResponse) = {
          result.success(NingWSResponse(response))
          response
        }

        override def onThrowable(t: Throwable) = {
          result.failure(t)
        }
      })
      result.future
    } catch {
      case e: Exception => Future.failed(e)
    }
  }

  private[libs] def executeStream(): Future[(WSResponseHeaders, Enumerator[Array[Byte]])] = {
    import com.ning.http.client.AsyncHandler
    import play.api.libs.concurrent.Execution.Implicits.defaultContext

    val result = Promise[(WSResponseHeaders, Enumerator[Array[Byte]])]()

    val errorInStream = Promise[Unit]()

    calc.map(_.sign(this))

    val promisedIteratee = Promise[Iteratee[Array[Byte], Unit]]()

    @volatile var doneOrError = false
    @volatile var statusCode = 0
    @volatile var current: Iteratee[Array[Byte], Unit] = Iteratee.flatten(promisedIteratee.future)

    try {
      client.executeRequest(builder.build(), new AsyncHandler[Unit]() {

        import com.ning.http.client.AsyncHandler.STATE

        override def onStatusReceived(status: HttpResponseStatus) = {
          statusCode = status.getStatusCode
          STATE.CONTINUE
        }

        override def onHeadersReceived(h: HttpResponseHeaders) = {
          val headers = h.getHeaders

          val responseHeader = DefaultWSResponseHeaders(statusCode, ningHeadersToMap(headers))
          val enumerator = new Enumerator[Array[Byte]]() {
            def apply[A](i: Iteratee[Array[Byte], A]) = {

              val doneIteratee = Promise[Iteratee[Array[Byte], A]]()

              // Map it so that we can complete the iteratee when it returns
              val mapped = i.map { a =>
                doneIteratee.trySuccess(Done(a))
                ()
              }.recover {
                // but if an error happens, we want to propogate that
                case e =>
                  doneIteratee.tryFailure(e)
                  throw e
              }

              // Redeem the iteratee that we promised to the AsyncHandler
              promisedIteratee.trySuccess(mapped)

              // If there's an error in the stream from upstream, then fail this returned future with that
              errorInStream.future.onFailure {
                case e => doneIteratee.tryFailure(e)
              }

              doneIteratee.future
            }
          }

          result.trySuccess((responseHeader, enumerator))
          STATE.CONTINUE
        }

        override def onBodyPartReceived(bodyPart: HttpResponseBodyPart) = {
          if (!doneOrError) {
            current = current.pureFlatFold {
              case Step.Done(a, e) =>
                doneOrError = true
                Done(a, e)

              case Step.Cont(k) =>
                k(El(bodyPart.getBodyPartBytes))

              case Step.Error(e, input) =>
                doneOrError = true
                Error(e, input)

            }
            STATE.CONTINUE
          } else {
            current = null
            // Must close underlying connection, otherwise async http client will drain the stream
            bodyPart.markUnderlyingConnectionAsClosed()
            STATE.ABORT
          }
        }

        override def onCompleted() = {
          Option(current).foreach(_.run)
        }

        override def onThrowable(t: Throwable) = {
          result.tryFailure(t)
          errorInStream.tryFailure(t)
        }
      })
      result.future
    } catch {
      case e: Exception => Future.failed(e)
    }
  }

}

/**
 * A WS Request builder.
 */
case class NingWSRequestHolder(client: NingWSClient,
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
    proxyServer: Option[WSProxyServer]) extends WSRequestHolder {

  def sign(calc: WSSignatureCalculator): WSRequestHolder = copy(calc = Some(calc))

  def withAuth(username: String, password: String, scheme: WSAuthScheme) =
    copy(auth = Some((username, password, scheme)))

  def withHeaders(hdrs: (String, String)*) = {
    val headers = hdrs.foldLeft(this.headers)((m, hdr) =>
      if (m.contains(hdr._1)) m.updated(hdr._1, m(hdr._1) :+ hdr._2)
      else m + (hdr._1 -> Seq(hdr._2))
    )
    copy(headers = headers)
  }

  def withQueryString(parameters: (String, String)*) =
    copy(queryString = parameters.foldLeft(queryString) {
      case (m, (k, v)) => m + (k -> (v +: m.get(k).getOrElse(Nil)))
    })

  def withFollowRedirects(follow: Boolean) = copy(followRedirects = Some(follow))

  def withRequestTimeout(timeout: Int) = copy(requestTimeout = Some(timeout))

  def withVirtualHost(vh: String) = copy(virtualHost = Some(vh))

  def withProxyServer(proxyServer: WSProxyServer) = copy(proxyServer = Some(proxyServer))

  def withBody(body: WSBody) = copy(body = body)

  def withMethod(method: String) = copy(method = method)

  def execute(): Future[WSResponse] = {
    prepare().execute
  }

  def stream(): Future[(WSResponseHeaders, Enumerator[Array[Byte]])] = {
    prepare().executeStream()
  }

  private[ning] def prepare(): NingWSRequest = {
    val builder = createBuilder()
    val builderWithBody = body match {
      case EmptyBody => builder
      case FileBody(file) =>
        import com.ning.http.client.generators.FileBodyGenerator
        val bodyGenerator = new FileBodyGenerator(file)
        builder.setBody(bodyGenerator)
      case InMemoryBody(bytes) =>
        builder.setBody(bytes)
      case StreamedBody(bytes) =>
        builder
    }
    new NingWSRequest(client, method, auth, calc, headers, body, builderWithBody)
  }

  private def createBuilder() = {
    val builder = new RequestBuilder(method).setUrl(url)

    for {
      header <- headers
      value <- header._2
    } builder.addHeader(header._1, value)

    for {
      (key, values) <- queryString
      value <- values
    } builder.addQueryParameter(key, value)

    virtualHost.map(builder.setVirtualHost)
    followRedirects.map(builder.setFollowRedirects)

    proxyServer.map { p =>
      builder.setProxyServer(createProxy(p))
    }

    requestTimeout.map { t =>
      val config = new PerRequestConfig()
      config.setRequestTimeoutInMs(t)
      builder.setPerRequestConfig(config)
    }

    builder
  }

  private[play] def createProxy(wsServer: WSProxyServer) = {
    import com.ning.http.client.ProxyServer.Protocol
    val protocol: Protocol = wsServer.protocol.getOrElse("http").toLowerCase match {
      case "http" => Protocol.HTTP
      case "https" => Protocol.HTTPS
      case "kerberos" => Protocol.KERBEROS
      case "ntlm" => Protocol.NTLM
      case "spnego" => Protocol.SPNEGO
      case _ => scala.sys.error("Unrecognized protocol!")
    }

    val ningServer = new AHCProxyServer(
      protocol,
      wsServer.host,
      wsServer.port,
      wsServer.principal.getOrElse(null),
      wsServer.password.getOrElse(null))

    wsServer.encoding.map(ningServer.setEncoding(_))
    wsServer.ntlmDomain.map(ningServer.setNtlmDomain(_))
    for {
      hosts <- wsServer.nonProxyHosts
      host <- hosts
    } ningServer.addNonProxyHost(host)

    ningServer
  }

}

/**
 * WSPlugin implementation hook.
 */
class NingWSPlugin(app: Application) extends WSPlugin {

  @volatile var loaded = false

  override lazy val enabled = true

  private val config = new DefaultWSConfigParser(app.configuration, app.classloader).parse()

  private lazy val ningAPI = new NingWSAPI(app, config)

  override def onStart() {
    loaded = true
  }

  override def onStop() {
    if (loaded) {
      ningAPI.resetClient()
      loaded = false
    }
  }

  def api = ningAPI

}

class NingWSAPI(app: Application, clientConfig: WSClientConfig) extends WSAPI {

  private val clientHolder: AtomicReference[Option[NingWSClient]] = new AtomicReference(None)

  private[play] def newClient(): NingWSClient = {
    val asyncClientConfig = buildAsyncClientConfig(clientConfig)

    new SystemConfiguration().configure(clientConfig)
    clientConfig.ssl.map {
      _.debug.map { debugConfig =>
        app.mode match {
          case Mode.Prod =>
            Play.logger.warn("NingWSAPI: ws.ssl.debug settings enabled in production mode!")
          case _ => // do nothing
        }
        new DebugConfiguration().configure(debugConfig)
      }
    }

    new NingWSClient(asyncClientConfig)
  }

  def client: NingWSClient = {
    clientHolder.get.getOrElse({
      // A critical section of code. Only one caller has the opportunity of creating a default client.
      synchronized {
        clientHolder.get match {
          case None =>
            val client = newClient()
            clientHolder.set(Some(client))
            client

          case Some(client) => client
        }
      }
    })
  }

  def url(url: String) = client.url(url)

  /**
   * resets the underlying AsyncHttpClient
   */
  private[play] def resetClient(): Unit = {
    clientHolder.getAndSet(None).map(oldClient => oldClient.close())
  }

  private[play] def buildAsyncClientConfig(wsClientConfig: WSClientConfig): AsyncHttpClientConfig = {
    new NingAsyncHttpClientConfigBuilder(wsClientConfig).build()
  }
}

/**
 * The Ning implementation of a WS cookie.
 */
private class NingWSCookie(ahcCookie: AHCCookie) extends WSCookie {

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
   * The expiry date.
   */
  def expires: Option[Long] = if (ahcCookie.getExpires == -1) None else Some(ahcCookie.getExpires)

  /**
   * The maximum age.
   */
  def maxAge: Option[Int] = if (ahcCookie.getMaxAge == -1) None else Some(ahcCookie.getMaxAge)

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
case class NingWSResponse(ahcResponse: AHCResponse) extends WSResponse {

  import scala.xml._
  import play.api.libs.json._

  /**
   * Get the underlying response object.
   */
  @deprecated("Use underlying", "2.3.0")
  def getAHCResponse = ahcResponse

  /**
   * Return the headers of the response as a case-insensitive map
   */
  lazy val allHeaders: Map[String, Seq[String]] = {
    TreeMap[String, Seq[String]]()(CaseInsensitiveOrdered) ++
      mapAsScalaMapConverter(ahcResponse.getHeaders).asScala.mapValues(_.asScala)
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
    ahcResponse.getCookies.asScala.map(new NingWSCookie(_))
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
    val charset = Option(AsyncHttpProviderUtils.parseCharset(contentType)).getOrElse {
      if (contentType.startsWith("text/"))
        AsyncHttpProviderUtils.DEFAULT_CHARSET
      else
        "utf-8"
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

}
