/*
 *
 *  * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import java.security.{PrivilegedExceptionAction, AccessController}
import scala.util.control.NonFatal


class SunSecurityDebugLogger(logger: org.slf4j.Logger) extends sun.security.util.Debug {

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

object CertificateDebug {

  private val unsafe: sun.misc.Unsafe = {
    val field = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe")
    field.setAccessible(true)
    field.get(null).asInstanceOf[sun.misc.Unsafe]
  }

  def apply(newDebug:sun.security.util.Debug) {
    try {
      AccessController.doPrivileged(
        new PrivilegedExceptionAction[Unit]() {
          private def setNewDebug(debugClass: Class[_], fieldName: String) {
            val debugField = debugClass.getDeclaredField(fieldName)
            val base = unsafe.staticFieldBase(debugField)
            val offset = unsafe.staticFieldOffset(debugField)
            unsafe.putObject(base, offset, newDebug)
          }

          def run() {
            // Switch out the args (for certpath loggers that AREN'T static and final)
            // This will result in those classes using the base Debug class which will write to System.out, but
            // I don't know how to switch out the Debug.getInstance call without using a java agent.
            val argsField = classOf[sun.security.util.Debug].getDeclaredField("args")
            val base = unsafe.staticFieldBase(argsField)
            val offset = unsafe.staticFieldOffset(argsField)
            unsafe.putObject(base, offset, "certpath")

            val certPathDebugClasses: Seq[Class[_]] = Seq(
              Class.forName("java.security.cert.CertPathBuilder"),
              Class.forName("java.security.cert.CertPathValidator"),
              Class.forName("java.security.cert.X509CertSelector"),
              Class.forName("java.security.cert.X509CRLSelector"),
              Class.forName("sun.security.provider.certpath.BasicChecker"),
              Class.forName("sun.security.provider.certpath.Builder"),
              Class.forName("sun.security.provider.certpath.BuildStep"),
              Class.forName("sun.security.provider.certpath.ConstraintsChecker"),
              Class.forName("sun.security.provider.certpath.CrlRevocationChecker"),
              Class.forName("sun.security.provider.certpath.DistributionPointFetcher"),
              Class.forName("sun.security.provider.certpath.ForwardBuilder"),
              Class.forName("sun.security.provider.certpath.ForwardState"),
              Class.forName("sun.security.provider.certpath.KeyChecker"),
              //Class.forName(LDAPCertStore],
              Class.forName("sun.security.provider.certpath.OCSP"),
              Class.forName("sun.security.provider.certpath.OCSPRequest"),
              Class.forName("sun.security.provider.certpath.PKIXCertPathValidator"),
              Class.forName("sun.security.provider.certpath.PKIXMasterCertPathValidator"),
              Class.forName("sun.security.provider.certpath.PolicyChecker"),
              Class.forName("sun.security.provider.certpath.ReverseState"),
              Class.forName("sun.security.provider.certpath.SunCertPathBuilder"),
              Class.forName("sun.security.provider.certpath.SunCertPathBuilderResult"),
              Class.forName("sun.security.provider.certpath.UntrustedChecker"),
              Class.forName("sun.security.provider.certpath.URICertStore"),
              Class.forName("sun.security.provider.certpath.Vertex"),
              Class.forName("sun.security.x509.InhibitAnyPolicyExtension")
            )

            certPathDebugClasses.map {
              debugClass =>
                setNewDebug(debugClass, "debug")
            }

            setNewDebug(Class.forName("sun.security.provider.certpath.OCSPResponse"), "DEBUG")
            setNewDebug(Class.forName("sun.security.provider.certpath.OCSPChecker"), "DEBUG")
          }
        })
    } catch {
      case NonFatal(e) =>
        throw new IllegalStateException("CertificateDebug configuration error", e)
    }
  }


}
