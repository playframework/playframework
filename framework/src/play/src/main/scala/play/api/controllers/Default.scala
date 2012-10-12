package controllers

import play.api._
import play.api.mvc._
import play.api.libs._

/**
 * Default actions ready to use as is from your routes file.
 *
 * Example:
 * {{{
 * GET   /google          controllers.Default.redirect(to = "http://www.google.com")
 * GET   /favicon.ico     controllers.Default.notFound
 * GET   /admin           controllers.Default.todo
 * GET   /xxx             controllers.Default.error
 * }}}
 */
object Default extends Controller {

  /**
   * Returns a 501 NotImplemented response.
   *
   * Example:
   * {{{
   * GET   /admin           controllers.Default.todo
   * }}}
   */
  def todo: Action[AnyContent, Request] = TODO

  /**
   * Returns a 404 NotFound response.
   *
   * Example:
   * {{{
   * GET   /favicon.ico     controllers.Default.notFound
   * }}}
   */
  def notFound: Action[AnyContent, Request] = Action {
    NotFound
  }

  /**
   * Returns a 302 Redirect response.
   *
   * Example:
   * {{{
   * GET   /google          controllers.Default.redirect(to = "http://www.google.com")
   * }}}
   */
  def redirect(to: String): Action[AnyContent, Request] = Action {
    Redirect(to)
  }

  /**
   * Returns a 500 InternalServerError response.
   *
   * Example:
   * {{{
   * GET   /xxx             controllers.Default.error
   * }}}
   */
  def error: Action[AnyContent, Request] = Action {
    InternalServerError
  }

}