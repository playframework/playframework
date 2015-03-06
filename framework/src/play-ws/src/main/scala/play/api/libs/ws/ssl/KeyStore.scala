/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import java.security.KeyStore

import java.io._
import java.security.cert._
import org.apache.commons.codec.binary.Base64
import scala.collection.JavaConverters

trait KeyStoreBuilder {
  def build(): KeyStore
}

object KeystoreFormats {

  def loadCertificates(certs: TraversableOnce[Certificate]): KeyStore = {
    // Must be JKS internally, to enable trusted certificates in JDK 1.6
    val keystore = KeyStore.getInstance(KeyStore.getDefaultType)
    keystore.load(null)
    certs.foreach { cert =>
      val alias = cert.getSubjectX500Principal.getName
      keystore.setCertificateEntry(alias, cert)
    }
    keystore
  }

}

import KeystoreFormats._

/**
 * Builds a keystore from a string containing PEM encoded certificates, using CertificateFactory internally.
 *
 * @see java.security.cert.CertificateFactory
 */
class StringBasedKeyStoreBuilder(data: String) extends KeyStoreBuilder {

  val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def build(): KeyStore = {
    val certs = readCertificates(data)
    val store = loadCertificates(certs)
    store

  }

  def readCertificates(certificateString: String): Seq[Certificate] = {
    val cf = CertificateFactory.getInstance("X.509")
    // CertificateFactory throws EOF on whitespace after end cert, which is very common in triple quoted strings.
    val trimmedString = certificateString.trim()
    val is = new ByteArrayInputStream(trimmedString.getBytes("UTF-8"))
    val bis = new BufferedInputStream(is)
    val buffer = new scala.collection.mutable.ListBuffer[Certificate]()
    while (bis.available() > 0) {
      val cert = cf.generateCertificate(bis)
      buffer.append(cert)
    }
    buffer.toList
  }

}

/**
 * Builds a keystore from a file containing PEM encoded certificates, using CertificateFactory internally.
 *
 * @see java.security.cert.CertificateFactory
 */
class FileBasedKeyStoreBuilder(keyStoreType: String,
    filePath: String,
    password: Option[Array[Char]]) extends KeyStoreBuilder {

  val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def build(): KeyStore = {
    val file = new File(filePath)

    require(file.exists, s"Key store file $filePath does not exist!")
    require(file.canRead, s"Cannot read from key store file $filePath!")

    keyStoreType match {
      case "PEM" =>
        val certs = readCertificates(file)
        loadCertificates(certs)
      case otherFormat =>
        buildFromKeystoreFile(otherFormat, file)
    }
  }

  def buildFromKeystoreFile(storeType: String, file: File): KeyStore = {
    val inputStream = new BufferedInputStream(new FileInputStream(file))
    try {
      val storeType = keyStoreType
      val store = KeyStore.getInstance(storeType)
      store.load(inputStream, password.orNull)
      store
    } finally {
      inputStream.close()
    }
  }

  def readCertificates(file: File): Iterable[Certificate] = {
    import scala.collection.JavaConverters._
    val cf = CertificateFactory.getInstance("X.509")
    val fis = new FileInputStream(file)
    val bis = new BufferedInputStream(fis)

    cf.generateCertificates(bis).asScala
  }

}
