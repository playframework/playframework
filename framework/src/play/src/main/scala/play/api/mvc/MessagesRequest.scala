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
 * class Controller @Inject()(messagesAction: MessagesAction, cc: ControllerComponents) extends AbstractController(cc) {
 *   def index = messagesAction { implicit request: MessagesRequest[AnyContent] =>
 *
 *   }
 * }
 * }}}
 *
 * This is useful when you don't want to have to extend I18nSupport.
 */
class MessagesAction @Inject() (parsers: PlayBodyParsers, messagesApi: MessagesApi) extends ActionBuilder[MessagesRequest, AnyContent] {
  override def invokeBlock[A](request: Request[A], block: (MessagesRequest[A]) => Future[Result]): Future[Result] = {
    block(new MessagesRequest[A](request, messagesApi))
  }

  override protected def executionContext: ExecutionContext = play.core.Execution.trampoline

  override def parser: BodyParser[AnyContent] = parsers.anyContent
}
