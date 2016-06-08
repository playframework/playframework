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
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.Files.TemporaryFile
import play.api.libs.prop.{HasProps, PropMap}
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.Future
import scala.xml.NodeSeq

/**
 * Fake HTTP headers implementation.
 *
 * @param data Headers data.
 */
case class FakeHeaders(data: Seq[(String, String)] = Seq.empty) extends Headers(data)

/**
 * Fake HTTP request implementation.
 */
class FakeRequest[+A](override protected val propBehavior: HasProps.Behavior,
                       override protected val propMap: PropMap) extends RequestLike[A, FakeRequest] with Request[A] with HasProps.WithMapState[FakeRequest[A]] {

  override protected def newState(newMap: PropMap): HasProps.State[FakeRequest[A]] = new FakeRequest[A](propBehavior, newMap)

  @deprecated("Use withProp or another with* method instead", "2.6.0")
  def copyFakeRequest[B](
            id: java.lang.Long = null,
            tags: Map[String, String] = null,
            uri: String = null,
            path: String = null,
            method: String = null,
            version: String = null,
            headers: Headers = null,
            remoteAddress: String = null,
            secure: java.lang.Boolean = null,
            clientCertificateChain: Option[Seq[X509Certificate]] = null,
            body: B = null): FakeRequest[B] = {

    var state: HasProps.State[FakeRequest[A]] = propState
    if (id != null) { state = state.propStateUpdate(RequestHeaderProp.Id, id.longValue()) }
    if (tags != null) { state = state.propStateUpdate(RequestHeaderProp.Tags, tags) }
    if (uri != null) { state = state.propStateUpdate(RequestHeaderProp.Uri, uri) }
    if (path != null) { state = state.propStateUpdate(RequestHeaderProp.Path, path) }
    if (method != null) { state = state.propStateUpdate(RequestHeaderProp.Method, method) }
    if (version != null) { state = state.propStateUpdate(RequestHeaderProp.Version, version) }
    if (headers != null) { state = state.propStateUpdate(RequestHeaderProp.Headers, headers) }
    if (remoteAddress != null) { state = state.propStateUpdate(RequestHeaderProp.RemoteAddress, remoteAddress) }
    if (secure != null) { state = state.propStateUpdate(RequestHeaderProp.Secure, secure.booleanValue()) }
    if (clientCertificateChain != null) { state = state.propStateUpdate(RequestHeaderProp.ClientCertificateChain, clientCertificateChain) }
    if (body != null) { state = state.propStateUpdate(RequestProp.Body[B], body) }
    state.propStateToRepr.asInstanceOf[FakeRequest[B]]
  }

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

  /**
    * Create a FakeRequest.
    *
    * @tparam B The body type.
    * @param method The request HTTP method.
    * @param uri The request uri.
    * @param headers The request HTTP headers.
    * @param body The request body.
    * @param remoteAddress The client IP.
    */
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
    withRoutes: PartialFunction[(String, String), Handler] = PartialFunction.empty) extends Application {

  private val app: Application = new GuiceApplicationBuilder()
    .in(Environment(path, classloader, Mode.Test))
    .configure(additionalConfiguration)
    .routes(withRoutes)
    .build

  override def mode: Mode.Mode = app.mode
  override def configuration: Configuration = app.configuration
  override def actorSystem: ActorSystem = app.actorSystem
  override implicit def materializer: Materializer = app.materializer
  override def requestHandler: HttpRequestHandler = app.requestHandler
  override def errorHandler: HttpErrorHandler = app.errorHandler
  override def stop(): Future[_] = app.stop()
  override def injector: Injector = app.injector
}

