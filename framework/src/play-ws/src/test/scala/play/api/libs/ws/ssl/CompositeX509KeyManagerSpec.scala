/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import java.net.Socket
import java.security.{ Principal, PrivateKey }

import scala.Array

import javax.net.ssl.{ X509ExtendedKeyManager, SSLEngine, X509KeyManager }
import org.specs2.mock._
import org.specs2.mutable._
import java.security.cert.X509Certificate

object CompositeX509KeyManagerSpec extends Specification with Mockito {

  def mockExtendedX509KeyManager(clientResponse: String = null, serverResponse: String = null) = new X509ExtendedKeyManager() {

    override def chooseEngineClientAlias(keyType: Array[String], issuers: Array[Principal], engine: SSLEngine): String = clientResponse

    override def chooseEngineServerAlias(keyType: String, issuers: Array[Principal], engine: SSLEngine): String = serverResponse

    def getClientAliases(keyType: String, issuers: Array[Principal]): Array[String] = ???

    def chooseClientAlias(keyType: Array[String], issuers: Array[Principal], socket: Socket): String = ???

    def getServerAliases(keyType: String, issuers: Array[Principal]): Array[String] = ???

    def chooseServerAlias(keyType: String, issuers: Array[Principal], socket: Socket): String = ???

    def getCertificateChain(alias: String): Array[X509Certificate] = ???

    def getPrivateKey(alias: String): PrivateKey = ???
  }

  "CompositeX509KeyManager" should {

    "chooseEngineClientAlias" should {
      "not do anything with a X509KeyManager" in {
        val mockKeyManager = mock[X509KeyManager]
        val keyManager = new CompositeX509KeyManager(Seq(mockKeyManager))
        val keyType = Array("derp")
        val issuers = Array[Principal]()
        val engine = mock[SSLEngine]

        val serverAlias = keyManager.chooseEngineClientAlias(keyType = keyType, issuers = issuers, engine = engine)
        serverAlias must beNull
      }

      "return a result" in {
        val mockKeyManager = mockExtendedX509KeyManager(clientResponse = "clientAlias")
        val keyManager = new CompositeX509KeyManager(Seq(mockKeyManager))
        val keyType = Array("derp")
        val issuers = Array[Principal]()
        val engine = mock[SSLEngine]

        val serverAlias = keyManager.chooseEngineClientAlias(keyType = keyType, issuers = issuers, engine = engine)
        serverAlias must be_==("clientAlias")
      }

      "return null" in {
        val mockKeyManager = mockExtendedX509KeyManager()
        val keyManager = new CompositeX509KeyManager(Seq(mockKeyManager))
        val keyType = Array("derp")
        val issuers = Array[Principal]()
        val engine = mock[SSLEngine]

        val serverAlias = keyManager.chooseEngineClientAlias(keyType = keyType, issuers = issuers, engine = engine)
        serverAlias must beNull
      }
    }

    "chooseEngineServerAlias" should {

      "not do anything with a X509KeyManager" in {
        val mockKeyManager = mock[X509KeyManager]
        val keyManager = new CompositeX509KeyManager(Seq(mockKeyManager))
        val keyType = "derp"
        val issuers = Array[Principal]()
        val engine = mock[SSLEngine]

        val serverAlias = keyManager.chooseEngineServerAlias(keyType = keyType, issuers = issuers, engine = engine)
        serverAlias must beNull
      }

      "return a result" in {
        val mockKeyManager = mockExtendedX509KeyManager(serverResponse = "serverAlias")
        val keyManager = new CompositeX509KeyManager(Seq(mockKeyManager))
        val keyType = "derp"
        val issuers = Array[Principal]()
        val engine = mock[SSLEngine]

        val serverAlias = keyManager.chooseEngineServerAlias(keyType = keyType, issuers = issuers, engine = engine)
        serverAlias must be_==("serverAlias")
      }

      "return null" in {
        val mockKeyManager = mockExtendedX509KeyManager()
        val keyManager = new CompositeX509KeyManager(Seq(mockKeyManager))
        val keyType = "derp"
        val issuers = Array[Principal]()
        val engine = mock[SSLEngine]

        val serverAlias = keyManager.chooseEngineServerAlias(keyType = keyType, issuers = issuers, engine = engine)
        serverAlias must beNull
      }
    }

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
