/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
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
 * @param shouldProtect A function that decides based on the headers of the request if a check is needed.
 * @param nonce the CSP nonce configuration
 * @param hashes a list of CSP hashes that can be added to the header
 * @param directives the CSP directives configuration
 */
case class CSPConfig(
    reportOnly: Boolean = false,
    shouldProtect: RequestHeader => Boolean = _ => true,
    nonce: CSPNonceConfig = CSPNonceConfig(),
    hashes: Seq[CSPHashConfig] = Seq.empty,
    directives: CSPDirectivesConfig = CSPDirectivesConfig()) {

  import java.{ util => ju }

  import play.mvc.Http.{ RequestHeader => JRequestHeader }

  import scala.compat.java8.FunctionConverters._

  /** Java Constructor */
  def this() = this(reportOnly = true)

  def withReportOnly(reportOnly: Boolean): CSPConfig =
    copy(reportOnly = reportOnly)

  def withShouldProtect(shouldProtect: ju.function.Predicate[JRequestHeader]): CSPConfig =
    copy(shouldProtect = shouldProtect.asScala.compose(_.asJava))

  def withNonce(nonce: CSPNonceConfig): CSPConfig = {
    copy(nonce = nonce)
  }

  def withHashes(hashes: java.util.List[CSPHashConfig]): CSPConfig = {
    import scala.collection.JavaConverters._
    copy(hashes = hashes.asScala)
  }

  def withDirectives(directives: CSPDirectivesConfig): CSPConfig = {
    copy(directives = directives)
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
    val shouldProtect: RequestHeader => Boolean = { rh => checkRouteModifiers(rh) }

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
    val directivesConfig = CSPDirectivesConfig(
      baseUri = drctves.get[Option[String]]("base-uri"),
      blockAllMixedContent = drctves.get[Boolean]("block-all-mixed-content"),
      childSrc = drctves.get[Option[String]]("child-src"),
      connectSrc = drctves.get[Option[String]]("connect-src"),
      defaultSrc = drctves.get[Option[String]]("default-src"),
      disownOpener = drctves.get[Boolean]("disown-opener"),
      fontSrc = drctves.get[Option[String]]("font-src"),
      formAction = drctves.get[Option[String]]("form-action"),
      frameAncestors = drctves.get[Option[String]]("frame-ancestors"),
      frameSrc = drctves.get[Option[String]]("frame-src"),
      imgSrc = drctves.get[Option[String]]("img-src"),
      manifestSrc = drctves.get[Option[String]]("manifest-src"),
      mediaSrc = drctves.get[Option[String]]("media-src"),
      objectSrc = drctves.get[Option[String]]("object-src"),
      pluginTypes = drctves.get[Option[String]]("plugin-types"),
      reportUri = drctves.get[Option[String]]("report-uri"),
      reportTo = drctves.get[Option[String]]("report-to"),
      requireSriFor = drctves.get[Option[String]]("require-sri-for"),
      sandbox = drctves.get[Option[String]]("sandbox"),
      scriptSrc = drctves.get[Option[String]]("script-src"),
      styleSrc = drctves.get[Option[String]]("style-src"),
      workerSrc = drctves.get[Option[String]]("worker-src"),
      upgradeInsecureRequests = drctves.get[Boolean]("upgrade-insecure-requests")
    )

    CSPConfig(
      reportOnly = reportOnly,
      shouldProtect = shouldProtect,
      nonce = nonceConfig,
      hashes = hashConfigs,
      directives = directivesConfig)
  }
}

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

/**
 * CSP directives configuration settings.
 *
 * @param baseUri the basic uri directive [[https://www.w3.org/TR/CSP3/#directive-base-uri base-uri direcitve]]
 * @param blockAllMixedContent the [[https://www.w3.org/TR/mixed-content/#block-all-mixed-content block-all-mixed-content directive]]
 * @param childSrc the [[https://www.w3.org/TR/CSP3/#directive-child-src child-src directive]]
 * @param connectSrc the [[https://www.w3.org/TR/CSP3/#directive-connect-src connect-src directive]]
 * @param defaultSrc the [[https://www.w3.org/TR/CSP3/#directive-default-src default-src directive]]
 * @param disownOpener the [[https://www.w3.org/TR/CSP3/#directive-disown-opener disown-opener directive]]
 * @param fontSrc the [[https://www.w3.org/TR/CSP3/#directive-font-src font-src directive]]
 * @param formAction the [[https://www.w3.org/TR/CSP3/#directive-form-action form-action directive]]
 * @param frameAncestors the [[https://www.w3.org/TR/CSP3/#directive-frame-ancestors frame-ancestors directive]]
 * @param frameSrc the [[https://www.w3.org/TR/CSP3/#directive-frame-src frame-src directive]]
 * @param imgSrc the [[https://www.w3.org/TR/CSP3/#directive-img-src img-src directive]]
 * @param manifestSrc the [[https://www.w3.org/TR/CSP3/#directive-manifest-src manifest-src directive]]
 * @param mediaSrc the [[https://www.w3.org/TR/CSP3/#directive-media-src media-src directive]]
 * @param objectSrc the [[https://www.w3.org/TR/CSP3/#directive-object-src object-src directive]]
 * @param pluginTypes the [[https://www.w3.org/TR/CSP3/#directive-plugin-types plugin-types directive]]
 * @param reportUri the [[https://www.w3.org/TR/CSP3/#directive-report-uri report-uri directive]]
 * @param reportTo the [[https://www.w3.org/TR/CSP3/#directive-report-to report-to directive]]
 * @param requireSriFor the [[https://w3c.github.io/webappsec-subresource-integrity/#opt-in-require-sri-for require-sri-for directive]]
 * @param sandbox the [[https://www.w3.org/TR/CSP3/#directive-sandbox sandbox directive]]
 * @param scriptSrc the [[https://www.w3.org/TR/CSP3/#directive-script-src script-src directive]]
 * @param styleSrc the [[https://www.w3.org/TR/CSP3/#directive-style-src style-src directive]]
 * @param workerSrc the [[https://www.w3.org/TR/CSP3/#directive-worker-src worker-src directive]]
 * @param upgradeInsecureRequests the [[https://www.w3.org/TR/upgrade-insecure-requests/#delivery upgrade-insecure-requests directive]]
 */
case class CSPDirectivesConfig(
    baseUri: Option[String] = None,
    blockAllMixedContent: Boolean = false,
    childSrc: Option[String] = None,
    connectSrc: Option[String] = None,
    defaultSrc: Option[String] = None,
    disownOpener: Boolean = false,
    fontSrc: Option[String] = None,
    formAction: Option[String] = None,
    frameAncestors: Option[String] = None,
    frameSrc: Option[String] = None,
    imgSrc: Option[String] = None,
    manifestSrc: Option[String] = None,
    mediaSrc: Option[String] = None,
    objectSrc: Option[String] = None,
    pluginTypes: Option[String] = None,
    reportUri: Option[String] = None,
    reportTo: Option[String] = None,
    requireSriFor: Option[String] = None,
    sandbox: Option[String] = None,
    scriptSrc: Option[String] = None,
    styleSrc: Option[String] = None,
    workerSrc: Option[String] = None,
    upgradeInsecureRequests: Boolean = false) {

  import java.{ util => ju }

  import scala.compat.java8.OptionConverters._

  /** https://www.w3.org/TR/CSP3/#directive-base-uri */
  def withBaseUri(baseUri: ju.Optional[String]): CSPDirectivesConfig =
    copy(baseUri = baseUri.asScala)

  /** https://www.w3.org/TR/mixed-content/#block-all-mixed-content */
  def withBlockAllMixedContent(blockAllMixedContent: Boolean): CSPDirectivesConfig =
    copy(blockAllMixedContent = blockAllMixedContent)

  /** https://www.w3.org/TR/CSP3/#directive-child-src */
  def withChildSrc(childSrc: ju.Optional[String]): CSPDirectivesConfig =
    copy(childSrc = childSrc.asScala)

  /** https://www.w3.org/TR/CSP3/#directive-connect-src */
  def withConnectSrc(connectSrc: ju.Optional[String]): CSPDirectivesConfig =
    copy(connectSrc = connectSrc.asScala)

  /** https://www.w3.org/TR/CSP3/#directive-default-src */
  def withDefaultSrc(defaultSrc: ju.Optional[String]): CSPDirectivesConfig =
    copy(defaultSrc = defaultSrc.asScala)

  /** https://www.w3.org/TR/CSP3/#directive-disown-opener */
  def withDisownOpener(disownOpener: Boolean): CSPDirectivesConfig =
    copy(disownOpener = disownOpener)

  /** https://www.w3.org/TR/CSP3/#directive-font-src */
  def withFontSrc(fontSrc: ju.Optional[String]): CSPDirectivesConfig =
    copy(fontSrc = fontSrc.asScala)

  /** https://www.w3.org/TR/CSP3/#directive-form-action */
  def withFormAction(formAction: ju.Optional[String]): CSPDirectivesConfig =
    copy(formAction = formAction.asScala)

  /** https://www.w3.org/TR/CSP3/#directive-frame-ancestors */
  def withFrameAncestors(
    frameAncestors: ju.Optional[String]): CSPDirectivesConfig =
    copy(frameAncestors = frameAncestors.asScala)

  /** https://www.w3.org/TR/CSP3/#directive-frame-src */
  def withFrameSrc(frameSrc: ju.Optional[String]): CSPDirectivesConfig =
    copy(frameSrc = frameSrc.asScala)

  /** https://www.w3.org/TR/CSP3/#directive-img-src */
  def withImgSrc(imgSrc: ju.Optional[String]): CSPDirectivesConfig =
    copy(imgSrc = imgSrc.asScala)

  /** https://www.w3.org/TR/CSP3/#directive-manifest-src */
  def withManifestSrc(manifestSrc: ju.Optional[String]): CSPDirectivesConfig =
    copy(manifestSrc = manifestSrc.asScala)

  /** https://www.w3.org/TR/CSP3/#directive-media-src */
  def withMediaSrc(mediaSrc: ju.Optional[String]): CSPDirectivesConfig =
    copy(mediaSrc = mediaSrc.asScala)

  /** https://www.w3.org/TR/CSP3/#directive-object-src */
  def withObjectSrc(objectSrc: ju.Optional[String]): CSPDirectivesConfig =
    copy(objectSrc = objectSrc.asScala)

  /** https://www.w3.org/TR/CSP3/#directive-plugin-types */
  def withPluginTypes(pluginTypes: ju.Optional[String]): CSPDirectivesConfig =
    copy(pluginTypes = pluginTypes.asScala)

  /** https://www.w3.org/TR/CSP3/#directive-report-uri */
  def withReportUri(reportUri: ju.Optional[String]): CSPDirectivesConfig =
    copy(reportUri = reportUri.asScala)

  /** https://www.w3.org/TR/CSP3/#directive-report-to */
  def withReportTo(reportTo: ju.Optional[String]): CSPDirectivesConfig =
    copy(reportTo = reportTo.asScala)

  /** https://w3c.github.io/webappsec-subresource-integrity/#opt-in-require-sri-for */
  def withRequireSriFor(
    requireSriFor: ju.Optional[String]): CSPDirectivesConfig =
    copy(requireSriFor = requireSriFor.asScala)

  /** https://www.w3.org/TR/CSP3/#directive-sandbox */
  def withSandbox(sandbox: ju.Optional[String]): CSPDirectivesConfig =
    copy(sandbox = sandbox.asScala)

  /** https://www.w3.org/TR/CSP3/#directive-script-src */
  def withScriptSrc(scriptSrc: ju.Optional[String]): CSPDirectivesConfig =
    copy(scriptSrc = scriptSrc.asScala)

  /** https://www.w3.org/TR/CSP3/#directive-style-src */
  def withStyleSrc(styleSrc: ju.Optional[String]): CSPDirectivesConfig =
    copy(styleSrc = styleSrc.asScala)

  /** https://www.w3.org/TR/CSP3/#directive-worker-src */
  def withWorkerSrc(workerSrc: ju.Optional[String]): CSPDirectivesConfig =
    copy(workerSrc = workerSrc.asScala)

  /** https://www.w3.org/TR/upgrade-insecure-requests/#delivery */
  def withUpgradeInsecureRequests(upgradeInsecureRequests: Boolean): CSPDirectivesConfig =
    copy(upgradeInsecureRequests = upgradeInsecureRequests)

}
