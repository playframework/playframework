/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.ws

import java.net.URI

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.json.JsValue

import scala.xml.Elem

/**
 * A WS Response that can use Play specific classes.
 */
trait WSResponse extends StandaloneWSResponse with WSBodyReadables {

  /**
   * The response status code.
   */
  override def status: Int

  /**
   * The response status message.
   */
  override def statusText: String

  /**
   * Return the current headers for this response.
   */
  override def headers: Map[String, scala.collection.Seq[String]]

  /**
   * Get the underlying response object.
   */
  override def underlying[T]: T

  /**
   * Get all the cookies.
   */
  override def cookies: scala.collection.Seq[WSCookie]

  /**
   * Get only one cookie, using the cookie name.
   */
  override def cookie(name: String): Option[WSCookie]

  override def contentType: String = super.contentType

  override def header(name: String): Option[String] = super.header(name)

  override def headerValues(name: String): scala.collection.Seq[String] = super.headerValues(name)

  /**
   * The response body as the given type.  This renders as the given type.
   * You must have a BodyReadable in implicit scope, which is done with
   *
   * {{{
   * class MyClass extends play.api.libs.ws.WSBodyReadables {
   *   // JSON and XML body readables
   * }
   * }}}
   *
   * The simplest use case is
   *
   * {{{
   * val responseBodyAsString: String = response.body[String]
   * }}}
   *
   * But you can also render as JSON
   *
   * {{{
   * val responseBodyAsJson: JsValue = response.body[JsValue]
   * }}}
   *
   * or as XML:
   *
   * {{{
   * val xml: Elem = response.body[Elem]
   * }}}
   */
  override def body[T: BodyReadable]: T = super.body[T]

  /**
   * The response body as String.
   */
  override def body: String

  /**
   * The response body as a byte string.
   */
  override def bodyAsBytes: ByteString

  override def bodyAsSource: Source[ByteString, _]

  @deprecated("Use response.headers", "2.6.0")
  def allHeaders: Map[String, scala.collection.Seq[String]]

  def xml: Elem

  def json: JsValue
}
