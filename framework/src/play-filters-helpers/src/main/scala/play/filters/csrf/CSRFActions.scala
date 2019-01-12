/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csrf

import java.net.{ URLDecoder, URLEncoder }
import java.util.Locale
import javax.inject.Inject

import akka.stream._
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source }
import akka.stream.stage._
import akka.util.ByteString
import play.api.MarkerContexts.SecurityMarkerContext
import play.api.http.HttpEntity
import play.api.http.HeaderNames._
import play.api.http.SessionConfiguration
import play.api.libs.crypto.CSRFTokenSigner
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.core.parsers.Multipart
import play.filters.cors.CORSFilter
import play.filters.csrf.CSRF._
import play.libs.typedmap.TypedKey
import play.mvc.Http.RequestBuilder

import scala.concurrent.Future

/**
 * An action that provides CSRF protection.
 *
 * @param config The CSRF configuration.
 * @param tokenSigner The CSRF token signer.
 * @param tokenProvider A token provider to use.
 * @param next The composed action that is being protected.
 * @param errorHandler handling failed token error.
 */
class CSRFAction(
    next: EssentialAction,
    config: CSRFConfig = CSRFConfig(),
    tokenSigner: CSRFTokenSigner,
    tokenProvider: TokenProvider,
    sessionConfiguration: SessionConfiguration,
    errorHandler: => ErrorHandler = CSRF.DefaultErrorHandler)(implicit mat: Materializer) extends EssentialAction {

  import play.core.Execution.Implicits.trampoline

  lazy val csrfActionHelper = new CSRFActionHelper(sessionConfiguration, config, tokenSigner, tokenProvider)

  private def checkFailed(req: RequestHeader, msg: String): Accumulator[ByteString, Result] =
    Accumulator.done(csrfActionHelper.clearTokenIfInvalid(req, errorHandler, msg))

  def apply(untaggedRequest: RequestHeader): Accumulator[ByteString, Result] = {
    val request = csrfActionHelper.tagRequestFromHeader(untaggedRequest)

    // this function exists purely to aid readability
    def continue = next(request)

    // Only filter unsafe methods and content types
    if (config.checkMethod(request.method) && config.checkContentType(request.contentType)) {

      if (!csrfActionHelper.requiresCsrfCheck(request)) {
        continue
      } else {

        // Only proceed with checks if there is an incoming token in the header, otherwise there's no point
        csrfActionHelper.getTokenToValidate(request).map { headerToken =>

          // First check if there's a token in the query string or header, if we find one, don't bother handling the body
          csrfActionHelper.getHeaderToken(request).map { queryStringToken =>

            if (tokenProvider.compareTokens(headerToken, queryStringToken)) {
              filterLogger.trace("[CSRF] Valid token found in query string")
              continue
            } else {
              filterLogger.warn("[CSRF] Check failed because invalid token found in query string: " +
                request.uri)(SecurityMarkerContext)
              checkFailed(request, "Bad CSRF token found in query String")
            }

          } getOrElse {

            // Check the body
            request.contentType match {
              case Some("application/x-www-form-urlencoded") =>
                filterLogger.trace(s"[CSRF] Check form body with url encoding")
                checkFormBody(request, next, headerToken, config.tokenName)
              case Some("multipart/form-data") =>
                filterLogger.trace(s"[CSRF] Check form body with multipart")
                checkMultipartBody(request, next, headerToken, config.tokenName)
              // No way to extract token from other content types
              case Some(content) =>
                filterLogger.warn(s"[CSRF] Check failed because $content for request " + request.uri)(SecurityMarkerContext)
                checkFailed(request, s"No CSRF token found for $content body")
              case None =>
                filterLogger.warn(s"[CSRF] Check failed because request without content type for " + request.uri)(SecurityMarkerContext)
                checkFailed(request, s"No CSRF token found for body without content type")
            }

          }
        } getOrElse {

          filterLogger.warn("[CSRF] Check failed because no token found in headers for " + request.uri)(SecurityMarkerContext)
          checkFailed(request, "No CSRF token found in headers")

        }
      }
    } else if (csrfActionHelper.getTokenToValidate(request).isEmpty && config.createIfNotFound(request)) {

      // No token in header and we have to create one if not found, so create a new token
      val requestWithNewToken = csrfActionHelper.tagRequestHeaderWithNewToken(request)

      // Once done, add it to the result
      next(requestWithNewToken).map(csrfActionHelper.addTokenToResponse(requestWithNewToken, _))

    } else {
      filterLogger.trace("[CSRF] No check necessary")
      next(request)
    }
  }

  private def checkFormBody: (RequestHeader, EssentialAction, String, String) => Accumulator[ByteString, Result] =
    checkBody(extractTokenFromFormBody)

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

      Flow[ByteString]
        .via(new BodyHandler(config, { body =>
          if (extractor(body, tokenName).fold(false)(tokenProvider.compareTokens(_, tokenFromHeader))) {
            filterLogger.trace("[CSRF] Valid token found in body")
            true
          } else {
            filterLogger.warn("[CSRF] Check failed because no or invalid token found in body for " + request.uri)(SecurityMarkerContext)
            false
          }
        }))
        .splitWhen(_ => false)
        .prefixAndTail(0) // TODO rewrite BodyHandler such that it emits sub-source then we can avoid all these dancing around
        .map(_._2)
        .concatSubstreams
        .toMat(Sink.head[Source[ByteString, _]])(Keep.right)
    ).mapFuture { validatedBodySource =>
        filterLogger.trace(s"[CSRF] running with validated body source")
        action(request).run(validatedBodySource)
      }.recoverWith {
        case NoTokenInBody =>
          filterLogger.warn("[CSRF] Check failed with NoTokenInBody for " + request.uri)(SecurityMarkerContext)
          csrfActionHelper.clearTokenIfInvalid(request, errorHandler, "No CSRF token found in body")
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
private class BodyHandler(config: CSRFConfig, checkBody: ByteString => Boolean) extends GraphStage[FlowShape[ByteString, ByteString]] {

  private val PostBodyBufferMax = config.postBodyBuffer

  val in: Inlet[ByteString] = Inlet("BodyHandler.in")
  val out: Outlet[ByteString] = Outlet("BodyHandler.out")

  override val shape = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with OutHandler with InHandler with StageLogging {

      var buffer: ByteString = ByteString.empty
      var next: ByteString = _

      def continueHandler = new InHandler with OutHandler {
        override def onPush(): Unit = push(out, grab(in))
        override def onPull(): Unit = {
          if (next ne null) {
            push(out, next)
            next = null
          } else {
            pull(in)
          }
        }

        override def onUpstreamFinish(): Unit = {
          if (next == null) completeStage()
        }
      }

      def onPush(): Unit = {
        val elem = grab(in)
        if (exceededBufferLimit(elem)) {
          // We've finished buffering up to the configured limit, try to validate
          buffer ++= elem
          if (checkBody(buffer)) {
            // Switch to continue, and push the buffer
            setHandlers(in, out, continueHandler)
            if (!(isClosed(in) || hasBeenPulled(in))) {
              val toPush = buffer
              buffer = null
              push(out, toPush)
            } else {
              next = buffer
              buffer = null
            }
          } else {
            // CSRF check failed
            failStage(NoTokenInBody)
          }
        } else {
          // Buffer
          buffer ++= elem
          pull(in)
        }
      }

      def onPull(): Unit = {
        if (!hasBeenPulled(in)) pull(in)
      }

      override def onUpstreamFinish(): Unit = {
        // CSRF check
        if (checkBody(buffer)) emit(out, buffer, () => completeStage())
        else failStage(NoTokenInBody)
      }

      private def exceededBufferLimit(elem: ByteString) = {
        buffer.size + elem.size > PostBodyBufferMax
      }

      setHandlers(in, out, this)
    }

}

private[csrf] object NoTokenInBody extends RuntimeException(null, null, false, false)

class CSRFActionHelper(
    sessionConfiguration: SessionConfiguration,
    csrfConfig: CSRFConfig,
    tokenSigner: CSRFTokenSigner,
    tokenProvider: TokenProvider
) {

  /**
   * Construct a new CSRFActionHelper and determine the TokenProvider from configuration.
   */
  def this(sessionConfiguration: SessionConfiguration, csrfConfig: CSRFConfig, tokenSigner: CSRFTokenSigner) = {
    this(sessionConfiguration, csrfConfig, tokenSigner, new TokenProviderProvider(csrfConfig, tokenSigner).get)
  }

  /**
   * @return true if the token is HTTP only, i.e. the token cannot be accessed from client-side JavaScript.
   */
  private def tokenIsHttpOnly: Boolean = {
    if (csrfConfig.cookieName.isDefined) csrfConfig.httpOnlyCookie else sessionConfiguration.httpOnly
  }

  /**
   * Get the header token, that is, the token that should be validated.
   */
  def getTokenToValidate(request: RequestHeader): Option[String] = {
    val attrToken = CSRF.getToken(request).map(_.value)
    val cookieOrSessionToken = csrfConfig.cookieName match {
      case Some(cookieName) => request.cookies.get(cookieName).map(_.value)
      case None => request.session.get(csrfConfig.tokenName)
    }
    cookieOrSessionToken orElse attrToken filter { token =>
      // return None if the token is invalid
      !csrfConfig.signTokens || tokenSigner.extractSignedToken(token).isDefined
    }
  }

  /**
   * Tag incoming requests with the token in the header
   */
  def tagRequestFromHeader(request: RequestHeader): RequestHeader = {
    getTokenToValidate(request).fold(request) { tokenValue =>
      val token = Token(csrfConfig.tokenName, tokenValue)
      val newReq = tagRequestHeader(request, token)
      if (csrfConfig.signTokens) {
        // Extract the signed token, and then resign it. This makes the token random per request, preventing the BREACH
        // vulnerability
        val extractedTokenValue = tokenSigner.extractSignedToken(token.value)
        extractedTokenValue.fold(newReq)(tv =>
          tagRequestHeader(newReq, token.copy(value = tokenSigner.signToken(tv)))
        )
      } else {
        newReq
      }
    }
  }

  def tagRequestFromHeader[A](request: Request[A]): Request[A] = {
    Request(tagRequestFromHeader(request: RequestHeader), request.body)
  }

  def tagRequestHeader(request: RequestHeader, token: => Token): RequestHeader = {
    request.addAttr(Token.InfoAttr, TokenInfo(token))
  }

  // This method is used only from Java
  def tagRequest[A](request: Request[A], token: Token): Request[A] = {
    request.addAttr(Token.InfoAttr, TokenInfo(token))
  }

  def tagRequestWithNewToken[A](request: Request[A]): Request[A] = {
    request.addAttr(Token.InfoAttr, TokenInfo(generateToken))
  }

  def tagRequestHeaderWithNewToken(request: RequestHeader): RequestHeader = {
    request.addAttr(Token.InfoAttr, TokenInfo(generateToken))
  }

  def tagRequestWithNewToken(requestBuilder: RequestBuilder): RequestBuilder = {
    requestBuilder.attr(new TypedKey(Token.InfoAttr), TokenInfo(generateToken))
  }

  // a newly generated token
  def generateToken: Token = Token(csrfConfig.tokenName, tokenProvider.generateToken)

  def getHeaderToken(request: RequestHeader): Option[String] = {
    val queryStringToken = request.getQueryString(csrfConfig.tokenName)
    val headerToken = request.headers.get(csrfConfig.headerName)

    queryStringToken orElse headerToken
  }

  def requiresCsrfCheck(request: RequestHeader): Boolean = {
    if (csrfConfig.bypassCorsTrustedOrigins && request.attrs.contains(CORSFilter.Attrs.Origin)) {
      filterLogger.trace("[CSRF] Bypassing check because CORSFilter request tag found")
      false
    } else {
      csrfConfig.shouldProtect(request)
    }
  }

  def addTokenToResponse(request: RequestHeader, result: Result): Result = {
    request.attrs.get(CSRF.Token.InfoAttr) match {
      case None =>
        filterLogger.warn("[CSRF] No token found on request!")
        result
      case Some(tokenInfo) if {
        tokenIsHttpOnly && // the token is not going to be accessed and used from JS
          result.body.isInstanceOf[HttpEntity.Strict] && // the body was fully rendered
          !tokenInfo.wasRendered // the token was not rendered in the body of the response
      } =>
        filterLogger.trace("[CSRF] Not emitting CSRF token because token was never rendered")
        result
      case _ if isCached(result) =>
        filterLogger.trace("[CSRF] Not adding token to cached response")
        result
      case Some(tokenInfo) =>
        val Token(tokenName, tokenValue) = tokenInfo.toToken
        filterLogger.trace("[CSRF] Adding token to result: " + result)
        csrfConfig.cookieName.map { name =>
          result.withCookies(Cookie(
            name, tokenValue,
            path = sessionConfiguration.path, domain = sessionConfiguration.domain,
            secure = csrfConfig.secureCookie, httpOnly = csrfConfig.httpOnlyCookie, sameSite = csrfConfig.sameSiteCookie))
        } getOrElse {
          val newSession = result.session(request) + (tokenName -> tokenValue)
          result.withSession(newSession)
        }
    }
  }

  def isCached(result: Result): Boolean =
    result.header.headers.get(CACHE_CONTROL).fold(false)(!_.contains("no-cache"))

  def clearTokenIfInvalid(request: RequestHeader, errorHandler: ErrorHandler, msg: String): Future[Result] = {
    import play.core.Execution.Implicits.trampoline

    errorHandler.handle(request, msg) map { result =>
      CSRF.getToken(request).fold(
        csrfConfig.cookieName.flatMap { cookie =>
          request.cookies.get(cookie).map { token =>
            result.discardingCookies(
              DiscardingCookie(cookie, domain = sessionConfiguration.domain, path = sessionConfiguration.path, secure = csrfConfig.secureCookie))
          }
        }.getOrElse {
          result.withSession(result.session(request) - csrfConfig.tokenName)
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
case class CSRFCheck @Inject() (config: CSRFConfig, tokenSigner: CSRFTokenSigner, sessionConfiguration: SessionConfiguration) {

  private class CSRFCheckAction[A](
      tokenProvider: TokenProvider,
      errorHandler: ErrorHandler,
      wrapped: Action[A],
      csrfActionHelper: CSRFActionHelper
  ) extends Action[A] {
    def parser = wrapped.parser
    def executionContext = wrapped.executionContext
    def apply(untaggedRequest: Request[A]) = {
      val request = csrfActionHelper.tagRequestFromHeader(untaggedRequest)

      // Maybe bypass
      if (!csrfActionHelper.requiresCsrfCheck(request) || !config.checkContentType(request.contentType)) {
        wrapped(request)
      } else {
        // Get token from header
        csrfActionHelper.getTokenToValidate(request).flatMap { headerToken =>
          // Get token from query string
          csrfActionHelper.getHeaderToken(request)
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
          filterLogger.warn("CSRF token check failed")(SecurityMarkerContext)
          csrfActionHelper.clearTokenIfInvalid(request, errorHandler, "CSRF token check failed")
        }
      }
    }
  }

  /**
   * Wrap an action in a CSRF check.
   */
  def apply[A](action: Action[A], errorHandler: ErrorHandler): Action[A] =
    new CSRFCheckAction(new TokenProviderProvider(config, tokenSigner).get, errorHandler, action, new CSRFActionHelper(sessionConfiguration, config, tokenSigner))

  /**
   * Wrap an action in a CSRF check.
   */
  def apply[A](action: Action[A]): Action[A] =
    new CSRFCheckAction(new TokenProviderProvider(config, tokenSigner).get, CSRF.DefaultErrorHandler, action, new CSRFActionHelper(sessionConfiguration, config, tokenSigner))
}

/**
 * CSRF add token action.
 *
 * Apply this to all actions that render a form that contains a CSRF token.
 */
case class CSRFAddToken @Inject() (config: CSRFConfig, crypto: CSRFTokenSigner, sessionConfiguration: SessionConfiguration) {

  private class CSRFAddTokenAction[A](
      config: CSRFConfig,
      wrapped: Action[A],
      csrfActionHelper: CSRFActionHelper
  ) extends Action[A] {
    def parser = wrapped.parser
    def executionContext = wrapped.executionContext
    def apply(untaggedRequest: Request[A]) = {
      val request = csrfActionHelper.tagRequestFromHeader(untaggedRequest)

      if (csrfActionHelper.getTokenToValidate(request).isEmpty) {

        // No token in header, so add a new token
        val requestWithNewToken = csrfActionHelper.tagRequestWithNewToken(request)

        // Once done, add it to the result
        import play.core.Execution.Implicits.trampoline
        wrapped(requestWithNewToken).map(csrfActionHelper.addTokenToResponse(requestWithNewToken, _))
      } else {
        wrapped(request)
      }
    }
  }

  /**
   * Wrap an action in an action that ensures there is a CSRF token.
   */
  def apply[A](action: Action[A]): Action[A] =
    new CSRFAddTokenAction(config, action, new CSRFActionHelper(sessionConfiguration, config, crypto))
}
