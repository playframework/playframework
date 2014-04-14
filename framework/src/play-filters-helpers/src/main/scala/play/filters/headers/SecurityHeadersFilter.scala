/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.headers

import play.api.mvc._
import scala.concurrent.Future
import play.api.Configuration

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
  val X_CONTENT_SECURITY_POLICY_HEADER = "X-Content-Security-Policy"
  val CONTENT_SECURITY_POLICY_HEADER = "Content-Security-Policy"

  val DEFAULT_FRAME_OPTIONS = "DENY"
  val DEFAULT_XSS_PROTECTION = "1; mode=block"
  val DEFAULT_CONTENT_TYPE_OPTIONS = "nosniff"
  val DEFAULT_PERMITTED_CROSS_DOMAIN_POLICIES = "master-only"
  val DEFAULT_CONTENT_SECURITY_POLICY = "default-src 'self'"

  /**
   * Convenience method for creating a SecurityHeadersFilter that reads settings from application.conf.  Generally speaking,
   * you'll want to use this or the apply(SecurityHeadersConfig) method.
   *
   * @return a configured SecurityHeadersFilter.
   */
  def apply(): SecurityHeadersFilter = {
    val securityHeadersConfig = new SecurityHeadersParser().parse(play.api.Play.current.configuration)
    apply(securityHeadersConfig)
  }

  /**
   * Convenience method for creating a filter using play.api.Configuration.  Good for testing.
   *
   * @param config a configuration object that may contain string settings.
   * @return a configured SecurityHeadersFilter.
   */
  def apply(config: Configuration): SecurityHeadersFilter = {
    val securityHeadersConfig = new SecurityHeadersParser().parse(config)
    apply(securityHeadersConfig)
  }

  /**
   * Convenience method for creating a filter using SecurityHeadersConfig case class.  Use this if you have settings
   * that you want to specifically turn off by setting to None.
   *
   * @param securityHeaderConfig
   * @return
   */
  def apply(securityHeaderConfig: SecurityHeadersConfig): SecurityHeadersFilter = {
    new SecurityHeadersFilter(securityHeaderConfig)
  }
}

/**
 * SecurityHeaders trait.  The default case class doesn't use it, but if you create a class which may return
 * different values based off the header and the result, this is where to start.
 */
trait SecurityHeadersConfig {
  def frameOptions: Option[String]

  def xssProtection: Option[String]

  def contentTypeOptions: Option[String]

  def permittedCrossDomainPolicies: Option[String]

  def contentSecurityPolicy: Option[String]
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
case class DefaultSecurityHeadersConfig(frameOptions: Option[String],
  xssProtection: Option[String],
  contentTypeOptions: Option[String],
  permittedCrossDomainPolicies: Option[String],
  contentSecurityPolicy: Option[String]) extends SecurityHeadersConfig

/**
 * Parses out a SecurityHeadersConfig from play.api.Configuration (usually this means application.conf).
 */
class SecurityHeadersParser {

  val FRAME_OPTIONS_CONFIG_PATH: String = "play.filters.headers.frameOptions"
  val XSS_PROTECTION_CONFIG_PATH: String = "play.filters.headers.xssProtection"
  val CONTENT_TYPE_OPTIONS_CONFIG_PATH: String = "play.filters.headers.contentTypeOptions"
  val PERMITTED_CROSS_DOMAIN_POLICIES_CONFIG_PATH: String = "play.filters.headers.permittedCrossDomainPolicies"
  val CONTENT_SECURITY_POLICY_CONFIG_PATH: String = "play.filters.headers.contentSecurityPolicy"

  def parse(config: Configuration): SecurityHeadersConfig = {
    import SecurityHeadersFilter._

    val frameOptions: String = config.getString(FRAME_OPTIONS_CONFIG_PATH).getOrElse(DEFAULT_FRAME_OPTIONS)
    val xssProtection: String = config.getString(XSS_PROTECTION_CONFIG_PATH).getOrElse(DEFAULT_XSS_PROTECTION)
    val contentTypeOptions: String = config.getString(CONTENT_TYPE_OPTIONS_CONFIG_PATH).getOrElse(DEFAULT_CONTENT_TYPE_OPTIONS)
    val permittedCrossDomainPolicies: String = config.getString(PERMITTED_CROSS_DOMAIN_POLICIES_CONFIG_PATH).getOrElse(DEFAULT_PERMITTED_CROSS_DOMAIN_POLICIES)
    val contentSecurityPolicy: String = config.getString(CONTENT_SECURITY_POLICY_CONFIG_PATH).getOrElse(DEFAULT_CONTENT_SECURITY_POLICY)

    DefaultSecurityHeadersConfig(
      frameOptions = Option(frameOptions),
      xssProtection = Option(xssProtection),
      contentTypeOptions = Option(contentTypeOptions),
      permittedCrossDomainPolicies = Option(permittedCrossDomainPolicies),
      contentSecurityPolicy = Option(contentSecurityPolicy))
  }
}

/**
 * The case class that implements the filter.  This gives you the most control, but you may want to use the apply()
 * method on the companion singleton for convenience.
 */
class SecurityHeadersFilter(config: SecurityHeadersConfig) extends Filter {

  /**
   * Zero argument constructor.  This allows the Java GlobalSettings class to call this class and load configured
   * options from application.conf.
   *
   * @return a new configured instance of SecurityHeadersFilter.
   */
  def this() = this(new SecurityHeadersParser().parse(play.api.Play.current.configuration))

  /**
   * Applies the filter to an action, appending the headers to the result so it shows in the HTTP response.
   *
   * @param f the rawest form of Action.
   * @param rh the request header.
   * @return a result with the security headers included, using r.withHeaders.
   */
  def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    import SecurityHeadersFilter._

    val result = f(rh)
    result.map {
      r =>
        val headers: Seq[(String, String)] = Seq(
          config.frameOptions.map(X_FRAME_OPTIONS_HEADER -> _),
          config.xssProtection.map(X_XSS_PROTECTION_HEADER -> _),
          config.contentTypeOptions.map(X_CONTENT_TYPE_OPTIONS_HEADER -> _),
          config.permittedCrossDomainPolicies.map(X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER -> _),
          config.contentSecurityPolicy.map(X_CONTENT_SECURITY_POLICY_HEADER -> _),
          config.contentSecurityPolicy.map(CONTENT_SECURITY_POLICY_HEADER -> _)
        ).flatten
        r.withHeaders(headers: _*)
    }
  }
}