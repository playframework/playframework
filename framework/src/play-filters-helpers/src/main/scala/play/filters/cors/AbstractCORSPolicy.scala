/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.cors

import scala.collection.immutable
import scala.concurrent.Future

import java.net.{ URI, URISyntaxException }

import play.api.LoggerLike
import play.api.http.{ HeaderNames, HttpVerbs }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{ RequestHeader, Results, Result }

/**
 * An abstraction for providing [[play.api.mvc.Action]]s and [[play.api.mvc.Filter]]s that support Cross-Origin
 * Resource Sharing (CORS)
 *
 * @see [[http://www.w3.org/TR/cors/ CORS specification]]
 */
trait AbstractCORSPolicy {

  protected val logger: LoggerLike

  protected def corsConfig: CORSConfig

  /**
   * HTTP Methods support by Play
   */
  private val HttpMethods = {
    import HttpVerbs._
    immutable.HashSet(GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS)
  }

  protected def filterRequest(f: () => Future[Result], request: RequestHeader): Future[Result] = {
    request.headers.get(HeaderNames.ORIGIN) match {
      case None =>
        /* http://www.w3.org/TR/cors/#resource-requests
         * § 6.1.1
         * If the Origin header is not present terminate this set of steps.
         */
        f()
      case Some(originHeader) =>
        if (originHeader.isEmpty || !isValidOrigin(originHeader)) {
          handleInvalidCORSRequest(request)
        } else if ({
          val originUri = new URI(originHeader)
          val hostUri = new URI("//" + request.host)
          originUri.getHost == hostUri.getHost && originUri.getPort == hostUri.getPort
        }) {
          // HOST and ORIGIN match, so this is a same-origin request, pass through.
          f()
        } else {
          val method = request.method
          if (HttpMethods.contains(method)) {
            if (method == HttpVerbs.OPTIONS) {
              request.headers.get(HeaderNames.ACCESS_CONTROL_REQUEST_METHOD) match {
                case None =>
                  handleCORSRequest(f, request)
                case Some(requestMethod) =>
                  if (requestMethod.isEmpty) {
                    handleInvalidCORSRequest(request)
                  } else {
                    handlePreFlightCORSRequest(request)
                  }
              }
            } else {
              handleCORSRequest(f, request)
            }
          } else {
            // unrecognized method so invalid request
            handleInvalidCORSRequest(request)
          }
        }
    }
  }

  /* Handles a CORS request
   *
   * @see [[http://www.w3.org/TR/cors/#resource-requests Simple Cross-Origin Request, Actual Request, and Redirects]]
   */
  private def handleCORSRequest(f: () => Future[Result], request: RequestHeader): Future[Result] = {
    val origin = {
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
    if (!isOriginAllowed(origin)) {
      handleInvalidCORSRequest(request)
    } else {
      val headerBuilder = Seq.newBuilder[(String, String)]

      /* http://www.w3.org/TR/cors/#resource-requests
       * § 6.1.3
       */
      if (corsConfig.supportsCredentials) {
        /* If the resource supports credentials add a single Access-Control-Allow-Origin header,
         * with the value of the Origin header as value, and add a single
         * Access-Control-Allow-Credentials header with the case-sensitive string "true" as value.
         */
        headerBuilder += HeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"
        headerBuilder += HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> origin
        /* http://www.w3.org/TR/cors/#resource-implementation
         * § 6.4
         * Resources that wish to enable themselves to be shared with multiple Origins but do
         * not respond uniformly with "*" must in practice generate the Access-Control-Allow-Origin
         * header dynamically in response to every request they wish to allow. As a consequence,
         * authors of such resources should send a Vary: Origin HTTP header or provide other
         * appropriate control directives to prevent caching of such responses, which may be
         * inaccurate if re-used across-origins.
         */
        headerBuilder += HeaderNames.VARY -> HeaderNames.ORIGIN
      } else {
        /* Otherwise, add a single Access-Control-Allow-Origin header,
         * with either the value of the Origin header or the string "*" as value.
         */
        if (corsConfig.anyOriginAllowed) {
          headerBuilder += HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
        } else {
          headerBuilder += HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> origin
          /* http://www.w3.org/TR/cors/#resource-implementation
           * § 6.4
           */
          headerBuilder += HeaderNames.VARY -> HeaderNames.ORIGIN
        }
      }

      /* http://www.w3.org/TR/cors/#resource-requests
       * § 6.1.4
       * If the list of exposed headers is not empty add one or more Access-Control-Expose-Headers headers,
       * with as values the header field names given in the list of exposed headers.
       */
      if (corsConfig.exposedHeaders.nonEmpty) {
        headerBuilder += HeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS -> corsConfig.exposedHeaders.mkString(",")
      }

      f().map(_.withHeaders(headerBuilder.result(): _*))
    }
  }

  private def handlePreFlightCORSRequest(request: RequestHeader): Future[Result] = {
    val origin = {
      val originOpt = request.headers.get(HeaderNames.ORIGIN)
      assume(originOpt.isDefined, "The presence of the ORIGIN header should guaranteed at this point.")
      originOpt.get
    }

    /* http://www.w3.org/TR/cors/#resource-preflight-requests
     * § 6.2.2
     * If the value of the Origin header is not a case-sensitive match for
     * any of the values in list of origins, do not set any additional
     * headers and terminate this set of steps.
     */
    if (!isOriginAllowed(origin)) {
      handleInvalidCORSRequest(request)
    } else {
      request.headers.get(HeaderNames.ACCESS_CONTROL_REQUEST_METHOD) match {
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
          if (!HttpMethods.contains(accessControlRequestMethod) ||
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
              request.headers.get(HeaderNames.ACCESS_CONTROL_REQUEST_HEADERS) match {
                case None => List.empty[String]
                case Some(headerVal) =>
                  headerVal.trim.split(',').map(_.trim.toLowerCase)(collection.breakOut)
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
                headerBuilder += HeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"
                headerBuilder += HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> origin

                /* http://www.w3.org/TR/cors/#resource-implementation
                 * § 6.4
                 * Resources that wish to enable themselves to be shared with multiple Origins but do
                 * not respond uniformly with "*" must in practice generate the Access-Control-Allow-Origin
                 * header dynamically in response to every request they wish to allow. As a consequence,
                 * authors of such resources should send a Vary: Origin HTTP header or provide other
                 * appropriate control directives to prevent caching of such responses, which may be
                 * inaccurate if re-used across-origins.
                 */
                headerBuilder += HeaderNames.VARY -> HeaderNames.ORIGIN
              } else {
                /* Otherwise, add a single Access-Control-Allow-Origin header,
                 * with either the value of the Origin header or the string "*" as value.
                 */
                if (corsConfig.anyOriginAllowed) {
                  headerBuilder += HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
                } else {
                  headerBuilder += HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> origin
                  /* http://www.w3.org/TR/cors/#resource-implementation
                   * § 6.4
                   */
                  headerBuilder += HeaderNames.VARY -> HeaderNames.ORIGIN
                }
              }

              /* http://www.w3.org/TR/cors/#resource-preflight-requests
               * § 6.2.8
               * Optionally add a single Access-Control-Max-Age header with as value the amount
               * of seconds the user agent is allowed to cache the result of the request.
               */
              if (corsConfig.preflightMaxAge.toSeconds > 0) {
                headerBuilder += HeaderNames.ACCESS_CONTROL_MAX_AGE -> corsConfig.preflightMaxAge.toSeconds.toString
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
              headerBuilder += HeaderNames.ACCESS_CONTROL_ALLOW_METHODS -> accessControlRequestMethod

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
              if (!accessControlRequestHeaders.isEmpty) {
                headerBuilder += HeaderNames.ACCESS_CONTROL_ALLOW_HEADERS -> accessControlRequestHeaders.mkString(",")
              }

              Future.successful {
                Results.Ok.withHeaders(headerBuilder.result(): _*)
              }
            }
          }
      }
    }
  }

  private def handleInvalidCORSRequest(request: RequestHeader): Future[Result] = {
    logger.trace(s"""Invalid CORS request;Origin=${request.headers.get(HeaderNames.ORIGIN)};Method=${request.method};${HeaderNames.ACCESS_CONTROL_REQUEST_HEADERS}=${request.headers.get(HeaderNames.ACCESS_CONTROL_REQUEST_HEADERS)}""")
    Future.successful(Results.Forbidden)
  }

  private def isOriginAllowed(origin: String): Boolean = {
    corsConfig.anyOriginAllowed || corsConfig.allowedOrigins.contains(origin)
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
}
