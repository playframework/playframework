/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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
package object mvc
