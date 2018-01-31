/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import javax.inject.Inject

import play.api.http.FileMimeTypes
import play.api.i18n.{ Langs, Messages, MessagesApi, MessagesProvider }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * This trait is a [[play.api.i18n.MessagesProvider]] that can be applied to a RequestHeader, and
 * uses messagesApi.preferred(requestHeader) to return the messages.
 */
trait PreferredMessagesProvider extends MessagesProvider { self: RequestHeader =>
  /**
   * @return the messagesApi used to produce a Messages instance.
   */
  def messagesApi: MessagesApi

  /**
   * @return the output from messagesApi.preferred(self)
   */
  lazy val messages: Messages = messagesApi.preferred(self)
}

/**
 * This trait is a RequestHeader that can provide a play.api.i18n.Messages instance.
 *
 * This is very useful with when used for forms processing, as the form helpers defined
 * in views.helper (e.g. inputText.scala.html) take a MessagesProvider.
 */
trait MessagesRequestHeader extends RequestHeader with MessagesProvider

/**
 * This class is a wrapped Request that is "i18n-aware" and can return the preferred
 * messages associated with the request.
 *
 * @param request the original request
 * @param messagesApi the injected messagesApi
 * @tparam A the body type of the request
 */
class MessagesRequest[+A](request: Request[A], val messagesApi: MessagesApi) extends WrappedRequest(request)
  with PreferredMessagesProvider with MessagesRequestHeader

/**
 * This trait is an [[ActionBuilder]] that provides a [[MessagesRequest]] to the block:
 *
 * {{{
 * class MyController @Inject()(
 *   messagesAction: MessagesActionBuilder,
 *   cc: ControllerComponents
 * ) extends AbstractController(cc) {
 *   def index = messagesAction { implicit request: MessagesRequest[AnyContent] =>
 *      Ok(views.html.formTemplate(form)) // twirl template with form builders
 *   }
 * }
 * }}}
 *
 * This is useful when you don't want to have to add [[play.api.i18n.I18nSupport]] to a controller for form processing.
 */
trait MessagesActionBuilder extends ActionBuilder[MessagesRequest, AnyContent]

class MessagesActionBuilderImpl[B](val parser: BodyParser[B], messagesApi: MessagesApi)(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[MessagesRequest, B] {

  def invokeBlock[A](request: Request[A], block: (MessagesRequest[A]) => Future[Result]): Future[Result] = {
    block(new MessagesRequest[A](request, messagesApi))
  }
}

class DefaultMessagesActionBuilderImpl(parser: BodyParser[AnyContent], messagesApi: MessagesApi)(implicit ec: ExecutionContext)
  extends MessagesActionBuilderImpl(parser, messagesApi) with MessagesActionBuilder {
  @Inject
  def this(parser: BodyParsers.Default, messagesApi: MessagesApi)(implicit ec: ExecutionContext) = {
    this(parser: BodyParser[AnyContent], messagesApi)
  }
}

/**
 * Controller components with a [[MessagesActionBuilder]].
 */
trait MessagesControllerComponents extends ControllerComponents {
  def messagesActionBuilder: MessagesActionBuilder
}

case class DefaultMessagesControllerComponents @Inject() (
    messagesActionBuilder: MessagesActionBuilder,
    actionBuilder: DefaultActionBuilder,
    parsers: PlayBodyParsers,
    messagesApi: MessagesApi,
    langs: Langs,
    fileMimeTypes: FileMimeTypes,
    executionContext: scala.concurrent.ExecutionContext
) extends MessagesControllerComponents

/**
 * A base controller that returns a [[MessagesRequest]] as the base Action.
 */
trait MessagesBaseController extends BaseControllerHelpers {

  /**
   * The components needed to use the controller methods
   */
  protected def controllerComponents: MessagesControllerComponents

  def Action: ActionBuilder[MessagesRequest, AnyContent] = {
    controllerComponents.messagesActionBuilder.compose(controllerComponents.actionBuilder)
  }
}

/**
 * An abstract controller class that returns a [[MessagesRequest]] as the default Action.
 *
 * An abstract implementation of [[MessagesBaseController]] to make it slightly easier to use.
 *
 * {{{
 *   class MyController @Inject()(cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {
 *     def index = Action { implicit request: MessagesRequest[AnyContent] =>
 *       Ok(views.html.formTemplate(form)) // twirl template with form builders
 *     }
 *   }
 * }}}
 */
abstract class MessagesAbstractController @Inject() (
    protected val controllerComponents: MessagesControllerComponents
) extends MessagesBaseController