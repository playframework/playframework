/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.mvc

import play.api.http._
import play.api.i18n.{ Messages, Lang }
import play.api.Play

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
  implicit def request2session(implicit request: RequestHeader) = request.session

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
  implicit def request2flash(implicit request: RequestHeader) = request.flash

  implicit def request2lang(implicit request: RequestHeader) = {
    Messages.messagesApi.map(_.preferred(request).lang)
      .getOrElse(request.acceptLanguages.headOption.getOrElse(play.api.i18n.Lang.defaultLang))
  }

}
