/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.csrf

import java.util.Optional

import akka.stream.Materializer
import com.typesafe.config.ConfigMemorySize
import play.filters.csrf.CSRF.{ CSRFHttpErrorHandler, ErrorHandler }
import play.mvc.Http
import play.utils.Reflect

import scala.concurrent.Future

import play.api._
import play.api.inject.{ Binding, Module }
import play.api.http.HttpErrorHandler
import play.api.libs.Crypto
import play.api.mvc.Results._
import play.api.mvc._
import play.core.j.JavaHelpers
import javax.inject.{ Singleton, Provider, Inject }

/**
 * CSRF configuration.
 *
 * @param tokenName The name of the token.
 * @param cookieName If defined, the name of the cookie to read the token from/write the token to.
 * @param secureCookie If using a cookie, whether it should be secure.
 * @param httpOnlyCookie If using a cookie, whether it should have the HTTP only flag.
 * @param postBodyBuffer How much of the POST body should be buffered if checking the body for a token.
 * @param signTokens Whether tokens should be signed.
 * @param checkMethod Returns true if a request for that method should be checked.
 * @param checkContentType Returns true if a request for that content type should be checked.
 * @param headerName The name of the HTTP header to check for tokens from.
 * @param headerBypass Whether CSRF check can be bypassed by the presence of certain headers, such as X-Requested-By.
 */
case class CSRFConfig(tokenName: String = "csrfToken",
  cookieName: Option[String] = None,
  secureCookie: Boolean = false,
  httpOnlyCookie: Boolean = false,
  createIfNotFound: (RequestHeader) => Boolean = CSRFConfig.defaultCreateIfNotFound,
  postBodyBuffer: Long = 102400,
  signTokens: Boolean = true,
  checkMethod: String => Boolean = CSRFConfig.UnsafeMethods,
  checkContentType: Option[String] => Boolean = _.exists(CSRFConfig.UnsafeContentTypes),
  headerName: String = "Csrf-Token",
  headerBypass: Boolean = true)

object CSRFConfig {
  private val UnsafeContentTypes = Set("application/x-www-form-urlencoded", "text/plain", "multipart/form-data")

  private val UnsafeMethods = Set("POST")

  private def defaultCreateIfNotFound(request: RequestHeader) = {
    // If the request isn't accepting HTML, then it won't be rendering a form, so there's no point in generating a
    // CSRF token for it.
    (request.method == "GET" || request.method == "HEAD") && (request.accepts("text/html") || request.accepts("application/xml+xhtml"))
  }

  private[play] val HeaderNoCheck = "nocheck"

  def global = Play.maybeApplication.map(_.injector.instanceOf[CSRFConfig]).getOrElse(CSRFConfig())

  def fromConfiguration(conf: Configuration): CSRFConfig = {
    val config = PlayConfig(conf).getDeprecatedWithFallback("play.filters.csrf", "csrf")

    val methodWhiteList = config.get[Seq[String]]("method.whiteList").toSet
    val methodBlackList = config.get[Seq[String]]("method.blackList").toSet

    val checkMethod: String => Boolean = if (methodWhiteList.nonEmpty) {
      !methodWhiteList.contains(_)
    } else {
      methodBlackList.contains
    }

    val contentTypeWhiteList = config.get[Seq[String]]("contentType.whiteList").toSet
    val contentTypeBlackList = config.get[Seq[String]]("contentType.blackList").toSet

    val checkContentType: String => Boolean = if (contentTypeWhiteList.nonEmpty) {
      !contentTypeWhiteList.contains(_)
    } else {
      contentTypeBlackList.contains
    }

    CSRFConfig(
      tokenName = config.get[String]("token.name"),
      cookieName = config.get[Option[String]]("cookie.name"),
      secureCookie = config.get[Boolean]("cookie.secure"),
      httpOnlyCookie = config.get[Boolean]("cookie.httpOnly"),
      postBodyBuffer = config.get[ConfigMemorySize]("body.bufferSize").toBytes,
      signTokens = config.get[Boolean]("token.sign"),
      checkMethod = checkMethod,
      checkContentType = _.exists(checkContentType),
      headerName = config.get[String]("header.name"),
      headerBypass = config.get[Boolean]("header.bypass")
    )
  }
}

@Singleton
class CSRFConfigProvider @Inject() (config: Configuration) extends Provider[CSRFConfig] {
  lazy val get = CSRFConfig.fromConfiguration(config)
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

  /**
   * Extract token from current request
   */
  def getToken(request: RequestHeader): Option[Token] = {
    val global = CSRFConfig.global
    // First check the tags, this is where tokens are added if it's added to the current request
    val token = request.tags.get(Token.RequestTag)
      // Check cookie if cookie name is defined
      .orElse(global.cookieName.flatMap(n => request.cookies.get(n).map(_.value)))
      // Check session
      .orElse(request.session.get(global.tokenName))
    if (global.signTokens) {
      // Extract the signed token, and then resign it. This makes the token random per request, preventing the BREACH
      // vulnerability
      token.flatMap(Crypto.extractSignedToken)
        .map(token => Token(Crypto.signToken(token)))
    } else {
      token.map(Token.apply)
    }
  }

  /**
   * Extract token from current Java request
   *
   * @param request The request to extract the token from
   * @return The token, if found.
   */
  def getToken(request: play.mvc.Http.Request): Optional[Token] = {
    Optional.ofNullable(getToken(request._underlyingHeader()).orNull)
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

  class TokenProviderProvider @Inject() (config: CSRFConfig) extends Provider[TokenProvider] {
    override val get = config.signTokens match {
      case true => SignedTokenProvider
      case false => UnsignedTokenProvider
    }
  }

  class ConfigTokenProvider(config: => CSRFConfig) extends TokenProvider {
    lazy val underlying = new TokenProviderProvider(config).get
    def generateToken = underlying.generateToken
    def compareTokens(tokenA: String, tokenB: String) = underlying.compareTokens(tokenA, tokenB)
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

  class JavaCSRFErrorHandlerAdapter @Inject() (underlying: CSRFErrorHandler) extends ErrorHandler {
    def handle(request: RequestHeader, msg: String) =
      JavaHelpers.invokeWithContext(request, req => underlying.handle(req, msg))
  }

  class JavaCSRFErrorHandlerDelegate @Inject() (delegate: ErrorHandler) extends CSRFErrorHandler {
    import play.api.libs.iteratee.Execution.Implicits.trampoline

    def handle(req: Http.RequestHeader, msg: String) =
      play.libs.F.Promise.wrap(delegate.handle(req._underlyingHeader(), msg).map(_.asJava))
  }

  object ErrorHandler {
    def bindingsFromConfiguration(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
      Reflect.bindingsFromConfiguration[ErrorHandler, CSRFErrorHandler, JavaCSRFErrorHandlerAdapter, JavaCSRFErrorHandlerDelegate, CSRFHttpErrorHandler](environment, PlayConfig(configuration),
        "play.filters.csrf.errorHandler", "CSRFErrorHandler")
    }
  }

}

/**
 * The CSRF module.
 */
class CSRFModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[CSRFConfig].toProvider[CSRFConfigProvider],
      bind[CSRF.TokenProvider].toProvider[CSRF.TokenProviderProvider],
      bind[CSRFFilter].toSelf
    ) ++ ErrorHandler.bindingsFromConfiguration(environment, configuration)
  }
}

/**
 * The CSRF components.
 */
trait CSRFComponents {
  def configuration: Configuration
  def httpErrorHandler: HttpErrorHandler
  implicit def flowMaterializer: Materializer

  lazy val csrfConfig: CSRFConfig = CSRFConfig.fromConfiguration(configuration)
  lazy val csrfTokenProvider: CSRF.TokenProvider = new CSRF.TokenProviderProvider(csrfConfig).get
  lazy val csrfErrorHandler: CSRF.ErrorHandler = new CSRFHttpErrorHandler(httpErrorHandler)
  lazy val csrfFilter: CSRFFilter = new CSRFFilter(csrfConfig, csrfTokenProvider, csrfErrorHandler)
}
