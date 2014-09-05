/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.cors

import scala.concurrent.Future

import play.api.{ Configuration, Play }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{ ActionBuilder, Request, Result }

/**
 * An [[ActionBuilder]] that implements Cross-Origin Resource Sharing (CORS)
 *
 * It can be configured to...
 *
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
 * @see [[CORSFilter]]
 * @see [[http://www.w3.org/TR/cors/ CORS specification]]
 */
trait CORSActionBuilder extends ActionBuilder[Request] with AbstractCORSFilter {

  override protected val logger = Play.logger

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    filterRequest(() => block(request), request)
  }
}

/**
 * An [[ActionBuilder]] that implements Cross-Origin Resource Sharing (CORS)
 *
 * It can be configured to...
 *
 *  - allow only requests with origins from a whitelist (by default all origins are allowed)
 *  - allow only HTTP methods from a whitelist for preflight requests (by default all methods are allowed)
 *  - allow only HTTP headers from a whitelist for preflight requests (by default all methods are allowed)
 *  - set custom HTTP headers to be exposed in the response (by default no headers are exposed)
 *  - disable/enable support for credentials (by default credentials support is enabled)
 *  - set how long (in seconds) the results of a preflight request can be cached in a preflight result cache (by default 3600 seconds, 1 hour)
 *
 * @example
 * {{{
 * CORSActionBuilder { Ok } // an action that uses the application configuration
 *
 * CORSActionBuilder("my-conf-path") { Ok } // an action that uses a subtree of the application configuration
 *
 * val conf: Configuration = ...
 * CORSActionBuilder(conf) { Ok } // an action that uses a locally defined configuration
 * }}}
 *
 * @see [[CORSFilter]]
 * @see [[http://www.w3.org/TR/cors/ CORS specification]]
 */
object CORSActionBuilder extends CORSActionBuilder {

  override protected def conf = Play.maybeApplication.map(_.configuration).getOrElse(Configuration.empty)

  /**
   * Construct an action builder that uses a subtree of the application configuration.
   *
   * @param  configPath  The path to the subtree of the application configuration.
   */
  def apply(configPath: String): CORSActionBuilder = new CORSActionBuilder {
    override protected def conf =
      Play.maybeApplication.flatMap(_.configuration.getConfig(configPath)).getOrElse(Configuration.empty)
  }

  /**
   * Construct an action builder that uses locally defined configuration.
   *
   * @param  configuration  The local configuration to use in place of the global configuration.
   */
  def apply(configuration: Configuration): CORSActionBuilder = new CORSActionBuilder {
    override protected val conf = configuration
  }
}
