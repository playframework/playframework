/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import java.security.KeyStore

import java.io._
import java.security.cert._
import com.ning.http.util.Base64
import scala.collection.JavaConverters

/**
 * http://nelenkov.blogspot.com/2011/12/using-custom-certificate-trust-store-on.html
 * http://markgamache.blogspot.com/2013/05/demystifying-certificate-requirements.html
 * http://www.sslshopper.com/article-how-to-create-a-self-signed-certificate-using-java-keytool.html
 * http://www.sslshopper.com/article-most-common-java-keytool-keystore-commands.html
 * https://gist.github.com/mtigas/952344
 * http://jcalcote.wordpress.com/2008/11/13/java-secure-http-keys/
 * http://tech.lanesnotes.com/2009/04/creating-ssl-certificates-with-multiple.html
 *
 * The client key store contains the client’s self-signed certificate and private key.
 *
 * Generate
 * keytool -genkey -alias client -keyalg RSA -storepass changeit -keystore client-keystore.jks
 *
 * Create your own certificate authority.
 *
 * Generate the sever keystore (contains server private and public keys):
 * keytool -genkey -alias server -keyalg RSA -storepass changeit -keystore server-keystore.jks
 * Use server's DNS name for the CN of the Subject DN, e.g.
 * What is your first and last name?
 * [Unknown]:  myserver.example.com
 *
 * You should see CN=myserver.example.com in your certificate.
 *
 * If you want to define the IP address, you should set it as subjectAltName in the certificate.
 * 'keytool' is unable to add a "Subject Alternative Name", so you have to use openssl for that.
 *
 *
 * Export server's public certificate into a stand alone certificate file:
 * keytool -exportcert -keystore server-keystore.jks -file server.der -alias server -storepass changeit
 *
 * Import the server's public certificate into a truststore (JKS format):
 * keytool -importcert -trustcacerts -alias server -file server.der -keystore server-truststore.jks -storepass changeit
 *
 * The client trust store contains the server’s self-signed certificate.
 * Trust Manager MUST use JKS format for trusted certificates.
 */

// The most complete reference on SSL client certificates, and what goes wrong (in 6u25):
// http://blog.palominolabs.com/2011/10/18/java-2-way-tlsssl-client-certificates-and-pkcs12-vs-jks-keystores/
//
//    The result of all this analysis: For your SSLServerSocket’s TrustManager’s KeyStore, use a JKS containing only the
//    CA cert for the client certs. If you us a PKCS12, you’ll get no certs. If you include the cert and key that you’re
//    looking for as well as the CA cert and you create the JKS keystore from one PKCS12 containing all three entities,
//    you’ll get the wrong cert. If you create a JKS keystore using the CA cert and then add the client cert (with or
//    without key) later, you’ll get too many certs.
//
// More odd things involving client certificates:
// http://blog.chariotsolutions.com/2013/01/https-with-client-certificates-on.html
//
// Another blog post on building client certificates (using openssl, worthwhile for docs):
// http://quakology.blogspot.com/2009/06/how-to-use-ssl-with-client-certificate.html
// http://codyaray.com/2013/04/java-ssl-with-multiple-keystores
// http://stackoverflow.com/questions/1793979/registering-multiple-keystores-in-jvm?rq=1

// There is a note in jsslutils about chooseServerAlias returning the wrong method
/*
   This is an X509KeyManager that will always choose the server alias name it
    has been constructed with.
  */

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
    val binaryData = Base64.decode(data)

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

  //  /**
  //   * Reads a string (using "UTF-8" charset) and creates a DER certificate out of it.
  //   *
  //   * @param certificateString
  //   * @return
  //   */
  //  def readCertificate(certificateString: String): X509Certificate = {
  //    val bais = new ByteArrayInputStream(certificateString.getBytes("UTF-8"))
  //    val cf = CertificateFactory.getInstance("X.509")
  //    val bis = new BufferedInputStream(bais)
  //
  //    cf.generateCertificate(bis).asInstanceOf[X509Certificate]
  //  }

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