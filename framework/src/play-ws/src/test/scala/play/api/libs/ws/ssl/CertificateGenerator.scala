/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import sun.security.x509._
import java.security.cert._
import java.security._
import java.math.BigInteger
import java.util.Date
import sun.security.util.ObjectIdentifier
import org.joda.time.Instant

/**
 * Used for testing only.  This relies on internal sun.security packages, so cannot be used in OpenJDK.
 */
object CertificateGenerator {

  // The FakeKeyStore implementation...
  //  def generateSelfSignedCertificate(DnName:String, keyPair: KeyPair): X509Certificate = {
  //    val certInfo = new X509CertInfo()
  //
  //    // Serial number and version
  //    certInfo.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(new BigInteger(64, new SecureRandom())))
  //    certInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3))
  //
  //    // Validity
  //    val validFrom = new Date()
  //    val validTo = new Date(validFrom.getTime + 50l * 365l * 24l * 60l * 60l * 1000l)
  //    val validity = new CertificateValidity(validFrom, validTo)
  //    certInfo.set(X509CertInfo.VALIDITY, validity)
  //
  //    // Subject and issuer
  //    val owner = new X500Name(DnName)
  //    certInfo.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner))
  //    certInfo.set(X509CertInfo.ISSUER, new CertificateIssuerName(owner))
  //
  //    // Key and algorithm
  //    certInfo.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic))
  //    val algorithm = new AlgorithmId(AlgorithmId.sha1WithRSAEncryption_oid)
  //    certInfo.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algorithm))
  //
  //    // Create a new certificate and sign it
  //    val cert = new X509CertImpl(certInfo)
  //    cert.sign(keyPair.getPrivate, "SHA1withRSA")
  //
  //    // Since the SHA1withRSA provider may have a different algorithm ID to what we think it should be,
  //    // we need to reset the algorithm ID, and resign the certificate
  //    val actualAlgorithm = cert.get(X509CertImpl.SIG_ALG).asInstanceOf[AlgorithmId]
  //    certInfo.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, actualAlgorithm)
  //    val newCert = new X509CertImpl(certInfo)
  //    newCert.sign(keyPair.getPrivate, "SHA1withRSA")
  //    newCert
  //  }

  // http://docs.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html#KeyPairGenerator
  // http://www.keylength.com/en/4/

  /**
   * Generates a certificate using RSA (which is available in 1.6).
   */
  def generateRSAWithSHA256(keySize:Int = 2048) : X509Certificate = {
    val dn = "CN=localhost, OU=Unit Testing, O=Mavericks, L=Moon Base 1, ST=Cyberspace, C=CY"
    val from = Instant.now
    val to = from.plus(5000000)

    val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(keySize, new SecureRandom())
    val pair = keyGen.generateKeyPair()
    generateCertificate(dn, pair, from.toDate, to.toDate, "SHA256WithRSA", AlgorithmId.sha256WithRSAEncryption_oid)
  }

  def generateRSAWithMD5(keySize:Int = 2048) : X509Certificate = {
    val dn = "CN=localhost, OU=Unit Testing, O=Mavericks, L=Moon Base 1, ST=Cyberspace, C=CY"
    val from = Instant.now
    val to = from.plus(5000000)

    val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(keySize, new SecureRandom())
    val pair = keyGen.generateKeyPair()
    generateCertificate(dn, pair, from.toDate, to.toDate, "MD5WithRSA", AlgorithmId.md5WithRSAEncryption_oid)
  }

  private[play] def generateCertificate(dn: String, pair: KeyPair, from: Date, to: Date, algorithm: String, oid: ObjectIdentifier): X509Certificate = {

    val info: X509CertInfo = new X509CertInfo
    val interval: CertificateValidity = new CertificateValidity(from, to)
    // I have no idea why 64 bits specifically are used for the certificate serial number.
    val sn: BigInteger = new BigInteger(64, new SecureRandom)
    val owner: X500Name = new X500Name(dn)

    info.set(X509CertInfo.VALIDITY, interval)
    info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn))
    info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner))
    info.set(X509CertInfo.ISSUER, new CertificateIssuerName(owner))
    info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic))
    info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3))

    var algo: AlgorithmId = new AlgorithmId(oid)

    info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo))
    var cert: X509CertImpl = new X509CertImpl(info)
    val privkey: PrivateKey = pair.getPrivate
    cert.sign(privkey, algorithm)
    algo = cert.get(X509CertImpl.SIG_ALG).asInstanceOf[AlgorithmId]
    info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo)
    cert = new X509CertImpl(info)
    cert.sign(privkey, algorithm)
    cert
  }
}