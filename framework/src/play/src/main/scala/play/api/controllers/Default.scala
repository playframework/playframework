/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers

import javax.inject.Inject

import play.api.mvc._

object Default extends Default

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
class Default @Inject() () extends Controller {

  /**
   * Returns a 501 NotImplemented response.
   *
   * Example:
   * {{{
   * GET   /admin           controllers.Default.todo
   * }}}
   */
  def todo: Action[AnyContent] = TODO

  /**
   * Returns a 404 NotFound response.
   *
   * Example:
   * {{{
   * GET   /favicon.ico     controllers.Default.notFound
   * }}}
   */
  def notFound: Action[AnyContent] = Action {
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
  def redirect(to: String): Action[AnyContent] = Action {
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
  def error: Action[AnyContent] = Action {
    InternalServerError
  }

}
