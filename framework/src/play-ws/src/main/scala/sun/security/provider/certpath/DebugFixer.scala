/*
 *
 *  * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package sun.security.provider.certpath

class DebugFixer {

  val logger = org.slf4j.LoggerFactory.getLogger("certpath")

  private val checkers : Seq[Class[_]] = Seq(
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

  private val newDebug = new sun.security.util.Debug() {
    override def println(message: String) {
      //System.out.println(message)
      if (logger.isDebugEnabled) {
        logger.debug(message)
      }
    }

    override def println() {
      //System.out.println()
      if (logger.isDebugEnabled) {
        logger.debug("")
      }
    }
  }

  private val unsafe: sun.misc.Unsafe = {
    val field = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe")
    field.setAccessible(true)
    field.get(null).asInstanceOf[sun.misc.Unsafe]
  }

  def substituteDebug() {
    checkers.map {
      debugClass =>
        val argsField = debugClass.getDeclaredField("debug")
        setNewDebug(argsField)
    }

    setNewDebug(Class.forName("sun.security.provider.certpath.OCSPResponse").getDeclaredField("DEBUG"))
    setNewDebug(Class.forName("sun.security.provider.certpath.OCSPChecker").getDeclaredField("DEBUG"))
  }

  private def setNewDebug(argsField: java.lang.reflect.Field) {
    val base = unsafe.staticFieldBase(argsField)
    val offset = unsafe.staticFieldOffset(argsField)
    unsafe.putObject(base, offset, newDebug)
  }
}
