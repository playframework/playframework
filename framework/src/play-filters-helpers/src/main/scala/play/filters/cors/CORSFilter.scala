/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.cors

import scala.concurrent.Future

import play.api.{ Configuration, Logger, Play }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
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
 * @see [[CORSActionBuilder]]
 * @see [[http://www.w3.org/TR/cors/ CORS specification]]
 */
object CORSFilter extends Filter with AbstractCORSFilter {

  override protected val logger = Logger("play.filters")

  override protected def conf = Play.maybeApplication.map(_.configuration).getOrElse(Configuration.empty)

  private def pathPrefixes: Seq[String] =
    conf.getStringSeq("cors.path.prefixes").getOrElse(Seq("/"))

  override def apply(f: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {
    if (pathPrefixes.exists(request.path startsWith _)) {
      filterRequest(() => f(request), request)
    } else {
      f(request)
    }
  }
}
