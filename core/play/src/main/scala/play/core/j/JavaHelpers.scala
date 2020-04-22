/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import java.net.InetAddress
import java.net.URI
import java.net.URLDecoder
import java.security.cert.X509Certificate
import java.util
import java.util.Locale
import java.util.Optional

import play.api.http.DefaultFileMimeTypesProvider
import play.api.http.FileMimeTypes
import play.api.http.HttpConfiguration
import play.api.http.MediaRange
import play.api.i18n.Langs
import play.api.i18n.MessagesApi
import play.api.i18n._
import play.api.mvc._
import play.api.Configuration
import play.api.Environment
import play.api.mvc.request.RemoteConnection
import play.api.mvc.request.RequestTarget
import play.i18n
import play.libs.typedmap.TypedEntry
import play.libs.typedmap.TypedKey
import play.libs.typedmap.TypedMap
import play.mvc.Http.RequestBody
import play.mvc.Http.{ Cookie => JCookie }
import play.mvc.Http.{ Cookies => JCookies }
import play.mvc.Http.{ Request => JRequest }
import play.mvc.Http.{ RequestHeader => JRequestHeader }
import play.mvc.Http.{ RequestImpl => JRequestImpl }
import play.mvc.Http

import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters

/**
 * Provides helper methods that manage Java to Scala Result and Scala to Java Context
 * creation
 */
trait JavaHelpers {
  def cookiesToScalaCookies(cookies: java.lang.Iterable[play.mvc.Http.Cookie]): Seq[Cookie] = {
    cookies.asScala.toSeq.map(_.asScala())
  }

  def cookiesToJavaCookies(cookies: Cookies) = {
    new JCookies {
      override def get(name: String): Optional[JCookie] = Optional.ofNullable(cookies.get(name).map(_.asJava).orNull)

      def iterator: java.util.Iterator[JCookie] = cookies.toIterator.map(_.asJava).asJava
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
    // First, update the secure flag for this request, but only if the scheme
    // was set.
    def updateSecure(r: Request[A], newSecure: Boolean): Request[A] = {
      val c = r.connection
      r.withConnection(new RemoteConnection {
        override def remoteAddress: InetAddress                           = c.remoteAddress
        override def remoteAddressString: String                          = c.remoteAddressString
        override def secure: Boolean                                      = newSecure
        override def clientCertificateChain: Option[Seq[X509Certificate]] = c.clientCertificateChain
      })
    }
    val reqWithConnection = parsedUri.getScheme match {
      case "http"  => updateSecure(req, newSecure = false)
      case "https" => updateSecure(req, newSecure = true)
      case _       => req
    }

    // Next create a target based on the URI
    reqWithConnection.withTarget(new RequestTarget {
      override val uri: URI          = parsedUri
      override val uriString: String = parsedUri.toString
      override val path: String      = parsedUri.getRawPath
      override val queryMap: Map[String, Seq[String]] = {
        val query: String = uri.getRawQuery
        if (query == null || query.length == 0) {
          Map.empty
        } else {
          query.split("&").foldLeft[Map[String, Seq[String]]](Map.empty) {
            case (acc, pair) =>
              val idx: Int    = pair.indexOf("=")
              val key: String = if (idx > 0) URLDecoder.decode(pair.substring(0, idx), "UTF-8") else pair
              val value: String =
                if (idx > 0 && pair.length > idx + 1) URLDecoder.decode(pair.substring(idx + 1), "UTF-8") else null
              acc.get(key) match {
                case None         => acc.updated(key, Seq(value))
                case Some(values) => acc.updated(key, values :+ value)
              }
          }
        }
      }
    })
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
    javaMap.asScala.mapValues(_.asScala.toSeq).toMap
  }
}

class RequestHeaderImpl(header: RequestHeader) extends JRequestHeader {
  override def asScala: RequestHeader = header

  override def uri: String           = header.uri
  override def method: String        = header.method
  override def version: String       = header.version
  override def remoteAddress: String = header.remoteAddress
  override def secure: Boolean       = header.secure

  override def attrs: TypedMap                                                = new TypedMap(header.attrs)
  override def withAttrs(newAttrs: TypedMap): JRequestHeader                  = header.withAttrs(newAttrs.asScala).asJava
  override def addAttr[A](key: TypedKey[A], value: A): JRequestHeader         = withAttrs(attrs.put(key, value))
  override def addAttrs(e1: TypedEntry[_]): JRequestHeader                    = withAttrs(attrs.putAll(e1))
  override def addAttrs(e1: TypedEntry[_], e2: TypedEntry[_]): JRequestHeader = withAttrs(attrs.putAll(e1, e2))
  override def addAttrs(e1: TypedEntry[_], e2: TypedEntry[_], e3: TypedEntry[_]): JRequestHeader =
    withAttrs(attrs.putAll(e1, e2, e3))
  override def addAttrs(entries: TypedEntry[_]*): JRequestHeader           = withAttrs(attrs.putAll(entries: _*))
  override def addAttrs(entries: util.List[TypedEntry[_]]): JRequestHeader = withAttrs(attrs.putAll(entries))
  override def removeAttr(key: TypedKey[_]): JRequestHeader                = withAttrs(attrs.remove(key))

  override def withBody(body: RequestBody): JRequest = new JRequestImpl(header.withBody(body))

  override def host: String = header.host
  override def path: String = header.path

  override def acceptLanguages: util.List[i18n.Lang] = header.acceptLanguages.map(new play.i18n.Lang(_)).asJava

  override def queryString: util.Map[String, Array[String]] = header.queryString.mapValues(_.toArray).toMap.asJava

  override def acceptedTypes: util.List[MediaRange] = header.acceptedTypes.asJava

  override def accepts(mediaType: String): Boolean = header.accepts(mediaType)

  override def cookies = JavaHelpers.cookiesToJavaCookies(header.cookies)

  override def clientCertificateChain() = OptionConverters.toJava(header.clientCertificateChain.map(_.asJava))

  @deprecated
  override def getQueryString(key: String): String = {
    if (queryString().containsKey(key) && queryString().get(key).length > 0) queryString().get(key)(0) else null
  }

  override def queryString(key: String): Optional[String] = OptionConverters.toJava(header.getQueryString(key))

  @deprecated override def cookie(name: String): JCookie = cookies().get(name).orElse(null)

  override def getCookie(name: String): Optional[JCookie] = cookies().get(name)

  override def hasBody: Boolean = header.hasBody

  override def contentType(): Optional[String] = OptionConverters.toJava(header.contentType)

  override def charset(): Optional[String] = OptionConverters.toJava(header.charset)

  override def withTransientLang(lang: play.i18n.Lang): JRequestHeader = addAttr(i18n.Messages.Attrs.CurrentLang, lang)

  @deprecated
  override def withTransientLang(code: String): JRequestHeader = withTransientLang(play.i18n.Lang.forCode(code))

  override def withTransientLang(locale: Locale): JRequestHeader = withTransientLang(new play.i18n.Lang(locale))

  override def withoutTransientLang(): JRequestHeader = removeAttr(i18n.Messages.Attrs.CurrentLang)

  override def toString: String = header.toString

  override lazy val getHeaders: Http.Headers = header.headers.asJava
}

/**
 * trait needed as workaround for https://github.com/scala/bug/issues/11944
 * Also see original pull request: https://github.com/playframework/playframework/pull/10199
 * sealed so that lack of implementation can't be accidentally used elsewhere
 */
private[j] sealed trait RequestImplHelper extends JRequest {
  override def addAttrs(entries: TypedEntry[_]*): JRequest = ???
}

class RequestImpl(request: Request[RequestBody]) extends RequestHeaderImpl(request) with RequestImplHelper {
  override def asScala: Request[RequestBody] = request

  override def attrs: TypedMap                                          = new TypedMap(asScala.attrs)
  override def withAttrs(newAttrs: TypedMap): JRequest                  = new JRequestImpl(request.withAttrs(newAttrs.asScala))
  override def addAttr[A](key: TypedKey[A], value: A): JRequest         = withAttrs(attrs.put(key, value))
  override def addAttrs(e1: TypedEntry[_]): JRequest                    = withAttrs(attrs.putAll(e1))
  override def addAttrs(e1: TypedEntry[_], e2: TypedEntry[_]): JRequest = withAttrs(attrs.putAll(e1, e2))
  override def addAttrs(e1: TypedEntry[_], e2: TypedEntry[_], e3: TypedEntry[_]): JRequest =
    withAttrs(attrs.putAll(e1, e2, e3))
  override def addAttrs(entries: TypedEntry[_]*): JRequest           = withAttrs(attrs.putAll(entries: _*))
  override def addAttrs(entries: util.List[TypedEntry[_]]): JRequest = withAttrs(attrs.putAll(entries))
  override def removeAttr(key: TypedKey[_]): JRequest                = withAttrs(attrs.remove(key))

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
