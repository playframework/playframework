/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.i18n

import play.api.mvc._

/**
 * Brings convenient implicit conversions from [[play.api.mvc.RequestHeader]] to [[Messages]].
 *
 * Example:
 * {{{
 *   import play.api.i18n.Messages
 *   class MyController(val messagesApi: MessagesApi ...)
 *     extends AbstractController(cc) with I18nSupport {
 *     val action = Action { implicit request =>
 *       val messageFromRequest = request.messages("hello.world")
 *       Ok(s"\$messageFromRequest")
 *     }
 *   }
 * }}}
 */
trait I18nSupport extends I18NSupportLowPriorityImplicits {
  def messagesApi: MessagesApi

  /**
   * Converts from a request directly into a Messages.
   *
   * @param request the incoming request
   * @return The preferred [[Messages]] according to the given [[play.api.mvc.RequestHeader]]
   */
  implicit def request2Messages(implicit request: RequestHeader): Messages = messagesApi.preferred(request)
}

/**
 * A static object with type enrichment for request and responses.
 *
 * {{{
 * import I18nSupport._
 * }}}
 */
object I18nSupport extends I18NSupportLowPriorityImplicits

/**
 * Implicit conversions for using i18n with requests and results.
 */
trait I18NSupportLowPriorityImplicits {

  /**
   * Adds convenient methods to handle the messages.
   */
  implicit class RequestWithMessagesApi(request: RequestHeader) {
    /**
     * Adds a `messages` method that can be used on a request,
     * returning the Messages object in the preferred language
     * of the request.
     *
     * For example:
     * {{{
     *   implicit val messagesApi: MessagesApi = ...
     *   val messageFromRequest: Messages = request.messages("hello.world")
     * }}}
     */
    def messages(implicit messagesApi: MessagesApi): Messages = {
      messagesApi.preferred(request)
    }

    /**
     * Adds a `lang` method that can be used on a request,
     * returning the lang corresponding to the preferred language
     * of the request.
     *
     * For example:
     * {{{
     *   implicit val messagesApi: MessagesApi = ...
     *   val lang: Lang = request.lang
     * }}}
     */
    def lang(implicit messagesApi: MessagesApi): Lang = {
      messagesApi.preferred(request).lang
    }
  }

  /**
   * Adds convenient methods to handle the client-side language
   */
  implicit class ResultWithMessagesApi(result: Result) {
    /**
     * Sets the user's language permanently for future requests by storing it in a cookie.
     *
     * For example:
     * {{{
     * implicit val messagesApi: MessagesApi = ...
     * val lang = Lang("fr-FR")
     * Ok(Messages("hello.world")).withLang(lang)
     * }}}
     *
     * @param lang the language to store for the user
     * @return the new result
     */
    def withLang(lang: Lang)(implicit messagesApi: MessagesApi): Result = {
      messagesApi.setLang(result, lang)
    }

    /**
     * Clears the user's language by discarding the language cookie set by withLang
     *
     * For example:
     * {{{
     * implicit val messagesApi: MessagesApi = ...
     * Ok(Messages("hello.world")).withoutLang
     * }}}
     *
     * @return the new result
     */
    def withoutLang(implicit messagesApi: MessagesApi): Result = {
      messagesApi.clearLang(result)
    }

    @deprecated("Use withoutLang", "2.7.0")
    def clearingLang(implicit messagesApi: MessagesApi): Result = withoutLang
  }
}

/**
 * A trait for extracting a Messages instance from Langs
 */
trait LangImplicits {
  def messagesApi: MessagesApi

  /**
   * @return A [[Messages]] value that uses the [[Lang]] found in the implicit scope.
   */
  implicit def lang2Messages(implicit lang: Lang): Messages = MessagesImpl(lang, messagesApi)
}
