/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import javax.inject.Inject

import play.api.i18n.{ Messages, MessagesApi, MessagesProvider }

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
class MessagesRequest[A](request: Request[A], val messagesApi: MessagesApi) extends WrappedRequest(request)
  with PreferredMessagesProvider with MessagesRequestHeader

/**
 * This class is an ActionBuilder that provides a MessagesRequest to the block:
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
 * This is useful when you don't want to have to add I18nSupport to a controller for form processing.
 *
 * You can also replace AbstractController completely and use an Action which uses MessagesRequest by default:
 *
 * {{{
 * abstract class FormAwareController @Inject()(
 *   protected val controllerComponents: MyControllerComponents
 * ) extends ControllerHelpers {
 *
 *   def Action: ActionBuilder[MessagesRequest, AnyContent] = {
 *     controllerComponents.messagesActionBuilder.compose(controllerComponents.actionBuilder)
 *   }
 *
 *   def parse: PlayBodyParsers = controllerComponents.parsers
 *
 *   def defaultExecutionContext: ExecutionContext = controllerComponents.executionContext
 *
 *   implicit def messagesApi: MessagesApi = controllerComponents.messagesApi
 *
 *   implicit def supportedLangs: Langs = controllerComponents.langs
 *
 *   implicit def fileMimeTypes: FileMimeTypes = controllerComponents.fileMimeTypes
 * }
 *
 * case class MyControllerComponents @Inject()(
 *   messagesActionBuilder: MessagesAction,
 *   actionBuilder: DefaultActionBuilder,
 *   parsers: PlayBodyParsers,
 *   messagesApi: MessagesApi,
 *   langs: Langs,
 *   fileMimeTypes: FileMimeTypes,
 *   executionContext: scala.concurrent.ExecutionContext
 * ) extends ControllerComponents
 * }}}
 */
class MessagesActionBuilder @Inject() (parsers: PlayBodyParsers, messagesApi: MessagesApi) extends ActionBuilder[MessagesRequest, AnyContent] {
  override def invokeBlock[A](request: Request[A], block: (MessagesRequest[A]) => Future[Result]): Future[Result] = {
    block(new MessagesRequest[A](request, messagesApi))
  }

  override protected def executionContext: ExecutionContext = play.core.Execution.trampoline

  override def parser: BodyParser[AnyContent] = parsers.anyContent
}
