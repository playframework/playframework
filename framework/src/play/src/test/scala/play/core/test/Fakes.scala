/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.test

import java.security.cert.X509Certificate

import akka.util.ByteString
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.inject.{ Binding, Injector }
import play.api.libs.Files.TemporaryFile
import play.api.libs.prop.{ HasProps, PropMap }
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.Future
import scala.xml.NodeSeq

/**
 * Utilities to help with testing
 */
object Fakes {

  /**
   * Create an injector from the given bindings.
   *
   * @param bindings The bindings
   * @return The injector
   */
  def injectorFromBindings(bindings: Seq[Binding[_]]): Injector = {
    new GuiceInjectorBuilder().bindings(bindings).injector
  }

}

/**
 * Fake HTTP headers implementation.
 *
 * @param data Headers data.
 */
case class FakeHeaders(data: Seq[(String, String)] = Seq.empty) extends Headers(data)

class FakeRequest[+A](override protected val propBehavior: HasProps.Behavior,
    override protected val propMap: PropMap) extends RequestLike[A, FakeRequest] with Request[A] with HasProps.WithMapState[FakeRequest[A]] {

  override protected def newState(newMap: PropMap): HasProps.State[FakeRequest[A]] = new FakeRequest[A](propBehavior, newMap)

  /**
   * The request path.
   */
  override lazy val path = uri.split('?').take(1).mkString // FIXME: Use property

  /**
   * The request query String
   */
  override lazy val queryString: Map[String, Seq[String]] = // FIXME: Use property
    play.core.parsers.FormUrlEncodedParser.parse(rawQueryString)

  /**
   * Constructs a new request with additional headers. Any existing headers of the same name will be replaced.
   */
  def withHeaders(newHeaders: (String, String)*): FakeRequest[A] = {
    withProp(RequestHeaderProp.Headers, prop(RequestHeaderProp.Headers).replace(newHeaders: _*))
  }

  /**
   * Constructs a new request with additional Flash.
   */
  def withFlash(data: (String, String)*): FakeRequest[A] = {
    withHeaders(play.api.http.HeaderNames.COOKIE ->
      Cookies.mergeCookieHeader(headers.get(play.api.http.HeaderNames.COOKIE).getOrElse(""),
        Seq(Flash.encodeAsCookie(new Flash(flash.data ++ data)))
      )
    )
  }

  /**
   * Constructs a new request with additional Cookies.
   */
  def withCookies(cookies: Cookie*): FakeRequest[A] = {
    withHeaders(play.api.http.HeaderNames.COOKIE ->
      Cookies.mergeCookieHeader(headers.get(play.api.http.HeaderNames.COOKIE).getOrElse(""), cookies)
    )
  }

  /**
   * Constructs a new request with additional session.
   */
  def withSession(newSessions: (String, String)*): FakeRequest[A] = {
    withHeaders(play.api.http.HeaderNames.COOKIE ->
      Cookies.mergeCookieHeader(headers.get(play.api.http.HeaderNames.COOKIE).getOrElse(""),
        Seq(Session.encodeAsCookie(new Session(session.data ++ newSessions)))
      )
    )
  }

  /**
   * Set a Form url encoded body to this request.
   */
  def withFormUrlEncodedBody(data: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = {
    withBody(AnyContentAsFormUrlEncoded(play.utils.OrderPreserving.groupBy(data.toSeq)(_._1)))
  }

  def certs = Future.successful(IndexedSeq.empty)

  /**
   * Adds a JSON body to the request.
   */
  def withJsonBody(json: JsValue): FakeRequest[AnyContentAsJson] = {
    withBody(AnyContentAsJson(json))
  }

  /**
   * Adds an XML body to the request.
   */
  def withXmlBody(xml: NodeSeq): FakeRequest[AnyContentAsXml] = {
    withBody(AnyContentAsXml(xml))
  }

  /**
   * Adds a text body to the request.
   */
  def withTextBody(text: String): FakeRequest[AnyContentAsText] = {
    withBody(AnyContentAsText(text))
  }

  /**
   * Adds a raw body to the request
   */
  def withRawBody(bytes: ByteString): FakeRequest[AnyContentAsRaw] = {
    withBody(AnyContentAsRaw(RawBuffer(bytes.size, bytes)))
  }

  /**
   * Adds a multipart form data body to the request
   */
  def withMultipartFormDataBody(form: MultipartFormData[TemporaryFile]) = {
    withBody(AnyContentAsMultipartFormData(form))
  }

  /**
   * Returns the current method
   */
  def getMethod: String = method
}

/**
 * Helper utilities to build FakeRequest values.
 */
object FakeRequest {

  def apply[B](
    method: String,
    uri: String,
    headers: Headers,
    body: B,
    remoteAddress: String = "127.0.0.1",
    version: String = "HTTP/1.1",
    id: Long = 666,
    tags: Map[String, String] = Map.empty[String, String],
    secure: Boolean = false,
    clientCertificateChain: Option[Seq[X509Certificate]] = None): FakeRequest[B] = {
    val propMap = PropMap(
      RequestHeaderProp.Method ~> method,
      RequestHeaderProp.Uri ~> uri,
      RequestHeaderProp.Headers ~> headers,
      RequestProp.Body ~> body,
      RequestHeaderProp.RemoteAddress ~> remoteAddress,
      RequestHeaderProp.Version ~> version,
      RequestHeaderProp.Id ~> id,
      RequestHeaderProp.Tags ~> tags,
      RequestHeaderProp.Secure ~> secure,
      RequestHeaderProp.ClientCertificateChain ~> clientCertificateChain
    )
    new FakeRequest[B](Request.defaultBehavior, propMap)
  }

  /**
   * Constructs a new GET / fake request.
   */
  def apply(): FakeRequest[AnyContentAsEmpty.type] = {
    FakeRequest("GET", "/", FakeHeaders(), AnyContentAsEmpty)
  }

  /**
   * Constructs a new request.
   */
  def apply(method: String, path: String): FakeRequest[AnyContentAsEmpty.type] = {
    FakeRequest(method, path, FakeHeaders(), AnyContentAsEmpty)
  }

  def apply(call: Call): FakeRequest[AnyContentAsEmpty.type] = {
    apply(call.method, call.url)
  }
}

