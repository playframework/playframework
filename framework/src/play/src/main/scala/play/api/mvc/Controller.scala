/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import javax.inject.Inject

import play.api.http._
import play.api.i18n._
import play.twirl.api.Html

import scala.concurrent.ExecutionContext

/**
 * Useful mixins for controller classes (no global state)
 */
trait BaseController extends Results with HttpProtocol with Status with HeaderNames with ContentTypes with RequestExtractors with Rendering {
  def components: ControllerComponents
}

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
trait Controller extends BaseController {

  private var _components: ControllerComponents = _

  @Inject
  def setControllerComponents(components: ControllerComponents): Unit = _components = components

  override def components: ControllerComponents = play.api.Play.maybeApplication.fold(_components) { app =>
    play.Logger.warn(
      s"""
        |You are using deprecated global state in your controllers. This is probably happening because your test
        |code is not setting ControllerComponents inside your controller (${this.getClass}). To fix that, you have to
        |either migrate to use play.api.mvc.AbstractController or you should call setControllerComponents method after
        |creating your controller.
      """.stripMargin)
    app.injector.instanceOf[ControllerComponents]
  }

  /**
   * The default ActionBuilder. Used to construct an action, for example:
   *
   * {{{
   *   def foo(query: String) = Action {
   *     Ok
   *   }
   * }}}
   *
   * This is meant to be a replacement for the now-deprecated Action object, and can be used in the same way.
   */
  def Action: ActionBuilder[Request, AnyContent] = components.actionBuilder

  /**
   * The default body parsers provided by Play. This can be used along with the Action helper to customize the body
   * parser, for example:
   *
   * {{{
   *   def foo(query: String) = Action(parse.tolerantJson) { request =>
   *     Ok(request.body)
   *   }
   * }}}
   */
  def parse: PlayBodyParsers = components.parsers

  /**
   * The default execution context provided by Play. You should use this for non-blocking code only. You can do so by
   * passing it explicitly, or by defining an implicit in your controller like so:
   *
   * {{{
   *   implicit lazy val executionContext = defaultExecutionContext
   * }}}
   */
  def defaultExecutionContext: ExecutionContext = components.executionContext

  /**
   * The MessagesApi provided by Play. This can be used to provide the MessagesApi needed by i18nComponents.
   */
  implicit def messagesApi: MessagesApi = components.messagesApi

  /**
   * The default Langs provided by Play. Can be used to determine the application's supported languages.
   */
  implicit def supportedLangs: Langs = components.langs

  /**
   * The default FileMimeTypes provided by Play. Used to map between file name extensions and mime types.
   */
  implicit def fileMimeTypes: FileMimeTypes = components.fileMimeTypes

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
 * An base controller class that provides a "parse" method containing parsers and an "Action" method, as well as other
 * commonly used components.
 *
 * This is intended to provide the idiomatic Play API for actions, allowing you to use "Action" for the default
 * action builder and "parse" to access Play's default body parsers. You may want to extend this to provide your own
 * base controller class, or write your own version with similar code.
 */
abstract class AbstractController(val components: ControllerComponents) extends BaseController {

  /**
   * The default ActionBuilder. Used to construct an action, for example:
   *
   * {{{
   *   def foo(query: String) = Action {
   *     Ok
   *   }
   * }}}
   *
   * This is meant to be a replacement for the now-deprecated Action object, and can be used in the same way.
   */
  def Action: ActionBuilder[Request, AnyContent] = components.actionBuilder

  /**
   * The default body parsers provided by Play. This can be used along with the Action helper to customize the body
   * parser, for example:
   *
   * {{{
   *   def foo(query: String) = Action(parse.tolerantJson) { request =>
   *     Ok(request.body)
   *   }
   * }}}
   */
  def parse: PlayBodyParsers = components.parsers

  /**
   * Used to mark an action that is still not implemented, e.g.:
   *
   * {{{
   *   def action(query: String) = TODO
   * }}}
   */
  lazy val TODO: Action[AnyContent] = Action {
    NotImplemented[Html](views.html.defaultpages.todo())
  }

  /**
   * The default execution context provided by Play. You should use this for non-blocking code only. You can do so by
   * passing it explicitly, or by defining an implicit in your controller like so:
   *
   * {{{
   *   implicit lazy val executionContext = defaultExecutionContext
   * }}}
   */
  def defaultExecutionContext: ExecutionContext = components.executionContext

  /**
   * The MessagesApi provided by Play. This can be used to provide the MessagesApi needed by i18nComponents.
   */
  implicit def messagesApi: MessagesApi = components.messagesApi

  /**
   * The default Langs provided by Play. Can be used to determine the application's supported languages.
   */
  implicit def supportedLangs: Langs = components.langs

  /**
   * The default FileMimeTypes provided by Play. Used to map between file name extensions and mime types.
   */
  implicit def fileMimeTypes: FileMimeTypes = components.fileMimeTypes
}

trait ControllerComponents {
  def actionBuilder: ActionBuilder[Request, AnyContent]
  def parsers: PlayBodyParsers
  def messagesApi: MessagesApi
  def langs: Langs
  def fileMimeTypes: FileMimeTypes
  def executionContext: scala.concurrent.ExecutionContext
}

case class DefaultControllerComponents @Inject() (
  actionBuilder: DefaultActionBuilder,
  parsers: PlayBodyParsers,
  messagesApi: MessagesApi,
  langs: Langs,
  fileMimeTypes: FileMimeTypes,
  executionContext: scala.concurrent.ExecutionContext)
    extends ControllerComponents
