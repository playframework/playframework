package play.filters.csrf

import play.api.mvc._

/**
 * A filter that provides CSRF protection.
 *
 * @param tokenName The key used to store the token in the Play session.  Defaults to csrfToken.
 * @param cookieName If defined, causes the filter to store the token in a Cookie with this name instead of the session.
 * @param secureCookie If storing the token in a cookie, whether this Cookie should set the secure flag.  Defaults to
 *                     whether the session cookie is configured to be secure.
 * @param createIfNotFound Whether a new CSRF token should be created if it's not found.  Default creates one if it's
 *                         a GET request that accepts HTML.
 */
case class CSRFFilter(tokenName: String = CSRFConf.TokenName,
    cookieName: Option[String] = CSRFConf.CookieName,
    secureCookie: Boolean = CSRFConf.SecureCookie,
    createIfNotFound: (RequestHeader) => Boolean = CSRFConf.defaultCreateIfNotFound) extends EssentialFilter {

  /**
   * Default constructor, useful from Java
   */
  def this() = this(CSRFConf.TokenName, CSRFConf.CookieName, CSRFConf.SecureCookie, CSRFConf.defaultCreateIfNotFound)

  def apply(next: EssentialAction): EssentialAction = new CSRFAction(next, tokenName, cookieName, secureCookie,
    createIfNotFound)
}
