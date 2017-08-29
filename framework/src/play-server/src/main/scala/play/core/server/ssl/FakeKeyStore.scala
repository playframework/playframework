/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.ssl

import play.api.Logger
import java.security.{ KeyStore, SecureRandom, KeyPairGenerator, KeyPair }
import sun.security.x509._
import java.util.Date
import java.math.BigInteger
import java.security.cert.X509Certificate
import java.io.File
import javax.net.ssl.KeyManagerFactory
import play.utils.PlayIO
import java.security.interfaces.RSAPublicKey

/**
 * A fake key store
 */
object FakeKeyStore {
  private val logger = Logger(FakeKeyStore.getClass)
  val GeneratedKeyStore = "conf/generated.keystore"
  val DnName = "CN=localhost, OU=Unit Testing, O=Mavericks, L=Moon Base 1, ST=Cyberspace, C=CY"
  val SignatureAlgorithmOID = AlgorithmId.sha256WithRSAEncryption_oid
  val SignatureAlgorithmName = "SHA256withRSA"

  def shouldGenerate(keyStoreFile: File): Boolean = {
    import scala.collection.JavaConverters._

    if (!keyStoreFile.exists()) {
      return true
    }

    // Should regenerate if we find an unacceptably weak key in there.
    val store = KeyStore.getInstance("JKS")
    val in = java.nio.file.Files.newInputStream(keyStoreFile.toPath)
    try {
      store.load(in, "".toCharArray)
    } finally {
      PlayIO.closeQuietly(in)
    }
    store.aliases().asScala.exists { alias =>
      Option(store.getCertificate(alias)).exists(c => certificateTooWeak(c))
    }
  }

  def certificateTooWeak(c: java.security.cert.Certificate): Boolean = {
    val key: RSAPublicKey = c.getPublicKey.asInstanceOf[RSAPublicKey]
    key.getModulus.bitLength < 2048 || c.asInstanceOf[X509CertImpl].getSigAlgName != SignatureAlgorithmName
  }

  def keyManagerFactory(appPath: File): KeyManagerFactory = {
    val keyStoreFile = new File(appPath, GeneratedKeyStore)
    val keyStore: KeyStore = if (shouldGenerate(keyStoreFile)) {
      logger.info("Generating HTTPS key pair in " + keyStoreFile.getAbsolutePath + " - this may take some time. If nothing happens, try moving the mouse/typing on the keyboard to generate some entropy.")

      val freshKeyStore: KeyStore = generateKeyStore
      val out = java.nio.file.Files.newOutputStream(keyStoreFile.toPath)
      try {
        freshKeyStore.store(out, "".toCharArray)
      } finally {
        PlayIO.closeQuietly(out)
      }
      freshKeyStore
    } else {
      // Load a KeyStore from a file
      val loadedKeystore: KeyStore = KeyStore.getInstance("JKS")
      val in = java.nio.file.Files.newInputStream(keyStoreFile.toPath)
      try {
        loadedKeystore.load(in, "".toCharArray)
      } finally {
        PlayIO.closeQuietly(in)
      }
      loadedKeystore
    }

    // Load the key and certificate into a key manager factory
    val kmf = KeyManagerFactory.getInstance("SunX509")
    kmf.init(keyStore, "".toCharArray)
    kmf
  }

  /**
   * Generate a fresh KeyStore object in memory. This KeyStore
   * is not saved to disk. If you want that, then call `keyManagerFactory`.
   *
   * This method has has `private[play]` access so it can be used for
   * testing.
   */
  private[play] def generateKeyStore: KeyStore = {
    // Create a new KeyStore
    val keyStore: KeyStore = KeyStore.getInstance("JKS")

    // Generate the key pair
    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048) // 2048 is the NIST acceptable key length until 2030
    val keyPair = keyPairGenerator.generateKeyPair()

    // Generate a self signed certificate
    val cert: X509Certificate = createSelfSignedCertificate(keyPair)

    // Create the key store, first set the store pass
    keyStore.load(null, "".toCharArray)
    keyStore.setKeyEntry("playgenerated", keyPair.getPrivate, "".toCharArray, Array(cert))
    keyStore.setCertificateEntry("playgeneratedtrusted", cert)
    keyStore
  }

  def createSelfSignedCertificate(keyPair: KeyPair): X509Certificate = {
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
    val owner = new X500Name(DnName)
    certInfo.set(X509CertInfo.SUBJECT, owner)
    certInfo.set(X509CertInfo.ISSUER, owner)

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
    newCert.sign(keyPair.getPrivate, SignatureAlgorithmName)
    newCert
  }
}
