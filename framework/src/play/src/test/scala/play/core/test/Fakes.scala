/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.test

import java.net.URI
import java.security.cert.X509Certificate

import akka.util.ByteString
import play.api.http.HttpConfiguration
import play.api.libs.Files.{ SingletonTemporaryFileCreator, TemporaryFile }
import play.api.libs.json.JsValue
import play.api.libs.typedmap.{ TypedKey, TypedMap }
import play.api.mvc._
import play.api.mvc.request._
import play.core.parsers.FormUrlEncodedParser

import scala.concurrent.Future
import scala.xml.NodeSeq

/**
 * Fake HTTP headers implementation.
 *
 * @param data Headers data.
 */
case class FakeHeaders(data: Seq[(String, String)] = Seq.empty) extends Headers(data)

/**
 * A `Request` with a few extra methods that are useful for testing.
 *
 * @param request The original request that this `FakeRequest` wraps.
 * @tparam A the body content type.
 */
class FakeRequest[+A](request: Request[A]) extends Request[A] {
  override def connection: RemoteConnection = request.connection
  override def method: String = request.method
  override def target: RequestTarget = request.target
  override def version: String = request.version
  override def headers: Headers = request.headers
  override def body: A = request.body
  override def attrs: TypedMap = request.attrs

  override def withConnection(newConnection: RemoteConnection): FakeRequest[A] =
    new FakeRequest(request.withConnection(newConnection))
  override def withMethod(newMethod: String): FakeRequest[A] =
    new FakeRequest(request.withMethod(newMethod))
  override def withTarget(newTarget: RequestTarget): FakeRequest[A] =
    new FakeRequest(request.withTarget(newTarget))
  override def withVersion(newVersion: String): FakeRequest[A] =
    new FakeRequest(request.withVersion(newVersion))
  override def withHeaders(newHeaders: Headers): FakeRequest[A] =
    new FakeRequest(request.withHeaders(newHeaders))
  override def withAttrs(attrs: TypedMap): FakeRequest[A] =
    new FakeRequest(request.withAttrs(attrs))
  override def addAttr[B](key: TypedKey[B], value: B): FakeRequest[A] =
    withAttrs(attrs.updated(key, value))
  override def withBody[B](body: B): FakeRequest[B] =
    new FakeRequest(request.withBody(body))

  /**
   * Constructs a new request with additional headers. Any existing headers of the same name will be replaced.
   */
  def withHeaders(newHeaders: (String, String)*): FakeRequest[A] = {
    withHeaders(headers.replace(newHeaders: _*))
  }

  /**
   * Constructs a new request with additional Flash.
   */
  def withFlash(data: (String, String)*): FakeRequest[A] = {
    val newFlash = new Flash(flash.data ++ data)
    addAttr(RequestAttrKey.Flash, Cell(newFlash))
  }

  /**
   * Constructs a new request with additional Cookies.
   */
  def withCookies(cookies: Cookie*): FakeRequest[A] = {
    val newCookies: Cookies = Cookies(CookieHeaderMerging.mergeCookieHeaderCookies(this.cookies ++ cookies))
    addAttr(RequestAttrKey.Cookies, Cell(newCookies))
  }

  /**
   * Constructs a new request with additional session.
   */
  def withSession(newSessions: (String, String)*): FakeRequest[A] = {
    val newSession = Session(this.session.data ++ newSessions)
    addAttr(RequestAttrKey.Session, Cell(newSession))
  }

  /**
   * Set a Form url encoded body to this request.
   */
  def withFormUrlEncodedBody(data: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = {
    withBody(body = AnyContentAsFormUrlEncoded(play.utils.OrderPreserving.groupBy(data.toSeq)(_._1)))
  }

  def certs = Future.successful(IndexedSeq.empty)

  /**
   * Adds a JSON body to the request.
   */
  def withJsonBody(json: JsValue): FakeRequest[AnyContentAsJson] = {
    withBody(body = AnyContentAsJson(json))
  }

  /**
   * Adds an XML body to the request.
   */
  def withXmlBody(xml: NodeSeq): FakeRequest[AnyContentAsXml] = {
    withBody(body = AnyContentAsXml(xml))
  }

  /**
   * Adds a text body to the request.
   */
  def withTextBody(text: String): FakeRequest[AnyContentAsText] = {
    withBody(body = AnyContentAsText(text))
  }

  /**
   * Adds a raw body to the request
   */
  def withRawBody(bytes: ByteString): FakeRequest[AnyContentAsRaw] = {
    val tempFileCreator = SingletonTemporaryFileCreator
    withBody(body = AnyContentAsRaw(RawBuffer(bytes.size, tempFileCreator, bytes)))
  }

  /**
   * Adds a multipart form data body to the request
   */
  def withMultipartFormDataBody(form: MultipartFormData[TemporaryFile]) = {
    withBody(body = AnyContentAsMultipartFormData(form))
  }

  /**
   * Returns the current method
   */
  def getMethod: String = method
}

/**
 * Object with helper methods for building [[play.core.test.FakeRequest]] values. This object uses a
 * play.api.mvc.request.DefaultRequestFactory with default configuration to build
 * the requests.
 */
object FakeRequest extends FakeRequestFactory(new DefaultRequestFactory(HttpConfiguration()))

/**
 * Helper methods for building [[FakeRequest]] values.
 *
 * @param requestFactory Used to construct the wrapped requests.
 */
class FakeRequestFactory(requestFactory: RequestFactory) {

  /**
   * Constructs a new GET / fake request.
   */
  def apply(): FakeRequest[AnyContentAsEmpty.type] = {
    apply(method = "GET", uri = "/", headers = FakeHeaders(), body = AnyContentAsEmpty)
  }

  /**
   * Constructs a new request.
   */
  def apply(method: String, path: String): FakeRequest[AnyContentAsEmpty.type] = {
    apply(method = method, uri = path, headers = FakeHeaders(), body = AnyContentAsEmpty)
  }

  def apply(call: Call): FakeRequest[AnyContentAsEmpty.type] = {
    apply(method = call.method, uri = call.url, headers = FakeHeaders(), body = AnyContentAsEmpty)
  }

  def apply[A](
    method: String,
    uri: String,
    headers: Headers,
    body: A,
    remoteAddress: String = "127.0.0.1",
    version: String = "HTTP/1.1",
    id: Long = 666,
    secure: Boolean = false,
    clientCertificateChain: Option[Seq[X509Certificate]] = None,
    attrs: TypedMap = TypedMap.empty): FakeRequest[A] = {

    val _uri = uri
    val request: Request[A] = requestFactory.createRequest(
      RemoteConnection(remoteAddress, secure, clientCertificateChain),
      method,
      new RequestTarget {
        override lazy val uri: URI = new URI(uriString)
        override def uriString: String = _uri
        override lazy val path = uriString.split('?').take(1).mkString
        override lazy val queryMap: Map[String, Seq[String]] = FormUrlEncodedParser.parse(queryString)
      },
      version,
      headers,
      attrs + (RequestAttrKey.Id -> id),
      body
    )
    new FakeRequest(request)
  }

}
