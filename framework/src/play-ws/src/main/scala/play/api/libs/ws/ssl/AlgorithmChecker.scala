/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.ws.ssl

import java.security.cert._

import scala.collection.JavaConverters._
import javax.naming.ldap.{ Rdn, LdapName }
import javax.naming.InvalidNameException

/**
 * Looks for disabled algorithms in the certificate.  This is because some certificates are signed with
 * forgable hashes such as MD2 or MD5, so we can't be certain of their authenticity.
 *
 * This class is needed because the JDK 1.6 Algorithm checker doesn't give us any way to customize the list of
 * disabled algorithms, and we need to be able to support that.
 *
 * Also note that we need to check the trust anchor for disabled key sizes, and the CertPath explicitly removes
 * the trust anchor from the chain of certificates.  This means we need to check the trust anchor explicitly in the
 * through the CompositeTrustManager.
 */
class AlgorithmChecker(val signatureConstraints: Set[AlgorithmConstraint], val keyConstraints: Set[AlgorithmConstraint]) extends PKIXCertPathChecker {

  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  private val signatureConstraintsMap: Map[String, AlgorithmConstraint] = {
    for (c <- signatureConstraints.iterator) yield {
      c.algorithm -> c
    }
  }.toMap

  private val keyConstraintsMap: Map[String, AlgorithmConstraint] = {
    for (c <- keyConstraints.iterator) yield {
      c.algorithm -> c
    }
  }.toMap

  def isForwardCheckingSupported: Boolean = false

  def getSupportedExtensions: java.util.Set[String] = java.util.Collections.emptySet()

  def init(forward: Boolean) {
    logger.debug(s"init: forward = $forward")
    // forward is from target to most-trusted CA
    // backwards is from most-trusted CA to target, which means we get the root CA first.
    if (forward) {
      throw new CertPathValidatorException("Forward checking not supported")
    }
  }

  def findSignatureConstraint(algorithm: String): Option[AlgorithmConstraint] = {
    signatureConstraintsMap.get(algorithm)
  }

  def findKeyConstraint(algorithm: String): Option[AlgorithmConstraint] = {
    keyConstraintsMap.get(algorithm)
  }

  /**
   * Checks for signature algorithms in the certificate and throws CertPathValidatorException if matched.
   * @param x509Cert
   */
  def checkSignatureAlgorithms(x509Cert: X509Certificate): Unit = {
    val sigAlgName = x509Cert.getSigAlgName
    val sigAlgorithms = Algorithms.decomposes(sigAlgName)

    logger.debug(s"checkSignatureAlgorithms: sigAlgName = $sigAlgName, sigAlgName = $sigAlgName, sigAlgorithms = $sigAlgorithms")

    for (a <- sigAlgorithms) {
      findSignatureConstraint(a).map {
        constraint =>
          if (constraint.matches(a)) {
            logger.debug(s"checkSignatureAlgorithms: x509Cert = $x509Cert failed on constraint $constraint")
            val msg = s"Certificate failed: $a matched constraint $constraint"
            throw new CertPathValidatorException(msg)
          }
      }
    }
  }

  /**
   * Checks for key algorithms in the certificate and throws CertPathValidatorException if matched.
   * @param x509Cert
   */
  def checkKeyAlgorithms(x509Cert: X509Certificate): Unit = {
    val key = x509Cert.getPublicKey
    val keyAlgorithmName = key.getAlgorithm
    val keySize = Algorithms.keySize(key).getOrElse(throw new IllegalStateException(s"No keySize found for $key"))

    val keyAlgorithms = Algorithms.decomposes(keyAlgorithmName)
    logger.debug(s"checkKeyAlgorithms: keyAlgorithmName = $keyAlgorithmName, keySize = $keySize, keyAlgorithms = $keyAlgorithms")

    for (a <- keyAlgorithms) {
      findKeyConstraint(a).map {
        constraint =>
          if (constraint.matches(a, keySize)) {
            val certName = x509Cert.getSubjectX500Principal.getName
            logger.debug(s"""checkKeyAlgorithms: cert = "certName" failed on constraint $constraint, algorithm = $a, keySize = $keySize""")

            val msg = s"""Certificate failed: cert = "$certName" failed on constraint $constraint, algorithm = $a, keySize = $keySize"""
            throw new CertPathValidatorException(msg)
          }
      }
    }
  }

  /**
   * Checks the algorithms in the given certificate.  Note that this implementation skips signature checking in a
   * root certificate, as a trusted root cert by definition is in the trust store and doesn't need to be signed.
   */
  def check(cert: Certificate, unresolvedCritExts: java.util.Collection[String]) {
    cert match {
      case x509Cert: X509Certificate =>

        val commonName = getCommonName(x509Cert)
        val subAltNames = x509Cert.getSubjectAlternativeNames
        val certName = x509Cert.getSubjectX500Principal.getName
        logger.debug(s"check: checking certificate commonName = $commonName, subjAltName = $subAltNames, certName = $certName")

        checkSignatureAlgorithms(x509Cert)
        checkKeyAlgorithms(x509Cert)
      case _ =>
        throw new UnsupportedOperationException("check only works with x509 certificates!")
    }
  }

  /**
   * Useful way to get certificate info without getting spammed with data.
   */
  def getCommonName(cert: X509Certificate) = {
    // http://stackoverflow.com/a/18174689/5266
    try {
      val ldapName = new LdapName(cert.getSubjectX500Principal.getName)
      /*
       * Looking for the "most specific CN" (i.e. the last).
       */
      var cn: String = null
      for (rdn: Rdn <- ldapName.getRdns.asScala) {
        if ("CN".equalsIgnoreCase(rdn.getType)) {
          cn = rdn.getValue.toString
        }
      }
      cn
    } catch {
      case e: InvalidNameException =>
        null
    }
  }

}
