/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

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
trait FormRequestHeader extends RequestHeader with MessagesProvider

/**
 * This class is a wrapped Request that is "i18n-aware" and can return the preferred
 * messages associated with the request.
 *
 * @param request the original request
 * @param messagesApi the injected messagesApi
 * @tparam A the body type of the request
 */
class FormRequest[A](request: Request[A], val messagesApi: MessagesApi) extends WrappedRequest(request)
  with PreferredMessagesProvider with FormRequestHeader

/**
 * This class is an ActionFunction that takes a Request[A] and returns a FormRequest[A].
 *
 * You can compose this with an existing ActionBuilder:
 *
 * {{{
 *   myActionBuilder.andThen(new FormActionFunction(messagesApi))
 * }}}
 */
class FormActionFunction(messagesApi: MessagesApi) extends ActionFunction[Request, FormRequest] {
  override def invokeBlock[A](request: Request[A], block: (FormRequest[A]) => Future[Result]): Future[Result] = {
    block(new FormRequest[A](request, messagesApi))
  }

  override protected def executionContext: ExecutionContext = play.core.Execution.trampoline
}
