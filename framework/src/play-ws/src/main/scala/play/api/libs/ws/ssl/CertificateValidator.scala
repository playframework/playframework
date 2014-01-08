/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.ws.ssl

import java.security.cert._

import scala.collection.JavaConverters._

/**
 * Define a certificate validator with our own custom checkers and builders.
 */
class CertificateValidator(val constraints: Set[AlgorithmConstraint], val revocationEnabled: Boolean) {
  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  // Add the algorithm checker in here...
  val checkers: Seq[PKIXCertPathChecker] = Seq(
    new AlgorithmChecker(constraints)
  )

  logger.debug(s"constructor: constraints = $constraints, revocationEnabled = $revocationEnabled")

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
    logger.debug(s"validate: chain = $chain, trustedCerts = $trustedCerts")

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
    val anchors = for { cert <- certs } yield { new TrustAnchor(cert, nameConstraints.orNull) }
    anchors.toSet
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
class AlgorithmChecker(val constraints: Set[AlgorithmConstraint]) extends PKIXCertPathChecker {

  val constraintsMap: Map[String, AlgorithmConstraint] = {
    for (c <- constraints.iterator) yield {
      c.algorithm -> c
    }
  }.toMap

  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def isForwardCheckingSupported: Boolean = false

  def getSupportedExtensions: java.util.Set[String] = java.util.Collections.emptySet()

  def init(forward: Boolean) {
    // do nothing
  }

  def findConstraint(algorithm: String): Option[AlgorithmConstraint] = {
    constraintsMap.get(algorithm)
  }

  /**
   * Checks the signature algorithm of the specified certificate.
   */
  def check(cert: Certificate, unresolvedCritExts: java.util.Collection[String]) {
    cert match {
      case x509Cert: X509Certificate =>
        logger.debug(s"check: cert = ${x509Cert.getSubjectX500Principal.getName()}, unresolvedCritExts = $unresolvedCritExts")

        val key = x509Cert.getPublicKey
        val keySize = Algorithms.keySize(key)

        val algName = x509Cert.getSigAlgName
        val algorithms = Algorithms.decomposes(algName)

        logger.debug(s"check: algName = $algName, algorithms = $algorithms, keySize = $keySize")
        for (a <- algorithms) {
          findConstraint(a).map {
            constraint =>
              if (constraint.matches(a, keySize)) {
                //logger.debug(s"check: cert = $cert failed on constraint $constraint")
                //println(s"check: cert = $cert failed on constraint $constraint")
                val msg = s"Certificate failed: $a with $keySize matched constraint $constraint"
                throw new CertPathValidatorException(msg)
              }
          }
        }
      case _ =>
        throw new UnsupportedOperationException("check only works with x509 certificates")
    }
  }

}
