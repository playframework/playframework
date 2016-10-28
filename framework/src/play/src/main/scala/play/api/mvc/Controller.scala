/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import javax.inject.Inject

import play.api.Play
import play.api.http.{ ContentTypes, HeaderNames, HttpProtocol, Status }
import play.api.i18n._
import play.api.libs.concurrent.Execution
import play.twirl.api.Html

/**
 * Useful mixins for controller classes (no global state)
 */
trait BaseController extends Results with HttpProtocol with Status with HeaderNames with ContentTypes with RequestExtractors with Rendering

/**
 * Defines utility methods to generate `Action` and `Results` types.
 *
 * For example:
 * {{{
 * class HomeController @Inject()() extends Controller {
 *
 *   def hello(name:String) = Action { request =>
 *     Ok("Hello " + name)
 *   }
 *
 * }
 * }}}
 *
 * This controller provides some deprecated global state. To inject this state you can AbstractController instead.
 */
trait Controller extends BodyParsers with BaseController {

  /**
   * Provides an empty `Action` implementation: the result is a standard ‘Not implemented yet’ result page.
   *
   * For example:
   * {{{
   * def index(name:String) = TODO
   * }}}
   */
  lazy val TODO: Action[AnyContent] = ActionBuilder.ignoringBody {
    NotImplemented[Html](views.html.defaultpages.todo())
  }

  /**
   * Retrieves the session implicitly from the request.
   *
   * For example:
   * {{{
   * def index(name:String) = Action { implicit request =>
   *   val username = request2session("username")
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
   *   val message = request2flash("message")
   *   Ok("Got " + message)
   * }
   * }}}
   */
  implicit def request2flash(implicit request: RequestHeader): Flash = request.flash

  /**
   * Retrieve the language implicitly from the request.
   *
   * For example:
   * {{{
   * def index(name:String) = Action { implicit request =>
   *   val lang: Lang = request2lang
   *   Ok("Got " + lang)
   * }
   * }}}
   *
   * @deprecated This class relies on MessagesApi. Use [[play.api.i18n.I18nSupport]]
   *            and use `request.messages.lang`.
   */
  @deprecated("See https://www.playframework.com/documentation/2.6.x/MessagesMigration26", "2.6.0")
  implicit def request2lang(implicit request: RequestHeader): Lang = {
    play.api.Play.privateMaybeApplication.map(app => play.api.i18n.Messages.messagesApiCache(app).preferred(request).lang)
      .getOrElse(request.acceptLanguages.headOption.getOrElse(play.api.i18n.Lang.defaultLang))
  }

}

/**
 * An alternative to `Controller` that provides a "parse" field containing parsers and an "Action" method.
 *
 * This is intended to provide the idiomatic Play API for actions, allowing you to use "Action" for the default
 * action builder and "parse" to access Play's default body parsers. You may want to extend this to provide your own
 * base controller class.
 */
abstract class AbstractController(components: ControllerComponents) extends BaseController {
  def Action = components.actionBuilder
  def parse = components.parsers
  implicit def messagesApi: MessagesApi = components.messagesApi
  lazy val TODO: Action[AnyContent] = Action {
    NotImplemented[Html](views.html.defaultpages.todo())
  }
}

trait ControllerComponents {
  def actionBuilder: ActionBuilder[Request, AnyContent]
  def parsers: PlayBodyParsers
  def messagesApi: MessagesApi
  def langs: Langs
  def executionContext: scala.concurrent.ExecutionContext
}

case class DefaultControllerComponents @Inject() (
  actionBuilder: DefaultActionBuilder,
  parsers: PlayBodyParsers,
  messagesApi: MessagesApi,
  langs: Langs,
  executionContext: scala.concurrent.ExecutionContext)
    extends ControllerComponents
