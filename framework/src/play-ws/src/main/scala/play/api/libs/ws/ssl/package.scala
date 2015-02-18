/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws

import java.security.cert.{ PKIXCertPathValidatorResult, CertPathValidatorResult, Certificate, X509Certificate }
import scala.util.Properties.{ isJavaAtLeast, javaVmName }

package object ssl {

  import scala.language.implicitConversions

  implicit def certificate2X509Certificate(cert: java.security.cert.Certificate): X509Certificate = {
    cert.asInstanceOf[X509Certificate]
  }

  implicit def arrayCertsToListCerts(chain: Array[Certificate]): java.util.List[Certificate] = {
    import scala.collection.JavaConverters._
    chain.toList.asJava
  }

  implicit def certResult2PKIXResult(result: CertPathValidatorResult): PKIXCertPathValidatorResult = {
    result.asInstanceOf[PKIXCertPathValidatorResult]
  }

  def debugChain(chain: Array[X509Certificate]): Seq[String] = {
    chain.map {
      cert =>
        s"${cert.getSubjectDN.getName}"
    }
  }

  def foldVersion[T](run16: => T, runHigher: => T): T = {
    System.getProperty("java.specification.version") match {
      case "1.6" =>
        run16
      case higher =>
        runHigher
    }
  }

  def isOpenJdk: Boolean = javaVmName contains "OpenJDK"

  // NOTE: Some SSL classes in OpenJDK 6 are in the same locations as JDK 7
  def foldRuntime[T](older: => T, newer: => T): T = {
    if (isJavaAtLeast("1.7") || isOpenJdk) newer else older
  }

}
