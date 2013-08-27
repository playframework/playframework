package play.filters.csrf

import play.api.mvc._
import play.api._
import play.api.libs.Crypto

private[csrf] object CSRFConf {
  import play.api.Play.current

  def c = Play.configuration

  def TokenName: String = c.getString("csrf.token.name").getOrElse("csrfToken")
  def CookieName: Option[String] = c.getString("csrf.cookie.name")
  def SecureCookie: Boolean = c.getBoolean("csrf.cookie.secure").getOrElse(Session.secure)
  def PostBodyBuffer: Long = c.getBytes("csrf.body.bufferSize").getOrElse(102400L)

  val UnsafeMethods = Set("POST")
  val UnsafeContentTypes = Set("application/x-www-form-urlencoded", "text/plain", "multipart/form-data")

  val HeaderName = "Csrf-Token"
  val HeaderNoCheck = "nocheck"

  def defaultCreateIfNotFound(request: RequestHeader) = {
    // If the request isn't accepting HTML, then it won't be rendering a form, so there's no point in generating a
    // CSRF token for it.
    request.method == "GET" && (request.accepts("text/html") || request.accepts("application/xml+xhtml"))
  }
}

object CSRF {

  private[csrf] val filterLogger = play.api.Logger("play.filters")

  /**
   * A CSRF token
   */
  case class Token(value: String)

  object Token {
    val RequestTag = "CSRF_TOKEN"

    implicit def getToken(implicit request: RequestHeader): Token = {
      CSRF.getToken(request).getOrElse(sys.error("Missing CSRF Token"))
    }
  }

  // Allows the template helper to access it
  def TokenName = CSRFConf.TokenName

  import CSRFConf._

  /**
   * Extract token from current request
   */
  def getToken(request: RequestHeader): Option[Token] =
    // First check the tags, this is where tokens are added if it's added to the current request
    request.tags.get(Token.RequestTag)
      // Check cookie if cookie name is defined
      .orElse(CookieName.flatMap(n => request.cookies.get(n).map(_.value)))
      // Check session
      .orElse(request.session.get(TokenName))
      // Extract the signed token, and then resign it. This makes the token random per request, preventing the BREACH
      // vulnerability
      .flatMap(Crypto.extractSignedToken)
      .map(token => Token(Crypto.signToken(token)))

}

/**
 * Default global, use this if CSRF is your only Filter
 */
object Global extends WithFilters(new CSRFFilter()) with GlobalSettings

