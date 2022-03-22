/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.ssl

import sun.security.x509._
import java.security.cert._
import java.security._
import java.math.BigInteger
import java.util.Date
import java.time.Duration
import java.time.Instant

/**
 * Used for testing only.  This relies on internal sun.security packages, so cannot be used in OpenJDK.
 */
object CertificateGenerator {
  // http://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#KeyPairGenerator
  // http://www.keylength.com/en/4/

  /**
   * Generates a certificate using RSA (which is available in 1.6).
   */
  def generateRSAWithSHA256(
      keySize: Int = 2048,
      from: Instant = Instant.now,
      duration: Duration = Duration.ofDays(365)
  ): X509Certificate = {
    val dn = "CN=localhost, OU=Unit Testing, O=Mavericks, L=Play Base 1, ST=Cyberspace, C=CY"
    val to = from.plus(duration)

    val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(keySize, new SecureRandom())
    val pair = keyGen.generateKeyPair()
    generateCertificate(
      dn,
      pair,
      Date.from(from),
      Date.from(to),
      "SHA256withRSA",
      AlgorithmId.get("SHA256WithRSA")
    )
  }

  def generateRSAWithSHA1(
      keySize: Int = 2048,
      from: Instant = Instant.now,
      duration: Duration = Duration.ofDays(365)
  ): X509Certificate = {
    val dn = "CN=localhost, OU=Unit Testing, O=Mavericks, L=Play Base 1, ST=Cyberspace, C=CY"
    val to = from.plus(duration)

    val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(keySize, new SecureRandom())
    val pair = keyGen.generateKeyPair()
    generateCertificate(
      dn,
      pair,
      Date.from(from),
      Date.from(to),
      "SHA1withRSA",
      AlgorithmId.get("SHA256WithRSA")
    )
  }

  def toPEM(certificate: X509Certificate) = {
    val encoder   = java.util.Base64.getMimeEncoder(64, Array('\r', '\n'))
    val certBegin = "-----BEGIN CERTIFICATE-----\n"
    val certEnd   = "-----END CERTIFICATE-----"

    val derCert    = certificate.getEncoded()
    val pemCertPre = new String(encoder.encode(derCert), "UTF-8")
    val pemCert    = certBegin + pemCertPre + certEnd
    pemCert
  }

  def generateRSAWithMD5(
      keySize: Int = 2048,
      from: Instant = Instant.now,
      duration: Duration = Duration.ofDays(365)
  ): X509Certificate = {
    val dn = "CN=localhost, OU=Unit Testing, O=Mavericks, L=Play Base 1, ST=Cyberspace, C=CY"
    val to = from.plus(duration)

    val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(keySize, new SecureRandom())
    val pair = keyGen.generateKeyPair()
    generateCertificate(dn, pair, Date.from(from), Date.from(to), "MD5WithRSA", AlgorithmId.get("MD5WithRSA"))
  }

  private[play] def generateCertificate(
      dn: String,
      pair: KeyPair,
      from: Date,
      to: Date,
      algorithm: String,
      algoId: AlgorithmId
  ): X509Certificate = {
    val info: X509CertInfo            = new X509CertInfo
    val interval: CertificateValidity = new CertificateValidity(from, to)
    // I have no idea why 64 bits specifically are used for the certificate serial number.
    val sn: BigInteger  = new BigInteger(64, new SecureRandom)
    val owner: X500Name = new X500Name(dn)

    info.set(X509CertInfo.VALIDITY, interval)
    info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn))
    info.set(X509CertInfo.SUBJECT, owner)
    info.set(X509CertInfo.ISSUER, owner)
    info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic))
    info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3))

    info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algoId))
    var cert: X509CertImpl  = new X509CertImpl(info)
    val privkey: PrivateKey = pair.getPrivate
    cert.sign(privkey, algorithm)
    val algo = cert.get(X509CertImpl.SIG_ALG).asInstanceOf[AlgorithmId]
    info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo)
    cert = new X509CertImpl(info)
    cert.sign(privkey, algorithm)
    cert
  }
}
