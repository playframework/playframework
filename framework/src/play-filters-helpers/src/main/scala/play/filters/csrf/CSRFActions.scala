/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.csrf

import play.api.mvc._
import play.api.http.HeaderNames._
import play.filters.csrf.CSRF._
import play.api.libs.iteratee._
import play.api.mvc.BodyParsers.parse._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

/**
 * An action that provides CSRF protection.
 *
 * @param config The CSRF configuration.
 * @param tokenProvider A token provider to use.
 * @param next The composed action that is being protected.
 * @param errorHandler handling failed token error.
 */
class CSRFAction(next: EssentialAction,
    config: CSRFConfig = CSRFConfig(),
    tokenProvider: TokenProvider = SignedTokenProvider,
    errorHandler: => ErrorHandler = CSRF.DefaultErrorHandler) extends EssentialAction {

  import CSRFAction._

  // An iteratee that returns a forbidden result saying the CSRF check failed
  private def checkFailed(req: RequestHeader, msg: String): Iteratee[Array[Byte], Result] =
    Iteratee.flatten(clearTokenIfInvalid(req, config, errorHandler, msg) map (Done(_)))

  def apply(request: RequestHeader) = {

    // this function exists purely to aid readability
    def continue = next(request)

    // Only filter unsafe methods and content types
    if (config.checkMethod(request.method) && config.checkContentType(request.contentType)) {

      if (checkCsrfBypass(request, config)) {
        continue
      } else {

        // Only proceed with checks if there is an incoming token in the header, otherwise there's no point
        getTokenFromHeader(request, config).map { headerToken =>

          // First check if there's a token in the query string or header, if we find one, don't bother handling the body
          getTokenFromQueryString(request, config).map { queryStringToken =>

            if (tokenProvider.compareTokens(headerToken, queryStringToken)) {
              filterLogger.trace("[CSRF] Valid token found in query string")
              continue
            } else {
              filterLogger.trace("[CSRF] Check failed because invalid token found in query string: " + queryStringToken)
              checkFailed(request, "Bad CSRF token found in query String")
            }

          } getOrElse {

            // Check the body
            request.contentType match {
              case Some("application/x-www-form-urlencoded") => checkFormBody(request, headerToken, config.tokenName, next)
              case Some("multipart/form-data") => checkMultipartBody(request, headerToken, config.tokenName, next)
              // No way to extract token from other content types
              case Some(content) =>
                filterLogger.trace(s"[CSRF] Check failed because $content request")
                checkFailed(request, s"No CSRF token found for $content body")
              case None =>
                filterLogger.trace(s"[CSRF] Check failed because request without content type")
                checkFailed(request, s"No CSRF token found for body without content type")
            }

          }
        } getOrElse {

          filterLogger.trace("[CSRF] Check failed because no token found in headers")
          checkFailed(request, "No CSRF token found in headers")

        }
      }
    } else if (getTokenFromHeader(request, config).isEmpty && config.createIfNotFound(request)) {

      // No token in header and we have to create one if not found, so create a new token
      val newToken = tokenProvider.generateToken

      // The request
      val requestWithNewToken = request.copy(tags = request.tags + (Token.RequestTag -> newToken))

      // Once done, add it to the result
      next(requestWithNewToken).map(result =>
        CSRFAction.addTokenToResponse(config, newToken, request, result))

    } else {
      filterLogger.trace("[CSRF] No check necessary")
      next(request)
    }
  }

  private def checkFormBody = checkBody[Map[String, Seq[String]]](tolerantFormUrlEncoded, identity) _
  private def checkMultipartBody = checkBody[MultipartFormData[Unit]](multipartFormData[Unit]({
    case _ => Iteratee.ignore[Array[Byte]].map(_ => MultipartFormData.FilePart("", "", None, ()))
  }), _.dataParts) _

  private def checkBody[T](parser: BodyParser[T], extractor: (T => Map[String, Seq[String]]))(request: RequestHeader, tokenFromHeader: String, tokenName: String, next: EssentialAction) = {
    // Take up to 100kb of the body
    val firstPartOfBody: Iteratee[Array[Byte], Array[Byte]] =
      Traversable.take[Array[Byte]](config.postBodyBuffer.asInstanceOf[Int]) &>> Iteratee.consume[Array[Byte]]()

    firstPartOfBody.flatMap { bytes: Array[Byte] =>
      // Parse the first 100kb
      val parsedBody = Enumerator(bytes) |>>> parser(request)

      Iteratee.flatten(parsedBody.map { parseResult =>
        val validToken = parseResult.fold(
          // error parsing the body, we couldn't find a valid token
          _ => false,
          // extract the token and verify
          body => (for {
            values <- extractor(body).get(tokenName)
            token <- values.headOption
          } yield tokenProvider.compareTokens(token, tokenFromHeader)).getOrElse(false)
        )

        if (validToken) {
          // Feed the buffered bytes into the next request, and return the iteratee
          filterLogger.trace("[CSRF] Valid token found in body")
          Iteratee.flatten(Enumerator(bytes) |>> next(request))
        } else {
          filterLogger.trace("[CSRF] Check failed because no or invalid token found in body")
          checkFailed(request, "Invalid token found in form body")
        }
      })
    }
  }

}

object CSRFAction {

  /**
   * Get the header token, that is, the token that should be validated.
   */
  private[csrf] def getTokenFromHeader(request: RequestHeader, config: CSRFConfig) = {
    val cookieToken = config.cookieName.flatMap(cookie => request.cookies.get(cookie).map(_.value))
    val sessionToken = request.session.get(config.tokenName)
    cookieToken orElse sessionToken
  }

  private[csrf] def getTokenFromQueryString(request: RequestHeader, config: CSRFConfig) = {
    val queryStringToken = request.getQueryString(config.tokenName)
    val headerToken = request.headers.get(config.headerName)

    queryStringToken orElse headerToken
  }

  private[csrf] def checkCsrfBypass(request: RequestHeader, config: CSRFConfig): Boolean = {
    if (config.headerBypass) {
      if (request.headers.get(config.headerName).exists(_ == CSRFConfig.HeaderNoCheck)) {

        // Since injecting arbitrary header values is not possible with a CSRF attack, the presence of this header
        // indicates that this is not a CSRF attack
        filterLogger.trace("[CSRF] Bypassing check because " + config.headerName + ": " + CSRFConfig.HeaderNoCheck + " header found")
        true

      } else if (request.headers.get("X-Requested-With").isDefined) {

        // AJAX requests are not CSRF attacks either because they are restricted to same origin policy
        filterLogger.trace("[CSRF] Bypassing check because X-Requested-With header found")
        true
      } else {
        false
      }
    } else {
      false
    }
  }

  private[csrf] def addTokenToResponse(config: CSRFConfig, newToken: String, request: RequestHeader, result: Result) = {

    if (isCached(result)) {
      filterLogger.trace("[CSRF] Not adding token to cached response")
      result
    } else {
      filterLogger.trace("[CSRF] Adding token to result: " + result)

      config.cookieName.map {
        // cookie
        name =>
          result.withCookies(Cookie(name, newToken, path = Session.path, domain = Session.domain,
            secure = config.secureCookie))
      } getOrElse {

        val newSession = result.session(request) + (config.tokenName -> newToken)
        result.withSession(newSession)
      }
    }

  }

  private[csrf] def isCached(result: Result): Boolean =
    result.header.headers.get(CACHE_CONTROL).fold(false)(!_.contains("no-cache"))

  private[csrf] def clearTokenIfInvalid(request: RequestHeader, config: CSRFConfig, errorHandler: ErrorHandler, msg: String): Future[Result] = {

    errorHandler.handle(request, msg) map { result =>
      CSRF.getToken(request).fold(
        config.cookieName.flatMap { cookie =>
          request.cookies.get(cookie).map { token =>
            result.discardingCookies(DiscardingCookie(cookie, domain = Session.domain, path = Session.path,
              secure = config.secureCookie))
          }
        }.getOrElse {
          result.withSession(result.session(request) - config.tokenName)
        }
      )(_ => result)
    }
  }
}

/**
 * CSRF check action.
 *
 * Apply this to all actions that require a CSRF check.
 */
object CSRFCheck {

  private class CSRFCheckAction[A](config: CSRFConfig, tokenProvider: TokenProvider, errorHandler: ErrorHandler,
      wrapped: Action[A]) extends Action[A] {
    def parser = wrapped.parser
    def apply(request: Request[A]) = {

      // Maybe bypass
      if (CSRFAction.checkCsrfBypass(request, config) || !config.checkContentType(request.contentType)) {
        wrapped(request)
      } else {
        // Get token from header
        CSRFAction.getTokenFromHeader(request, config).flatMap { headerToken =>
          // Get token from query string
          CSRFAction.getTokenFromQueryString(request, config)
            // Or from body if not found
            .orElse({
              val form = request.body match {
                case body: play.api.mvc.AnyContent if body.asFormUrlEncoded.isDefined => body.asFormUrlEncoded.get
                case body: play.api.mvc.AnyContent if body.asMultipartFormData.isDefined => body.asMultipartFormData.get.asFormUrlEncoded
                case body: Map[_, _] => body.asInstanceOf[Map[String, Seq[String]]]
                case body: play.api.mvc.MultipartFormData[_] => body.asFormUrlEncoded
                case _ => Map.empty[String, Seq[String]]
              }
              form.get(config.tokenName).flatMap(_.headOption)
            })
            // Execute if it matches
            .collect {
              case queryToken if tokenProvider.compareTokens(queryToken, headerToken) => wrapped(request)
            }
        }.getOrElse {
          CSRFAction.clearTokenIfInvalid(request, config, errorHandler, "CSRF token check failed")
        }
      }
    }
  }

  /**
   * Wrap an action in a CSRF check.
   */
  def apply[A](action: Action[A], errorHandler: ErrorHandler = CSRF.DefaultErrorHandler, config: CSRFConfig = CSRFConfig.global): Action[A] =
    new CSRFCheckAction(config, new TokenProviderProvider(config).get, errorHandler, action)
}

/**
 * CSRF add token action.
 *
 * Apply this to all actions that render a form that contains a CSRF token.
 */
object CSRFAddToken {

  private class CSRFAddTokenAction[A](config: CSRFConfig, tokenProvider: TokenProvider, wrapped: Action[A]) extends Action[A] {
    def parser = wrapped.parser
    def apply(request: Request[A]) = {
      if (CSRFAction.getTokenFromHeader(request, config).isEmpty) {
        // No token in header and we have to create one if not found, so create a new token
        val newToken = tokenProvider.generateToken

        // The request
        val requestWithNewToken = new WrappedRequest(request) {
          override val tags = request.tags + (Token.RequestTag -> newToken)
        }

        // Once done, add it to the result
        wrapped(requestWithNewToken).map(result =>
          CSRFAction.addTokenToResponse(config, newToken, request, result))
      } else {
        wrapped(request)
      }
    }
  }

  /**
   * Wrap an action in an action that ensures there is a CSRF token.
   */
  def apply[A](action: Action[A], config: CSRFConfig = CSRFConfig.global): Action[A] =
    new CSRFAddTokenAction(config, new TokenProviderProvider(config).get, action)
}
