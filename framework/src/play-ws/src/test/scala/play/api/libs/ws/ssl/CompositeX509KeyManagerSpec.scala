/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import org.specs2.mutable._
import org.specs2.mock._

import javax.net.ssl.X509KeyManager
import java.security.{PrivateKey, Principal}
import java.net.Socket
import java.security.cert.X509Certificate
import scala.Array

object CompositeX509KeyManagerSpec extends Specification with Mockito {

  "CompositeX509KeyManager" should {

    "chooseClientAlias" should {
       "return a result" in {
         val mockKeyManager = mock[X509KeyManager]
         val keyManager = new CompositeX509KeyManager(Seq(mockKeyManager))
         val keyType = Array("derp")
         val issuers = Array[Principal]()
         val socket = mock[Socket]

         mockKeyManager.chooseClientAlias(keyType, issuers, socket) returns "clientAlias"

         val serverAlias = keyManager.chooseClientAlias(keyType = keyType, issuers = issuers, socket = socket)
         serverAlias must be_==("clientAlias")
       }

      "return null" in {
        val mockKeyManager = mock[X509KeyManager]
        val keyManager = new CompositeX509KeyManager(Seq(mockKeyManager))
        val keyType = Array("derp")
        val issuers = Array[Principal]()
        val socket = mock[Socket]

        mockKeyManager.chooseClientAlias(keyType, issuers, socket) returns null

        val serverAlias = keyManager.chooseClientAlias(keyType = keyType, issuers = issuers, socket = socket)
        serverAlias must beNull
      }
    }

    "getClientAliases" should {

      "return a result" in {
        val mockKeyManager = mock[X509KeyManager]
        val keyManager = new CompositeX509KeyManager(Seq(mockKeyManager))
        val keyType = "derp"
        val issuers = Array[Principal]()

        mockKeyManager.getClientAliases(keyType, issuers) returns Array("clientAliases")

        val clientAliases = keyManager.getClientAliases(keyType = keyType, issuers = issuers)
        clientAliases must be_==(Array("clientAliases"))
      }

      "return null" in {
        val mockKeyManager = mock[X509KeyManager]
        val keyManager = new CompositeX509KeyManager(Seq(mockKeyManager))
        val keyType = "derp"
        val issuers = Array[Principal]()

        mockKeyManager.getClientAliases(keyType, issuers) returns null

        val clientAliases = keyManager.getClientAliases(keyType = keyType, issuers = issuers)
        clientAliases must beNull
      }
    }

    "getServerAliases" should {
      "return a result" in {
        val mockKeyManager = mock[X509KeyManager]
        val keyManager = new CompositeX509KeyManager(Seq(mockKeyManager))
        val keyType = "derp"
        val issuers = Array[Principal]()

        mockKeyManager.getServerAliases(keyType, issuers) returns Array("serverAliases")

        val serverAliases = keyManager.getServerAliases(keyType = keyType, issuers = issuers)
        serverAliases must be_==(Array("serverAliases"))
      }

      "return null" in {
        val mockKeyManager = mock[X509KeyManager]
        val keyManager = new CompositeX509KeyManager(Seq(mockKeyManager))
        val keyType = "derp"
        val issuers = Array[Principal]()

        mockKeyManager.getServerAliases(keyType, issuers) returns null

        val serverAliases = keyManager.getServerAliases(keyType = keyType, issuers = issuers)
        serverAliases must beNull
      }
    }

    "chooseServerAlias" should {
      "work fine" in {
        val mockKeyManager = mock[X509KeyManager]
        val keyManager = new CompositeX509KeyManager(Seq(mockKeyManager))
        val keyType = "derp"
        val issuers = Array[Principal]()
        val socket = mock[Socket]

        mockKeyManager.chooseServerAlias(keyType, issuers, socket) returns "serverAlias"

        val serverAlias = keyManager.chooseServerAlias(keyType = keyType, issuers = issuers, socket = socket)
        serverAlias must be_==("serverAlias")
      }

      "return null" in {
        val mockKeyManager = mock[X509KeyManager]
        val keyManager = new CompositeX509KeyManager(Seq(mockKeyManager))
        val keyType = "derp"
        val issuers = Array[Principal]()
        val socket = mock[Socket]

        mockKeyManager.chooseServerAlias(keyType, issuers, socket) returns null

        val serverAlias = keyManager.chooseServerAlias(keyType = keyType, issuers = issuers, socket = socket)
        serverAlias must beNull
      }
    }

    "getCertificateChain" should {
      "work fine" in {
        val mockKeyManager = mock[X509KeyManager]
        val keyManager = new CompositeX509KeyManager(Seq(mockKeyManager))
        val alias = "alias"
        val cert = CertificateGenerator.generateRSAWithSHA256()

        mockKeyManager.getCertificateChain(alias) returns Array(cert)

        val certChain = keyManager.getCertificateChain(alias = alias)
        certChain must be_==(Array(cert))
      }

      "return null" in {
        val mockKeyManager = mock[X509KeyManager]
        val keyManager = new CompositeX509KeyManager(Seq(mockKeyManager))
        val alias = "alias"

        mockKeyManager.getCertificateChain(alias) returns null

        val certChain = keyManager.getCertificateChain(alias = alias)
        certChain must beNull
      }
    }

    "getPrivateKey" should {
      "work fine" in {
        val mockKeyManager = mock[X509KeyManager]
        val keyManager = new CompositeX509KeyManager(Seq(mockKeyManager))
        val alias = "alias"
        val privateKey = mock[PrivateKey]

        mockKeyManager.getPrivateKey(alias) returns privateKey

        val actual = keyManager.getPrivateKey(alias = alias)
        actual must be_==(privateKey)
      }

      "return null" in {
        val mockKeyManager = mock[X509KeyManager]
        val keyManager = new CompositeX509KeyManager(Seq(mockKeyManager))
        val alias = "alias"

        mockKeyManager.getPrivateKey(alias) returns null

        val actual = keyManager.getPrivateKey(alias = alias)
        actual must beNull
      }
    }

  }
}
