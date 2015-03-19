/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import play.api.libs.ws.WSClientConfig
import java.security.Security

/**
 * Configures global system properties on the JSSE implementation, if defined.
 *
 * WARNING: This class sets system properties to configure JSSE code which typically uses static initialization on
 * load.  Because of this, if classes are loaded in BEFORE this code has a chance to operate, you may find that this
 * code works inconsistently.  The solution is to set the system properties on the command line explicitly (or in the
 * case of "ocsp.enable", in the security property file).
 */
class SystemConfiguration {

  val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def configure(config: WSClientConfig) {

    config.ssl.loose.allowUnsafeRenegotiation.map(configureUnsafeRenegotiation)
    config.ssl.loose.allowLegacyHelloMessages.map(configureAllowLegacyHelloMessages)
    config.ssl.checkRevocation.map(configureCheckRevocation)
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

    // 1.7: PXIXCertPathValidator.populateVariables, it is dynamic so no override needed.
    Security.setProperty("ocsp.enable", checkRevocation.toString)
    logger.debug("configureCheckRevocation: ocsp.enable = {}", checkRevocation.toString)
    System.setProperty("com.sun.security.enableCRLDP", checkRevocation.toString)
    logger.debug("configureCheckRevocation: com.sun.security.enableCRLDP = {}", checkRevocation.toString)
    System.setProperty("com.sun.net.ssl.checkRevocation", checkRevocation.toString)
  }

  /**
   * For use in testing.
   */
  def clearProperties() {
    Security.setProperty("ocsp.enable", "false")
    System.clearProperty("com.sun.security.enableCRLDP")
    System.clearProperty("com.sun.net.ssl.checkRevocation")

    System.clearProperty("sun.security.ssl.allowLegacyHelloMessages")
    System.clearProperty("sun.security.ssl.allowUnsafeRenegotiation")
  }
}

