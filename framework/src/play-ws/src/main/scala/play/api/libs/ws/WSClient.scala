/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.ws

import java.io.{ Closeable, IOException }

/**
 * A Play specific WS client that can use Play specific classes in the request and response building.
 *
 * Typically, access this class through dependency injection, i.e.
 *
 * {{{
 *   class MyService @Inject()(ws: WSClient) {
 *     val response: Future[WSResponse] = ws.url("http://example.com").get()
 *   }
 * }}}
 *
 * Please see the documentation at https://www.playframework.com/documentation/latest/ScalaWS for more details.
 */
trait WSClient extends Closeable {

  /**
   * The underlying implementation of the client, if any.  You must cast explicitly to the type you want.
   * @tparam T the type you are expecting (i.e. isInstanceOf)
   * @return the backing class.
   */
  def underlying[T]: T

  /**
   * Generates a request.
   *
   * @param url The base URL to make HTTP requests to.
   * @return a request
   */
  def url(url: String): WSRequest

  /** Closes this client, and releases underlying resources. */
  @throws[IOException] def close(): Unit
}
