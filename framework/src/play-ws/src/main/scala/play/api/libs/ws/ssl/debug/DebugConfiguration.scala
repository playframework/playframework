/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl.debug

import play.api.libs.ws.ssl.{ JavaxNetDebugBuilder, JavaSecurityDebugBuilder, SSLDebugConfig }
import play.api.libs.ws.ssl.debug.FixCertpathDebugLogging.SunSecurityUtilDebugLogger

class DebugConfiguration {

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  def configure(d: SSLDebugConfig) {
    configureJavaxNetDebug(d)
    configureJavaSecurityDebug(d)
  }

  def configureJavaxNetDebug(d: SSLDebugConfig) {
    val netDebugOptions = new JavaxNetDebugBuilder(d).build()
    logger.debug(s"configureJavaxNetDebug: d = $d, netDebugOptions = $netDebugOptions")
    FixInternalDebugLogging(netDebugOptions)
  }

  def configureJavaSecurityDebug(d: SSLDebugConfig) {
    val securityOptions = new JavaSecurityDebugBuilder(d).build()
    logger.debug(s"configureJavaSecurityDebug: d = $d, securityOptions = $securityOptions")
    System.setProperty("java.security.debug", securityOptions)
    FixCertpathDebugLogging(securityOptions)
  }

  //  val certpathLogger = org.slf4j.LoggerFactory.getLogger("java.security.debug")
  //  val newDebug = new SunSecurityUtilDebugLogger(certpathLogger)
  //
  //  private def logging(slf4jLogger: org.slf4j.Logger): Option[org.slf4j.Logger] = {
  //    val logbackLogger = slf4jLogger.asInstanceOf[ch.qos.logback.classic.Logger]
  //    if (logbackLogger.isDebugEnabled) Some(slf4jLogger) else None
  //  }
  //
  //  private def setLoggerDebug(slf4jLogger: org.slf4j.Logger) {
  //    val logbackLogger = slf4jLogger.asInstanceOf[ch.qos.logback.classic.Logger]
  //    logbackLogger.setLevel(ch.qos.logback.classic.Level.DEBUG)
  //  }

}
