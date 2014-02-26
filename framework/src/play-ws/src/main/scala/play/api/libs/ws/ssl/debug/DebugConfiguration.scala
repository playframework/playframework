/*
 *
 *  * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl.debug

import play.api.libs.ws.ssl.{JavaxNetDebugBuilder, JavaSecurityDebugBuilder, SSLDebugConfig}
import play.api.libs.ws.ssl.debug.FixCertpathDebugLogging.SunSecurityUtilDebugLogger

class DebugConfiguration {

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  def configure(d: SSLDebugConfig) {
    configureJavaxNetDebug(d)
    configureJavaSecurityDebug(d)
  }

  def configureJavaxNetDebug(d: SSLDebugConfig) {
    val netDebugOptions = new JavaxNetDebugBuilder(d).build()
    logger.debug(s"configureJavaxNetDebug: d = ${d}, netDebugOptions = ${netDebugOptions}")
    if (! netDebugOptions.trim.isEmpty) {
      System.setProperty("javax.net.debug", netDebugOptions)
      FixInternalDebugLogging(netDebugOptions)
    }
  }

  def configureJavaSecurityDebug(d: SSLDebugConfig) {
    val securityOptions = new JavaSecurityDebugBuilder(d).build()
    logger.debug(s"configureJavaSecurityDebug: d = ${d}, securityOptions = ${securityOptions}")
    if (! securityOptions.trim.isEmpty) {
      System.setProperty("java.security.debug", securityOptions)
      val certpathLogger = org.slf4j.LoggerFactory.getLogger("java.security.debug")
      val newDebug = new SunSecurityUtilDebugLogger(certpathLogger)
      FixCertpathDebugLogging(securityOptions, Some(newDebug))
    }
  }

  def logging(slf4jLogger: org.slf4j.Logger) : Option[org.slf4j.Logger] = {
    val logbackLogger = slf4jLogger.asInstanceOf[ch.qos.logback.classic.Logger]
    if (logbackLogger.isDebugEnabled) Some(slf4jLogger) else None
  }

  def setLoggerDebug(slf4jLogger: org.slf4j.Logger) {
    val logbackLogger = slf4jLogger.asInstanceOf[ch.qos.logback.classic.Logger]
    logbackLogger.setLevel(ch.qos.logback.classic.Level.DEBUG)
  }

}
