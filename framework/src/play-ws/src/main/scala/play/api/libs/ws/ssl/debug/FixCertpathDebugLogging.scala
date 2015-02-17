/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl.debug

import java.security.AccessController
import scala.util.control.NonFatal
import sun.security.util.Debug

/**
 * This singleton object turns on "certpath" debug logging (originally based off the "java.security.debug" debug flag),
 * and swaps out references to internal Sun JSSE classes to ensure that the new debug logging settings are honored, and
 * that debugging can be turned on dynamically, even after static class block initialization has been completed.  It
 * does this using some {{sun.misc.Unsafe}} black magic.
 *
 * Note that currently the only functionality is to turn debug output ON, with the assumption that all output will
 * go to an appropriately configured logger that can ignore calls to it.  There is no "off" method.
 */
object FixCertpathDebugLogging {

  val logger = org.slf4j.LoggerFactory.getLogger("play.api.libs.ws.ssl.debug.FixCertpathDebugLogging")

  class MonkeyPatchSunSecurityUtilDebugAction(val newDebug: Debug, val newOptions: String) extends FixLoggingAction {
    val logger = org.slf4j.LoggerFactory.getLogger("play.api.libs.ws.ssl.debug.FixCertpathDebugLogging.MonkeyPatchSunSecurityUtilDebugAction")

    val initialResource = "/sun/security/provider/certpath/Builder.class"

    val debugType = classOf[Debug]

    /**
     * Returns true if this class has an instance of {{Debug.getInstance("certpath")}}, false otherwise.
     *
     * @param className the name of the class.
     * @return true if this class should be returned in the set of findClasses, false otherwise.
     */
    def isValidClass(className: String): Boolean = {
      if (className.startsWith("java.security.cert")) return true
      if (className.startsWith("sun.security.provider.certpath")) return true
      if (className.equals("sun.security.x509.InhibitAnyPolicyExtension")) return true
      false
    }

    /**
     * Returns true if the new options contains certpath, false otherwise.  If it does not contain certpath,
     * then set the fields to null (which will disable logging).
     *
     * @return
     */
    def isUsingDebug: Boolean = (newOptions != null) && newOptions.contains("certpath")

    def run() {
      System.setProperty("java.security.debug", newOptions)

      logger.debug(s"run: debugType = $debugType")

      val debugValue = if (isUsingDebug) newDebug else null
      var isPatched = false
      for (
        debugClass <- findClasses;
        debugField <- debugClass.getDeclaredFields
      ) {
        if (isValidField(debugField, debugType)) {
          logger.debug(s"run: Patching $debugClass with $debugValue")
          monkeyPatchField(debugField, debugValue)
          isPatched = true
        }
      }

      // Add an assertion here in case the class location changes, so the tests fail...
      if (!isPatched) {
        throw new IllegalStateException("No debug classes found!")
      }

      // Switch out the args (for certpath loggers that AREN'T static and final)
      // This will result in those classes using the base Debug class which will write to System.out, but
      // I don't know how to switch out the Debug.getInstance method itself without using a java agent.
      val argsField = debugType.getDeclaredField("args")
      monkeyPatchField(argsField, newOptions)
    }
  }

  /**
   * Extends {{sun.security.util.Debug}} to delegate println to a logger.
   *
   * @param logger the logger which will receive debug calls.
   */
  class SunSecurityUtilDebugLogger(logger: org.slf4j.Logger) extends sun.security.util.Debug {
    override def println(message: String) {
      if (logger.isDebugEnabled) {
        logger.debug(message)
      }
    }

    override def println() {
      if (logger.isDebugEnabled) {
        logger.debug("")
      }
    }
  }

  def apply(newOptions: String, debugOption: Option[Debug] = None) {
    logger.trace(s"apply: newOptions = $newOptions, debugOption = $debugOption")
    try {
      val newDebug = debugOption match {
        case Some(d) => d
        case None => new Debug()
      }
      val action = new MonkeyPatchSunSecurityUtilDebugAction(newDebug, newOptions)
      AccessController.doPrivileged(action)
    } catch {
      case NonFatal(e) =>
        throw new IllegalStateException("CertificateDebug configuration error", e)
    }
  }
}
