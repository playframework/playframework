/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.csrf

import java.net.{ URLDecoder, URLEncoder }
import java.util.Locale
import javax.inject.Inject

import akka.stream.Materializer
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source }
import akka.stream.stage.{ DetachedContext, DetachedStage }
import akka.util.ByteString
import play.api.http.HeaderNames._
import play.api.libs.Crypto
import play.api.libs.crypto.CSRFTokenSigner
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.core.parsers.Multipart
import play.filters.cors.CORSFilter
import play.filters.csrf.CSRF._

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
    tokenSigner: CSRFTokenSigner = Crypto.crypto,
    tokenProvider: TokenProvider = new SignedTokenProvider(Crypto.crypto),
    errorHandler: => ErrorHandler = CSRF.DefaultErrorHandler)(implicit mat: Materializer) extends EssentialAction {

  import CSRFAction._
  import play.api.libs.iteratee.Execution.Implicits.trampoline

  private def checkFailed(req: RequestHeader, msg: String): Accumulator[ByteString, Result] =
    Accumulator.done(clearTokenIfInvalid(req, config, errorHandler, msg))

  def apply(untaggedRequest: RequestHeader) = {
    val request = tagRequestFromHeader(untaggedRequest, config, tokenSigner)

    // this function exists purely to aid readability
    def continue = next(request)

    // Only filter unsafe methods and content types
    if (config.checkMethod(request.method) && config.checkContentType(request.contentType)) {

      if (!requiresCsrfCheck(request, config)) {
        continue
      } else {

        // Only proceed with checks if there is an incoming token in the header, otherwise there's no point
        getTokenToValidate(request, config, tokenSigner).map { headerToken =>

          // First check if there's a token in the query string or header, if we find one, don't bother handling the body
          getHeaderToken(request, config).map { queryStringToken =>

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
              case Some("application/x-www-form-urlencoded") =>
                checkFormBody(request, next, headerToken, config.tokenName)
              case Some("multipart/form-data") =>
                checkMultipartBody(request, next, headerToken, config.tokenName)
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
    } else if (getTokenToValidate(request, config, tokenSigner).isEmpty && config.createIfNotFound(request)) {

      // No token in header and we have to create one if not found, so create a new token
      val newToken = tokenProvider.generateToken

      // The request
      val requestWithNewToken = tagRequest(request, Token(config.tokenName, newToken))

      // Once done, add it to the result
      next(requestWithNewToken).map(result =>
        CSRFAction.addTokenToResponse(config, newToken, request, result))

    } else {
      filterLogger.trace("[CSRF] No check necessary")
      next(request)
    }
  }

  private def checkFormBody = checkBody(extractTokenFromFormBody) _
  private def checkMultipartBody(request: RequestHeader, action: EssentialAction, tokenFromHeader: String, tokenName: String) = {
    (for {
      mt <- request.mediaType
      maybeBoundary <- mt.parameters.find(_._1.equalsIgnoreCase("boundary"))
      boundary <- maybeBoundary._2
    } yield {
      checkBody(extractTokenFromMultipartFormDataBody(ByteString(boundary)))(request, action, tokenFromHeader, tokenName)
    }).getOrElse(checkFailed(request, "No boundary found in multipart/form-data request"))
  }

  private def checkBody[T](extractor: (ByteString, String) => Option[String])(request: RequestHeader, action: EssentialAction, tokenFromHeader: String, tokenName: String) = {
    // We need to ensure that the action isn't actually executed until the body is validated.
    // To do that, we use Flow.splitWhen(_ => false).  This basically says, give me a Source
    // containing all the elements when you receive the first element.  Our BodyHandler doesn't
    // output any part of the body until it has validated the CSRF check, so we know that
    // the source is validated. Then using a Sink.head, we turn that Source into an Accumulator,
    // which we can then map to execute and feed into our action.
    // CSRF check failures are used by failing the stream with a NoTokenInBody exception.
    Accumulator(
      Flow[ByteString].transform(() => new BodyHandler(config, { body =>
        if (extractor(body, tokenName).fold(false)(tokenProvider.compareTokens(_, tokenFromHeader))) {
          filterLogger.trace("[CSRF] Valid token found in body")
          true
        } else {
          filterLogger.trace("[CSRF] Check failed because no or invalid token found in body")
          false
        }
      }))
        .splitWhen(_ => false)
        .prefixAndTail(0)
        .map(_._2)
        .concatSubstreams
        .toMat(Sink.head[Source[ByteString, _]])(Keep.right)
    ).mapFuture { validatedBodySource =>
        action(request).run(validatedBodySource)
      }.recoverWith {
        case NoTokenInBody => clearTokenIfInvalid(request, config, errorHandler, "No CSRF token found in body")
      }
  }

  /**
   * Does a very simple parse of the form body to find the token, if it exists.
   */
  private def extractTokenFromFormBody(body: ByteString, tokenName: String): Option[String] = {
    val tokenEquals = ByteString(URLEncoder.encode(tokenName, "utf-8")) ++ ByteString('=')

    // First check if it's the first token
    if (body.startsWith(tokenEquals)) {
      Some(URLDecoder.decode(body.drop(tokenEquals.size).takeWhile(_ != '&').utf8String, "utf-8"))
    } else {
      val andTokenEquals = ByteString('&') ++ tokenEquals
      val index = body.indexOfSlice(andTokenEquals)
      if (index == -1) {
        None
      } else {
        Some(URLDecoder.decode(body.drop(index + andTokenEquals.size).takeWhile(_ != '&').utf8String, "utf-8"))
      }
    }
  }

  /**
   * Does a very simple multipart/form-data parse to find the token if it exists.
   */
  private def extractTokenFromMultipartFormDataBody(boundary: ByteString)(body: ByteString, tokenName: String): Option[String] = {
    val crlf = ByteString("\r\n")
    val boundaryLine = ByteString("\r\n--") ++ boundary

    /**
     * A boundary will start with CRLF, unless it's the first boundary in the body.  So that we don't have to handle
     * the first boundary differently, prefix the whole body with CRLF.
     */
    val prefixedBody = crlf ++ body

    /**
     * Extract the headers from the given position.
     *
     * This is invoked recursively, and exits when it reaches the end of stream, or a blank line (indicating end of
     * headers).  It returns the headers, and the position of the first byte after the headers.  The headers are all
     * converted to lower case.
     */
    def extractHeaders(position: Int): (Int, List[(String, String)]) = {
      // If it starts with CRLF, we've reached the end of the headers
      if (prefixedBody.startsWith(crlf, position)) {
        (position + 2) -> Nil
      } else {
        // Read up to the next CRLF
        val nextCrlf = prefixedBody.indexOfSlice(crlf, position)
        if (nextCrlf == -1) {
          // Technically this is a protocol error
          position -> Nil
        } else {
          val header = prefixedBody.slice(position, nextCrlf).utf8String
          header.split(":", 2) match {
            case Array(_) =>
              // Bad header, ignore
              extractHeaders(nextCrlf + 2)
            case Array(key, value) =>
              val (endIndex, headers) = extractHeaders(nextCrlf + 2)
              endIndex -> ((key.trim().toLowerCase(Locale.ENGLISH) -> value.trim()) :: headers)
          }
        }
      }
    }

    /**
     * Find the token.
     *
     * This is invoked recursively, once for each part found.  It finds the start of the next part, then extracts
     * the headers, and if the header has a name of our token name, then it extracts the body, and returns that,
     * otherwise it moves onto the next part.
     */
    def findToken(position: Int): Option[String] = {
      // Find the next boundary from position
      prefixedBody.indexOfSlice(boundaryLine, position) match {
        case -1 => None
        case nextBoundary =>
          // Progress past the CRLF at the end of the boundary
          val nextCrlf = prefixedBody.indexOfSlice(crlf, nextBoundary + boundaryLine.size)
          if (nextCrlf == -1) {
            None
          } else {
            val startOfNextPart = nextCrlf + 2
            // Extract the headers
            val (startOfPartData, headers) = extractHeaders(startOfNextPart)
            headers.toMap match {
              case Multipart.PartInfoMatcher(name) if name == tokenName =>
                // This part is the token, find the next boundary
                val endOfData = prefixedBody.indexOfSlice(boundaryLine, startOfPartData)
                if (endOfData == -1) {
                  None
                } else {
                  // Extract the token value
                  Some(prefixedBody.slice(startOfPartData, endOfData).utf8String)
                }
              case _ =>
                // Find the next part
                findToken(startOfPartData)
            }
          }
      }
    }

    findToken(0)
  }

}

/**
 * A body handler.
 *
 * This will buffer the body until it reaches the end of stream, or until the buffer limit is reached.
 *
 * Once it has finished buffering, it will attempt to find the token in the body, and if it does, validates it,
 * failing the stream if it's invalid.  If it's valid, it forwards the buffered body, and then stops buffering and
 * continues forwarding the body as is (or finishes if the stream was finished).
 */
private class BodyHandler(config: CSRFConfig, checkBody: ByteString => Boolean) extends DetachedStage[ByteString, ByteString] {
  var buffer: ByteString = ByteString.empty
  var next: ByteString = null
  var continue = false

  def onPush(elem: ByteString, ctx: DetachedContext[ByteString]) = {
    if (continue) {
      // Standard contract for forwarding as is in DetachedStage
      if (ctx.isHoldingDownstream) {
        ctx.pushAndPull(elem)
      } else {
        next = elem
        ctx.holdUpstream()
      }
    } else {
      if (buffer.size + elem.size > config.postBodyBuffer) {
        // We've finished buffering up to the configured limit, try to validate
        buffer ++= elem
        if (checkBody(buffer)) {
          // Switch to continue, and push the buffer
          continue = true
          if (ctx.isHoldingDownstream) {
            val toPush = buffer
            buffer = null
            ctx.pushAndPull(toPush)
          } else {
            next = buffer
            buffer = null
            ctx.holdUpstream()
          }
        } else {
          // CSRF check failed
          ctx.fail(CSRFAction.NoTokenInBody)
        }
      } else {
        // Buffer
        buffer ++= elem
        ctx.pull()
      }
    }
  }

  def onPull(ctx: DetachedContext[ByteString]) = {
    if (continue) {
      // Standard contract for forwarding as is in DetachedStage
      if (next != null) {
        val toPush = next
        next = null
        if (ctx.isFinishing) {
          ctx.pushAndFinish(toPush)
        } else {
          ctx.pushAndPull(toPush)
        }
      } else {
        if (ctx.isFinishing) {
          ctx.finish()
        } else {
          ctx.holdDownstream()
        }
      }
    } else {
      // Otherwise hold because we're buffering
      ctx.holdDownstream()
    }
  }

  override def onUpstreamFinish(ctx: DetachedContext[ByteString]) = {
    if (continue) {
      if (next != null) {
        ctx.absorbTermination()
      } else {
        ctx.finish()
      }
    } else {
      // CSRF check
      if (checkBody(buffer)) {
        // Absorb the termination, hold the buffer, and enter the continue state.
        // Even if we're holding downstream, Akka streams will send another onPull so that we can flush it.
        next = buffer
        buffer = null
        continue = true
        ctx.absorbTermination()
      } else {
        ctx.fail(CSRFAction.NoTokenInBody)
      }
    }
  }
}

object CSRFAction {

  private[csrf] object NoTokenInBody extends RuntimeException(null, null, false, false)

  /**
   * Get the header token, that is, the token that should be validated.
   */
  private[csrf] def getTokenToValidate(request: RequestHeader, config: CSRFConfig, tokenSigner: CSRFTokenSigner) = {
    val tagToken = request.tags.get(Token.RequestTag)
    val cookieToken = config.cookieName.flatMap(cookie => request.cookies.get(cookie).map(_.value))
    val sessionToken = request.session.get(config.tokenName)
    cookieToken orElse sessionToken orElse tagToken filter { token =>
      // return None if the token is invalid
      !config.signTokens || tokenSigner.extractSignedToken(token).isDefined
    }
  }

  /**
   * Tag incoming requests with the token in the header
   */
  private[csrf] def tagRequestFromHeader(request: RequestHeader, config: CSRFConfig, tokenSigner: CSRFTokenSigner): RequestHeader = {
    getTokenToValidate(request, config, tokenSigner).fold(request) { tokenValue =>
      val token = Token(config.tokenName, tokenValue)
      val newReq = tagRequest(request, token)
      if (config.signTokens) {
        // Extract the signed token, and then resign it. This makes the token random per request, preventing the BREACH
        // vulnerability
        val newTokenValue = tokenSigner.extractSignedToken(token.value).map(tokenSigner.signToken)
        newTokenValue.fold(newReq)(newReq.withTag(Token.ReSignedRequestTag, _))
      } else {
        newReq
      }
    }
  }

  private[csrf] def tagRequestFromHeader[A](request: Request[A], config: CSRFConfig, tokenSigner: CSRFTokenSigner): Request[A] = {
    Request(tagRequestFromHeader(request: RequestHeader, config, tokenSigner), request.body)
  }

  private[csrf] def tagRequest(request: RequestHeader, token: Token): RequestHeader = {
    request.copy(tags = request.tags ++ Map(
      Token.NameRequestTag -> token.name,
      Token.RequestTag -> token.value
    ))
  }

  private[csrf] def tagRequest[A](request: Request[A], token: Token): Request[A] = {
    Request(tagRequest(request: RequestHeader, token), request.body)
  }

  private[csrf] def getHeaderToken(request: RequestHeader, config: CSRFConfig) = {
    val queryStringToken = request.getQueryString(config.tokenName)
    val headerToken = request.headers.get(config.headerName)

    queryStringToken orElse headerToken
  }

  private[csrf] def requiresCsrfCheck(request: RequestHeader, config: CSRFConfig): Boolean = {
    if (config.bypassCorsTrustedOrigins && request.tags.contains(CORSFilter.RequestTag)) {
      filterLogger.trace("[CSRF] Bypassing check because CORSFilter request tag found")
      false
    } else {
      config.shouldProtect(request)
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
            secure = config.secureCookie, httpOnly = config.httpOnlyCookie))
      } getOrElse {

        val newSession = result.session(request) + (config.tokenName -> newToken)
        result.withSession(newSession)
      }
    }

  }

  private[csrf] def isCached(result: Result): Boolean =
    result.header.headers.get(CACHE_CONTROL).fold(false)(!_.contains("no-cache"))

  private[csrf] def clearTokenIfInvalid(request: RequestHeader, config: CSRFConfig, errorHandler: ErrorHandler, msg: String): Future[Result] = {
    import play.api.libs.iteratee.Execution.Implicits.trampoline

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
case class CSRFCheck @Inject() (config: CSRFConfig, tokenSigner: CSRFTokenSigner) {

  private class CSRFCheckAction[A](tokenProvider: TokenProvider, errorHandler: ErrorHandler, wrapped: Action[A]) extends Action[A] {
    def parser = wrapped.parser
    def apply(untaggedRequest: Request[A]) = {
      val request = CSRFAction.tagRequestFromHeader(untaggedRequest, config, tokenSigner)

      // Maybe bypass
      if (!CSRFAction.requiresCsrfCheck(request, config) || !config.checkContentType(request.contentType)) {
        wrapped(request)
      } else {
        // Get token from header
        CSRFAction.getTokenToValidate(request, config, tokenSigner).flatMap { headerToken =>
          // Get token from query string
          CSRFAction.getHeaderToken(request, config)
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
  def apply[A](action: Action[A], errorHandler: ErrorHandler): Action[A] =
    new CSRFCheckAction(new TokenProviderProvider(config, tokenSigner).get, errorHandler, action)

  /**
   * Wrap an action in a CSRF check.
   */
  def apply[A](action: Action[A]): Action[A] =
    new CSRFCheckAction(new TokenProviderProvider(config, tokenSigner).get, CSRF.DefaultErrorHandler, action)
}

object CSRFCheck {
  @deprecated("Use CSRFCheck class with dependency injection instead", "2.5.0")
  def apply[A](action: Action[A], errorHandler: ErrorHandler = CSRF.DefaultErrorHandler, config: CSRFConfig = CSRFConfig.global, tokenSigner: CSRFTokenSigner = Crypto.crypto): Action[A] = {
    CSRFCheck(config, tokenSigner)(action, errorHandler)
  }
}

/**
 * CSRF add token action.
 *
 * Apply this to all actions that render a form that contains a CSRF token.
 */
case class CSRFAddToken @Inject() (config: CSRFConfig, crypto: CSRFTokenSigner) {

  private class CSRFAddTokenAction[A](config: CSRFConfig, tokenProvider: TokenProvider, wrapped: Action[A]) extends Action[A] {
    def parser = wrapped.parser
    def apply(untaggedRequest: Request[A]) = {
      val request = CSRFAction.tagRequestFromHeader(untaggedRequest, config, crypto)

      if (CSRFAction.getTokenToValidate(request, config, crypto).isEmpty) {
        // No token in header and we have to create one if not found, so create a new token
        val newToken = tokenProvider.generateToken

        // The request
        val requestWithNewToken = CSRFAction.tagRequest(request, Token(config.tokenName, newToken))

        // Once done, add it to the result
        import play.api.libs.iteratee.Execution.Implicits.trampoline
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
  def apply[A](action: Action[A]): Action[A] =
    new CSRFAddTokenAction(config, new TokenProviderProvider(config, crypto).get, action)
}
object CSRFAddToken {
  @deprecated("Use CSRFAddToken class with dependency injection instead", "2.5.0")
  def apply[A](action: Action[A], config: CSRFConfig = CSRFConfig.global, tokenSigner: CSRFTokenSigner = Crypto.crypto): Action[A] =
    CSRFAddToken(config, tokenSigner)(action)
}
