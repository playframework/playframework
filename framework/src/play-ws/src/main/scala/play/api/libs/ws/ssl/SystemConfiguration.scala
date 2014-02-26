/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import play.api.libs.ws.WSClientConfig

/**
 * Configures global system properties on the JSSE implementation, if defined.
 */
class SystemConfiguration {

  val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def configure(config: WSClientConfig) {
    val allowUnsafeRenegotiation = (for {
      ssl <- config.ssl
      loose <- ssl.loose
      looseAllowUnsafeRenegotiation <- loose.allowUnsafeRenegotiation
    } yield looseAllowUnsafeRenegotiation).getOrElse(false)
    configureUnsafeRenegotiation(allowUnsafeRenegotiation)

    val allowLegacyHelloMessages = (for {
      ssl <- config.ssl
      loose <- ssl.loose
      looseAllowLegacyHelloMessages <- loose.allowLegacyHelloMessages
    } yield looseAllowLegacyHelloMessages).getOrElse(false)
    configureAllowLegacyHelloMessages(allowLegacyHelloMessages)

    val disableCheckRevocation = (for {
      ssl <- config.ssl
      loose <- ssl.loose
      looseDisableCheckRevocation <- loose.disableCheckRevocation
    } yield looseDisableCheckRevocation).getOrElse(false)
    configureCheckRevocation(!disableCheckRevocation)
  }

  def configureUnsafeRenegotiation(allowUnsafeRenegotiation: Boolean) {
    System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", allowUnsafeRenegotiation.toString)
    logger.debug("configureUnsafeRenegotiation: sun.security.ssl.allowUnsafeRenegotiation = {}", allowUnsafeRenegotiation.toString)
  }

  def configureAllowLegacyHelloMessages(allowLegacyHelloMessages: Boolean) {
    System.setProperty("sun.security.ssl.allowLegacyHelloMessages", allowLegacyHelloMessages.toString)
    logger.debug("configureAllowLegacyHelloMessages: sun.security.ssl.allowLegacyHelloMessages = {}", allowLegacyHelloMessages.toString)
  }

  def configureCheckRevocation(checkRevocation: Boolean) {
    // http://docs.oracle.com/javase/6/docs/technotes/guides/security/certpath/CertPathProgGuide.html#AppC
    // https://blogs.oracle.com/xuelei/entry/enable_ocsp_checking
    System.setProperty("ocsp.enable", checkRevocation.toString)
    logger.debug("configureCheckRevocation: ocsp.enable = {}", checkRevocation.toString)

    System.setProperty("com.sun.security.enableCRLDP", checkRevocation.toString)
    logger.debug("configureCheckRevocation: com.sun.security.enableCRLDP = {}", checkRevocation.toString)

    // 1.7: Sets up sun.security.validator.PKIXValidator, which then sets up PKIXBuilderParameters.
    // 1.6: Used by sun.security.ssl.X509TrustManagerImpl
    System.setProperty("com.sun.net.ssl.checkRevocation", checkRevocation.toString)
    logger.debug("configureCheckRevocation: com.sun.net.ssl.checkRevocation = {}", checkRevocation.toString)
  }

  /**
   * For use in testing.
   */
  def clearProperties() {
    System.clearProperty("ocsp.enable")
    System.clearProperty("com.sun.security.enableCRLDP")
    System.clearProperty("com.sun.net.ssl.checkRevocation")

    System.clearProperty("sun.security.ssl.allowLegacyHelloMessages")
    System.clearProperty("sun.security.ssl.allowUnsafeRenegotiation")
  }
}

