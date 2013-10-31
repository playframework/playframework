/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package controllers

import play.api.mvc._

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
   * Returns a 200 OK response.
   *
   * Example:
   * {{{
   * GET   /xxx             controllers.Default.ok("hello")
   * GET   /xxx             controllers.Default.ok
   * }}}
   */
  def ok(content: String): Action[AnyContent] = Action {
    Ok(content)
  }
  val ok: Action[AnyContent] = ok("")

  /**
   * Returns a 204 No Content response.
   *
   * Example:
   * {{{
   * GET   /xxx             controllers.Default.noContent
   * }}}
   */
  def noContent: Action[AnyContent] = Action {
    NoContent
  }

  /**
   * Returns a 205 Reset Content response.
   *
   * Example:
   * {{{
   * GET   /xxx             controllers.Default.resetContent
   * }}}
   */
  def resetContent: Action[AnyContent] = Action {
    ResetContent
  }

  /**
   * Returns a 301 Moved Permanently response.
   *
   * Example:
   * {{{
   * GET   /google          controllers.Default.movedPermanently(to = "http://www.google.com")
   * }}}
   */
  def movedPermanently(to: String): Action[AnyContent] = Action {
    MovedPermanently(to)
  }

  /**
   * Returns a 302 Found response.
   *
   * Example:
   * {{{
   * GET   /google          controllers.Default.found(to = "http://www.google.com")
   * }}}
   */
  def found(to: String): Action[AnyContent] = Action {
    Found(to)
  }

  /**
   * Returns a 303 See Other response.
   *
   * Example:
   * {{{
   * GET   /google          controllers.Default.seeOther(to = "http://www.google.com")
   * }}}
   */
  def seeOther(to: String): Action[AnyContent] = Action {
    SeeOther(to)
  }
  val redirect = seeOther _

  /**
   * Returns a 307 Temporary Redirect response.
   *
   * Example:
   * {{{
   * GET   /google          controllers.Default.temporaryRedirect(to = "http://www.google.com")
   * }}}
   */
  def temporaryRedirect(to: String): Action[AnyContent] = Action {
    TemporaryRedirect(to)
  }

  /**
   * Returns a 308 Permanent Redirect response.
   * http://tools.ietf.org/html/draft-reschke-http-status-308 is now in experimental status
   *
   * Example:
   * {{{
   * GET   /google          controllers.Default.permanentRedirect(to = "http://www.google.com")
   * }}}
   */
  def permanentRedirect(to: String): Action[AnyContent] = Action {
    PermanentRedirect(to)
  }

  /**
   * Returns a 404 Not Found response.
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
   * Returns a 410 Gone response.
   *
   * Example:
   * {{{
   * GET   /favicon.ico     controllers.Default.gone
   * }}}
   */
  def gone: Action[AnyContent] = Action {
    Gone
  }

  /**
   * Returns a 429 Too Many Requests response.
   *
   * Example:
   * {{{
   * GET   /admin           controllers.Default.tooManyRequests
   * GET   /admin           controllers.Default.tooManyRequests(retryAfter = "60")
   * }}}
   */
  def tooManyRequests(retryAfter: String): Action[AnyContent] = Action {
    TooManyRequests(Option(retryAfter).map(x => Integer.parseInt(x)))
  }
  def tooManyRequests: Action[AnyContent] = Action {
    TooManyRequests(None)
  }

  /**
   * Returns a 500 Internal Server Error response.
   *
   * Example:
   * {{{
   * GET   /xxx             controllers.Default.internalServerError
   * }}}
   */
  def internalServerError: Action[AnyContent] = Action {
    InternalServerError
  }
  val error = internalServerError

  /**
   * Returns a 501 Not Implemented response.
   *
   * Example:
   * {{{
   * GET   /admin           controllers.Default.notImplemented
   * }}}
   */
  def notImplemented: Action[AnyContent] = Action {
    NotImplemented
  }
  val todo = notImplemented

  /**
   * Returns a 503 Service Unavailable response.
   *
   * Example:
   * {{{
   * GET   /admin           controllers.Default.serviceUnavailable
   * }}}
   */
  def serviceUnavailable: Action[AnyContent] = Action {
    ServiceUnavailable
  }

}
