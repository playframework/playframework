/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.ssl

import java.security.{ KeyPair, KeyPairGenerator, KeyStore, SecureRandom }

import sun.security.x509._
import java.util.Date
import java.math.BigInteger
import java.security.cert.X509Certificate
import java.io.File

import javax.net.ssl.KeyManagerFactory

import sun.security.util.ObjectIdentifier

import com.typesafe.sslconfig.{ ssl => sslconfig }
import com.typesafe.sslconfig.util.NoopLogger

/**
 * A fake key store
 */
@deprecated("Deprecated in favour of the FakeKeyStore in ssl-config", "2.7.0")
object FakeKeyStore {
  private final val FakeKeyStore = new sslconfig.FakeKeyStore(NoopLogger.factory())

  val GeneratedKeyStore: String = sslconfig.FakeKeyStore.GeneratedKeyStore
  val ExportedCert: String = sslconfig.FakeKeyStore.ExportedCert
  val TrustedAlias = sslconfig.FakeKeyStore.TrustedAlias
  val DistinguishedName = sslconfig.FakeKeyStore.DistinguishedName
  val SignatureAlgorithmName = sslconfig.FakeKeyStore.SignatureAlgorithmName
  val SignatureAlgorithmOID: ObjectIdentifier = sslconfig.FakeKeyStore.SignatureAlgorithmOID

  object CertificateAuthority {
    val ExportedCertificate = sslconfig.FakeKeyStore.CertificateAuthority.ExportedCertificate
    val TrustedAlias = sslconfig.FakeKeyStore.CertificateAuthority.TrustedAlias
    val DistinguishedName = sslconfig.FakeKeyStore.CertificateAuthority.DistinguishedName
  }

  /**
   * @param appPath a file descriptor to the root folder of the project (the root, not a particular module).
   */
  def getKeyStoreFilePath(appPath: File) = FakeKeyStore.getKeyStoreFilePath(appPath)

  def createKeyStore(appPath: File): KeyStore = FakeKeyStore.createKeyStore(appPath)

  // The version in ssl-config isn't public
  private[ssl] def keyManagerFactory(appPath: File): KeyManagerFactory = {
    val keyStore = FakeKeyStore.createKeyStore(appPath)

    // Load the key and certificate into a key manager factory
    val kmf = KeyManagerFactory.getInstance("SunX509")
    kmf.init(keyStore, Array.emptyCharArray)
    kmf
  }

  /**
   * Generate a fresh KeyStore object in memory. This KeyStore
   * is not saved to disk. If you want that, then call `keyManagerFactory`.
   *
   * This method has has `private[play]` access so it can be used for
   * testing.
   */
  // The version in ssl-config isn't public
  private[play] def generateKeyStore: KeyStore = {
    // Create a new KeyStore
    val keyStore: KeyStore = KeyStore.getInstance("JKS")

    // Generate the key pair
    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048) // 2048 is the NIST acceptable key length until 2030
    val keyPair = keyPairGenerator.generateKeyPair()
    val certificateAuthorityKeyPair = keyPairGenerator.generateKeyPair()

    val cacert: X509Certificate = createCertificateAuthority(certificateAuthorityKeyPair)
    // Generate a self signed certificate
    val cert: X509Certificate = createSelfSignedCertificate(keyPair, certificateAuthorityKeyPair)

    // Create the key store, first set the store pass
    keyStore.load(null, Array.emptyCharArray)
    keyStore.setKeyEntry("sslconfiggeneratedCA", keyPair.getPrivate, Array.emptyCharArray, Array(cacert))
    keyStore.setCertificateEntry(CertificateAuthority.TrustedAlias, cacert)
    keyStore.setKeyEntry("sslconfiggenerated", keyPair.getPrivate, Array.emptyCharArray, Array(cert))
    keyStore.setCertificateEntry(TrustedAlias, cert)
    keyStore
  }

  private def createSelfSignedCertificate(keyPair: KeyPair, certificateAuthorityKeyPair: KeyPair): X509Certificate = {
    val certInfo = new X509CertInfo()

    // Serial number and version
    certInfo.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(new BigInteger(64, new SecureRandom())))
    certInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3))

    // Validity
    val validFrom = new Date()
    val validTo = new Date(validFrom.getTime + 50l * 365l * 24l * 60l * 60l * 1000l)
    val validity = new CertificateValidity(validFrom, validTo)
    certInfo.set(X509CertInfo.VALIDITY, validity)

    // Subject and issuer
    val certificateAuthorityName = new X500Name(CertificateAuthority.DistinguishedName)
    certInfo.set(X509CertInfo.ISSUER, certificateAuthorityName)
    val owner = new X500Name(DistinguishedName)
    certInfo.set(X509CertInfo.SUBJECT, owner)

    // Key and algorithm
    certInfo.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic))
    val algorithm = new AlgorithmId(SignatureAlgorithmOID)
    certInfo.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algorithm))

    // Create a new certificate and sign it
    val cert = new X509CertImpl(certInfo)
    cert.sign(keyPair.getPrivate, SignatureAlgorithmName)

    // Since the signature provider may have a different algorithm ID to what we think it should be,
    // we need to reset the algorithm ID, and resign the certificate
    val actualAlgorithm = cert.get(X509CertImpl.SIG_ALG).asInstanceOf[AlgorithmId]
    certInfo.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, actualAlgorithm)
    val newCert = new X509CertImpl(certInfo)
    newCert.sign(certificateAuthorityKeyPair.getPrivate, SignatureAlgorithmName)
    newCert
  }

  private def createCertificateAuthority(keyPair: KeyPair): X509Certificate = {
    val certInfo = new X509CertInfo()

    // Serial number and version
    certInfo.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(new BigInteger(64, new SecureRandom())))
    certInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3))

    // Validity
    val validFrom = new Date()
    val validTo = new Date(validFrom.getTime + 50l * 365l * 24l * 60l * 60l * 1000l) // 50 years
    val validity = new CertificateValidity(validFrom, validTo)
    certInfo.set(X509CertInfo.VALIDITY, validity)

    // Subject and issuer
    val owner = new X500Name(CertificateAuthority.DistinguishedName)
    certInfo.set(X509CertInfo.SUBJECT, owner)
    certInfo.set(X509CertInfo.ISSUER, owner)

    // Key and algorithm
    certInfo.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic))
    val algorithm = new AlgorithmId(SignatureAlgorithmOID)
    certInfo.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algorithm))

    val caExtension =
      new CertificateExtensions
    caExtension.set(BasicConstraintsExtension.NAME, new BasicConstraintsExtension( /* isCritical */ true, /* isCA */ true, 0))
    certInfo.set(X509CertInfo.EXTENSIONS, caExtension)

    // Create a new certificate and sign it
    val cert = new X509CertImpl(certInfo)
    cert.sign(keyPair.getPrivate, SignatureAlgorithmName)

    // Since the signature provider may have a different algorithm ID to what we think it should be,
    // we need to reset the algorithm ID, and resign the certificate
    val actualAlgorithm = cert.get(X509CertImpl.SIG_ALG).asInstanceOf[AlgorithmId]
    certInfo.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, actualAlgorithm)
    val newCert = new X509CertImpl(certInfo)
    newCert.sign(keyPair.getPrivate, SignatureAlgorithmName)
    newCert
  }
}
