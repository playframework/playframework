/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.cors

import play.api.{ PlayConfig, Configuration }

import scala.concurrent.duration._

/**
 * Configuration for [[AbstractCORSPolicy]]
 *
 *  - allow only requests with origins from a whitelist (by default all origins are allowed)
 *  - allow only HTTP methods from a whitelist for preflight requests (by default all methods are allowed)
 *  - allow only HTTP headers from a whitelist for preflight requests (by default all methods are allowed)
 *  - set custom HTTP headers to be exposed in the response (by default no headers are exposed)
 *  - disable/enable support for credentials (by default credentials support is enabled)
 *  - set how long (in seconds) the results of a preflight request can be cached in a preflight result cache (by default 3600 seconds, 1 hour)
 *
 * @param  anyOriginAllowed
 *   [[http://www.w3.org/TR/cors/#resource-requests §6.1.2]]
 *   [[http://www.w3.org/TR/cors/#resource-preflight-requests §6.2.2]]
 *   Always matching is acceptable since the list of origins can be unbounded.
 * @param  isHttpMethodAllowed
 *   [[http://www.w3.org/TR/cors/#resource-preflight-requests §6.2.5]]
 *   Always matching is acceptable since the list of methods can be unbounded.
 * @param isHttpHeaderAllowed
 *   [[http://www.w3.org/TR/cors/#resource-preflight-requests §6.2.6]]
 *   Always matching is acceptable since the list of headers can be unbounded.
 *
 */
case class CORSConfig(
  anyOriginAllowed: Boolean = true,
  allowedOrigins: Set[String] = Set.empty,
  isHttpMethodAllowed: String => Boolean = _ => true,
  isHttpHeaderAllowed: String => Boolean = _ => true,
  exposedHeaders: Seq[String] = Seq.empty,
  supportsCredentials: Boolean = true,
  preflightMaxAge: Duration = 1.hour)

/**
 * Helpers to build CORS policy configurations
 */
object CORSConfig {

  /**
   *
   */
  val denyAll: CORSConfig =
    CORSConfig(
      anyOriginAllowed = false,
      allowedOrigins = Set.empty,
      isHttpMethodAllowed = _ => false,
      isHttpHeaderAllowed = _ => false,
      exposedHeaders = Seq.empty,
      supportsCredentials = true,
      preflightMaxAge = 0.seconds)

  /**
   * Build a from a [[Configuration]]
   *
   * @example The configuration is as follows:
   * {{{
   * play.filters.cors {
   *     pathPrefixes = ["/myresource", ...]  # ["/"] by default
   *     allowedOrigins = ["http://...", ...]  # If null, all origins are allowed
   *     allowedHttpMethods = ["PATCH", ...]  # If null, all methods are allowed
   *     allowedHttpHeaders = ["Custom-Header", ...]  # If null, all headers are allowed
   *     exposedHeaders = [...]  # empty by default
   *     supportsCredentials = true  # true by default
   *     preflightMaxAge = 1 hour  # 1 hour by default
   * }
   *
   * }}}
   */
  def fromConfiguration(conf: Configuration): CORSConfig = {
    val config = PlayConfig(conf).get[PlayConfig]("play.filters.cors")
    fromUnprefixedConfiguration(config)
  }

  private[cors] def fromUnprefixedConfiguration(config: PlayConfig): CORSConfig = {
    val origins = config.getOptional[Seq[String]]("allowedOrigins")
    CORSConfig(
      anyOriginAllowed = origins.isEmpty,
      allowedOrigins = origins.map(_.toSet).getOrElse(Set.empty),
      isHttpMethodAllowed =
        config.getOptional[Seq[String]]("allowedHttpMethods").map { methods =>
          val s = methods.toSet
          s.contains _
        }.getOrElse(_ => true),
      isHttpHeaderAllowed =
        config.getOptional[Seq[String]]("allowedHttpHeaders").map { headers =>
          val s = headers.map(_.toLowerCase).toSet
          s.contains _
        }.getOrElse(_ => true),
      exposedHeaders =
        config.get[Seq[String]]("exposedHeaders"),
      supportsCredentials =
        config.get[Boolean]("supportsCredentials"),
      preflightMaxAge =
        config.get[Duration]("preflightMaxAge")
    )
  }
}
