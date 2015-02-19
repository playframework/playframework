/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.mvc

import play.api.http.{ ContentTypes, HeaderNames, HttpProtocol, Status }
import play.api.i18n.Lang

/**
 * Defines utility methods to generate `Action` and `Results` types.
 *
 * For example:
 * {{{
 * object Application extends Controller {
 *
 *   def hello(name:String) = Action { request =>
 *     Ok("Hello " + name)
 *   }
 *
 * }
 * }}}
 */
trait Controller extends Results with BodyParsers with HttpProtocol with Status with HeaderNames with ContentTypes with RequestExtractors with Rendering {

  /**
   * Provides an empty `Action` implementation: the result is a standard ‘Not implemented yet’ result page.
   *
   * For example:
   * {{{
   * def index(name:String) = TODO
   * }}}
   */
  val TODO = Action {
    NotImplemented[play.twirl.api.Html](views.html.defaultpages.todo())
  }

  /**
   * Retrieves the session implicitly from the request.
   *
   * For example:
   * {{{
   * def index(name:String) = Action { implicit request =>
   *   val username = session("username")
   *   Ok("Hello " + username)
   * }
   * }}}
   */
  implicit def request2session(implicit request: RequestHeader): Session = request.session

  /**
   * Retrieve the flash scope implicitly from the request.
   *
   * For example:
   * {{{
   * def index(name:String) = Action { implicit request =>
   *   val message = flash("message")
   *   Ok("Got " + message)
   * }
   * }}}
   */
  implicit def request2flash(implicit request: RequestHeader): Flash = request.flash

  implicit def request2lang(implicit request: RequestHeader): Lang = {
    play.api.Play.maybeApplication.map(app => play.api.i18n.Messages.messagesApiCache(app).preferred(request).lang)
      .getOrElse(request.acceptLanguages.headOption.getOrElse(play.api.i18n.Lang.defaultLang))
  }

}
