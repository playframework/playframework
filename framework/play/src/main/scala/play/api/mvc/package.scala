package play.api

/**
 * Contains all the Controller/Action/Result API to handle HTTP requests.
 *
 * Example, a typical controller:
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
package object mvc