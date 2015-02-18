/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

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
}
