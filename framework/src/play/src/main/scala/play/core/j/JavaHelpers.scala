/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.j

import java.net.{ InetAddress, URI, URLDecoder }
import java.security.cert.X509Certificate
import java.util
import java.util.Optional
import java.util.concurrent.CompletionStage

import play.api.http.{ DefaultFileMimeTypesProvider, FileMimeTypes, HttpConfiguration, MediaRange }
import play.api.i18n.{ Langs, MessagesApi, _ }
import play.api.mvc._
import play.api.{ Configuration, Environment }
import play.api.mvc.request.{ RemoteConnection, RequestTarget }
import play.core.Execution.Implicits.trampoline
import play.i18n
import play.libs.typedmap.{ TypedKey, TypedMap }
import play.mvc.Http.{ RequestBody, Context => JContext, Cookie => JCookie, Cookies => JCookies, Request => JRequest, RequestHeader => JRequestHeader, RequestImpl => JRequestImpl }
import play.mvc.{ Http, Result => JResult }

import scala.collection.JavaConverters._
import scala.compat.java8.{ FutureConverters, OptionConverters }
import scala.concurrent.Future

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

  def javaMapOfListToScalaSeqOfPairs(m: java.util.Map[String, java.util.List[String]]): Seq[(String, String)] = {
    for {
      (k, arr) <- m.asScala.to[Vector]
      el <- arr.asScala
    } yield (k, el)
  }

  def javaMapOfArraysToScalaSeqOfPairs(m: java.util.Map[String, Array[String]]): Seq[(String, String)] = {
    for {
      (k, arr) <- m.asScala.to[Vector]
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
      case "http" => updateSecure(req, newSecure = false)
      case "https" => updateSecure(req, newSecure = true)
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
   * Creates a scala result from java context/request and result objects
   * @param javaRequest the Java Http.Request
   * @param javaResult the Java Result
   */
  def createResult(javaRequest: JRequest, javaResult: JResult): Result = {
    require(javaResult != null, "Your Action (or some of its compositions) returned a null Result")
    val scalaResult = javaResult.asScala
    val wResult = scalaResult.withHeaders(javaRequest.response.getHeaders.asScala.toSeq: _*)
      .withCookies(cookiesToScalaCookies(javaRequest.response.cookies): _*)

    if (javaRequest.session.isDirty && javaRequest.flash.isDirty) {
      wResult.withSession(Session(javaRequest.session.asScala.toMap)).flashing(Flash(javaRequest.flash.asScala.toMap))
    } else {
      if (javaRequest.session.isDirty) {
        wResult.withSession(Session(javaRequest.session.asScala.toMap))
      } else {
        if (javaRequest.flash.isDirty) {
          wResult.flashing(Flash(javaRequest.flash.asScala.toMap))
        } else {
          wResult
        }
      }
    }
  }

  /**
   * Creates java context components from environment, using
   * play.api.Configuration.reference and play.api.Environment.simple as defaults.
   *
   * @return an instance of JavaRequestComponents.
   */
  def createContextComponents(): JavaRequestComponents = {
    val reference: Configuration = play.api.Configuration.reference
    val environment = play.api.Environment.simple()
    createContextComponents(reference, environment)
  }

  /**
   * Creates context components from environment.
   * @param configuration play config.
   * @param env play environment.
   * @return an instance of JavaRequestComponents with default messagesApi and langs.
   */
  def createContextComponents(configuration: Configuration, env: Environment): JavaRequestComponents = {
    val langs = new DefaultLangsProvider(configuration).get
    val httpConfiguration = HttpConfiguration.fromConfiguration(configuration, env)
    val messagesApi = new DefaultMessagesApiProvider(env, configuration, langs, httpConfiguration).get
    val fileMimeTypes = new DefaultFileMimeTypesProvider(httpConfiguration.fileMimeTypes).get
    createContextComponents(messagesApi, langs, fileMimeTypes, httpConfiguration)
  }

  /**
   * Creates JavaRequestComponents directly from components..
   * @param messagesApi the messagesApi instance
   * @param langs the langs instance
   * @param fileMimeTypes the file mime types
   * @param httpConfiguration the http configuration
   * @return an instance of JavaRequestComponents with given input components.
   */
  def createContextComponents(
    messagesApi: MessagesApi,
    langs: Langs,
    fileMimeTypes: FileMimeTypes,
    httpConfiguration: HttpConfiguration): JavaRequestComponents = {
    val jMessagesApi = new play.i18n.MessagesApi(messagesApi)
    val jLangs = new play.i18n.Langs(langs)
    val jFileMimeTypes = new play.mvc.FileMimeTypes(fileMimeTypes)
    new DefaultJavaRequestComponents(jMessagesApi, jLangs, jFileMimeTypes, httpConfiguration)
  }

  /**
   * Invoke the given function with the right java request set, converting the scala request to a
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
  def invokeWithRequest(request: RequestHeader, components: JavaRequestComponents, f: JRequest => CompletionStage[JResult]): Future[Result] = {
    withRequest(request, components) { javaRequest =>
      FutureConverters.toScala(f(javaRequest)).map(createResult(javaRequest, _))(trampoline)
    }
  }

  /**
   * Invoke the given block with Java request created from the scala request header
   */
  def withRequest[A](request: RequestHeader, components: JavaRequestComponents)(block: JRequest => A) = {
    val javaRequest = new JRequestImpl(request, components)
    try {
      JContext.current.set(javaRequest)
      block(javaRequest)
    } finally {
      JContext.current.remove()
    }

  }

}

object JavaHelpers extends JavaHelpers

class RequestHeaderImpl(header: RequestHeader) extends JRequestHeader {

  override def asScala: RequestHeader = header

  override def uri: String = header.uri

  override def method: String = header.method

  override def version: String = header.version

  override def remoteAddress: String = header.remoteAddress

  override def secure: Boolean = header.secure

  override def attrs: TypedMap = new TypedMap(header.attrs)
  override def withAttrs(newAttrs: TypedMap): JRequestHeader = header.withAttrs(newAttrs.underlying()).asJava
  override def addAttr[A](key: TypedKey[A], value: A): JRequestHeader = withAttrs(attrs.put(key, value))

  override def withBody(body: RequestBody, components: JavaRequestComponents): JRequest = new JRequestImpl(header.withBody(body), components)

  override def host: String = header.host

  override def path: String = header.path

  override def acceptLanguages: util.List[i18n.Lang] = header.acceptLanguages.map(new play.i18n.Lang(_)).asJava

  override def queryString: util.Map[String, Array[String]] = header.queryString.mapValues(_.toArray).asJava

  override def acceptedTypes: util.List[MediaRange] = header.acceptedTypes.asJava

  override def accepts(mediaType: String): Boolean = header.accepts(mediaType)

  override def cookies = JavaHelpers.cookiesToJavaCookies(header.cookies)

  override def clientCertificateChain() = OptionConverters.toJava(header.clientCertificateChain.map(_.asJava))

  override def getQueryString(key: String): String = {
    if (queryString().containsKey(key) && queryString().get(key).length > 0) queryString().get(key)(0) else null
  }

  override def cookie(name: String): JCookie = {
    cookies().get(name)
  }

  override def hasBody: Boolean = header.hasBody

  override def contentType(): Optional[String] = OptionConverters.toJava(header.contentType)

  override def charset(): Optional[String] = OptionConverters.toJava(header.charset)

  override def toString: String = header.toString

  override lazy val getHeaders: Http.Headers = header.headers.asJava

}

class RequestImpl(request: Request[RequestBody], components: JavaRequestComponents) extends RequestHeaderImpl(request) with JRequest {
  override def asScala: Request[RequestBody] = request

  override def attrs: TypedMap = new TypedMap(asScala.attrs)
  override def withAttrs(newAttrs: TypedMap): JRequest =
    new RequestImpl(request.withAttrs(newAttrs.underlying()), components)
  override def addAttr[A](key: TypedKey[A], value: A): JRequest =
    withAttrs(attrs.put(key, value))

  override def body: RequestBody = request.body
  override def hasBody: Boolean = request.hasBody
  override def withBody(body: RequestBody): JRequest = new RequestImpl(request.withBody(body), components)

  def changeLang(lang: play.i18n.Lang): Boolean = ???
  def changeLang(code: String): Boolean = ???
  def clearLang(): Unit = ???
  def clearTransientLang(): Unit = ???
  def fileMimeTypes(): play.mvc.FileMimeTypes = ???
  def flash(): play.mvc.Http.Flash = ???
  def id(): java.lang.Long = null
  def lang(): play.i18n.Lang = ???
  def messages(): play.i18n.Messages = ???
  def request(): play.mvc.Http.Request = ???
  def response(): play.mvc.Http.Response = ???
  def session(): play.mvc.Http.Session = ???
  def setTransientLang(lang: play.i18n.Lang): Unit = ???
  def setTransientLang(code: String): Unit = ???
  def args(): java.util.Map[String, Object] = ???

}
