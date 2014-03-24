/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import play.api.libs.iteratee.Enumerator

/**
 * Contains the Controller/Action/Result API to handle HTTP requests.
 *
 * For example, a typical controller:
 * {{{
 * object Application extends Controller {
 *
 *   def index = Action {
 *     Ok("It works!")
 *   }
 *
 * }
 * }}}
 */
package object mvc {

  /**
   * Alias types for Sockets
   */
  object Socket {

    /**
     * A Socket Out
     */
    type Out[A] = play.api.libs.iteratee.Iteratee[A, Unit]

  }

  @deprecated("SimpleResult has been renamed to Result", "2.3")
  type SimpleResult = Result

  @deprecated("SimpleResult has been renamed to Result", "2.3")
  object SimpleResult {

    @deprecated("SimpleResult has been renamed to Result", "2.3")
    def apply(header: ResponseHeader, body: Enumerator[Array[Byte]],
      connection: HttpConnection.Connection = HttpConnection.KeepAlive) =
      Result.apply(header, body, connection)

    @deprecated("SimpleResult has been renamed to Result", "2.3")
    def unapply(result: Result): Option[(ResponseHeader, Enumerator[Array[Byte]], HttpConnection.Connection)] = {
      Result.unapply(result)
    }
  }

}
