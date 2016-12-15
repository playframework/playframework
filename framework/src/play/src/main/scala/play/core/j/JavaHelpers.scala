/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.j

import java.net.{ InetAddress, URI, URLDecoder }
import java.security.cert.X509Certificate
import java.util.concurrent.CompletionStage

import play.api.http.{ DefaultFileMimeTypesProvider, FileMimeTypes, HttpConfiguration }
import play.api.i18n.{ Langs, MessagesApi, _ }
import play.api.mvc._
import play.api.{ Configuration, Environment }
import play.api.mvc.request.{ RemoteConnection, RequestTarget }
import play.core.Execution.Implicits.trampoline
import play.libs.typedmap.TypedMap
import play.mvc.Http.{ RequestBody, Context => JContext, Cookie => JCookie, Cookies => JCookies, Request => JRequest, RequestHeader => JRequestHeader, RequestImpl => JRequestImpl }
import play.mvc.{ Security, Result => JResult }

import scala.collection.{ JavaConverters, mutable }
import scala.collection.JavaConverters._
import scala.compat.java8.{ FutureConverters, OptionConverters }
import scala.concurrent.Future

/**
 * Provides helper methods that manage Java to Scala Result and Scala to Java Context
 * creation
 */
trait JavaHelpers {

  def cookieToScalaCookie(c: play.mvc.Http.Cookie): Cookie = {
    Cookie(c.name, c.value,
      if (c.maxAge == null) None else Some(c.maxAge), c.path, Option(c.domain), c.secure, c.httpOnly)
  }

  def cookiesToScalaCookies(cookies: java.lang.Iterable[play.mvc.Http.Cookie]): Seq[Cookie] = {
    cookies.asScala.toSeq.map(cookieToScalaCookie(_))
  }

  def cookiesToJavaCookies(cookies: Cookies) = {
    new JCookies {
      def get(name: String): JCookie = {
        cookies.get(name).map(_.asJava).orNull
      }
      def iterator: java.util.Iterator[JCookie] = {
        cookies.toIterator.map(_.asJava).asJava
      }
    }
  }

  def mergeNewCookie(cookies: Cookies, newCookie: Cookie): Cookies = {
    Cookies(CookieHeaderMerging.mergeCookieHeaderCookies(cookies ++ Seq(newCookie)))
  }

  def javaMapToImmutableScalaMap[A, B](m: java.util.Map[A, B]): Map[A, B] = {
    val mapBuilder = Map.newBuilder[A, B]
    val itr = m.entrySet().iterator()
    while (itr.hasNext) {
      val entry = itr.next()
      mapBuilder += (entry.getKey -> entry.getValue)
    }
    mapBuilder.result()
  }

  def javaMapOfArraysToScalaSeqOfPairs(m: java.util.Map[String, Array[String]]): Seq[(String, String)] = {
    for {
      (k, arr) <- JavaConverters.mapAsScalaMap(m).to[Vector]
      el <- arr
    } yield (k, el)
  }

  def scalaMapOfSeqsToJavaMapOfArrays(m: Map[String, Seq[String]]): java.util.Map[String, Array[String]] = {
    val javaMap = new java.util.HashMap[String, Array[String]]()
    for ((k, v) <- m) {
      javaMap.put(k, v.toArray)
    }
    javaMap
  }

  def updateRequestWithUri[A](req: Request[A], parsedUri: URI): Request[A] = {

    // First, update the secure flag for this request, but only if the scheme
    // was set.
    def updateSecure(r: Request[A], newSecure: Boolean): Request[A] = {
      val c = r.connection
      r.withConnection(new RemoteConnection {
        override def remoteAddress: InetAddress = c.remoteAddress
        override def remoteAddressString: String = c.remoteAddressString
        override def secure: Boolean = newSecure
        override def clientCertificateChain: Option[Seq[X509Certificate]] = c.clientCertificateChain
      })
    }
    val reqWithConnection = parsedUri.getScheme match {
      case "http" => updateSecure(req, false)
      case "https" => updateSecure(req, true)
      case _ => req
    }

    // Next create a target based on the URI
    reqWithConnection.withTarget(new RequestTarget {
      override val uri: URI = parsedUri
      override val uriString: String = parsedUri.toString
      override val path: String = parsedUri.getRawPath
      override val queryMap: Map[String, Seq[String]] = {
        val query: String = uri.getRawQuery
        if (query == null || query.length == 0) {
          Map.empty
        } else {
          query.split("&").foldLeft[Map[String, Seq[String]]](Map.empty) {
            case (acc, pair) =>
              val idx: Int = pair.indexOf("=")
              val key: String = if (idx > 0) URLDecoder.decode(pair.substring(0, idx), "UTF-8") else pair
              val value: String = if (idx > 0 && pair.length > idx + 1) URLDecoder.decode(pair.substring(idx + 1), "UTF-8") else null
              acc.get(key) match {
                case None => acc.updated(key, Seq(value))
                case Some(values) => acc.updated(key, values :+ value)
              }
          }
        }
      }
    })
  }

  /**
   * Creates a scala result from java context and result objects
   * @param javaContext
   * @param javaResult
   */
  def createResult(javaContext: JContext, javaResult: JResult): Result = {
    val scalaResult = javaResult.asScala
    val wResult = scalaResult.withHeaders(javaContext.response.getHeaders.asScala.toSeq: _*)
      .withCookies(cookiesToScalaCookies(javaContext.response.cookies): _*)

    if (javaContext.session.isDirty && javaContext.flash.isDirty) {
      wResult.withSession(Session(javaContext.session.asScala.toMap)).flashing(Flash(javaContext.flash.asScala.toMap))
    } else {
      if (javaContext.session.isDirty) {
        wResult.withSession(Session(javaContext.session.asScala.toMap))
      } else {
        if (javaContext.flash.isDirty) {
          wResult.flashing(Flash(javaContext.flash.asScala.toMap))
        } else {
          wResult
        }
      }
    }
  }

  /**
   * Creates a java context from a scala RequestHeader
   * @param req the scala request
   * @param components the context components (use JavaHelpers.createContextComponents)
   */
  def createJavaContext(req: RequestHeader, components: JavaContextComponents): JContext = {
    require(components != null, "Null JavaContextComponents")
    new JContext(
      req.id,
      req,
      new JRequestImpl(req),
      req.session.data.asJava,
      req.flash.data.asJava,
      req.tags.mapValues(_.asInstanceOf[AnyRef]).asJava,
      components
    )
  }

  /**
   * Creates a java context from a scala Request[RequestBody]
   * @param req the scala request
   * @param components the context components (use JavaHelpers.createContextComponents)
   */
  def createJavaContext(req: Request[RequestBody], components: JavaContextComponents): JContext = {
    require(components != null, "Null JavaContextComponents")
    new JContext(
      req.id,
      req,
      new JRequestImpl(req),
      req.session.data.asJava,
      req.flash.data.asJava,
      req.tags.mapValues(_.asInstanceOf[AnyRef]).asJava,
      components
    )
  }

  /**
   * Creates java context components from environment, using
   * Configuration.reference and Environment.simple as defaults.
   *
   * @return an instance of JavaContextComponents.
   */
  def createContextComponents(): JavaContextComponents = {
    val reference: Configuration = play.api.Configuration.reference
    val environment = play.api.Environment.simple()
    createContextComponents(reference, environment)
  }

  /**
   * Creates context components from environment.
   * @param configuration play config.
   * @param env play environment.
   * @return an instance of JavaContextComponents with default messagesApi and langs.
   */
  def createContextComponents(configuration: Configuration, env: Environment): JavaContextComponents = {
    val langs = new DefaultLangsProvider(configuration).get
    val httpConfiguration = HttpConfiguration.fromConfiguration(configuration)
    val messagesApi = new DefaultMessagesApiProvider(env, configuration, langs, httpConfiguration).get
    val fileMimeTypes = new DefaultFileMimeTypesProvider(httpConfiguration.fileMimeTypes).get
    createContextComponents(messagesApi, langs, fileMimeTypes, httpConfiguration)
  }

  /**
   * Creates JavaContextComponents directly from components..
   * @param messagesApi the messagesApi instance
   * @param langs the langs instance
   * @param fileMimeTypes the file mime types
   * @param httpConfiguration the http configuration
   * @return an instance of JavaContextComponents with given input components.
   */
  def createContextComponents(
    messagesApi: MessagesApi,
    langs: Langs,
    fileMimeTypes: FileMimeTypes,
    httpConfiguration: HttpConfiguration): JavaContextComponents = {
    val jMessagesApi = new play.i18n.MessagesApi(messagesApi)
    val jLangs = new play.i18n.Langs(langs)
    val jFileMimeTypes = new play.mvc.FileMimeTypes(fileMimeTypes)
    new DefaultJavaContextComponents(jMessagesApi, jLangs, jFileMimeTypes, httpConfiguration)
  }

  /**
   * Invoke the given function with the right context set, converting the scala request to a
   * Java request, and converting the resulting Java result to a Scala result, before returning
   * it.
   *
   * This is intended for use by methods in the JavaGlobalSettingsAdapter, which need to be handled
   * like Java actions, but are not Java actions. In this case, f may return null, so we wrap its
   * result in an Option. E.g. see the default behavior of GlobalSettings.onError.
   *
   * @param request The request
   * @param components the context components
   * @param f The function to invoke
   * @return The result
   */
  def invokeWithContextOpt(request: RequestHeader, components: JavaContextComponents, f: JRequest => CompletionStage[JResult]): Option[Future[Result]] = {
    val javaContext = createJavaContext(request, components)
    try {
      JContext.current.set(javaContext)
      Option(f(javaContext.request())).map(cs => FutureConverters.toScala(cs).map(createResult(javaContext, _))(trampoline))
    } finally {
      JContext.current.remove()
    }
  }

  /**
   * Invoke the given function with the right context set, converting the scala request to a
   * Java request, and converting the resulting Java result to a Scala result, before returning
   * it.
   *
   * This is intended for use by callback methods in Java adapters.
   *
   * @param request The request
   * @param components the context components
   * @param f The function to invoke
   * @return The result
   */
  def invokeWithContext(request: RequestHeader, components: JavaContextComponents, f: JRequest => CompletionStage[JResult]): Future[Result] = {
    withContext(request, components) { javaContext =>
      FutureConverters.toScala(f(javaContext.request())).map(createResult(javaContext, _))(trampoline)
    }
  }

  /**
   * Invoke the given block with Java context created from the request header
   */
  def withContext[A](request: RequestHeader, components: JavaContextComponents)(block: JContext => A) = {
    val javaContext = createJavaContext(request, components)
    try {
      JContext.current.set(javaContext)
      block(javaContext)
    } finally {
      JContext.current.remove()
    }

  }

}

object JavaHelpers extends JavaHelpers

class RequestHeaderImpl(header: RequestHeader) extends JRequestHeader {

  override def _underlyingHeader: RequestHeader = header

  def uri = header.uri

  def method = header.method

  def version = header.version

  def remoteAddress = header.remoteAddress

  def secure = header.secure

  override def attrs: TypedMap = new TypedMap(header.attrs)
  override def withAttrs(newAttrs: TypedMap): JRequestHeader =
    new RequestHeaderImpl(header.withAttrs(newAttrs.underlying()))

  def withBody(body: RequestBody): JRequest = new JRequestImpl(header.withBody(body))

  def host = header.host

  def path = header.path

  def headers = createHeaderMap(header.headers)

  def acceptLanguages = header.acceptLanguages.map(new play.i18n.Lang(_)).asJava

  def queryString = {
    header.queryString.mapValues(_.toArray).asJava
  }

  def acceptedTypes = header.acceptedTypes.asJava

  def accepts(mediaType: String) = header.accepts(mediaType)

  def cookies = JavaHelpers.cookiesToJavaCookies(header.cookies)

  override def clientCertificateChain() = OptionConverters.toJava(header.clientCertificateChain.map(_.asJava))

  def getQueryString(key: String): String = {
    if (queryString().containsKey(key) && queryString().get(key).length > 0) queryString().get(key)(0) else null
  }

  def cookie(name: String): JCookie = {
    cookies().get(name)
  }

  def getHeader(headerName: String): String = {
    val header: Array[String] = headers.get(headerName)
    if (header == null) null else header(0)
  }

  def hasHeader(headerName: String): Boolean = {
    getHeader(headerName) != null
  }

  def hasBody: Boolean = header.hasBody

  private def createHeaderMap(headers: Headers): java.util.Map[String, Array[String]] = {
    val map = new java.util.TreeMap[String, Array[String]](play.core.utils.CaseInsensitiveOrdered)
    map.putAll(headers.toMap.mapValues(_.toArray).asJava)
    map
  }

  def contentType() = OptionConverters.toJava(header.contentType)

  def charset() = OptionConverters.toJava(header.charset)

  def tags = header.tags.asJava

  def withTag(name: String, value: String) = header.withTag(name, value)

  override def toString = header.toString

}

class RequestImpl(request: Request[RequestBody]) extends RequestHeaderImpl(request) with JRequest {
  override def _underlyingRequest: Request[RequestBody] = request

  override def attrs: TypedMap = new TypedMap(_underlyingHeader.attrs)
  override def withAttrs(newAttrs: TypedMap): JRequest =
    new RequestImpl(request.withAttrs(newAttrs.underlying()))

  override def body: RequestBody = request.body
  override def hasBody: Boolean = request.hasBody
  override def withBody(body: RequestBody): JRequest = new RequestImpl(request.withBody(body))

  override def username: String = attrs().getOptional(Security.USERNAME).orElse(null)
  override def withUsername(username: String): JRequest = withAttrs(attrs.put(Security.USERNAME, username))
}
