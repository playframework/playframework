/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import javax.inject.Inject

import play.api.Logger
import play.api.http._
import play.api.i18n.{ Lang, Langs, MessagesApi }
import play.twirl.api.Html

import scala.concurrent.ExecutionContext

/**
 * Useful mixins for controller classes.
 *
 * If you wish to write a controller with minimal dependencies, you can mix in this trait, which includes helpers and
 * useful constants.
 *
 * {{{
 *   class MyController @Inject() (action: DefaultActionBuilder, parse: PlayBodyParsers) extends ControllerHelpers {
 *     def index = action(parse.text) {
 *       Ok
 *     }
 *   }
 * }}}
 */
trait ControllerHelpers extends Results with HttpProtocol with Status with HeaderNames with ContentTypes with RequestExtractors with Rendering with RequestImplicits {

  /**
   * Used to mark an action that is still not implemented, e.g.:
   *
   * {{{
   *   def action(query: String) = TODO
   * }}}
   */
  lazy val TODO: Action[AnyContent] = ActionBuilder.ignoringBody {
    NotImplemented[Html](views.html.defaultpages.todo())
  }
}

object ControllerHelpers extends ControllerHelpers

/**
 * Useful prewired mixins for controller components, assuming an available [[ControllerComponents]].
 *
 * If you want to extend your own [[AbstractController]] but want to use a different base "Action",
 * you can mix in this trait.
 */
trait BaseControllerHelpers extends ControllerHelpers {

  /**
   * The components needed to use the controller methods
   */
  protected def controllerComponents: ControllerComponents

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
  def parse: PlayBodyParsers = controllerComponents.parsers

  /**
   * The default execution context provided by Play. You should use this for non-blocking code only. You can do so by
   * passing it explicitly, or by defining an implicit in your controller like so:
   *
   * {{{
   *   implicit lazy val executionContext = defaultExecutionContext
   * }}}
   */
  def defaultExecutionContext: ExecutionContext = controllerComponents.executionContext

  /**
   * The MessagesApi provided by Play. This can be used to provide the MessagesApi needed by play.api.i18n.I18nSupport.
   */
  implicit def messagesApi: MessagesApi = controllerComponents.messagesApi

  /**
   * The default Langs provided by Play. Can be used to determine the application's supported languages.
   */
  implicit def supportedLangs: Langs = controllerComponents.langs

  /**
   * The default FileMimeTypes provided by Play. Used to map between file name extensions and mime types.
   */
  implicit def fileMimeTypes: FileMimeTypes = controllerComponents.fileMimeTypes
}

/**
 * Useful mixin for methods that do implicit transformations of a request
 */
trait RequestImplicits {

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

}

/**
 * Defines utility methods to generate `Action` and `Results` types.
 *
 * For example:
 * {{{
 * class HomeController @Inject() (val controllerComponents: ControllerComponents) extends BaseController {
 *
 *   def hello(name:String) = Action { request =>
 *     Ok("Hello " + name)
 *   }
 *
 * }
 * }}}
 *
 *
 * This is intended to provide the idiomatic Play API for actions, allowing you to use "Action" for the default
 * action builder and "parse" to access Play's default body parsers. You may want to extend this to provide your own
 * base controller class, or write your own version with similar code.
 */
trait BaseController extends BaseControllerHelpers {

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
  def Action: ActionBuilder[Request, AnyContent] = controllerComponents.actionBuilder

}

/**
 * An abstract implementation of [[BaseController]] to make it slightly easier to use.
 */
abstract class AbstractController(protected val controllerComponents: ControllerComponents) extends BaseController

/**
 * A variation of [[BaseController]] that gets its components via method injection.
 */
trait InjectedController extends BaseController {

  private val logger = Logger(getClass)

  private[this] var _components: ControllerComponents = _

  override protected def controllerComponents: ControllerComponents = {
    if (_components == null) fallbackControllerComponents else _components
  }

  /**
   * Call this method to set the [[ControllerComponents]] instance.
   */
  @Inject
  def setControllerComponents(components: ControllerComponents): Unit = {
    _components = components
  }

  /**
   * Defines fallback components to use in case setControllerComponents has not been called.
   */
  protected def fallbackControllerComponents: ControllerComponents = {
    throw new NoSuchElementException(
      "ControllerComponents not set! Call setControllerComponents or create the instance with dependency injection.")
  }
}

/**
 * The base controller components dependencies that most controllers rely on.
 */
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

/**
 * Implements deprecated controller functionality. We recommend moving away from this and using one of the classes or
 * traits extending [[BaseController]] instead.
 */
@deprecated(
  "Your controller should extend AbstractController, BaseController, or InjectedController instead.",
  "2.6.0")
trait Controller extends ControllerHelpers with BodyParsers {
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
