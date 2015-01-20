package play.api.i18n

import play.api.mvc.{ RequestHeader, Result }

/**
 * Brings a convenient implicit conversion from [[RequestHeader]] to [[Messages]].
 *
 * Example:
 * {{{
 *   import play.api.i18n.Messages
 *   class MyController(val messagesApi: MessagesApi) extends ControllerLike with I18nSupport {
 *     val action = Action { implicit request =>
 *       Ok(Messages("some.key")) // Uses the client’s preferred language
 *     }
 *   }
 * }}}
 */
trait I18nSupport extends I18NSupportLowPriorityImplicits {

  /** The [[MessagesApi]] to use. */
  def messagesApi: MessagesApi

  /**
   * @return The preferred [[Messages]] according to the given [[RequestHeader]]
   */
  implicit def request2Messages(implicit request: RequestHeader): Messages = messagesApi.preferred(request)

  /**
   * Adds convenient methods to handle the client-side language
   */
  implicit class ResultWithLang(result: Result) {

    /**
     * Sets the user's language permanently for future requests by storing it in a cookie.
     *
     * For example:
     * {{{
     * implicit val lang = Lang("fr-FR")
     * Ok(Messages("hello.world")).withLang(lang)
     * }}}
     *
     * @param lang the language to store for the user
     * @return the new result
     */
    def withLang(lang: Lang): Result =
      messagesApi.setLang(result, lang)

    /**
     * Clears the user's language by discarding the language cookie set by withLang
     *
     * For example:
     * {{{
     * Ok(Messages("hello.world")).clearingLang
     * }}}
     *
     * @return the new result
     */
    def clearingLang: Result =
      messagesApi.clearLang(result)

  }

}

trait I18NSupportLowPriorityImplicits { this: I18nSupport =>

  /**
   * @return A [[Messages]] value that uses the [[Lang]] found in the implicit scope
   */
  implicit def lang2Messages(implicit lang: Lang): Messages = Messages(lang, messagesApi)

}