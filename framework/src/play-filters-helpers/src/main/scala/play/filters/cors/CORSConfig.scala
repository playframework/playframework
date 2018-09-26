/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.cors

import play.api.Configuration
import play.filters.cors.CORSConfig.Origins

import scala.concurrent.duration._

/**
 * Configuration for play.filters.cors.AbstractCORSPolicy.
 *
 * <ul>
 * <li>allow only requests with origins from a whitelist (by default all origins are allowed)</li>
 * <li>allow only HTTP methods from a whitelist for preflight requests (by default all methods are allowed)</li>
 * <li>allow only HTTP headers from a whitelist for preflight requests (by default all headers are allowed)</li>
 * <li>set custom HTTP headers to be exposed in the response (by default no headers are exposed)</li>
 * <li>disable/enable support for credentials (by default credentials support is enabled)</li>
 * <li>set how long (in seconds) the results of a preflight request can be cached in a preflight result cache (by default 3600 seconds, 1 hour)</li>
 * <li>enable/disable serving requests with origins not in whitelist as non-CORS requests (by default they are forbidden)</li>
 * </ul>
 *
 * @param  allowedOrigins
 * [[http://www.w3.org/TR/cors/#resource-requests §6.1.2]]
 * [[http://www.w3.org/TR/cors/#resource-preflight-requests §6.2.2]]
 * Always matching is acceptable since the list of origins can be unbounded.
 * @param  isHttpMethodAllowed
 * [[http://www.w3.org/TR/cors/#resource-preflight-requests §6.2.5]]
 * Always matching is acceptable since the list of methods can be unbounded.
 * @param isHttpHeaderAllowed
 * [[http://www.w3.org/TR/cors/#resource-preflight-requests §6.2.6]]
 * Always matching is acceptable since the list of headers can be unbounded.
 *
 */
case class CORSConfig(
    allowedOrigins: Origins = Origins.None,
    isHttpMethodAllowed: String => Boolean = _ => true,
    isHttpHeaderAllowed: String => Boolean = _ => true,
    exposedHeaders: Seq[String] = Seq.empty,
    supportsCredentials: Boolean = true,
    preflightMaxAge: Duration = 1.hour,
    serveForbiddenOrigins: Boolean = false
) {
  def anyOriginAllowed: Boolean = allowedOrigins == Origins.All

  def withAnyOriginAllowed = withOriginsAllowed(Origins.All)

  def withOriginsAllowed(origins: String => Boolean): CORSConfig = copy(allowedOrigins = Origins.Matching(origins))

  def withMethodsAllowed(methods: String => Boolean): CORSConfig = copy(isHttpMethodAllowed = methods)

  def withHeadersAllowed(headers: String => Boolean): CORSConfig = copy(isHttpHeaderAllowed = headers)

  def withExposedHeaders(headers: Seq[String]): CORSConfig = copy(exposedHeaders = headers)

  def withCredentialsSupport(supportsCredentials: Boolean): CORSConfig = copy(supportsCredentials = supportsCredentials)

  def withPreflightMaxAge(maxAge: Duration): CORSConfig = copy(preflightMaxAge = maxAge)

  def withServeForbiddenOrigins(serveForbiddenOrigins: Boolean): CORSConfig = copy(serveForbiddenOrigins = serveForbiddenOrigins)

  import scala.collection.JavaConverters._
  import scala.compat.java8.FunctionConverters._
  import java.util.{ function => juf }

  def withOriginsAllowed(origins: juf.Function[String, Boolean]): CORSConfig = withOriginsAllowed(origins.asScala)

  def withMethodsAllowed(methods: juf.Function[String, Boolean]): CORSConfig = withMethodsAllowed(methods.asScala)

  def withHeadersAllowed(headers: juf.Function[String, Boolean]): CORSConfig = withHeadersAllowed(headers.asScala)

  def withExposedHeaders(headers: java.util.List[String]): CORSConfig = withExposedHeaders(headers.asScala.toSeq)

  def withPreflightMaxAge(maxAge: java.time.Duration): CORSConfig = withPreflightMaxAge(Duration.fromNanos(maxAge.toNanos))
}

/**
 * Helpers to build CORS policy configurations
 */
object CORSConfig {

  /**
   * Origins allowed by the CORS filter
   */
  sealed trait Origins extends (String => Boolean)

  object Origins {

    case object All extends Origins {
      override def apply(v: String) = true
    }

    case class Matching(func: String => Boolean) extends Origins {
      override def apply(v: String) = func(v)
    }

    val None = Matching(_ => false)
  }

  /**
   *
   */
  val denyAll: CORSConfig =
    CORSConfig(
      allowedOrigins = Origins.None,
      isHttpMethodAllowed = _ => false,
      isHttpHeaderAllowed = _ => false,
      exposedHeaders = Seq.empty,
      supportsCredentials = true,
      preflightMaxAge = 0.seconds,
      serveForbiddenOrigins = false
    )

  /**
   * Builds a [[CORSConfig]] from a play.api.Configuration instance.
   *
   * @example The configuration is as follows:
   *          {{{
   *           play.filters.cors {
   *               pathPrefixes = ["/myresource", ...]  # ["/"] by default
   *               allowedOrigins = ["http://...", ...]  # If null, all origins are allowed
   *               allowedHttpMethods = ["PATCH", ...]  # If null, all methods are allowed
   *               allowedHttpHeaders = ["Custom-Header", ...]  # If null, all headers are allowed
   *               exposedHeaders = [...]  # empty by default
   *               supportsCredentials = true  # true by default
   *               preflightMaxAge = 1 hour  # 1 hour by default
   *               serveForbiddenOrigins = false  # false by default
   *           }
   *
   *          }}}
   */
  def fromConfiguration(conf: Configuration): CORSConfig = {
    val config = conf.get[Configuration]("play.filters.cors")
    fromUnprefixedConfiguration(config)
  }

  private[cors] def fromUnprefixedConfiguration(config: Configuration): CORSConfig = {
    CORSConfig(
      allowedOrigins = config.get[Option[Seq[String]]]("allowedOrigins") match {
        case Some(allowed) => Origins.Matching(allowed.toSet)
        case None => Origins.All
      },
      isHttpMethodAllowed =
        config.get[Option[Seq[String]]]("allowedHttpMethods").map { methods =>
          val s = methods.toSet
          s.contains _
        }.getOrElse(_ => true),
      isHttpHeaderAllowed =
        config.get[Option[Seq[String]]]("allowedHttpHeaders").map { headers =>
          val s = headers.map(_.toLowerCase(java.util.Locale.ENGLISH)).toSet
          s.contains _
        }.getOrElse(_ => true),
      exposedHeaders =
        config.get[Seq[String]]("exposedHeaders"),
      supportsCredentials =
        config.get[Boolean]("supportsCredentials"),
      preflightMaxAge =
        config.get[Duration]("preflightMaxAge"),
      serveForbiddenOrigins =
        config.get[Boolean]("serveForbiddenOrigins")
    )
  }
}
