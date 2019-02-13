/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.headers

import javax.inject.{ Inject, Provider, Singleton }
import play.api.Configuration
import play.api.http.HeaderNames
import play.api.inject._
import play.api.mvc._

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
 * <li>{{play.filters.headers.referrerPolicy}} - sets referrerPolicy.  Some("origin-when-cross-origin, strict-origin-when-cross-origin") by default.
 * <li>{{play.filters.headers.allowActionSpecificHeaders}} - sets whether .withHeaders may be used to provide page-specific overrides.  False by default.
 * </ul>
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/HTTP/X-Frame-Options">X-Frame-Options</a>
 * @see <a href="http://blogs.msdn.com/b/ie/archive/2008/09/02/ie8-security-part-vi-beta-2-update.aspx">X-Content-Type-Options</a>
 * @see <a href="http://blogs.msdn.com/b/ie/archive/2008/07/02/ie8-security-part-iv-the-xss-filter.aspx">X-XSS-Protection</a>
 * @see <a href="http://www.adobe.com/devnet/articles/crossdomain_policy_file_spec.html">Cross Domain Policy File Specification</a>
 * @see <a href="https://www.w3.org/TR/referrer-policy/">Referrer Policy</a>
 */
object SecurityHeadersFilter {
  val X_FRAME_OPTIONS_HEADER: String = HeaderNames.X_FRAME_OPTIONS
  val X_XSS_PROTECTION_HEADER: String = HeaderNames.X_XSS_PROTECTION
  val X_CONTENT_TYPE_OPTIONS_HEADER: String = HeaderNames.X_CONTENT_TYPE_OPTIONS
  val X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER: String = HeaderNames.X_PERMITTED_CROSS_DOMAIN_POLICIES
  val CONTENT_SECURITY_POLICY_HEADER: String = HeaderNames.CONTENT_SECURITY_POLICY
  val REFERRER_POLICY: String = HeaderNames.REFERRER_POLICY

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
 * @param permittedCrossDomainPolicies "X-Permitted-Cross-Domain-Policies"
 * @param contentSecurityPolicy "Content-Security-Policy" - this is deprecated in favor of the dedicated CSPFilter.
 * @param referrerPolicy "Referrer-Policy"
 * @param allowActionSpecificHeaders Allows specific headers
 */
case class SecurityHeadersConfig(
    frameOptions: Option[String] = Some("DENY"),
    xssProtection: Option[String] = Some("1; mode=block"),
    contentTypeOptions: Option[String] = Some("nosniff"),
    permittedCrossDomainPolicies: Option[String] = Some("master-only"),
    @deprecated("Please use play.filters.csp.CSPFilter", "2.7.0") contentSecurityPolicy: Option[String] = None,
    referrerPolicy: Option[String] = Some("origin-when-cross-origin, strict-origin-when-cross-origin"),
    allowActionSpecificHeaders: Boolean = false) {
  def this() {
    this(frameOptions = Some("DENY"))
  }

  import java.{ util => ju }

  import scala.compat.java8.OptionConverters._

  def withFrameOptions(frameOptions: ju.Optional[String]): SecurityHeadersConfig =
    copy(frameOptions = frameOptions.asScala)
  def withXssProtection(xssProtection: ju.Optional[String]): SecurityHeadersConfig =
    copy(xssProtection = xssProtection.asScala)
  def withContentTypeOptions(contentTypeOptions: ju.Optional[String]): SecurityHeadersConfig =
    copy(contentTypeOptions = contentTypeOptions.asScala)
  def withPermittedCrossDomainPolicies(permittedCrossDomainPolicies: ju.Optional[String]): SecurityHeadersConfig =
    copy(permittedCrossDomainPolicies = permittedCrossDomainPolicies.asScala)

  @deprecated("Please use play.filters.csp.CSPFilter", "2.7.0")
  def withContentSecurityPolicy(contentSecurityPolicy: ju.Optional[String]): SecurityHeadersConfig =
    copy(contentSecurityPolicy = contentSecurityPolicy.asScala)
  def withReferrerPolicy(referrerPolicy: ju.Optional[String]): SecurityHeadersConfig = copy(referrerPolicy = referrerPolicy.asScala)
}

/**
 * Parses out a SecurityHeadersConfig from play.api.Configuration (usually this means application.conf).
 */
object SecurityHeadersConfig {

  def fromConfiguration(conf: Configuration): SecurityHeadersConfig = {

    val config = conf.get[Configuration]("play.filters.headers")

    config.getOptional[String]("contentSecurityPolicy").foreach { _ =>
      val logger = play.api.Logger(getClass)
      logger.warn("""play.filters.headers.contentSecurityPolicy is deprecated in 2.7.0.  Please use play.filters.csp.CSPFilter instead.""")
    }

    SecurityHeadersConfig(
      frameOptions = config.get[Option[String]]("frameOptions"),
      xssProtection = config.get[Option[String]]("xssProtection"),
      contentTypeOptions = config.get[Option[String]]("contentTypeOptions"),
      permittedCrossDomainPolicies = config.get[Option[String]]("permittedCrossDomainPolicies"),
      contentSecurityPolicy = config.get[Option[String]]("contentSecurityPolicy"),
      referrerPolicy = config.get[Option[String]]("referrerPolicy"),
      allowActionSpecificHeaders = config.get[Option[Boolean]]("allowActionSpecificHeaders").getOrElse(false))
  }
}

/**
 * The case class that implements the filter.  This gives you the most control, but you may want to use the apply()
 * method on the companion singleton for convenience.
 */
@Singleton
class SecurityHeadersFilter @Inject() (config: SecurityHeadersConfig) extends EssentialFilter {
  import SecurityHeadersFilter._

  /**
   * Returns the security headers for a request.
   * All security headers applied to all requests by default.
   * Omit any headers explicitly provided in the result object, provided
   * play.filters.headers.allowActionSpecificHeaders is true.
   * Override this method to alter that behavior.
   */
  protected def headers(request: RequestHeader, result: Result): Seq[(String, String)] = {
    val headers = Seq(
      config.frameOptions.map(X_FRAME_OPTIONS_HEADER -> _),
      config.xssProtection.map(X_XSS_PROTECTION_HEADER -> _),
      config.contentTypeOptions.map(X_CONTENT_TYPE_OPTIONS_HEADER -> _),
      config.permittedCrossDomainPolicies.map(X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER -> _),
      config.contentSecurityPolicy.map(CONTENT_SECURITY_POLICY_HEADER -> _),
      config.referrerPolicy.map(REFERRER_POLICY -> _)
    ).flatten

    if (config.allowActionSpecificHeaders) {
      headers.filter { case (name, _) => result.header.headers.get(name).isEmpty }
    } else {
      headers
    }
  }

  /**
   * Applies the filter to an action, appending the headers to the result so it shows in the HTTP response.
   */
  def apply(next: EssentialAction) = EssentialAction { req =>
    import play.core.Execution.Implicits.trampoline
    next(req).map(result => result.withHeaders(headers(req, result): _*))
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
class SecurityHeadersModule extends SimpleModule(
  bind[SecurityHeadersConfig].toProvider[SecurityHeadersConfigProvider],
  bind[SecurityHeadersFilter].toSelf
)

/**
 * The security headers components.
 */
trait SecurityHeadersComponents {
  def configuration: Configuration

  lazy val securityHeadersConfig: SecurityHeadersConfig = SecurityHeadersConfig.fromConfiguration(configuration)
  lazy val securityHeadersFilter: SecurityHeadersFilter = SecurityHeadersFilter(securityHeadersConfig)
}
