/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.test

import java.security.KeyStore
import javax.net.ssl._

import play.api.Configuration
import play.core.ApplicationProvider
import play.core.server.ssl.FakeKeyStore
import play.core.server.{AkkaHttpServer, NettyServer, ServerConfig, ServerProvider}
import play.server.api.SSLEngineProvider

/**
 * A recipe for making a [[ServerEndpoint]]. Recipes are often used
 * when describing which tests to run. The recipe can be used to start
 * servers with the correct [[ServerEndpoint]]s.
 *
 * @see [[ServerEndpoint.withEndpoint()]]
 */
final case class ServerEndpointRecipe(
    description: String,
    serverConfiguration: Configuration,
    serverProvider: ServerProvider,
    configuredHttpPort: Option[Int],
    configuredHttpsPort: Option[Int],
    serverSsl: ServerEndpointRecipe.ServerSSL,
    scheme: String,
    httpVersions: Set[String],
    serverAttr: Option[String],
) {
  def isAkkaHttp: Boolean = serverProvider == AkkaHttpServer.provider
  def isNetty: Boolean = serverProvider == NettyServer.provider
  def isEncrypted: Boolean = scheme == "https"
  def supportsHttp11: Boolean = httpVersions.contains("1.1")
  def supportsHttp2: Boolean = httpVersions.contains("2")
}

object ServerEndpointRecipe {

  /** SSL information used by an endpoint server and client. */
  case class ServerSSL(sslContext: SSLContext, trustManager: X509TrustManager)

  /**
   * An SSLEngineProvider which simply references the values in the
   * SelfSigned object.
   */
  private class SelfSignedSSLEngineProvider(serverConfig: ServerConfig, appProvider: ApplicationProvider) extends SSLEngineProvider {
    override def createSSLEngine: SSLEngine = SelfSigned.sslContext.createSSLEngine()
  }

  /**
   * Contains a statically initialized self-signed certificate.
   */
  private object SelfSigned {

    /**
     * The SSLContext and TrustManager associated with the self-signed certificate.
     */
    lazy val (sslContext, trustManager): (SSLContext, X509TrustManager) = {
      val keyStore: KeyStore = FakeKeyStore.generateKeyStore

      val kmf: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
      kmf.init(keyStore, "".toCharArray)
      val kms: Array[KeyManager] = kmf.getKeyManagers

      val tmf: TrustManagerFactory = TrustManagerFactory
          .getInstance(TrustManagerFactory.getDefaultAlgorithm())
      tmf.init(keyStore)
      val tms: Array[TrustManager] = tmf.getTrustManagers
      val x509TrustManager: X509TrustManager = tms(0).asInstanceOf[X509TrustManager]

      val sslContext: SSLContext = SSLContext.getInstance("TLS")
      sslContext.init(kms, tms, null)

      (sslContext, x509TrustManager)
    }
  }

  private def createRecipe(
      description: String,
      provider: ServerProvider,
      encrypted: Boolean,
      configureHttp2: Boolean,
      httpVersions: Set[String],
      serverAttr: Option[String]): ServerEndpointRecipe = {
    ServerEndpointRecipe(
      description = description,
      serverConfiguration = Configuration((
        Seq("play.server.akka.http2.enabled" -> configureHttp2) ++
        (if (encrypted) Seq("play.server.https.engineProvider" -> classOf[ServerEndpointRecipe.SelfSignedSSLEngineProvider].getName) else Seq.empty)
      ): _*),
      serverProvider = provider,
      configuredHttpPort = if (encrypted) None else Some(0),
      configuredHttpsPort = if (encrypted) Some(0) else None,
      serverSsl = ServerSSL(
        ServerEndpointRecipe.SelfSigned.sslContext,
        ServerEndpointRecipe.SelfSigned.trustManager
      ),
      scheme = if (encrypted) "https" else "http",
      httpVersions = httpVersions,
      serverAttr = serverAttr
    )
  }

  // Default endpoint recipes
  val Netty11Plaintext = createRecipe("Netty HTTP/1.1 (plaintext)", NettyServer.provider, encrypted = false, configureHttp2 = false, Set("1.0", "1.1"), Option("netty"))
  val Netty11Encrypted = createRecipe("Netty HTTP/1.1 (encrypted)", NettyServer.provider, encrypted = true, configureHttp2 = false, Set("1.0", "1.1"), Option("netty"))
  val AkkaHttp11Plaintext = createRecipe("Akka HTTP HTTP/1.1 (plaintext)", AkkaHttpServer.provider, encrypted = false, configureHttp2 = false, Set("1.0", "1.1"), None)
  val AkkaHttp11Encrypted = createRecipe("Akka HTTP HTTP/1.1 (encrypted)", AkkaHttpServer.provider, encrypted = true, configureHttp2 = false, Set("1.0", "1.1"), None)
  val AkkaHttp20Encrypted = createRecipe("Akka HTTP HTTP/2 (encrypted)", AkkaHttpServer.provider, encrypted = true, configureHttp2 = true, Set("1.0", "1.1", "2"), None)

  /**
   * The list of server endpoints.
   */
  val AllRecipes: Seq[ServerEndpointRecipe] = Seq(
    Netty11Plaintext,
    Netty11Encrypted,
    AkkaHttp11Plaintext,
    AkkaHttp11Encrypted,
    AkkaHttp20Encrypted
  )
}