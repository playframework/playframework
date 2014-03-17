/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import play.api.libs.ws.WSClientConfig
import java.security.{ Security, PrivilegedExceptionAction }

/**
 * Configures global system properties on the JSSE implementation, if defined.
 */
class SystemConfiguration extends MonkeyPatcher {

  val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def configure(config: WSClientConfig) {

    config.ssl.map {
      ssl =>
        ssl.loose.map {
          loose =>
            loose.allowUnsafeRenegotiation.map(configureUnsafeRenegotiation)
            loose.allowLegacyHelloMessages.map(configureAllowLegacyHelloMessages)
        }
        ssl.checkRevocation.map(configureCheckRevocation)
    }
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
    val javaBool: java.lang.Boolean = checkRevocation

    // 1.7: PXIXCertPathValidator.populateVariables, it is dynamic so no override needed.
    Security.setProperty("ocsp.enable", checkRevocation.toString)
    logger.debug("configureCheckRevocation: ocsp.enable = {}", checkRevocation.toString)

    // JDK 1.6 & 1.7 are the same
    java.security.AccessController.doPrivileged(
      new PrivilegedExceptionAction[Unit] {
        override def run(): Unit = {
          // CRL checking
          System.setProperty("com.sun.security.enableCRLDP", checkRevocation.toString)
          logger.debug("configureCheckRevocation: com.sun.security.enableCRLDP = {}", checkRevocation.toString)

          val className = "sun.security.provider.certpath.DistributionPointFetcher"
          val revocationClassType = Thread.currentThread().getContextClassLoader.loadClass(className)
          val revocationField = revocationClassType.getDeclaredField("USE_CRLDP")
          monkeyPatchField(revocationField, javaBool)
        }
      }
    )

    java.security.AccessController.doPrivileged(
      new PrivilegedExceptionAction[Unit] {
        override def run(): Unit = {
          System.setProperty("com.sun.net.ssl.checkRevocation", checkRevocation.toString)
          foldVersion(run16 = {
            // 1.6: Used by sun.security.ssl.X509TrustManagerImpl
            val className = "sun.security.ssl.X509TrustManagerImpl"
            val revocationClassType = Thread.currentThread().getContextClassLoader.loadClass(className)
            val revocationField = revocationClassType.getDeclaredField("checkRevocation")
            monkeyPatchField(revocationField, javaBool)
          }, runHigher = {
            // 1.7: Sets up sun.security.validator.PKIXValidator, which then sets up PKIXBuilderParameters.
            val className = "sun.security.validator.PKIXValidator"
            val revocationClassType = Thread.currentThread().getContextClassLoader.loadClass(className)
            val revocationField = revocationClassType.getDeclaredField("checkTLSRevocation")
            monkeyPatchField(revocationField, javaBool)
          })
          logger.debug("configureCheckRevocation: com.sun.net.ssl.checkRevocation = {}", checkRevocation.toString)
        }
      }
    )

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

