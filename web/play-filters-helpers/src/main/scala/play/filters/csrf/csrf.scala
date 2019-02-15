/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csrf

import java.util.Optional
import javax.inject.{ Inject, Provider, Singleton }

import akka.stream.Materializer
import com.typesafe.config.ConfigMemorySize
import play.api._
import play.api.http.{ HttpConfiguration, HttpErrorHandler }
import play.api.inject.{ Binding, Module, bind }
import play.api.libs.crypto.{ CSRFTokenSigner, CSRFTokenSignerProvider }
import play.api.libs.typedmap.TypedKey
import play.api.mvc.Cookie.SameSite
import play.api.mvc.Results._
import play.api.mvc._
import play.core.j.{ JavaContextComponents, JavaHelpers }
import play.filters.csrf.CSRF.{ CSRFHttpErrorHandler, _ }
import play.mvc.Http
import play.utils.Reflect

import scala.compat.java8.FutureConverters
import scala.concurrent.Future

/**
 * CSRF configuration.
 *
 * @param tokenName The name of the token.
 * @param cookieName If defined, the name of the cookie to read the token from/write the token to.
 * @param secureCookie If using a cookie, whether it should be secure.
 * @param httpOnlyCookie If using a cookie, whether it should have the HTTP only flag.
 * @param sameSiteCookie If using a cookie, the cookie's SameSite attribute.
 * @param postBodyBuffer How much of the POST body should be buffered if checking the body for a token.
 * @param signTokens Whether tokens should be signed.
 * @param checkMethod Returns true if a request for that method should be checked.
 * @param checkContentType Returns true if a request for that content type should be checked.
 * @param headerName The name of the HTTP header to check for tokens from.
 * @param shouldProtect A function that decides based on the headers of the request if a check is needed.
 * @param bypassCorsTrustedOrigins Whether to bypass the CSRF check if the CORS filter trusts this origin
 */
case class CSRFConfig(
    tokenName: String = "csrfToken",
    cookieName: Option[String] = None,
    secureCookie: Boolean = false,
    httpOnlyCookie: Boolean = false,
    sameSiteCookie: Option[SameSite] = Some(SameSite.Lax),
    createIfNotFound: RequestHeader => Boolean = CSRFConfig.defaultCreateIfNotFound,
    postBodyBuffer: Long = 102400,
    signTokens: Boolean = true,
    checkMethod: String => Boolean = !CSRFConfig.SafeMethods.contains(_),
    checkContentType: Option[String] => Boolean = _ => true,
    headerName: String = "Csrf-Token",
    shouldProtect: RequestHeader => Boolean = _ => false,
    bypassCorsTrustedOrigins: Boolean = true) {

  // Java builder methods
  def this() = this(cookieName = None)

  import java.{ util => ju }

  import play.mvc.Http.{ RequestHeader => JRequestHeader }

  import scala.compat.java8.FunctionConverters._
  import scala.compat.java8.OptionConverters._

  def withTokenName(tokenName: String) = copy(tokenName = tokenName)
  def withHeaderName(headerName: String) = copy(headerName = headerName)
  def withCookieName(cookieName: ju.Optional[String]) = copy(cookieName = cookieName.asScala)
  def withSecureCookie(isSecure: Boolean) = copy(secureCookie = isSecure)
  def withHttpOnlyCookie(isHttpOnly: Boolean) = copy(httpOnlyCookie = isHttpOnly)
  def withSameSiteCookie(sameSite: Option[SameSite]) = copy(sameSiteCookie = sameSite)
  def withCreateIfNotFound(pred: ju.function.Predicate[JRequestHeader]) =
    copy(createIfNotFound = pred.asScala.compose(_.asJava))
  def withPostBodyBuffer(bufsize: Long) = copy(postBodyBuffer = bufsize)
  def withSignTokens(signTokens: Boolean) = copy(signTokens = signTokens)
  def withMethods(checkMethod: ju.function.Predicate[String]) = copy(checkMethod = checkMethod.asScala)
  def withContentTypes(checkContentType: ju.function.Predicate[Optional[String]]) =
    copy(checkContentType = checkContentType.asScala.compose(_.asJava))
  def withShouldProtect(shouldProtect: ju.function.Predicate[JRequestHeader]) =
    copy(shouldProtect = shouldProtect.asScala.compose(_.asJava))
  def withBypassCorsTrustedOrigins(bypass: Boolean) = copy(bypassCorsTrustedOrigins = bypass)
}

object CSRFConfig {
  private val SafeMethods = Set("GET", "HEAD", "OPTIONS")

  private def defaultCreateIfNotFound(request: RequestHeader) = {
    // If the request isn't accepting HTML, then it won't be rendering a form, so there's no point in generating a
    // CSRF token for it.
    import play.api.http.MimeTypes._
    (request.method == "GET" || request.method == "HEAD") && (request.accepts(HTML) || request.accepts(XHTML))
  }

  def fromConfiguration(conf: Configuration): CSRFConfig = {
    val config = conf.getDeprecatedWithFallback("play.filters.csrf", "csrf")

    val methodWhiteList = config.get[Seq[String]]("method.whiteList").toSet
    val methodBlackList = config.get[Seq[String]]("method.blackList").toSet

    val checkMethod: String => Boolean = if (methodWhiteList.nonEmpty) {
      !methodWhiteList.contains(_)
    } else {
      if (methodBlackList.isEmpty) {
        _ => true
      } else {
        methodBlackList.contains
      }
    }

    val contentTypeWhiteList = config.get[Seq[String]]("contentType.whiteList").toSet
    val contentTypeBlackList = config.get[Seq[String]]("contentType.blackList").toSet

    val checkContentType: Option[String] => Boolean = if (contentTypeWhiteList.nonEmpty) {
      _.forall(!contentTypeWhiteList.contains(_))
    } else {
      if (contentTypeBlackList.isEmpty) {
        _ => true
      } else {
        _.exists(contentTypeBlackList.contains)
      }
    }

    val whitelistModifiers = config.get[Seq[String]]("routeModifiers.whiteList")
    val blacklistModifiers = config.get[Seq[String]]("routeModifiers.blackList")
    @inline def checkRouteModifiers(rh: RequestHeader): Boolean = {
      import play.api.routing.Router.RequestImplicits._
      if (whitelistModifiers.isEmpty) {
        blacklistModifiers.exists(rh.hasRouteModifier)
      } else {
        !whitelistModifiers.exists(rh.hasRouteModifier)
      }
    }

    val protectHeaders = config.get[Option[Map[String, String]]]("header.protectHeaders").getOrElse(Map.empty)
    val bypassHeaders = config.get[Option[Map[String, String]]]("header.bypassHeaders").getOrElse(Map.empty)
    @inline def checkHeaders(rh: RequestHeader): Boolean = {
      @inline def foundHeaderValues(headersToCheck: Map[String, String]) = {
        headersToCheck.exists {
          case (name, "*") => rh.headers.get(name).isDefined
          case (name, value) => rh.headers.get(name).contains(value)
        }
      }
      (protectHeaders.isEmpty || foundHeaderValues(protectHeaders)) && !foundHeaderValues(bypassHeaders)
    }

    val shouldProtect: RequestHeader => Boolean = { rh => checkRouteModifiers(rh) && checkHeaders(rh) }

    CSRFConfig(
      tokenName = config.get[String]("token.name"),
      cookieName = config.get[Option[String]]("cookie.name"),
      secureCookie = config.get[Boolean]("cookie.secure"),
      httpOnlyCookie = config.get[Boolean]("cookie.httpOnly"),
      sameSiteCookie = HttpConfiguration.parseSameSite(config, "cookie.sameSite"),
      postBodyBuffer = config.get[ConfigMemorySize]("body.bufferSize").toBytes,
      signTokens = config.get[Boolean]("token.sign"),
      checkMethod = checkMethod,
      checkContentType = checkContentType,
      headerName = config.get[String]("header.name"),
      shouldProtect = shouldProtect,
      bypassCorsTrustedOrigins = config.get[Boolean]("bypassCorsTrustedOrigins")
    )
  }
}

@Singleton
class CSRFConfigProvider @Inject() (config: Configuration) extends Provider[CSRFConfig] {
  lazy val get = CSRFConfig.fromConfiguration(config)
}

object CSRF {

  private[csrf] val filterLogger = play.api.Logger("play.filters.CSRF")

  /**
   * A CSRF token
   */
  case class Token(name: String, value: String)

  /**
   * INTERNAL API: used for storing tokens on the request
   */
  class TokenInfo private (token: => Token) {

    private[this] var _rendered = false
    private[csrf] def wasRendered: Boolean = _rendered

    /**
     * Call this method to render the token.
     *
     * @return the generated token
     */
    lazy val toToken: Token = {
      _rendered = true
      token
    }
  }
  object TokenInfo {
    def apply(token: => Token): TokenInfo = new TokenInfo(token)
  }

  object Token {
    val InfoAttr: TypedKey[TokenInfo] = TypedKey("TOKEN_INFO")
  }

  /**
   * Extract token from current request
   */
  def getToken(implicit request: RequestHeader): Option[Token] = {
    request.attrs.get(Token.InfoAttr).map(_.toToken)
  }

  /**
   * Extract token from current Java request
   *
   * @param requestHeader The request to extract the token from
   * @return The token, if found.
   */
  def getToken(requestHeader: play.mvc.Http.RequestHeader): Optional[Token] = {
    Optional.ofNullable(getToken(requestHeader.asScala()).orNull)
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

  class TokenProviderProvider @Inject() (config: CSRFConfig, tokenSigner: CSRFTokenSigner) extends Provider[TokenProvider] {
    override val get = config.signTokens match {
      case true => new SignedTokenProvider(tokenSigner)
      case false => new UnsignedTokenProvider(tokenSigner)
    }
  }

  class ConfigTokenProvider(config: => CSRFConfig, tokenSigner: CSRFTokenSigner) extends TokenProvider {
    lazy val underlying = new TokenProviderProvider(config, tokenSigner).get
    def generateToken = underlying.generateToken
    override def compareTokens(tokenA: String, tokenB: String) = underlying.compareTokens(tokenA, tokenB)
  }

  class SignedTokenProvider(tokenSigner: CSRFTokenSigner) extends TokenProvider {
    def generateToken = tokenSigner.generateSignedToken
    def compareTokens(tokenA: String, tokenB: String) = tokenSigner.compareSignedTokens(tokenA, tokenB)
  }

  class UnsignedTokenProvider(tokenSigner: CSRFTokenSigner) extends TokenProvider {
    def generateToken = tokenSigner.generateToken
    override def compareTokens(tokenA: String, tokenB: String) = {
      java.security.MessageDigest.isEqual(tokenA.getBytes("utf-8"), tokenB.getBytes("utf-8"))
    }
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

  class JavaCSRFErrorHandlerAdapter @Inject() (underlying: CSRFErrorHandler, contextComponents: JavaContextComponents) extends ErrorHandler {
    def handle(request: RequestHeader, msg: String) =
      JavaHelpers.invokeWithContext(request, contextComponents, req => underlying.handle(req, msg))
  }

  class JavaCSRFErrorHandlerDelegate @Inject() (delegate: ErrorHandler) extends CSRFErrorHandler {
    import play.core.Execution.Implicits.trampoline

    def handle(requestHeader: Http.RequestHeader, msg: String) =
      FutureConverters.toJava(delegate.handle(requestHeader.asScala(), msg).map(_.asJava))
  }

  object ErrorHandler {
    def bindingsFromConfiguration(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
      Reflect.bindingsFromConfiguration[ErrorHandler, CSRFErrorHandler, JavaCSRFErrorHandlerAdapter, JavaCSRFErrorHandlerDelegate, CSRFHttpErrorHandler](environment, configuration,
        "play.filters.csrf.errorHandler", "CSRFErrorHandler")
    }
  }

}

/**
 * The CSRF module.
 */
class CSRFModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = Seq(
    bind[play.libs.crypto.CSRFTokenSigner].to(classOf[play.libs.crypto.DefaultCSRFTokenSigner]),
    bind[CSRFTokenSigner].toProvider[CSRFTokenSignerProvider],
    bind[CSRFConfig].toProvider[CSRFConfigProvider],
    bind[CSRF.TokenProvider].toProvider[CSRF.TokenProviderProvider],
    bind[CSRFFilter].toSelf
  ) ++ ErrorHandler.bindingsFromConfiguration(environment, configuration)
}

/**
 * The CSRF components.
 */
trait CSRFComponents {
  def configuration: Configuration
  def csrfTokenSigner: CSRFTokenSigner
  def httpErrorHandler: HttpErrorHandler
  def httpConfiguration: HttpConfiguration
  implicit def materializer: Materializer

  lazy val csrfConfig: CSRFConfig = CSRFConfig.fromConfiguration(configuration)
  lazy val csrfTokenProvider: CSRF.TokenProvider = new CSRF.TokenProviderProvider(csrfConfig, csrfTokenSigner).get
  lazy val csrfErrorHandler: CSRF.ErrorHandler = new CSRFHttpErrorHandler(httpErrorHandler)
  lazy val csrfFilter: CSRFFilter = new CSRFFilter(csrfConfig, csrfTokenSigner, httpConfiguration.session, csrfTokenProvider, csrfErrorHandler)
  lazy val csrfCheck: CSRFCheck = CSRFCheck(csrfConfig, csrfTokenSigner, httpConfiguration.session)
  lazy val csrfAddToken: CSRFAddToken = CSRFAddToken(csrfConfig, csrfTokenSigner, httpConfiguration.session)

}
