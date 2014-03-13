/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.ws.ssl

import java.security.cert._

import scala.collection.JavaConverters._
import javax.naming.ldap.{ Rdn, LdapName }
import javax.naming.InvalidNameException
import scala.util.control.NonFatal

/**
 * Define a certificate validator with our own custom checkers and builders.
 */
class CertificateValidator(val signatureConstraints: Set[AlgorithmConstraint], val keyConstraints: Set[AlgorithmConstraint], val revocationEnabled: Boolean = false) {
  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  // Add the algorithm checker in here...
  val checkers: Seq[PKIXCertPathChecker] = Seq(
    new AlgorithmChecker(signatureConstraints, keyConstraints)
  )

  logger.debug(s"constructor: signatureConstraints = $signatureConstraints, keyConstraints = $keyConstraints, revocationEnabled = $revocationEnabled")

  // This follows the model of the 1.6 sun.security.validator.PKIXValidator class, which also
  // manages a CertPathBuilder in the same way.  However... the PKIXValidator doesn't check for
  // deprecated algorithms and hardcodes revocation checking to be off.
  private val factory: CertificateFactory = CertificateFactory.getInstance("X.509")

  /**
   * Validates a yet to be trusted certificate chain, using the trust manager as the trusted source.
   */
  def validate(chain: Array[X509Certificate],
    trustedCerts: Traversable[X509Certificate],
    nameConstraints: Option[Array[Byte]] = None): PKIXCertPathValidatorResult = {
    logger.debug(s"validate: chain = ${debugChain(chain)}, trustedCerts = $trustedCerts")

    val trustAnchors = findTrustAnchors(trustedCerts, nameConstraints)
    val params = paramsFrom(trustAnchors, None, nameConstraints)
    val validator = CertPathValidator.getInstance("PKIX")
    val path = factory.generateCertPath(chain.toList.asJava)
    val result = validator.validate(path, params).asInstanceOf[PKIXCertPathValidatorResult]

    result
  }

  /**
   * Maps from the trust manager's accepted issuers to a set of trust anchors.
   */
  def findTrustAnchors(certs: Traversable[X509Certificate], nameConstraints: Option[Array[Byte]]): Set[TrustAnchor] = {
    certs.flatMap {
      cert =>
        try {
          // Believe it or not, the trust store doesn't check for expired root certificates:
          // https://stackoverflow.com/questions/5206859/java-trustmanager-behavior-on-expired-certificates
          // checkValidity will throw an exception, and we will filter out the anchors here.
          cert.checkValidity()

          Some(new TrustAnchor(cert, nameConstraints.orNull))
        } catch {
          case e: CertificateException =>
            logger.warn(s"Invalid root certificate ${cert}", e)
            None
          case NonFatal(ex) =>
            logger.error(s"Exception when creating trust anchor from certificate ${cert}", ex)
            None
        }
    }.toSet
  }

  /**
   * Initializes the builder parameters with the trust anchors and checkers.
   */
  def paramsFrom(trustAnchors: Set[TrustAnchor],
    certSelect: Option[X509CertSelector],
    nameConstraints: Option[Array[Byte]]): PKIXParameters = {
    val params = new PKIXBuilderParameters(trustAnchors.asJava, certSelect.orNull)

    // Use the custom cert path checkers we defined...
    params.setCertPathCheckers(checkers.asJava)

    // Set revocation based on whether or not it's enabled in this config...
    params.setRevocationEnabled(revocationEnabled)

    params
  }

}

/**
 * Looks for disabled algorithms in the certificate.  This is because some certificates are signed with
 * forgable hashes such as MD2 or MD5, so we can't be certain of their authenticity.
 *
 * This class is needed because the JDK 1.6 Algorithm checker doesn't give us any way to customize the list of
 * disabled algorithms, and we need to be able to support that.
 */
class AlgorithmChecker(val signatureConstraints: Set[AlgorithmConstraint], val keyConstraints: Set[AlgorithmConstraint]) extends PKIXCertPathChecker {

  // PKIXCertPathChecker is not thread-safe, so mutability is fine here.
  private var isRootCert: Boolean = true

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
    val keySize = Algorithms.keySize(key)
    val keyAlgorithms = Algorithms.decomposes(keyAlgorithmName)
    logger.debug(s"checkKeyAlgorithms: keyAlgorithmName = $keyAlgorithmName, keySize = $keySize, keyAlgorithms = $keyAlgorithms")

    for (a <- keyAlgorithms) {
      findKeyConstraint(a).map {
        constraint =>
          if (constraint.matches(a, keySize)) {
            logger.debug(s"checkKeyAlgorithms: cert = $x509Cert failed on constraint $constraint")

            val msg = s"Certificate failed: $a matched constraint $constraint"
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
        logger.debug(s"check: checking certificate commonName = $commonName, subjAltName = $subAltNames")

        if (isRootCert) {
          logger.debug(s"checkSignatureAlgorithms: skipping signature checks on trusted root certificate $commonName")
          isRootCert = false
        } else {
          checkSignatureAlgorithms(x509Cert)
        }

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
