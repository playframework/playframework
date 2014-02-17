/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import play.api.libs.ws.WSClientConfig
import sun.security.provider.certpath.DebugFixer

class SystemProperties {

  val logger = org.slf4j.LoggerFactory.getLogger(getClass)

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

  /**
   * Configures global system properties on the JSSE implementation, if defined.
   */
  def configureSystemProperties(config: WSClientConfig) {
    config.ssl.map { _.debug.map(configureDebug) }

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

    JavaSecurityDebugProperties(securityOptions)
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

  // Because java.security.debug goes through sun.security.util.Debug, and the
  // initialization is done in a static block to a private static final field,
  // if we want to change the debug option after the program has already started
  // then we have to do it through super evil unsafe field swapping.
  object JavaSecurityDebugProperties {

    private val unsafe : sun.misc.Unsafe = {
      val field = classOf[sun.misc.Unsafe].getDeclaredField("theUnsafe")
      field.setAccessible(true)
      field.get(null).asInstanceOf[sun.misc.Unsafe]
    }

    def apply(options:String) {
      // turn on the debug logger if it's in the string
      if (options.contains("certpath")) {
        val certpathLogger = org.slf4j.LoggerFactory.getLogger("certpath").asInstanceOf[ch.qos.logback.classic.Logger]
        certpathLogger.setLevel(ch.qos.logback.classic.Level.DEBUG)
      }

      // Switch out the args (for loggers that aren't static and final)
      val argsField = classOf[sun.security.util.Debug].getDeclaredField("args")
      val base = unsafe.staticFieldBase(argsField)
      val offset = unsafe.staticFieldOffset(argsField)
      unsafe.putObject(base, offset, options)

      // Then switch out the Debug references to use a subclassed version that will use the logger.
      val fixer = new DebugFixer()
      fixer.substituteDebug()
    }

  }
}

