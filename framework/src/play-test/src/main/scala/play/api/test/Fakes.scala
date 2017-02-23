/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.test

import java.security.cert.X509Certificate

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.util.ByteString
import play.api._
import play.api.http._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject._
import play.api.mvc._
import play.api.libs.json.JsValue
import scala.concurrent.Future
import xml.NodeSeq
import play.api.libs.Files.TemporaryFile

/**
 * Fake HTTP headers implementation.
 *
 * @param data Headers data.
 */
case class FakeHeaders(data: Seq[(String, String)] = Seq.empty) extends Headers(data)

/**
 * Fake HTTP request implementation.
 *
 * @tparam A The body type.
 * @param method The request HTTP method.
 * @param uri The request uri.
 * @param headers The request HTTP headers.
 * @param body The request body.
 * @param remoteAddress The client IP.
 */
case class FakeRequest[A](method: String, uri: String, headers: Headers, body: A, remoteAddress: String = "127.0.0.1", version: String = "HTTP/1.1", id: Long = 666, tags: Map[String, String] = Map.empty[String, String], secure: Boolean = false, clientCertificateChain: Option[Seq[X509Certificate]] = None) extends Request[A] {

  def copyFakeRequest[B](
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
    body: B = this.body): FakeRequest[B] = {
    new FakeRequest[B](
      method, uri, headers, body, remoteAddress, version, id, tags, secure, clientCertificateChain
    )
  }

  /**
   * The request path.
   */
  lazy val path = uri.split('?').take(1).mkString

  /**
   * The request query String
   */
  lazy val queryString: Map[String, Seq[String]] =
    play.core.parsers.FormUrlEncodedParser.parse(rawQueryString)

  /**
   * Constructs a new request with additional headers. Any existing headers of the same name will be replaced.
   */
  def withHeaders(newHeaders: (String, String)*): FakeRequest[A] = {
    copyFakeRequest(headers = headers.replace(newHeaders: _*))
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
    copyFakeRequest(body = AnyContentAsFormUrlEncoded(play.utils.OrderPreserving.groupBy(data.toSeq)(_._1)))
  }

  def certs = Future.successful(IndexedSeq.empty)

  /**
   * Adds a JSON body to the request.
   */
  def withJsonBody(json: JsValue): FakeRequest[AnyContentAsJson] = {
    copyFakeRequest(body = AnyContentAsJson(json))
  }

  /**
   * Adds an XML body to the request.
   */
  def withXmlBody(xml: NodeSeq): FakeRequest[AnyContentAsXml] = {
    copyFakeRequest(body = AnyContentAsXml(xml))
  }

  /**
   * Adds a text body to the request.
   */
  def withTextBody(text: String): FakeRequest[AnyContentAsText] = {
    copyFakeRequest(body = AnyContentAsText(text))
  }

  /**
   * Adds a raw body to the request
   */
  def withRawBody(bytes: ByteString): FakeRequest[AnyContentAsRaw] = {
    copyFakeRequest(body = AnyContentAsRaw(RawBuffer(bytes.size, bytes)))
  }

  /**
   * Adds a multipart form data body to the request
   */
  def withMultipartFormDataBody(form: MultipartFormData[TemporaryFile]) = {
    copyFakeRequest(body = AnyContentAsMultipartFormData(form))
  }

  /**
   * Adds a body to the request.
   */
  def withBody[B](body: B): FakeRequest[B] = {
    copyFakeRequest(body = body)
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

import play.api.Application
/**
 * A Fake application.
 *
 * @param path The application path
 * @param classloader The application classloader
 * @param additionalConfiguration Additional configuration
 * @param withRoutes A partial function of method name and path to a handler for handling the request
 */
@deprecated("Use GuiceApplicationBuilder instead.", "2.5.0")
case class FakeApplication(
    override val path: java.io.File = new java.io.File("."),
    override val classloader: ClassLoader = classOf[FakeApplication].getClassLoader,
    additionalConfiguration: Map[String, _ <: Any] = Map.empty,
    @deprecated("Use dependency injection", "2.5.0") withGlobal: Option[GlobalSettings] = None,
    withRoutes: PartialFunction[(String, String), Handler] = PartialFunction.empty) extends Application {

  private val app: Application = new GuiceApplicationBuilder()
    .in(Environment(path, classloader, Mode.Test))
    .global(withGlobal.orNull)
    .configure(additionalConfiguration)
    .routes(withRoutes)
    .build

  override def mode: Mode.Mode = app.mode
  @deprecated("Use dependency injection", "2.5.0") override def global: GlobalSettings = app.global
  override def configuration: Configuration = app.configuration
  override def actorSystem: ActorSystem = app.actorSystem
  override implicit def materializer: Materializer = app.materializer
  override def requestHandler: HttpRequestHandler = app.requestHandler
  override def errorHandler: HttpErrorHandler = app.errorHandler
  override def stop(): Future[_] = app.stop()
  override def injector: Injector = app.injector
}

