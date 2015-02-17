/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import org.specs2.mutable._

import java.security.{ SecureRandom, KeyPairGenerator }
import org.joda.time.Instant
import sun.security.x509.AlgorithmId

object AlgorithmsSpec extends Specification {
  import Algorithms._

  "keySize" should {

    "show a keysize of 1024 for RSA" in {
      val dn = "cn=Common Name, ou=eng  ineering, o=company, c=US"
      val from = Instant.now
      val to = from.plus(5000000)

      // Use RSA with a SHA1 certificate signing algoirthm.
      val keyGen = KeyPairGenerator.getInstance("RSA")
      keyGen.initialize(1024, new SecureRandom())
      val pair = keyGen.generateKeyPair()
      val cert = CertificateGenerator.generateCertificate(dn, pair, from.toDate, to.toDate, "SHA1WithRSA", AlgorithmId.sha1WithRSAEncryption_oid)

      // RSA is getModulus.bitLength
      keySize(cert.getPublicKey) must_== Some(1024)
    }

    "show a keysize of 1024 for DSA" in {
      val dn = "cn=Common Name, ou=engineering, o=company, c=US"
      val from = Instant.now
      val to = from.plus(5000000)

      // Use RSA with a DSA certificate signing algoirthm.
      val keyGen = KeyPairGenerator.getInstance("DSA")
      keyGen.initialize(1024, new SecureRandom())
      val pair = keyGen.generateKeyPair()
      val cert = CertificateGenerator.generateCertificate(dn, pair, from.toDate, to.toDate, "SHA1WithDSA", AlgorithmId.sha1WithDSA_oid)

      // DSA is getP.bitLength
      keySize(cert.getPublicKey) must_== Some(1024)
    }

  }

  "decompose" should {

    "decompose MD5" in {
      decomposes("MD5WithRSA") must containTheSameElementsAs(Seq("MD5", "RSA"))
    }

    "decompose MD2" in {
      decomposes("MD2WithRSA") must containTheSameElementsAs(Seq("MD2", "RSA"))
    }

    "decompose SHA1" in {
      decomposes("SHA1WithRSA") must containTheSameElementsAs(Seq("SHA1", "SHA-1", "RSA"))
    }

    "map SHA-1 to SHA1" in {
      decomposes("SHA-1WithRSA") must containTheSameElementsAs(Seq("SHA1", "SHA-1", "RSA"))
    }

    "decompose SHA256" in {
      decomposes("SHA256WithRSA") must containTheSameElementsAs(Seq("SHA256", "RSA"))
    }

  }

}
