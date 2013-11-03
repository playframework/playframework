/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.ws.ning

import com.ning.http.client.{ Response => AHCResponse, Cookie => AHCCookie, ProxyServer => AHCProxyServer, _ }
import com.ning.http.client.Realm.{ RealmBuilder, AuthScheme }
import com.ning.http.util.AsyncHttpProviderUtils

import collection.immutable.TreeMap

import scala.concurrent.{ Future, Promise, ExecutionContext }

import java.io.File
import java.util.concurrent.atomic.AtomicReference

import play.api.libs.ws._
import play.api.http.{ Writeable, ContentTypeOf }
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input.El
import play.api.{ Application, Play }

import play.core.utils.CaseInsensitiveOrdered

/**
 * A WS client backed by a Ning AsyncHttpClient.
 * @param config the config for AsyncHttpClient
 */
class NingWSClient(config: AsyncHttpClientConfig) extends WSClient {
  private val asyncHttpClient = new AsyncHttpClient(config)

  def underlying[T] = asyncHttpClient.asInstanceOf[T]

  private[libs] def executeRequest[T](request: Request, handler: AsyncHandler[T]): ListenableFuture[T] = asyncHttpClient.executeRequest(request, handler)

  def close() = asyncHttpClient.close()

  def url(url: String): WSRequestHolder = NingWSRequestHolder(this, url, Map(), Map(), None, None, None, None, None, None)
}

/**
 * A WS Request.
 */
case class NingWSRequest(client: NingWSClient,
  private val _method: String,
  private val _auth: Option[(String, String, WSAuthScheme)],
  private val _calc: Option[WSSignatureCalculator],
  builder: RequestBuilder)
    extends WSRequest {

  import scala.collection.JavaConverters._

  protected var body: Option[String] = None

  protected var calculator: Option[WSSignatureCalculator] = _calc

  protected var headers: Map[String, Seq[String]] = Map()

  protected var _url: String = null

  //this will do a java mutable set hence the {} response
  _auth.map(data => auth(data._1, data._2, authScheme(data._3))).getOrElse({})

  /**
   * Return the current headers of the request being constructed
   */
  def allHeaders: Map[String, Seq[String]] = {
    mapAsScalaMapConverter(builder.build().getHeaders).asScala.map(e => e._1 -> e._2.asScala.toSeq).toMap
  }

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

  /**
   * The HTTP method.
   */
  def method: String = _method

  /**
   * The URL
   */
  def url: String = _url

  def getStringData: String = body.getOrElse("")

  /**
   * Set an HTTP header.
   */
  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  override def setHeader(name: String, value: String): NingWSRequest = {
    headers = headers + (name -> List(value))
    this.copy(builder = builder.setHeader(name, value))
  }

  /**
   * Add an HTTP header (used for headers with multiple values).
   */
  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  override def addHeader(name: String, value: String): NingWSRequest = {
    headers = headers + (name -> (headers.get(name).getOrElse(List()) :+ value))
    this.copy(builder = builder.addHeader(name, value))
  }

  /**
   * Defines the request headers.
   */
  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  def setHeaders(hdrs: FluentCaseInsensitiveStringsMap): NingWSRequest = {
    headers = ningHeadersToMap(hdrs)
    this.copy(builder = builder.setHeaders(hdrs))
  }

  /**
   * Defines the request headers.
   */
  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  def setHeaders(hdrs: java.util.Map[String, java.util.Collection[String]]): NingWSRequest = {
    headers = ningHeadersToMap(hdrs)
    this.copy(builder = builder.setHeaders(hdrs))
  }

  /**
   * Defines the request headers.
   */
  @scala.deprecated("This will be a protected method, please use WSRequestHolder", "2.3.0")
  def setHeaders(hdrs: Map[String, Seq[String]]): NingWSRequest = {
    headers = hdrs
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
    _url = url
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
    this.body = Some(s)
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
  private def auth(username: String, password: String, scheme: AuthScheme = AuthScheme.BASIC): WSRequest = {
    this.copy(builder = builder.setRealm((new RealmBuilder)
      .setScheme(scheme)
      .setPrincipal(username)
      .setPassword(password)
      .setUsePreemptiveAuth(true)
      .build()))
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
    calculator.map(_.sign(this))
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
  }

  private[libs] def executeStream[A](consumer: WSResponseHeaders => Iteratee[Array[Byte], A])(implicit ec: ExecutionContext): Future[Iteratee[Array[Byte], A]] = {
    import com.ning.http.client.AsyncHandler
    var doneOrError = false
    calculator.map(_.sign(this))

    var statusCode = 0
    val iterateeP = Promise[Iteratee[Array[Byte], A]]()
    var iteratee: Iteratee[Array[Byte], A] = null

    client.executeRequest(builder.build(), new AsyncHandler[Unit]() {

      import com.ning.http.client.AsyncHandler.STATE

      override def onStatusReceived(status: HttpResponseStatus) = {
        statusCode = status.getStatusCode()
        STATE.CONTINUE
      }

      override def onHeadersReceived(h: HttpResponseHeaders) = {
        val headers = h.getHeaders()
        iteratee = consumer(DefaultWSResponseHeaders(statusCode, ningHeadersToMap(headers)))
        STATE.CONTINUE
      }

      override def onBodyPartReceived(bodyPart: HttpResponseBodyPart) = {
        if (!doneOrError) {
          iteratee = iteratee.pureFlatFold {
            case Step.Done(a, e) => {
              doneOrError = true
              val it = Done(a, e)
              iterateeP.success(it)
              it
            }

            case Step.Cont(k) => {
              k(El(bodyPart.getBodyPartBytes()))
            }

            case Step.Error(e, input) => {
              doneOrError = true
              val it = Error(e, input)
              iterateeP.success(it)
              it
            }
          }
          STATE.CONTINUE
        } else {
          iteratee = null
          // Must close underlying connection, otherwise async http client will drain the stream
          bodyPart.markUnderlyingConnectionAsClosed()
          STATE.ABORT
        }
      }

      override def onCompleted() = {
        Option(iteratee).map(iterateeP.success)
      }

      override def onThrowable(t: Throwable) = {
        iterateeP.failure(t)
      }
    })
    iterateeP.future
  }

}

/**
 * A WS Request builder.
 */
case class NingWSRequestHolder(client: NingWSClient,
    url: String,
    headers: Map[String, Seq[String]],
    queryString: Map[String, Seq[String]],
    calc: Option[WSSignatureCalculator],
    auth: Option[(String, String, WSAuthScheme)],
    followRedirects: Option[Boolean],
    requestTimeout: Option[Int],
    virtualHost: Option[String],
    proxyServer: Option[WSProxyServer]) extends WSRequestHolder {

  /**
   * sets the signature calculator for the request
   * @param calc
   */
  def sign(calc: WSSignatureCalculator): WSRequestHolder = this.copy(calc = Some(calc))

  /**
   * sets the authentication realm
   */
  def withAuth(username: String, password: String, scheme: WSAuthScheme): WSRequestHolder =
    this.copy(auth = Some((username, password, scheme)))

  /**
   * adds any number of HTTP headers
   * @param hdrs
   */
  def withHeaders(hdrs: (String, String)*): WSRequestHolder = {
    val headers = hdrs.foldLeft(this.headers)((m, hdr) =>
      if (m.contains(hdr._1)) m.updated(hdr._1, m(hdr._1) :+ hdr._2)
      else m + (hdr._1 -> Seq(hdr._2))
    )
    this.copy(headers = headers)
  }

  /**
   * adds any number of query string parameters to the
   */
  def withQueryString(parameters: (String, String)*): WSRequestHolder =
    this.copy(queryString = parameters.foldLeft(queryString) {
      case (m, (k, v)) => m + (k -> (v +: m.get(k).getOrElse(Nil)))
    })

  /**
   * Sets whether redirects (301, 302) should be followed automatically
   */
  def withFollowRedirects(follow: Boolean): WSRequestHolder =
    this.copy(followRedirects = Some(follow))

  @scala.deprecated("use withRequestTimeout instead", "2.1.0")
  def withTimeout(timeout: Int): WSRequestHolder =
    this.withRequestTimeout(timeout)

  /**
   * Sets the maximum time in millisecond you accept the request to take.
   * Warning: a stream consumption will be interrupted when this time is reached.
   */
  def withRequestTimeout(timeout: Int): WSRequestHolder =
    this.copy(requestTimeout = Some(timeout))

  def withVirtualHost(vh: String): WSRequestHolder = {
    this.copy(virtualHost = Some(vh))
  }

  def withProxyServer(proxyServer: WSProxyServer): WSRequestHolder = {
    this.copy(proxyServer = Some(proxyServer))
  }

  /**
   * performs a get with supplied body
   */

  def get(): Future[NingWSResponse] = prepare("GET").execute

  /**
   * performs a get with supplied body
   * @param consumer that's handling the response
   */
  def get[A](consumer: WSResponseHeaders => Iteratee[Array[Byte], A])(implicit ec: ExecutionContext): Future[Iteratee[Array[Byte], A]] =
    prepare("GET").executeStream(consumer)

  /**
   * Perform a PATCH on the request asynchronously.
   */
  def patch[T](body: T)(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): Future[NingWSResponse] = prepare("PATCH", body).execute

  /**
   * Perform a PATCH on the request asynchronously.
   * Request body won't be chunked
   */
  def patch(body: File): Future[NingWSResponse] = prepare("PATCH", body).execute

  /**
   * performs a POST with supplied body
   * @param consumer that's handling the response
   */
  def patchAndRetrieveStream[A, T](body: T)(consumer: WSResponseHeaders => Iteratee[Array[Byte], A])(implicit wrt: Writeable[T], ct: ContentTypeOf[T], ec: ExecutionContext): Future[Iteratee[Array[Byte], A]] = prepare("PATCH", body).executeStream(consumer)

  /**
   * Perform a POST on the request asynchronously.
   */
  def post[T](body: T)(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): Future[NingWSResponse] = prepare("POST", body).execute

  /**
   * Perform a POST on the request asynchronously.
   * Request body won't be chunked
   */
  def post(body: File): Future[NingWSResponse] = prepare("POST", body).execute

  /**
   * performs a POST with supplied body
   * @param consumer that's handling the response
   */
  def postAndRetrieveStream[A, T](body: T)(consumer: WSResponseHeaders => Iteratee[Array[Byte], A])(implicit wrt: Writeable[T], ct: ContentTypeOf[T], ec: ExecutionContext): Future[Iteratee[Array[Byte], A]] = prepare("POST", body).executeStream(consumer)

  /**
   * Perform a PUT on the request asynchronously.
   */
  def put[T](body: T)(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): Future[NingWSResponse] = prepare("PUT", body).execute

  /**
   * Perform a PUT on the request asynchronously.
   * Request body won't be chunked
   */
  def put(body: File): Future[NingWSResponse] = prepare("PUT", body).execute

  /**
   * performs a PUT with supplied body
   * @param consumer that's handling the response
   */
  def putAndRetrieveStream[A, T](body: T)(consumer: WSResponseHeaders => Iteratee[Array[Byte], A])(implicit wrt: Writeable[T], ct: ContentTypeOf[T], ec: ExecutionContext): Future[Iteratee[Array[Byte], A]] = prepare("PUT", body).executeStream(consumer)

  /**
   * Perform a DELETE on the request asynchronously.
   */
  def delete(): Future[NingWSResponse] = prepare("DELETE").execute

  /**
   * Perform a HEAD on the request asynchronously.
   */
  def head(): Future[NingWSResponse] = prepare("HEAD").execute

  /**
   * Perform a OPTIONS on the request asynchronously.
   */
  def options(): Future[NingWSResponse] = prepare("OPTIONS").execute

  /**
   * Execute an arbitrary method on the request asynchronously.
   *
   * @param method The method to execute
   */
  def execute(method: String): Future[WSResponse] = prepare(method).execute

  private[play] def prepare(method: String): NingWSRequest = {
    val request: NingWSRequest = new NingWSRequest(client, method, auth, calc, new RequestBuilder(method)).setUrl(url)
      .setHeaders(headers)
      .setQueryString(queryString)
    followRedirects.map(request.setFollowRedirects)
    requestTimeout.map {
      t: Int =>
        val config = new PerRequestConfig()
        config.setRequestTimeoutInMs(t)
        request.setPerRequestConfig(config)
    }

    virtualHost.map {
      v =>
        request.setVirtualHost(v)
    }

    prepareProxy(request)

    request
  }

  private[play] def prepareProxy(request: NingWSRequest) {
    proxyServer.map {
      p =>
        import com.ning.http.client.ProxyServer.Protocol
        val protocol: Protocol = p.protocol.getOrElse("http").toLowerCase match {
          case "http" => Protocol.HTTP
          case "https" => Protocol.HTTPS
          case "kerberos" => Protocol.KERBEROS
          case "ntlm" => Protocol.NTLM
          case "spnego" => Protocol.SPNEGO
          case _ => scala.sys.error("Unrecognized protocol!")
        }

        val proxyServer = new AHCProxyServer(
          protocol,
          p.host,
          p.port,
          p.principal.getOrElse(null),
          p.password.getOrElse(null))

        p.encoding.map {
          e =>
            proxyServer.setEncoding(e)
        }

        p.nonProxyHosts.map {
          nonProxyHosts =>
            nonProxyHosts.foreach {
              nonProxyHost =>
                proxyServer.addNonProxyHost(nonProxyHost)
            }
        }

        p.ntlmDomain.map {
          ntlm =>
            proxyServer.setNtlmDomain(ntlm)
        }

        request.setProxyServer(proxyServer)
    }
  }

  private[play] def prepare(method: String, body: File): NingWSRequest = {
    import com.ning.http.client.generators.FileBodyGenerator

    val bodyGenerator = new FileBodyGenerator(body)

    val request = new NingWSRequest(client, method, auth, calc, new RequestBuilder(method)).setUrl(url)
      .setHeaders(headers)
      .setQueryString(queryString)
      .setBody(bodyGenerator)
    followRedirects.map(request.setFollowRedirects)
    requestTimeout.map {
      t: Int =>
        val config = new PerRequestConfig()
        config.setRequestTimeoutInMs(t)
        request.setPerRequestConfig(config)
    }
    virtualHost.map {
      v =>
        request.setVirtualHost(v)
    }

    prepareProxy(request)

    request
  }

  private[play] def prepare[T](method: String, body: T)(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): NingWSRequest = {
    val request = new NingWSRequest(client, method, auth, calc, new RequestBuilder(method)).setUrl(url)
      .setHeaders(Map("Content-Type" -> Seq(ct.mimeType.getOrElse("text/plain"))) ++ headers)
      .setQueryString(queryString)
      .setBody(wrt.transform(body))
    followRedirects.map(request.setFollowRedirects)
    requestTimeout.map {
      t: Int =>
        val config = new PerRequestConfig()
        config.setRequestTimeoutInMs(t)
        request.setPerRequestConfig(config)
    }
    virtualHost.map {
      v =>
        request.setVirtualHost(v)
    }
    prepareProxy(request)

    request
  }
}

/**
 * WSPlugin implementation hook.
 */
class NingWSPlugin(app: Application) extends WSPlugin {

  @volatile var loaded = false

  override lazy val enabled = true

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

  private lazy val ningAPI = new NingWSAPI(app)

}

class NingWSAPI(app: Application) extends WSAPI {

  import javax.net.ssl.SSLContext

  private val clientHolder: AtomicReference[Option[NingWSClient]] = new AtomicReference(None)

  private[play] def newClient(): NingWSClient = {
    val playConfig = app.configuration
    val asyncHttpConfig = new AsyncHttpClientConfig.Builder()
      .setConnectionTimeoutInMs(playConfig.getMilliseconds("ws.timeout.connection").getOrElse(120000L).toInt)
      .setIdleConnectionTimeoutInMs(playConfig.getMilliseconds("ws.timeout.idle").getOrElse(120000L).toInt)
      .setRequestTimeoutInMs(playConfig.getMilliseconds("ws.timeout.request").getOrElse(120000L).toInt)
      .setFollowRedirects(playConfig.getBoolean("ws.followRedirects").getOrElse(true))
      .setUseProxyProperties(playConfig.getBoolean("ws.useProxyProperties").getOrElse(true))

    playConfig.getString("ws.useragent").map {
      useragent =>
        asyncHttpConfig.setUserAgent(useragent)
    }
    if (!playConfig.getBoolean("ws.acceptAnyCertificate").getOrElse(false)) {
      asyncHttpConfig.setSSLContext(SSLContext.getDefault)
    }
    new NingWSClient(asyncHttpConfig.build())
  }

  def client: NingWSClient = {
    clientHolder.get.getOrElse({
      // A critical section of code. Only one caller has the opportuntity of creating a new client.
      synchronized {
        clientHolder.get match {
          case None => {
            val client = newClient()
            clientHolder.set(Some(client))
            client
          }
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
   * The maximum age.
   */
  def maxAge: Int = ahcCookie.getMaxAge

  /**
   * If the cookie is secure.
   */
  def secure: Boolean = ahcCookie.isSecure

  /**
   * The cookie version.
   */
  def version: Int = ahcCookie.getVersion

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
    import scala.collection.JavaConverters._
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
