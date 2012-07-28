package play.api.test;

import play.api.mvc._
import org.codehaus.jackson.JsonNode
import play.api.libs.json.JsValue

/**
 * Fake HTTP headers implementation.
 *
 * @param data Headers data.
 */
case class FakeHeaders(data: Map[String, Seq[String]] = Map.empty) extends Headers {

  /**
   * All header keys.
   */
  lazy val keys = data.keySet

  /**
   * Get all header values defined for this key.
   */
  def getAll(key: String): Seq[String] = data.get(key).getOrElse(Seq.empty)

}

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
case class FakeRequest[A](method: String, uri: String, headers: FakeHeaders, body: A, remoteAddress: String = "127.0.0.1") extends Request[A] {

  /**
   * The request path.
   */
  lazy val path = uri.split('?').take(1).mkString

  /**
   * The request query String
   */
  lazy val queryString: Map[String, Seq[String]] = play.core.parsers.FormUrlEncodedParser.parse(rawQueryString)

  /**
   * Constructs a new request with additional headers.
   */
  def withHeaders(newHeaders: (String, String)*): FakeRequest[A] = {
    copy(headers = FakeHeaders(
      headers.data ++ newHeaders.groupBy(_._1).mapValues(_.map(_._2))
    ))
  }

 /**
  * Constructs a new request with additional Flash.
  */ 
 def withFlash(data: (String, String)*): FakeRequest[A] = {
    withHeaders(play.api.http.HeaderNames.COOKIE ->
      Cookies.merge(headers.get(play.api.http.HeaderNames.COOKIE).getOrElse(""), 
          Seq(Flash.encodeAsCookie(new Flash (flash.data ++ data)))
      )
    )
  }

  /**
  * Constructs a new request with additional Cookies.
  */ 
  def withCookies(cookies: Cookie*): FakeRequest[A] = {
    withHeaders(play.api.http.HeaderNames.COOKIE ->
      Cookies.merge(headers.get(play.api.http.HeaderNames.COOKIE).getOrElse(""), cookies )
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
    copy(body = AnyContentAsFormUrlEncoded(data.groupBy(_._1).mapValues(_.map(_._2))))
  }

  /**
   * Sets a JSON body to this request.
   * The content type is set to <tt>application/json</tt>.
   * The method is set to <tt>POST</tt>.
   *
   * @param node the JSON Node.
   * @param _method The request HTTP method, <tt>POST</tt> by default.
   * @return the current fake request
   */
  def withJsonBody(node: JsValue,  _method: String = Helpers.POST): FakeRequest[AnyContentAsJson] = {
    copy(method = _method, body = AnyContentAsJson(node))
        .withHeaders(play.api.http.HeaderNames.CONTENT_TYPE -> "application/json")
  }

}

/**
 * Helper utilities to build FakeRequest values.
 */
object FakeRequest {

  /**
   * Constructs a new GET / fake request.
   */
  def apply(): FakeRequest[play.api.mvc.AnyContent] = {
    FakeRequest("GET", "/", FakeHeaders(), AnyContentAsEmpty)
  }

  /**
   * Constructs a new request.
   */
  def apply(method: String, path: String): FakeRequest[play.api.mvc.AnyContent] = {
    FakeRequest(method, path, FakeHeaders(), AnyContentAsEmpty)
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
 */
case class FakeApplication(
    override val path: java.io.File = new java.io.File("."),
    override val classloader: ClassLoader = classOf[FakeApplication].getClassLoader,
    val additionalPlugins: Seq[String] = Nil,
    val withoutPlugins: Seq[String] = Nil,
    val additionalConfiguration: Map[String, String] = Map.empty) extends play.api.Application(path, classloader, None, play.api.Mode.Test) {

  override def pluginClasses = {
    additionalPlugins ++ super.pluginClasses.diff(withoutPlugins)
  }

  override def configuration = {
    super.configuration ++ play.api.Configuration.from(additionalConfiguration)
  }

}