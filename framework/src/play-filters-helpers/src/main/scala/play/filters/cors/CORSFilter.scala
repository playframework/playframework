/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.cors

import scala.collection
import scala.collection.immutable
import scala.concurrent.Future

import java.net.{ URI, URISyntaxException }

import play.api.{ Configuration, Logger, Play }
import play.api.http.{ HeaderNames, HttpVerbs, MimeTypes }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{ Filter, RequestHeader, Results, Result }

/**
 * A filter that implements Cross-Origin Resource Sharing (CORS)
 *
 * It can be configured to...
 *
 *  - filter paths by a whitelist of path prefixes
 *  - allow only requests with origins from a whitelist (by default all origins are allowed)
 *  - allow only HTTP methods from a whitelist for preflight requests (by default all methods are allowed)
 *  - allow only HTTP headers from a whitelist for preflight requests (by default all methods are allowed)
 *  - set custom HTTP headers to be exposed in the response (by default no headers are exposed)
 *  - disable/enable support for credentials (by default credentials support is enabled)
 *  - set how long (in seconds) the results of a preflight request can be cached in a preflight result cache (by default 3600 seconds, 1 hour)
 *
 * @example The configuration is as follows:
 * {{{
 * cors {
 *     path.prefixes = ["/myresource", ...]  # If left undefined, all paths are filtered
 *     allowed {
 *         origins = ["http://...", ...]  # If left undefined, all origins are allowed
 *         http {
 *             methods = ["PATCH", ...]  # If left undefined, all methods are allowed
 *             headers = ["Custom-Header", ...]  # If left undefined, all headers are allowed
 *         }
 *     }
 *     exposed.headers = [...]  # empty by default
 *     supports.credentials = true  # true by default
 *     preflight.maxage = 3600  # 3600 by default
 * }
 *
 * }}}
 *
 * @see [[http://www.w3.org/TR/cors/ CORS specification]]
 */
object CORSFilter extends Filter {

  private val filterLogger = Logger("play.filters")

  private def conf: Configuration = Play.maybeApplication.map(_.configuration).getOrElse(Configuration.empty)

  private def pathPrefixes: Seq[String] =
    conf.getStringSeq("cors.path.prefixes").getOrElse(Seq("/"))

  /* http://www.w3.org/TR/cors/#resource-requests
   * §6.1.2
   * http://www.w3.org/TR/cors/#resource-preflight-requests
   * §6.2.2
   * Always matching is acceptable since the list of origins can be unbounded.
   */
  private def anyOriginAllowed: Boolean =
    conf.getStringSeq("cors.allowed.origins").isEmpty

  private def allowedOrigins: Set[String] =
    conf.getStringSeq("cors.allowed.origins").map(_.toSet).getOrElse(Set.empty)

  /* http://www.w3.org/TR/cors/#resource-preflight-requests
   * §6.2.5
   * Always matching is acceptable since the list of methods can be unbounded.
   */
  private def isHttpMethodAllowed: String => Boolean =
    conf.getStringSeq("cors.allowed.http.methods").map { methods =>
      val s = methods.toSet
      s.contains _
    }.getOrElse(_ => true)

  /* http://www.w3.org/TR/cors/#resource-preflight-requests
   * §6.2.6
   * Always matching is acceptable since the list of headers can be unbounded.
   */
  private def isHttpHeaderAllowed: String => Boolean =
    conf.getStringSeq("cors.allowed.http.headers").map { headers =>
      val s = headers.map(_.toLowerCase).toSet
      s.contains _
    }.getOrElse(_ => true)

  private def exposedHeaders: Seq[String] =
    conf.getStringSeq("cors.exposed.headers").getOrElse(Seq.empty)

  private def supportsCredentials: Boolean =
    conf.getBoolean("cors.support.credentials").getOrElse(true)

  private def preflightMaxAge: Int =
    conf.getInt("cors.preflight.maxage").getOrElse(3600)

  /**
   * HTTP Methods support by Play
   */
  private val HttpMethods = {
    import HttpVerbs._
    immutable.HashSet(GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS)
  }

  override def apply(f: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {
    val path = request.path
    if (pathPrefixes.exists(path startsWith _)) {
      request.headers.get(HeaderNames.ORIGIN) match {
        case None =>
          /* http://www.w3.org/TR/cors/#resource-requests
           * § 6.1.1
           * If the Origin header is not present terminate this set of steps.
           */
          f(request)
        case Some(originHeader) =>
          if (originHeader.isEmpty || !isValidOrigin(originHeader)) {
            handleInvalidCORSRequest(f, request)
          } else {
            val method = request.method
            if (HttpMethods.contains(method)) {
              if (method == HttpVerbs.OPTIONS) {
                request.headers.get(HeaderNames.ACCESS_CONTROL_REQUEST_METHOD) match {
                  case None =>
                    handleCORSRequest(f, request)
                  case Some(requestMethod) =>
                    if (requestMethod.isEmpty) {
                      handleInvalidCORSRequest(f, request)
                    } else {
                      handlePreFlightCORSRequest(f, request)
                    }
                }
              } else {
                handleCORSRequest(f, request)
              }
            } else {
              // unrecognized method so invalid request
              handleInvalidCORSRequest(f, request)
            }
          }
      }
    } else {
      f(request)
    }
  }

  /**
   * Handles a CORS request
   *
   * @see <a href="http://www.w3.org/TR/cors/#resource-requests">Simple
   *      Cross-Origin Request, Actual Request, and Redirects</a>
   */
  private def handleCORSRequest(f: RequestHeader => Future[Result], request: RequestHeader): Future[Result] = {
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
      handleInvalidCORSRequest(f, request)
    } else {
      val headerBuilder = Seq.newBuilder[(String, String)]

      /* http://www.w3.org/TR/cors/#resource-requests
       * § 6.1.3
       */
      if (supportsCredentials) {
        /* If the resource supports credentials add a single Access-Control-Allow-Origin header,
         * with the value of the Origin header as value, and add a single
         * Access-Control-Allow-Credentials header with the case-sensitive string "true" as value.
         */
        headerBuilder += HeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"
        headerBuilder += HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> origin
      } else {
        /* Otherwise, add a single Access-Control-Allow-Origin header,
         * with either the value of the Origin header or the string "*" as value.
         */
        if (anyOriginAllowed) {
          headerBuilder += HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
        } else {
          headerBuilder += HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> origin
        }
      }

      /* http://www.w3.org/TR/cors/#resource-requests
       * § 6.1.4
       * If the list of exposed headers is not empty add one or more Access-Control-Expose-Headers headers,
       * with as values the header field names given in the list of exposed headers.
       */
      if (exposedHeaders.nonEmpty) {
        headerBuilder += HeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS -> exposedHeaders.mkString(",")
      }

      f(request).map(_.withHeaders(headerBuilder.result(): _*))
    }
  }

  private def handlePreFlightCORSRequest(f: RequestHeader => Future[Result], request: RequestHeader): Future[Result] = {
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
      handleInvalidCORSRequest(f, request)
    } else {
      request.headers.get(HeaderNames.ACCESS_CONTROL_REQUEST_METHOD) match {
        case None =>
          /* http://www.w3.org/TR/cors/#resource-preflight-requests
           * § 6.2.3
           * If there is no Access-Control-Request-Method header or if
           * parsing failed, do not set any additional headers and
           * terminate this set of steps.
           */
          handleInvalidCORSRequest(f, request)
        case Some(requestMethod) =>
          val accessControlRequestMethod = requestMethod.trim
          val methodPredicate = isHttpMethodAllowed // call def to get function val
          /* http://www.w3.org/TR/cors/#resource-preflight-requests
           * § 6.2.5
           * If method is not a case-sensitive match for any of the
           * values in list of methods do not set any additional
           * headers and terminate this set of steps.
           */
          if (!HttpMethods.contains(accessControlRequestMethod) ||
            !methodPredicate(accessControlRequestMethod)) {
            handleInvalidCORSRequest(f, request)
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

            val headerPredicate = isHttpHeaderAllowed // call def to get function val
            /* http://www.w3.org/TR/cors/#resource-preflight-requests
             * § 6.2.6
             * If any of the header field-names is not a ASCII case-insensitive
             * match for any of the values in list of headers do not
             * set any additional headers and terminate this set of steps.
             */
            if (!accessControlRequestHeaders.forall(headerPredicate(_))) {
              handleInvalidCORSRequest(f, request)
            } else {
              val headerBuilder = Seq.newBuilder[(String, String)]

              /* http://www.w3.org/TR/cors/#resource-preflight-requests
               * § 6.2.7
               */
              if (supportsCredentials) {
                /* If the resource supports credentials add a single Access-Control-Allow-Origin header,
                 * with the value of the Origin header as value, and add a single
                 * Access-Control-Allow-Credentials header with the case-sensitive string "true" as value.
                 */
                headerBuilder += HeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"
                headerBuilder += HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> origin
              } else {
                /* Otherwise, add a single Access-Control-Allow-Origin header,
                 * with either the value of the Origin header or the string "*" as value.
                 */
                if (anyOriginAllowed) {
                  headerBuilder += HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*"
                } else {
                  headerBuilder += HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> origin
                }
              }

              /* http://www.w3.org/TR/cors/#resource-preflight-requests
               * § 6.2.8
               * Optionally add a single Access-Control-Max-Age header with as value the amount
               * of seconds the user agent is allowed to cache the result of the request.
               */
              if (preflightMaxAge > 0) {
                headerBuilder += HeaderNames.ACCESS_CONTROL_MAX_AGE -> preflightMaxAge.toString
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

  private def handleInvalidCORSRequest(f: RequestHeader => Future[Result], request: RequestHeader): Future[Result] = {
    filterLogger.trace(s"""Invalid CORS request;Origin=${request.headers.get(HeaderNames.ORIGIN)};Method=${request.method};${HeaderNames.ACCESS_CONTROL_REQUEST_HEADERS}=${request.headers.get(HeaderNames.ACCESS_CONTROL_REQUEST_HEADERS)}""")
    Future.successful(Results.Forbidden)
  }

  private def isOriginAllowed(origin: String): Boolean = {
    anyOriginAllowed || allowedOrigins.contains(origin)
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
