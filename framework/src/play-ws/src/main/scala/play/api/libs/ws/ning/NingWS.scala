/*
  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.ws.ning

import java.io.UnsupportedEncodingException
import java.nio.charset.{ Charset, StandardCharsets }
import javax.inject.{ Inject, Provider, Singleton }

import com.ning.http.client.{ Response => AHCResponse, ProxyServer => AHCProxyServer, _ }
import com.ning.http.client.cookie.{ Cookie => AHCCookie }
import com.ning.http.client.Realm.{ RealmBuilder, AuthScheme }
import com.ning.http.util.AsyncHttpProviderUtils
import org.jboss.netty.handler.codec.http.HttpHeaders
import play.api.inject.{ ApplicationLifecycle, Module }
import play.core.parsers.FormUrlEncodedParser

import collection.immutable.TreeMap

import scala.concurrent.{ Future, Promise }

import play.api.libs.ws._
import play.api.libs.ws.ssl._

import play.api.libs.iteratee._
import play.api._
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
case class NingWSClient(config: AsyncHttpClientConfig) extends WSClient {

  private val asyncHttpClient = new AsyncHttpClient(config)

  def underlying[T] = asyncHttpClient.asInstanceOf[T]

  private[libs] def executeRequest[T](request: Request, handler: AsyncHandler[T]): ListenableFuture[T] = asyncHttpClient.executeRequest(request, handler)

  def close() = asyncHttpClient.close()

  def url(url: String): WSRequest = NingWSRequest(this, url, "GET", EmptyBody, Map(), Map(), None, None, None, None, None, None, None)
}

object NingWSClient {
  /**
   * Convenient factory method that uses a [[WSClientConfig]] value for configuration instead of an [[AsyncHttpClientConfig]].
   *
   * Typical usage:
   *
   * {{{
   *   val client = NingWSClient()
   *   val request = client.url(someUrl).get()
   *   request.foreach { response =>
   *     doSomething(response)
   *     client.close()
   *   }
   * }}}
   *
   * @param config configuration settings
   */
  def apply(config: NingWSClientConfig = NingWSClientConfig()): NingWSClient = {
    val client = new NingWSClient(new NingAsyncHttpClientConfigBuilder(config).build())
    new SystemConfiguration().configure(config.wsClientConfig)
    client
  }
}

/**
 * A Ning WS Request.
 */
case class NingWSRequest(client: NingWSClient,
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
    disableUrlEncoding: Option[Boolean]) extends WSRequest {

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
      case (m, (k, v)) => m + (k -> (v +: m.get(k).getOrElse(Nil)))
    })

  def withFollowRedirects(follow: Boolean): WSRequest = copy(followRedirects = Some(follow))

  def withRequestTimeout(timeout: Long): WSRequest = {
    require(timeout >= 0 && timeout <= Int.MaxValue, s"Request timeout must be between 0 and ${Int.MaxValue}")
    copy(requestTimeout = Some(timeout.toInt))
  }

  def withVirtualHost(vh: String): WSRequest = copy(virtualHost = Some(vh))

  def withProxyServer(proxyServer: WSProxyServer): WSRequest = copy(proxyServer = Some(proxyServer))

  def withBody(body: WSBody): WSRequest = copy(body = body)

  def withMethod(method: String): WSRequest = copy(method = method)

  def execute(): Future[WSResponse] = execute(buildRequest())

  def stream(): Future[(WSResponseHeaders, Enumerator[Array[Byte]])] = executeStream(buildRequest())

  /**
   * Returns the current headers of the request, using the request builder.  This may be signed,
   * so may return extra headers that were not directly input.
   */
  def requestHeaders: Map[String, Seq[String]] = ningHeadersToMap(buildRequest().getHeaders)

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
  def getBody: Option[Array[Byte]] = {
    body match {
      case InMemoryBody(bytes) => Some(bytes)
      case _ => None
    }
  }

  private[libs] def authScheme(scheme: WSAuthScheme): AuthScheme = scheme match {
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
  private[libs] def auth(username: String, password: String, scheme: AuthScheme = AuthScheme.BASIC): Realm = {
    (new RealmBuilder)
      .setScheme(scheme)
      .setPrincipal(username)
      .setPassword(password)
      .setUsePreemptiveAuth(true)
      .build()
  }

  private[libs] def ningHeadersToMap(headers: FluentCaseInsensitiveStringsMap) = {
    val res = mapAsScalaMapConverter(headers).asScala.map(e => e._1 -> e._2.asScala.toSeq).toMap
    //todo: wrap the case insensitive ning map instead of creating a new one (unless perhaps immutabilty is important)
    TreeMap(res.toSeq: _*)(CaseInsensitiveOrdered)
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
    followRedirects.foreach(builder.setFollowRedirects)
    proxyServer.foreach(p => builder.setProxyServer(createProxy(p)))
    requestTimeout.foreach(builder.setRequestTimeout)

    // Set the body.
    var possiblyModifiedHeaders = this.headers
    val builderWithBody = body match {
      case EmptyBody => builder
      case FileBody(file) =>
        import com.ning.http.client.generators.FileBodyGenerator
        val bodyGenerator = new FileBodyGenerator(file)
        builder.setBody(bodyGenerator)
      case InMemoryBody(bytes) =>
        // extract the content type and the charset
        val ct: String = contentType.getOrElse("text/plain")
        val charset = Charset.forName(
          Option(AsyncHttpProviderUtils.parseCharset(ct)).getOrElse {
            // NingWSRequest modifies headers to include the charset, but this fails tests in Scala.
            //val contentTypeList = Seq(ct + "; charset=utf-8")
            //possiblyModifiedHeaders = this.headers.updated(HttpHeaders.Names.CONTENT_TYPE, contentTypeList)
            "utf-8"
          }
        )

        try {
          // Get the string body given the given charset...
          val stringBody = new String(bytes, charset)
          // The Ning signature calculator uses request.getFormParams() for calculation,
          // so we have to parse it out and add it rather than using setBody.
          if (ct.contains(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED)) {
            val params = for {
              (key, values) <- FormUrlEncodedParser.parse(stringBody).toSeq
              value <- values
            } yield new Param(key, value)
            builder.setFormParams(params.asJava)
          } else {
            builder.setBody(stringBody)
          }
        } catch {
          case e: UnsupportedEncodingException =>
            throw new RuntimeException(e)
        }

        builder
      case StreamedBody(bytes) =>
        builder
    }

    // headers
    for {
      header <- possiblyModifiedHeaders
      value <- header._2
    } builder.addHeader(header._1, value)

    // Set the signature calculator.
    calc.map {
      case signatureCalculator: com.ning.http.client.SignatureCalculator =>
        builderWithBody.setSignatureCalculator(signatureCalculator)
      case _ =>
        throw new IllegalStateException("Unknown signature calculator found: use a class that implements SignatureCalculator")
    }

    builderWithBody.build()
  }

  private[libs] def execute(request: Request): Future[NingWSResponse] = {

    import com.ning.http.client.AsyncCompletionHandler
    val result = Promise[NingWSResponse]()

    client.executeRequest(request, new AsyncCompletionHandler[AHCResponse]() {
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

  private[libs] def executeStream(request: Request): Future[(WSResponseHeaders, Enumerator[Array[Byte]])] = {

    import com.ning.http.client.AsyncHandler
    import play.api.libs.concurrent.Execution.Implicits.defaultContext

    val result = Promise[(WSResponseHeaders, Enumerator[Array[Byte]])]()

    val errorInStream = Promise[Unit]()

    val promisedIteratee = Promise[Iteratee[Array[Byte], Unit]]()

    @volatile var doneOrError = false
    @volatile var statusCode = 0
    @volatile var current: Iteratee[Array[Byte], Unit] = Iteratee.flatten(promisedIteratee.future)

    client.executeRequest(request, new AsyncHandler[Unit]() {

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
            val mapped = i.map {
              a =>
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
          bodyPart.markUnderlyingConnectionAsToBeClosed()
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
  }

  private[libs] def createProxy(wsProxyServer: WSProxyServer): AHCProxyServer = {

    import com.ning.http.client.ProxyServer.Protocol

    val protocol: Protocol = wsProxyServer.protocol.getOrElse("http").toLowerCase match {
      case "http" => Protocol.HTTP
      case "https" => Protocol.HTTPS
      case "kerberos" => Protocol.KERBEROS
      case "ntlm" => Protocol.NTLM
      case "spnego" => Protocol.SPNEGO
      case _ => scala.sys.error("Unrecognized protocol!")
    }

    val ningProxyServer = new AHCProxyServer(
      protocol,
      wsProxyServer.host,
      wsProxyServer.port,
      wsProxyServer.principal.orNull,
      wsProxyServer.password.orNull)

    wsProxyServer.encoding.foreach(enc => ningProxyServer.setCharset(Charset.forName(enc)))
    wsProxyServer.ntlmDomain.foreach(ningProxyServer.setNtlmDomain)
    for {
      hosts <- wsProxyServer.nonProxyHosts
      host <- hosts
    } ningProxyServer.addNonProxyHost(host)

    ningProxyServer
  }

}

class NingWSModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[WSAPI].to[NingWSAPI],
      bind[NingWSClientConfig].toProvider[NingWSClientConfigParser].in[Singleton],
      bind[WSClientConfig].toProvider[WSConfigParser].in[Singleton],
      bind[WSClient].toProvider[WSClientProvider].in[Singleton]
    )
  }
}

class WSClientProvider @Inject() (wsApi: WSAPI) extends Provider[WSClient] {
  def get() = wsApi.client
}

@Singleton
class NingWSAPI @Inject() (environment: Environment, clientConfig: NingWSClientConfig, lifecycle: ApplicationLifecycle) extends WSAPI {

  private val logger = Logger(classOf[NingWSAPI])

  lazy val client = {
    if (clientConfig.wsClientConfig.ssl.debug.enabled) {
      environment.mode match {
        case Mode.Prod =>
          logger.warn("NingWSAPI: ws.ssl.debug settings enabled in production mode!")
        case _ => // do nothing
      }
      new DebugConfiguration().configure(clientConfig.wsClientConfig.ssl.debug)
    }

    val client = NingWSClient(clientConfig)

    lifecycle.addStopHook { () =>
      Future.successful(client.close())
    }
    client
  }

  def url(url: String) = client.url(url)

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

  import play.api.libs.json._

  import scala.xml._

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
    val charset: String = Option(AsyncHttpProviderUtils.parseCharset(contentType)).getOrElse {
      if (contentType.startsWith("text/"))
        AsyncHttpProviderUtils.DEFAULT_CHARSET.toString
      else
        StandardCharsets.UTF_8.toString
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
   * The response body as a byte array.
   */
  def bodyAsBytes: Array[Byte] = ahcResponse.getResponseBodyAsBytes

  override def toString: String = {
    s"NingWSResponse($status, $statusText)"
  }
}

/**
 * Ning WS API implementation components.
 */
trait NingWSComponents {

  def environment: Environment
  def configuration: Configuration
  def applicationLifecycle: ApplicationLifecycle

  lazy val wsClientConfig: WSClientConfig = new WSConfigParser(configuration, environment).parse()
  lazy val ningWsClientConfig: NingWSClientConfig =
    new NingWSClientConfigParser(wsClientConfig, configuration, environment).parse()
  lazy val wsApi: WSAPI = new NingWSAPI(environment, ningWsClientConfig, applicationLifecycle)
  lazy val wsClient: WSClient = wsApi.client
}
