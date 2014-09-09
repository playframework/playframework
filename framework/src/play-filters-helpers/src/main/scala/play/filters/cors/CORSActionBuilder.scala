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
 * @see [[CORSFilter]]
 * @see [[http://www.w3.org/TR/cors/ CORS specification]]
 */
trait CORSActionBuilder extends ActionBuilder[Request] with AbstractCORSPolicy {

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
 * val corsConfig: CORSConfig = ...
 * CORSActionBuilder(conf) { Ok } // an action that uses a locally defined configuration
 * }}}
 *
 * @see [[CORSFilter]]
 * @see [[http://www.w3.org/TR/cors/ CORS specification]]
 */
object CORSActionBuilder extends CORSActionBuilder {

  private def globalConf =
    Play.maybeApplication.map(_.configuration).getOrElse(Configuration.empty)

  override protected def corsConfig =
    CORSConfig.fromConfiguration(globalConf)

  /**
   * Construct an action builder that uses a subtree of the application configuration.
   *
   * @param  configPath  The path to the subtree of the application configuration.
   */
  def apply(configPath: String): CORSActionBuilder = new CORSActionBuilder {
    override protected def corsConfig =
      CORSConfig.fromConfiguration(
        Play.maybeApplication.flatMap(
          _.configuration.getConfig(configPath)).getOrElse(Configuration.empty))
  }

  /**
   * Construct an action builder that uses locally defined configuration.
   *
   * @param  config  The local configuration to use in place of the global configuration.
   * @see [[CORSConfig]]
   */
  def apply(config: CORSConfig): CORSActionBuilder = new CORSActionBuilder {
    override protected val corsConfig = config
  }
}
