/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package views.html.helper

import play.api.mvc._
import play.twirl.api.{ Html, HtmlFormat }

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
      } + play.filters.csrf.CSRFConfig.global.tokenName + "=" + token.value
    )
  }

  /**
   * Render a CSRF form field token
   */
  def formField(implicit token: play.filters.csrf.CSRF.Token): Html = {
    // probably not possible for an attacker to XSS with a CSRF token, but just to be on the safe side...
    Html(s"""<input type="hidden" name="${play.filters.csrf.CSRFConfig.global.tokenName}" value="${HtmlFormat.escape(token.value)}"/>""")
  }

}
