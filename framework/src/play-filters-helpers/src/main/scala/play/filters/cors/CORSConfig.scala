/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.cors

import play.api.Configuration

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
  anyOriginAllowed: Boolean,
  allowedOrigins: Set[String],
  isHttpMethodAllowed: String => Boolean,
  isHttpHeaderAllowed: String => Boolean,
  exposedHeaders: Seq[String],
  supportsCredentials: Boolean,
  preflightMaxAge: Int)

/**
 * Helpers to build CORS policy configurations
 */
object CORSConfig {

  /**
   * A permissive default
   *
   */
  val default: CORSConfig =
    CORSConfig(
      anyOriginAllowed = true,
      allowedOrigins = Set.empty,
      isHttpMethodAllowed = _ => true,
      isHttpHeaderAllowed = _ => true,
      exposedHeaders = Seq.empty,
      supportsCredentials = true,
      preflightMaxAge = 3600)

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
      preflightMaxAge = 0)

  /**
   * Build a from a [[Configuration]]
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
   */
  def fromConfiguration(conf: Configuration): CORSConfig = {
    val origins = conf.getStringSeq("cors.allowed.origins")
    CORSConfig(
      anyOriginAllowed = origins.isEmpty,
      allowedOrigins = origins.map(_.toSet).getOrElse(Set.empty),
      isHttpMethodAllowed =
        conf.getStringSeq("cors.allowed.http.methods").map { methods =>
          val s = methods.toSet
          s.contains _
        }.getOrElse(_ => true),
      isHttpHeaderAllowed =
        conf.getStringSeq("cors.allowed.http.headers").map { headers =>
          val s = headers.map(_.toLowerCase).toSet
          s.contains _
        }.getOrElse(_ => true),
      exposedHeaders =
        conf.getStringSeq("cors.exposed.headers").getOrElse(Seq.empty),
      supportsCredentials =
        conf.getBoolean("cors.support.credentials").getOrElse(true),
      preflightMaxAge =
        conf.getInt("cors.preflight.maxage").getOrElse(3600))
  }
}
