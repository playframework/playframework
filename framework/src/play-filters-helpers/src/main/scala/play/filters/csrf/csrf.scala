/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.csrf

import scala.concurrent.Future

import play.api._
import play.api.inject.Module
import play.api.http.HttpErrorHandler
import play.api.libs.Crypto
import play.api.mvc.Results._
import play.api.mvc._
import play.core.j.JavaHelpers
import javax.inject.Inject

private[csrf] object CSRFConf extends CSRFConf(
  Play.maybeApplication map (_.configuration) getOrElse Configuration.empty)

private[csrf] class CSRFConf(conf: => Configuration) {

  def TokenName: String = conf.getString("csrf.token.name").getOrElse("csrfToken")
  def CookieName: Option[String] = conf.getString("csrf.cookie.name")
  def SecureCookie: Boolean = conf.getBoolean("csrf.cookie.secure").getOrElse(Session.secure)
  def PostBodyBuffer: Long = conf.getBytes("csrf.body.bufferSize").getOrElse(102400L)
  def SignTokens: Boolean = conf.getBoolean("csrf.sign.tokens").getOrElse(true)

  val UnsafeMethods = Set("POST")
  val UnsafeContentTypes = Set("application/x-www-form-urlencoded", "text/plain", "multipart/form-data")

  val HeaderName = "Csrf-Token"
  val HeaderNoCheck = "nocheck"

  def defaultCreateIfNotFound(request: RequestHeader) = {
    // If the request isn't accepting HTML, then it won't be rendering a form, so there's no point in generating a
    // CSRF token for it.
    (request.method == "GET" || request.method == "HEAD") && (request.accepts("text/html") || request.accepts("application/xml+xhtml"))
  }

  /**
   * This is used by the noarg constructor of CSRFFilter, so that Java developers can select an error handler.
   */
  def defaultJavaErrorHandler: CSRF.ErrorHandler = {
    conf.getString("csrf.error.handler").map { className =>
      val clazz = try {
        Play.maybeApplication.get.classloader.loadClass(className)
      } catch {
        case c: ClassNotFoundException => throw new RuntimeException("Could not find CSRF error handler " + className, c)
      }
      if (classOf[CSRFErrorHandler].isAssignableFrom(clazz)) {
        import play.mvc.Http.{ Context => JContext }
        val errorHandler = clazz.newInstance().asInstanceOf[CSRFErrorHandler]
        new CSRF.ErrorHandler {
          def handle(req: RequestHeader, msg: String) = {
            val ctx = JavaHelpers.createJavaContext(req)
            JContext.current.set(ctx)
            try {
              Future.successful(errorHandler.handle(msg).toScala)
            } finally {
              JContext.current.remove()
            }
          }
        }
      } else if (classOf[CSRF.ErrorHandler].isAssignableFrom(clazz)) {
        clazz.newInstance().asInstanceOf[CSRF.ErrorHandler]
      } else {
        throw new RuntimeException(s"Error handler must implement ${classOf[CSRFErrorHandler]} or ${classOf[CSRF.ErrorHandler]}")
      }
    }.getOrElse(CSRF.DefaultErrorHandler)
  }

  def defaultTokenProvider = {
    if (SignTokens) {
      CSRF.SignedTokenProvider
    } else {
      CSRF.UnsignedTokenProvider
    }
  }

  def defaultConfig: CSRF.Config = CSRF.Config(TokenName, CookieName, SecureCookie, defaultCreateIfNotFound)
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
  def getToken(request: RequestHeader): Option[Token] = {
    // First check the tags, this is where tokens are added if it's added to the current request
    val token = request.tags.get(Token.RequestTag)
      // Check cookie if cookie name is defined
      .orElse(CookieName.flatMap(n => request.cookies.get(n).map(_.value)))
      // Check session
      .orElse(request.session.get(TokenName))
    if (SignTokens) {
      // Extract the signed token, and then resign it. This makes the token random per request, preventing the BREACH
      // vulnerability
      token.flatMap(Crypto.extractSignedToken)
        .map(token => Token(Crypto.signToken(token)))
    } else {
      token.map(Token.apply)
    }
  }

  /**
   * A token provider, for generating and comparing tokens.
   *
   * This abstraction allows the use of randomised tokens.
   */
  trait TokenProvider {
    /** Generate a token */
    def generateToken: String
    /** Compare two tokens */
    def compareTokens(tokenA: String, tokenB: String): Boolean
  }

  object SignedTokenProvider extends TokenProvider {
    def generateToken = Crypto.generateSignedToken
    def compareTokens(tokenA: String, tokenB: String) = Crypto.compareSignedTokens(tokenA, tokenB)
  }

  object UnsignedTokenProvider extends TokenProvider {
    def generateToken = Crypto.generateToken
    def compareTokens(tokenA: String, tokenB: String) = Crypto.constantTimeEquals(tokenA, tokenB)
  }

  /**
   * This trait handles the CSRF error.
   */
  trait ErrorHandler {
    /** Handle a result */
    def handle(req: RequestHeader, msg: String): Future[Result]
  }

  class CSRFHttpErrorHandler @Inject() (httpErrorHandler: HttpErrorHandler) extends ErrorHandler {
    import play.api.http.Status.FORBIDDEN
    def handle(req: RequestHeader, msg: String) = httpErrorHandler.onClientError(req, FORBIDDEN, msg)
  }

  object DefaultErrorHandler extends ErrorHandler {
    def handle(req: RequestHeader, msg: String) = Future.successful(Forbidden(msg))
  }

  /**
   * Configuration options for the CSRF filter
   *
   * @param tokenName The key used to store the token in the Play session.  Defaults to csrfToken.
   * @param cookieName If defined, causes the filter to store the token in a Cookie with this name instead of the session.
   * @param secureCookie If storing the token in a cookie, whether this Cookie should set the secure flag.  Defaults to
   *                     whether the session cookie is configured to be secure.
   * @param createIfNotFound Whether a new CSRF token should be created if it's not found.  Default creates one if it's
   *                         a GET request that accepts HTML.
   */
  case class Config(
    tokenName: String = CSRFConf.TokenName,
    cookieName: Option[String] = CSRFConf.CookieName,
    secureCookie: Boolean = CSRFConf.SecureCookie,
    createIfNotFound: (RequestHeader) => Boolean = CSRFConf.defaultCreateIfNotFound)
}

/**
 * The CSRF module.
 */
class CSRFModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    val c = new CSRFConf(configuration)
    Seq(
      bind[CSRF.ErrorHandler].to[CSRF.CSRFHttpErrorHandler],
      bind[CSRF.Config] toInstance c.defaultConfig,
      bind[CSRF.TokenProvider] toInstance c.defaultTokenProvider,
      bind[CSRFFilter].toSelf
    )
  }
}

/**
 * Default global, use this if CSRF is your only Filter
 */
object Global extends WithFilters(new CSRFFilter()) with GlobalSettings

