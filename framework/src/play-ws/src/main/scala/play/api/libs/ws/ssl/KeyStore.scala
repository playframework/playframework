/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
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

  def loadCertificate(cert: X509Certificate): KeyStore = {
    val alias = cert.getSubjectX500Principal.getName

    val keystore = KeyStore.getInstance(KeyStore.getDefaultType)
    keystore.load(null)
    keystore.setCertificateEntry(alias, cert)
    keystore
  }

}

import KeystoreFormats._

/**
 * Builds a keystore from a string that is passed in.  Currently only supports PEM.
 */
class StringBasedKeyStoreBuilder(keyStoreType: String,
    data: String,
    password: Option[Array[Char]] = None) extends KeyStoreBuilder {

  val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def build(): KeyStore = {
    keyStoreType match {
      case "PEM" =>
        val cert = readCertificate(data)
        val store = loadCertificate(cert)
        store
      case unsupportedType =>
        throw new IllegalStateException(s"No support for embedded strings with type $unsupportedType")
    }
  }

  def readCertificate(certificateString: String): X509Certificate = {
    val certLines = certificateString.split('\n').iterator
    val data = textBlock(certLines)
    val base64 = new Base64()
    val binaryData = base64.decode(data)

    val cf = CertificateFactory.getInstance("X.509")
    val bais = new ByteArrayInputStream(binaryData)
    val bis = new BufferedInputStream(bais)
    cf.generateCertificate(bis).asInstanceOf[X509Certificate]
  }

  def textBlock(lines: Iterator[String]): String = {
    val certStartText = "-----BEGIN CERTIFICATE-----"
    val certEndText = "-----END CERTIFICATE-----"

    lines.dropWhile(certStartText.!=).drop(1).takeWhile(certEndText.!=).mkString
  }

}

/**
 * Builds a keystore from a filepath.
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
        val cert = readCertificate(file)
        loadCertificate(cert)
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

  def readCertificate(file: File): X509Certificate = {
    val cf = CertificateFactory.getInstance("X.509")
    val fis = new FileInputStream(file)
    val bis = new BufferedInputStream(fis)

    cf.generateCertificate(bis).asInstanceOf[X509Certificate]
  }

}