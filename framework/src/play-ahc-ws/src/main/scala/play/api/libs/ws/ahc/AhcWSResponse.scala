package play.api.libs.ws.ahc

import akka.util.ByteString
import play.shaded.ahc.org.asynchttpclient.{ Response => AHCResponse }
import play.api.libs.json.JsValue
import play.api.libs.ws._

import scala.xml.Elem

/**
 * A WS HTTP Response backed by an AsyncHttpClient response.
 *
 * @param underlying
 */
case class AhcWSResponse(underlying: StandaloneAhcWSResponse) extends WSResponse {

  def this(ahcResponse: AHCResponse) = {
    this(StandaloneAhcWSResponse(ahcResponse))
  }

  /**
   * The response body as String.
   */
  override def body: String = underlying.body

  /**
   * The response body as Xml.
   */
  override lazy val xml: Elem = underlying.xml

  /**
   * Return the current headers of the request being constructed
   */
  override def allHeaders: Map[String, Seq[String]] = underlying.allHeaders

  /**
   * Get the underlying response object, i.e. play.shaded.ahc.org.asynchttpclient.Response
   *
   * {{{
   * val ahcResponse = response.underlying[play.shaded.ahc.org.asynchttpclient.Response]
   * }}}
   */
  override def underlying[T]: T = underlying.underlying[T]

  /**
   * The response status code.
   */
  override def status: Int = underlying.status

  /**
   * The response status message.
   */
  override def statusText: String = underlying.statusText

  /**
   * Get a response header.
   */
  override def header(key: String): Option[String] = underlying.header(key)

  /**
   * Get all the cookies.
   */
  override def cookies: Seq[WSCookie] = underlying.cookies

  /**
   * Get only one cookie, using the cookie name.
   */
  override def cookie(name: String): Option[WSCookie] = underlying.cookie(name)

  /**
   * The response body as Json.
   */
  override def json: JsValue = underlying.json

  /**
   * The response body as a byte string.
   */
  override def bodyAsBytes: ByteString = underlying.bodyAsBytes
}
