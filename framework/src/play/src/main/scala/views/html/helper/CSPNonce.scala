/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package views.html.helper

import play.api.mvc.RequestHeader
import play.api.mvc.request.RequestAttrKey
import play.twirl.api.Html

/**
 * A helper that renders the CSP nonce if it is present.
 *
 * @see [[play.api.mvc.request.RequestAttrKey.CSPNonce]]
 */
object CSPNonce {

  /**
   * Gets nonce if RequestAttr.CSPNonce has a nonce value set.
   *
   * @param request the request containing the CSP nonce attribute
   * @return nonce value, or exception if not set.
   */
  def apply()(implicit request: RequestHeader): String = {
    get.getOrElse(
      sys.error("No CSP nonce was generated for this request! Is CSPFilter installed and nonce enabled?")
    )
  }

  /**
   * Gets nonce if RequestAttr.CSPNonce has a nonce value set.
   *
   * @param request the request containing the CSP nonce attribute
   * @return Some(nonce) or None if not set.
   */
  def get(implicit request: RequestHeader): Option[String] = {
    request.attrs.get(RequestAttrKey.CSPNonce)
  }

  /**
   * Gets Html containing "nonce=\$nonce" attribute value.
   *
   * @param request the request containing the CSP nonce attribute
   * @return Html with nonce attribute or empty string if not defined.
   */
  def attr(implicit request: RequestHeader): Html = {
    import play.twirl.api.HtmlFormat._
    get.map(nonce => raw(s"""nonce="$nonce"""")).getOrElse(empty)
  }

  /**
   * Gets nonce attribute if RequestAttr.CSPNonce has a nonce value set.
   *
   * @param request the request containing the CSP nonce attribute
   * @return Map("nonce" -> nonce), or Map.empty if not set.
   */
  def attrMap(implicit request: RequestHeader): Map[String, String] = {
    get match {
      case Some(nonce) =>
        Map("nonce" -> nonce)
      case None =>
        Map.empty
    }
  }

}
