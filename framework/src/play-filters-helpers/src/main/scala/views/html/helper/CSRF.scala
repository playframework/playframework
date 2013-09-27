package views.html.helper

import play.api.mvc._
import play.api.templates.{ HtmlFormat, Html }

/**
 * CSRF helper for Play calls
 */
object CSRF {

  /**
   * Add the CSRF token as a query String parameter to this reverse router request
   */
  def apply(call: Call)(implicit token: play.filters.csrf.CSRF.Token): Call = {
    new Call(
      call.method,
      call.url + {
        if (call.url.contains("?")) "&" else "?"
      } + play.filters.csrf.CSRF.TokenName + "=" + token.value
    )
  }

  /**
   * Render a CSRF form field token
   */
  def formField(implicit token: play.filters.csrf.CSRF.Token): Html = {
    // probably not possible for an attacker to XSS with a CSRF token, but just to be on the safe side...
    Html(s"""<input type="hidden" name="${play.filters.csrf.CSRF.TokenName}" value="${HtmlFormat.escape(token.value)}"/>""")
  }

}
