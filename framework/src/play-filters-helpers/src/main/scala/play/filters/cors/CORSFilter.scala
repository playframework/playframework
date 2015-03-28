/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.cors

import scala.concurrent.Future

import play.api.Logger
import play.api.mvc.{ Filter, RequestHeader, Result }

/**
 * A [[Filter]] that implements Cross-Origin Resource Sharing (CORS)
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
 * @param  corsConfig  configuration of the CORS policy
 * @param  pathPrefixes  whitelist of path prefixes to restrict the filter
 *
 * @see [[CORSConfig]]
 * @see [[AbstractCORSPolicy]]
 * @see [[CORSActionBuilder]]
 * @see [[http://www.w3.org/TR/cors/ CORS specification]]
 */
class CORSFilter(
    override protected val corsConfig: CORSConfig = CORSConfig(),
    private val pathPrefixes: Seq[String] = Seq("/")) extends Filter with AbstractCORSPolicy {

  override protected val logger = Logger(classOf[CORSFilter])

  override def apply(f: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {
    if (pathPrefixes.exists(request.path startsWith _)) {
      filterRequest(() => f(request), request)
    } else {
      f(request)
    }
  }
}

object CORSFilter {

  def apply(corsConfig: CORSConfig = CORSConfig(), pathPrefixes: Seq[String] = Seq("/")) =
    new CORSFilter(corsConfig, pathPrefixes)

}
