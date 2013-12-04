/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import play.api.libs.ws.WSClientConfig

class SystemProperties(config: WSClientConfig) {

  val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  /**
   * Configures global system properties on the JSSE implementation, if defined.
   */
  def configureSystemProperties() {
    config.ssl.map {
      ssl =>
        ssl.debug.map(configureDebug)

        // Turn on some secure defaults unless disabled.
        if (!ssl.off.getOrElse(false)) {
          val allowUnsafeRenegotiation = ssl.loose.flatMap(_.allowUnsafeRenegotiation).getOrElse(false)
          configureUnsafeRenegotiation(allowUnsafeRenegotiation)

          val allowLegacyHelloMessages = ssl.loose.flatMap(_.allowLegacyHelloMessages).getOrElse(false)
          configureAllowLegacyHelloMessages(allowLegacyHelloMessages)

          val checkRevocation = !ssl.loose.flatMap(_.disableCheckRevocation).getOrElse(false)
          configureCheckRevocation(checkRevocation)
        } else {
          logger.warn("configureSystemProperties: ws.ssl.off is true, disabling all system property configuration")
        }
    }
  }

  def configureDebug(d: SSLDebugConfig) {
    val netDebugOptions = new JavaxNetDebugBuilder(d).build()
    val securityOptions = new JavaSecurityDebugBuilder(d).build()

    if (Option(System.getProperty("javax.net.debug")).isDefined) {
      logger.warn("configureDebug: javax.net.debug system property is not empty, overriding anyway...")
    }

    System.setProperty("javax.net.debug", netDebugOptions)
    logger.debug("configureDebug: javax.net.debug = {}", netDebugOptions)

    if (Option(System.getProperty("java.security.debug")).isDefined) {
      logger.warn("configureDebug: java.security.debug system property is not empty, overriding anyway...")
    }

    System.setProperty("java.security.debug", securityOptions)
    logger.debug("configureDebug: java.security.debug = {}", securityOptions)
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
    if (checkRevocation) {
      // http://docs.oracle.com/javase/6/docs/technotes/guides/security/certpath/CertPathProgGuide.html#AppC
      // https://blogs.oracle.com/xuelei/entry/enable_ocsp_checking
      //
      // http://blogs.nologin.es/rickyepoderi/index.php?/archives/77-BUG-in-Java-OCSP-Implementation-PKIX.html
      // http://blogs.nologin.es/rickyepoderi/index.php?/archives/79-OCSP-Java-Bug-Part-II.html
      // http://blogs.nologin.es/rickyepoderi/index.php?/archives/81-OCSP-Java-Bug-Part-III.html
      // http://blogs.nologin.es/rickyepoderi/index.php?/archives/92-OCSP-Java-Bug-Final-Chapter.html
      //
      System.setProperty("ocsp.enable", "true")
      logger.debug("configureCheckRevocation: ocsp.enable = {}", "true")

      System.setProperty("com.sun.security.enableCRLDP", "true")
      logger.debug("configureCheckRevocation: com.sun.security.enableCRLDP = {}", "true")

      // 1.7: Sets up sun.security.validator.PKIXValidator, which then sets up PKIXBuilderParameters.
      // 1.6: Used by sun.security.ssl.X509TrustManagerImpl
      System.setProperty("com.sun.net.ssl.checkRevocation", "true")
      logger.debug("configureCheckRevocation: com.sun.net.ssl.checkRevocation = {}", "true")
    } else {
      logger.debug("configureCheckRevocation: checkRevocation = {}", checkRevocation.toString)
    }
  }
}

