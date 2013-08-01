package play.api.test

import play.api.mvc._
import play.api.libs.json.JsValue
import collection.immutable.TreeMap
import play.core.utils.CaseInsensitiveOrdered
import scala.concurrent.Future
import xml.NodeSeq
import play.core.Router
import scala.runtime.AbstractPartialFunction
import play.api.libs.Files.TemporaryFile

/**
 * Fake HTTP headers implementation.
 *
 * @param data Headers data.
 */
case class FakeHeaders(override val data: Seq[(String, Seq[String])] = Seq.empty) extends Headers

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
case class FakeRequest[A](method: String, uri: String, headers: FakeHeaders, body: A, remoteAddress: String = "127.0.0.1", version: String = "HTTP/1.1", id: Long = 666, tags: Map[String, String] = Map.empty[String, String]) extends Request[A] {

  private def _copy[B](
    id: Long = this.id,
    tags: Map[String, String] = this.tags,
    uri: String = this.uri,
    path: String = this.path,
    method: String = this.method,
    version: String = this.version,
    headers: FakeHeaders = this.headers,
    remoteAddress: String = this.remoteAddress,
    body: B = this.body): FakeRequest[B] = {
    new FakeRequest[B](
      method, uri, headers, body, remoteAddress, version, id, tags
    )
  }

  /**
   * The request path.
   */
  lazy val path = uri.split('?').take(1).mkString

  /**
   * The request query String
   */
  lazy val queryString: Map[String, Seq[String]] = play.core.parsers.FormUrlEncodedParser.parse(rawQueryString)

  /**
   * Constructs a new request with additional headers. Any existing headers of the same name will be replaced.
   */
  def withHeaders(newHeaders: (String, String)*): FakeRequest[A] = {
    _copy(headers = FakeHeaders({
      val newData = newHeaders.map {
        case (k, v) => (k, Seq(v))
      }
      (Map() ++ (headers.data ++ newData)).toSeq
    }))
  }

  /**
   * Constructs a new request with additional Flash.
   */
  def withFlash(data: (String, String)*): FakeRequest[A] = {
    withHeaders(play.api.http.HeaderNames.COOKIE ->
      Cookies.merge(headers.get(play.api.http.HeaderNames.COOKIE).getOrElse(""),
        Seq(Flash.encodeAsCookie(new Flash(flash.data ++ data)))
      )
    )
  }

  /**
   * Constructs a new request with additional Cookies.
   */
  def withCookies(cookies: Cookie*): FakeRequest[A] = {
    withHeaders(play.api.http.HeaderNames.COOKIE ->
      Cookies.merge(headers.get(play.api.http.HeaderNames.COOKIE).getOrElse(""), cookies)
    )
  }

  /**
   * Constructs a new request with additional session.
   */
  def withSession(newSessions: (String, String)*): FakeRequest[A] = {
    withHeaders(play.api.http.HeaderNames.COOKIE ->
      Cookies.merge(headers.get(play.api.http.HeaderNames.COOKIE).getOrElse(""),
        Seq(Session.encodeAsCookie(new Session(session.data ++ newSessions)))
      )
    )
  }

  /**
   * Set a Form url encoded body to this request.
   */
  def withFormUrlEncodedBody(data: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = {
    _copy(body = AnyContentAsFormUrlEncoded(data.groupBy(_._1).mapValues(_.map(_._2))))
  }

  def certs = Future.successful(IndexedSeq.empty)

  /**
   * Sets a JSON body to this request.
   * The content type is set to <tt>application/json</tt>.
   * The method is set to <tt>POST</tt>.
   *
   * @param node the JSON Node.
   * @param _method The request HTTP method, <tt>POST</tt> by default.
   * @return the current fake request
   */
  @deprecated("Use FakeRequest(method, path) to specify the method", "2.1.0")
  def withJsonBody(node: JsValue, _method: String): FakeRequest[AnyContentAsJson] = {
    _copy(method = _method, body = AnyContentAsJson(node))
      .withHeaders(play.api.http.HeaderNames.CONTENT_TYPE -> "application/json")
  }

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
  def withRawBody(bytes: Array[Byte]): FakeRequest[AnyContentAsRaw] = {
    _copy(body = AnyContentAsRaw(RawBuffer(bytes.length, bytes)))
  }

  /**
   * Adds a multipart form data body to the request
   */
  def withMultipartFormDataBody(form: MultipartFormData[TemporaryFile]) = {
    _copy(body = AnyContentAsMultipartFormData(form))
  }

  /**
   * Adds a body to the request.
   */
  def withBody[B](body: B): FakeRequest[B] = {
    _copy(body = body)
  }
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

/**
 * A Fake application.
 *
 * @param path The application path
 * @param classloader The application classloader
 * @param additionalPlugins Additional plugins class names loaded by this application
 * @param withoutPlugins Plugins class names to disable
 * @param additionalConfiguration Additional configuration
 * @param withRoutes A partial function of method name and path to a handler for handling the request
 */

import play.api.{ Application, WithDefaultConfiguration, WithDefaultGlobal, WithDefaultPlugins }
case class FakeApplication(
  override val path: java.io.File = new java.io.File("."),
  override val classloader: ClassLoader = classOf[FakeApplication].getClassLoader,
  val additionalPlugins: Seq[String] = Nil,
  val withoutPlugins: Seq[String] = Nil,
  val additionalConfiguration: Map[String, _ <: Any] = Map.empty,
  val withGlobal: Option[play.api.GlobalSettings] = None,
  val withRoutes: PartialFunction[(String, String), Handler] = PartialFunction.empty) extends {
  override val sources = None
  override val mode = play.api.Mode.Test
} with Application with WithDefaultConfiguration with WithDefaultGlobal with WithDefaultPlugins {
  override def pluginClasses = {
    additionalPlugins ++ super.pluginClasses.diff(withoutPlugins)
  }

  override def configuration = {
    super.configuration ++ play.api.Configuration.from(additionalConfiguration)
  }

  override lazy val global = withGlobal.getOrElse(super.global)

  override lazy val routes: Option[Router.Routes] = {
    val parentRoutes = loadRoutes
    Some(new Router.Routes() {
      def documentation = parentRoutes.map(_.documentation).getOrElse(Nil)
      // Use withRoutes first, then delegate to the parentRoutes if no route is defined
      val routes = new AbstractPartialFunction[RequestHeader, Handler] {
        override def applyOrElse[A <: RequestHeader, B >: Handler](rh: A, default: A => B) =
          withRoutes.applyOrElse((rh.method, rh.path), (_: (String, String)) => default(rh))
        def isDefinedAt(rh: RequestHeader) = withRoutes.isDefinedAt((rh.method, rh.path))
      } orElse new AbstractPartialFunction[RequestHeader, Handler] {
        override def applyOrElse[A <: RequestHeader, B >: Handler](rh: A, default: A => B) =
          parentRoutes.map(_.routes.applyOrElse(rh, default)).getOrElse(default(rh))
        def isDefinedAt(x: RequestHeader) = parentRoutes.map(_.routes.isDefinedAt(x)).getOrElse(false)
      }
      def setPrefix(prefix: String) {
        parentRoutes.foreach(_.setPrefix(prefix))
      }
      def prefix = parentRoutes.map(_.prefix).getOrElse("")
    })
  }
}
