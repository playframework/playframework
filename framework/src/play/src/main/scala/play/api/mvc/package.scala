/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

/**
 * Contains the Controller/Action/Result API to handle HTTP requests.
 *
 * For example, a typical controller:
 * {{{
 * class HomeController @Inject() (val controllerComponents: ControllerComponents) extends BaseController {
 *
 *   def index = Action {
 *     Ok("It works!")
 *   }
 *
 * }
 * }}}
 */
package object mvc {

}
