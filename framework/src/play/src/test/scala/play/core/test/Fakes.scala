/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.test

import java.security.cert.X509Certificate

import akka.util.ByteString
import com.netaporter.uri.Uri
import play.api.inject.{ Binding, Injector }
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.JsValue
import play.api.libs.typedmap.TypedMap
import play.api.mvc._

import scala.concurrent.Future
import scala.xml.NodeSeq

/**
 * Fake HTTP headers implementation.
 *
 * @param data Headers data.
 */
case class FakeHeaders(data: Seq[(String, String)] = Seq.empty) extends Headers(data)

case class FakeRequest[A](
    method: String,
    uri: String,
    headers: Headers,
    body: A,
    remoteAddress: String = "127.0.0.1",
    version: String = "HTTP/1.1",
    id: Long = 666,
    tags: Map[String, String] = Map.empty[String, String],
    secure: Boolean = false,
    clientCertificateChain: Option[Seq[X509Certificate]] = None,
    attrMap: TypedMap = TypedMap.empty) extends Request[A] with WithAttrMap[FakeRequest[A]] {

  private def _copy[B](
    id: Long = this.id,
    tags: Map[String, String] = this.tags,
    uri: String = this.uri,
    path: String = this.path,
    method: String = this.method,
    version: String = this.version,
    headers: Headers = this.headers,
    remoteAddress: String = this.remoteAddress,
    secure: Boolean = this.secure,
    clientCertificateChain: Option[Seq[X509Certificate]] = this.clientCertificateChain,
    attrMap: TypedMap = this.attrMap,
    body: B = this.body): FakeRequest[B] = {
    new FakeRequest[B](
      method, uri, headers, body, remoteAddress, version, id, tags, secure, clientCertificateChain
    )
  }

  private lazy val parsedUri = Uri.parse(uri)

  /**
   * The request path.
   */
  lazy val path = parsedUri.path

  /**
   * The request query String
   */
  lazy val queryString: Map[String, Seq[String]] = parsedUri.query.paramMap

  /**
   * Constructs a new request with additional headers. Any existing headers of the same name will be replaced.
   */
  def withHeaders(newHeaders: (String, String)*): FakeRequest[A] = {
    _copy(headers = headers.replace(newHeaders: _*))
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
    _copy(body = AnyContentAsFormUrlEncoded(play.utils.OrderPreserving.groupBy(data.toSeq)(_._1)))
  }

  def certs = Future.successful(IndexedSeq.empty)

  /**
   * Adds a JSON body to the request.
   */
  def withJsonBody(json: JsValue): FakeRequest[AnyContentAsJson] = {
    _copy(body = AnyContentAsJson(json))
  }

  /**
   * Adds an XML body to the request.
   */
  def withXmlBody(xml: NodeSeq): FakeRequest[AnyContentAsXml] = {
    _copy(body = AnyContentAsXml(xml))
  }

  /**
   * Adds a text body to the request.
   */
  def withTextBody(text: String): FakeRequest[AnyContentAsText] = {
    _copy(body = AnyContentAsText(text))
  }

  /**
   * Adds a raw body to the request
   */
  def withRawBody(bytes: ByteString): FakeRequest[AnyContentAsRaw] = {
    _copy(body = AnyContentAsRaw(RawBuffer(bytes.size, bytes)))
  }

  /**
   * Adds a multipart form data body to the request
   */
  def withMultipartFormDataBody(form: MultipartFormData[TemporaryFile]) = {
    _copy(body = AnyContentAsMultipartFormData(form))
  }

  override protected def withAttrMap(newAttrMap: TypedMap): FakeRequest[A] = {
    new FakeRequest[A](
      method, uri, headers, body, remoteAddress, version, id, tags, secure, clientCertificateChain, newAttrMap
    )
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

