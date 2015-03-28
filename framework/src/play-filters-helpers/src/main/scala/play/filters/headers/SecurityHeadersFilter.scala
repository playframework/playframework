/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.headers

import javax.inject.{ Singleton, Inject, Provider }

import play.api.inject.Module
import play.api.mvc._
import scala.concurrent.Future
import play.api.{ Environment, PlayConfig, Configuration }

/**
 * This class sets a number of common security headers on the HTTP request.
 *
 * NOTE: Because these are security headers, they are "secure by default."  If the filter is applied, but these
 * fields are NOT defined in Configuration, the defaults on the filter are NOT omitted, but are instead
 * set to the strictest possible value.
 *
 * <ul>
 * <li>{{play.filters.headers.frameOptions}} - sets frameOptions.  Some("DENY") by default.
 * <li>{{play.filters.headers.xssProtection}} - sets xssProtection.  Some("1; mode=block") by default.
 * <li>{{play.filters.headers.contentTypeOptions}} - sets contentTypeOptions. Some("nosniff") by default.
 * <li>{{play.filters.headers.permittedCrossDomainPolicies}} - sets permittedCrossDomainPolicies. Some("master-only") by default.
 * <li>{{play.filters.headers.contentSecurityPolicy}} - sets contentSecurityPolicy. Some("default-src 'self'") by default.
 * </ul>
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/HTTP/X-Frame-Options">X-Frame-Options</a>
 * @see <a href="http://blogs.msdn.com/b/ie/archive/2008/09/02/ie8-security-part-vi-beta-2-update.aspx">X-Content-Type-Options</a>
 * @see <a href="http://blogs.msdn.com/b/ie/archive/2008/07/02/ie8-security-part-iv-the-xss-filter.aspx">X-XSS-Protection</a>
 * @see <a href="http://www.html5rocks.com/en/tutorials/security/content-security-policy/">Content-Security-Policy</a>
 * @see <a href="http://www.adobe.com/devnet/articles/crossdomain_policy_file_spec.html">Cross Domain Policy File Specification</a>
 */
object SecurityHeadersFilter {
  val X_FRAME_OPTIONS_HEADER = "X-Frame-Options"
  val X_XSS_PROTECTION_HEADER = "X-XSS-Protection"
  val X_CONTENT_TYPE_OPTIONS_HEADER = "X-Content-Type-Options"
  val X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER = "X-Permitted-Cross-Domain-Policies"
  val CONTENT_SECURITY_POLICY_HEADER = "Content-Security-Policy"

  /**
   * Convenience method for creating a SecurityHeadersFilter that reads settings from application.conf.  Generally speaking,
   * you'll want to use this or the apply(SecurityHeadersConfig) method.
   *
   * @return a configured SecurityHeadersFilter.
   */
  def apply(config: SecurityHeadersConfig = SecurityHeadersConfig()): SecurityHeadersFilter = {
    new SecurityHeadersFilter(config)
  }

  /**
   * Convenience method for creating a filter using play.api.Configuration.  Good for testing.
   *
   * @param config a configuration object that may contain string settings.
   * @return a configured SecurityHeadersFilter.
   */
  def apply(config: Configuration): SecurityHeadersFilter = {
    new SecurityHeadersFilter(SecurityHeadersConfig.fromConfiguration(config))
  }
}

/**
 * A type safe configuration object for setting security headers.
 *
 * @param frameOptions "X-Frame-Options":
 * @param xssProtection "X-XSS-Protection":
 * @param contentTypeOptions "X-Content-Type-Options"
 * @param permittedCrossDomainPolicies "X-Permitted-Cross-Domain-Policies".
 * @param contentSecurityPolicy "Content-Security-Policy"
 */
case class SecurityHeadersConfig(frameOptions: Option[String] = Some("DENY"),
  xssProtection: Option[String] = Some("1; mode=block"),
  contentTypeOptions: Option[String] = Some("nosniff"),
  permittedCrossDomainPolicies: Option[String] = Some("master-only"),
  contentSecurityPolicy: Option[String] = Some("default-src 'self'"))

/**
 * Parses out a SecurityHeadersConfig from play.api.Configuration (usually this means application.conf).
 */
object SecurityHeadersConfig {

  def fromConfiguration(conf: Configuration): SecurityHeadersConfig = {
    val config = PlayConfig(conf).get[PlayConfig]("play.filters.headers")

    SecurityHeadersConfig(
      frameOptions = config.getOptional[String]("frameOptions"),
      xssProtection = config.getOptional[String]("xssProtection"),
      contentTypeOptions = config.getOptional[String]("contentTypeOptions"),
      permittedCrossDomainPolicies = config.getOptional[String]("permittedCrossDomainPolicies"),
      contentSecurityPolicy = config.getOptional[String]("contentSecurityPolicy"))
  }
}

/**
 * The case class that implements the filter.  This gives you the most control, but you may want to use the apply()
 * method on the companion singleton for convenience.
 */
@Singleton
class SecurityHeadersFilter @Inject() (config: SecurityHeadersConfig) extends EssentialFilter {
  import SecurityHeadersFilter._

  private val headers: Seq[(String, String)] = Seq(
    config.frameOptions.map(X_FRAME_OPTIONS_HEADER -> _),
    config.xssProtection.map(X_XSS_PROTECTION_HEADER -> _),
    config.contentTypeOptions.map(X_CONTENT_TYPE_OPTIONS_HEADER -> _),
    config.permittedCrossDomainPolicies.map(X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER -> _),
    config.contentSecurityPolicy.map(CONTENT_SECURITY_POLICY_HEADER -> _)
  ).flatten

  /**
   * Applies the filter to an action, appending the headers to the result so it shows in the HTTP response.
   */
  def apply(next: EssentialAction) = EssentialAction { req =>
    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    next(req).map(_.withHeaders(headers: _*))
  }
}

/**
 * Provider for security headers configuration.
 */
@Singleton
class SecurityHeadersConfigProvider @Inject() (configuration: Configuration) extends Provider[SecurityHeadersConfig] {
  lazy val get = SecurityHeadersConfig.fromConfiguration(configuration)
}

/**
 * The security headers module.
 */
class SecurityHeadersModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = Seq(
    bind[SecurityHeadersConfig].toProvider[SecurityHeadersConfigProvider],
    bind[SecurityHeadersFilter].toSelf
  )
}

/**
 * The security headers components.
 */
trait SecurityHeadersComponents {
  def configuration: Configuration

  lazy val securityHeadersConfig: SecurityHeadersConfig = SecurityHeadersConfig.fromConfiguration(configuration)
  lazy val securityHeadersFilter: SecurityHeadersFilter = SecurityHeadersFilter(securityHeadersConfig)
}