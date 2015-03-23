/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
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
import scala.util.Properties.isJavaAtLeast

/**
 * Used for testing only.  This relies on internal sun.security packages, so cannot be used in OpenJDK.
 */
object CertificateGenerator {

  // http://docs.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html#KeyPairGenerator
  // http://www.keylength.com/en/4/

  /**
   * Generates a certificate using RSA (which is available in 1.6).
   */
  def generateRSAWithSHA256(keySize: Int = 2048, from: Instant = Instant.now, duration: Int = 5000000): X509Certificate = {
    val dn = "CN=localhost, OU=Unit Testing, O=Mavericks, L=Moon Base 1, ST=Cyberspace, C=CY"
    val to = from.plus(duration)

    val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(keySize, new SecureRandom())
    val pair = keyGen.generateKeyPair()
    generateCertificate(dn, pair, from.toDate, to.toDate, "SHA256withRSA", AlgorithmId.sha256WithRSAEncryption_oid)
  }

  def toPEM(certificate: X509Certificate) = {
    import org.apache.commons.codec.binary.Base64
    val encoder = new Base64(64)
    val certBegin = "-----BEGIN CERTIFICATE-----\n"
    val certEnd = "-----END CERTIFICATE-----"

    val derCert = certificate.getEncoded()
    val pemCertPre = new String(encoder.encode(derCert), "UTF-8")
    val pemCert = certBegin + pemCertPre + certEnd
    pemCert
  }

  def generateRSAWithMD5(keySize: Int = 2048, from: Instant = Instant.now, duration: Int = 5000000): X509Certificate = {
    val dn = "CN=localhost, OU=Unit Testing, O=Mavericks, L=Moon Base 1, ST=Cyberspace, C=CY"
    val to = from.plus(duration)

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

    // Note: CertificateSubjectName and CertificateIssuerName are removed in Java 8
    // and when setting the subject or issuer just the X500Name should be used.
    val justName = isJavaAtLeast("1.8")

    info.set(X509CertInfo.VALIDITY, interval)
    info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn))
    info.set(X509CertInfo.SUBJECT, if (justName) owner else new CertificateSubjectName(owner))
    info.set(X509CertInfo.ISSUER, if (justName) owner else new CertificateIssuerName(owner))
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
