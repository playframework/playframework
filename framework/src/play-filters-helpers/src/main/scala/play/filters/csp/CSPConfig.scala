/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csp

import play.api.Configuration
import play.api.mvc.RequestHeader

/**
 * CSP Configuration.
 *
 * @see <a href="https://www.w3.org/TR/CSP3/">Content Security Policy Level 3</a>
 *
 * @param reportOnly true if the header should be Content-Security-Policy-Report-Only.
 * @param shouldFilterRequest A function that decides based on the headers of the request if a check is needed.
 * @param nonce the CSP nonce configuration
 * @param hashes a list of CSP hashes that can be added to the header
 * @param directives the CSP directives configuration
 */
case class CSPConfig(
    reportOnly: Boolean = false,
    shouldFilterRequest: RequestHeader => Boolean = _ => true,
    nonce: CSPNonceConfig = CSPNonceConfig(),
    hashes: Seq[CSPHashConfig] = Seq.empty,
    directives: Seq[CSPDirective] = Seq.empty) {

  import java.{ util => ju }

  import play.mvc.Http.{ RequestHeader => JRequestHeader }

  import scala.compat.java8.FunctionConverters._

  /** Java Constructor */
  def this() = this(reportOnly = true)

  def withReportOnly(reportOnly: Boolean): CSPConfig =
    copy(reportOnly = reportOnly)

  def withShouldProtect(shouldFilterRequest: ju.function.Predicate[JRequestHeader]): CSPConfig =
    copy(shouldFilterRequest = shouldFilterRequest.asScala.compose(_.asJava))

  def withNonce(nonce: CSPNonceConfig): CSPConfig = {
    copy(nonce = nonce)
  }

  def withHashes(hashes: java.util.List[CSPHashConfig]): CSPConfig = {
    import scala.collection.JavaConverters._
    copy(hashes = hashes.asScala)
  }

  def withDirectives(directives: java.util.List[CSPDirective]): CSPConfig = {
    import scala.collection.JavaConverters._
    copy(directives = directives.asScala)
  }
}

/**
 * This singleton object contains factory methods to create a CSPConfig instance
 * from configuration.
 */
object CSPConfig {

  /**
   * Creates CSPConfig from a raw Configuration object, using "play.filters.csp".
   *
   * @param conf a configuration instance.
   * @return a CSPConfig instance.
   */
  def fromConfiguration(conf: Configuration): CSPConfig = {
    val config = conf.get[Configuration]("play.filters.csp")
    fromUnprefixedConfiguration(config)
  }

  /**
   * Creates CSPConfig from a raw configuration object with no prefix.
   *
   * @param config a configuration instance
   * @return a CSPConfig instance.
   */
  def fromUnprefixedConfiguration(config: Configuration): CSPConfig = {
    import collection.JavaConverters._

    val reportOnly = config.get[Boolean]("reportOnly")

    val whitelistModifiers = config.get[Seq[String]]("routeModifiers.whiteList")
    val blacklistModifiers = config.get[Seq[String]]("routeModifiers.blackList")
    @inline def checkRouteModifiers(rh: RequestHeader): Boolean = {
      import play.api.routing.Router.RequestImplicits._
      if (whitelistModifiers.isEmpty) {
        blacklistModifiers.exists(rh.hasRouteModifier)
      } else {
        !whitelistModifiers.exists(rh.hasRouteModifier)
      }
    }
    val shouldFilterRequest: RequestHeader => Boolean = { rh => checkRouteModifiers(rh) }

    val nonce = config.get[Configuration]("nonce")
    val nonceConfig = CSPNonceConfig(
      enabled = nonce.get[Boolean]("enabled"),
      pattern = nonce.get[String]("pattern"),
      header = nonce.get[Boolean]("header")
    )

    val hashes = config.underlying.getConfigList("hashes").asScala
    val hashConfigs = hashes.map { hc =>
      val hashConfig = Configuration(hc)
      CSPHashConfig(
        algorithm = hashConfig.getAndValidate[String]("algorithm", Set("sha256", "sha384", "sha512")),
        hash = hashConfig.get[String]("hash"),
        pattern = hashConfig.get[String]("pattern"))
    }

    val drctves = config.get[Configuration]("directives")
    val directivesConfig: Seq[CSPDirective] = drctves.entrySet.to[Seq].flatMap {
      case (k, v) if v.unwrapped() == null =>
        None
      case (k, v) =>
        Some(CSPDirective(k, v.unwrapped().toString))
    }

    CSPConfig(
      reportOnly = reportOnly,
      shouldFilterRequest = shouldFilterRequest,
      nonce = nonceConfig,
      hashes = hashConfigs,
      directives = directivesConfig)
  }
}

case class CSPDirective(name: String, value: String)

/**
 * CSP Nonce Configuration.
 *
 * @param enabled if true, a nonce is generated in processing
 * @param pattern the pattern in directives to substitute with nonce, DEFAULT_CSP_NONCE_PATTERN by default.
 * @param header if true, renders HeaderNames.X_CONTENT_SECURITY_POLICY_NONCE_HEADER
 */
case class CSPNonceConfig(
    enabled: Boolean = true,
    pattern: String = CPSNonceConfig.DEFAULT_CSP_NONCE_PATTERN,
    header: Boolean = true) {

  /** Java constructor */
  def this() = this(true)

  def withEnabled(enabled: Boolean): CSPNonceConfig = {
    copy(enabled = enabled)
  }

  def withPattern(pattern: String): CSPNonceConfig = {
    copy(pattern = pattern)
  }

  def withHeader(header: Boolean): CSPNonceConfig = copy(header = header)
}

object CPSNonceConfig {
  val DEFAULT_CSP_NONCE_PATTERN = "%CSP_NONCE_PATTERN%"
}

/**
 * CSP Hash Configuration.
 *
 * @param algorithm the algorithm from https://www.w3.org/TR/CSP3/#grammardef-hash-algorithm
 * @param hash set to the hash value in configuration
 * @param pattern the pattern in directives to substitute with hash.
 */
case class CSPHashConfig(algorithm: String, hash: String, pattern: String) {

  // There is no default Java constructor, since all values are required here.

  def withAlgorithm(algorithm: String): CSPHashConfig = {
    copy(algorithm = algorithm)
  }

  def withHash(hash: String): CSPHashConfig = {
    copy(hash = hash)
  }

  def withPattern(pattern: String): CSPHashConfig = {
    copy(pattern = pattern)
  }
}
