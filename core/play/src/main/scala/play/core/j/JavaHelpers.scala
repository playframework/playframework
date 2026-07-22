/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.util
import java.util.Locale
import java.util.Optional

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

import play.api.http.DefaultFileMimeTypesProvider
import play.api.http.FileMimeTypes
import play.api.http.HttpConfiguration
import play.api.http.MediaRange
import play.api.i18n._
import play.api.i18n.Langs
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.api.mvc.request.RequestTarget
import play.api.mvc.request.Scheme
import play.api.Configuration
import play.api.Environment
import play.i18n
import play.libs.typedmap.TypedEntry
import play.libs.typedmap.TypedKey
import play.libs.typedmap.TypedMap
import play.mvc.Http
import play.mvc.Http.{ Cookie => JCookie }
import play.mvc.Http.{ Cookies => JCookies }
import play.mvc.Http.{ Request => JRequest }
import play.mvc.Http.{ RequestHeader => JRequestHeader }
import play.mvc.Http.{ RequestImpl => JRequestImpl }
import play.mvc.Http.RequestBody

/**
 * Provides helper methods that manage Java to Scala Result and Scala to Java Context
 * creation
 */
trait JavaHelpers {
  def cookiesToScalaCookies(cookies: java.lang.Iterable[play.mvc.Http.Cookie]): Seq[Cookie] = {
    cookies.asScala.toSeq.map(_.asScala())
  }

  def cookiesToJavaCookies(cookies: Cookies): JCookies = {
    new JCookies {
      override def get(name: String): Optional[JCookie] = Optional.ofNullable(cookies.get(name).map(_.asJava).orNull)

      def iterator: java.util.Iterator[JCookie] = cookies.iterator.map(_.asJava).asJava
    }
  }

  def mergeNewCookie(cookies: Cookies, newCookie: Cookie): Cookies = {
    Cookies(CookieHeaderMerging.mergeCookieHeaderCookies(cookies ++ Seq(newCookie)))
  }

  def javaMapToImmutableScalaMap[A, B](m: java.util.Map[A, B]): Map[A, B] = {
    val mapBuilder = Map.newBuilder[A, B]
    val itr        = m.entrySet().iterator()
    while (itr.hasNext) {
      val entry = itr.next()
      mapBuilder += (entry.getKey -> entry.getValue)
    }
    mapBuilder.result()
  }

  def javaMapOfListToScalaSeqOfPairs(m: java.util.Map[String, java.util.List[String]]): Seq[(String, String)] = {
    for {
      (k, arr) <- m.asScala.toVector
      el       <- arr.asScala
    } yield (k, el)
  }

  def javaMapOfArraysToScalaSeqOfPairs(m: java.util.Map[String, Array[String]]): Seq[(String, String)] = {
    for {
      (k, arr) <- m.asScala.toVector
      el       <- arr
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
    req.withTarget(new RequestTarget {
      override val uri: URI                           = parsedUri
      override val uriString: String                  = parsedUri.toString
      override val path: String                       = parsedUri.getRawPath
      override val queryMap: Map[String, Seq[String]] = {
        val query: String = uri.getRawQuery
        if (query == null || query.isEmpty) {
          Map.empty
        } else {
          query.split("&").foldLeft[Map[String, Seq[String]]](Map.empty) {
            case (acc, pair) =>
              val idx: Int      = pair.indexOf("=")
              val key: String   = URLDecoder.decode(if (idx > 0) pair.substring(0, idx) else pair, "UTF-8")
              val value: String =
                if (idx > 0 && pair.length > idx + 1) URLDecoder.decode(pair.substring(idx + 1), "UTF-8") else ""
              acc.get(key) match {
                case None         => acc.updated(key, Seq(value))
                case Some(values) => acc.updated(key, values :+ value)
              }
          }
        }
      }
    })
  }

  /** Replace only the request-target path while preserving its raw authority, query, and fragment. */
  def updateRequestWithPath[A](req: Request[A], path: String): Request[A] = {
    val encodedPath = try {
      new URI(null, null, path, null).getRawPath
    } catch {
      case error: URISyntaxException => throw new IllegalArgumentException("New path couldn't be parsed", error)
    }
    val currentTarget = req.target.uriString
    val suffixStart   = Seq(currentTarget.indexOf('?'), currentTarget.indexOf('#'))
      .filter(_ >= 0)
      .minOption
      .getOrElse(currentTarget.length)
    val schemeSeparator = currentTarget.indexOf("://")
    val absolute        =
      schemeSeparator > 0 && Scheme.parse(currentTarget.substring(0, schemeSeparator)).isRight
    val pathStart = if (absolute) {
      val slash = currentTarget.indexOf('/', schemeSeparator + 3)
      if (slash >= 0 && slash < suffixStart) slash else suffixStart
    } else {
      0
    }

    if (absolute && encodedPath.nonEmpty && !encodedPath.startsWith("/")) {
      throw new IllegalArgumentException("An absolute request target path must be empty or start with '/'")
    }

    val updatedTarget =
      currentTarget.substring(0, pathStart) + encodedPath + currentTarget.substring(suffixStart)
    req.withTarget(req.target.withUriString(updatedTarget).withPath(encodedPath))
  }

  /**
   * Creates java context components from environment, using
   * play.api.Configuration.reference and play.api.Environment.simple as defaults.
   *
   * @return an instance of JavaContextComponents.
   */
  @deprecated("Inject MessagesApi, Langs, FileMimeTypes or HttpConfiguration instead", "2.8.0")
  def createContextComponents(): JavaContextComponents = {
    val reference: Configuration = play.api.Configuration.reference
    val environment              = play.api.Environment.simple()
    createContextComponents(reference, environment)
  }

  /**
   * Creates context components from environment.
   * @param configuration play config.
   * @param env play environment.
   * @return an instance of JavaContextComponents with default messagesApi and langs.
   */
  @deprecated("Inject MessagesApi, Langs, FileMimeTypes or HttpConfiguration instead", "2.8.0")
  def createContextComponents(configuration: Configuration, env: Environment): JavaContextComponents = {
    val langs             = new DefaultLangsProvider(configuration).get
    val httpConfiguration = HttpConfiguration.fromConfiguration(configuration, env)
    val messagesApi       = new DefaultMessagesApiProvider(env, configuration, langs, httpConfiguration).get
    val fileMimeTypes     = new DefaultFileMimeTypesProvider(httpConfiguration.fileMimeTypes).get
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
  @deprecated("Inject MessagesApi, Langs, FileMimeTypes or HttpConfiguration instead", "2.8.0")
  def createContextComponents(
      messagesApi: MessagesApi,
      langs: Langs,
      fileMimeTypes: FileMimeTypes,
      httpConfiguration: HttpConfiguration
  ): JavaContextComponents = {
    val jMessagesApi   = new play.i18n.MessagesApi(messagesApi)
    val jLangs         = new play.i18n.Langs(langs)
    val jFileMimeTypes = new play.mvc.FileMimeTypes(fileMimeTypes)
    new DefaultJavaContextComponents(jMessagesApi, jLangs, jFileMimeTypes, httpConfiguration)
  }
}

object JavaHelpers extends JavaHelpers {
  def javaMapOfListToImmutableScalaMapOfSeq[A, B](javaMap: java.util.Map[A, java.util.List[B]]): Map[A, Seq[B]] = {
    javaMap.asScala.view.mapValues(_.asScala.toSeq).toMap
  }
}

class RequestHeaderImpl(header: RequestHeader) extends JRequestHeader {
  override def asScala: RequestHeader = header

  override def uri: String                                             = header.uri
  override def method: String                                          = header.method
  override def version: String                                         = header.version
  override def scheme: Http.Scheme                                     = header.scheme.asJava
  override def authority: Optional[Http.RequestAuthority]              = header.authority.map(_.asJava).toJava
  override def remote: Http.RemoteInfo                                 = header.remote.asJava
  override def clientCertificate: Optional[Http.ClientCertificateInfo] =
    header.clientCertificate.map(_.asJava).toJava
  override def xForwardedClientCertificates: util.List[Http.XForwardedClientCert] =
    java.util.List.copyOf(header.xForwardedClientCertificates.map(_.asJava).asJava)
  override def secure: Boolean = header.secure

  override def attrs: TypedMap                                                                   = new TypedMap(header.attrs)
  override def withAttrs(newAttrs: TypedMap): JRequestHeader                                     = header.withAttrs(newAttrs.asScala).asJava
  override def addAttr[A](key: TypedKey[A], value: A): JRequestHeader                            = withAttrs(attrs.put(key, value))
  override def addAttrs(e1: TypedEntry[?]): JRequestHeader                                       = withAttrs(attrs.putAll(e1))
  override def addAttrs(e1: TypedEntry[?], e2: TypedEntry[?]): JRequestHeader                    = withAttrs(attrs.putAll(e1, e2))
  override def addAttrs(e1: TypedEntry[?], e2: TypedEntry[?], e3: TypedEntry[?]): JRequestHeader =
    withAttrs(attrs.putAll(e1, e2, e3))
  override def addAttrs(entries: util.List[TypedEntry[?]]): JRequestHeader = withAttrs(attrs.putAll(entries))
  override def removeAttr(key: TypedKey[?]): JRequestHeader                = withAttrs(attrs.remove(key))

  override def withBody(body: RequestBody): JRequest = new JRequestImpl(header.withBody(body))

  override def host: String = header.host
  override def path: String = header.path

  override def acceptLanguages: util.List[i18n.Lang] = header.acceptLanguages.map(new play.i18n.Lang(_)).asJava

  override def queryString: util.Map[String, Array[String]] = header.queryString.view.mapValues(_.toArray).toMap.asJava

  override def acceptedTypes: util.List[MediaRange] = header.acceptedTypes.asJava

  override def accepts(mediaType: String): Boolean = header.accepts(mediaType)

  override def cookies = JavaHelpers.cookiesToJavaCookies(header.cookies)

  @deprecated
  override def getQueryString(key: String): String = {
    if (queryString.containsKey(key) && queryString.get(key).length > 0) queryString.get(key)(0) else null
  }

  override def queryString(key: String): Optional[String] = header.getQueryString(key).toJava

  override def cookie(name: String) = cookies().get(name)

  @deprecated override def getCookie(name: String): Optional[JCookie] = cookie(name)

  override def hasBody: Boolean = header.hasBody

  override def contentType(): Optional[String] = header.contentType.toJava

  override def charset(): Optional[String] = header.charset.toJava

  override def withTransientLang(lang: play.i18n.Lang): JRequestHeader = addAttr(i18n.Messages.Attrs.CurrentLang, lang)

  @deprecated
  override def withTransientLang(code: String): JRequestHeader = withTransientLang(play.i18n.Lang.forCode(code))

  override def withTransientLang(locale: Locale): JRequestHeader = withTransientLang(new play.i18n.Lang(locale))

  override def withoutTransientLang(): JRequestHeader = removeAttr(i18n.Messages.Attrs.CurrentLang)

  override def toString: String = header.toString

  @deprecated
  override lazy val getHeaders: Http.Headers = headers

  override lazy val headers: Http.Headers = header.headers.asJava
}

class RequestImpl(request: Request[RequestBody]) extends RequestHeaderImpl(request) with JRequest {
  override def asScala: Request[RequestBody] = request

  override def attrs: TypedMap                                                             = new TypedMap(asScala.attrs)
  override def withAttrs(newAttrs: TypedMap): JRequest                                     = new JRequestImpl(request.withAttrs(newAttrs.asScala))
  override def addAttr[A](key: TypedKey[A], value: A): JRequest                            = withAttrs(attrs.put(key, value))
  override def addAttrs(e1: TypedEntry[?]): JRequest                                       = withAttrs(attrs.putAll(e1))
  override def addAttrs(e1: TypedEntry[?], e2: TypedEntry[?]): JRequest                    = withAttrs(attrs.putAll(e1, e2))
  override def addAttrs(e1: TypedEntry[?], e2: TypedEntry[?], e3: TypedEntry[?]): JRequest =
    withAttrs(attrs.putAll(e1, e2, e3))
  override def addAttrs(entries: util.List[TypedEntry[?]]): JRequest = withAttrs(attrs.putAll(entries))
  override def removeAttr(key: TypedKey[?]): JRequest                = withAttrs(attrs.remove(key))

  override def body: RequestBody                     = request.body
  override def hasBody: Boolean                      = request.hasBody
  override def withBody(body: RequestBody): JRequest = new JRequestImpl(request.withBody(body))

  override def withTransientLang(lang: play.i18n.Lang): JRequest =
    addAttr(i18n.Messages.Attrs.CurrentLang, lang)
  @deprecated
  override def withTransientLang(code: String): JRequest =
    withTransientLang(play.i18n.Lang.forCode(code))
  override def withTransientLang(locale: Locale): JRequest =
    withTransientLang(new play.i18n.Lang(locale))
  override def withoutTransientLang(): JRequest =
    removeAttr(i18n.Messages.Attrs.CurrentLang)
}
