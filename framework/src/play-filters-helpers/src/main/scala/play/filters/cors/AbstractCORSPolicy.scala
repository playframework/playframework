/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.cors

import java.util.Locale

import scala.collection.immutable
import scala.concurrent.Future
import java.net.{ URI, URISyntaxException }

import akka.util.ByteString
import play.api.LoggerLike
import play.api.MarkerContexts.SecurityMarkerContext
import play.api.http.{ HeaderNames, HttpErrorHandler, HttpVerbs }
import play.api.libs.streams.Accumulator
import play.api.mvc._

/**
 * An abstraction for providing [[play.api.mvc.Action]]s and [[play.api.mvc.Filter]]s that support Cross-Origin
 * Resource Sharing (CORS)
 *
 * @see [[http://www.w3.org/TR/cors/ CORS specification]]
 */
private[cors] trait AbstractCORSPolicy {

  protected val logger: LoggerLike

  protected def corsConfig: CORSConfig

  protected def errorHandler: HttpErrorHandler

  /**
   * HTTP Methods supported by Play
   */
  private val SupportedHttpMethods: Set[String] = {
    import HttpVerbs._
    immutable.HashSet(GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS)
  }

  protected def filterRequest(next: EssentialAction, request: RequestHeader): Accumulator[ByteString, Result] = {
    val resultAcc = (request.headers.get(HeaderNames.ORIGIN), request.method) match {
      case (None, _) =>
        /* http://www.w3.org/TR/cors/#resource-requests
         * § 6.1.1
         * If the Origin header is not present terminate this set of steps.
         */
        next(request)
      case (Some(originHeader), _) if originHeader.isEmpty || !isValidOrigin(originHeader) =>
        /*
         * If the value of the Origin header is not a case-sensitive match for any of the values in list of origins, do
         * not set any additional headers and terminate this set of steps.
         */
        if (corsConfig.serveForbiddenOrigins)
          next(request)
        else
          handleInvalidCORSRequest(request)
      case (Some(originHeader), _) if isSameOrigin(originHeader, request) =>
        // Same-origin request
        next(request)
      case (_, HttpVerbs.OPTIONS) =>
        // Check for preflight request
        request.headers.get(HeaderNames.ACCESS_CONTROL_REQUEST_METHOD) match {
          case None =>
            handleCORSRequest(next, request)
          case Some("") =>
            handleInvalidCORSRequest(request)
          case _ =>
            handlePreFlightCORSRequest(request)
        }
      case (_, method) if SupportedHttpMethods.contains(method) =>
        handleCORSRequest(next, request)
      case _ =>
        // unrecognized method so invalid request
        handleInvalidCORSRequest(request)
    }

    /* http://www.w3.org/TR/cors/#resource-implementation
     * § 6.4
     * Resources that wish to enable themselves to be shared with multiple Origins but do
     * not respond uniformly with "*" must in practice generate the Access-Control-Allow-Origin
     * header dynamically in response to every request they wish to allow. As a consequence,
     * authors of such resources should send a Vary: Origin HTTP header or provide other
     * appropriate control directives to prevent caching of such responses, which may be
     * inaccurate if re-used across-origins.
     */
    resultAcc.map { result =>
      result.withHeaders(result.header.varyWith(HeaderNames.ORIGIN))
    }(play.core.Execution.trampoline)
  }

  /* Handles a CORS request
   *
   * @see [[http://www.w3.org/TR/cors/#resource-requests Simple Cross-Origin Request, Actual Request, and Redirects]]
   */
  private def handleCORSRequest(next: EssentialAction, request: RequestHeader): Accumulator[ByteString, Result] = {
    val origin: String = {
      val originOpt = request.headers.get(HeaderNames.ORIGIN)
      assume(originOpt.isDefined, "The presence of the ORIGIN header should guaranteed at this point.")
      originOpt.get
    }

    /* http://www.w3.org/TR/cors/#resource-requests
     * § 6.1.2
     * If the value of the Origin header is not a case-sensitive match for
     * any of the values in list of origins, do not set any additional
     * headers and terminate this set of steps.
     */
    if (!corsConfig.allowedOrigins(origin)) {
      if (corsConfig.serveForbiddenOrigins)
        next(request)
      else
        handleInvalidCORSRequest(request)
    } else {
      import play.core.Execution.Implicits.trampoline

      val taggedRequest = request
        .addAttr(CORSFilter.Attrs.Origin, origin)

      // We must recover any errors so that we can add the headers to them to allow clients to see the result
      val result = try {
        next(taggedRequest).recoverWith {
          case e: Throwable =>
            errorHandler.onServerError(taggedRequest, e)
        }
      } catch {
        case e: Throwable =>
          Accumulator.done(errorHandler.onServerError(taggedRequest, e))
      }
      result.map(addCorsHeaders(_, origin))
    }
  }

  private def addCorsHeaders(result: Result, origin: String): Result = {
    import HeaderNames._

    val headerBuilder = Seq.newBuilder[(String, String)]

    /* http://www.w3.org/TR/cors/#resource-requests
     * § 6.1.3
     */
    if (corsConfig.supportsCredentials) {
      /* If the resource supports credentials add a single Access-Control-Allow-Origin header,
       * with the value of the Origin header as value, and add a single
       * Access-Control-Allow-Credentials header with the case-sensitive string "true" as value.
       */
      headerBuilder += ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"
      headerBuilder += ACCESS_CONTROL_ALLOW_ORIGIN -> origin
    } else {
      /* Otherwise, add a single Access-Control-Allow-Origin header,
       * with either the value of the Origin header or the string "*" as value.
       */
      if (corsConfig.anyOriginAllowed) {
        headerBuilder += ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
      } else {
        headerBuilder += ACCESS_CONTROL_ALLOW_ORIGIN -> origin
      }
    }

    /* http://www.w3.org/TR/cors/#resource-requests
     * § 6.1.4
     * If the list of exposed headers is not empty add one or more Access-Control-Expose-Headers headers,
     * with as values the header field names given in the list of exposed headers.
     */
    if (corsConfig.exposedHeaders.nonEmpty) {
      headerBuilder += ACCESS_CONTROL_EXPOSE_HEADERS -> corsConfig.exposedHeaders.mkString(",")
    }

    result.withHeaders(headerBuilder.result(): _*)
  }

  private def handlePreFlightCORSRequest(request: RequestHeader): Accumulator[ByteString, Result] = {
    import HeaderNames._

    val origin = {
      val originOpt = request.headers.get(ORIGIN)
      assume(originOpt.isDefined, "The presence of the ORIGIN header should guaranteed at this point.")
      originOpt.get
    }

    /* http://www.w3.org/TR/cors/#resource-preflight-requests
     * § 6.2.2
     * If the value of the Origin header is not a case-sensitive match for
     * any of the values in list of origins, do not set any additional
     * headers and terminate this set of steps.
     */
    if (!corsConfig.allowedOrigins(origin)) {
      handleInvalidCORSRequest(request)
    } else {
      request.headers.get(ACCESS_CONTROL_REQUEST_METHOD) match {
        case None =>
          /* http://www.w3.org/TR/cors/#resource-preflight-requests
           * § 6.2.3
           * If there is no Access-Control-Request-Method header or if
           * parsing failed, do not set any additional headers and
           * terminate this set of steps.
           */
          handleInvalidCORSRequest(request)
        case Some(requestMethod) =>
          val accessControlRequestMethod = requestMethod.trim
          val methodPredicate = corsConfig.isHttpMethodAllowed // call def to get function val
          /* http://www.w3.org/TR/cors/#resource-preflight-requests
           * § 6.2.5
           * If method is not a case-sensitive match for any of the
           * values in list of methods do not set any additional
           * headers and terminate this set of steps.
           */
          if (!SupportedHttpMethods.contains(accessControlRequestMethod) ||
            !methodPredicate(accessControlRequestMethod)) {
            handleInvalidCORSRequest(request)
          } else {
            /* http://www.w3.org/TR/cors/#resource-preflight-requests
             * § 6.2.4
             * Let header field-names be the values as result of parsing
             * the Access-Control-Request-Headers headers.
             * If there are no Access-Control-Request-Headers headers
             * let header field-names be the empty list.
             */
            val accessControlRequestHeaders: List[String] = {
              request.headers.get(ACCESS_CONTROL_REQUEST_HEADERS) match {
                case None => List.empty[String]
                case Some(headerVal) =>
                  headerVal.trim.split(',').map(_.trim.toLowerCase(java.util.Locale.ENGLISH))(collection.breakOut)
              }
            }

            val headerPredicate = corsConfig.isHttpHeaderAllowed // call def to get function val
            /* http://www.w3.org/TR/cors/#resource-preflight-requests
             * § 6.2.6
             * If any of the header field-names is not a ASCII case-insensitive
             * match for any of the values in list of headers do not
             * set any additional headers and terminate this set of steps.
             */
            if (!accessControlRequestHeaders.forall(headerPredicate(_))) {
              handleInvalidCORSRequest(request)
            } else {
              val headerBuilder = Seq.newBuilder[(String, String)]

              /* http://www.w3.org/TR/cors/#resource-preflight-requests
               * § 6.2.7
               */
              if (corsConfig.supportsCredentials) {
                /* If the resource supports credentials add a single Access-Control-Allow-Origin header,
                 * with the value of the Origin header as value, and add a single
                 * Access-Control-Allow-Credentials header with the case-sensitive string "true" as value.
                 */
                headerBuilder += ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"
                headerBuilder += ACCESS_CONTROL_ALLOW_ORIGIN -> origin
              } else {
                /* Otherwise, add a single Access-Control-Allow-Origin header,
                 * with either the value of the Origin header or the string "*" as value.
                 */
                if (corsConfig.anyOriginAllowed) {
                  headerBuilder += ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
                } else {
                  headerBuilder += ACCESS_CONTROL_ALLOW_ORIGIN -> origin
                }
              }

              /* http://www.w3.org/TR/cors/#resource-preflight-requests
               * § 6.2.8
               * Optionally add a single Access-Control-Max-Age header with as value the amount
               * of seconds the user agent is allowed to cache the result of the request.
               */
              if (corsConfig.preflightMaxAge.toSeconds > 0) {
                headerBuilder += ACCESS_CONTROL_MAX_AGE -> corsConfig.preflightMaxAge.toSeconds.toString
              }

              /* http://www.w3.org/TR/cors/#resource-preflight-requests
               * § 6.2.9
               * Add one or more Access-Control-Allow-Methods headers consisting of (a subset of)
               * the list of methods.
               * Note: If a method is a simple method it does not need to be listed,
               * but this is not prohibited.
               * Note: Since the list of methods can be unbounded, simply returning the method
               * indicated by Access-Control-Request-Method (if supported) can be enough.
               */
              headerBuilder += ACCESS_CONTROL_ALLOW_METHODS -> accessControlRequestMethod

              /* http://www.w3.org/TR/cors/#resource-preflight-requests
               * § 6.2.9
               * If each of the header field-names is a simple header and none is Content-Type,
               * this step may be skipped.
               * Add one or more Access-Control-Allow-Headers headers consisting of
               * (a subset of) the list of headers.
               * Note: If a header field name is a simple header and is not Content-Type,
               * it is not required to be listed. Content-Type is to be listed as only a subset
               * of its values makes it qualify as simple header.
               * Note: Since the list of headers can be unbounded, simply returning supported
               * headers from Access-Control-Allow-Headers can be enough.
               */
              if (accessControlRequestHeaders.nonEmpty) {
                headerBuilder += ACCESS_CONTROL_ALLOW_HEADERS -> accessControlRequestHeaders.mkString(",")
              }

              Accumulator.done(Results.Ok.withHeaders(headerBuilder.result(): _*))
            }
          }
      }
    }
  }

  private def handleInvalidCORSRequest(request: RequestHeader): Accumulator[ByteString, Result] = {
    logger.warn(s"""Invalid CORS request;Origin=${request.headers.get(HeaderNames.ORIGIN)};Method=${request.method};${HeaderNames.ACCESS_CONTROL_REQUEST_HEADERS}=${request.headers.get(HeaderNames.ACCESS_CONTROL_REQUEST_HEADERS)}""")(SecurityMarkerContext)
    Accumulator.done(Future.successful(Results.Forbidden))
  }

  // http://tools.ietf.org/html/rfc6454#section-7.1
  private def isValidOrigin(origin: String): Boolean = {
    // Checks for encoded characters. Helps prevent CRLF injection.
    if (origin.contains("%")) {
      false
    } else {
      try {
        new URI(origin).getScheme ne null
      } catch {
        case _: URISyntaxException => false
      }
    }
  }

  private def isSameOrigin(origin: String, request: RequestHeader): Boolean = {
    val hostUri = new URI(origin.toLowerCase(Locale.ENGLISH))
    val originUri = new URI((if (request.secure) "https://" else "http://") + request.host.toLowerCase(Locale.ENGLISH))
    (hostUri.getScheme, hostUri.getHost, hostUri.getPort) == (originUri.getScheme, originUri.getHost, originUri.getPort)
  }
}
